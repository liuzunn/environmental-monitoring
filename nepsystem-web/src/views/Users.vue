<template>
  <div class="page-container">
    <div class="head-row">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-subtitle">系统用户维护与启停管理</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增用户</el-button>
    </div>

    <div class="apple-card">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索用户名 / 昵称" :prefix-icon="Search" clearable style="width: 240px" @keyup.enter="reload" @clear="reload" />
      </div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" effect="plain" size="small">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" round size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="onToggle(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="440px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="密码" :required="!form.id">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空则不修改' : ''" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio-button value="USER">USER</el-radio-button>
            <el-radio-button value="ADMIN">ADMIN</el-radio-button>
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
import { Plus, Search } from '@element-plus/icons-vue'
import { getUsersPage, addUser, updateUser, deleteUser, changeUserStatus } from '@/api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, username: '', password: '', nickname: '', role: 'USER' })

const fmtTime = t => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

async function load() {
  loading.value = true
  try {
    const d = await getUsersPage({ page: page.value, size, keyword: keyword.value || undefined })
    rows.value = d.records || []
    total.value = Number(d.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() { page.value = 1; load() }

function openDialog(row) {
  if (row) {
    Object.assign(form, { id: row.id, username: row.username, password: '', nickname: row.nickname, role: row.role })
  } else {
    Object.assign(form, { id: null, username: '', password: '', nickname: '', role: 'USER' })
  }
  dialogVisible.value = true
}

async function onSave() {
  if (!form.username || (!form.id && !form.password)) {
    ElMessage.warning('请填写用户名与密码')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      const payload = { id: form.id, username: form.username, nickname: form.nickname, role: form.role }
      if (form.password) payload.password = form.password
      await updateUser(payload)
      ElMessage.success('修改成功')
    } else {
      await addUser({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

async function onToggle(row) {
  const target = row.status === 1 ? 0 : 1
  try {
    await changeUserStatus(row.id, target)
    ElMessage.success(target === 1 ? '已启用' : '已禁用')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

async function onDelete(row) {
  await ElMessageBox.confirm('确定删除用户「' + row.username + '」？', '删除确认', { type: 'warning' }).then(async () => {
    try {
      await deleteUser(row.id)
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
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>