<script setup>
import { ref, onMounted } from 'vue'
import { listUsers, updateUser, updateUserStatus, grantExp } from '@/api/user'
import { ElMessage } from 'element-plus'

const users = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')  // 搜索关键词

async function fetchUsers() {
  loading.value = true
  try{
    const res = await listUsers({ page: page.value, pageSize: pageSize.value, keyword: keyword.value })
    users.value = res.data.list 
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  fetchUsers()
}

// 等级 → tag 颜色：低等级绿，高等级红
function levelTagType(level) {
  if (level <= 2) return 'success'   // 绿：新手
  if (level <= 4) return 'primary'   // 蓝：进阶
  if (level === 5) return 'warning'  // 橙：高级
  return 'danger'                    // 红：满级
}

// 等级阈值：与后端 UserServiceImpl 的 LEVEL_THRESHOLDS 保持一致
const LEVEL_THRESHOLDS = [0, 993, 5461, 11451, 23333, 36666]

// 计算当前等级内的进度百分比（0-100），满级返回 100
function calcProgress(row) {
  const level = row.level || 1
  const exp = row.exp || 0
  const currentLevelExp = LEVEL_THRESHOLDS[level - 1]  // 本等级起始经验
  const nextLevelExp = LEVEL_THRESHOLDS[level]          // 下一等级起始经验
  if (!nextLevelExp) return 100                          // LV.6 满级，无下一级
  const progress = Math.round(((exp - currentLevelExp) / (nextLevelExp - currentLevelExp)) * 100)
  return Math.min(100, Math.max(0, progress))
}

// 进度条内文字：显示"当前经验/下一级所需"，满级显示 MAX
function formatExpText(row) {
  const level = row.level || 1
  const next = LEVEL_THRESHOLDS[level]
  return next ? `${row.exp} / ${next}` : `${row.exp} / MAX`
}

// ===== 编辑用户 =====
const dialogVisible = ref(false)
const form = ref({ id: null, nickname: '', intro: '' })

function openEdit(row) {
  // 预填当前行数据，避免空表单
  form.value = { id: row.id, nickname: row.nickname, intro: row.intro }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.nickname) {
    ElMessage.warning('请输入昵称')
    return
  }
  await updateUser(form.value.id, { nickname: form.value.nickname, intro: form.value.intro })
  ElMessage.success('用户信息更新成功')
  dialogVisible.value = false
  fetchUsers() // 刷新列表
}

// ===== 封号/解封 =====
async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1  // 取反：封禁→解封，正常→封禁
  await updateUserStatus(row.id, newStatus)
  ElMessage.success(newStatus === 1 ? '用户已封禁' : '用户已解封')
  fetchUsers()
}

// ===== 授予经验 =====
const expDialogVisible = ref(false)
const expForm = ref({ id: null, nickname: '', delta: null })

function openGrantExp(row) {
  expForm.value = { id: row.id, nickname: row.nickname, delta: null }
  expDialogVisible.value = true
}

async function handleGrantExp() {
  if (!expForm.value.delta || expForm.value.delta <= 0) {
    ElMessage.warning('请输入正整数经验值')
    return
  }
  await grantExp(expForm.value.id, expForm.value.delta)
  ElMessage.success('经验授予成功')
  expDialogVisible.value = false
  fetchUsers()
}

onMounted(fetchUsers)
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索昵称或邮箱"
        clearable
        style="width: 240px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <el-table :data="users" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="头像" width="80">
        <template #default="{ row }">
          <el-avatar :size="40" :src="row.avatar">
            {{ row.nickname?.charAt(0) }}
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="nickname" label="昵称" width="180" show-overflow-tooltip />
      <el-table-column prop="email" label="邮箱" width="260" show-overflow-tooltip />
      <el-table-column label="等级 / 经验" width="280">
        <template #default="{ row }">
          <div class="level-cell">
            <el-tag :type="levelTagType(row.level)" size="small" effect="plain">LV.{{ row.level }}</el-tag>
            <div class="progress-wrapper">
              <el-progress
                :percentage="calcProgress(row)"
                :stroke-width="18"
                :show-text="false"
              />
              <span class="progress-text">{{ formatExpText(row) }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="封禁状态" min-width="60" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag v-if="row.status == 0" type="success" size="small" effect="plain">正常</el-tag>
          <el-tag v-else type="danger" size="small" effect="plain">封禁中</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320">
        <template #default="{ row }">
          <el-button v-permission="'user:update'" type="primary" size="small" link @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'user:grant-exp'" type="success" size="small" link @click="openGrantExp(row)">
            授予经验
          </el-button>
          <el-popconfirm
            :title="row.status === 1 ? '确认解封该用户？' : '确认封禁该用户？'"
            @confirm="handleToggleStatus(row)"
          >
            <template #reference>
              <el-button
                v-permission="'user:ban'"
                :type="row.status === 1 ? 'success' : 'danger'"
                size="small"
                link
              >
                {{ row.status === 1 ? '解封' : '封号' }}
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @current-change="fetchUsers"
      @size-change="fetchUsers"
    />

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" title="编辑用户" width="480px">
      <el-form label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.intro" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 授予经验弹窗 -->
    <el-dialog v-model="expDialogVisible" title="授予经验" width="400px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <el-input :model-value="expForm.nickname" disabled />
        </el-form-item>
        <el-form-item label="经验值">
          <el-input-number
            v-model="expForm.delta"
            :min="1"
            style="width: 100%"
            placeholder="输入要授予的经验值"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="expDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGrantExp">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}
.level-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.progress-wrapper {
  flex: 1;
  position: relative; /* 作为绝对定位文字的定位基准 */
}
/* 文字绝对定位在整个进度条正中间，不随填充部分移动 */
.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  color: #303133;
  text-shadow: 0 0 3px rgba(255, 255, 255, 0.9);
  white-space: nowrap;
  pointer-events: none; /* 不拦截鼠标事件，避免影响表格交互 */
}
</style>
