<template>
  <div class="supervise-page">
    <div class="page-head">
      <h2 class="page-title">我要监督</h2>
      <p class="page-sub">如实描述环境问题，提交后进入审核流程</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="污染类型" prop="eventType">
        <el-radio-group v-model="form.eventType" class="type-group">
          <el-radio-button value="POLLUTION">污染</el-radio-button>
          <el-radio-button value="NOISE">噪声</el-radio-button>
          <el-radio-button value="DEVICE_FAULT">设备故障</el-radio-button>
          <el-radio-button value="OTHER">其他</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="严重程度" prop="level">
        <el-radio-group v-model="form.level">
          <el-radio-button value="WARN">预警</el-radio-button>
          <el-radio-button value="ALARM">报警</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="问题标题" prop="title">
        <el-input v-model="form.title" placeholder="一句话概括问题" maxlength="100" show-word-limit />
      </el-form-item>

      <el-form-item label="位置地址" prop="location">
        <el-input v-model="form.location" placeholder="如：教学楼A栋东侧、人工湖西岸…" maxlength="255" />
      </el-form-item>

      <el-form-item label="问题描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请描述问题现象、持续时间、影响范围等" maxlength="500" show-word-limit />
      </el-form-item>

      <el-form-item label="现场图片">
        <div class="upload-grid">
          <div v-for="(img, i) in previews" :key="i" class="upload-item">
            <img :src="img.url" alt="现场图片" />
            <span class="remove" @click="removeImage(i)">×</span>
          </div>
          <label v-if="previews.length < 6" class="upload-add">
            <input type="file" accept="image/*" multiple hidden @change="onPick" />
            <el-icon :size="22"><Plus /></el-icon>
            <span>添加图片</span>
          </label>
        </div>
        <p class="upload-tip">最多 6 张 · 本阶段登记文件名与类型，文件实体上传通道由后端后续提供</p>
      </el-form-item>

      <el-button type="primary" class="submit-btn" :loading="submitting" @click="onSubmit">
        提交监督
      </el-button>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createSupervision, uploadFile } from '@/api'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const previews = ref([])

const form = reactive({
  eventType: 'POLLUTION',
  level: 'WARN',
  title: '',
  location: '',
  description: '',
  attachments: []
})

const rules = {
  eventType: [{ required: true, message: '请选择污染类型', trigger: 'change' }],
  level: [{ required: true, message: '请选择严重程度', trigger: 'change' }],
  title: [{ required: true, message: '请输入问题标题', trigger: 'blur' }],
  description: [{ required: true, message: '请描述问题', trigger: 'blur' }]
}

async function onPick(e) {
  const files = Array.from(e.target.files || [])
  const pending = []
  for (const f of files) {
    if (previews.value.length + pending.length >= 6) break
    pending.push(f)
  }
  e.target.value = ''
  for (const f of pending) {
    try {
      const data = await uploadFile(f)
      previews.value.push({ url: data.url, file: f })  // url 为真实存储地址
    } catch (err) {
      ElMessage.error('图片上传失败：' + f.name)
    }
  }
}

function removeImage(i) {
  previews.value.splice(i, 1)
}

async function onSubmit() {
  await formRef.value.validate().catch(() => Promise.reject())
  submitting.value = true
  try {
    const data = {
      eventType: form.eventType,
      level: form.level,
      title: form.title,
      location: form.location,
      description: form.description,
      attachments: previews.value.map(img => ({
        fileName: img.file ? img.file.name : 'image.jpg',
        fileSize: img.file ? img.file.size : null,
        contentType: img.file ? img.file.type : 'image/jpeg',
        filePath: img.url || null
      }))
    }
    const created = await createSupervision(data)
    ElMessage.success('提交成功，等待审核')
    router.push('/supervision/' + created.id)
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.supervise-page { padding: 16px; }
.page-head { margin-bottom: 16px; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-sub { font-size: 12px; color: var(--text-muted); margin: 0; }

.type-group { display: flex; flex-wrap: wrap; }

.upload-grid { display: flex; flex-wrap: wrap; gap: 10px; }
.upload-item { position: relative; width: 84px; height: 84px; border-radius: 12px; overflow: hidden; border: 1px solid var(--border-subtle); }
.upload-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.upload-item .remove {
  position: absolute; top: 2px; right: 2px;
  width: 18px; height: 18px; line-height: 16px; text-align: center;
  background: rgba(0, 0, 0, 0.6); color: #fff; border-radius: 50%;
  font-size: 13px; cursor: pointer;
}
.upload-add {
  width: 84px; height: 84px; border-radius: 12px;
  border: 1px dashed var(--border-soft);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; color: var(--text-muted); font-size: 11px; cursor: pointer;
}
.upload-tip { font-size: 11px; color: var(--text-faint); margin: 8px 0 0; }

.submit-btn { width: 100%; height: 48px; border-radius: 14px; font-size: 16px; font-weight: 600; letter-spacing: 0.2em; margin-top: 8px; }
</style>