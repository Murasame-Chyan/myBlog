<script setup>
import { ref, onMounted } from 'vue'
import { listBlogs, listBinBlogs, offlineBlog, recoverBlog, deleteBlog } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('online')

// 在线博客
const blogs = ref([])
const blogLoading = ref(false)
const blogTotal = ref(0)
const blogPage = ref(1)
const blogPageSize = ref(10)
const blogKeyword = ref('')

// 回收站
const binBlogs = ref([])
const binLoading = ref(false)
const binTotal = ref(0)
const binPage = ref(1)
const binPageSize = ref(10)
const binKeyword = ref('')

async function fetchBlogs() {
  blogLoading.value = true
  try {
    const res = await listBlogs({
      page: blogPage.value,
      pageSize: blogPageSize.value,
      keyword: blogKeyword.value || null,
    })
    blogs.value = res.data.list
    blogTotal.value = res.data.total
  } finally {
    blogLoading.value = false
  }
}

async function fetchBinBlogs() {
  binLoading.value = true
  try {
    const res = await listBinBlogs({
      page: binPage.value,
      pageSize: binPageSize.value,
      keyword: binKeyword.value || null,
    })
    binBlogs.value = res.data.list
    binTotal.value = res.data.total
  } finally {
    binLoading.value = false
  }
}

// 下架
async function handleOffline(row) {
  await ElMessageBox.confirm(`确定下架博客「${row.title}」吗？`, '下架确认', {
    type: 'warning',
    confirmButtonText: '下架',
    cancelButtonText: '取消',
  })
  await offlineBlog(row.id)
  ElMessage.success('博客已下架')
  fetchBlogs()
}

// 恢复
async function handleRecover(row) {
  await recoverBlog(row.id)
  ElMessage.success('博客已恢复')
  fetchBinBlogs()
}

// 永久删除
async function handleDelete(row) {
  await ElMessageBox.confirm('永久删除后不可恢复，确定删除？', '删除确认', {
    type: 'warning',
    confirmButtonText: '永久删除',
    cancelButtonText: '取消',
  })
  await deleteBlog(row.id)
  ElMessage.success('博客已永久删除')
  fetchBinBlogs()
}

onMounted(fetchBlogs)
</script>

<template>
  <el-card>
    <el-tabs v-model="activeTab" @tab-change="(name) => name === 'bin' && fetchBinBlogs()">
      <!-- ===== 在线博客 ===== -->
      <el-tab-pane label="在线博客" name="online">
        <div class="toolbar">
          <el-input
            v-model="blogKeyword"
            placeholder="搜索标题"
            clearable
            style="width: 240px"
            @keyup.enter="blogPage = 1; fetchBlogs()"
            @clear="blogPage = 1; fetchBlogs()"
          />
          <el-button type="primary" @click="blogPage = 1; fetchBlogs()">查询</el-button>
        </div>

        <el-table :data="blogs" v-loading="blogLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="author" label="作者" width="120" />
          <el-table-column prop="read_count" label="阅读" width="80" />
          <el-table-column prop="like_count" label="点赞" width="80" />
          <el-table-column prop="comment_count" label="评论" width="80" />
          <el-table-column prop="created_at" label="发布时间" width="180" />
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button v-permission="'blog:offline'" type="warning" size="small" link @click="handleOffline(row)">
                下架
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="blogPage"
          v-model:page-size="blogPageSize"
          :total="blogTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          style="margin-top: 16px; justify-content: flex-end"
          @current-change="fetchBlogs"
          @size-change="fetchBlogs"
        />
      </el-tab-pane>

      <!-- ===== 回收站 ===== -->
      <el-tab-pane label="回收站" name="bin">
        <div class="toolbar">
          <el-input
            v-model="binKeyword"
            placeholder="搜索标题"
            clearable
            style="width: 240px"
            @keyup.enter="binPage = 1; fetchBinBlogs()"
            @clear="binPage = 1; fetchBinBlogs()"
          />
          <el-button type="primary" @click="binPage = 1; fetchBinBlogs()">查询</el-button>
        </div>

        <el-table :data="binBlogs" v-loading="binLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="author" label="作者" width="120" />
          <el-table-column prop="deleted_at" label="下架时间" width="180" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button v-permission="'blog:offline'" type="success" size="small" link @click="handleRecover(row)">
                恢复
              </el-button>
              <el-button v-permission="'blog:delete'" type="danger" size="small" link @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="binPage"
          v-model:page-size="binPageSize"
          :total="binTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          style="margin-top: 16px; justify-content: flex-end"
          @current-change="fetchBinBlogs"
          @size-change="fetchBinBlogs"
        />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}
</style>
