<template>
  <div class="home-page">
    <!-- AQI 卡片 -->
    <section class="aqi-card">
      <div class="aqi-head">
        <span class="aqi-label">当前环境质量</span>
        <span class="aqi-status">{{ aqiText() }}</span>
      </div>
      <div class="aqi-value">
        <span class="num tabular-nums">{{ qualityOverall ?? '--' }}</span>
        <span class="unit">/ 100</span>
      </div>
      <div class="aqi-foot">
        <span>{{ onlineDevices }} 个监测点在线 · 今日上报 {{ todayReports }} 条</span>
      </div>
    </section>

    <!-- 我要监督（突出入口） -->
    <section class="supervise-entry" @click="$router.push('/supervise')">
      <div class="entry-icon"><el-icon :size="26"><EditPen /></el-icon></div>
      <div class="entry-text">
        <div class="entry-title">我要监督</div>
        <div class="entry-sub">发现污染、噪声、设备异常？立即上报</div>
      </div>
      <el-icon :size="16" class="entry-arrow"><ArrowRight /></el-icon>
    </section>

    <!-- 我的监督数量 -->
    <section class="mine-card" @click="$router.push('/mine')">
      <div class="mine-left">
        <span class="mine-label">我的监督</span>
        <span class="mine-count tabular-nums">{{ mineTotal }}</span>
        <span class="mine-unit">条记录</span>
      </div>
      <el-icon :size="18" class="mine-arrow"><ArrowRight /></el-icon>
    </section>

    <!-- 附近监测点 -->
    <section class="section-block">
      <div class="section-head">
        <span class="section-title">附近监测点</span>
        <span class="section-sub">共 {{ devices.length }} 个</span>
      </div>
      <div v-if="devices.length" class="device-list">
        <div v-for="d in devices" :key="d.id" class="device-item">
          <span class="dev-dot" :class="d.status === 1 ? 'online' : 'offline'"></span>
          <div class="dev-info">
            <div class="dev-name">{{ d.deviceName }}</div>
            <div class="dev-meta">{{ deviceTypeText(d.type) }} · {{ d.location || '位置待补充' }}</div>
          </div>
          <span class="dev-status" :class="d.status === 1 ? 'on' : 'off'">{{ d.status === 1 ? '在线' : '离线' }}</span>
        </div>
      </div>
      <div v-else class="empty">暂无监测点数据</div>
    </section>

    <!-- 近期环境问题 -->
    <section class="section-block">
      <div class="section-head">
        <span class="section-title">近期环境问题</span>
        <span class="section-sub">最新告警</span>
      </div>
      <div v-if="alerts.length" class="alert-list">
        <div v-for="a in alerts" :key="a.id" class="alert-item">
          <span class="alert-badge" :class="a.level === 'ALARM' ? 'alarm' : 'warn'">{{ levelText(a.level) }}</span>
          <div class="alert-body">
            <div class="alert-msg">{{ a.message }}</div>
            <div class="alert-meta">{{ a.sensorCode }} · {{ fmtTime(a.createTime) }}</div>
          </div>
        </div>
      </div>
      <div v-else class="empty">暂无告警，环境状态良好</div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getQuality, getOverview, getDevicesPage, getAlertsPage, getSupervisionMine } from '@/api'
import { deviceTypeText, levelText, fmtTime } from '@/utils/format'

const router = useRouter()
const qualityOverall = ref(null)
const onlineDevices = ref(0)
const todayReports = ref(0)
const devices = ref([])
const alerts = ref([])
const mineTotal = ref(0)

const aqiText = () => {
  const v = qualityOverall.value
  if (v === null || v === undefined) return '—'
  if (v <= 50) return '优'
  if (v <= 100) return '良'
  return '超标'
}

onMounted(async () => {
  try {
    const q = await getQuality()
    if (q && q.overall !== null && q.overall !== undefined) qualityOverall.value = q.overall
  } catch (e) { /* 忽略 */ }
  try {
    const o = await getOverview()
    onlineDevices.value = o.onlineDevices || 0
    todayReports.value = o.todayReports || 0
  } catch (e) { /* 忽略 */ }
  try {
    const d = await getDevicesPage({ page: 1, size: 100 })
    devices.value = d.records || []
  } catch (e) { /* 忽略 */ }
  try {
    const a = await getAlertsPage({ page: 1, size: 5 })
    alerts.value = a.records || []
  } catch (e) { /* 忽略 */ }
  try {
    const m = await getSupervisionMine({ page: 1, size: 1 })
    mineTotal.value = m.total || 0
  } catch (e) { /* 忽略 */ }
})
</script>

<style scoped>
.home-page { padding: 16px; display: flex; flex-direction: column; gap: 14px; }

.aqi-card {
  background: linear-gradient(135deg, rgba(126, 226, 184, 0.12), rgba(124, 167, 255, 0.08));
  border: 1px solid rgba(126, 226, 184, 0.25);
  border-radius: 18px;
  padding: 18px 20px;
}
.aqi-head { display: flex; justify-content: space-between; align-items: center; }
.aqi-label { font-size: 12px; letter-spacing: 0.12em; color: var(--text-secondary); }
.aqi-status { font-size: 12px; font-weight: 700; color: var(--env-green); }
.aqi-value { margin: 10px 0 8px; }
.aqi-value .num { font-size: 46px; font-weight: 700; color: var(--env-green); }
.aqi-value .unit { font-size: 13px; color: var(--text-muted); margin-left: 4px; }
.aqi-foot { font-size: 11px; color: var(--text-muted); }

.supervise-entry {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--env-green);
  color: #0A0D0C;
  border-radius: 16px;
  padding: 16px 18px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(126, 226, 184, 0.25);
}
.entry-icon { display: flex; }
.entry-title { font-size: 16px; font-weight: 700; }
.entry-sub { font-size: 11px; opacity: 0.75; margin-top: 2px; }
.entry-arrow { margin-left: auto; }

.mine-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  padding: 16px 18px;
  cursor: pointer;
}
.mine-label { font-size: 13px; font-weight: 600; }
.mine-count { font-size: 22px; font-weight: 700; color: var(--env-green); margin: 0 4px 0 8px; }
.mine-unit { font-size: 11px; color: var(--text-muted); }
.mine-arrow { color: var(--text-muted); }

.section-block { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 16px; padding: 14px 16px; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.section-title { font-size: 14px; font-weight: 600; }
.section-sub { font-size: 11px; color: var(--text-muted); }

.device-list { display: flex; flex-direction: column; }
.device-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}
.device-item:last-child { border-bottom: none; }
.dev-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.dev-dot.online { background: var(--env-green); box-shadow: 0 0 8px rgba(126, 226, 184, 0.7); }
.dev-dot.offline { background: var(--text-faint); }
.dev-info { flex: 1; min-width: 0; }
.dev-name { font-size: 13px; font-weight: 500; }
.dev-meta { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.dev-status { font-size: 11px; }
.dev-status.on { color: var(--env-green); }
.dev-status.off { color: var(--text-muted); }

.alert-list { display: flex; flex-direction: column; }
.alert-item { display: flex; align-items: flex-start; gap: 10px; padding: 10px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
.alert-item:last-child { border-bottom: none; }
.alert-badge { font-size: 10px; font-weight: 700; padding: 3px 8px; border-radius: 999px; flex-shrink: 0; }
.alert-badge.alarm { background: rgba(226, 109, 109, 0.15); color: #E26D6D; }
.alert-badge.warn { background: rgba(229, 181, 103, 0.15); color: #E5B567; }
.alert-msg { font-size: 13px; line-height: 1.4; }
.alert-meta { font-size: 11px; color: var(--text-muted); margin-top: 2px; }

.empty { text-align: center; padding: 22px 0; font-size: 12px; color: var(--text-muted); }
</style>
