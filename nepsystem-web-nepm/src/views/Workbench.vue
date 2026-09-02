<template>
  <div class="workbench">
    <div class="stat-grid">
      <div v-for="s in stats" :key="s.key" class="stat-card" :style="{ borderColor: s.color + '44' }">
        <div class="stat-value tabular-nums" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <div class="panel-grid">
      <section class="panel">
        <div class="panel-head">
          <span class="panel-title">待处理事件</span>
          <span class="panel-link" @click="$router.push('/events')">全部 →</span>
        </div>
        <div v-if="pending.length" class="row-list">
          <div v-for="e in pending" :key="e.id" class="row-item" @click="$router.push('/events/' + e.id)">
            <el-tag :type="statusTag(e.status)" size="small">{{ statusText(e.status) }}</el-tag>
            <span class="row-title">{{ e.title }}</span>
            <span class="row-time">{{ fmtTime(e.createTime) }}</span>
          </div>
        </div>
        <div v-else class="empty">暂无待处理事件</div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <span class="panel-title">最近任务</span>
          <span class="panel-link" @click="$router.push('/tasks')">全部 →</span>
        </div>
        <div v-if="tasks.length" class="row-list">
          <div v-for="t in tasks" :key="t.id" class="row-item">
            <el-tag :type="priorityTag(t.priority)" size="small">{{ priorityText(t.priority) }}</el-tag>
            <span class="row-title">{{ t.taskNo }}</span>
            <span class="row-time">{{ t.assigneeName || '-' }}</span>
          </div>
        </div>
        <div v-else class="empty">暂无任务</div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <span class="panel-title">未处理告警</span>
          <span class="panel-link" @click="$router.push('/events')">告警 →</span>
        </div>
        <div v-if="alerts.length" class="row-list">
          <div v-for="a in alerts" :key="a.id" class="row-item">
            <el-tag :type="a.level === 'ALARM' ? 'danger' : 'warning'" size="small">{{ levelText(a.level) }}</el-tag>
            <span class="row-title">{{ a.message }}</span>
            <span class="row-time">{{ fmtTime(a.createTime) }}</span>
          </div>
        </div>
        <div v-else class="empty">暂无未处理告警</div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getWorkbenchStats, getPendingEvents, getTasksPage, getAlertsPage } from '@/api'
import { statusText, statusTag, levelText, priorityText, priorityTag, fmtTime } from '@/utils/format'

const stats = ref([
  { key: 'todayEvents', label: '今日监督', value: 0, color: '#7CA7FF' },
  { key: 'pendingReview', label: '待审核', value: 0, color: '#E5B567' },
  { key: 'pendingAssign', label: '待派单', value: 0, color: '#B7A6E8' },
  { key: 'processing', label: '处理中', value: 0, color: '#6FD3C7' },
  { key: 'todayClosed', label: '今日完成', value: 0, color: '#7EE2B8' }
])
const pending = ref([])
const tasks = ref([])
const alerts = ref([])

onMounted(async () => {
  try {
    const s = await getWorkbenchStats()
    for (const item of stats.value) item.value = s[item.key] || 0
  } catch (e) { /* 忽略 */ }
  try { pending.value = (await getPendingEvents({ limit: 6 })) || [] } catch (e) { /* 忽略 */ }
  try { tasks.value = (await getTasksPage({ page: 1, size: 6 })).records || [] } catch (e) { /* 忽略 */ }
  try { alerts.value = (await getAlertsPage({ page: 1, size: 6, status: 0 })).records || [] } catch (e) { /* 忽略 */ }
})
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; margin-bottom: 18px; }
.stat-card {
  background: var(--bg-tertiary); border: 1px solid var(--border-subtle);
  border-radius: 14px; padding: 18px 16px;
}
.stat-value { font-size: 32px; font-weight: 700; }
.stat-label { font-size: 12px; color: var(--text-secondary); margin-top: 6px; letter-spacing: 0.08em; }

.panel-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.panel { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 14px; padding: 16px; }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.panel-title { font-size: 14px; font-weight: 600; }
.panel-link { font-size: 11px; color: var(--text-muted); cursor: pointer; }
.panel-link:hover { color: var(--env-green); }
.row-list { display: flex; flex-direction: column; }
.row-item { display: flex; align-items: center; gap: 8px; padding: 9px 4px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); cursor: pointer; font-size: 12px; }
.row-item:last-child { border-bottom: none; }
.row-title { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-primary); }
.row-time { font-size: 11px; color: var(--text-faint); flex-shrink: 0; }
.empty { text-align: center; padding: 24px 0; font-size: 12px; color: var(--text-faint); }

@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(3, 1fr); }
  .panel-grid { grid-template-columns: 1fr; }
}
</style>
