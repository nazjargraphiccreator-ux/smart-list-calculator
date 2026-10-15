$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$project = Split-Path -Parent $PSScriptRoot
$gradleVer = '8.9'
$zipUrl = "https://services.gradle.org/distributions/gradle-$gradleVer-bin.zip"
$zipPath = Join-Path $env:TEMP "gradle-$gradleVer-bin.zip"
$extractRoot = 'D:\Android\gradle'

$jdk = Get-ChildItem 'C:\Program Files\Microsoft' -Directory -Filter 'jdk-17*' | Select-Object -First 1
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$($env:JAVA_HOME)\bin;$($env:Path)"

New-Item -ItemType Directory -Force -Path $extractRoot | Out-Null
if (-not (Test-Path "$extractRoot\gradle-$gradleVer\bin\gradle.bat")) {
    Write-Output "Downloading Gradle $gradleVer ..."
    Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath
    Expand-Archive -Path $zipPath -DestinationPath $extractRoot -Force
}

Push-Location $project
try {
    & "$extractRoot\gradle-$gradleVer\bin\gradle.bat" wrapper --gradle-version $gradleVer --distribution-type bin --no-daemon
} finally {
    Pop-Location
}
Write-Output 'Wrapper generated.'
