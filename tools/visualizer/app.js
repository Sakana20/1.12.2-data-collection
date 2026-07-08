(function () {
  var state = {
    recordings: [],
    current: null,
    externalExitRegions: [],
    view: null,
    playhead: 0
  };

  var els = {
    recordingInput: document.getElementById("recordingInput"),
    folderInput: document.getElementById("folderInput"),
    journeyMapInput: document.getElementById("journeyMapInput"),
    recordingSelect: document.getElementById("recordingSelect"),
    colorMode: document.getElementById("colorMode"),
    journeyMapLayer: document.getElementById("journeyMapLayer"),
    showJourneyMap: document.getElementById("showJourneyMap"),
    showYaw: document.getElementById("showYaw"),
    showGrid: document.getElementById("showGrid"),
    playhead: document.getElementById("playhead"),
    fitMap: document.getElementById("fitMap"),
    zoomInMap: document.getElementById("zoomInMap"),
    zoomOutMap: document.getElementById("zoomOutMap"),
    summaryGrid: document.getElementById("summaryGrid"),
    routeCanvas: document.getElementById("routeCanvas"),
    speedCanvas: document.getElementById("speedCanvas"),
    distanceCanvas: document.getElementById("distanceCanvas"),
    sampleDetails: document.getElementById("sampleDetails"),
    exitList: document.getElementById("exitList"),
    segmentTable: document.getElementById("segmentTable"),
    segmentCount: document.getElementById("segmentCount"),
    emptyState: document.getElementById("emptyState"),
    mapSubtitle: document.getElementById("mapSubtitle"),
    serverStatus: document.getElementById("serverStatus"),
    connectServer: document.getElementById("connectServer"),
    shutdownServer: document.getElementById("shutdownServer")
  };

  var palette = ["#52c7a5", "#efb752", "#64a8ff", "#ff7a90", "#b98cff", "#8bd45f", "#f07fd1", "#58d2e9"];
  var api = {
    enabled: false,
    baseUrl: "",
    tileCache: Object.create(null),
    missingTiles: Object.create(null),
    maxVisibleTiles: 260
  };
  var journeyMap = {
    tileSize: 512,
    layers: { day: [], night: [], topo: [] },
    loaded: false
  };
  var dragState = {
    active: false,
    pointerId: null,
    lastX: 0,
    lastY: 0
  };

  els.recordingInput.addEventListener("change", function (event) {
    var file = event.target.files && event.target.files[0];
    if (!file) return;
    readJsonFile(file).then(function (data) {
      setRecordings([{ name: file.name, data: normalizeRecording(data, file.name) }]);
    }).catch(showError);
  });

  els.folderInput.addEventListener("change", function (event) {
    var files = Array.prototype.slice.call(event.target.files || []);
    loadFolder(files).catch(showError);
  });

  els.journeyMapInput.addEventListener("change", function (event) {
    var files = Array.prototype.slice.call(event.target.files || []);
    loadJourneyMapTiles(files);
    render();
  });

  els.recordingSelect.addEventListener("change", function () {
    var next = state.recordings[Number(els.recordingSelect.value)];
    if (!next) return;
    if (api.enabled && !next.data) loadServerRecording(next.name).catch(showError);
    else setCurrent(next.data);
  });

  els.colorMode.addEventListener("change", render);
  els.journeyMapLayer.addEventListener("change", render);
  els.showJourneyMap.addEventListener("change", render);
  els.showYaw.addEventListener("change", render);
  els.showGrid.addEventListener("change", render);
  els.playhead.addEventListener("input", function () {
    state.playhead = Number(els.playhead.value);
    render();
  });
  els.fitMap.addEventListener("click", function () {
    state.view = null;
    render();
  });
  els.zoomInMap.addEventListener("click", function () {
    zoomMapAtCenter(1.35);
  });
  els.zoomOutMap.addEventListener("click", function () {
    zoomMapAtCenter(1 / 1.35);
  });
  els.routeCanvas.addEventListener("wheel", handleMapWheel, { passive: false });
  els.routeCanvas.addEventListener("pointerdown", handleMapPointerDown);
  els.routeCanvas.addEventListener("pointermove", handleMapPointerMove);
  els.routeCanvas.addEventListener("pointerup", handleMapPointerUp);
  els.routeCanvas.addEventListener("pointercancel", handleMapPointerUp);
  els.routeCanvas.addEventListener("dblclick", function () {
    state.view = null;
    render();
  });
  els.connectServer.addEventListener("click", function () {
    initServerMode(true);
  });
  els.shutdownServer.addEventListener("click", shutdownServer);
  window.addEventListener("resize", render);
  initServerMode(false);

  function initServerMode(showFailure) {
    var candidates = [];
    if (window.location.protocol === "http:" || window.location.protocol === "https:") {
      candidates.push("");
    }
    candidates.push("http://localhost:8787");

    els.connectServer.disabled = true;
    tryConnectServer(candidates).then(function (result) {
      api.baseUrl = result.baseUrl;
      api.enabled = true;
      journeyMap.loaded = true;
      if (result.config && result.config.tileSize) journeyMap.tileSize = Number(result.config.tileSize) || journeyMap.tileSize;
      document.body.classList.add("server-mode");
      els.serverStatus.textContent = api.baseUrl ? "已连接本地服务" : "本地服务模式";

      return Promise.all([
        fetchJson("/api/exit_regions").catch(function () { return []; }),
        fetchJson("/api/recordings")
      ]);
    }).then(function (results) {
      state.externalExitRegions = Array.isArray(results[0]) ? results[0] : [];
      var recordings = (results[1].recordings || []).map(function (item) {
        return {
          name: item.name,
          bytes: item.bytes,
          lastWriteTime: item.lastWriteTime,
          data: null
        };
      });
      setRecordings(recordings);
    }).catch(function () {
      api.enabled = false;
      api.baseUrl = "";
      els.connectServer.disabled = false;
      els.connectServer.textContent = "连接本地服务";
      journeyMap.loaded = Object.keys(journeyMap.layers).some(function (layer) {
        return journeyMap.layers[layer].length > 0;
      });
      if (showFailure) {
        showError(new Error("没有连接到 http://localhost:8787。请先运行 tools\\visualizer-server.ps1。"));
      }
    });
  }

  function tryConnectServer(candidates) {
    var index = 0;
    function next() {
      if (index >= candidates.length) return Promise.reject(new Error("No local service available."));
      var baseUrl = candidates[index++];
      return fetchJson("/api/config", baseUrl).then(function (config) {
        return { baseUrl: baseUrl, config: config };
      }).catch(next);
    }
    return next();
  }

  function fetchJson(url, baseUrl) {
    var root = baseUrl == null ? api.baseUrl : baseUrl;
    return fetch(root + url, { cache: "no-store" }).then(function (response) {
      if (!response.ok) throw new Error("HTTP " + response.status + " " + url);
      return response.json();
    });
  }

  function shutdownServer() {
    if (!api.enabled) return;
    els.shutdownServer.disabled = true;
    els.shutdownServer.textContent = "正在关闭";
    fetchJson("/api/shutdown").then(function () {
      api.enabled = false;
      api.baseUrl = "";
      document.body.classList.remove("server-mode");
      els.serverStatus.textContent = "服务已关闭";
      els.shutdownServer.textContent = "已关闭";
      els.connectServer.disabled = false;
      els.connectServer.textContent = "连接本地服务";
    }).catch(function (error) {
      els.shutdownServer.disabled = false;
      els.shutdownServer.textContent = "关闭服务";
      showError(error);
    });
  }

  function loadServerRecording(name) {
    return fetchJson("/api/recordings/" + encodeURIComponent(name)).then(function (data) {
      var index = state.recordings.findIndex(function (recording) {
        return recording.name === name;
      });
      var normalized = normalizeRecording(data, name);
      if (index !== -1) state.recordings[index].data = normalized;
      setCurrent(normalized);
    });
  }

  function loadFolder(files) {
    var jsonFiles = files.filter(function (file) {
      return /\.json$/i.test(file.name);
    });

    return Promise.all(jsonFiles.map(function (file) {
      return readJsonFile(file).then(function (data) {
        return { file: file, data: data };
      });
    })).then(function (items) {
      var exitFile = items.find(function (item) {
        return item.file.name.toLowerCase() === "exit_regions.json";
      });
      state.externalExitRegions = Array.isArray(exitFile && exitFile.data) ? exitFile.data : [];

      var recordings = items
        .filter(function (item) { return item.file.name.toLowerCase() !== "exit_regions.json"; })
        .filter(function (item) { return item.data && Array.isArray(item.data.samples); })
        .sort(function (a, b) { return a.file.name.localeCompare(b.file.name); })
        .map(function (item) {
          return { name: item.file.name, data: normalizeRecording(item.data, item.file.name) };
        });

      setRecordings(recordings);
    });
  }

  function readJsonFile(file) {
    return file.text().then(function (text) {
      return JSON.parse(text);
    });
  }

  function normalizeRecording(data, name) {
    var samples = (data.samples || []).map(function (sample, index) {
      var prev = data.samples[index - 1];
      var dt = prev ? Math.max(0, Number(sample.timeSeconds || 0) - Number(prev.timeSeconds || 0)) : 0;
      var distance = Number(sample.delta && sample.delta.distance2d || 0);
      return Object.assign({}, sample, {
        index: index,
        speed2d: dt > 0 ? distance / dt : 0
      });
    });
    var exitRegions = data.exitRegions && data.exitRegions.length ? data.exitRegions : state.externalExitRegions;
    return Object.assign({}, data, {
      fileName: name,
      samples: samples,
      exitRegions: exitRegions || [],
      segments: buildSegments(samples)
    });
  }

  function setRecordings(recordings) {
    state.recordings = recordings;
    els.recordingSelect.innerHTML = "";
    recordings.forEach(function (recording, index) {
      var option = document.createElement("option");
      option.value = String(index);
      option.textContent = recording.name;
      els.recordingSelect.appendChild(option);
    });

    if (recordings.length) {
      els.recordingSelect.value = "0";
      if (api.enabled && !recordings[0].data) loadServerRecording(recordings[0].name).catch(showError);
      else setCurrent(recordings[0].data);
    } else {
      showError(new Error("没有在所选文件夹中找到录制 JSON。"));
    }
  }

  function setCurrent(recording) {
    state.current = recording;
    state.playhead = 0;
    state.view = null;
    els.playhead.max = Math.max(0, recording.samples.length - 1);
    els.playhead.value = "0";
    els.emptyState.classList.add("hidden");
    render();
  }

  function render() {
    if (!state.current) return;
    renderSummary();
    renderDetails();
    renderExitList();
    renderSegments();
    drawRoute();
    drawLineChart(els.speedCanvas, state.current.samples.map(function (sample) {
      return { x: sample.timeSeconds, y: sample.speed2d };
    }), "#52c7a5", "速度");
    drawLineChart(els.distanceCanvas, state.current.samples.map(function (sample) {
      return { x: sample.timeSeconds, y: sample.odometer ? sample.odometer.distance2d : 0 };
    }), "#efb752", "里程");
  }

  function renderSummary() {
    var rec = state.current;
    var summary = rec.summary || {};
    var maxSpeed = rec.samples.reduce(function (max, sample) {
      return Math.max(max, sample.speed2d || 0);
    }, 0);
    var avgSpeed = summary.durationSeconds > 0 ? Number(summary.totalDistance2d || 0) / Number(summary.durationSeconds) : 0;
    var metrics = [
      ["采样点", formatInt(summary.sampleCount || rec.samples.length)],
      ["录制时长", formatSeconds(summary.durationSeconds)],
      ["水平里程", formatDistance(summary.totalDistance2d)],
      ["平均速度", formatSpeed(avgSpeed)],
      ["最高速度", formatSpeed(maxSpeed)]
    ];
    els.summaryGrid.innerHTML = metrics.map(function (metric) {
      return "<article class=\"metric\"><span>" + escapeHtml(metric[0]) + "</span><strong>" + escapeHtml(metric[1]) + "</strong></article>";
    }).join("");
  }

  function renderDetails() {
    var sample = state.current.samples[state.playhead] || state.current.samples[0];
    if (!sample) return;
    var pos = sample.position || {};
    var details = [
      ["Tick", sample.tick],
      ["时间", formatSeconds(sample.timeSeconds)],
      ["坐标", [pos.x, pos.y, pos.z].map(formatCoord).join(", ")],
      ["Yaw", formatNumber(sample.yaw, 2) + " deg"],
      ["速度", formatSpeed(sample.speed2d)],
      ["累计里程", formatDistance(sample.odometer && sample.odometer.distance2d)],
      ["出口", sample.exitRegionId || "-"]
    ];
    els.sampleDetails.innerHTML = details.map(function (item) {
      return "<dt>" + escapeHtml(item[0]) + "</dt><dd>" + escapeHtml(String(item[1])) + "</dd>";
    }).join("");
  }

  function renderExitList() {
    var regions = state.current.exitRegions || [];
    if (!regions.length) {
      els.exitList.innerHTML = "<div class=\"exit-item\"><span>没有出口区域数据</span></div>";
      return;
    }
    els.exitList.innerHTML = regions.map(function (region, index) {
      var hits = state.current.samples.filter(function (sample) {
        return sample.exitRegionId === region.id;
      }).length;
      return "<div class=\"exit-item\"><strong><i class=\"swatch\" style=\"background:" + colorFor(index) + "\"></i>" +
        escapeHtml(region.id) + "</strong><span>" + describeRegion(region) + "</span><span>" + hits + " 个采样点命中</span></div>";
    }).join("");
  }

  function renderSegments() {
    var segments = state.current.segments || [];
    els.segmentCount.textContent = segments.length + " 段";
    els.segmentTable.innerHTML = segments.map(function (segment) {
      return "<tr><td>" + escapeHtml(segment.id) + "</td><td>" + formatSeconds(segment.start.timeSeconds) + "</td><td>" +
        formatSeconds(segment.end.timeSeconds) + "</td><td>" + formatSeconds(segment.duration) + "</td><td>" +
        formatDistance(segment.distance) + "</td><td>" + segment.count + "</td></tr>";
    }).join("") || "<tr><td colspan=\"6\">这份录制没有命中出口区域。</td></tr>";
  }

  function drawRoute() {
    var canvas = els.routeCanvas;
    var ctx = prepareCanvas(canvas);
    var rec = state.current;
    var samples = rec.samples || [];
    if (!samples.length) return;

    var bounds = routeBounds(samples, rec.exitRegions);
    if (!state.view) state.view = makeView(bounds, canvas.width, canvas.height);
    else syncViewSize(state.view, canvas.width, canvas.height);
    var view = state.view;

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    drawJourneyMap(ctx, view);
    drawGrid(ctx, canvas, view);
    drawRegions(ctx, rec.exitRegions || [], view);
    drawPath(ctx, samples, view);
    drawPlayhead(ctx, samples[state.playhead], view);

    var b = bounds;
    els.mapSubtitle.textContent = "X " + formatCoord(b.minX) + " to " + formatCoord(b.maxX) + " / Z " + formatCoord(b.minZ) + " to " + formatCoord(b.maxZ);
  }

  function loadJourneyMapTiles(files) {
    var next = { day: [], night: [], topo: [] };
    files.filter(function (file) {
      return /\.png$/i.test(file.name);
    }).forEach(function (file) {
      var match = file.name.match(/^(-?\d+),(-?\d+)\.png$/i);
      if (!match) return;
      var layer = inferJourneyMapLayer(file);
      if (!next[layer]) return;
      var url = URL.createObjectURL(file);
      var image = new Image();
      var tile = {
        x: Number(match[1]),
        z: Number(match[2]),
        image: image,
        loaded: false,
        url: url
      };
      image.onload = function () {
        tile.loaded = true;
        if (els.showJourneyMap.checked) render();
      };
      image.src = url;
      next[layer].push(tile);
    });

    Object.keys(journeyMap.layers).forEach(function (layer) {
      journeyMap.layers[layer].forEach(function (tile) {
        URL.revokeObjectURL(tile.url);
      });
    });
    journeyMap.layers = next;
    journeyMap.loaded = Object.keys(next).some(function (layer) {
      return next[layer].length > 0;
    });
  }

  function inferJourneyMapLayer(file) {
    var path = (file.webkitRelativePath || file.name).replace(/\\/g, "/");
    var parts = path.split("/").map(function (part) {
      return part.toLowerCase();
    });
    if (parts.indexOf("day") !== -1) return "day";
    if (parts.indexOf("night") !== -1) return "night";
    if (parts.indexOf("topo") !== -1) return "topo";
    return "";
  }

  function drawJourneyMap(ctx, view) {
    if (!els.showJourneyMap.checked || !journeyMap.loaded) return;
    if (api.enabled) {
      drawServerJourneyMap(ctx, view);
      return;
    }
    var layer = els.journeyMapLayer.value || "day";
    var tiles = journeyMap.layers[layer] || [];
    if (!tiles.length) return;
    var visible = visibleWorldBounds(view);

    ctx.save();
    ctx.globalAlpha = 0.92;
    tiles.forEach(function (tile) {
      if (!tile.loaded) return;
      var x0 = tile.x * journeyMap.tileSize;
      var z0 = tile.z * journeyMap.tileSize;
      var x1 = x0 + journeyMap.tileSize;
      var z1 = z0 + journeyMap.tileSize;
      if (x1 < visible.minX || x0 > visible.maxX || z1 < visible.minZ || z0 > visible.maxZ) return;
      var topLeft = project({ x: x0, z: z0 }, view);
      var bottomRight = project({ x: x1, z: z1 }, view);
      ctx.drawImage(tile.image, topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
    });
    ctx.restore();
  }

  function drawServerJourneyMap(ctx, view) {
    var layer = els.journeyMapLayer.value || "day";
    var visible = visibleWorldBounds(view);
    var minTileX = Math.floor(visible.minX / journeyMap.tileSize);
    var maxTileX = Math.floor(visible.maxX / journeyMap.tileSize);
    var minTileZ = Math.floor(visible.minZ / journeyMap.tileSize);
    var maxTileZ = Math.floor(visible.maxZ / journeyMap.tileSize);
    var tileCount = (maxTileX - minTileX + 1) * (maxTileZ - minTileZ + 1);
    if (tileCount > api.maxVisibleTiles) return;

    ctx.save();
    ctx.globalAlpha = 0.92;
    for (var tileX = minTileX; tileX <= maxTileX; tileX++) {
      for (var tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
        var tile = getServerTile(layer, tileX, tileZ);
        if (!tile || !tile.loaded) continue;
        var x0 = tileX * journeyMap.tileSize;
        var z0 = tileZ * journeyMap.tileSize;
        var x1 = x0 + journeyMap.tileSize;
        var z1 = z0 + journeyMap.tileSize;
        var topLeft = project({ x: x0, z: z0 }, view);
        var bottomRight = project({ x: x1, z: z1 }, view);
        ctx.drawImage(tile.image, topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
      }
    }
    ctx.restore();
  }

  function getServerTile(layer, tileX, tileZ) {
    var key = layer + "/" + tileX + "," + tileZ;
    if (api.missingTiles[key]) return null;
    if (api.tileCache[key]) return api.tileCache[key];

    var image = new Image();
    if (api.baseUrl) image.crossOrigin = "anonymous";
    var tile = {
      image: image,
      loaded: false
    };
    image.onload = function () {
      tile.loaded = true;
      render();
    };
    image.onerror = function () {
      api.missingTiles[key] = true;
      delete api.tileCache[key];
    };
    image.src = api.baseUrl + "/tiles/" + encodeURIComponent(layer) + "/" + tileX + "," + tileZ + ".png";
    api.tileCache[key] = tile;
    return tile;
  }

  function prepareCanvas(canvas) {
    var rect = canvas.getBoundingClientRect();
    var ratio = window.devicePixelRatio || 1;
    var width = Math.max(1, Math.round(rect.width * ratio));
    var height = Math.max(1, Math.round(rect.height * ratio));
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width;
      canvas.height = height;
    }
    var ctx = canvas.getContext("2d");
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    return ctx;
  }

  function routeBounds(samples, regions) {
    var xs = samples.map(function (sample) { return sample.position.x; });
    var zs = samples.map(function (sample) { return sample.position.z; });
    (regions || []).forEach(function (region) {
      xs.push(region.min.x, region.max.x);
      zs.push(region.min.z, region.max.z);
    });
    return {
      minX: Math.min.apply(Math, xs),
      maxX: Math.max.apply(Math, xs),
      minZ: Math.min.apply(Math, zs),
      maxZ: Math.max.apply(Math, zs)
    };
  }

  function makeView(bounds, width, height) {
    var pad = Math.max(32, Math.min(width, height) * 0.08);
    var worldW = Math.max(1, bounds.maxX - bounds.minX);
    var worldH = Math.max(1, bounds.maxZ - bounds.minZ);
    var scale = Math.min((width - pad * 2) / worldW, (height - pad * 2) / worldH);
    return {
      bounds: bounds,
      pad: pad,
      scale: scale,
      baseScale: scale,
      minScale: Math.max(scale / 8, 0.02),
      maxScale: Math.max(scale * 256, scale + 1),
      width: width,
      height: height,
      offsetX: 0,
      offsetY: 0
    };
  }

  function syncViewSize(view, width, height) {
    if (view.width === width && view.height === height) return;
    view.offsetX += (width - view.width) / 2;
    view.offsetY += (height - view.height) / 2;
    view.width = width;
    view.height = height;
    view.pad = Math.max(32, Math.min(width, height) * 0.08);
  }

  function project(point, view) {
    return {
      x: view.pad + view.offsetX + (point.x - view.bounds.minX) * view.scale,
      y: view.pad + view.offsetY + (point.z - view.bounds.minZ) * view.scale
    };
  }

  function screenToWorld(point, view) {
    return {
      x: view.bounds.minX + (point.x - view.pad - view.offsetX) / view.scale,
      z: view.bounds.minZ + (point.y - view.pad - view.offsetY) / view.scale
    };
  }

  function visibleWorldBounds(view) {
    var a = screenToWorld({ x: 0, y: 0 }, view);
    var b = screenToWorld({ x: view.width, y: view.height }, view);
    return {
      minX: Math.min(a.x, b.x),
      maxX: Math.max(a.x, b.x),
      minZ: Math.min(a.z, b.z),
      maxZ: Math.max(a.z, b.z)
    };
  }

  function zoomMapAtCenter(factor) {
    if (!state.current) return;
    var canvas = els.routeCanvas;
    prepareCanvas(canvas);
    if (!state.view) {
      var bounds = routeBounds(state.current.samples || [], state.current.exitRegions || []);
      state.view = makeView(bounds, canvas.width, canvas.height);
    }
    zoomViewAt({ x: canvas.width / 2, y: canvas.height / 2 }, factor);
  }

  function zoomViewAt(point, factor) {
    var view = state.view;
    if (!view) return;
    var before = screenToWorld(point, view);
    var nextScale = clamp(view.scale * factor, view.minScale, view.maxScale);
    if (nextScale === view.scale) return;
    view.scale = nextScale;
    view.offsetX = point.x - view.pad - (before.x - view.bounds.minX) * view.scale;
    view.offsetY = point.y - view.pad - (before.z - view.bounds.minZ) * view.scale;
    render();
  }

  function handleMapWheel(event) {
    if (!state.current) return;
    event.preventDefault();
    var point = canvasPoint(event);
    var factor = event.deltaY < 0 ? 1.18 : 1 / 1.18;
    zoomViewAt(point, factor);
  }

  function handleMapPointerDown(event) {
    if (!state.current || event.button !== 0) return;
    var point = canvasPoint(event);
    dragState.active = true;
    dragState.pointerId = event.pointerId;
    dragState.lastX = point.x;
    dragState.lastY = point.y;
    els.routeCanvas.classList.add("dragging");
    els.routeCanvas.setPointerCapture(event.pointerId);
  }

  function handleMapPointerMove(event) {
    if (!dragState.active || dragState.pointerId !== event.pointerId || !state.view) return;
    var point = canvasPoint(event);
    state.view.offsetX += point.x - dragState.lastX;
    state.view.offsetY += point.y - dragState.lastY;
    dragState.lastX = point.x;
    dragState.lastY = point.y;
    render();
  }

  function handleMapPointerUp(event) {
    if (!dragState.active || dragState.pointerId !== event.pointerId) return;
    dragState.active = false;
    dragState.pointerId = null;
    els.routeCanvas.classList.remove("dragging");
    if (els.routeCanvas.hasPointerCapture(event.pointerId)) {
      els.routeCanvas.releasePointerCapture(event.pointerId);
    }
  }

  function canvasPoint(event) {
    var canvas = els.routeCanvas;
    var rect = canvas.getBoundingClientRect();
    return {
      x: (event.clientX - rect.left) * canvas.width / rect.width,
      y: (event.clientY - rect.top) * canvas.height / rect.height
    };
  }

  function drawGrid(ctx, canvas, view) {
    if (!els.showGrid.checked) return;
    ctx.save();
    ctx.strokeStyle = "rgba(154, 167, 182, 0.13)";
    ctx.lineWidth = 1;
    var step = niceStep(60 / view.scale);
    var visible = visibleWorldBounds(view);
    var startX = Math.floor(visible.minX / step) * step;
    var endX = Math.ceil(visible.maxX / step) * step;
    var startZ = Math.floor(visible.minZ / step) * step;
    var endZ = Math.ceil(visible.maxZ / step) * step;
    for (var x = startX; x <= endX; x += step) {
      var px = project({ x: x, z: view.bounds.minZ }, view).x;
      ctx.beginPath();
      ctx.moveTo(px, 0);
      ctx.lineTo(px, canvas.height);
      ctx.stroke();
    }
    for (var z = startZ; z <= endZ; z += step) {
      var py = project({ x: view.bounds.minX, z: z }, view).y;
      ctx.beginPath();
      ctx.moveTo(0, py);
      ctx.lineTo(canvas.width, py);
      ctx.stroke();
    }
    ctx.restore();
  }

  function drawRegions(ctx, regions, view) {
    regions.forEach(function (region, index) {
      var a = project({ x: region.min.x, z: region.min.z }, view);
      var b = project({ x: region.max.x, z: region.max.z }, view);
      var x = Math.min(a.x, b.x);
      var y = Math.min(a.y, b.y);
      var w = Math.max(4, Math.abs(b.x - a.x));
      var h = Math.max(4, Math.abs(b.y - a.y));
      ctx.save();
      ctx.fillStyle = hexToRgba(colorFor(index), 0.16);
      ctx.strokeStyle = colorFor(index);
      ctx.lineWidth = 2;
      ctx.fillRect(x, y, w, h);
      ctx.strokeRect(x, y, w, h);
      ctx.fillStyle = "#eff3f7";
      ctx.font = "13px Segoe UI, sans-serif";
      ctx.fillText(region.id, x + 7, y + 17);
      ctx.restore();
    });
  }

  function drawPath(ctx, samples, view) {
    var colorMode = els.colorMode.value;
    var maxSpeed = samples.reduce(function (max, sample) { return Math.max(max, sample.speed2d || 0); }, 1);
    ctx.save();
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    for (var i = 1; i < samples.length; i++) {
      var a = project(samples[i - 1].position, view);
      var b = project(samples[i].position, view);
      ctx.beginPath();
      ctx.moveTo(a.x, a.y);
      ctx.lineTo(b.x, b.y);
      ctx.strokeStyle = segmentColor(samples[i], i, samples.length, maxSpeed, colorMode);
      ctx.lineWidth = i <= state.playhead ? 4 : 2;
      ctx.globalAlpha = i <= state.playhead ? 0.95 : 0.38;
      ctx.stroke();
    }
    ctx.restore();
  }

  function drawPlayhead(ctx, sample, view) {
    if (!sample) return;
    var p = project(sample.position, view);
    ctx.save();
    ctx.fillStyle = "#ffffff";
    ctx.strokeStyle = "#111318";
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.arc(p.x, p.y, 7, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    if (els.showYaw.checked && Number.isFinite(sample.yaw)) {
      var rad = sample.yaw * Math.PI / 180;
      ctx.strokeStyle = "#efb752";
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(p.x, p.y);
      ctx.lineTo(p.x - Math.sin(rad) * 30, p.y + Math.cos(rad) * 30);
      ctx.stroke();
    }
    ctx.restore();
  }

  function drawLineChart(canvas, points, color) {
    var ctx = prepareCanvas(canvas);
    var width = canvas.width;
    var height = canvas.height;
    var pad = { left: 48, right: 18, top: 22, bottom: 34 };
    ctx.clearRect(0, 0, width, height);
    if (!points.length) return;
    var maxX = Math.max.apply(Math, points.map(function (p) { return p.x || 0; })) || 1;
    var maxY = Math.max.apply(Math, points.map(function (p) { return p.y || 0; })) || 1;

    ctx.save();
    ctx.strokeStyle = "rgba(154, 167, 182, 0.16)";
    ctx.lineWidth = 1;
    for (var i = 0; i <= 4; i++) {
      var y = pad.top + (height - pad.top - pad.bottom) * i / 4;
      ctx.beginPath();
      ctx.moveTo(pad.left, y);
      ctx.lineTo(width - pad.right, y);
      ctx.stroke();
    }

    ctx.strokeStyle = color;
    ctx.lineWidth = 3;
    ctx.beginPath();
    points.forEach(function (point, index) {
      var x = pad.left + (width - pad.left - pad.right) * ((point.x || 0) / maxX);
      var y = height - pad.bottom - (height - pad.top - pad.bottom) * ((point.y || 0) / maxY);
      if (index === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();

    var play = state.current.samples[state.playhead];
    if (play) {
      var px = pad.left + (width - pad.left - pad.right) * ((play.timeSeconds || 0) / maxX);
      ctx.strokeStyle = "#ffffff";
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(px, pad.top);
      ctx.lineTo(px, height - pad.bottom);
      ctx.stroke();
    }

    ctx.fillStyle = "#9aa7b6";
    ctx.font = "12px Segoe UI, sans-serif";
    ctx.fillText("0", 10, height - pad.bottom);
    ctx.fillText(formatNumber(maxY, 2), 10, pad.top + 4);
    ctx.fillText(formatSeconds(maxX), width - 92, height - 10);
    ctx.restore();
  }

  function buildSegments(samples) {
    var segments = [];
    var current = null;
    samples.forEach(function (sample) {
      var id = sample.exitRegionId;
      if (!id) {
        if (current) {
          current.end = samples[sample.index - 1] || current.start;
          segments.push(finishSegment(current));
          current = null;
        }
        return;
      }
      if (!current || current.id !== id) {
        if (current) segments.push(finishSegment(current));
        current = { id: id, start: sample, end: sample, count: 0 };
      }
      current.end = sample;
      current.count += 1;
    });
    if (current) segments.push(finishSegment(current));
    return segments;
  }

  function finishSegment(segment) {
    var startDistance = segment.start.odometer ? segment.start.odometer.distance2d : 0;
    var endDistance = segment.end.odometer ? segment.end.odometer.distance2d : startDistance;
    return Object.assign({}, segment, {
      duration: Number(segment.end.timeSeconds || 0) - Number(segment.start.timeSeconds || 0),
      distance: endDistance - startDistance
    });
  }

  function segmentColor(sample, index, total, maxSpeed, mode) {
    if (mode === "exit" && sample.exitRegionId) {
      var regionIndex = (state.current.exitRegions || []).findIndex(function (region) { return region.id === sample.exitRegionId; });
      return colorFor(Math.max(0, regionIndex));
    }
    if (mode === "time") {
      return mixColor("#64a8ff", "#efb752", index / Math.max(1, total - 1));
    }
    var t = Math.min(1, (sample.speed2d || 0) / Math.max(0.1, maxSpeed));
    return mixColor("#64a8ff", "#ff6b6b", t);
  }

  function colorFor(index) {
    return palette[index % palette.length];
  }

  function niceStep(value) {
    var pow = Math.pow(10, Math.floor(Math.log10(value)));
    var normalized = value / pow;
    var step = normalized < 2 ? 2 : normalized < 5 ? 5 : 10;
    return step * pow;
  }

  function clamp(value, min, max) {
    return Math.max(min, Math.min(max, value));
  }

  function describeRegion(region) {
    return "X " + region.min.x + " to " + region.max.x + ", Y " + region.min.y + " to " + region.max.y + ", Z " + region.min.z + " to " + region.max.z;
  }

  function formatSeconds(value) {
    return formatNumber(value || 0, 2) + " s";
  }

  function formatDistance(value) {
    return formatNumber(value || 0, 2) + " m";
  }

  function formatSpeed(value) {
    return formatNumber(value || 0, 2) + " m/s";
  }

  function formatCoord(value) {
    return formatNumber(value || 0, 2);
  }

  function formatInt(value) {
    return Number(value || 0).toLocaleString("zh-CN");
  }

  function formatNumber(value, digits) {
    return Number(value || 0).toLocaleString("zh-CN", {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits
    });
  }

  function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, function (char) {
      return ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#039;" })[char];
    });
  }

  function hexToRgba(hex, alpha) {
    var clean = hex.replace("#", "");
    var r = parseInt(clean.slice(0, 2), 16);
    var g = parseInt(clean.slice(2, 4), 16);
    var b = parseInt(clean.slice(4, 6), 16);
    return "rgba(" + r + "," + g + "," + b + "," + alpha + ")";
  }

  function mixColor(a, b, t) {
    var ca = parseHex(a);
    var cb = parseHex(b);
    var r = Math.round(ca.r + (cb.r - ca.r) * t);
    var g = Math.round(ca.g + (cb.g - ca.g) * t);
    var blue = Math.round(ca.b + (cb.b - ca.b) * t);
    return "rgb(" + r + "," + g + "," + blue + ")";
  }

  function parseHex(hex) {
    var clean = hex.replace("#", "");
    return {
      r: parseInt(clean.slice(0, 2), 16),
      g: parseInt(clean.slice(2, 4), 16),
      b: parseInt(clean.slice(4, 6), 16)
    };
  }

  function showError(error) {
    els.emptyState.classList.remove("hidden");
    els.emptyState.innerHTML = "<strong>读取失败</strong><span>" + escapeHtml(error.message || String(error)) + "</span>";
    console.error(error);
  }
})();
