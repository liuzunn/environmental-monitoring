<template>
  <div class="home-page">
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-value tabular-nums" style="color: #E5B567">{{ stats.pendingAccept ?? 0 }}</div>
        <div class="stat-label">待接收任务</div>
      </div>
      <div class="stat-card">
        <div class="stat-value tabular-nums" style="color: #7CA7FF">{{ stats.processing ?? 0 }}</div>
        <div class="stat-label">进行中</div>
      </div>
      <div class="stat-card">
        <div class="stat-value tabular-nums" style="color: #7EE2B8">{{ stats.todayDone ?? 0 }}</div>
        <div class="stat-label">今日完成</div>
      </div>
      <div class="stat-card">
        <div class="stat-value tabular-nums" style="color: #E26D6D">{{ stats.overdue ?? 0 }}</div>
        <div class="stat-label">超时任务</div>
      </div>
    </div>

    <div class="status-tabs">
      <div v-for="t in tabs" :key="t.key" class="status-tab" :class="{ active: activeTab === t.key }" @click="activeTab = t.key">
        {{ t.label }}
      </div>
    </div>

    <div v-if="filtered.length" class="task-list">
      <div v-for="t in filtered" :key="t.id" class="task-card" @click="$router.push('/task/' + t.id)">
        <div class="task-head">
          <span class="task-no">{{ t.taskNo }}</span>
          <el-tag :type="statusTag(t.status)" size="small" round>{{ statusText(t.status) }}</el-tag>
        </div>
        <div class="task-meta">
          <span class="type-badge">{{ typeText(t.eventTitle ? 'POLLUTION' : 'OTHER') }}</span>
          <span class="addr">{{ eventLocation(t) }}</span>
        </div>
        <div class="task-foot">
          <span class="prio" :class="prioClass(t.priority)">优先级：{{ priorityText(t.priority) }}</span>
          <span class="deadline">截止 {{ fmtTime(t.deadline) }}</span>
        </div>
      </div>
    </div>
    <div v-else class="empty">暂无任务</div>

    <div v-if="hasMore" class="load-more" @click="loadMore">加载更多</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMyTasks, getMyTaskStats } from '@/api'
import { statusText, statusTag, typeText, priorityText, fmtTime } from '@/utils/format'

const stats = ref({})
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const activeTab = ref('ALL')

const tabs = [
  { key: 'ALL', label: '全部' },
  { key: 'ASSIGNED', label: '待接收' },
  { key: 'PROCESSING', label: '进行中' },
  { key: 'INSPECTED', label: '已完成' }
]

const PROCESSING = ['ACCEPTED', 'INSPECTING']
const filtered = computed(() => {
  if (activeTab.value === 'ALL') return rows.value
  if (activeTab.value === 'PROCESSING') return rows.value.filter(t => PROCESSING.includes(t.status))
  return rows.value.filter(t => t.status === activeTab.value)
})
const hasMore = computed(() => rows.value.length < total.value)

function eventLocation(t) {
  return t.eventTitle ? '关联事件：' + t.eventTitle : (t.deviceName || '独立巡检')
}
function prioClass(p) {
  return { HIGH: 'high', MEDIUM: 'medium', LOW: 'low' }[p] || ''
}

async function load(reset) {
  if (reset) { page.value = 1; rows.value = [] }
  try {
    const d = await getMyTasks({ page: page.value, size: pageSize })
    total.value = Number(d.total || 0)
    rows.value = reset ? (d.records || []) : rows.value.concat(d.records || [])
  } catch (e) { /* 忽略 */ }
}
function loadMore() { page.value += 1; load(false) }

onMounted(async () => {
  load(true)
  try { stats.value = await getMyTaskStats() } catch (e) { /* 忽略 */ }
})
</script>

<style scoped>
.home-page { padding: 16px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 16px; }
.stat-card { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 14px; padding: 14px 10px; text-align: center; }
.stat-value { font-size: 26px; font-weight: 700; }
.stat-label { font-size: 11px; color: var(--text-secondary); margin-top: 4px; }

.status-tabs { display: flex; gap: 6px; margin-bottom: 14px; overflow-x: auto; }
.status-tab {
  flex-shrink: 0; font-size: 12px; padding: 6px 14px; border-radius: 999px;
  background: var(--bg-tertiary); border: 1px solid var(--border-subtle);
  color: var(--text-secondary); cursor: pointer;
}
.status-tab.active { background: var(--env-green); border-color: var(--env-green); color: #0A0D0C; font-weight: 600; }

.task-list { display: flex; flex-direction: column; gap: 10px; }
.task-card { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 14px; padding: 14px 16px; cursor: pointer; }
.task-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.task-no { font-size: 12px; color: var(--text-muted); letter-spacing: 0.04em; }
.task-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.type-badge { font-size: 11px; padding: 3px 10px; border-radius: 999px; background: rgba(124,167,255,0.15); color: #7CA7FF; flex-shrink: 0; }
.addr { font-size: 13px; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-foot { display: flex; justify-content: space-between; align-items: center; padding-top: 10px; border-top: 1px solid rgba(255,255,255,0.05); font-size: 11px; }
.prio.high { color: #E26D6D; }
.prio.medium { color: #E5B567; }
.prio.low { color: var(--text-muted); }
.deadline { color: var(--text-faint); }
.empty { text-align: center; padding: 50px 0; color: var(--text-muted); font-size: 13px; }
.load-more { text-align: center; padding: 12px 0; font-size: 12px; color: var(--text-muted); cursor: pointer; }
</style>
