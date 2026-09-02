<template>
  <div class="mine-page">
    <div class="page-head">
      <h2 class="page-title">我的监督</h2>
      <p class="page-sub">共 {{ total }} 条记录</p>
    </div>

    <div class="status-tabs">
      <div v-for="t in tabs" :key="t.key" class="status-tab" :class="{ active: activeTab === t.key }" @click="activeTab = t.key">
        {{ t.label }}
      </div>
    </div>

    <div v-if="filtered.length" class="event-list">
      <div v-for="e in filtered" :key="e.id" class="event-card" @click="$router.push('/supervision/' + e.id)">
        <div class="event-head">
          <el-tag :type="statusTag(e.status)" size="small" round>{{ statusText(e.status) }}</el-tag>
          <span class="event-no">{{ e.eventNo }}</span>
        </div>
        <div class="event-title">{{ e.title }}</div>
        <div class="event-meta">
          <span>{{ typeText(e.eventType) }}</span>
          <span class="dot">·</span>
          <span>{{ e.location || '位置未填写' }}</span>
        </div>
        <div class="event-foot">
          <span class="event-time">{{ fmtTime(e.createTime) }}</span>
          <el-icon :size="14" class="event-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>
    <div v-else class="empty">
      <p>暂无{{ activeTab === 'ALL' ? '' : '该状态下的' }}监督记录</p>
      <el-button type="primary" plain round @click="$router.push('/supervise')">去监督</el-button>
    </div>

    <div v-if="hasMore" class="load-more" @click="loadMore">加载更多</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getSupervisionMine } from '@/api'
import { statusText, statusTag, typeText, fmtTime } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const activeTab = ref('ALL')

// 分类：待审核 / 处理中 / 已完成 / 已驳回（后端一次返回，前端按状态分组）
const tabs = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING_REVIEW', label: '待审核' },
  { key: 'PROCESSING', label: '处理中' },
  { key: 'CLOSED', label: '已完成' },
  { key: 'REJECTED', label: '已驳回' }
]

const PROCESSING_STATES = ['APPROVED', 'ASSIGNED', 'ACCEPTED', 'INSPECTING', 'INSPECTED', 'VERIFIED']

const filtered = computed(() => {
  if (activeTab.value === 'ALL') return rows.value
  if (activeTab.value === 'PROCESSING') return rows.value.filter(e => PROCESSING_STATES.includes(e.status))
  return rows.value.filter(e => e.status === activeTab.value)
})

const hasMore = computed(() => rows.value.length < total.value)

async function load(reset) {
  if (reset) { page.value = 1; rows.value = [] }
  try {
    const d = await getSupervisionMine({ page: page.value, size: pageSize })
    total.value = Number(d.total || 0)
    rows.value = reset ? (d.records || []) : rows.value.concat(d.records || [])
  } catch (e) { /* 忽略 */ }
}

function loadMore() {
  page.value += 1
  load(false)
}

onMounted(() => load(true))
</script>

<style scoped>
.mine-page { padding: 16px; }
.page-head { margin-bottom: 14px; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-sub { font-size: 12px; color: var(--text-muted); margin: 0; }

.status-tabs { display: flex; gap: 6px; margin-bottom: 14px; overflow-x: auto; }
.status-tab {
  flex-shrink: 0;
  font-size: 12px;
  padding: 6px 14px;
  border-radius: 999px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  cursor: pointer;
}
.status-tab.active {
  background: var(--env-green);
  border-color: var(--env-green);
  color: #0A0D0C;
  font-weight: 600;
}

.event-list { display: flex; flex-direction: column; gap: 10px; }
.event-card {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
  padding: 14px 16px;
  cursor: pointer;
}
.event-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.event-no { font-size: 11px; color: var(--text-faint); letter-spacing: 0.04em; }
.event-title { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.event-meta { font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 6px; }
.event-meta .dot { color: var(--text-faint); }
.event-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding-top: 10px; border-top: 1px solid rgba(255, 255, 255, 0.05); }
.event-time { font-size: 11px; color: var(--text-faint); }
.event-arrow { color: var(--text-faint); }

.empty { text-align: center; padding: 40px 0; color: var(--text-muted); font-size: 13px; }
.empty p { margin-bottom: 14px; }
.load-more { text-align: center; padding: 12px 0; font-size: 12px; color: var(--text-muted); cursor: pointer; }
</style>
