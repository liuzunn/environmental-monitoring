<template>
  <div class="detail-page">
    <el-button link :icon="'ArrowLeft'" @click="$router.back()" class="back-btn">返回</el-button>
    <div v-if="detail.event" class="detail-grid">
      <div class="left-col">
        <!-- 事件信息 -->
        <section class="card">
          <div class="card-head">
            <el-tag :type="statusTag(detail.event.status)" round>{{ statusText(detail.event.status) }}</el-tag>
            <span class="no">{{ detail.event.eventNo }}</span>
          </div>
          <h2 class="title">{{ detail.event.title }}</h2>
          <div class="info-rows">
            <div class="info-row"><span class="k">污染类型</span><span class="v">{{ typeText(detail.event.eventType) }}</span></div>
            <div class="info-row"><span class="k">严重程度</span><span class="v">{{ levelText(detail.event.level) }}</span></div>
            <div class="info-row"><span class="k">公众信息</span><span class="v">{{ detail.event.submitterName || '匿名' }}（ID: {{ detail.event.submitterId || '-' }}）</span></div>
            <div class="info-row"><span class="k">位置</span><span class="v">{{ detail.event.location || '-' }}{{ posTail }}</span></div>
            <div class="info-row"><span class="k">提交时间</span><span class="v">{{ fmtTime(detail.event.createTime) }}</span></div>
            <div class="info-row"><span class="k">问题描述</span><span class="v">{{ detail.event.description || '-' }}</span></div>
          </div>
          <div class="actions" v-if="['PENDING_REVIEW','APPROVED','INSPECTED','VERIFIED'].includes(detail.event.status)">
            <el-button v-if="detail.event.status === 'PENDING_REVIEW'" type="success" @click="onApprove">审核通过</el-button>
            <el-button v-if="detail.event.status === 'PENDING_REVIEW'" type="danger" @click="onReject">驳回</el-button>
            <el-button v-if="detail.event.status === 'APPROVED'" type="warning" @click="openAssign">派单</el-button>
            <el-button v-if="detail.event.status === 'INSPECTED'" type="success" :loading="verifying" @click="onVerify">核实结果</el-button>
            <el-button v-if="detail.event.status === 'VERIFIED'" type="primary" :loading="verifying" @click="onClose">关闭事件</el-button>
          </div>
        </section>

        <!-- 现场图片 -->
        <section class="card">
          <div class="card-label">现场图片</div>
          <div v-if="detail.attachments.length" class="img-grid">
            <el-image v-for="(a, i) in detail.attachments" :key="i" class="img-item" fit="cover"
              :src="a.filePath || ''" :preview-src-list="imgList">
              <template #error>
                <div class="img-placeholder">{{ a.fileName }}<span class="pending">未上传</span></div>
              </template>
            </el-image>
          </div>
          <div v-else class="empty">未上传图片</div>
        </section>

        <!-- 检测结果（Phase 6） -->
        <section v-if="detail.taskId" class="card">
          <div class="card-label">检测结果（{{ detail.taskNo }}）</div>
          <div v-if="records.length" class="record-box">
            <div v-for="rec in records" :key="rec.id" class="record-item">
              <div class="record-grid">
                <div class="rec-cell"><span class="rec-k">PM2.5</span><span class="rec-v">{{ rec.pm25 ?? '-' }}</span></div>
                <div class="rec-cell"><span class="rec-k">PM10</span><span class="rec-v">{{ rec.pm10 ?? '-' }}</span></div>
                <div class="rec-cell"><span class="rec-k">SO₂</span><span class="rec-v">{{ rec.so2 ?? '-' }}</span></div>
                <div class="rec-cell"><span class="rec-k">NO₂</span><span class="rec-v">{{ rec.no2 ?? '-' }}</span></div>
                <div class="rec-cell"><span class="rec-k">CO</span><span class="rec-v">{{ rec.co ?? '-' }}</span></div>
                <div class="rec-cell"><span class="rec-k">O₃</span><span class="rec-v">{{ rec.o3 ?? '-' }}</span></div>
                <div class="rec-cell aqi"><span class="rec-k">AQI</span><span class="rec-v" :style="{ color: rec.aqiValue > 50 ? '#E26D6D' : '#7EE2B8' }">{{ rec.aqiValue ?? '-' }}</span></div>
                <div class="rec-cell"><span class="rec-k">时间</span><span class="rec-v">{{ fmtTime(rec.createTime) }}</span></div>
              </div>
              <div class="rec-note">{{ rec.content || '-' }}</div>
              <div class="rec-meta">检测人：{{ rec.recorderName || '-' }} · 位置：{{ rec.lat != null ? rec.lat + ', ' + rec.lng : '未记录' }} · 照片：{{ rec.images ? rec.images.join('、') : '无' }}</div>
            </div>
          </div>
          <div v-else class="empty">暂无检测记录</div>
        </section>

        <!-- 处理时间线 -->
        <section class="card">
          <div class="card-label">处理时间线</div>
          <el-timeline v-if="detail.statusLogs.length">
            <el-timeline-item v-for="(log, i) in detail.statusLogs" :key="i" :timestamp="fmtTime(log.createTime)" :type="log.toStatus === 'REJECTED' ? 'danger' : 'primary'">
              <div>{{ statusText(log.toStatus) }}<span v-if="log.fromStatus" class="from">（由 {{ statusText(log.fromStatus) }}）</span></div>
              <div class="log-remark">{{ log.remark || '—' }} · {{ log.operatorName || '系统' }}</div>
            </el-timeline-item>
          </el-timeline>
          <div v-else class="empty">暂无流转记录</div>
        </section>
      </div>

      <div class="right-col">
        <!-- 当前AQI -->
        <section class="card">
          <div class="card-label">当前 AQI（{{ currentDeviceName }}）</div>
          <div class="aqi-big" :style="{ color: aqiColor }">{{ aqiOverall ?? '--' }}</div>
          <div class="aqi-sub">得分 0-100 · 越高越差（{{ aqiOverall > 50 ? '超标风险' : '良好' }}）</div>
        </section>

        <!-- 历史AQI趋势 -->
        <section class="card">
          <div class="card-label">历史 AQI 趋势（近24小时）</div>
          <div ref="trendRef" class="trend-chart"></div>
          <div v-if="!hasTrend" class="empty">该设备暂无趋势数据</div>
        </section>

        <!-- 附近设备 -->
        <section class="card">
          <div class="card-label">附近设备</div>
          <div v-if="nearbyDevices.length" class="dev-list">
            <div v-for="d in nearbyDevices" :key="d.id" class="dev-item">
              <span class="dev-dot" :class="d.status === 1 ? 'on' : 'off'"></span>
              <div class="dev-info">
                <div class="dev-name">{{ d.deviceName }}</div>
                <div class="dev-meta">{{ typeText(d.type) }} · {{ d.location || '-' }}</div>
              </div>
              <el-tag size="small" effect="plain">{{ d.id === detail.event.deviceId ? '关联' : '附近' }}</el-tag>
            </div>
          </div>
          <div v-else class="empty">暂无设备数据</div>
        </section>

        <!-- 设备告警 -->
        <section class="card">
          <div class="card-label">设备告警（{{ currentDeviceName }}）</div>
          <div v-if="alerts.length" class="dev-list">
            <div v-for="a in alerts" :key="a.id" class="dev-item">
              <el-tag :type="a.level === 'ALARM' ? 'danger' : 'warning'" size="small">{{ levelText(a.level) }}</el-tag>
              <div class="dev-info">
                <div class="dev-name">{{ a.message }}</div>
                <div class="dev-meta">{{ a.sensorCode }} · {{ fmtTime(a.createTime) }}</div>
              </div>
            </div>
          </div>
          <div v-else class="empty">该设备暂无告警</div>
        </section>
      </div>
    </div>

    <!-- 派单对话框（同列表页） -->
    <el-dialog v-model="assignVisible" title="事件派单" width="480px">
      <el-form :model="assignForm" label-width="90px">
        <el-form-item label="所属网格">
          <el-select v-model="assignForm.gridId" placeholder="选择网格" style="width: 100%" @change="onGridChange">
            <el-option v-for="g in grids" :key="g.id" :label="g.gridName + ' (' + g.gridCode + ')'" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行网格员">
          <el-select v-model="assignForm.assigneeId" placeholder="选择网格员" style="width: 100%">
            <el-option v-for="m in members" :key="m.id" :label="(m.nickname || m.username) + '（' + m.gridName + '）'" :value="m.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="assignForm.priority">
            <el-radio-button value="LOW">低</el-radio-button>
            <el-radio-button value="MEDIUM">中</el-radio-button>
            <el-radio-button value="HIGH">高</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="assignForm.deadline" type="datetime" placeholder="选择截止时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="派单备注">
          <el-input v-model="assignForm.remark" type="textarea" :rows="2" placeholder="派单说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="onAssign">确认派单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { getEventDetail, approveEvent, rejectEvent, assignEvent, getGrids, getGridMembers, getDevicesPage, getQuality, getTrend, getSensors, getAlertsPage, getTaskRecords, verifyTask, closeTask } from '@/api'
import { statusText, statusTag, typeText, levelText, fmtTime } from '@/utils/format'

const route = useRoute()
const detail = ref({})
const devices = ref([])
const sensors = ref([])
const alerts = ref([])
const aqiOverall = ref(null)
const hasTrend = ref(false)
const trendRef = ref()
let chart = null
const grids = ref([])
const members = ref([])
const assignVisible = ref(false)
const assigning = ref(false)
const records = ref([])
const verifying = ref(false)
const assignForm = reactive({ gridId: null, assigneeId: null, priority: 'MEDIUM', deadline: null, remark: '' })

const currentDeviceId = computed(() => detail.value.event?.deviceId || nearbyDevices.value[0]?.id || null)
const currentDeviceName = computed(() => {
  const d = devices.value.find(x => x.id === currentDeviceId.value)
  return d ? d.deviceName : '—'
})
const nearbyDevices = computed(() => {
  const ev = detail.value.event
  if (!ev) return []
  const list = devices.value.filter(d => d.id !== ev.deviceId)
  if (ev.lat != null && ev.lng != null) {
    // 按坐标距离排序
    return list.slice().sort((a, b) => dist(a, ev) - dist(b, ev)).slice(0, 6)
  }
  if (ev.regionId != null) {
    return list.filter(d => d.regionId === ev.regionId).slice(0, 6)
  }
  return list.slice(0, 6)
})
const posTail = computed(() => {
  const ev = detail.value.event
  return ev && ev.lat != null && ev.lng != null ? '（' + ev.lat + ', ' + ev.lng + '）' : ''
})
const imgList = computed(() => detail.value.attachments?.filter(a => a.filePath).map(a => a.filePath) || [])
const aqiColor = computed(() => {
  const v = aqiOverall.value
  if (v === null || v === undefined) return 'var(--text-muted)'
  return v > 50 ? '#E26D6D' : 'var(--env-green)'
})

function dist(d, ev) {
  if (d.lat == null || d.lng == null) return 1e9
  const dx = Number(d.lat) - Number(ev.lat)
  const dy = Number(d.lng) - Number(ev.lng)
  return Math.sqrt(dx * dx + dy * dy)
}

async function load() {
  try {
    detail.value = await getEventDetail(route.params.id)
    if (detail.value.taskId) {
      try { records.value = (await getTaskRecords(detail.value.taskId)) || [] } catch (e) { /* 忽略 */ }
    } else {
      records.value = []
    }
  } catch (e) { /* 拦截器已提示 */ }
}

async function onVerify() {
  await ElMessageBox.confirm('确认核实检测结果？核实后事件进入 VERIFIED。', '核实确认', { type: 'info' }).then(async () => {
    verifying.value = true
    try { await verifyTask(detail.value.taskId, '结果核实通过'); ElMessage.success('已核实'); load() } catch (e) { /* 拦截器已提示 */ } finally { verifying.value = false }
  }).catch(() => {})
}

async function onClose() {
  await ElMessageBox.confirm('确认关闭该事件？关闭后事件进入 CLOSED（已完成）。', '关闭确认', { type: 'warning' }).then(async () => {
    verifying.value = true
    try { await closeTask(detail.value.taskId, '事件处置完毕'); ElMessage.success('事件已关闭'); load() } catch (e) { /* 拦截器已提示 */ } finally { verifying.value = false }
  }).catch(() => {})
}

async function loadAux() {
  try { devices.value = (await getDevicesPage({ page: 1, size: 100 })).records || [] } catch (e) { /* 忽略 */ }
  try { sensors.value = (await getSensors()) || [] } catch (e) { /* 忽略 */ }
}

async function loadAqi() {
  if (!currentDeviceId.value) return
  try {
    const q = await getQuality({ deviceId: currentDeviceId.value })
    if (q && q.overall !== null && q.overall !== undefined) aqiOverall.value = q.overall
  } catch (e) { /* 忽略 */ }
  // 历史 AQI：各指标 trend(近24h) avg / standard_max * 100 平均（与后端 quality 算法一致）
  try {
    const codes = sensors.value.filter(s => !s.deviceType || !detail.value.event?.type || s.deviceType === detail.value.event?.type).map(s => s.sensorCode)
    const std = {}
    for (const s of sensors.value) std[s.sensorCode] = Number(s.standardMax)
    const seriesMap = {}
    for (const code of codes) {
      const d24 = new Date(Date.now() - 24 * 3600 * 1000)
      const start = d24.getFullYear() + '-' + String(d24.getMonth() + 1).padStart(2, '0') + '-' + String(d24.getDate()).padStart(2, '0') + ' ' + String(d24.getHours()).padStart(2, '0') + ':' + String(d24.getMinutes()).padStart(2, '0') + ':00'
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

async function loadAlerts() {
  if (!currentDeviceId.value) return
  try {
    const d = await getAlertsPage({ page: 1, size: 6, deviceId: currentDeviceId.value })
    alerts.value = d.records || []
  } catch (e) { /* 忽略 */ }
}

async function onApprove() {
  await ElMessageBox.confirm('确认审核通过？', '审核确认', { type: 'info' }).then(async () => {
    try { await approveEvent(route.params.id, null); ElMessage.success('已审核通过'); load() } catch (e) { /* 拦截器已提示 */ }
  }).catch(() => {})
}
async function onReject() {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回确认', { inputValidator: v => !!v || '请填写驳回原因' }).catch(() => ({}))
  if (!value) return
  try { await rejectEvent(route.params.id, value); ElMessage.success('已驳回'); load() } catch (e) { /* 拦截器已提示 */ }
}
async function openAssign() {
  Object.assign(assignForm, { gridId: null, assigneeId: null, priority: 'MEDIUM', deadline: null, remark: '' })
  try { grids.value = (await getGrids()) || [] } catch (e) { /* 忽略 */ }
  members.value = []
  assignVisible.value = true
}
async function onGridChange(gridId) {
  assignForm.assigneeId = null
  if (!gridId) { members.value = []; return }
  try { members.value = (await getGridMembers({ gridId })) || [] } catch (e) { /* 忽略 */ }
}
async function onAssign() {
  if (!assignForm.assigneeId) { ElMessage.warning('请选择执行网格员'); return }
  assigning.value = true
  try {
    await assignEvent(route.params.id, { ...assignForm })
    ElMessage.success('派单成功')
    assignVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally { assigning.value = false }
}

onMounted(async () => {
  await Promise.all([load(), loadAux()])
  if (trendRef.value) chart = echarts.init(trendRef.value)
  await Promise.all([loadAqi(), loadAlerts()])
})
onUnmounted(() => { chart?.dispose() })
</script>

<style scoped>
.back-btn { margin-bottom: 10px; }
.detail-grid { display: grid; grid-template-columns: 1fr 360px; gap: 14px; align-items: start; }
.left-col, .right-col { display: flex; flex-direction: column; gap: 14px; }
.card { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 14px; padding: 16px; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.no { font-size: 11px; color: var(--text-faint); }
.title { font-size: 17px; font-weight: 700; margin: 0 0 12px; }
.card-label { font-size: 12px; font-weight: 600; letter-spacing: 0.1em; color: var(--text-secondary); margin-bottom: 12px; }
.info-rows { display: flex; flex-direction: column; gap: 9px; }
.info-row { display: flex; gap: 12px; font-size: 13px; }
.info-row .k { color: var(--text-muted); flex-shrink: 0; width: 64px; }
.info-row .v { line-height: 1.5; word-break: break-all; }
.actions { margin-top: 14px; display: flex; gap: 10px; flex-wrap: wrap; }
.record-box { display: flex; flex-direction: column; gap: 10px; }
.record-item { background: var(--bg-inset); border-radius: 10px; padding: 12px; }
.record-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.rec-cell { display: flex; flex-direction: column; gap: 2px; }
.rec-k { font-size: 10px; color: var(--text-faint); }
.rec-v { font-size: 15px; font-weight: 600; }
.rec-cell.aqi .rec-v { font-size: 18px; }
.rec-note { font-size: 12px; color: var(--text-secondary); margin-top: 8px; }
.rec-meta { font-size: 11px; color: var(--text-faint); margin-top: 6px; }
.img-grid { display: flex; flex-wrap: wrap; gap: 10px; }
.img-item { width: 100px; height: 100px; border-radius: 10px; border: 1px solid var(--border-subtle); }
.img-placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; font-size: 10px; color: var(--text-muted); background: var(--bg-inset); }
.pending { font-size: 9px; background: rgba(255,255,255,0.06); padding: 1px 6px; border-radius: 999px; }
.from { font-size: 11px; color: var(--text-muted); }
.log-remark { font-size: 11px; color: var(--text-secondary); margin-top: 2px; }
.aqi-big { font-size: 44px; font-weight: 700; line-height: 1; }
.aqi-sub { font-size: 11px; color: var(--text-muted); margin-top: 6px; }
.trend-chart { height: 160px; }
.dev-list { display: flex; flex-direction: column; }
.dev-item { display: flex; align-items: center; gap: 8px; padding: 8px 2px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); font-size: 12px; }
.dev-item:last-child { border-bottom: none; }
.dev-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.dev-dot.on { background: var(--env-green); }
.dev-dot.off { background: var(--text-faint); }
.dev-info { flex: 1; min-width: 0; }
.dev-name { font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dev-meta { font-size: 11px; color: var(--text-muted); margin-top: 1px; }
.empty { text-align: center; padding: 16px 0; font-size: 12px; color: var(--text-faint); }
@media (max-width: 1100px) { .detail-grid { grid-template-columns: 1fr; } }
</style>