<template>
  <div class="messages-page">
    <div class="page-head">
      <h2 class="page-title">消息中心</h2>
      <p class="page-sub">我的监督事件状态动态</p>
    </div>

    <div v-if="items.length" class="msg-list">
      <div v-for="(m, i) in items" :key="i" class="msg-item" @click="$router.push('/supervision/' + m.id)">
        <div class="msg-dot" :class="dotClass(m)"></div>
        <div class="msg-body">
          <div class="msg-title">{{ m.title }}</div>
          <div class="msg-text">最新状态：<b>{{ statusText(m.status) }}</b> · {{ m.eventNo }}</div>
          <div class="msg-time">{{ fmtTime(m.updateTime || m.createTime) }}</div>
        </div>
        <el-icon :size="14" class="msg-arrow"><ArrowRight /></el-icon>
      </div>
    </div>
    <div v-else class="empty">
      <p>暂无消息</p>
      <p class="empty-sub">提交监督事件后，状态变化会显示在这里</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getSupervisionMine } from '@/api'
import { statusText, fmtTime } from '@/utils/format'

const items = ref([])

function dotClass(m) {
  if (m.status === 'REJECTED') return 'rejected'
  if (m.status === 'PENDING_REVIEW') return 'pending'
  return 'done'
}

onMounted(async () => {
  try {
    const d = await getSupervisionMine({ page: 1, size: 50 })
    const list = (d.records || []).slice()
    list.sort((a, b) => String(b.updateTime || b.createTime).localeCompare(String(a.updateTime || a.createTime)))
    items.value = list
  } catch (e) { /* 忽略 */ }
})
</script>

<style scoped>
.messages-page { padding: 16px; }
.page-head { margin-bottom: 14px; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-sub { font-size: 12px; color: var(--text-muted); margin: 0; }

.msg-list { display: flex; flex-direction: column; gap: 10px; }
.msg-item {
  display: flex; align-items: flex-start; gap: 12px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
  padding: 14px 16px;
  cursor: pointer;
}
.msg-dot { width: 9px; height: 9px; border-radius: 50%; margin-top: 5px; flex-shrink: 0; }
.msg-dot.pending { background: #E5B567; box-shadow: 0 0 8px rgba(229, 181, 103, 0.5); }
.msg-dot.done { background: var(--env-green); box-shadow: 0 0 8px rgba(126, 226, 184, 0.5); }
.msg-dot.rejected { background: #E26D6D; box-shadow: 0 0 8px rgba(226, 109, 109, 0.5); }
.msg-body { flex: 1; min-width: 0; }
.msg-title { font-size: 14px; font-weight: 600; }
.msg-text { font-size: 12px; color: var(--text-secondary); margin-top: 3px; }
.msg-time { font-size: 11px; color: var(--text-faint); margin-top: 4px; }
.msg-arrow { color: var(--text-faint); margin-top: 5px; }

.empty { text-align: center; padding: 50px 0; color: var(--text-muted); }
.empty p { margin: 0 0 6px; font-size: 14px; }
.empty-sub { font-size: 12px; color: var(--text-faint); }
</style>
