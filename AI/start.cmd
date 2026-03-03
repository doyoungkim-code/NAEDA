@echo off
setlocal
cd /d "%~dp0"

set "PYTHON_EXE=.\.venv\Scripts\python.exe"
set "VENV_PY_VER="

if exist "%PYTHON_EXE%" (
  for /f %%v in ('"%PYTHON_EXE%" -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')"') do set "VENV_PY_VER=%%v"
  if not "%VENV_PY_VER%"=="3.11" (
    echo [INFO] Existing .venv uses Python %VENV_PY_VER%. Recreating with Python 3.11...
    rmdir /s /q .venv
  )
)

if not exist "%PYTHON_EXE%" (
  echo [INFO] .venv not found. Creating virtual environment...
  py -3.11 -m venv .venv >nul 2>nul
  if not exist "%PYTHON_EXE%" (
    where python >nul 2>nul
    if %errorlevel%==0 (
      python -m venv .venv || goto :error
    ) else (
      echo [ERROR] Python 3.11 was not found. Install Python 3.11+ and retry.
      exit /b 1
    )
  )
)

"%PYTHON_EXE%" -c "import fastapi, uvicorn" >nul 2>nul
if %errorlevel% neq 0 (
  echo [INFO] Installing dependencies...
  "%PYTHON_EXE%" -m pip install -r requirements.txt || goto :error
)

if not exist ".env" (
  if exist ".env.example" (
    copy /Y ".env.example" ".env" >nul
    echo [INFO] Created .env from .env.example
  )
)

"%PYTHON_EXE%" -m app.run
exit /b %errorlevel%

:error
echo [ERROR] Failed while preparing the environment.
exit /b 1
