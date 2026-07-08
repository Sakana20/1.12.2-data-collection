# 星云工艺：统计

此 Mod 旨在解决录制 POV 时的数据统计不便问题。常规后期公路 POV 剪辑只能人工校对出口位置，使用此 Mod 可以实时记录路线、统计里程，并为出口区域打点。

## 基本信息

- modid: `nebulaestats`
- display name: `Nebulaecraft Statistics`
- Minecraft: `1.12.2`
- Forge: `14.23.5.2864`
- 命令前缀: `/ncs`

## 主要功能

- 每 tick 记录玩家位置 `x/y/z`、水平朝向 `yaw` 和真实经过时间 `timeSeconds`。
- 自动计算单 tick 位移 `dx/dy/dz`、水平移动距离、三维移动距离。
- 自动累加水平里程和三维里程。
- 使用出口设置工具选择出口区域。
- 录制样本中自动写入玩家当前所在的 `exitRegionId`。
- 支持中文出口 ID。
- 支持游戏内出口区域管理 GUI。
- 支持手持工具时显示出口区域线框和名称提示。

## 命令列表

### 录制命令

```text
/ncs start
/ncs stop
/ncs toggle
/ncs status
/ncs reset
```

- `/ncs start`：开始录制。
- `/ncs stop`：停止录制并导出 JSON。
- `/ncs toggle`：开始/停止录制切换。
- `/ncs status`：查看当前录制状态、样本数和累计里程。
- `/ncs reset`：清空当前录制缓存。

默认快捷键：

```text
F9 -> /ncs toggle
```

### 出口区域命令

```text
/ncs exit create <id>
/ncs exit remove <id>
/ncs exit list
/ncs exit info <id>
/ncs exit gui
```

- `/ncs exit create <id>`：用当前 `pos1/pos2` 选区创建出口区域。
- `/ncs exit remove <id>`：删除指定出口区域。
- `/ncs exit list`：列出所有出口区域。
- `/ncs exit info <id>`：查看指定出口区域的坐标信息。
- `/ncs exit gui`：打开出口区域管理 GUI。

## 出口设置工具

物品 ID：

```text
nebulaestats:exit_settings_tool
```

开发测试可用：

```text
/give @p nebulaestats:exit_settings_tool
```

操作方式：

- 左键方块：设置 `pos1`。
- 右键方块：设置 `pos2`。
- 右键空气：选区完整后打开出口区域管理 GUI。

手持工具时，已定义出口区域会显示绿色线框；当前 `pos1/pos2` 选区会显示更醒目的线框。准星指向出口区域时，会显示类似实体命名牌的出口名称提示。

## 出口 ID 规则

出口 ID 支持：

- 中文等 Unicode 字母。
- 数字。
- `_`
- `-`
- `:`
- `.`

示例：

```text
上虞
exit_1685
ramp:a
```

## 文件输出

停止录制后，轨迹 JSON 会保存到：

```text
run/nebulaestats/yyyy-MM-dd_HH-mm-ss.json
```

出口区域数据会保存到：

```text
run/nebulaestats/exit_regions.json
```

录制 JSON 中包含：

- `meta`：Minecraft 版本、Mod 信息、玩家信息、时间来源。
- `summary`：样本数、录制时长、累计水平里程、累计三维里程。
- `exitRegions`：录制结束时的出口区域定义。
- `samples`：每 tick 的位置、朝向、位移、累计里程和出口区域 ID。

## 数据可视化

仓库内置一个纯前端可视化页面：

```text
docs/visualizer/index.html
```

直接用浏览器打开后，可以选择单个录制 JSON，或选择整个 `nebulaestats` 文件夹。页面会绘制 X/Z 路线平面图、出口区域、当前采样点、速度曲线、累计里程曲线和出口命中片段。

推荐使用本地服务模式，它会实时读取录制文件和 JourneyMap 瓦片，只加载当前地图视口需要的图片：

```powershell
powershell -ExecutionPolicy Bypass -File tools\visualizer-server.ps1
```

启动后打开：

```text
http://localhost:8787/
```

也可以直接打开本地文件：

```text
docs/visualizer/index.html
```

本地 HTML 会自动尝试连接 `http://localhost:8787`，也可以点击“连接本地服务”手动重试。浏览器不能从 HTML 直接启动本机 PowerShell 进程，所以服务仍需要先用上面的脚本启动；连接后可在页面里点击“关闭服务”停止它。

默认读取：

```text
D:\Minecraft\NebulaeCraft\.minecraft\nebulaestats
D:\Minecraft\NebulaeCraft\.minecraft\journeymap\data\mp\NebulaeCraft\DIM0
```

如果 Minecraft 目录或 JourneyMap 世界目录不同，可以传参数：

```powershell
powershell -ExecutionPolicy Bypass -File tools\visualizer-server.ps1 -MinecraftDir "D:\Minecraft\NebulaeCraft\.minecraft" -JourneyMapWorld "mp\NebulaeCraft\DIM0"
```

如果需要叠加 JourneyMap/旅行地图数据，点击“选择旅行地图目录”，选择类似下面的目录之一：

```text
D:\Minecraft\NebulaeCraft\.minecraft\journeymap\data
D:\Minecraft\NebulaeCraft\.minecraft\journeymap\data\mp\NebulaeCraft\DIM0
D:\Minecraft\NebulaeCraft\.minecraft\journeymap\data\mp\NebulaeCraft\DIM0\day
```

旅行地图图层默认关闭，勾选“地图图层”后才会显示。当前支持 `day`、`night`、`topo` 三类瓦片。

路线图支持地图式浏览：

- 鼠标滚轮：以光标所在位置放大/缩小。
- 鼠标拖动：平移地图。
- `+` / `-`：以画布中心放大/缩小。
- `⤢` 或双击路线图：重新适配整条路线。

## 使用流程

1. 进入世界后获取出口设置工具。
2. 左键/右键方块选择出口区域的两个角点。
3. 右键空气打开出口区域管理 GUI。
4. 输入出口名称并点击添加。
5. 按 `F9` 或执行 `/ncs start` 开始录制。
6. 沿路线移动。
7. 按 `F9` 或执行 `/ncs stop` 停止录制并导出 JSON。

## 注意事项

- 当前项目仍处于测试阶段，不保证旧数据兼容。
- 录制只记录当前维度下的数据，尚未实现跨维度区域管理。
- 如果更换新版 jar，请从 `mods` 文件夹中移除旧的 `datacap` 或旧版 `nebulaestats` jar，避免重复加载。
