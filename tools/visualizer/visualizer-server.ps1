param(
  [int]$Port = 8787,
  [string]$MapMinecraftDir = "D:\Minecraft\NebulaeCraft\.minecraft",
  [string]$DefaultSourceId = "nebulaecraft_dh",
  [string]$JourneyMapWorld = "mp\NebulaeCraft\DIM0"
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$WebRoot = $PSScriptRoot
$StatsSources = @(
  @{
    id = "nebulaecraft"
    label = "NebulaeCraft"
    minecraftDir = "D:\Minecraft\NebulaeCraft\.minecraft"
  },
  @{
    id = "nebulaecraft_dh"
    label = "NebulaeCraft DH"
    minecraftDir = "D:\Minecraft\NebulaeCraft_DH\.minecraft"
  }
)
$JourneyMapRoot = Join-Path $MapMinecraftDir "journeymap\data"
$JourneyMapDimRoot = Join-Path $JourneyMapRoot $JourneyMapWorld
$script:ShouldStop = $false

foreach ($source in $StatsSources) {
  $source.statsRoot = Join-Path $source.minecraftDir "nebulaestats"
}

function Get-StatsSource {
  param([string]$Id)

  foreach ($source in $StatsSources) {
    if ($source.id -eq $Id) {
      return $source
    }
  }
  return $null
}

if ($null -eq (Get-StatsSource -Id $DefaultSourceId)) {
  $DefaultSourceId = $StatsSources[0].id
}

$DefaultStatsSource = Get-StatsSource -Id $DefaultSourceId
$StatsRoot = $DefaultStatsSource.statsRoot

function Resolve-ContainedPath {
  param(
    [Parameter(Mandatory=$true)][string]$Root,
    [Parameter(Mandatory=$true)][string]$RelativePath
  )

  $rootFull = [System.IO.Path]::GetFullPath($Root)
  $candidate = [System.IO.Path]::GetFullPath((Join-Path $rootFull $RelativePath))
  if (-not $candidate.StartsWith($rootFull, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Path escapes configured root."
  }
  return $candidate
}

function Get-StatusText {
  param([int]$StatusCode)
  switch ($StatusCode) {
    200 { "OK" }
    400 { "Bad Request" }
    404 { "Not Found" }
    500 { "Internal Server Error" }
    default { "OK" }
  }
}

function Send-Bytes {
  param(
    [Parameter(Mandatory=$true)]$Stream,
    [Parameter(Mandatory=$true)][byte[]]$Bytes,
    [Parameter(Mandatory=$true)][string]$ContentType,
    [int]$StatusCode = 200,
    [string]$CacheControl = "no-store"
  )

  $statusText = Get-StatusText -StatusCode $StatusCode
  $header = "HTTP/1.1 $StatusCode $statusText`r`n" +
    "Content-Type: $ContentType`r`n" +
    "Content-Length: $($Bytes.Length)`r`n" +
    "Cache-Control: $CacheControl`r`n" +
    "Access-Control-Allow-Origin: *`r`n" +
    "Connection: close`r`n" +
    "`r`n"
  $headerBytes = [System.Text.Encoding]::ASCII.GetBytes($header)
  $Stream.Write($headerBytes, 0, $headerBytes.Length)
  $Stream.Write($Bytes, 0, $Bytes.Length)
}

function Send-Text {
  param(
    [Parameter(Mandatory=$true)]$Stream,
    [Parameter(Mandatory=$true)][string]$Text,
    [string]$ContentType = "text/plain; charset=utf-8",
    [int]$StatusCode = 200
  )

  $bytes = [System.Text.Encoding]::UTF8.GetBytes($Text)
  Send-Bytes -Stream $Stream -Bytes $bytes -ContentType $ContentType -StatusCode $StatusCode
}

function Send-Json {
  param(
    [Parameter(Mandatory=$true)]$Stream,
    [Parameter(Mandatory=$true)]$Value,
    [int]$Depth = 6
  )

  Send-Text -Stream $Stream -Text ($Value | ConvertTo-Json -Depth $Depth) -ContentType "application/json; charset=utf-8"
}

function Send-File {
  param(
    [Parameter(Mandatory=$true)]$Stream,
    [Parameter(Mandatory=$true)][string]$Path,
    [Parameter(Mandatory=$true)][string]$ContentType,
    [string]$CacheControl = "no-store"
  )

  if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    Send-Text -Stream $Stream -Text "Not found" -StatusCode 404
    return
  }

  $bytes = [System.IO.File]::ReadAllBytes($Path)
  Send-Bytes -Stream $Stream -Bytes $bytes -ContentType $ContentType -CacheControl $CacheControl
}

function Send-TextFileAsUtf8 {
  param(
    [Parameter(Mandatory=$true)]$Stream,
    [Parameter(Mandatory=$true)][string]$Path,
    [Parameter(Mandatory=$true)][string]$ContentType
  )

  if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    Send-Text -Stream $Stream -Text "Not found" -StatusCode 404
    return
  }

  $bytes = [System.IO.File]::ReadAllBytes($Path)
  $utf8 = [System.Text.UTF8Encoding]::new($false, $true)
  try {
    $text = $utf8.GetString($bytes)
  } catch {
    $text = [System.Text.Encoding]::Default.GetString($bytes)
  }
  Send-Text -Stream $Stream -Text $text -ContentType $ContentType
}

function Get-ContentType {
  param([string]$Path)
  switch ([System.IO.Path]::GetExtension($Path).ToLowerInvariant()) {
    ".html" { "text/html; charset=utf-8" }
    ".css" { "text/css; charset=utf-8" }
    ".js" { "text/javascript; charset=utf-8" }
    ".json" { "application/json; charset=utf-8" }
    ".png" { "image/png" }
    default { "application/octet-stream" }
  }
}

function Get-StatsSourceResponse {
  return @($StatsSources | ForEach-Object {
    @{
      id = $_.id
      label = $_.label
      minecraftDir = $_.minecraftDir
      statsRoot = $_.statsRoot
    }
  })
}

function Send-Recordings {
  param(
    [Parameter(Mandatory=$true)]$Stream,
    [Parameter(Mandatory=$true)]$Source
  )

  $files = @()
  if (Test-Path -LiteralPath $Source.statsRoot -PathType Container) {
    $files = Get-ChildItem -LiteralPath $Source.statsRoot -File -Filter "*.json" |
      Where-Object { $_.Name -ne "exit_regions.json" } |
      Sort-Object Name |
      ForEach-Object {
        @{
          name = $_.Name
          bytes = $_.Length
          lastWriteTime = $_.LastWriteTime.ToString("s")
        }
      }
  }
  Send-Json -Stream $Stream -Value @{
    sourceId = $Source.id
    recordings = @($files)
  }
}

function Send-Recording {
  param(
    [Parameter(Mandatory=$true)]$Stream,
    [Parameter(Mandatory=$true)]$Source,
    [Parameter(Mandatory=$true)][string]$Name
  )

  if ($Name -notmatch "^[^\\/]+\.json$" -or $Name -eq "exit_regions.json") {
    Send-Text -Stream $Stream -Text "Bad recording name" -StatusCode 400
    return
  }

  $file = Resolve-ContainedPath -Root $Source.statsRoot -RelativePath $Name
  Send-TextFileAsUtf8 -Stream $Stream -Path $file -ContentType "application/json; charset=utf-8"
}

function Send-ExitRegions {
  param(
    [Parameter(Mandatory=$true)]$Stream,
    [Parameter(Mandatory=$true)]$Source
  )

  $file = Join-Path $Source.statsRoot "exit_regions.json"
  if (Test-Path -LiteralPath $file -PathType Leaf) {
    Send-TextFileAsUtf8 -Stream $Stream -Path $file -ContentType "application/json; charset=utf-8"
  } else {
    Send-Json -Stream $Stream -Value @()
  }
}

function Read-RequestPath {
  param([Parameter(Mandatory=$true)]$Stream)

  $reader = [System.IO.StreamReader]::new($Stream, [System.Text.Encoding]::ASCII, $false, 1024, $true)
  $requestLine = $reader.ReadLine()
  if ([string]::IsNullOrWhiteSpace($requestLine)) {
    return $null
  }

  while ($true) {
    $line = $reader.ReadLine()
    if ($null -eq $line -or $line.Length -eq 0) {
      break
    }
  }

  $parts = $requestLine.Split(" ")
  if ($parts.Length -lt 2 -or $parts[0] -ne "GET") {
    return $null
  }

  $rawPath = $parts[1]
  $queryIndex = $rawPath.IndexOf("?")
  if ($queryIndex -ge 0) {
    $rawPath = $rawPath.Substring(0, $queryIndex)
  }
  return [System.Uri]::UnescapeDataString($rawPath)
}

function Handle-Request {
  param(
    [Parameter(Mandatory=$true)]$Stream,
    [Parameter(Mandatory=$true)][string]$Path
  )

  if ($Path -eq "/") {
    Send-File -Stream $Stream -Path (Join-Path $WebRoot "index.html") -ContentType "text/html; charset=utf-8"
  } elseif ($Path -eq "/api/shutdown") {
    Send-Json -Stream $Stream -Value @{ ok = $true; message = "Server shutting down." }
    $script:ShouldStop = $true
  } elseif ($Path -eq "/api/config") {
    Send-Json -Stream $Stream -Value @{
      statsRoot = $StatsRoot
      statsSources = Get-StatsSourceResponse
      activeStatsSourceId = $DefaultSourceId
      journeyMapRoot = $JourneyMapDimRoot
      tileSize = 512
      layers = @("day", "night", "topo")
    }
  } elseif ($Path -eq "/api/sources") {
    Send-Json -Stream $Stream -Value @{
      activeStatsSourceId = $DefaultSourceId
      sources = Get-StatsSourceResponse
    }
  } elseif ($Path -match "^/api/sources/([^/]+)/recordings$") {
    $source = Get-StatsSource -Id $Matches[1]
    if ($null -eq $source) {
      Send-Text -Stream $Stream -Text "Unknown stats source" -StatusCode 404
    } else {
      Send-Recordings -Stream $Stream -Source $source
    }
  } elseif ($Path -match "^/api/sources/([^/]+)/recordings/([^/]+\.json)$") {
    $source = Get-StatsSource -Id $Matches[1]
    if ($null -eq $source) {
      Send-Text -Stream $Stream -Text "Unknown stats source" -StatusCode 404
    } else {
      Send-Recording -Stream $Stream -Source $source -Name ([System.IO.Path]::GetFileName($Path))
    }
  } elseif ($Path -match "^/api/sources/([^/]+)/exit_regions$") {
    $source = Get-StatsSource -Id $Matches[1]
    if ($null -eq $source) {
      Send-Text -Stream $Stream -Text "Unknown stats source" -StatusCode 404
    } else {
      Send-ExitRegions -Stream $Stream -Source $source
    }
  } elseif ($Path -eq "/api/recordings") {
    Send-Recordings -Stream $Stream -Source $DefaultStatsSource
  } elseif ($Path -like "/api/recordings/*") {
    $name = [System.IO.Path]::GetFileName($Path)
    Send-Recording -Stream $Stream -Source $DefaultStatsSource -Name $name
  } elseif ($Path -eq "/api/exit_regions") {
    Send-ExitRegions -Stream $Stream -Source $DefaultStatsSource
  } elseif ($Path -match "^/tiles/(day|night|topo)/(-?\d+),(-?\d+)\.png$") {
    $layer = $Matches[1]
    $tileName = "$($Matches[2]),$($Matches[3]).png"
    $file = Resolve-ContainedPath -Root $JourneyMapDimRoot -RelativePath (Join-Path $layer $tileName)
    Send-File -Stream $Stream -Path $file -ContentType "image/png" -CacheControl "public, max-age=3600"
  } else {
    $relative = if ($Path.StartsWith("/tools/visualizer/")) {
      $Path.Substring("/tools/visualizer/".Length)
    } elseif ($Path.StartsWith("/docs/visualizer/")) {
      $Path.Substring("/docs/visualizer/".Length)
    } else {
      $Path.TrimStart("/")
    }

    if ([string]::IsNullOrWhiteSpace($relative)) {
      $relative = "index.html"
    }

    $file = Resolve-ContainedPath -Root $WebRoot -RelativePath $relative
    Send-File -Stream $Stream -Path $file -ContentType (Get-ContentType -Path $file)
  }
}

$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
$listener.Start()

Write-Host "NebulaeStats visualizer server"
Write-Host "URL:              http://localhost:$Port/"
Write-Host "Web root:         $WebRoot"
Write-Host "Default source:   $($DefaultStatsSource.label) -> $StatsRoot"
Write-Host "Stats sources:"
foreach ($source in $StatsSources) {
  Write-Host "  $($source.id): $($source.statsRoot)"
}
Write-Host "JourneyMap root:  $JourneyMapDimRoot"
Write-Host "Press Ctrl+C to stop."

try {
  while (-not $script:ShouldStop) {
    $client = $listener.AcceptTcpClient()
    try {
      $stream = $client.GetStream()
      $path = Read-RequestPath -Stream $stream
      if ($null -eq $path) {
        Send-Text -Stream $stream -Text "Bad request" -StatusCode 400
      } else {
        try {
          Handle-Request -Stream $stream -Path $path
        } catch {
          Send-Text -Stream $stream -Text $_.Exception.Message -StatusCode 500
        }
      }
    } finally {
      $client.Close()
    }
  }
} finally {
  $listener.Stop()
}
