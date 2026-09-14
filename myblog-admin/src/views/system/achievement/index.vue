<script setup>
import { ref, onMounted } from 'vue'
import {
  listAchievements,
  createAchievement,
  updateAchievement,
  deleteAchievement,
  grantAchievement,
  revokeAchievement,
} from '@/api/achievement'
import { ElMessage, ElMessageBox } from 'element-plus'

const achievements = ref([])
const loading = ref(false)

// 新增/编辑对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: null, name: '', icon: '', fullImage: '' })

// 授予/撤销对话框
const grantDialogVisible = ref(false)
const grantMode = ref('grant') // 'grant' 授予 / 'revoke' 撤销
const grantForm = ref({ userId: null, achievementId: null, achievementName: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await listAchievements()
    achievements.value = res.data
  } finally {
    loading.value = false
  }
}

// ===== 成就 CRUD =====
function openCreate() {
  isEdit.value = false
  form.value = { id: null, name: '', icon: '', fullImage: '' }
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = { id: row.id, name: row.name, icon: row.icon, fullImage: row.fullImage }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请填写成就名称')
    return
  }
  if (isEdit.value) {
    await updateAchievement(form.value.id, form.value)
  } else {
    await createAchievement(form.value)
  }
  ElMessage.success(isEdit.value ? '成就更新成功' : '成就创建成功')
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除成就「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteAchievement(row.id)
  ElMessage.success('成就删除成功')
  fetchData()
}

// ===== 授予 / 撤销 =====
function openGrant(row, mode) {
  grantMode.value = mode
  grantForm.value = { userId: null, achievementId: row.id, achievementName: row.name }
  grantDialogVisible.value = true
}

async function handleGrantSubmit() {
  if (!grantForm.value.userId) {
    ElMessage.warning('请输入用户ID')
    return
  }
  const data = { userId: grantForm.value.userId, achievementId: grantForm.value.achievementId }
  if (grantMode.value === 'grant') {
    await grantAchievement(data)
    ElMessage.success('荣誉已授予')
  } else {
    await revokeAchievement(data)
    ElMessage.success('荣誉已撤销')
  }
  grantDialogVisible.value = false
}

onMounted(fetchData)
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button v-permission="'achievement:create'" type="primary" @click="openCreate">
        新增成就
      </el-button>
    </div>

    <el-table :data="achievements" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="图标" width="80">
        <template #default="{ row }">
          <el-image v-if="row.icon" :src="row.icon" style="width: 40px; height: 40px" fit="cover" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="成就名称" min-width="160" />
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button v-permission="'achievement:grant'" size="small" link type="success" @click="openGrant(row, 'grant')">
            授予
          </el-button>
          <el-button v-permission="'achievement:grant'" size="small" link type="warning" @click="openGrant(row, 'revoke')">
            撤销
          </el-button>
          <el-button v-permission="'achievement:update'" size="small" link type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'achievement:delete'" size="small" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑成就' : '新增成就'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="成就名称">
          <el-input v-model="form.name" placeholder="如：签到达人" />
        </el-form-item>
        <el-form-item label="图标地址">
          <el-input v-model="form.icon" placeholder="图标 URL 或路径" />
        </el-form-item>
        <el-form-item label="大图地址">
          <el-input v-model="form.fullImage" placeholder="完整图 URL 或路径" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 授予/撤销对话框 -->
    <el-dialog v-model="grantDialogVisible" :title="grantMode === 'grant' ? '授予荣誉' : '撤销荣誉'" width="400px">
      <el-form label-width="80px">
        <el-form-item label="成就">
          <el-input :model-value="grantForm.achievementName" disabled />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input-number v-model="grantForm.userId" :min="1" style="width: 100%" placeholder="输入要操作的用户ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantDialogVisible = false">取消</el-button>
        <el-button :type="grantMode === 'grant' ? 'success' : 'warning'" @click="handleGrantSubmit">
          {{ grantMode === 'grant' ? '授予' : '撤销' }}
        </el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
