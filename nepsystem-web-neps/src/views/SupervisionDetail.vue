<template>
  <div class="detail-page">
    <div class="detail-card">
      <div class="detail-head">
        <el-tag :type="statusTag(detail.status)" size="small" round>{{ statusText(detail.status) }}</el-tag>
        <span class="detail-no">{{ detail.eventNo }}</span>
      </div>
      <h2 class="detail-title">{{ detail.title }}</h2>
      <div class="detail-type-row">
        <span class="type-badge">{{ typeText(detail.eventType) }}</span>
        <span class="level-badge" :class="detail.level === 'ALARM' ? 'alarm' : 'warn'">{{ levelText(detail.level) }}</span>
      </div>
    </div>

    <!-- 现场图片 -->
    <div class="detail-card">
      <div class="card-label">现场图片</div>
      <div v-if="attachments.length" class="img-grid">
        <div v-for="(a, i) in attachments" :key="i" class="img-item">
          <img v-if="a.filePath" :src="a.filePath" :alt="a.fileName" />
          <div v-else class="img-placeholder">
            <el-icon :size="22"><Picture /></el-icon>
            <span>{{ a.fileName }}</span>
            <span class="pending-tag">未上传</span>
          </div>
        </div>
      </div>
      <div v-else class="no-img">未上传图片</div>
    </div>

    <!-- 事件信息 -->
    <div class="detail-card">
      <div class="card-label">事件信息</div>
      <div class="info-rows">
        <div class="info-row"><span class="k">问题描述</span><span class="v">{{ detail.description || '-' }}</span></div>
        <div class="info-row"><span class="k">位置地址</span><span class="v">{{ detail.location || '-' }}{{ posTail }}</span></div>
        <div class="info-row"><span class="k">提交时间</span><span class="v">{{ fmtTime(detail.createTime) }}</span></div>
        <div class="info-row"><span class="k">提交人</span><span class="v">{{ detail.submitterName || '匿名' }}</span></div>
      </div>
    </div>

    <!-- 处理时间线 -->
    <div class="detail-card">
      <div class="card-label">处理时间线</div>
      <el-timeline v-if="logs.length">
        <el-timeline-item
          v-for="(log, i) in logs"
          :key="i"
          :timestamp="fmtTime(log.createTime)"
          :type="log.toStatus === 'REJECTED' ? 'danger' : (log.toStatus === 'PENDING_REVIEW' ? 'warning' : 'success')"
        >
          <div class="log-title">{{ statusText(log.toStatus) }}<span v-if="log.fromStatus" class="log-from">（由 {{ statusText(log.fromStatus) }} 变更）</span></div>
          <div class="log-remark">{{ log.remark || '—' }}</div>
          <div class="log-operator">{{ log.operatorName || '系统' }}</div>
        </el-timeline-item>
      </el-timeline>
      <div v-else class="no-img">暂无流转记录</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getSupervisionDetail } from '@/api'
import { statusText, statusTag, typeText, levelText, fmtTime } from '@/utils/format'

const route = useRoute()
const detail = ref({})
const attachments = ref([])
const logs = ref([])

const posTail = computed(() => {
  const d = detail.value
  if (d.lat != null && d.lng != null) return '（' + d.lat + ', ' + d.lng + '）'
  return ''
})

onMounted(async () => {
  try {
    const data = await getSupervisionDetail(route.params.id)
    detail.value = data.event || {}
    attachments.value = data.attachments || []
    logs.value = data.statusLogs || []
  } catch (e) { /* 拦截器已提示 */ }
})
</script>

<style scoped>
.detail-page { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.detail-card { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 16px; padding: 16px; }
.detail-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.detail-no { font-size: 11px; color: var(--text-faint); letter-spacing: 0.04em; }
.detail-title { font-size: 18px; font-weight: 700; margin: 0 0 8px; line-height: 1.4; }
.detail-type-row { display: flex; gap: 8px; }
.type-badge { font-size: 11px; padding: 3px 10px; border-radius: 999px; background: rgba(124, 167, 255, 0.15); color: #7CA7FF; }
.level-badge { font-size: 11px; padding: 3px 10px; border-radius: 999px; }
.level-badge.alarm { background: rgba(226, 109, 109, 0.15); color: #E26D6D; }
.level-badge.warn { background: rgba(229, 181, 103, 0.15); color: #E5B567; }

.card-label { font-size: 12px; font-weight: 600; letter-spacing: 0.1em; color: var(--text-secondary); margin-bottom: 12px; }

.img-grid { display: flex; flex-wrap: wrap; gap: 10px; }
.img-item { width: 96px; height: 96px; border-radius: 12px; overflow: hidden; border: 1px solid var(--border-subtle); }
.img-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.img-placeholder {
  width: 100%; height: 100%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; color: var(--text-muted); font-size: 10px; text-align: center; padding: 4px;
  box-sizing: border-box;
}
.pending-tag { font-size: 9px; background: rgba(255, 255, 255, 0.06); padding: 1px 6px; border-radius: 999px; }
.no-img { font-size: 12px; color: var(--text-faint); padding: 8px 0; text-align: center; }

.info-rows { display: flex; flex-direction: column; gap: 10px; }
.info-row { display: flex; gap: 12px; font-size: 13px; }
.info-row .k { color: var(--text-muted); flex-shrink: 0; width: 64px; }
.info-row .v { color: var(--text-primary); line-height: 1.5; word-break: break-all; }

.log-title { font-size: 13px; font-weight: 600; }
.log-from { font-size: 11px; color: var(--text-muted); font-weight: 400; }
.log-remark { font-size: 12px; color: var(--text-secondary); margin-top: 3px; }
.log-operator { font-size: 11px; color: var(--text-faint); margin-top: 2px; }
</style>