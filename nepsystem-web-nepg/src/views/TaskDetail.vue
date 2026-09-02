<template>
  <div class="detail-page">
    <el-button link :icon="'ArrowLeft'" @click="$router.back()" class="back-btn">返回</el-button>

    <template v-if="task.id">
      <!-- 任务信息 -->
      <section class="card">
        <div class="card-head">
          <el-tag :type="statusTag(task.status)" round>{{ statusText(task.status) }}</el-tag>
          <span class="no">{{ task.taskNo }}</span>
        </div>
        <div class="info-rows">
          <div class="info-row"><span class="k">污染类型</span><span class="v">{{ eventTypeText }}</span></div>
          <div class="info-row"><span class="k">地址</span><span class="v">{{ eventLocation }}</span></div>
          <div class="info-row"><span class="k">优先级</span><span class="v"><el-tag :type="priorityTag(task.priority)" size="small">{{ priorityText(task.priority) }}</el-tag></span></div>
          <div class="info-row"><span class="k">截止时间</span><span class="v">{{ fmtDateTime(task.deadline) }}</span></div>
          <div class="info-row"><span class="k">任务说明</span><span class="v">{{ task.result || '-' }}</span></div>
        </div>
      </section>

      <!-- 公众反馈 -->
      <section v-if="event" class="card">
        <div class="card-label">公众反馈</div>
        <div class="feedback">
          <div class="fb-title">{{ event.title }}</div>
          <div class="fb-desc">{{ event.description || '-' }}</div>
          <div class="fb-meta">{{ event.eventNo }} · {{ event.submitterName || '匿名' }} · {{ fmtTime(event.createTime) }}</div>
        </div>
        <div v-if="eventAttachments.length" class="img-grid">
          <div v-for="(a, i) in eventAttachments" :key="i" class="img-item">
            <img v-if="a.filePath" :src="a.filePath" :alt="a.fileName" />
            <div v-else class="img-placeholder">{{ a.fileName }}</div>
          </div>
        </div>
        <div class="fb-loc" v-if="event.location">📍 {{ event.location }}</div>
      </section>

      <!-- 当前AQI + 历史AQI -->
      <section class="card">
        <div class="card-label">当前 AQI（{{ currentDeviceName }}）</div>
        <div class="aqi-big" :style="{ color: aqiColor }">{{ aqiOverall ?? '--' }}</div>
        <div class="trend-title">历史 AQI 趋势（近24小时）</div>
        <div ref="trendRef" class="trend-chart"></div>
        <div v-if="!hasTrend" class="empty">暂无趋势数据</div>
      </section>

      <!-- 位置示意地图（Phase 10：真实坐标散点，无坐标回退示意布局） -->
      <section class="card">
        <div class="card-label">位置示意</div>
        <div ref="mapRef" class="map-chart"></div>
        <div class="map-hint">{{ hasCoords ? '坐标模式（任务/事件/当前位置）' : '示意布局（未配置坐标）' }}</div>
      </section>

      <!-- 附近设备 -->
      <section class="card">
        <div class="card-label">附近设备</div>
        <div v-if="nearby.length" class="dev-list">
          <div v-for="d in nearby" :key="d.id" class="dev-item">
            <span class="dev-dot" :class="d.status === 1 ? 'on' : 'off'"></span>
            <div class="dev-info">
              <div class="dev-name">{{ d.deviceName }}</div>
              <div class="dev-meta">{{ d.location || '-' }}</div>
            </div>
          </div>
        </div>
        <div v-else class="empty">暂无设备数据</div>
      </section>

      <!-- 状态操作按钮 -->
      <section class="action-bar">
        <el-button v-if="task.status === 'ASSIGNED'" type="primary" size="large" class="action-btn" :loading="acting" @click="onAccept">接收任务</el-button>
        <el-button v-if="task.status === 'ACCEPTED'" type="primary" size="large" class="action-btn" :loading="acting" @click="onStart">开始检测</el-button>
        <el-button v-if="task.status === 'INSPECTING'" type="success" size="large" class="action-btn" @click="detectVisible = true">提交检测</el-button>
        <el-button v-if="task.status === 'INSPECTED'" type="success" size="large" class="action-btn" disabled>检测已完成</el-button>
      </section>
    </template>

    <!-- 现场检测对话框 -->
    <el-dialog v-model="detectVisible" title="现场检测" width="92%" top="6vh" class="detect-dialog">
      <div class="detect-aqi" v-if="previewAQI !== null">
        实时 AQI：<b :style="{ color: previewAQI > 50 ? '#E26D6D' : '#7EE2B8' }">{{ previewAQI }}</b>
        <span class="aqi-tip">（按国标 HJ633-2012 1小时均值表计算，缺项不参与）</span>
      </div>
      <el-form :model="form" label-width="92px" label-position="left">
        <el-form-item label="PM2.5 (μg/m³)">
          <el-input-number v-model="form.pm25" :min="0" :max="5000" :precision="1" style="width: 100%" placeholder="如 55.0" />
        </el-form-item>
        <el-form-item label="PM10 (μg/m³)">
          <el-input-number v-model="form.pm10" :min="0" :max="5000" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="SO₂ (μg/m³)">
          <el-input-number v-model="form.so2" :min="0" :max="5000" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="NO₂ (μg/m³)">
          <el-input-number v-model="form.no2" :min="0" :max="5000" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="CO (mg/m³)">
          <el-input-number v-model="form.co" :min="0" :max="1000" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="O₃ (μg/m³)">
          <el-input-number v-model="form.o3" :min="0" :max="5000" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="检测位置">
          <el-button :icon="'Location'" size="small" @click="getLocation">获取当前位置</el-button>
          <span class="pos-text" v-if="position">{{ position.lat.toFixed(6) }}, {{ position.lng.toFixed(6) }}</span>
        </el-form-item>
        <el-form-item label="现场照片">
          <div class="upload-grid">
            <div v-for="(img, i) in previews" :key="i" class="upload-item">
              <img :src="img.url" alt="现场照片" />
              <span class="remove" @click="removeImage(i)">×</span>
            </div>
            <label v-if="previews.length < 6" class="upload-add">
              <input type="file" accept="image/*" multiple hidden @change="onPick" />
              <el-icon :size="20"><Plus /></el-icon>
              <span>拍照/添加</span>
            </label>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.content" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="现场情况描述、检测说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detectVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="onSubmit">提交检测记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { Plus } from '@element-plus/icons-vue'
import { getMyTaskDetail, acceptTask, startTask, submitDetect, getDevicesPage, getQuality, getTrend, getSensors, uploadFile } from '@/api'
import { statusText, statusTag, typeText, priorityText, priorityTag, fmtTime, fmtDateTime, calcAQI } from '@/utils/format'

const route = useRoute()
const task = ref({})
const event = ref(null)
const eventAttachments = ref([])
const devices = ref([])
const sensors = ref([])
const aqiOverall = ref(null)
const hasTrend = ref(false)
const trendRef = ref()
const mapRef = ref()
const nearby = ref([])
const acting = ref(false)
let chart = null
let mapChart = null

const detectVisible = ref(false)
const submitting = ref(false)
const previews = ref([])
const position = ref(null)
const form = reactive({ pm25: null, pm10: null, so2: null, no2: null, co: null, o3: null, content: '' })

const eventTypeText = computed(() => typeText(event.value?.eventType || 'OTHER'))
const eventLocation = computed(() => event.value?.location || task.value.deviceName || task.value.gridName || '-')
const currentDeviceId = computed(() => task.value.deviceId || null)
const currentDeviceName = computed(() => {
  const d = devices.value.find(x => x.id === currentDeviceId.value)
  return d ? d.deviceName : '—'
})
const previewAQI = computed(() => calcAQI(form))
const aqiColor = computed(() => {
  const v = aqiOverall.value
  if (v === null || v === undefined) return 'var(--text-muted)'
  return v > 50 ? '#E26D6D' : 'var(--env-green)'
})

async function onPick(e) {
  const files = Array.from(e.target.files || [])
  const pending = []
  for (const f of files) {
    if (previews.value.length + pending.length >= 6) break
    pending.push(f)
  }
  e.target.value = ''
  for (const f of pending) {
    try {
      const data = await uploadFile(f)
      previews.value.push({ url: data.url, file: f })
    } catch (err) {
      ElMessage.error('照片上传失败：' + f.name)
    }
  }
}
function removeImage(i) {
  previews.value.splice(i, 1)
}
function getLocation() {
  if (!navigator.geolocation) { ElMessage.warning('当前浏览器不支持定位'); return }
  navigator.geolocation.getCurrentPosition(
    pos => {
      position.value = { lat: pos.coords.latitude, lng: pos.coords.longitude }
      ElMessage.success('已获取当前位置')
      renderMap()
    },
    () => ElMessage.warning('定位失败，请检查权限')
  )
}

async function load() {
  try {
    const d = await getMyTaskDetail(route.params.id)
    task.value = d.task || {}
    event.value = d.event || null
    eventAttachments.value = d.eventAttachments || []
    if (event.value?.deviceId && !task.value.deviceId) task.value.deviceId = event.value.deviceId
  } catch (e) { /* 拦截器已提示 */ }
}

async function loadDevices() {
  try {
    devices.value = (await getDevicesPage({ page: 1, size: 100 })).records || []
    if (task.value.deviceId) {
      const self = devices.value.find(x => x.id === task.value.deviceId)
      nearby.value = devices.value.filter(x => x.id !== task.value.deviceId).slice(0, 5)
      if (self) nearby.value.unshift(self)
    } else {
      nearby.value = devices.value.slice(0, 5)
    }
  } catch (e) { /* 忽略 */ }
  renderMap()
}

async function loadAqi() {
  if (!currentDeviceId.value) return
  try {
    const q = await getQuality({ deviceId: currentDeviceId.value })
    if (q && q.overall !== null && q.overall !== undefined) aqiOverall.value = q.overall
  } catch (e) { /* 忽略 */ }
  try {
    sensors.value = (await getSensors()) || []
  } catch (e) { /* 忽略 */ }
  try {
    const std = {}
    for (const s of sensors.value) std[s.sensorCode] = Number(s.standardMax)
    const d24 = new Date(Date.now() - 86400000)
    const start = d24.getFullYear() + '-' + String(d24.getMonth() + 1).padStart(2, '0') + '-' + String(d24.getDate()).padStart(2, '0') + ' ' + String(d24.getHours()).padStart(2, '0') + ':' + String(d24.getMinutes()).padStart(2, '0') + ':00'
    const seriesMap = {}
    const codes = sensors.value.map(s => s.sensorCode)
    for (const code of codes) {
      const rows = (await getTrend({ deviceId: currentDeviceId.value, sensorCode: code, interval: 'hour', start })) || []
      for (const r of rows) {
        const t = String(r.t).replace('T', ' ').slice(0, 13)
        if (!seriesMap[t]) seriesMap[t] = []
        const stdv = std[code]
        if (stdv > 0) seriesMap[t].push(Number(r.avg_value) / stdv * 100)
      }
    }
    const times = Object.keys(seriesMap).sort()
    const aqiSeries = times.map(t => {
      const vals = seriesMap[t]
      return vals.length ? Math.round(vals.reduce((a, b) => a + b, 0) / vals.length * 10) / 10 : null
    })
    hasTrend.value = times.length > 1
    if (chart && hasTrend.value) {
      chart.setOption({
        backgroundColor: 'transparent',
        grid: { left: 36, right: 10, top: 12, bottom: 22 },
        xAxis: { type: 'category', data: times.map(t => t.slice(5)), axisLine: { lineStyle: { color: 'rgba(255,255,255,0.12)' } }, axisLabel: { color: '#56615C', fontSize: 10 } },
        yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }, axisLabel: { color: '#56615C', fontSize: 10 } },
        series: [{ type: 'line', data: aqiSeries, smooth: true, showSymbol: false, lineStyle: { width: 2, color: '#7CA7FF' }, areaStyle: { opacity: 0.08, color: '#7CA7FF' } }]
      })
    }
  } catch (e) { /* 忽略 */ }
}

const hasCoords = computed(() => {
  const pts = mapPoints.value
  return pts.some(p => p.lat != null && p.lng != null)
})

const mapPoints = computed(() => {
  const pts = []
  // 任务关联设备坐标
  const dev = devices.value.find(x => x.id === currentDeviceId.value)
  if (dev && dev.lat != null && dev.lng != null) {
    pts.push({ name: dev.deviceName, lat: Number(dev.lat), lng: Number(dev.lng), kind: 'device' })
  }
  // 事件坐标
  if (event.value && event.value.lat != null && event.value.lng != null) {
    pts.push({ name: '事件位置', lat: Number(event.value.lat), lng: Number(event.value.lng), kind: 'event' })
  }
  // 当前位置
  if (position.value) {
    pts.push({ name: '当前位置', lat: position.value.lat, lng: position.value.lng, kind: 'me' })
  }
  return pts
})

function renderMap() {
  if (!mapRef.value) return
  if (!mapChart) mapChart = echarts.init(mapRef.value)
  const pts = mapPoints.value
  if (hasCoords.value && pts.length) {
    // 坐标模式：真实 lng/lat 散点
    const xs = pts.map(p => p.lng)
    const ys = pts.map(p => p.lat)
    const padX = (Math.max(...xs) - Math.min(...xs)) * 0.3 || 0.01
    const padY = (Math.max(...ys) - Math.min(...ys)) * 0.3 || 0.01
    mapChart.setOption({
      backgroundColor: 'transparent',
      grid: { left: 8, right: 8, top: 8, bottom: 8 },
      xAxis: { type: 'value', min: Math.min(...xs) - padX, max: Math.max(...xs) + padX, show: false },
      yAxis: { type: 'value', min: Math.min(...ys) - padY, max: Math.max(...ys) + padY, show: false, inverse: false },
      series: [{
        type: 'effectScatter', coordinateSystem: 'cartesian2d',
        rippleEffect: { brushType: 'stroke', scale: 2.6, period: 3 },
        data: pts.map(p => ({
          name: p.name, value: [p.lng, p.lat],
          itemStyle: {
            color: p.kind === 'me' ? '#F5A623' : (p.kind === 'event' ? '#E26D6D' : '#4ECB8D'),
            shadowBlur: 12,
            shadowColor: p.kind === 'me' ? 'rgba(245,166,35,0.6)' : 'rgba(126,226,184,0.5)'
          },
          label: { show: true, position: 'bottom', distance: 4, formatter: p.name, color: '#89938E', fontSize: 10 }
        }))
      }]
    })
  } else {
    // 示意布局：按类型三列抽象
    const typeX = { AIR: 18, WATER: 50, NOISE: 82 }
    const col = {}
    const fake = (devices.value.length ? devices.value : [{ id: 0, type: 'AIR', deviceName: '监测点' }]).map((d, i) => {
      const x = typeX[d.type] || 50
      const idx = col[x] || 0
      col[x] = idx + 1
      return { name: d.deviceName, value: [x, 85 - idx * 30] }
    })
    mapChart.setOption({
      backgroundColor: 'transparent',
      grid: { left: 8, right: 8, top: 8, bottom: 8 },
      xAxis: { type: 'value', min: 0, max: 100, show: false },
      yAxis: { type: 'value', min: 0, max: 100, show: false, inverse: true },
      series: [{
        type: 'scatter', coordinateSystem: 'cartesian2d', symbolSize: 12,
        data: fake,
        itemStyle: { color: 'rgba(78, 203, 141, 0.7)' },
        label: { show: true, position: 'bottom', formatter: p => p.data.name, color: '#56615C', fontSize: 10 }
      }]
    })
  }
}

async function onAccept() {
  await ElMessageBox.confirm('确认接收该任务？', '接收任务', { type: 'info' }).then(async () => {
    acting.value = true
    try { await acceptTask(route.params.id); ElMessage.success('已接收任务'); load() } catch (e) { /* 拦截器已提示 */ } finally { acting.value = false }
  }).catch(() => {})
}
async function onStart() {
  acting.value = true
  try { await startTask(route.params.id); ElMessage.success('已开始检测'); load() } catch (e) { /* 拦截器已提示 */ } finally { acting.value = false }
}
async function onSubmit() {
  if (previewAQI.value === null) { ElMessage.warning('请至少填写一项污染物检测值'); return }
  submitting.value = true
  try {
    await submitDetect(route.params.id, {
      pm25: form.pm25, pm10: form.pm10, so2: form.so2, no2: form.no2, co: form.co, o3: form.o3,
      content: form.content,
      images: previews.value.map(img => img.url || (img.file ? img.file.name : '')),
      lat: position.value ? Number(position.value.lat.toFixed(7)) : null,
      lng: position.value ? Number(position.value.lng.toFixed(7)) : null
    })
    ElMessage.success('检测已提交')
    detectVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally { submitting.value = false }
}

onMounted(async () => {
  await load()
  if (trendRef.value) chart = echarts.init(trendRef.value)
  await Promise.all([loadDevices(), loadAqi()])
})
onUnmounted(() => {
  chart?.dispose()
  mapChart?.dispose()
})
</script>

<style scoped>
.detail-page { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.back-btn { margin-bottom: 0; align-self: flex-start; }
.card { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 16px; padding: 16px; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.no { font-size: 11px; color: var(--text-faint); }
.card-label { font-size: 12px; font-weight: 600; letter-spacing: 0.1em; color: var(--text-secondary); margin-bottom: 10px; }
.info-rows { display: flex; flex-direction: column; gap: 9px; }
.info-row { display: flex; gap: 12px; font-size: 13px; }
.info-row .k { color: var(--text-muted); flex-shrink: 0; width: 64px; }
.info-row .v { line-height: 1.5; word-break: break-all; }

.feedback { background: var(--bg-inset); border-radius: 10px; padding: 12px; margin-bottom: 10px; }
.fb-title { font-size: 14px; font-weight: 600; margin-bottom: 4px; }
.fb-desc { font-size: 12px; color: var(--text-secondary); line-height: 1.5; }
.fb-meta { font-size: 11px; color: var(--text-faint); margin-top: 6px; }
.fb-loc { font-size: 12px; color: var(--text-secondary); margin-top: 8px; }
.img-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.img-item { width: 76px; height: 76px; border-radius: 10px; overflow: hidden; border: 1px solid var(--border-subtle); }
.img-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.img-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; font-size: 9px; color: var(--text-muted); background: var(--bg-inset); text-align: center; padding: 4px; box-sizing: border-box; }

.aqi-big { font-size: 42px; font-weight: 700; line-height: 1; margin-bottom: 10px; }
.trend-title { font-size: 11px; color: var(--text-muted); margin: 10px 0 6px; }
.trend-chart { height: 150px; }
.map-chart { height: 200px; background: linear-gradient(180deg, rgba(78, 203, 141, 0.04), transparent), var(--bg-inset); border-radius: 12px; }
.map-hint { font-size: 10px; color: var(--text-faint); margin-top: 8px; text-align: center; letter-spacing: 0.06em; }

.dev-list { display: flex; flex-direction: column; }
.dev-item { display: flex; align-items: center; gap: 8px; padding: 8px 2px; border-bottom: 1px solid rgba(255,255,255,0.05); }
.dev-item:last-child { border-bottom: none; }
.dev-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.dev-dot.on { background: var(--env-green); }
.dev-dot.off { background: var(--text-faint); }
.dev-info { flex: 1; min-width: 0; }
.dev-name { font-size: 12px; }
.dev-meta { font-size: 11px; color: var(--text-muted); margin-top: 1px; }

.action-bar { display: flex; gap: 10px; padding: 4px 0 8px; }
.action-btn { flex: 1; height: 48px; border-radius: 14px; font-size: 16px; font-weight: 600; letter-spacing: 0.1em; }

.detect-aqi { background: var(--bg-inset); border-radius: 10px; padding: 12px 14px; margin-bottom: 12px; font-size: 15px; }
.aqi-tip { font-size: 10px; color: var(--text-faint); margin-left: 6px; }
.pos-text { font-size: 12px; color: var(--text-secondary); margin-left: 10px; }
.upload-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.upload-item { position: relative; width: 72px; height: 72px; border-radius: 10px; overflow: hidden; border: 1px solid var(--border-subtle); }
.upload-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.upload-item .remove { position: absolute; top: 2px; right: 2px; width: 16px; height: 16px; line-height: 14px; text-align: center; background: rgba(0,0,0,0.6); color: #fff; border-radius: 50%; font-size: 12px; cursor: pointer; }
.upload-add { width: 72px; height: 72px; border-radius: 10px; border: 1px dashed var(--border-soft); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; color: var(--text-muted); font-size: 10px; cursor: pointer; }
.empty { text-align: center; padding: 14px 0; font-size: 12px; color: var(--text-faint); }
</style>