@echo off
REM ============================================================
REM Environmental Monitoring System - Smoke Test
REM Prerequisite: backend running at http://localhost:8080
REM Usage: double click or run in cmd: smoke-test.bat
REM ============================================================
setlocal
set BASE=http://localhost:8080
set TMPF=%TEMP%\smoke_resp.txt
set PASS=0
set FAIL=0

REM login (POST with JSON, -f: fail on HTTP >= 400)
echo {"adminCode":"admin","password":"123456"} > %TEMP%\smoke_login.json
curl.exe -s -f --max-time 10 -X POST %BASE%/api/auth/login -H "Content-Type: application/json" --data-binary @%TEMP%\smoke_login.json > %TMPF%
if %errorlevel%==0 ( echo [PASS] login api & set /a PASS+=1 ) else ( echo [FAIL] login api & type %TMPF% & set /a FAIL+=1 )

call :check "devices page" "/api/devices/page"
call :check "sensors dict" "/api/sensors"
call :check "stats overview" "/api/stats/overview"
call :check "alerts unhandled" "/api/alerts/unhandled"
call :check "quality score" "/api/stats/quality?deviceId=1"
call :check "history page" "/api/data/history?deviceId=1"

echo.
echo ============ RESULT ============
echo PASS: %PASS%   FAIL: %FAIL%
if %FAIL% GTR 0 (
    echo [FAIL] Some checks failed, please check backend log.
    exit /b 1
) else (
    echo [OK] All checks passed.
    exit /b 0
)

:check
set DESC=%~2
curl.exe -s -f --max-time 10 %BASE%%DESC% > %TMPF%
if %errorlevel%==0 (
    echo [PASS] %DESC%
    set /a PASS+=1
) else (
    echo [FAIL] %DESC%
    type %TMPF%
    set /a FAIL+=1
)
exit /b