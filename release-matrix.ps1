<#
.SYNOPSIS
  Builds every mod (or one) against each supported Minecraft series and collects the jars into
  releases/<mod>/.

.DESCRIPTION
  One jar is cut per Minecraft series (26.1, 26.2, 26.3, ...). For each series the newest game
  version in that series is compiled against (26.1 -> 26.1.2, a not-yet-released series -> its
  latest RC/snapshot), and the matching fabric-api build is looked up from Fabric's maven metadata.
  minecraft_version is overridden per run via Gradle -P properties, so the tracked gradle.properties
  (the everyday dev target) is never modified. The build itself derives the "+mc<series>" jar suffix,
  the fabric.mod.json minecraft range and the src/versions/<series>/ overlay from that one property.

.PARAMETER Mod
  Folder name of a single mod to build. Omit to build all mods.

.PARAMETER Versions
  Exact Minecraft versions to build against, one per series. Defaults to the supported set.

.EXAMPLE
  .\release-matrix.ps1
  .\release-matrix.ps1 -Mod cascade
  .\release-matrix.ps1 -Mod cascade -Versions 26.2, 26.3-rc-3
#>
param(
    [string]$Mod,
    [string[]]$Versions = @("26.1.2", "26.2", "26.3-rc-3")
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

$mods = if ($Mod) { @($Mod) } else {
    Get-ChildItem $root -Directory | Where-Object { Test-Path (Join-Path $_.FullName "build.gradle") } | ForEach-Object Name
}
foreach ($m in $mods) {
    if (-not (Test-Path (Join-Path $root "$m\build.gradle"))) { throw "No such mod folder: $m" }
}

Write-Host "Fetching fabric-api version list..." -ForegroundColor DarkGray
[xml]$metadata = Invoke-RestMethod "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml"
$allFabricApiVersions = $metadata.metadata.versioning.versions.version

$results = @()

foreach ($mcVersion in $Versions) {
    # 26.1.2 -> 26.1, 26.3-rc-3 -> 26.3. fabric-api tags its builds with the series or an exact
    # version inside it (+26.1.2, +26.3), so match either.
    $series = ($mcVersion -replace '^(\d+\.\d+).*$', '$1')
    $fabricApiVersion = $allFabricApiVersions |
        Where-Object { $_ -match ('\+' + [regex]::Escape($series) + '(\.\d+)?$') } | Select-Object -Last 1
    if (-not $fabricApiVersion) {
        Write-Warning "No fabric-api build published for $mcVersion - skipping"
        foreach ($m in $mods) { $results += [pscustomobject]@{ Mod = $m; Minecraft = $mcVersion; Status = "no fabric-api build" } }
        continue
    }

    foreach ($m in $mods) {
        Write-Host ""
        Write-Host "=== $m : Minecraft $mcVersion (series $series, fabric-api $fabricApiVersion) ===" -ForegroundColor Cyan
        $modDir = Join-Path $root $m
        $outDir = Join-Path $root "releases\$m"
        New-Item -ItemType Directory -Force -Path $outDir | Out-Null

        Push-Location $modDir
        try {
            # Windows PowerShell 5.1 wraps every native stderr line (the JVM prints warnings there) in an
            # ErrorRecord, which $ErrorActionPreference = Stop would turn into a terminating error. Relax it
            # for this one call and flatten everything to plain strings; $LASTEXITCODE is what matters.
            $prevEap = $ErrorActionPreference
            $ErrorActionPreference = "Continue"
            $buildLog = & .\gradlew.bat clean build "-Pminecraft_version=$mcVersion" "-Pfabric_api_version=$fabricApiVersion" --console=plain 2>&1 |
                ForEach-Object { "$_" }
            $ErrorActionPreference = $prevEap

            if ($LASTEXITCODE -ne 0) {
                Write-Host "  FAILED" -ForegroundColor Red
                $failLog = Join-Path $outDir "build-$mcVersion.log"
                $buildLog | Out-File $failLog -Encoding utf8
                Write-Host "    full log: $failLog" -ForegroundColor Red
                $buildLog | Select-String -Pattern "error:|FAILURE|What went wrong" | Select-Object -First 8 |
                    ForEach-Object { Write-Host "    $_" -ForegroundColor Red }
                $results += [pscustomobject]@{ Mod = $m; Minecraft = $mcVersion; Status = "build failed" }
                continue
            }

            # The jar is already named <mod>-<mod_version>+mc<series>.jar by the build.
            $jar = Get-ChildItem (Join-Path $modDir "build\libs\*.jar") |
                Where-Object { $_.Name -notlike "*-sources.jar" } | Select-Object -First 1
            Copy-Item $jar.FullName (Join-Path $outDir $jar.Name) -Force
            Write-Host "  -> releases\$m\$($jar.Name)" -ForegroundColor Green
            $results += [pscustomobject]@{ Mod = $m; Minecraft = $mcVersion; Status = "ok"; Jar = $jar.Name }
        } finally {
            Pop-Location
        }
    }
}

Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize
if ($results | Where-Object { $_.Status -ne "ok" }) { exit 1 }
