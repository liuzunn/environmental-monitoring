<template>
  <div class="big-screen" data-theme="dark">
    <!-- 顶栏统计：点击跳转对应功能页 -->
    <div class="stat-row">
      <div
        v-for="s in stats"
        :key="s.label"
        class="stat-card"
        :style="{ '--accent': s.color }"
        @click="go(s.to)"
      >
        <div class="stat-glow" :style="{ background: s.color }"></div>
        <div class="stat-icon" :style="{ background: s.color }">
          <el-icon :size="15" color="#fff"><component :is="s.icon" /></el-icon>
        </div>
        <div class="stat-value tabular-nums" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="stat-label">
          {{ s.label }}
          <span class="stat-link">
            查看
            <el-icon :size="12"><ArrowRight /></el-icon>
          </span>
        </div>
      </div>
    </div>

    <!-- 设备切换 -->
    <div class="device-bar">
      <span class="bar-label">监测点位</span>
      <el-radio-group v-model="activeDeviceId" size="large">
        <el-radio-button v-for="d in devices" :key="d.id" :value="d.id">
          {{ d.deviceName }}
        </el-radio-button>
      </el-radio-group>
      <el-tag v-if="activeDevice" :type="activeDevice.status === 1 ? 'success' : 'info'" round effect="dark" class="status-tag">
        <span class="tag-dot" :class="activeDevice.status === 1 ? 'online' : 'offline'"></span>
        {{ activeDevice.status === 1 ? '在线' : '离线' }}
      </el-tag>
      <span class="device-bar-link" @click="go('/devices')">
        管理设备
        <el-icon :size="12"><ArrowRight /></el-icon>
      </span>
    </div>

    <div class="main-row">
      <!-- 左侧：Gauge 仪表盘 -->
      <div class="panel gauge-panel">
        <div class="panel-title">
          <span class="title-bar"></span>实时指标
          <span class="panel-link" @click="go({ path: '/history', query: { deviceId: activeDeviceId } })">
            历史详情
            <el-icon :size="12"><ArrowRight /></el-icon>
          </span>
        </div>
        <div class="gauge-grid">
          <div v-for="s in activeSensors" :key="s.sensorCode" class="gauge-item">
            <div :ref="(el) => setGaugeRef(s.sensorCode, el)" class="gauge-box"></div>
            <div class="gauge-value">
              <span :class="{ over: isOver(s.sensorCode) }">{{ fmtValue(s.sensorCode) }}</span>
              <span class="gauge-unit">{{ s.unit }}</span>
            </div>
            <div class="gauge-name">{{ s.sensorName }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧：实时曲线 -->
      <div class="panel trend-panel">
        <div class="panel-title">
          <span class="title-bar"></span>实时趋势（最近 40 个数据点 · 平滑曲线，5 秒刷新）
          <span class="panel-link" @click="go({ path: '/history', query: { deviceId: activeDeviceId } })">
            趋势分析
            <el-icon :size="12"><ArrowRight /></el-icon>
          </span>
        </div>
        <div ref="trendChartRef" class="trend-chart"></div>
      </div>
    </div>

    <div class="bottom-row">
      <!-- 设备列表：最新值 + 超标高亮 -->
      <div class="panel device-panel">
        <div class="panel-title">
          <span class="title-bar"></span>设备最新数据
          <span class="panel-link" @click="go('/devices')">
            设备管理
            <el-icon :size="12"><ArrowRight /></el-icon>
          </span>
        </div>
        <div class="device-grid">
          <div
            v-for="d in devices"
            :key="d.id"
            class="device-card"
            :class="{ active: d.id === activeDeviceId }"
            @click="activeDeviceId = d.id"
          >
            <div class="dev-head">
              <span class="dev-name">{{ d.deviceName }}</span>
              <span class="dev-dot" :class="d.status === 1 ? 'online' : 'offline'"></span>
            </div>
            <div class="dev-code">{{ d.deviceCode }} · {{ typeName(d.type) }}</div>
            <div class="dev-values">
              <div v-for="(v, code) in latestMap[d.id] || {}" :key="code" class="dev-val">
                <span class="dev-val-code">{{ code }}</span>
                <span class="dev-val-num tabular-nums" :class="{ over: isOverFor(d.id, code) }">{{ v }}</span>
              </div>
            </div>
            <div class="dev-link" @click.stop="go({ path: '/history', query: { deviceId: d.id } })">
              查看历史数据
              <el-icon :size="11"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 告警滚动 -->
      <div class="panel alert-panel">
        <div class="panel-title">
          <span class="title-bar"></span>最新告警
          <el-tag v-if="unhandled.count > 0" type="danger" round effect="dark" size="small" class="alert-count">
            {{ unhandled.count }} 未处理
          </el-tag>
          <span class="panel-link" @click="go('/alerts')">
            告警中心
            <el-icon :size="12"><ArrowRight /></el-icon>
          </span>
        </div>
        <div v-if="unhandled.latest && unhandled.latest.length" class="alert-list">
          <div
            v-for="a in unhandled.latest"
            :key="a.id"
            class="alert-item"
            :class="a.level === 'ALARM' ? 'alarm' : 'warn'"
            @click="go('/alerts')"
          >
            <span class="alert-level" :class="a.level === 'ALARM' ? 'alarm' : 'warn'">
              {{ a.level === 'ALARM' ? '报警' : '预警' }}
            </span>
            <span class="alert-msg">{{ a.message }}</span>
            <span class="alert-time">{{ fmtTime(a.createTime) }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无告警" :image-size="60" />
      </div>

      <!-- 近 7 天告警趋势 -->
      <div class="panel alert-trend-panel">
        <div class="panel-title">
          <span class="title-bar"></span>近 7 天告警趋势
          <span class="panel-link" @click="go('/alerts')">
            全部告警
            <el-icon :size="12"><ArrowRight /></el-icon>
          </span>
        </div>
        <div ref="alertTrendChartRef" class="alert-trend-chart"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getDevicesPage, getDeviceLatest, getOverview, getUnhandled, getSensors, getHistory, getAlertsStat } from '@/api'
import { connectWS, onWSMessage } from '@/utils/ws'

const router = useRouter()

/** 跳转到功能页（query 参数让目标页预筛选） */
function go(to) {
  router.push(to)
}

const devices = ref([])
const sensors = ref([])
const activeDeviceId = ref(null)
const latestMap = ref({})       // { deviceId: { sensorCode: value } }
const standards = ref({})       // { sensorCode: standardMax }
const unhandled = ref({ count: 0, latest: [] })
const stats = ref([
  { label: '设备总数', value: 0, color: '#007AFF', icon: 'Monitor', to: '/devices' },
  { label: '在线设备', value: 0, color: '#34C759', icon: 'Connection', to: { path: '/devices', query: { status: 1 } } },
  { label: '今日上报', value: 0, color: '#5AC8FA', icon: 'DataLine', to: '/history' },
  { label: '未处理告警', value: 0, color: '#FF3B30', icon: 'Bell', to: '/alerts' }
])

const gaugeRefs = {}
let offWS = null
const trendChartRef = ref()
const alertTrendChartRef = ref()
let trendChart = null
let alertTrendChart = null
let gaugeCharts = {}
let timers = []

const activeDevice = computed(() => devices.value.find(d => d.id === activeDeviceId.value) || null)
const activeSensors = computed(() => {
  if (!activeDevice.value) return []
  return sensors.value.filter(s => !s.deviceType || s.deviceType === activeDevice.value.type)
})

function setGaugeRef(code, el) {
  if (el) gaugeRefs[code] = el
}

function typeName(t) {
  return { AIR: '空气', WATER: '水质', NOISE: '噪声' }[t] || t
}

function fmtValue(code) {
  const v = latestMap.value[activeDeviceId.value]?.[code]
  return v === undefined ? '--' : v
}

function isOver(code) {
  return isOverFor(activeDeviceId.value, code)
}

function isOverFor(deviceId, code) {
  const v = latestMap.value[deviceId]?.[code]
  const std = standards.value[code]
  return v !== undefined && std && Number(v) > Number(std)
}

function fmtTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

async function loadDevices() {
  try {
    const d = await getDevicesPage({ page: 1, size: 100 })
    devices.value = d.records || []
    if (devices.value.length && !activeDeviceId.value) {
      activeDeviceId.value = devices.value[0].id
    }
  } catch (e) { /* 忽略 */ }
}

async function loadSensors() {
  try {
    const list = await getSensors()
    sensors.value = list || []
    const std = {}
    for (const s of list) std[s.sensorCode] = s.standardMax
    standards.value = std
  } catch (e) { /* 忽略 */ }
}

async function loadLatest() {
  for (const d of devices.value) {
    try {
      const data = await getDeviceLatest(d.id)
      const values = data.values || {}
      const map = {}
      for (const [code, item] of Object.entries(values)) {
        map[code] = item.value
      }
      latestMap.value = { ...latestMap.value, [d.id]: map }
    } catch (e) { /* 忽略 */ }
  }
  updateGauges()
  loadTrend()
}

async function loadOverview() {
  try {
    const o = await getOverview()
    stats.value[0].value = o.totalDevices
    stats.value[1].value = o.onlineDevices
    stats.value[2].value = o.todayReports
    stats.value[3].value = o.unhandledAlerts
  } catch (e) { /* 忽略 */ }
}

async function loadUnhandled() {
  try {
    unhandled.value = await getUnhandled()
  } catch (e) { /* 忽略 */ }
}

// ---------- 近 7 天告警趋势 ----------
async function loadAlertTrend() {
  if (!alertTrendChartRef.value) return
  if (!alertTrendChart) alertTrendChart = echarts.init(alertTrendChartRef.value)
  let rows = []
  try {
    rows = (await getAlertsStat()) || []
  } catch (e) { /* 忽略 */ }
  const days = [...new Set(rows.map(r => r.day))].sort()
  const alarm = days.map(d => Number(rows.find(r => r.day === d && r.level === 'ALARM')?.cnt || 0))
  const warn = days.map(d => Number(rows.find(r => r.day === d && r.level === 'WARN')?.cnt || 0))
  alertTrendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#2C2C2E',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 }
    },
    legend: { data: ['报警', '预警'], textStyle: { color: 'rgba(255,255,255,0.6)', fontSize: 11 }, top: 0, icon: 'roundRect', itemWidth: 8, itemHeight: 8 },
    grid: { left: 28, right: 8, top: 30, bottom: 24 },
    xAxis: {
      type: 'category',
      data: days.map(d => String(d).slice(5)),
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.15)' } },
      axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
      axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 10 }
    },
    series: [
      { name: '报警', type: 'bar', data: alarm, barWidth: 8, itemStyle: { color: '#FF453A', borderRadius: [3, 3, 0, 0] } },
      { name: '预警', type: 'bar', data: warn, barWidth: 8, barGap: '30%', itemStyle: { color: '#FF9F0A', borderRadius: [3, 3, 0, 0] } }
    ]
  })
}

// ---------- ECharts ----------
function updateGauges() {
  if (!activeDevice.value) return
  nextTick(() => {
    for (const s of activeSensors.value) {
      const el = gaugeRefs[s.sensorCode]
      if (!el) continue
      const std = standards.value[s.sensorCode]
      const max = Number(s.maxRange || (std ? std * 2 : 100))
      if (!gaugeCharts[s.sensorCode]) {
        gaugeCharts[s.sensorCode] = echarts.init(el)
      }
      const v = Number(latestMap.value[activeDeviceId.value]?.[s.sensorCode] ?? 0)
      const over = std && v > Number(std)
      gaugeCharts[s.sensorCode].setOption({
        series: [{
          type: 'gauge',
          startAngle: 210, endAngle: -30,
          min: 0, max,
          progress: { show: true, width: 8, itemStyle: { color: over ? '#FF3B30' : (std ? '#007AFF' : '#34C759') } },
          axisLine: { lineStyle: { width: 8, color: [[1, 'rgba(255,255,255,0.08)']] } },
          axisTick: { show: false },
          splitLine: { show: false },
          axisLabel: { show: false },
          pointer: { show: false },
          detail: { show: false },
          data: [{ value: v }]
        }]
      })
    }
  })
}

async function loadTrend() {
  if (!activeDevice.value || !trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  const codes = activeSensors.value.map(s => s.sensorCode)
  const series = []
  const legend = []
  const xAxis = []
  for (const code of codes) {
    try {
      const rows = await getHistory({ deviceId: activeDeviceId.value, sensorCode: code, page: 1, size: 40 })
      const points = (rows.records || []).slice().reverse()
      // 移动平均平滑：抹平上报尖刺，避免曲线直上直下
      const smoothed = smoothValues(points, 5)
      legend.push(code)
      series.push({
        name: code,
        type: 'line',
        smooth: 0.6,
        showSymbol: false,
        lineStyle: { width: 2.5 },
        itemStyle: { color: colorOf(code) },
        areaStyle: { opacity: 0.08, color: colorOf(code) },
        data: smoothed.map(p => [String(p.reportTime).replace('T', ' ').slice(5, 16), Number(p.value)])
      })
      if (xAxis.length === 0) xAxis.push(...smoothed.map(p => String(p.reportTime).replace('T', ' ').slice(5, 16)))
    } catch (e) { /* 忽略 */ }
  }
  trendChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#2C2C2E', borderWidth: 0, textStyle: { color: '#fff' } },
    legend: { data: legend, textStyle: { color: 'rgba(255,255,255,0.6)', fontSize: 12 }, top: 0 },
    grid: { left: 40, right: 16, top: 36, bottom: 24 },
    xAxis: {
      type: 'category',
      data: xAxis,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.15)' } },
      axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
      axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 11 }
    },
    series
  })
}

/** 移动平均平滑（居中窗口）：平滑后曲线更平缓，保留整体趋势 */
function smoothValues(rows, window = 5) {
  const half = Math.floor(window / 2)
  const n = rows.length
  if (n < 3) return rows
  return rows.map((r, i) => {
    let sum = 0
    let cnt = 0
    for (let j = Math.max(0, i - half); j <= Math.min(n - 1, i + half); j++) {
      sum += Number(rows[j].value)
      cnt++
    }
    return { ...r, value: Number((sum / cnt).toFixed(2)) }
  })
}

function colorOf(code) {
  const palette = { TEMP: '#FF9F0A', HUMI: '#5AC8FA', PM25: '#FF453A', CO2: '#AF52DE', PH: '#30D158', TURBIDITY: '#64D2FF', DO: '#0A84FF', NOISE: '#FFD60A' }
  return palette[code] || '#007AFF'
}

// ---------- 生命周期 ----------
onMounted(async () => {
  await Promise.all([loadDevices(), loadSensors()])
  await Promise.all([loadLatest(), loadOverview(), loadUnhandled(), loadAlertTrend()])
  timers = [
    setInterval(loadOverview, 30000),
    setInterval(loadUnhandled, 30000),
    setInterval(loadAlertTrend, 60000)
  ]
  window.addEventListener('resize', onResize)

  // WebSocket 实时推送：数据到达即时更新（替代 5 秒轮询）
  connectWS()
  offWS = onWSMessage(msg => {
    if (msg.type === 'data') {
      const id = msg.deviceId
      latestMap.value = {
        ...latestMap.value,
        [id]: { ...(latestMap.value[id] || {}), [msg.sensorCode]: msg.value }
      }
      updateGauges()
      loadTrend()
    } else if (msg.type === 'alert') {
      loadUnhandled()
    }
  })
})

function onResize() {
  trendChart?.resize()
  alertTrendChart?.resize()
  for (const c of Object.values(gaugeCharts)) c?.resize()
}

onUnmounted(() => {
  timers.forEach(clearInterval)
  window.removeEventListener('resize', onResize)
  if (offWS) offWS()
  trendChart?.dispose()
  alertTrendChart?.dispose()
  for (const c of Object.values(gaugeCharts)) c?.dispose()
})
</script>

<style scoped>
.big-screen {
  min-height: 100%;
  background:
    radial-gradient(ellipse 80% 50% at 50% -10%, rgba(0, 122, 255, 0.08), transparent),
    #000;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: rgba(255, 255, 255, 0.9);
}

/* ---------- 统计卡片 ---------- */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #1F1F22, #1C1C1E);
  border-radius: 16px;
  padding: 20px 24px;
  border: 0.5px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
  transition: transform var(--dur-base) var(--ease-out), border-color var(--dur-base) var(--ease-out);
  cursor: pointer;
}
.stat-card:hover {
  transform: translateY(-2px);
  border-color: var(--accent);
}

.stat-glow {
  position: absolute;
  top: -30px;
  right: -30px;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  opacity: 0.14;
  filter: blur(24px);
}

.stat-icon {
  position: absolute;
  top: 18px;
  right: 20px;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.9;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.stat-value {
  font-size: 34px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.01em;
}

.stat-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}

.stat-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity var(--dur-base) var(--ease-out), transform var(--dur-base) var(--ease-out), color var(--dur-base) var(--ease-out);
}
.stat-card:hover .stat-link {
  opacity: 1;
  transform: translateX(0);
  color: var(--accent);
}

/* ---------- 设备切换 ---------- */
.device-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.bar-label {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
}

.device-bar :deep(.el-radio-group) {
  background: rgba(255, 255, 255, 0.06);
}
.device-bar :deep(.el-radio-button__inner) {
  background: transparent;
  border-color: transparent;
  color: rgba(255, 255, 255, 0.7);
  box-shadow: none;
  padding: 8px 18px;
}
.device-bar :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #007AFF;
  border-color: #007AFF;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.4);
}

.status-tag {
  background: rgba(255, 255, 255, 0.08);
  border: none;
  color: rgba(255, 255, 255, 0.85);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 26px;
}
.tag-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.tag-dot.online { background: #30D158; box-shadow: 0 0 6px #30D158; }
.tag-dot.offline { background: #8E8E93; }

.device-bar-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.55);
  cursor: pointer;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  transition: color var(--dur-fast) var(--ease-out), background var(--dur-fast) var(--ease-out);
}
.device-bar-link:hover {
  color: #0A84FF;
  background: rgba(0, 122, 255, 0.12);
}

/* ---------- 面板 ---------- */
.main-row {
  display: grid;
  grid-template-columns: 5fr 7fr;
  gap: 20px;
  min-height: 320px;
}

.bottom-row {
  display: grid;
  grid-template-columns: 4fr 3fr 3fr;
  gap: 20px;
  align-items: stretch;
}

.panel {
  background: linear-gradient(180deg, #1F1F22, #1C1C1E);
  border-radius: 16px;
  padding: 20px;
  border: 0.5px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
  min-width: 0;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-bar {
  width: 3px;
  height: 14px;
  border-radius: var(--radius-full);
  background: #007AFF;
  box-shadow: 0 0 8px rgba(0, 122, 255, 0.6);
}

.panel-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  transition: color var(--dur-fast) var(--ease-out), background var(--dur-fast) var(--ease-out);
}
.panel-link:hover {
  color: #0A84FF;
  background: rgba(0, 122, 255, 0.12);
}

/* ---------- 仪表盘 ---------- */
.gauge-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.gauge-item {
  text-align: center;
}

.gauge-box {
  height: 130px;
}

.gauge-value {
  font-size: 22px;
  font-weight: 700;
  margin-top: -8px;
}

.gauge-value .over {
  color: #FF453A;
}

.gauge-unit {
  font-size: 12px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.5);
  margin-left: 4px;
}

.gauge-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 2px;
}

.trend-chart {
  height: 280px;
}

/* ---------- 设备卡片 ---------- */
.device-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.device-card {
  background: #2C2C2E;
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: border-color var(--dur-base) var(--ease-out),
              transform var(--dur-base) var(--ease-out),
              background var(--dur-base) var(--ease-out);
}
.device-card:hover {
  transform: translateY(-1px);
  background: #323236;
}
.device-card.active {
  border-color: #007AFF;
  background: rgba(0, 122, 255, 0.08);
}

.dev-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dev-name {
  font-size: 14px;
  font-weight: 600;
}

.dev-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dev-dot.online { background: #30D158; box-shadow: 0 0 8px #30D158; }
.dev-dot.offline { background: #8E8E93; }

.dev-code {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 2px;
}

.dev-values {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px 12px;
  margin-top: 10px;
}

.dev-val {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.dev-val-code {
  color: rgba(255, 255, 255, 0.5);
}

.dev-val-num {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.dev-val-num.over {
  color: #FF453A;
}

.dev-link {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 0.5px solid rgba(255, 255, 255, 0.08);
  font-size: 11px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.35);
  transition: color var(--dur-fast) var(--ease-out);
}
.device-card:hover .dev-link {
  color: #0A84FF;
}

/* ---------- 告警 ---------- */
.alert-count {
  background: rgba(255, 59, 48, 0.15);
  color: #FF453A;
  border: none;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #2C2C2E;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 13px;
  border-left: 3px solid transparent;
  cursor: pointer;
  transition: background var(--dur-fast) var(--ease-out);
}
.alert-item:hover { background: #323236; }
.alert-item.alarm { border-left-color: #FF453A; }
.alert-item.warn { border-left-color: #FF9F0A; }

.alert-level {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
}

.alert-level.alarm { background: rgba(255, 69, 58, 0.2); color: #FF453A; }
.alert-level.warn { background: rgba(255, 159, 10, 0.2); color: #FF9F0A; }

.alert-msg {
  flex: 1;
  color: rgba(255, 255, 255, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  flex-shrink: 0;
}

/* ---------- 告警趋势 ---------- */
.alert-trend-chart {
  height: 210px;
}
</style>
