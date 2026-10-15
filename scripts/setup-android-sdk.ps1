$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$sdkRoot = 'D:\Android\Sdk'
$zipUrl  = 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip'
$zipPath = Join-Path $env:TEMP 'cmdline-tools.zip'

# 1. Locate JDK 17
$jdk = Get-ChildItem 'C:\Program Files\Microsoft' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $jdk) { throw 'JDK 17 not found under C:\Program Files\Microsoft' }
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$($env:JAVA_HOME)\bin;$($env:Path)"
Write-Output "JAVA_HOME = $($env:JAVA_HOME)"
& java -version

# 2. Download + extract command-line tools
New-Item -ItemType Directory -Force -Path "$sdkRoot\cmdline-tools" | Out-Null
if (-not (Test-Path "$sdkRoot\cmdline-tools\latest\bin\sdkmanager.bat")) {
    Write-Output 'Downloading Android command-line tools...'
    Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath
    $tmpExtract = Join-Path $env:TEMP 'cmdline-tools-extract'
    if (Test-Path $tmpExtract) { Remove-Item -Recurse -Force $tmpExtract }
    Expand-Archive -Path $zipPath -DestinationPath $tmpExtract -Force
    if (Test-Path "$sdkRoot\cmdline-tools\latest") { Remove-Item -Recurse -Force "$sdkRoot\cmdline-tools\latest" }
    Move-Item -Path "$tmpExtract\cmdline-tools" -Destination "$sdkRoot\cmdline-tools\latest"
}

# 3. Accept licenses + install required packages
$sdkmanager = "$sdkRoot\cmdline-tools\latest\bin\sdkmanager.bat"
Write-Output 'Accepting licenses...'
(1..30 | ForEach-Object { 'y' }) | & $sdkmanager --sdk_root=$sdkRoot --licenses | Out-Null

Write-Output 'Installing platform-tools, platforms;android-35, build-tools;35.0.0 ...'
& $sdkmanager --sdk_root=$sdkRoot 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'

# 4. Persist env vars for the user
[Environment]::SetEnvironmentVariable('ANDROID_HOME', $sdkRoot, 'User')
[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk.FullName, 'User')
Write-Output 'DONE: Android SDK ready at' $sdkRoot
