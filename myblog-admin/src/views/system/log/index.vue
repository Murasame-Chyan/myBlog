<script setup>
import { ref, onMounted } from 'vue'
import { listOperationLogs, listSignInLogs, listLoginLogs } from '@/api/log'

const activeTab = ref('operation')

// 操作日志
const opLogs = ref([])
const opLoading = ref(false)
const opTotal = ref(0)
const opPage = ref(1)
const opPageSize = ref(20)
const opFilter = ref({ module: '', userId: null, dateRange: null })

// 签到日志
const signLogs = ref([])
const signLoading = ref(false)
const signTotal = ref(0)
const signPage = ref(1)
const signPageSize = ref(20)
const signUserId = ref(null)

// 登录日志
const loginLogs = ref([])
const loginLoading = ref(false)
const loginTotal = ref(0)
const loginPage = ref(1)
const loginPageSize = ref(20)
const loginUserId = ref(null)
const loginEmail = ref('')

async function fetchOpLogs() {
  opLoading.value = true
  try {
    const params = {
      page: opPage.value,
      pageSize: opPageSize.value,
      module: opFilter.value.module || null,
      userId: opFilter.value.userId || null,
    }
    if (opFilter.value.dateRange && opFilter.value.dateRange.length === 2) {
      params.startTime = opFilter.value.dateRange[0]
      params.endTime = opFilter.value.dateRange[1]
    }
    const res = await listOperationLogs(params)
    opLogs.value = res.data.list
    opTotal.value = res.data.total
  } finally {
    opLoading.value = false
  }
}

async function fetchSignLogs() {
  signLoading.value = true
  try {
    const res = await listSignInLogs({
      page: signPage.value,
      pageSize: signPageSize.value,
      userId: signUserId.value || null,
    })
    signLogs.value = res.data.list
    signTotal.value = res.data.total
  } finally {
    signLoading.value = false
  }
}

function searchOp() {
  opPage.value = 1
  fetchOpLogs()
}

function resetOp() {
  opFilter.value = { module: '', userId: null, dateRange: null }
  opPage.value = 1
  fetchOpLogs()
}

function searchSign() {
  signPage.value = 1
  fetchSignLogs()
}

async function fetchLoginLogs() {
  loginLoading.value = true
  try {
    const res = await listLoginLogs({
      page: loginPage.value,
      pageSize: loginPageSize.value,
      userId: loginUserId.value || null,
      email: loginEmail.value || null,
    })
    loginLogs.value = res.data.list
    loginTotal.value = res.data.total
  } finally {
    loginLoading.value = false
  }
}

function searchLogin() {
  loginPage.value = 1
  fetchLoginLogs()
}

// 切换 tab 时按需加载对应数据
function handleTabChange(name) {
  if (name === 'sign') fetchSignLogs()
  if (name === 'login') fetchLoginLogs()
}

onMounted(fetchOpLogs)
</script>

<template>
  <el-card>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- ===== 操作日志 ===== -->
      <el-tab-pane label="操作日志" name="operation">
        <div class="filter-bar">
          <el-input
            v-model="opFilter.module"
            placeholder="模块名（如 用户管理）"
            clearable
            style="width: 180px"
          />
          <el-input
            v-model="opFilter.userId"
            placeholder="操作人ID"
            clearable
            style="width: 140px"
          />
          <el-date-picker
            v-model="opFilter.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
          <el-button type="primary" @click="searchOp">查询</el-button>
          <el-button @click="resetOp">重置</el-button>
        </div>

        <el-table :data="opLogs" v-loading="opLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="操作人" width="120" />
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="action" label="操作" width="140" />
          <el-table-column label="对象" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.targetName">{{ row.targetName }}</span>
              <el-tag v-else-if="row.targetId" type="info" size="small">#{{ row.targetId }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="createTime" label="时间" width="180" />
        </el-table>

        <el-pagination
          v-model:current-page="opPage"
          v-model:page-size="opPageSize"
          :total="opTotal"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          style="margin-top: 16px; justify-content: flex-end"
          @current-change="fetchOpLogs"
          @size-change="fetchOpLogs"
        />
      </el-tab-pane>

      <!-- ===== 签到日志 ===== -->
      <el-tab-pane label="签到日志" name="sign">
        <div class="filter-bar">
          <el-input
            v-model="signUserId"
            placeholder="用户ID"
            clearable
            style="width: 160px"
            @change="searchSign"
          />
          <el-button type="primary" @click="searchSign">查询</el-button>
        </div>

        <el-table :data="signLogs" v-loading="signLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="nickname" label="用户" width="140" />
          <el-table-column prop="sign_date" label="签到日期" width="120" />
          <el-table-column prop="letter" label="字母" width="80" />
          <el-table-column prop="total_exp" label="获得经验" width="100" />
          <el-table-column label="补签" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.is_makeup" type="warning" size="small">补签</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="signPage"
          v-model:page-size="signPageSize"
          :total="signTotal"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          style="margin-top: 16px; justify-content: flex-end"
          @current-change="fetchSignLogs"
          @size-change="fetchSignLogs"
        />
      </el-tab-pane>

      <!-- ===== 登录日志 ===== -->
      <el-tab-pane label="登录日志" name="login">
        <div class="filter-bar">
          <el-input
            v-model="loginUserId"
            placeholder="用户ID"
            clearable
            style="width: 140px"
          />
          <el-input
            v-model="loginEmail"
            placeholder="登录邮箱"
            clearable
            style="width: 180px"
          />
          <el-button type="primary" @click="searchLogin">查询</el-button>
        </div>

        <el-table :data="loginLogs" v-loading="loginLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="userId" label="用户ID" width="90" />
          <el-table-column prop="email" label="登录邮箱" min-width="180" show-overflow-tooltip />
          <el-table-column label="结果" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="loginTime" label="登录时间" width="180" />
        </el-table>

        <el-pagination
          v-model:current-page="loginPage"
          v-model:page-size="loginPageSize"
          :total="loginTotal"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          style="margin-top: 16px; justify-content: flex-end"
          @current-change="fetchLoginLogs"
          @size-change="fetchLoginLogs"
        />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
