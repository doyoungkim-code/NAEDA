$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$pythonExe = ".\.venv\Scripts\python.exe"

if (Test-Path $pythonExe) {
    $venvVersion = & $pythonExe -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')"
    if ($venvVersion -ne "3.11") {
        Write-Host "[INFO] Existing .venv uses Python $venvVersion. Recreating with Python 3.11..."
        Remove-Item -Recurse -Force .venv
    }
}

if (-not (Test-Path $pythonExe)) {
    Write-Host "[INFO] .venv not found. Creating virtual environment..."
    if (Get-Command py -ErrorAction SilentlyContinue) {
        & py -3.11 -m venv .venv
    }

    if (-not (Test-Path $pythonExe) -and (Get-Command python -ErrorAction SilentlyContinue)) {
        & python -m venv .venv
    }

    if (-not (Test-Path $pythonExe)) {
        throw "Python launcher was not found. Install Python 3.11+ and retry."
    }
}

try {
    & $pythonExe -c "import fastapi, uvicorn" | Out-Null
} catch {
    Write-Host "[INFO] Installing dependencies..."
    & $pythonExe -m pip install -r requirements.txt
}

if (-not (Test-Path ".env") -and (Test-Path ".env.example")) {
    Copy-Item ".env.example" ".env"
    Write-Host "[INFO] Created .env from .env.example"
}

& $pythonExe -m app.run
