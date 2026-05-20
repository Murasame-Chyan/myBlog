<template>
  <AppHeader current-page="index" />
  <div class="page-container" v-loading="loading">
    <h4>{{ isEdit ? '更新文章' : '写新博文' }}</h4>
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" maxlength="255" placeholder="文章标题" />
      </el-form-item>
      <el-form-item label="正文（Markdown）" required>
        <el-input v-model="form.content" type="textarea" :rows="16" placeholder="支持 Markdown 语法" />
      </el-form-item>
      <el-form-item label="标签（最多10个）">
        <el-select v-model="form.tagIds" multiple filterable placeholder="选择已有标签" style="width:100%">
          <el-option v-for="t in allTags" :key="t.id" :label="t.tagName" :value="t.id" />
        </el-select>
        <el-input v-model="newTagNames" class="mt-2" placeholder="新标签名，多个用逗号分隔" />
      </el-form-item>
      <el-button type="primary" @click="submit" :loading="submitting">{{ isEdit ? '更新文章' : '立即发布' }}</el-button>
      <el-button @click="goHome">返回首页</el-button>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/shared/components/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const auth = useAuthStore()
const loading = ref(true)
const submitting = ref(false)
const allTags = ref([])
const newTagNames = ref('')
const form = reactive({ title: '', content: '', tagIds: [], id: null })

const isEdit = computed(() => !!form.id)

function goHome() { window.location.href = '/' }

function parseEditId() {
  const m = window.location.pathname.match(/\/blogs\/edit\/(\d+)/)
  return m ? Number(m[1]) : null
}

async function submit() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写标题和正文')
    return
  }
  submitting.value = true
  try {
    const p = new URLSearchParams()
    p.append('title', form.title.trim())
    p.append('content', form.content.trim())
    if (form.tagIds.length) p.append('tagIds', form.tagIds.join(','))
    if (newTagNames.value.trim()) p.append('newTagNames', newTagNames.value.trim())
    let url = '/blogs/publish'
    if (isEdit.value) {
      url = '/blogs/update'
      p.append('id', form.id)
    }
    const { data } = await request.post(url, p)
    if (data.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '发布成功')
      const newId = data.data
      window.location.href = '/blogs/read/' + (isEdit.value ? form.id : newId)
    }
  } finally { submitting.value = false }
}

onMounted(async () => {
  await auth.init()
  if (!auth.loggedIn) {
    ElMessage.warning('请先登录')
    window.location.href = '/'
    return
  }
  try {
    const { data } = await request.get('/api/tags')
    if (data.code === 200) allTags.value = data.data || []
  } catch {}
  const editId = parseEditId()
  if (editId) {
    form.id = editId
    try {
      const { data } = await request.get('/api/blogs/' + editId)
      if (data.code === 200 && data.data?.blog) {
        form.title = data.data.blog.title || ''
        form.content = data.data.blog.content || ''
        form.tagIds = data.data.blog.t_id?.tagList || []
      }
    } catch {}
  }
  loading.value = false
})
</script>

<style scoped>
.mt-2 { margin-top: 8px; }
</style>
