<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="网格编号 / 名称" clearable style="width: 220px" @keyup.enter="load" @clear="load" />
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px" @change="load">
        <el-option label="启用" :value="1" /><el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增网格</el-button>
    </div>
    <div class="apple-card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="gridCode" label="网格编号" width="140" />
        <el-table-column prop="gridName" label="网格名称" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" round>{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="onToggle(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑网格' : '新增网格'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="网格编号" required>
          <el-input v-model="form.gridCode" placeholder="如 GRID-001" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="网格名称" required>
          <el-input v-model="form.gridName" placeholder="网格名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="网格描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getGrids, addGrid, updateGrid, deleteGrid, changeGridStatus } from '@/api'
import { fmtTime } from '@/utils/format'

const rows = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const keyword = ref('')
const statusFilter = ref(null)
const form = reactive({ id: null, gridCode: '', gridName: '', description: '' })

async function load() {
  loading.value = true
  try {
    rows.value = (await getGrids({ keyword: keyword.value || undefined, status: statusFilter.value ?? undefined })) || []
  } finally { loading.value = false }
}

function openDialog(row) {
  if (row) Object.assign(form, { id: row.id, gridCode: row.gridCode, gridName: row.gridName, description: row.description })
  else Object.assign(form, { id: null, gridCode: '', gridName: '', description: '' })
  dialogVisible.value = true
}

async function onSave() {
  if (!form.gridCode || !form.gridName) { ElMessage.warning('请填写编号与名称'); return }
  saving.value = true
  try {
    if (form.id) { await updateGrid({ ...form }); ElMessage.success('修改成功') }
    else { await addGrid({ ...form }); ElMessage.success('新增成功') }
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally { saving.value = false }
}

async function onToggle(row) {
  try {
    await changeGridStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? '已停用' : '已启用')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

async function onDelete(row) {
  await ElMessageBox.confirm('确定删除网格「' + row.gridName + '」？', '删除确认', { type: 'warning' }).then(async () => {
    try { await deleteGrid(row.id); ElMessage.success('删除成功'); load() } catch (e) { /* 拦截器已提示 */ }
  }).catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.apple-card { background: var(--bg-tertiary); border: 1px solid var(--border-subtle); border-radius: 14px; padding: 14px; }
</style>
