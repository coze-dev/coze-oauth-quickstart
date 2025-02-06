# PowerShell script to run gradle project
$CurrentPath = $PSScriptRoot
Start-Process -FilePath "cmd.exe" -ArgumentList "/c pushd `"$CurrentPath`" && call gradlew.bat run && popd" -Wait -NoNewWindow
