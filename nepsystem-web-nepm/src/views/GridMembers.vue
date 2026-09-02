<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="gridFilter" placeholder="全部网格" clearable style="width: 200px" @change="load">
        <el-option v-for="g in grids" :key="g.id" :label="g.gridName" :value="g.id" />
      </el-select>
      <el-input v-model="keyword" placeholder="用户名 / 昵称" clearable style="width: 200px" @keyup.enter="load" @clear="load" />
      <el-button type="primary" :icon="Plus" @click="openAssign()">分配网格员</el-button>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>
    <div class="apple-card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="gridName" label="所属网格" min-width="140" />
        <el-table-column label="网格角色" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.role === 'GRID_LEADER' ? '网格长' : '网格员' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="任务完成率" width="160">
          <template #default="{ row }">
            <span v-if="statsMap[row.userId]">
              {{ statsMap[row.userId].closedTasks }}/{{ statsMap[row.userId].totalTasks }}
              <el-progress :percentage="statsMap[row.userId].completionRate" :stroke-width="6" style="width: 90px; display: inline-block; margin-left: 6px;" />
            </span>
            <span v-else class="muted">暂无任务</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewTasks(row)">查看任务</el-button>
            <el-button link type="danger" @click="onRemove(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="assignVisible" title="分配网格员" width="420px">
      <el-form :model="assignForm" label-width="80px">
        <el-form-item label="所属网格" required>
          <el-select v-model="assignForm.gridId" placeholder="选择网格" style="width: 100%">
            <el-option v-for="g in grids" :key="g.id" :label="g.gridName + ' (' + g.gridCode + ')'" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择用户" required>
          <el-select v-model="assignForm.userId" placeholder="选择用户（users 表）" filterable style="width: 100%">
            <el-option v-for="u in allUsers" :key="u.id" :label="(u.nickname || u.username) + '（' + u.username + '）'" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="网格角色">
          <el-radio-group v-model="assignForm.role">
            <el-radio-button value="GRID_USER">网格员</el-radio-button>
            <el-radio-button value="GRID_LEADER">网格长</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="onAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getGridMembers, getGrids, assignGridMember, removeGridMember, getAssigneeStats, getUsersPage } from '@/api'

const router = useRouter()
const rows = ref([])
const grids = ref([])
const allUsers = ref([])
const statsMap = ref({})
const loading = ref(false)
const gridFilter = ref(null)
const keyword = ref('')
const assignVisible = ref(false)
const assigning = ref(false)
const assignForm = reactive({ gridId: null, userId: null, role: 'GRID_USER' })

async function load() {
  loading.value = true
  try {
    rows.value = (await getGridMembers({ gridId: gridFilter.value ?? undefined, keyword: keyword.value || undefined })) || []
  } finally { loading.value = false }
}

async function loadStats() {
  try {
    const list = (await getAssigneeStats()) || []
    statsMap.value = {}
    for (const s of list) statsMap.value[s.userId] = s
  } catch (e) { /* 忽略 */ }
}

async function openAssign() {
  Object.assign(assignForm, { gridId: null, userId: null, role: 'GRID_USER' })
  try {
    const d = await getUsersPage({ page: 1, size: 200 })
    allUsers.value = d.records || []
  } catch (e) { /* 忽略 */ }
  assignVisible.value = true
}

async function onAssign() {
  if (!assignForm.gridId || !assignForm.userId) { ElMessage.warning('请选择网格与用户'); return }
  assigning.value = true
  try {
    await assignGridMember({ ...assignForm })
    ElMessage.success('分配成功')
    assignVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally { assigning.value = false }
}

function viewTasks(row) {
  router.push({ path: '/tasks', query: { assigneeId: row.userId } })
}

async function onRemove(row) {
  await ElMessageBox.confirm('确定移除网格员「' + (row.nickname || row.username) + '」？', '移除确认', { type: 'warning' }).then(async () => {
    try { await removeGridMember(row.id); ElMessage.success('已移除'); load() } catch (e) { /* 拦截器已提示 */ }
  }).catch(() => {})
}

onMounted(async () => {
  load()
  loadStats()
  try { grids.value = (await getGrids()) || [] } catch (e) { /* 忽略 */ }
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.apple-card { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 14px; padding: 14px; }
.muted { color: var(--text-faint); font-size: 12px; }
</style>
