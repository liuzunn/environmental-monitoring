<template>
  <div class="page-container">
    <div class="head-row">
      <div>
        <h2 class="page-title">阈值设置</h2>
        <p class="page-subtitle">预警（WARN）与报警（ALARM）阈值配置，设备级优先于全局默认</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增阈值</el-button>
    </div>

    <div class="apple-card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="范围" width="140">
          <template #default="{ row }">
            <el-tag :type="row.deviceId ? 'primary' : 'info'" effect="plain" size="small">
              {{ row.deviceId ? '设备级 #' + row.deviceId : '全局默认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="指标" width="120">
          <template #default="{ row }">{{ sensorName(row.sensorCode) }} ({{ row.sensorCode }})</template>
        </el-table-column>
        <el-table-column label="预警区间" min-width="160">
          <template #default="{ row }">{{ fmtRange(row.warnMin, row.warnMax) }}</template>
        </el-table-column>
        <el-table-column label="报警区间" min-width="160">
          <template #default="{ row }">{{ fmtRange(row.alarmMin, row.alarmMax) }}</template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled === 1" @change="v => onToggle(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑阈值' : '新增阈值'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="作用范围">
          <el-select v-model="form.scope" style="width: 100%">
            <el-option label="全局默认（所有同指标设备）" :value="0" />
            <el-option v-for="d in devices" :key="d.id" :label="'设备：' + d.deviceName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="指标" required>
          <el-select v-model="form.sensorCode" style="width: 100%">
            <el-option v-for="s in sensors" :key="s.sensorCode" :label="s.sensorName + ' (' + s.sensorCode + ')'" :value="s.sensorCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警上限">
          <el-input-number v-model="form.warnMax" :min="0" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="报警上限">
          <el-input-number v-model="form.alarmMax" :min="0" :precision="1" style="width: 100%" />
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
import { getThresholds, addThreshold, updateThreshold, deleteThreshold, getDevicesPage, getSensors } from '@/api'

const rows = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const devices = ref([])
const sensors = ref([])
const form = reactive({ id: null, scope: 0, sensorCode: '', warnMax: null, alarmMax: null })

const sensorName = code => {
  const s = sensors.value.find(x => x.sensorCode === code)
  return s ? s.sensorName : code
}
const fmtRange = (min, max) => {
  const a = min === null || min === undefined ? '-' : min
  const b = max === null || max === undefined ? '-' : max
  return a + ' ~ ' + b
}

async function load() {
  loading.value = true
  try {
    rows.value = (await getThresholds()) || []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    Object.assign(form, { id: row.id, scope: row.deviceId || 0, sensorCode: row.sensorCode, warnMax: row.warnMax, alarmMax: row.alarmMax })
  } else {
    Object.assign(form, { id: null, scope: 0, sensorCode: '', warnMax: null, alarmMax: null })
  }
  dialogVisible.value = true
}

async function onSave() {
  if (!form.sensorCode) {
    ElMessage.warning('请选择指标')
    return
  }
  saving.value = true
  try {
    const payload = {
      id: form.id,
      deviceId: form.scope === 0 ? null : form.scope,
      sensorCode: form.sensorCode,
      warnMin: null,
      warnMax: form.warnMax,
      alarmMin: null,
      alarmMax: form.alarmMax,
      enabled: 1
    }
    if (form.id) {
      await updateThreshold(payload)
      ElMessage.success('修改成功')
    } else {
      await addThreshold(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

async function onToggle(row, v) {
  try {
    await updateThreshold({ ...row, enabled: v ? 1 : 0 })
    ElMessage.success(v ? '已启用' : '已停用')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

async function onDelete(row) {
  await ElMessageBox.confirm('确定删除该阈值配置？', '删除确认', { type: 'warning' }).then(async () => {
    try {
      await deleteThreshold(row.id)
      ElMessage.success('删除成功')
      load()
    } catch (e) { /* 拦截器已提示 */ }
  }).catch(() => {})
}

onMounted(async () => {
  const [d, s] = await Promise.all([getDevicesPage({ page: 1, size: 100 }), getSensors()])
  devices.value = d.records || []
  sensors.value = s || []
  load()
})
</script>

<style scoped>
.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
</style>