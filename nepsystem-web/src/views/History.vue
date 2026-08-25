<template>
  <div class="page-container">
    <div>
      <h2 class="page-title">历史数据</h2>
      <p class="page-subtitle">按设备与指标查询历史监测数据，支持趋势分析与 CSV 导出</p>
    </div>

    <!-- 筛选 + 趋势图（同一卡片：筛选为卡片工具条） -->
    <div class="apple-card trend-card">
      <div class="trend-head">
        <div class="trend-title">
          <span class="title-bar"></span>趋势分析
        </div>
        <div class="filter-bar">
          <el-select v-model="query.deviceId" placeholder="选择设备" style="width: 200px" clearable @change="loadAll">
            <el-option v-for="d in devices" :key="d.id" :label="d.deviceName" :value="d.id" />
          </el-select>
          <el-select v-model="query.sensorCodes" multiple placeholder="多选指标" style="width: 230px" collapse-tags>
            <el-option v-for="s in sensors" :key="s.sensorCode" :label="s.sensorName + ' (' + s.sensorCode + ')'" :value="s.sensorCode" />
          </el-select>
          <el-date-picker
            v-model="range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 340px"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
          <el-button type="primary" :icon="Search" @click="loadAll">查询</el-button>
          <el-button :icon="Download" @click="onExport">导出 CSV</el-button>
        </div>
      </div>
      <div class="chart-box" :class="{ 'is-empty': !hasTrend }" ref="chartRef"></div>
    </div>

    <!-- 明细表格 -->
    <div class="apple-card table-card">
      <div class="table-title">
        <span class="title-bar"></span>数据明细
      </div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="deviceId" label="设备ID" width="90" />
        <el-table-column prop="sensorCode" label="指标" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.sensorCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="value" label="数值" width="120">
          <template #default="{ row }">
            <span class="tabular-nums" :class="{ over: isOver(row) }">{{ row.value }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reportTime" label="上报时间" />
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadRows"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDevicesPage, getHistory, getTrend, getSensors, exportCsv } from '@/api'

const devices = ref([])
const sensors = ref([])
const query = ref({ deviceId: null, sensorCodes: [], start: null, end: null })
const range = ref(null)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(15)
const loading = ref(false)
const chartRef = ref()
let chart = null

/** 是否具备趋势图数据条件（避免未选择时渲染 320px 大空白） */
const hasTrend = computed(() => !!query.value.deviceId && query.value.sensorCodes.length > 0)

const standards = computed(() => {
  const m = {}
  for (const s of sensors.value) m[s.sensorCode] = s.standardMax
  return m
})

function isOver(row) {
  const std = standards.value[row.sensorCode]
  return std && Number(row.value) > Number(std)
}

function params(extra) {
  const p = { ...extra }
  if (query.value.deviceId) p.deviceId = query.value.deviceId
  if (query.value.start) p.start = query.value.start
  if (query.value.end) p.end = query.value.end
  return p
}

async function loadAll() {
  page.value = 1
  await Promise.all([loadTrend(), loadRows()])
}

async function loadRows() {
  loading.value = true
  try {
    const p = params({ page: page.value, size: size.value })
    if (query.value.sensorCodes.length === 1) p.sensorCode = query.value.sensorCodes[0]
    const d = await getHistory(p)
    rows.value = d.records || []
    total.value = Number(d.total || 0)
  } finally {
    loading.value = false
  }
}

async function loadTrend() {
  if (!chart) chart = echarts.init(chartRef.value)
  if (!hasTrend.value) {
    // 空态：紧凑提示，容器高度收缩，避免大块空白
    chart.clear()
    chart.setOption(emptyOption(), true)
    await nextTick()
    chart.resize()
    return
  }
  const series = []
  const xAxis = []
  const legend = []
  for (const code of query.value.sensorCodes) {
    const rows = await getTrend(params({ deviceId: query.value.deviceId, sensorCode: code, interval: 'hour' }))
    const pts = (rows || []).map(r => [String(r.t).replace('T', ' '), Number(r.avg_value)])
    if (xAxis.length === 0) xAxis.push(...pts.map(p => p[0]))
    legend.push(code)
    series.push({
      name: code, type: 'line', smooth: true, showSymbol: false,
      lineStyle: { width: 2 }, itemStyle: { color: colorOf(code) },
      areaStyle: { opacity: 0.08, color: colorOf(code) },
      data: pts
    })
  }
  // 容器高度可能在空态/数据态间切换，先等 DOM 高度变化再重绘
  // notMerge: 整体替换配置，避免空态 "暂无趋势数据" title 残留
  await nextTick()
  chart.resize()
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderWidth: 0,
      borderRadius: 10,
      padding: [8, 12],
      textStyle: { color: 'rgba(0,0,0,0.85)', fontSize: 12 },
      axisPointer: { lineStyle: { color: 'rgba(0,0,0,0.15)' } }
    },
    legend: { data: legend, top: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10 },
    grid: { left: 48, right: 20, top: 36, bottom: 28 },
    xAxis: {
      type: 'category', data: xAxis, boundaryGap: false,
      axisLine: { lineStyle: { color: 'rgba(0,0,0,0.1)' } },
      axisLabel: { color: 'rgba(0,0,0,0.45)', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(0,0,0,0.06)' } },
      axisLabel: { color: 'rgba(0,0,0,0.45)', fontSize: 11 }
    },
    series
  }, true)
}

function emptyOption() {
  return {
    title: {
      text: '暂无趋势数据',
      subtext: '请选择设备与指标后查看趋势',
      left: 'center', top: 'middle',
      textStyle: { color: '#8E8E93', fontSize: 15, fontWeight: 500 },
      subtextStyle: { color: '#AEAEB2', fontSize: 12 }
    },
    grid: {}, xAxis: { type: 'category', data: [] }, yAxis: { type: 'value' }, series: []
  }
}

function colorOf(code) {
  const palette = { TEMP: '#FF9F0A', HUMI: '#5AC8FA', PM25: '#FF453A', CO2: '#AF52DE', PH: '#30D158', TURBIDITY: '#64D2FF', DO: '#0A84FF', NOISE: '#FFD60A' }
  return palette[code] || '#007AFF'
}

async function onExport() {
  const p = params({})
  if (query.value.sensorCodes.length === 1) p.sensorCode = query.value.sensorCodes[0]
  const blob = await exportCsv(p)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'monitor_data.csv'
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(async () => {
  const [d, s] = await Promise.all([getDevicesPage({ page: 1, size: 100 }), getSensors()])
  devices.value = d.records || []
  sensors.value = s || []
  // 支持从大屏跳转带筛选：/history?deviceId=1&sensorCodes=TEMP,HUMI
  const q = useRoute().query
  if (q.deviceId !== undefined && q.deviceId !== '') {
    query.value.deviceId = Number(q.deviceId)
  } else if (devices.value.length) {
    query.value.deviceId = devices.value[0].id
  }
  if (q.sensorCodes) {
    query.value.sensorCodes = String(q.sensorCodes).split(',').filter(Boolean)
  }
  await loadAll()
  window.addEventListener('resize', onResize)
})

function onResize() {
  chart?.resize()
}

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<style scoped>
/* 趋势卡片：筛选工具条 + 图表一体 */
.trend-card {
  padding: var(--sp-20);
}

.trend-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-16);
  flex-wrap: wrap;
  margin-bottom: var(--sp-16);
}

.trend-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--fs-subhead);
  font-weight: var(--fw-semibold);
  color: var(--text-main);
  white-space: nowrap;
}

.title-bar {
  width: 3px;
  height: 14px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
}

.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.chart-box {
  height: 400px;
  transition: height var(--dur-slow) var(--ease-out);
}

/* 空态：收缩高度，消除大块空白 */
.chart-box.is-empty {
  height: 140px;
}

/* 明细表格卡片 */
.table-card {
  padding: var(--sp-20);
}

.table-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--fs-subhead);
  font-weight: var(--fw-semibold);
  color: var(--text-main);
  margin-bottom: var(--sp-12);
}

.over {
  color: #FF3B30;
  font-weight: 600;
}
</style>
