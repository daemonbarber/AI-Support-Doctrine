param(
    [string]$GameDir = "D:\Program Files (x86)\Fractal Softworks\Starsector",
    [string]$JdkBin = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot\bin"
)
$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$core = Join-Path $GameDir "starsector-core"
$cp = @("starfarer.api.jar", "starfarer_obf.jar", "lwjgl.jar", "lwjgl_util.jar", "json.jar", "log4j-1.2.9.jar") |
    ForEach-Object { Join-Path $core $_ }

# LunaLib is an optional dependency at runtime, but needed to compile AISDLunaBridge.
$luna = Get-ChildItem (Join-Path $GameDir "mods") -Directory -Filter "LunaLib*" |
    ForEach-Object { Join-Path $_.FullName "jars\LunaLib.jar" } | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $luna) { throw "LunaLib.jar not found under $GameDir\mods" }
$cp += $luna

$buildDir = Join-Path $root "build"
$jarDir = Join-Path $root "jars"
if (Test-Path $buildDir) { Remove-Item -Recurse -Force $buildDir }
New-Item -ItemType Directory -Force $buildDir, $jarDir | Out-Null

$sources = Get-ChildItem -Recurse (Join-Path $root "src") -Filter *.java | ForEach-Object FullName
& (Join-Path $JdkBin "javac.exe") --release 17 -encoding UTF-8 -nowarn -cp ($cp -join ";") -d $buildDir @sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

$jar = Join-Path $jarDir "AISupportDoctrine.jar"
& (Join-Path $JdkBin "jar.exe") --create --file $jar -C $buildDir .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }
Write-Host "Built $jar"
