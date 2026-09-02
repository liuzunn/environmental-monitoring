# ============================================================
# 环境监测保护系统 · 端到端测试（E2E）
# 覆盖：认证/设备/数据/告警/阈值/统计/用户/指标字典/WebSocket/前端链路
# 前置：后端 8080 与前端 5173 已启动；测试数据自动清理
# 用法：pwsh ./e2e-test.ps1
# ============================================================

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$BASE = "http://localhost:8080/api"
$WEB = "http://localhost:5173"
$PASS = 0
$FAIL = 0
$FAILED = @()

function T($name, [scriptblock]$block) {
    try {
        $null = & $block
        Write-Host "  [PASS] $name" -ForegroundColor Green
        $script:PASS++
    } catch {
        Write-Host "  [FAIL] $name -> $($_.Exception.Message)" -ForegroundColor Red
        $script:FAIL++
        $script:FAILED += $name
    }
}

function PostJson($url, $body) {
    Invoke-RestMethod -Uri $url -Method Post -ContentType "application/json" -Body ($body | ConvertTo-Json -Depth 5)
}

function GetJson($url) {
    Invoke-RestMethod -Uri $url -Method Get
}

Write-Host ""
Write-Host "========== M1 认证模块 ==========" -ForegroundColor Cyan
T "登录成功返回 token" {
    $r = PostJson "$BASE/auth/login" @{ adminCode = "admin"; password = "123456" }
    if ($r.code -ne 200 -or [string]::IsNullOrEmpty($r.data.token)) { throw "code=$($r.code)" }
    $script:TOKEN = $r.data.token
}
T "错误密码被拒绝(401)" {
    try { PostJson "$BASE/auth/login" @{ adminCode = "admin"; password = "wrong" } | Out-Null; throw "应返回401" }
    catch { if (-not $_.Exception) { throw "未抛出异常" } }
}
T "登出接口" {
    $r = PostJson "$BASE/auth/logout" @{}
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}

Write-Host "========== M2 设备管理模块 ==========" -ForegroundColor Cyan
$testDevId = $null
T "新增设备(唯一编码)" {
    $r = PostJson "$BASE/devices" @{ deviceCode = "E2E-DEV-001"; deviceName = "E2E测试设备"; type = "AIR"; location = "端到端测试"; status = 1 }
    if ($r.code -ne 200) { throw "code=$($r.code)" }
    $dev = GetJson "$BASE/devices/page?keyword=E2E-DEV-001"
    if ($dev.data.total -lt 1) { throw "未查询到新设备" }
    $script:testDevId = $dev.data.records[0].id
}
T "重复编码被拒绝" {
    try { PostJson "$BASE/devices" @{ deviceCode = "E2E-DEV-001"; deviceName = "重复"; type = "AIR" } | Out-Null; throw "应拒绝重复编码" }
    catch { if (-not $_.Exception) { throw "未抛出异常" } }
}
T "修改设备" {
    $r = Invoke-RestMethod -Uri "$BASE/devices" -Method Put -ContentType "application/json" -Body (@{ id = $testDevId; deviceCode = "E2E-DEV-001"; deviceName = "E2E测试设备-改"; type = "AIR"; status = 1 } | ConvertTo-Json)
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}
T "在线设备统计" {
    $r = GetJson "$BASE/devices/online/count"
    if ($r.code -ne 200 -or $r.data.total -lt 1) { throw "统计异常" }
}

Write-Host "========== M3 监测数据模块 ==========" -ForegroundColor Cyan
T "数据上报(正常)" {
    $r = PostJson "$BASE/data/report" @{ deviceCode = "E2E-DEV-001"; items = @(@{ sensorCode = "TEMP"; value = 26.5 }, @{ sensorCode = "PM25"; value = 42.3 }); reportTime = "2026-08-25 11:00:00" }
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}
T "数据上报(未知设备回滚)" {
    try { PostJson "$BASE/data/report" @{ deviceCode = "E2E-NO-SUCH"; items = @(@{ sensorCode = "TEMP"; value = 1 }) } | Out-Null; throw "应报错" }
    catch { if (-not $_.Exception) { throw "未抛出异常" } }
}
T "设备最新数据" {
    $r = GetJson "$BASE/devices/$testDevId/latest"
    if ($r.code -ne 200 -or -not $r.data.values.TEMP) { throw "无最新数据" }
}
T "最新数据接口" {
    $r = GetJson "$BASE/data/latest?deviceId=$testDevId"
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}
T "历史数据分页" {
    $r = GetJson "$BASE/data/history?deviceId=$testDevId&page=1&size=10"
    if ($r.code -ne 200 -or $r.data.total -lt 1) { throw "无历史数据" }
}
T "趋势聚合(小时)" {
    $r = GetJson "$BASE/data/trend?deviceId=$testDevId&sensorCode=TEMP&interval=hour"
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}
T "CSV导出" {
    $resp = Invoke-WebRequest -Uri "$BASE/data/export?deviceId=$testDevId" -Method Get -UseBasicParsing
    if ($resp.StatusCode -ne 200 -or $resp.Content -notmatch "sensor_code") { throw "CSV内容异常" }
}

Write-Host "========== M4 告警模块 ==========" -ForegroundColor Cyan
$alertId = $null
T "超阈值上报触发ALARM" {
    $r = PostJson "$BASE/data/report" @{ deviceCode = "E2E-DEV-001"; items = @(@{ sensorCode = "PM25"; value = 260 }) }
    if ($r.code -ne 200) { throw "code=$($r.code)" }
    Start-Sleep -Milliseconds 500
    $a = GetJson "$BASE/alerts/page?page=1&size=5&deviceId=$testDevId"
    if ($a.data.total -lt 1) { throw "未产生告警" }
    $script:alertId = $a.data.records[0].id
}
T "30分钟防重复" {
    $null = PostJson "$BASE/data/report" @{ deviceCode = "E2E-DEV-001"; items = @(@{ sensorCode = "PM25"; value = 261 }) }
    Start-Sleep -Milliseconds 300
    $a = GetJson "$BASE/alerts/page?page=1&size=10&deviceId=$testDevId"
    if ($a.data.total -gt 2) { throw "重复告警" }
}
T "未处理告警统计" {
    $r = GetJson "$BASE/alerts/unhandled"
    if ($r.code -ne 200 -or $r.data.count -lt 1) { throw "未处理数异常" }
}
T "处理告警" {
    $r = Invoke-RestMethod -Uri "$BASE/alerts/$alertId/handle?handleUser=e2e" -Method Put
    if ($r.code -ne 200) { throw "code=$($r.code)" }
    $a = GetJson "$BASE/alerts/page?page=1&size=5&deviceId=$testDevId"
    if ($a.data.records[0].status -ne 1) { throw "状态未更新" }
}
T "近7天告警统计" {
    $r = GetJson "$BASE/alerts/stat"
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}

Write-Host "========== M5 阈值模块 ==========" -ForegroundColor Cyan
$thrId = $null
T "新增全局阈值" {
    $r = PostJson "$BASE/thresholds" @{ deviceId = $null; sensorCode = "TEMP"; warnMax = 30; alarmMax = 35; enabled = 1 }
    if ($r.code -ne 200) { throw "code=$($r.code)" }
    $list = GetJson "$BASE/thresholds"
    $item = $list.data | Where-Object { $_.sensorCode -eq "TEMP" -and $null -eq $_.deviceId } | Select-Object -First 1
    if (-not $item) { throw "未查询到新阈值" }
    $script:thrId = $item.id
}
T "修改阈值" {
    $r = Invoke-RestMethod -Uri "$BASE/thresholds" -Method Put -ContentType "application/json" -Body (@{ id = $thrId; sensorCode = "TEMP"; warnMax = 32; alarmMax = 38; enabled = 1 } | ConvertTo-Json)
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}
T "删除阈值" {
    $r = Invoke-RestMethod -Uri "$BASE/thresholds/$thrId" -Method Delete
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}

Write-Host "========== M6 统计模块 ==========" -ForegroundColor Cyan
T "总览统计字段完整" {
    $r = GetJson "$BASE/stats/overview"
    if ($r.code -ne 200) { throw "code=$($r.code)" }
    foreach ($k in @("totalDevices", "onlineDevices", "todayReports", "unhandledAlerts")) { if ($null -eq $r.data.$k) { throw "缺字段 $k" } }
}
T "环境质量评分" {
    $r = GetJson "$BASE/stats/quality?deviceId=1"
    if ($r.code -ne 200 -or $null -eq $r.data.overall) { throw "评分异常" }
}
T "设备上报排行" {
    $r = GetJson "$BASE/stats/device-ranking"
    if ($r.code -ne 200 -or @($r.data).Count -lt 1) { throw "排行异常" }
}

Write-Host "========== M7 用户模块 ==========" -ForegroundColor Cyan
$testUserId = $null
T "新增用户" {
    $r = PostJson "$BASE/users" @{ username = "e2e_user"; password = "123456"; nickname = "E2E用户"; role = "USER"; status = 1 }
    if ($r.code -ne 200) { throw "code=$($r.code)" }
    $u = GetJson "$BASE/users/page?keyword=e2e_user"
    if ($u.data.total -lt 1) { throw "未查询到新用户" }
    $script:testUserId = $u.data.records[0].id
}
T "重复用户名被拒绝" {
    try { PostJson "$BASE/users" @{ username = "e2e_user"; password = "x" } | Out-Null; throw "应拒绝" }
    catch { if (-not $_.Exception) { throw "未抛出异常" } }
}
T "修改用户" {
    $r = Invoke-RestMethod -Uri "$BASE/users" -Method Put -ContentType "application/json" -Body (@{ id = $testUserId; nickname = "E2E用户改" } | ConvertTo-Json)
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}
T "禁用用户" {
    $r = Invoke-RestMethod -Uri "$BASE/users/$testUserId/status?status=0" -Method Put
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}
T "删除用户" {
    $r = Invoke-RestMethod -Uri "$BASE/users/$testUserId" -Method Delete
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}

Write-Host "========== M8 指标字典模块 ==========" -ForegroundColor Cyan
T "指标字典列表(8个)" {
    $r = GetJson "$BASE/sensors"
    if ($r.code -ne 200 -or @($r.data).Count -ne 8) { throw "数量异常" }
}
T "按设备类型过滤" {
    $r = GetJson "$BASE/sensors?deviceType=AIR"
    if ($r.code -ne 200 -or @($r.data).Count -lt 2) { throw "过滤异常" }
}

Write-Host "========== M9 WebSocket 实时推送 ==========" -ForegroundColor Cyan
T "数据/告警实时广播" {
    $wsScript = @'
const ws = new WebSocket('ws://localhost:8080/ws/notify');
let got = 0;
ws.onopen = () => {
  fetch('http://localhost:8080/api/data/report', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ deviceCode: 'DEV-NSE-001', items: [{ sensorCode: 'NOISE', value: 88 }] })
  }).catch(() => {});
};
ws.onmessage = (e) => {
  const m = JSON.parse(e.data);
  if (m.type === 'data') got++;
  if (m.type === 'alert') { console.log('ALERT_RECEIVED'); ws.close(); process.exit(0); }
};
setTimeout(() => { process.exit(got > 0 ? 0 : 1); }, 12000);
'@
    Set-Content -Path ".\\e2e-ws.mjs" -Value $wsScript -Encoding UTF8
    node .\\e2e-ws.mjs 2>&1 | Out-Null
    Remove-Item .\\e2e-ws.mjs -ErrorAction SilentlyContinue
    if ($LASTEXITCODE -ne 0) { throw "WebSocket 广播未到达" }
}

Write-Host "========== M10 前端链路 ==========" -ForegroundColor Cyan
T "前端首页可访问" {
    $r = Invoke-WebRequest -Uri "$WEB/" -Method Get -TimeoutSec 10 -UseBasicParsing
    if ($r.StatusCode -ne 200) { throw "HTTP $($r.StatusCode)" }
}
T "前端代理访问后端API" {
    $r = GetJson "$WEB/api/stats/overview"
    if ($r.code -ne 200) { throw "code=$($r.code)" }
}
T "六个页面模块可加载" {
    foreach ($p in @("Dashboard.vue", "History.vue", "Devices.vue", "Alerts.vue", "Thresholds.vue", "Users.vue", "Login.vue")) {
        $r = Invoke-WebRequest -Uri "$WEB/src/views/$p" -Method Get -TimeoutSec 10 -UseBasicParsing
        if ($r.StatusCode -ne 200) { throw "$p HTTP $($r.StatusCode)" }
    }
}

Write-Host ""
Write-Host "========== 测试数据清理 ==========" -ForegroundColor Cyan
try {
    $mysql = "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe"
    # 数据库密码从环境变量读取（Git 发布安全要求；本机可先设置 $env:DB_PASSWORD="124102"）
    if (-not $env:DB_PASSWORD) { Write-Host "  [跳过清理] 未设置环境变量 DB_PASSWORD（e2e 清理需要数据库密码）" -ForegroundColor Yellow; return }
    $env:MYSQL_PWD = $env:DB_PASSWORD
    if ($testDevId) {
        & $mysql -uroot -e "USE nep; DELETE FROM monitor_data WHERE device_id = $testDevId; DELETE FROM alerts WHERE device_id = $testDevId; DELETE FROM devices WHERE id = $testDevId;" 2>&1 | Out-Null
        Write-Host "  已清理 E2E 设备及其数据"
    }
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
} catch { Write-Host "  清理警告: $($_.Exception.Message)" -ForegroundColor Yellow }

Write-Host ""
Write-Host "================ 测试汇总 ================" -ForegroundColor White
Write-Host "通过: $PASS    失败: $FAIL"
if ($FAIL -gt 0) {
    Write-Host "失败项:" -ForegroundColor Red
    $FAILED | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
} else {
    Write-Host "全部端到端测试通过!" -ForegroundColor Green
    exit 0
}