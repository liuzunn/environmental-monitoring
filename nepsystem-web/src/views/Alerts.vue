<template>
  <div class="page-container">
    <div class="head-row">
      <div>
        <h2 class="page-title">告警中心</h2>
        <p class="page-subtitle">环境异常预警与报警记录</p>
      </div>
      <el-tag v-if="unhandledCount > 0" type="danger" size="large" round class="count-badge">
        {{ unhandledCount }} 条未处理
      </el-tag>
    </div>

    <div class="apple-card">
      <div class="filter-bar">
        <el-select v-model="query.level" placeholder="全部级别" clearable style="width: 140px" @change="reload">
          <el-option label="报警" value="ALARM" />
          <el-option label="预警" value="WARN" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="reload">
          <el-option label="未处理" :value="0" />
          <el-option label="已处理" :value="1" />
        </el-select>
        <el-select v-model="query.deviceId" placeholder="全部设备" clearable style="width: 200px" @change="reload">
          <el-option v-for="d in devices" :key="d.id" :label="d.deviceName" :value="d.id" />
        </el-select>
        <el-button :icon="Refresh" @click="reload">刷新</el-button>
      </div>

      <el-table :data="rows" v-loading="loading">
        <el-table-column label="级别" width="90">
          <template #default="{ row }">
            <el-tag :type="row.level === 'ALARM' ? 'danger' : 'warning'" round size="small">
              {{ row.level === 'ALARM' ? '报警' : '预警' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sensorCode" label="指标" width="110">
          <template #default="{ row }">{{ sensorName(row.sensorCode) }}</template>
        </el-table-column>
        <el-table-column prop="alertValue" label="触发值" width="110">
          <template #default="{ row }"><span class="tabular-nums value-num">{{ row.alertValue }}</span></template>
        </el-table-column>
        <el-table-column prop="message" label="告警描述" min-width="240" show-overflow-tooltip />
        <el-table-column prop="deviceId" label="设备ID" width="90" />
        <el-table-column prop="createTime" label="告警时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'danger' : 'success'" effect="plain" size="small">
              {{ row.status === 0 ? '未处理' : '已处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="primary" @click="onHandle(row)">处理</el-button>
            <span v-else class="handled-by">{{ row.handleUser || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getAlertsPage, handleAlert, getUnhandled, getDevicesPage, getSensors } from '@/api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const unhandledCount = ref(0)
const devices = ref([])
const sensors = ref([])
const query = reactive({ level: null, status: null, deviceId: null })

const sensorName = code => {
  const s = sensors.value.find(x => x.sensorCode === code)
  return s ? s.sensorName : code
}
const fmtTime = t => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

async function load() {
  loading.value = true
  try {
    const d = await getAlertsPage({ page: page.value, size: size.value, level: query.level || undefined, status: query.status ?? undefined, deviceId: query.deviceId ?? undefined })
    rows.value = d.records || []
    total.value = Number(d.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() { page.value = 1; load() }

async function loadUnhandled() {
  try {
    const d = await getUnhandled()
    unhandledCount.value = d.count || 0
  } catch (e) { /* 忽略 */ }
}

async function onHandle(row) {
  await ElMessageBox.confirm('确认已处理该告警？', '处理确认', { type: 'info' }).then(async () => {
    try {
      await handleAlert(row.id, 'admin')
      ElMessage.success('已处理')
      load()
      loadUnhandled()
    } catch (e) { /* 拦截器已提示 */ }
  }).catch(() => {})
}

onMounted(async () => {
  const [d, s] = await Promise.all([getDevicesPage({ page: 1, size: 100 }), getSensors()])
  devices.value = d.records || []
  sensors.value = s || []
  load()
  loadUnhandled()
})
</script>

<style scoped>
.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.count-badge {
  background: rgba(255, 59, 48, 0.1);
  color: #FF3B30;
  border: none;
  font-weight: 600;
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.value-num {
  font-weight: 600;
}
.handled-by {
  font-size: 13px;
  color: var(--text-sub);
}
</style>