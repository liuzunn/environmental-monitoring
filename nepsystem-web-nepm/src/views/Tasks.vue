<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="reload">
        <el-option v-for="(t, k) in STATUS_TEXT" :key="k" :label="t" :value="k" />
      </el-select>
      <el-select v-model="query.gridId" placeholder="全部网格" clearable style="width: 170px" @change="reload">
        <el-option v-for="g in grids" :key="g.id" :label="g.gridName" :value="g.id" />
      </el-select>
      <el-select v-model="query.assigneeId" placeholder="全部网格员" clearable style="width: 170px" @change="reload">
        <el-option v-for="m in members" :key="m.id" :label="m.nickname || m.username" :value="m.userId" />
      </el-select>
      <el-button type="primary" :icon="Plus" @click="openCreate">创建任务</el-button>
      <el-button :icon="Refresh" @click="reload">刷新</el-button>
    </div>

    <div class="apple-card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="taskNo" label="任务编号" width="170" />
        <el-table-column label="优先级" width="80">
          <template #default="{ row }"><el-tag :type="priorityTag(row.priority)" size="small">{{ priorityText(row.priority) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="eventTitle" label="关联事件" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.eventTitle || '独立任务' }}</template>
        </el-table-column>
        <el-table-column prop="gridName" label="网格" width="120" />
        <el-table-column prop="assigneeName" label="执行网格员" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small" round>{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.deadline) }}</template>
        </el-table-column>
        <el-table-column prop="result" label="备注/结论" min-width="150" show-overflow-tooltip />
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <el-dialog v-model="createVisible" title="创建巡检任务" width="480px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="所属网格">
          <el-select v-model="createForm.gridId" placeholder="选择网格" clearable style="width: 100%" @change="onGridChange">
            <el-option v-for="g in grids" :key="g.id" :label="g.gridName + ' (' + g.gridCode + ')'" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行网格员" required>
          <el-select v-model="createForm.assigneeId" placeholder="选择网格员" style="width: 100%">
            <el-option v-for="m in members" :key="m.id" :label="(m.nickname || m.username) + '（' + m.gridName + '）'" :value="m.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="createForm.priority">
            <el-radio-button value="LOW">低</el-radio-button>
            <el-radio-button value="MEDIUM">中</el-radio-button>
            <el-radio-button value="HIGH">高</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="任务类型">
          <el-radio-group v-model="createForm.taskType">
            <el-radio-button value="INSPECTION">巡检</el-radio-button>
            <el-radio-button value="VERIFY">核实</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="createForm.deadline" type="datetime" placeholder="选择截止时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="任务说明">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="任务要求/巡检重点" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">创建任务</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getTasksPage, createTask, getGrids, getGridMembers } from '@/api'
import { statusText, statusTag, priorityText, priorityTag, fmtTime, STATUS_TEXT } from '@/utils/format'

const route = useRoute()
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const grids = ref([])
const members = ref([])
const query = reactive({ status: null, gridId: null, assigneeId: null })
const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({ gridId: null, assigneeId: null, priority: 'MEDIUM', taskType: 'INSPECTION', deadline: null, description: '' })

async function load() {
  loading.value = true
  try {
    const d = await getTasksPage({
      page: page.value, size: size.value,
      status: query.status || undefined, gridId: query.gridId ?? undefined, assigneeId: query.assigneeId ?? undefined
    })
    rows.value = d.records || []
    total.value = Number(d.total || 0)
  } finally { loading.value = false }
}
function reload() { page.value = 1; load() }

async function loadGrids() {
  try { grids.value = (await getGrids()) || [] } catch (e) { /* 忽略 */ }
}

async function onGridChange(gridId) {
  createForm.assigneeId = null
  if (!gridId) { members.value = []; return }
  try { members.value = (await getGridMembers({ gridId })) || [] } catch (e) { /* 忽略 */ }
}

async function openCreate() {
  Object.assign(createForm, { gridId: null, assigneeId: null, priority: 'MEDIUM', taskType: 'INSPECTION', deadline: null, description: '' })
  members.value = []
  createVisible.value = true
}

async function onCreate() {
  if (!createForm.assigneeId) { ElMessage.warning('请选择执行网格员'); return }
  creating.value = true
  try {
    await createTask({ ...createForm })
    ElMessage.success('任务创建成功')
    createVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally { creating.value = false }
}

onMounted(async () => {
  await loadGrids()
  // 支持从网格员管理跳转带筛选
  if (route.query.assigneeId) query.assigneeId = Number(route.query.assigneeId)
  load()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.apple-card { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 14px; padding: 14px; }
</style>
