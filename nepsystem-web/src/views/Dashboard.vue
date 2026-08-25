<template>
  <div class="dashboard" data-theme="dark">
    <!-- ═══════ 01 / HERO ═══════ -->
    <section class="hero">
      <div class="hero-glow"></div>
      <div class="hero-grid-bg"></div>
      <div class="hero-inner">
        <div class="section-label reveal-hero d1"><span class="num">01</span> / OVERVIEW</div>

        <h1 class="hero-title">
          <span class="line reveal-hero d2">ENVIRONMENTAL</span>
          <span class="line reveal-hero d3">MONITORING</span>
          <span class="line accent reveal-hero d4">SYSTEM</span>
        </h1>

        <div class="hero-metric reveal-hero d5" @click="go({ path: '/history', query: { deviceId: activeDeviceId } })">
          <div class="hero-metric-value tabular-nums">{{ qualityOverall ?? '--' }}</div>
          <div class="hero-metric-label">AIR QUALITY INDEX</div>
          <div class="hero-metric-hint">点击查看详情 →</div>
        </div>

        <div class="hero-status reveal-hero d6">
          <span class="live-dot"></span>
          <span class="status-text">SYSTEM ONLINE</span>
          <span class="status-sep"></span>
          <span class="status-time tabular-nums">UPDATED {{ now }}</span>
        </div>
      </div>

      <div class="scroll-hint">
        <span class="hint-text">SCROLL TO EXPLORE</span>
        <span class="hint-line"></span>
      </div>
    </section>

    <!-- ═══════ 数据带：系统总览（点击跳转） ═══════ -->
    <section class="data-strip">
      <div v-for="(s, i) in stats" :key="s.label" class="strip-item reveal" :style="{ transitionDelay: (i * 90) + 'ms' }" @click="go(s.to)">
        <div class="strip-value tabular-nums" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="strip-label">{{ s.label }}</div>
        <span class="strip-arrow">→</span>
      </div>
    </section>

    <!-- ═══════ 02 / REAL-TIME DATA ═══════ -->
    <section class="section realtime">
      <div class="section-head reveal">
        <div class="section-label"><span class="num">02</span> / REAL-TIME DATA</div>
        <div class="device-bar">
          <span class="bar-label">监测点位</span>
          <el-radio-group v-model="activeDeviceId" size="large">
            <el-radio-button v-for="d in devices" :key="d.id" :value="d.id">{{ d.deviceName }}</el-radio-button>
          </el-radio-group>
          <span class="device-status" :class="activeDevice?.status === 1 ? 'online' : 'offline'">
            <span class="live-dot" :class="activeDevice?.status !== 1 ? 'off' : ''"></span>
            {{ activeDevice?.status === 1 ? 'ONLINE' : 'OFFLINE' }}
          </span>
        </div>
      </div>

      <div class="realtime-grid">
        <!-- 主指标：Gauge -->
        <div class="primary-panel reveal">
          <div class="panel-label">PRIMARY METRIC</div>
          <div v-if="primarySensor" class="gauge-wrap">
            <div :ref="(el) => setGaugeRef(primarySensor.sensorCode, el)" class="primary-gauge"></div>
            <div class="gauge-under">
              <span class="gauge-under-value tabular-nums" :class="{ over: isOver(primarySensor.sensorCode) }">
                {{ fmtValue(primarySensor.sensorCode) }}
              </span>
              <span class="gauge-under-unit">{{ primarySensor.unit }}</span>
            </div>
            <div class="gauge-under-name">{{ primarySensor.sensorName }}</div>
            <div class="gauge-standard">STANDARD ≤ {{ standards[primarySensor.sensorCode] ?? '—' }}</div>
          </div>
        </div>

        <!-- 辅助指标 -->
        <div class="aux-panel reveal" style="transitionDelay: 120ms">
          <div class="panel-label">SECONDARY METRICS</div>
          <div class="aux-grid">
            <div v-for="s in auxSensors" :key="s.sensorCode" class="aux-item" @click="go({ path: '/history', query: { deviceId: activeDeviceId } })">
              <div class="aux-value tabular-nums" :class="{ over: isOver(s.sensorCode) }">{{ fmtValue(s.sensorCode) }}</div>
              <div class="aux-name">{{ s.sensorName }}</div>
              <div class="aux-unit">{{ s.unit }}</div>
            </div>
            <div v-if="!auxSensors.length" class="aux-empty">选择设备查看指标</div>
          </div>
        </div>

        <!-- 实时趋势 -->
        <div class="trend-panel reveal" style="transitionDelay: 240ms">
          <div class="panel-head">
            <div class="panel-label">RECENT TREND</div>
            <span class="panel-link" @click="go({ path: '/history', query: { deviceId: activeDeviceId } })">ANALYZE →</span>
          </div>
          <div ref="trendChartRef" class="trend-chart"></div>
        </div>
      </div>
    </section>

    <!-- ═══════ 03 / SENSOR NETWORK ═══════ -->
    <section class="section network">
      <div class="section-head reveal">
        <div class="section-label"><span class="num">03</span> / SENSOR NETWORK</div>
        <span class="panel-link" @click="go('/devices')">MANAGE DEVICES →</span>
      </div>

      <div class="network-grid">
        <!-- 空间视图：传感器网络分布 -->
        <div class="spatial-panel reveal">
          <div class="panel-head">
            <div class="panel-label">SPATIAL VIEW</div>
            <span class="panel-note">点击节点切换设备</span>
          </div>
          <div ref="spatialChartRef" class="spatial-chart"></div>
          <div class="spatial-legend">
            <span class="lg"><i class="lg-dot online"></i>ONLINE</span>
            <span class="lg"><i class="lg-dot offline"></i>OFFLINE</span>
            <span class="lg"><i class="lg-dot selected"></i>SELECTED</span>
          </div>
        </div>

        <!-- 设备状态卡 -->
        <div class="device-cards-panel reveal" style="transitionDelay: 120ms">
          <div class="panel-label">DEVICE STATUS</div>
          <div class="device-grid">
            <div
              v-for="(d, i) in devices"
              :key="d.id"
              class="device-card"
              :style="{ transitionDelay: (i * 70) + 'ms' }"
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
                查看历史数据 →
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════ 04 / ALERTS ═══════ -->
    <section class="section alerts-section">
      <div class="section-head reveal">
        <div class="section-label"><span class="num">04</span> / ALERTS</div>
        <span class="panel-link" @click="go('/alerts')">ALERT CENTER →</span>
      </div>

      <div class="alerts-grid">
        <div class="alert-list-panel reveal">
          <div class="alert-count-line" :class="{ has: unhandled.count > 0 }">
            <span class="count-num tabular-nums">{{ unhandled.count }}</span>
            ACTIVE ALERTS
          </div>
          <div v-if="unhandled.latest && unhandled.latest.length" class="alert-lines">
            <div
              v-for="a in unhandled.latest"
              :key="a.id"
              class="alert-line"
              :class="a.level === 'ALARM' ? 'alarm' : 'warn'"
              @click="go('/alerts')"
            >
              <span class="alert-dot"></span>
              <div class="alert-body">
                <div class="alert-title">{{ a.message }}</div>
                <div class="alert-meta">{{ a.sensorCode }} · {{ fmtTime(a.createTime) }}</div>
              </div>
            </div>
          </div>
          <div v-else class="alert-empty">NO ACTIVE ALERTS — 环境状态良好</div>
        </div>

        <div class="alert-trend-panel reveal" style="transitionDelay: 120ms">
          <div class="panel-head">
            <div class="panel-label">7-DAY ALERT TREND</div>
            <span class="panel-link" @click="go('/alerts')">ALL →</span>
          </div>
          <div ref="alertTrendChartRef" class="alert-trend-chart"></div>
        </div>
      </div>
    </section>

    <footer class="page-footer">
      <span>ENVISION · ENVIRONMENTAL MONITORING SYSTEM</span>
      <span class="footer-right">© 2026 — DATA COCKPIT</span>
    </footer>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getDevicesPage, getDeviceLatest, getOverview, getUnhandled, getSensors, getHistory, getAlertsStat, getQuality } from '@/api'
import { connectWS, onWSMessage } from '@/utils/ws'

const router = useRouter()
function go(to) { router.push(to) }

const devices = ref([])
const sensors = ref([])
const activeDeviceId = ref(null)
const latestMap = ref({})
const standards = ref({})
const unhandled = ref({ count: 0, latest: [] })
const qualityOverall = ref(null)
const now = ref('')
const stats = ref([
  { label: '设备总数', value: 0, color: '#7CA7FF', icon: 'Monitor', to: '/devices' },
  { label: '在线设备', value: 0, color: '#7EE2B8', icon: 'Connection', to: { path: '/devices', query: { status: 1 } } },
  { label: '今日上报', value: 0, color: '#6FD3C7', icon: 'DataLine', to: '/history' },
  { label: '未处理告警', value: 0, color: '#E26D6D', icon: 'Bell', to: '/alerts' }
])

const gaugeRefs = {}
let offWS = null
const trendChartRef = ref()
const alertTrendChartRef = ref()
const spatialChartRef = ref()
let trendChart = null
let alertTrendChart = null
let spatialChart = null
let gaugeCharts = {}
let timers = []
let clockTimer = null
let observer = null

const activeDevice = computed(() => devices.value.find(d => d.id === activeDeviceId.value) || null)
const activeSensors = computed(() => {
  if (!activeDevice.value) return []
  return sensors.value.filter(s => !s.deviceType || s.deviceType === activeDevice.value.type)
})
const primarySensor = computed(() => activeSensors.value[0] || null)
const auxSensors = computed(() => activeSensors.value.slice(1))

function setGaugeRef(code, el) { if (el) gaugeRefs[code] = el }

function typeName(t) { return { AIR: '空气', WATER: '水质', NOISE: '噪声' }[t] || t }
function fmtValue(code) {
  const v = latestMap.value[activeDeviceId.value]?.[code]
  return v === undefined ? '--' : Number(v).toFixed(1)
}
function isOver(code) { return isOverFor(activeDeviceId.value, code) }
function isOverFor(deviceId, code) {
  const v = latestMap.value[deviceId]?.[code]
  const std = standards.value[code]
  return v !== undefined && std && Number(v) > Number(std)
}
function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '' }

async function loadDevices() {
  try {
    const d = await getDevicesPage({ page: 1, size: 100 })
    devices.value = d.records || []
    if (devices.value.length && !activeDeviceId.value) activeDeviceId.value = devices.value[0].id
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
      for (const [code, item] of Object.entries(values)) map[code] = item.value
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
async function loadQuality() {
  try {
    const q = await getQuality()
    if (q && q.overall !== null && q.overall !== undefined) qualityOverall.value = q.overall
  } catch (e) { /* 忽略 */ }
}

// ---------- 近 7 天告警趋势 ----------
async function loadAlertTrend() {
  if (!alertTrendChartRef.value) return
  if (!alertTrendChart) alertTrendChart = echarts.init(alertTrendChartRef.value)
  let rows = []
  try { rows = (await getAlertsStat()) || [] } catch (e) { /* 忽略 */ }
  const days = [...new Set(rows.map(r => r.day))].sort()
  const alarm = days.map(d => Number(rows.find(r => r.day === d && r.level === 'ALARM')?.cnt || 0))
  const warn = days.map(d => Number(rows.find(r => r.day === d && r.level === 'WARN')?.cnt || 0))
  alertTrendChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#1C2421', borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)', textStyle: { color: '#E8ECE9', fontSize: 12 } },
    legend: { data: ['ALARM', 'WARN'], textStyle: { color: '#56615C', fontSize: 10 }, top: 0, right: 0, icon: 'roundRect', itemWidth: 8, itemHeight: 8 },
    grid: { left: 30, right: 8, top: 28, bottom: 22 },
    xAxis: {
      type: 'category', data: days.map(d => String(d).slice(5)),
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.12)' } },
      axisTick: { show: false },
      axisLabel: { color: '#56615C', fontSize: 10 }
    },
    yAxis: {
      type: 'value', minInterval: 1,
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
      axisLabel: { color: '#56615C', fontSize: 10 }
    },
    series: [
      { name: 'ALARM', type: 'bar', data: alarm, barWidth: 7, itemStyle: { color: '#E26D6D', borderRadius: [3, 3, 0, 0] } },
      { name: 'WARN', type: 'bar', data: warn, barWidth: 7, barGap: '30%', itemStyle: { color: '#E5B567', borderRadius: [3, 3, 0, 0] } }
    ]
  })
}

// ---------- Gauge ----------
function updateGauges() {
  if (!primarySensor.value) return
  const code = primarySensor.value.sensorCode
  nextTick(() => {
    const el = gaugeRefs[code]
    if (!el) return
    const std = standards.value[code]
    const max = Number(primarySensor.value.maxRange || (std ? std * 2 : 100))
    if (!gaugeCharts[code]) gaugeCharts[code] = echarts.init(el)
    const v = Number(latestMap.value[activeDeviceId.value]?.[code] ?? 0)
    const over = std && v > Number(std)
    gaugeCharts[code].setOption({
      series: [{
        type: 'gauge',
        startAngle: 200, endAngle: -20,
        min: 0, max,
        progress: { show: true, width: 10, itemStyle: { color: over ? '#E26D6D' : (std ? '#7EE2B8' : '#6FD3C7') } },
        axisLine: { lineStyle: { width: 10, color: [[1, 'rgba(255,255,255,0.06)']] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        detail: { show: false },
        data: [{ value: v }]
      }]
    })
  })
}

// ---------- 实时趋势 ----------
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
      const smoothed = smoothValues(points, 5)
      legend.push(code)
      series.push({
        name: code, type: 'line', smooth: 0.6, showSymbol: false,
        lineStyle: { width: 2 },
        itemStyle: { color: colorOf(code) },
        areaStyle: { opacity: 0.06, color: colorOf(code) },
        data: smoothed.map(p => [String(p.reportTime).replace('T', ' ').slice(5, 16), Number(p.value)])
      })
      if (xAxis.length === 0) xAxis.push(...smoothed.map(p => String(p.reportTime).replace('T', ' ').slice(5, 16)))
    } catch (e) { /* 忽略 */ }
  }
  trendChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#1C2421', borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)', textStyle: { color: '#E8ECE9', fontSize: 12 } },
    legend: { data: legend, textStyle: { color: '#56615C', fontSize: 10 }, top: 0, icon: 'roundRect', itemWidth: 8, itemHeight: 8 },
    grid: { left: 42, right: 16, top: 30, bottom: 24 },
    xAxis: {
      type: 'category', data: xAxis,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.12)' } },
      axisTick: { show: false },
      axisLabel: { color: '#56615C', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
      axisLabel: { color: '#56615C', fontSize: 10 }
    },
    series
  })
}

function smoothValues(rows, window = 5) {
  const half = Math.floor(window / 2)
  const n = rows.length
  if (n < 3) return rows
  return rows.map((r, i) => {
    let sum = 0, cnt = 0
    for (let j = Math.max(0, i - half); j <= Math.min(n - 1, i + half); j++) {
      sum += Number(rows[j].value)
      cnt++
    }
    return { ...r, value: Number((sum / cnt).toFixed(2)) }
  })
}

function colorOf(code) {
  const palette = {
    TEMP: '#E5B567', HUMI: '#6FD3C7', PM25: '#E26D6D', CO2: '#B7A6E8',
    PH: '#7EE2B8', TURBIDITY: '#7CA7FF', DO: '#7EE2B8', NOISE: '#E8D08A'
  }
  return palette[code] || '#7CA7FF'
}

// ---------- 传感器网络空间视图 ----------
function renderSpatial() {
  if (!spatialChartRef.value) return
  if (!spatialChart) {
    spatialChart = echarts.init(spatialChartRef.value)
    spatialChart.on('click', params => {
      const id = params.data?.deviceId
      if (id) activeDeviceId.value = id
    })
  }
  // 按类型分列的空间抽象布局（AIR 左 / WATER 中 / NOISE 右）
  const typeX = { AIR: 15, WATER: 50, NOISE: 85 }
  const colCount = {}
  const data = devices.value.map(d => {
    const x = typeX[d.type] ?? 50
    const idx = colCount[x] || 0
    colCount[x] = idx + 1
    const y = 88 - idx * 30
    const over = Object.keys(latestMap.value[d.id] || {}).some(code => isOverFor(d.id, code))
    return {
      name: d.deviceName,
      value: [x, y],
      deviceId: d.id,
      status: d.status,
      over,
      selected: d.id === activeDeviceId.value
    }
  })
  spatialChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: '#1C2421',
      borderWidth: 1,
      borderColor: 'rgba(255,255,255,0.1)',
      textStyle: { color: '#E8ECE9', fontSize: 12 },
      formatter: p => {
        const d = p.data
        const st = d.status === 1 ? 'ONLINE' : 'OFFLINE'
        return '<b>' + d.name + '</b><br/>' + d.deviceId + ' · ' + st + (d.over ? ' · <span style="color:#E26D6D">OVER</span>' : '')
      }
    },
    grid: { left: 20, right: 20, top: 16, bottom: 16 },
    xAxis: {
      type: 'value', min: 0, max: 100, show: false,
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value', min: 0, max: 100, show: false,
      inverse: true,
      splitLine: { show: false }
    },
    series: [{
      type: 'effectScatter',
      coordinateSystem: 'cartesian2d',
      rippleEffect: { brushType: 'stroke', scale: 3, period: 3.5 },
      symbolSize: v => (v[2] === undefined ? 14 : 14),
      data: data.map(d => ({
        name: d.name,
        value: d.value,
        deviceId: d.deviceId,
        symbolSize: 14,
        itemStyle: {
          color: d.selected ? '#7EE2B8' : (d.over ? '#E26D6D' : (d.status === 1 ? 'rgba(126,226,184,0.85)' : '#3A423E')),
          borderColor: d.selected ? '#9BEACA' : 'transparent',
          borderWidth: d.selected ? 2 : 0,
          shadowBlur: d.status === 1 ? 14 : 0,
          shadowColor: d.over ? 'rgba(226,109,109,0.7)' : 'rgba(126,226,184,0.55)'
        },
        label: {
          show: true,
          position: 'bottom',
          distance: 6,
          formatter: d => d.deviceCode,
          color: d.selected ? '#7EE2B8' : '#56615C',
          fontSize: 10
        }
      }))
    }]
  })
}

// ---------- 滚动叙事 ----------
function setupReveal() {
  observer = new IntersectionObserver(entries => {
    for (const en of entries) {
      if (en.isIntersecting) {
        en.target.classList.add('in')
        observer.unobserve(en.target)
      }
    }
  }, { threshold: 0.12 })
  document.querySelectorAll('.dashboard .reveal, .dashboard .reveal-hero').forEach(el => observer.observe(el))
}

function tick() {
  const d = new Date()
  now.value = [d.getHours(), d.getMinutes(), d.getSeconds()].map(n => String(n).padStart(2, '0')).join(':')
}

// ---------- 生命周期 ----------
onMounted(async () => {
  await Promise.all([loadDevices(), loadSensors()])
  await Promise.all([loadLatest(), loadOverview(), loadUnhandled(), loadAlertTrend(), loadQuality()])
  renderSpatial()
  timers = [
    setInterval(loadOverview, 30000),
    setInterval(loadUnhandled, 30000),
    setInterval(loadAlertTrend, 60000),
    setInterval(loadQuality, 60000)
  ]
  clockTimer = setInterval(tick, 1000)
  tick()
  window.addEventListener('resize', onResize)
  setupReveal()

  connectWS()
  offWS = onWSMessage(msg => {
    if (msg.type === 'data') {
      const id = msg.deviceId
      latestMap.value = { ...latestMap.value, [id]: { ...(latestMap.value[id] || {}), [msg.sensorCode]: msg.value } }
      updateGauges()
      loadTrend()
      renderSpatial()
    } else if (msg.type === 'alert') {
      loadUnhandled()
    }
  })
})

function onResize() {
  trendChart?.resize()
  alertTrendChart?.resize()
  spatialChart?.resize()
  for (const c of Object.values(gaugeCharts)) c?.resize()
}

onUnmounted(() => {
  timers.forEach(clearInterval)
  clearInterval(clockTimer)
  window.removeEventListener('resize', onResize)
  observer?.disconnect()
  if (offWS) offWS()
  trendChart?.dispose()
  alertTrendChart?.dispose()
  spatialChart?.dispose()
  for (const c of Object.values(gaugeCharts)) c?.dispose()
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background: var(--bg-primary);
  overflow-x: hidden;
}

/* ═══════════ HERO ═══════════ */
.hero {
  position: relative;
  min-height: calc(100vh - 88px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 120px var(--sp-48) 80px;
  overflow: hidden;
}

.hero-glow {
  position: absolute;
  top: -20%;
  left: 50%;
  transform: translateX(-50%);
  width: 900px;
  height: 600px;
  background: radial-gradient(ellipse at center, rgba(126, 226, 184, 0.07), transparent 65%);
  pointer-events: none;
}

.hero-grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.025) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: radial-gradient(ellipse 80% 70% at 50% 30%, #000 20%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse 80% 70% at 50% 30%, #000 20%, transparent 75%);
  pointer-events: none;
}

.hero-inner {
  position: relative;
  z-index: 1;
}

.hero-title {
  margin: var(--sp-16) 0 var(--sp-40);
  font-size: clamp(44px, 7vw, 88px);
  font-weight: 700;
  line-height: 1.02;
  letter-spacing: -0.03em;
  color: var(--text-primary);
}
.hero-title .line { display: block; }
.hero-title .accent {
  color: transparent;
  -webkit-text-stroke: 1px rgba(232, 236, 233, 0.45);
}

.hero-metric {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  cursor: pointer;
  border-left: 1px solid var(--border-soft);
  padding-left: var(--sp-24);
  transition: border-color var(--dur-base) var(--ease-out);
}
.hero-metric:hover { border-color: var(--env-green); }

.hero-metric-value {
  font-size: clamp(64px, 9vw, 120px);
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.03em;
  color: var(--env-green);
  text-shadow: 0 0 60px rgba(126, 226, 184, 0.25);
}
.hero-metric-label {
  margin-top: var(--sp-12);
  font-size: var(--fs-label);
  font-weight: 600;
  letter-spacing: var(--tracking-label);
  color: var(--text-secondary);
}
.hero-metric-hint {
  margin-top: var(--sp-8);
  font-size: 11px;
  color: var(--text-muted);
  opacity: 0;
  transform: translateY(4px);
  transition: all var(--dur-base) var(--ease-out);
}
.hero-metric:hover .hero-metric-hint { opacity: 1; transform: none; }

.hero-status {
  display: flex;
  align-items: center;
  gap: var(--sp-16);
  margin-top: var(--sp-40);
  padding-top: var(--sp-24);
  border-top: 1px solid var(--border-subtle);
  max-width: 560px;
}
.status-text {
  font-size: var(--fs-label);
  font-weight: 600;
  letter-spacing: var(--tracking-label);
  color: var(--env-green);
}
.status-sep {
  width: 1px;
  height: 12px;
  background: var(--border-soft);
}
.status-time {
  font-size: var(--fs-label);
  letter-spacing: 0.14em;
  color: var(--text-muted);
}

/* Hero 入场动画 */
.reveal-hero {
  opacity: 0;
  transform: translateY(28px);
  animation: hero-in 0.9s var(--ease-out) forwards;
}
.d1 { animation-delay: 0.1s; }
.d2 { animation-delay: 0.25s; }
.d3 { animation-delay: 0.4s; }
.d4 { animation-delay: 0.55s; }
.d5 { animation-delay: 0.7s; }
.d6 { animation-delay: 0.85s; }
@keyframes hero-in {
  to { opacity: 1; transform: none; }
}

/* SCROLL 提示 */
.scroll-hint {
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  opacity: 0;
  animation: hero-in 0.9s var(--ease-out) 1.3s forwards;
}
.hint-text {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--text-muted);
}
.hint-line {
  width: 1px;
  height: 40px;
  background: linear-gradient(180deg, var(--text-faint), transparent);
  animation: hint-drop 2.2s ease-in-out infinite;
}
@keyframes hint-drop {
  0% { transform: scaleY(0); transform-origin: top; }
  45% { transform: scaleY(1); transform-origin: top; }
  55% { transform: scaleY(1); transform-origin: bottom; }
  100% { transform: scaleY(0); transform-origin: bottom; }
}

/* ═══════════ 数据带 ═══════════ */
.data-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid var(--border-subtle);
  border-bottom: 1px solid var(--border-subtle);
  margin: 0 var(--sp-48);
}

.strip-item {
  position: relative;
  padding: var(--sp-32) var(--sp-24);
  cursor: pointer;
  border-right: 1px solid var(--border-subtle);
  transition: background var(--dur-base) var(--ease-out);
}
.strip-item:last-child { border-right: none; }
.strip-item:hover { background: rgba(255, 255, 255, 0.03); }

.strip-value {
  font-size: 44px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.02em;
}
.strip-label {
  margin-top: var(--sp-12);
  font-size: var(--fs-label);
  font-weight: 500;
  letter-spacing: 0.12em;
  color: var(--text-secondary);
}
.strip-arrow {
  position: absolute;
  top: var(--sp-24);
  right: var(--sp-24);
  font-size: 13px;
  color: var(--text-faint);
  opacity: 0;
  transform: translateX(-6px);
  transition: all var(--dur-base) var(--ease-out);
}
.strip-item:hover .strip-arrow { opacity: 1; transform: none; color: var(--env-green); }

/* ═══════════ Section 通用 ═══════════ */
.section {
  padding: var(--sp-64) var(--sp-48);
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-24);
  flex-wrap: wrap;
  margin-bottom: var(--sp-40);
}

.panel-label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.24em;
  color: var(--text-muted);
  margin-bottom: var(--sp-16);
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--sp-16);
}
.panel-head .panel-label { margin-bottom: 0; }
.panel-link {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: var(--text-muted);
  cursor: pointer;
  transition: color var(--dur-fast) var(--ease-out);
}
.panel-link:hover { color: var(--env-green); }

/* ═══════════ 02 / REAL-TIME ═══════════ */
.device-bar {
  display: flex;
  align-items: center;
  gap: var(--sp-16);
  flex-wrap: wrap;
}
.bar-label {
  font-size: var(--fs-footnote);
  font-weight: 500;
  letter-spacing: 0.1em;
  color: var(--text-muted);
}
.device-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: var(--env-green);
}
.device-status.offline { color: var(--text-muted); }
.device-status .live-dot.off { background: var(--text-faint); animation: none; box-shadow: none; }

.realtime-grid {
  display: grid;
  grid-template-columns: 300px 300px 1fr;
  gap: var(--sp-24);
  align-items: stretch;
}

.primary-panel,
.aux-panel,
.trend-panel {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: var(--sp-24);
}

.gauge-wrap { text-align: center; }
.primary-gauge { height: 170px; }
.gauge-under { margin-top: -14px; }
.gauge-under-value {
  font-size: 34px;
  font-weight: 700;
  color: var(--text-primary);
}
.gauge-under-value.over { color: var(--danger); }
.gauge-under-unit {
  font-size: 13px;
  color: var(--text-muted);
  margin-left: 6px;
}
.gauge-under-name {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
}
.gauge-standard {
  margin-top: var(--sp-8);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--text-muted);
}

.aux-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 2px 12px;
}
.aux-item {
  padding: var(--sp-16) var(--sp-12);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--dur-fast) var(--ease-out);
}
.aux-item:hover { background: rgba(255, 255, 255, 0.04); }
.aux-value {
  font-size: 26px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}
.aux-value.over { color: var(--danger); }
.aux-name {
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-secondary);
}
.aux-unit {
  font-size: 10px;
  color: var(--text-muted);
}
.aux-empty {
  grid-column: 1 / -1;
  padding: var(--sp-24);
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
}

.trend-panel { min-width: 0; }
.trend-chart { height: 300px; }

/* ═══════════ 03 / SENSOR NETWORK ═══════════ */
.network-grid {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: var(--sp-24);
  align-items: stretch;
}

.spatial-panel,
.device-cards-panel {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: var(--sp-24);
}

.spatial-panel { display: flex; flex-direction: column; }
.panel-note {
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--text-faint);
}
.spatial-chart {
  flex: 1;
  min-height: 320px;
  background:
    radial-gradient(ellipse 60% 50% at 50% 40%, rgba(126, 226, 184, 0.04), transparent 70%),
    var(--bg-inset);
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.spatial-legend {
  display: flex;
  gap: var(--sp-20);
  margin-top: var(--sp-16);
  padding-top: var(--sp-16);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}
.lg {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: var(--text-muted);
}
.lg-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.lg-dot.online { background: var(--env-green); }
.lg-dot.offline { background: #3A423E; }
.lg-dot.selected { background: var(--env-green); box-shadow: 0 0 0 2px rgba(126, 226, 184, 0.3); }

.device-cards-panel { min-width: 0; }

.device-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--sp-16);
}

.device-card {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: var(--sp-20);
  cursor: pointer;
  transition: border-color var(--dur-base) var(--ease-out),
              transform var(--dur-base) var(--ease-out),
              background var(--dur-base) var(--ease-out);
}
.device-card:hover {
  transform: translateY(-2px);
  border-color: var(--border-soft);
  background: var(--bg-elevated);
}
.device-card.active {
  border-color: rgba(126, 226, 184, 0.5);
  background: rgba(126, 226, 184, 0.04);
}

.dev-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.dev-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.dev-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.dev-dot.online { background: var(--env-green); box-shadow: 0 0 8px rgba(126, 226, 184, 0.7); }
.dev-dot.offline { background: var(--text-faint); }

.dev-code {
  margin-top: 4px;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--text-muted);
}

.dev-values {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px 16px;
  margin-top: var(--sp-16);
  padding-top: var(--sp-16);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}
.dev-val {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.dev-val-code {
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--text-muted);
}
.dev-val-num {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.dev-val-num.over { color: var(--danger); }

.dev-link {
  margin-top: var(--sp-16);
  padding-top: var(--sp-12);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.08em;
  color: var(--text-faint);
  transition: color var(--dur-fast) var(--ease-out);
}
.device-card:hover .dev-link { color: var(--env-green); }

/* ═══════════ 04 / ALERTS ═══════════ */
.alerts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--sp-24);
}

.alert-list-panel,
.alert-trend-panel {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: var(--sp-24);
}

.alert-count-line {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: var(--fs-label);
  font-weight: 600;
  letter-spacing: var(--tracking-label);
  color: var(--text-muted);
  margin-bottom: var(--sp-20);
  padding-bottom: var(--sp-16);
  border-bottom: 1px solid var(--border-subtle);
}
.alert-count-line .count-num { font-size: 30px; font-weight: 700; }
.alert-count-line.has { color: var(--danger); }
.alert-count-line.has .count-num { color: var(--danger); }

.alert-lines { display: flex; flex-direction: column; }
.alert-line {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-16);
  padding: var(--sp-16) var(--sp-8);
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  transition: background var(--dur-fast) var(--ease-out);
}
.alert-line:last-child { border-bottom: none; }
.alert-line:hover { background: rgba(255, 255, 255, 0.03); }

.alert-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}
.alert-line.alarm .alert-dot { background: var(--danger); box-shadow: 0 0 8px rgba(226, 109, 109, 0.6); }
.alert-line.warn .alert-dot { background: var(--warning); box-shadow: 0 0 8px rgba(229, 181, 103, 0.5); }

.alert-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}
.alert-meta {
  margin-top: 3px;
  font-size: 11px;
  letter-spacing: 0.06em;
  color: var(--text-muted);
}
.alert-empty {
  padding: var(--sp-32) 0;
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--text-muted);
  text-align: center;
}

.alert-trend-chart { height: 240px; }

/* ═══════════ 滚动叙事 ═══════════ */
.reveal {
  opacity: 0;
  transform: translateY(26px);
  transition: opacity 0.7s var(--ease-out), transform 0.7s var(--ease-out);
}
.reveal.in {
  opacity: 1;
  transform: none;
}

/* ═══════════ 页脚 ═══════════ */
.page-footer {
  display: flex;
  justify-content: space-between;
  padding: var(--sp-32) var(--sp-48) var(--sp-40);
  border-top: 1px solid var(--border-subtle);
  font-size: 10px;
  letter-spacing: 0.18em;
  color: var(--text-faint);
}
.footer-right { letter-spacing: 0.1em; }

/* ═══════════ 响应式 ═══════════ */
@media (max-width: 1200px) {
  .network-grid { grid-template-columns: 1fr; }
}
@media (max-width: 1100px) {
  .realtime-grid { grid-template-columns: 1fr; }
  .alerts-grid { grid-template-columns: 1fr; }
  .device-grid { grid-template-columns: repeat(2, 1fr); }
  .data-strip { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 720px) {
  .device-grid { grid-template-columns: 1fr; }
  .data-strip { grid-template-columns: 1fr; }
  .section { padding: var(--sp-40) var(--sp-24); }
}
</style>
