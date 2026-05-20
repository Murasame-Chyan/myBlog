<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible',$event)" title="编辑资料" width="480px" :close-on-click-modal="false">
    <el-form label-position="top">
      <el-form-item label="头像">
        <div class="edit-avatar-area">
          <el-avatar :size="80" :src="avatarPreview" />
          <div><input ref="fi" type="file" accept="image/*" style="display:none" @change="onFile"><el-button size="small" @click="$refs.fi.click()">上传新头像</el-button><div v-if="avatarMsg" class="avatar-msg">{{ avatarMsg }}</div></div>
        </div>
      </el-form-item>
      <el-form-item label="昵称"><el-input v-model="form.nickname" maxlength="32" /></el-form-item>
      <el-form-item label="简介"><el-input v-model="form.intro" maxlength="255" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="form.email" maxlength="255" /></el-form-item>
      <el-form-item label="性别">
        <el-select v-model="form.gender">
          <el-option label="未设置" :value="0" /><el-option label="男" :value="1" /><el-option label="女" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="GitHub 用户名"><el-input v-model="form.githubUsername" maxlength="255" /></el-form-item>
      <el-form-item>
        <template #label>GitHub Token <span class="subtle-hint">（<a href="https://github.com/settings/tokens" target="_blank">获取方式</a>，需勾选 read:user）</span></template>
        <el-input v-model="form.githubToken" type="password" maxlength="255" placeholder="ghp_xxx" autocomplete="off" />
      </el-form-item>
    </el-form>
    <template #footer><el-button @click="$emit('update:visible',false)">取消</el-button><el-button type="primary" @click="save" :loading="saving">保存</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible', 'saved'])
const avatarPreview = ref('/images/default-avatar.png'), avatarMsg = ref(''), saving = ref(false)
const form = reactive({ nickname:'',intro:'',email:'',gender:0,githubUsername:'',githubToken:'' })

onMounted(async () => {
  try {
    const { data } = await request.get('/api/user/profile-data'); if (data.code === 200) { const u = data.data; Object.assign(form, { nickname:u.nickname||'',intro:u.intro||'',email:u.email||'',gender:u.gender||0,githubUsername:u.githubUsername||'' }); avatarPreview.value = u.avatar || '/images/default-avatar.png' }
  } catch {}
})

async function onFile(e) {
  const file = e.target.files?.[0]; if (!file) return
  if (file.size > 10*1024*1024) { avatarMsg.value = '图片不能超过10MB'; return }
  avatarMsg.value = '上传中...'; const fd = new FormData(); fd.append('file', file)
  try { const { data } = await request.post('/user/avatar/upload', fd); if (data.code === 200) { avatarPreview.value = data.data+'?t='+Date.now(); avatarMsg.value = '头像已更新' } } catch { avatarMsg.value = '上传失败' }
  e.target.value = ''
}

async function save() {
  saving.value = true
  try {
    const p = new URLSearchParams(); p.append('nickname',form.nickname); p.append('intro',form.intro); p.append('email',form.email); p.append('gender',form.gender); p.append('githubUsername',form.githubUsername); if (form.githubToken) p.append('githubToken',form.githubToken)
    const { data } = await request.post('/user/profile/update', p)
    if (data.code === 200) { ElMessage.success('保存成功'); emit('saved') }
  } finally { saving.value = false }
}
</script>

<style scoped>
.edit-avatar-area { display: flex; gap: 16px; align-items: center; } .avatar-msg { font-size: 0.72rem; color: #999; margin-top: 4px; }
.subtle-hint { font-size: 0.68rem; color: #aaa; font-weight: 400; } .subtle-hint a { color: var(--el-color-primary); }
</style>
