<script setup>
import { ref, onMounted } from 'vue'
import {
  listAnnouncements,
  createAnnouncement,
  updateAnnouncement,
  publishAnnouncement,
  withdrawAnnouncement,
  deleteAnnouncement,
} from '@/api/announcement'
import { ElMessage, ElMessageBox } from 'element-plus'

const announcements = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const statusFilter = ref(null)

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: null, title: '', content: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await listAnnouncements({
      page: page.value,
      pageSize: pageSize.value,
      status: statusFilter.value,
    })
    announcements.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  form.value = { id: null, title: '', content: '' }
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = { id: row.id, title: row.title, content: row.content }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写公告标题')
    return
  }
  if (isEdit.value) {
    await updateAnnouncement(form.value.id, form.value)
  } else {
    await createAnnouncement(form.value)
  }
  ElMessage.success(isEdit.value ? '公告更新成功' : '公告创建成功')
  dialogVisible.value = false
  fetchData()
}

async function handlePublish(row) {
  await publishAnnouncement(row.id)
  ElMessage.success('公告已发布')
  fetchData()
}

async function handleWithdraw(row) {
  await withdrawAnnouncement(row.id)
  ElMessage.success('公告已撤回')
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteAnnouncement(row.id)
  ElMessage.success('公告删除成功')
  fetchData()
}

function statusTag(status) {
  return { 0: '草稿', 1: '已发布', 2: '已撤回' }[status] || '未知'
}

function statusType(status) {
  return { 0: 'info', 1: 'success', 2: 'warning' }[status] || 'info'
}

onMounted(fetchData)
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button v-permission="'announcement:create'" type="primary" @click="openCreate">
        新增公告
      </el-button>
      <el-select
        v-model="statusFilter"
        placeholder="按状态筛选"
        clearable
        style="width: 160px; margin-left: 12px"
        @change="fetchData"
      >
        <el-option :value="0" label="草稿" />
        <el-option :value="1" label="已发布" />
        <el-option :value="2" label="已撤回" />
      </el-select>
    </div>

    <el-table :data="announcements" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ statusTag(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button v-if="row.status !== 1" v-permission="'announcement:publish'" size="small" link type="success" @click="handlePublish(row)">
            发布
          </el-button>
          <el-button v-if="row.status === 1" v-permission="'announcement:publish'" size="small" link type="warning" @click="handleWithdraw(row)">
            撤回
          </el-button>
          <el-button v-permission="'announcement:update'" size="small" link type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'announcement:delete'" size="small" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
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
      @current-change="fetchData"
      @size-change="fetchData"
    />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '新增公告'" width="600px">
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="公告内容（支持 Markdown）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
</style>
