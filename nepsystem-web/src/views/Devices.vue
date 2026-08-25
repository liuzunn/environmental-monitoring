<template>
  <div class="page-container">
    <div class="head-row">
      <div>
        <h2 class="page-title">设备管理</h2>
        <p class="page-subtitle">管理监测设备与站点，共 {{ total }} 台</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增设备</el-button>
    </div>

    <div class="apple-card">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索设备编号 / 名称" :prefix-icon="Search" clearable style="width: 240px" @keyup.enter="reload" @clear="reload" />
        <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 140px" @change="reload">
          <el-option label="空气" value="AIR" />
          <el-option label="水质" value="WATER" />
          <el-option label="噪声" value="NOISE" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="reload">
          <el-option label="在线" :value="1" />
          <el-option label="离线" :value="0" />
          <el-option label="停用" :value="2" />
        </el-select>
        <el-button :icon="Refresh" @click="reload">刷新</el-button>
      </div>

      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="deviceCode" label="设备编号" width="140" />
        <el-table-column prop="deviceName" label="设备名称" min-width="180" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" effect="plain" size="small">{{ typeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="安装位置" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" round size="small" class="status-tag">
              <span class="tag-dot" :class="'s' + row.status"></span>{{ statusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastReportTime" label="最近上报" width="170">
          <template #default="{ row }">{{ fmtTime(row.lastReportTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑设备' : '新增设备'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="设备编号" required>
          <el-input v-model="form.deviceCode" placeholder="如 DEV-AIR-002" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="设备名称" required>
          <el-input v-model="form.deviceName" placeholder="设备名称" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.type">
            <el-radio-button value="AIR">空气</el-radio-button>
            <el-radio-button value="WATER">水质</el-radio-button>
            <el-radio-button value="NOISE">噪声</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="安装位置">
          <el-input v-model="form.location" placeholder="安装位置描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="0">离线</el-radio-button>
            <el-radio-button :value="1">在线</el-radio-button>
            <el-radio-button :value="2">停用</el-radio-button>
          </el-radio-group>
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
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { getDevicesPage, addDevice, updateDevice, deleteDevice } from '@/api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const query = reactive({ keyword: '', type: null, status: null })
const form = reactive({ id: null, deviceCode: '', deviceName: '', type: 'AIR', location: '', status: 1 })

const typeName = t => ({ AIR: '空气', WATER: '水质', NOISE: '噪声' }[t] || t)
const typeTag = t => ({ AIR: 'primary', WATER: 'success', NOISE: 'warning' }[t] || 'info')
const statusName = s => ({ 0: '离线', 1: '在线', 2: '停用' }[s] || '-')
const statusTag = s => ({ 0: 'info', 1: 'success', 2: 'danger' }[s] || 'info')
const fmtTime = t => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

async function load() {
  loading.value = true
  try {
    const d = await getDevicesPage({ page: page.value, size: size.value, keyword: query.keyword || undefined, type: query.type || undefined, status: query.status ?? undefined })
    rows.value = d.records || []
    total.value = Number(d.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() { page.value = 1; load() }

function openDialog(row) {
  if (row) {
    Object.assign(form, { id: row.id, deviceCode: row.deviceCode, deviceName: row.deviceName, type: row.type, location: row.location, status: row.status })
  } else {
    Object.assign(form, { id: null, deviceCode: '', deviceName: '', type: 'AIR', location: '', status: 1 })
  }
  dialogVisible.value = true
}

async function onSave() {
  if (!form.deviceCode || !form.deviceName) {
    ElMessage.warning('请填写设备编号与名称')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updateDevice({ ...form })
      ElMessage.success('修改成功')
    } else {
      await addDevice({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(
    '确定删除设备「' + row.deviceName + '」吗？',
    '删除确认',
    { type: 'warning' }
  ).then(async () => {
    try {
      await deleteDevice(row.id)
      ElMessage.success('删除成功')
      load()
    } catch (e) { /* 拦截器已提示 */ }
  }).catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-16);
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.tag-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.tag-dot.s1 { color: var(--color-green); box-shadow: 0 0 4px var(--color-green); }
.tag-dot.s0 { color: var(--text-placeholder); }
.tag-dot.s2 { color: var(--color-red); }
</style>