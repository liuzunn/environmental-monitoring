<template>
  <div class="history-page">
    <div class="page-head">
      <h2 class="page-title">历史任务</h2>
      <p class="page-sub">已提交检测的任务记录</p>
    </div>
    <div v-if="rows.length" class="task-list">
      <div v-for="t in rows" :key="t.id" class="task-card" @click="$router.push('/task/' + t.id)">
        <div class="task-head">
          <span class="task-no">{{ t.taskNo }}</span>
          <el-tag type="success" size="small" round>已完成</el-tag>
        </div>
        <div class="task-meta">{{ t.eventTitle || '独立巡检' }}</div>
        <div class="task-foot">
          <span class="prio">AQI 检测 · {{ fmtTime(t.updateTime) }}</span>
        </div>
      </div>
    </div>
    <div v-else class="empty">暂无历史任务</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMyTasks } from '@/api'
import { fmtTime } from '@/utils/format'

const rows = ref([])
onMounted(async () => {
  try {
    const d = await getMyTasks({ page: 1, size: 100, status: 'INSPECTED' })
    rows.value = d.records || []
  } catch (e) { /* 忽略 */ }
})
</script>

<style scoped>
.history-page { padding: 16px; }
.page-head { margin-bottom: 14px; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-sub { font-size: 12px; color: var(--text-muted); margin: 0; }
.task-list { display: flex; flex-direction: column; gap: 10px; }
.task-card { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 14px; padding: 14px 16px; cursor: pointer; }
.task-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.task-no { font-size: 12px; color: var(--text-muted); }
.task-meta { font-size: 13px; margin-bottom: 10px; }
.task-foot { padding-top: 10px; border-top: 1px solid rgba(255,255,255,0.05); font-size: 11px; color: var(--text-faint); }
.empty { text-align: center; padding: 50px 0; color: var(--text-muted); font-size: 13px; }
</style>
