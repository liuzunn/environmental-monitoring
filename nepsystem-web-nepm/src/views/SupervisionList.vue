<template>
  <div class="events-page">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="reload">
        <el-option v-for="(t, k) in STATUS_TEXT" :key="k" :label="t" :value="k" />
      </el-select>
      <el-select v-model="query.eventType" placeholder="全部类型" clearable style="width: 140px" @change="reload">
        <el-option v-for="(t, k) in TYPE_TEXT" :key="k" :label="t" :value="k" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="编号 / 标题 / 描述" clearable style="width: 220px" @keyup.enter="reload" @clear="reload" />
      <el-button :icon="Refresh" @click="reload">刷新</el-button>
    </div>

    <div class="apple-card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="eventNo" label="编号" width="170" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ typeText(row.eventType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="级别" width="80">
          <template #default="{ row }"><el-tag :type="row.level === 'ALARM' ? 'danger' : 'warning'" size="small">{{ levelText(row.level) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="提交人" width="110">
          <template #default="{ row }">{{ row.submitterName || '匿名' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small" round>{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/events/' + row.id)">详情</el-button>
            <el-button v-if="row.status === 'PENDING_REVIEW'" link type="success" @click="onApprove(row)">审核通过</el-button>
            <el-button v-if="row.status === 'PENDING_REVIEW'" link type="danger" @click="onReject(row)">驳回</el-button>
            <el-button v-if="row.status === 'APPROVED'" link type="warning" @click="openAssign(row)">派单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <!-- 派单对话框 -->
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getEventsPage, approveEvent, rejectEvent, assignEvent, getGrids, getGridMembers } from '@/api'
import { statusText, statusTag, typeText, levelText, fmtTime, STATUS_TEXT, TYPE_TEXT } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const query = reactive({ status: null, eventType: null, keyword: '' })
const grids = ref([])
const members = ref([])
const assignVisible = ref(false)
const assigning = ref(false)
const currentEvent = ref(null)
const assignForm = reactive({ gridId: null, assigneeId: null, priority: 'MEDIUM', deadline: null, remark: '' })

async function load() {
  loading.value = true
  try {
    const d = await getEventsPage({
      page: page.value, size: size.value,
      status: query.status || undefined, keyword: query.keyword || undefined
    })
    rows.value = d.records || []
    total.value = Number(d.total || 0)
  } finally { loading.value = false }
}
function reload() { page.value = 1; load() }

async function onApprove(row) {
  await ElMessageBox.confirm('确认审核通过事件「' + row.title + '」？', '审核确认', { type: 'info' }).then(async () => {
    try { await approveEvent(row.id, null); ElMessage.success('已审核通过'); load() } catch (e) { /* 拦截器已提示 */ }
  }).catch(() => {})
}

async function onReject(row) {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回确认', { inputPlaceholder: '驳回原因', inputValidator: v => !!v || '请填写驳回原因' }).catch(() => ({}))
  if (!value) return
  try { await rejectEvent(row.id, value); ElMessage.success('已驳回'); load() } catch (e) { /* 拦截器已提示 */ }
}

async function openAssign(row) {
  currentEvent.value = row
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
    await assignEvent(currentEvent.value.id, { ...assignForm })
    ElMessage.success('派单成功')
    assignVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally { assigning.value = false }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.apple-card { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 14px; padding: 14px; }
</style>
