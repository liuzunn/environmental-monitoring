<template>
  <div class="situation-page">
    <!-- 状态横幅 -->
    <div class="status-banner" :class="overview.healthy ? 'ok' : 'alert'">
      <span class="dot"></span>
      <span class="banner-text">{{ overview.healthy ? '环境状态良好 · 无活跃告警' : '存在 ' + overview.activeAlerts + ' 条活跃告警（报警 ' + overview.alarmDevices + ' 台 / 预警 ' + overview.warnDevices + ' 台）' }}</span>
      <span class="banner-time">{{ now }}</span>
    </div>

    <!-- 统计 chips -->
    <div class="stat-row">
      <div class="chip"><span class="chip-num">{{ overview.totalDevices }}</span><span class="chip-label">设备总数</span></div>
      <div class="chip online"><span class="chip-num">{{ overview.onlineDevices }}</span><span class="chip-label">在线</span></div>
      <div class="chip offline"><span class="chip-num">{{ overview.offlineDevices }}</span><span class="chip-label">离线</span></div>
      <div class="chip warn"><span class="chip-num">{{ overview.warnDevices }}</span><span class="chip-label">预警</span></div>
      <div class="chip alarm"><span class="chip-num">{{ overview.alarmDevices }}</span><span class="chip-label">报警</span></div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="设备名称 / 编号" clearable class="f-keyword" @change="reload" />
      <el-select v-model="filters.type" placeholder="全部类型" clearable class="f-select" @change="reload">
        <el-option label="空气" value="AIR" />
        <el-option label="水质" value="WATER" />
        <el-option label="噪声" value="NOISE" />
      </el-select>
      <el-select v-model="filters.status" placeholder="全部状态" clearable class="f-select" @change="reload">
        <el-option label="在线" :value="1" />
        <el-option label="离线" :value="0" />
        <el-option label="停用" :value="2" />
      </el-select>
      <el-select v-model="filters.alertLevel" placeholder="全部告警" clearable class="f-select" @change="reload">
        <el-option label="报警中" value="ALARM" />
        <el-option label="预警中" value="WARN" />
        <el-option label="无告警" value="NONE" />
      </el-select>
      <el-button :icon="Refresh" circle class="f-refresh" @click="reload" />
    </div>

    <!-- 主体：态势图 + 详情 -->
    <div class="main-area">
      <div class="chart-wrap" :class="{ 'with-detail': selected }">
        <div ref="chartRef" class="chart"></div>
        <div v-if="!devices.length" class="chart-empty">暂无设备数据</div>
        <div class="chart-hint">{{ hasCoords ? '坐标示意模式（lat/lng）' : '类型分组布局（未配置坐标）' }}</div>
      </div>

      <!-- 设备详情 -->
      <transition name="fade-slide">
        <div v-if="selected" class="detail-card">
          <div class="detail-head">
            <span class="detail-title">{{ selected.deviceName }}</span>
            <span class="detail-type">{{ typeName(selected.type) }}</span>
            <el-button text :icon="Close" class="detail-close" @click="selected = null" />
          </div>
          <div class="detail-rows">
            <div class="d-row"><span class="d-label">设备编号</span><span class="d-value">{{ selected.deviceCode }}</span></div>
            <div class="d-row"><span class="d-label">所在位置</span><span class="d-value">{{ selected.location || '-' }}{{ posTail }}</span></div>
            <div class="d-row"><span class="d-label">在线状态</span><span class="d-value"><el-tag :type="selected.online ? 'success' : 'info'" size="small">{{ selected.online ? '在线' : '离线' }}</el-tag> <el-tag :type="stateTag(selected)" size="small">{{ stateName(selected) }}</el-tag></span></div>
            <div class="d-row"><span class="d-label">健康度</span><span class="d-value">
              <span class="health-score" :style="{ color: healthColor(selected.healthLevel) }">{{ selected.healthScore ?? '-' }}</span>
              <span class="health-level" :style="{ color: healthColor(selected.healthLevel) }">{{ selected.healthLevel || '-' }}</span>
              <span class="health-bar"><span class="health-bar-inner" :style="{ width: (selected.healthScore || 0) + '%', background: healthColor(selected.healthLevel) }"></span></span>
            </span></div>
            <div class="d-row"><span class="d-label">数据质量</span><span class="d-value"><el-tag :type="qualityTag(selected.qualityStatus)" size="small">{{ qualityName(selected.qualityStatus) }}</el-tag></span></div>
          </div>
          <div class="detail-section">当前数据</div>
          <table class="value-table">
            <thead><tr><th>指标</th><th>数值</th><th>单位</th><th>时间</th></tr></thead>
            <tbody>
              <tr v-for="(v, code) in selected.values" :key="code" :class="{ over: isOver(code, v) }">
                <td>{{ sensorName(code) }}</td>
                <td class="num">{{ fmtValue(v) }}</td>
                <td>{{ unitOf(code) }}</td>
                <td class="time">{{ fmtTime(v.reportTime) }}</td>
              </tr>
              <tr v-if="!Object.keys(selected.values || {}).length"><td colspan="4" class="empty-cell">暂无数据</td></tr>
            </tbody>
          </table>
          <div class="detail-section">当前告警</div>
          <div v-if="(selected.alerts || []).length" class="alert-list">
            <div v-for="(a, i) in selected.alerts" :key="i" class="alert-item">
              <el-tag :type="a.level === 'ALARM' ? 'danger' : 'warning'" size="small">{{ a.level }}</el-tag>
              <span class="alert-msg">{{ a.message || (sensorName(a.sensorCode) + ' 告警') }}</span>
              <span class="alert-time">{{ fmtTime(a.createTime) }}</span>
            </div>
          </div>
          <div v-else class="no-alert">无活跃告警 ✓</div>
        </div>
      </transition>
    </div>
  </div>
</template>
<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Refresh, Close } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getSituationOverview, getSituationDevices, getSensors } from '@/api'
import { connectWS, onWSMessage } from '@/utils/ws'

const chartRef = ref()
let chart = null

const overview = reactive({ totalDevices: 0, onlineDevices: 0, offlineDevices: 0, warnDevices: 0, alarmDevices: 0, activeAlerts: 0, healthy: true })
const devices = ref([])
const sensors = ref([])
const selected = ref(null)
const now = ref('')
const filters = reactive({ keyword: '', type: null, status: null, alertLevel: null })

let offWS = null
let pollTimer = null
let clockTimer = null
let refreshTimer = null

const hasCoords = computed(() => devices.value.some(d => d.lat != null && d.lng != null))

// ---------- 工具 ----------
function typeName(t) { return { AIR: '空气', WATER: '水质', NOISE: '噪声' }[t] || t }
function stateName(d) {
  if (d.alarm) return '报警'
  if (d.warn) return '预警'
  if (!d.online) return '离线'
  return '正常'
}
function stateTag(d) {
  if (d.alarm) return 'danger'
  if (d.warn) return 'warning'
  if (!d.online) return 'info'
  return 'success'
}
function healthColor(level) {
  return { HEALTHY: '#7EE2B8', FAIR: '#E5B567', POOR: '#E26D6D' }[level] || '#56615C'
}
function qualityTag(q) { return { BAD: 'danger', WARNING: 'warning', GOOD: 'success' }[q] || 'info' }
function qualityName(q) { return { BAD: '差', WARNING: '警告', GOOD: '良好' }[q] || '-' }
function sensorName(code) {
  const s = sensors.value.find(x => x.sensorCode === code)
  return s ? s.sensorName : code
}
function unitOf(code) {
  const s = sensors.value.find(x => x.sensorCode === code)
  return s && s.unit ? s.unit : '-'
}
function fmtValue(v) { return v && v.value !== undefined ? Number(v.value).toFixed(1) : '-' }
function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }
function isOver(code, v) {
  const std = standards[code]
  return std && v && v.value !== undefined && Number(v.value) > Number(std)
}
const standards = computed(() => {
  const m = {}
  for (const s of sensors.value) m[s.sensorCode] = s.standardMax
  return m
})
const posTail = computed(() => {
  const d = selected.value
  if (d && d.lat != null && d.lng != null) return '（' + d.lat + ', ' + d.lng + '）'
  return ''
})

// ---------- 数据加载 ----------
async function loadOverview() {
  try {
    Object.assign(overview, await getSituationOverview())
  } catch (e) { /* 忽略 */ }
}
async function loadDevices() {
  try {
    const list = await getSituationDevices({
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      status: filters.status ?? undefined,
      alertLevel: filters.alertLevel || undefined
    })
    const prevId = selected.value ? selected.value.deviceId : null
    devices.value = list || []
    if (prevId) {
      selected.value = devices.value.find(d => d.deviceId === prevId) || null
    }
    renderChart()
  } catch (e) { /* 忽略 */ }
}
async function loadSensors() {
  try { sensors.value = (await getSensors()) || [] } catch (e) { /* 忽略 */ }
}
function reload() { loadOverview(); loadDevices() }

// ---------- 态势图 ----------
function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const list = devices.value
  const std = standards.value

  // 状态/视觉优先级：报警(红) > 预警(琥珀) > 超标(橙) > 离线(灰) > 正常(绿)
  function stateOf(d) {
    if (d.alarm) return 'alarm'
    if (d.warn) return 'warn'
    if (d.online && Object.keys(d.values || {}).some(code => {
      const v = d.values[code]
      return std[code] && v && Number(v.value) > Number(std[code])
    })) return 'over'
    if (!d.online) return 'offline'
    return 'normal'
  }
  const colors = { normal: '#7EE2B8', over: '#FF9F0A', offline: '#3A423E', warn: '#E5B567', alarm: '#E26D6D' }

  const typeX = { AIR: 15, WATER: 50, NOISE: 85 }
  const colCount = {}
  const points = list.map(d => {
    const st = stateOf(d)
    let x, y
    if (d.lat != null && d.lng != null) {
      x = Number(d.lng)
      y = Number(d.lat)
    } else {
      x = typeX[d.type] ?? 50
      const idx = colCount[x] || 0
      colCount[x] = idx + 1
      y = 88 - idx * 26
    }
    return {
      name: d.deviceName,
      code: d.deviceCode,
      deviceId: d.deviceId,
      type: d.type,
      st,
      online: d.online,
      health: d.healthScore,
      value: [x, y],
      itemStyle: {
        color: d.deviceId === (selected.value && selected.value.deviceId) ? '#9BEACA' : colors[st],
        borderColor: d.deviceId === (selected.value && selected.value.deviceId) ? '#EAFBF3' : 'transparent',
        borderWidth: d.deviceId === (selected.value && selected.value.deviceId) ? 3 : 0,
        shadowBlur: st === 'alarm' ? 18 : (st === 'warn' ? 12 : 6),
        shadowColor: colors[st] + '88'
      },
      symbolSize: d.deviceId === (selected.value && selected.value.deviceId) ? 22 : (st === 'offline' ? 12 : 16)
    }
  })

  const hasGeo = list.some(d => d.lat != null && d.lng != null)
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: '#1C2421',
      borderWidth: 1,
      borderColor: 'rgba(255,255,255,0.1)',
      textStyle: { color: '#E8ECE9', fontSize: 12 },
      formatter: p => {
        const d = p.data
        const vals = Object.keys(d.values || {}).map(c => {
          const v = d.values[c]
          const over = std[c] && Number(v.value) > Number(std[c])
          return c + '=' + Number(v.value).toFixed(1) + (over ? ' ▲' : '')
        }).join('  ')
        return '<b>' + d.name + '</b><br/>' + d.code + ' · ' + typeName(d.type) + '<br/>' +
          '状态: ' + stateName(d) + ' · 健康度: ' + (d.health ?? '-') + '<br/>' +
          (vals ? vals + '<br/>' : '') +
          (d.alerts && d.alerts.length ? '告警: ' + d.alerts.length + ' 条' : '无活跃告警')
      }
    },
    grid: { left: 20, right: 20, top: 20, bottom: 20 },
    xAxis: {
      type: 'value', show: false,
      min: hasGeo ? undefined : 0, max: hasGeo ? undefined : 100,
      scale: hasGeo
    },
    yAxis: {
      type: 'value', show: false,
      min: hasGeo ? undefined : 0, max: hasGeo ? undefined : 100,
      scale: hasGeo,
      inverse: !hasGeo
    },
    series: [{
      type: 'effectScatter',
      coordinateSystem: 'cartesian2d',
      rippleEffect: { brushType: 'stroke', scale: 2.6, period: 3 },
      data: points,
      label: {
        show: true,
        position: 'bottom',
        distance: 6,
        formatter: p => p.data.code,
        color: p => p.data.deviceId === (selected.value && selected.value.deviceId) ? '#9BEACA' : '#56615C',
        fontSize: 10
      },
      emphasis: { scale: 1.6 }
    }]
  }
  chart.setOption(option, true)
}

function selectDevice(id) {
  selected.value = devices.value.find(d => d.deviceId === id) || null
  renderChart()
}

// ---------- 生命周期 ----------
function tick() {
  const d = new Date()
  now.value = [d.getHours(), d.getMinutes(), d.getSeconds()].map(n => String(n).padStart(2, '0')).join(':')
}

onMounted(async () => {
  await Promise.all([loadSensors(), loadOverview(), loadDevices()])
  clockTimer = setInterval(tick, 1000)
  tick()
  pollTimer = setInterval(reload, 30000)
  window.addEventListener('resize', onResize)

  connectWS()
  offWS = onWSMessage(() => {
    // WS 数据/告警消息 -> 2s 节流刷新（保持实时又不打爆接口）
    clearTimeout(refreshTimer)
    refreshTimer = setTimeout(reload, 2000)
  })
})

function onResize() {
  chart?.resize()
}

onUnmounted(() => {
  clearInterval(pollTimer)
  clearInterval(clockTimer)
  clearTimeout(refreshTimer)
  window.removeEventListener('resize', onResize)
  if (offWS) offWS()
  chart?.dispose()
  chart = null
})
</script>


<style scoped>
.situation-page {
  padding: 24px 28px 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

/* 状态横幅 */
.status-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.4px;
  background: rgba(126, 226, 184, 0.08);
  border: 1px solid rgba(126, 226, 184, 0.25);
  color: #7EE2B8;
}
.status-banner.alert {
  background: rgba(226, 109, 109, 0.1);
  border-color: rgba(226, 109, 109, 0.35);
  color: #E26D6D;
}
.status-banner .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 8px currentColor;
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}
.banner-text { flex: 1; }
.banner-time { font-weight: 400; opacity: 0.7; font-size: 12px; }

/* 统计 chips */
.stat-row { display: flex; gap: 12px; }
.chip {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.07);
}
.chip-num { font-size: 22px; font-weight: 700; color: #E8ECE9; }
.chip-label { font-size: 12px; color: #56615C; }
.chip.online .chip-num { color: #7EE2B8; }
.chip.offline .chip-num { color: #3A423E; }
.chip.warn .chip-num { color: #E5B567; }
.chip.alarm .chip-num { color: #E26D6D; }

/* 筛选栏 */
.filter-bar { display: flex; gap: 10px; align-items: center; }
.f-keyword { width: 220px; }
.f-select { width: 130px; }
.f-refresh { color: #7EE2B8; }

/* 主体 */
.main-area {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}
.chart-wrap {
  position: relative;
  flex: 1;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  background:
    radial-gradient(1200px 500px at 20% 0%, rgba(126, 226, 184, 0.05), transparent 60%),
    radial-gradient(900px 400px at 85% 100%, rgba(124, 167, 255, 0.05), transparent 60%),
    rgba(255, 255, 255, 0.02);
  overflow: hidden;
}
.chart { width: 100%; height: 100%; min-height: 380px; }
.chart-empty {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  color: #56615C; font-size: 14px;
}
.chart-hint {
  position: absolute; right: 12px; bottom: 10px;
  font-size: 11px; color: rgba(86, 97, 92, 0.8);
  background: rgba(0, 0, 0, 0.25);
  padding: 3px 8px; border-radius: 6px;
}
.chart-wrap.with-detail { flex: 0 0 62%; }

/* 详情卡片 */
.detail-card {
  width: 300px;
  flex: 0 0 300px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.09);
  background: rgba(255, 255, 255, 0.03);
  padding: 16px;
  overflow-y: auto;
  max-height: 100%;
}
.detail-head { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.detail-title { font-size: 15px; font-weight: 700; color: #E8ECE9; flex: 1; }
.detail-type {
  font-size: 11px; color: #7CA7FF;
  border: 1px solid rgba(124, 167, 255, 0.3);
  padding: 1px 7px; border-radius: 20px;
}
.detail-close { color: #56615C; }
.detail-rows { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.d-row { display: flex; gap: 8px; font-size: 12px; }
.d-label { width: 62px; color: #56615C; flex-shrink: 0; }
.d-value { color: #C7CECB; word-break: break-all; }
.health-score { font-size: 16px; font-weight: 700; margin-right: 6px; }
.health-level { font-size: 11px; margin-right: 8px; }
.health-bar {
  display: inline-block; vertical-align: middle;
  width: 70px; height: 4px; border-radius: 2px;
  background: rgba(255, 255, 255, 0.08); overflow: hidden;
}
.health-bar-inner { display: block; height: 100%; border-radius: 2px; }
.detail-section {
  font-size: 12px; color: #7EE2B8; margin: 10px 0 8px;
  letter-spacing: 0.5px;
}
.value-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.value-table th {
  text-align: left; color: #56615C; font-weight: 500;
  padding: 4px 6px; border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}
.value-table td { padding: 5px 6px; color: #C7CECB; }
.value-table tr.over td { color: #FF9F0A; }
.value-table .num { font-variant-numeric: tabular-nums; }
.value-table .time { color: #56615C; font-size: 11px; }
.empty-cell { color: #56615C; text-align: center; padding: 12px 0; }
.alert-list { display: flex; flex-direction: column; gap: 6px; }
.alert-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 12px; padding: 6px 8px;
  border-radius: 8px;
  background: rgba(226, 109, 109, 0.06);
  border: 1px solid rgba(226, 109, 109, 0.15);
}
.alert-msg { flex: 1; color: #E8ECE9; }
.alert-time { font-size: 11px; color: #56615C; }
.no-alert { font-size: 12px; color: #7EE2B8; padding: 6px 0; }
</style>
