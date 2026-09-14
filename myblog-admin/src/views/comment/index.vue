<script setup>
import { ref, onMounted } from 'vue'
import { listComments, deleteComment, pinComment, unpinComment } from '@/api/comment'
import { ElMessage, ElMessageBox } from 'element-plus'

const comments = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

async function fetchComments() {
  loading.value = true
  try {
    const res = await listComments({ page: page.value, pageSize: pageSize.value })
    comments.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除这条评论吗？', '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteComment(row.id)
  ElMessage.success('评论删除成功')
  fetchComments()
}

// 置顶/取消置顶
async function handlePin(row) {
  if (row.is_pinned === 1) {
    await unpinComment(row.id)
    ElMessage.success('已取消置顶')
  } else {
    await pinComment(row.id)
    ElMessage.success('评论已置顶')
  }
  fetchComments()
}

onMounted(fetchComments)
</script>

<template>
  <el-card>
    <el-table :data="comments" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="content" label="评论内容" min-width="200" show-overflow-tooltip />
      <el-table-column prop="blogTitle" label="所属博客" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.blogTitle">{{ row.blogTitle }}</span>
          <el-tag v-else type="info" size="small">博客已删除</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="authorName" label="评论人" width="120" />
      <el-table-column label="置顶" width="70">
        <template #default="{ row }">
          <el-tag v-if="row.is_pinned === 1" type="warning" size="small">置顶</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" label="时间" width="180" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button
            v-permission="'comment:pin'"
            :type="row.is_pinned === 1 ? 'warning' : 'primary'"
            size="small"
            link
            @click="handlePin(row)"
          >
            {{ row.is_pinned === 1 ? '取消置顶' : '置顶' }}
          </el-button>
          <el-button
            v-permission="'comment:delete'"
            type="danger"
            size="small"
            link
            @click="handleDelete(row)"
          >
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
      @current-change="fetchComments"
      @size-change="fetchComments"
    />
  </el-card>
</template>
