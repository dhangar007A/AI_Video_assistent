$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
Set-Location $PSScriptRoot

# Load environment variables from ../server/.env if present locally
$serverEnv = Join-Path $PSScriptRoot "..\server\.env"
if (Test-Path $serverEnv) {
    Get-Content $serverEnv | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $val = $matches[2].Trim()
            [System.Environment]::SetEnvironmentVariable($key, $val, 'Process')
        }
    }
}

& .\mvnw.cmd spring-boot:run
