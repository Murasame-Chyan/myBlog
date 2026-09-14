<script setup>
import { ref, onMounted } from 'vue'
import { listTags, createTag, deleteTag } from '@/api/tag'
import { ElMessage, ElMessageBox } from 'element-plus'

const tags = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const tagName = ref('')

// 加载标签列表
async function fetchTags() {
  loading.value = true
  try {
    const res = await listTags()
    tags.value = res.data
  } finally {
    loading.value = false
  }
}

// 打开新增对话框
function openCreate() {
  tagName.value = ''
  dialogVisible.value = true
}

// 提交新增
async function handleCreate() {
  if (!tagName.value.trim()) {
    ElMessage.warning('请输入标签名')
    return
  }
  await createTag({ tagName: tagName.value.trim() })
  ElMessage.success('标签创建成功')
  dialogVisible.value = false
  fetchTags()
}

// 删除（二次确认，防误删）
async function handleDelete(tag) {
  await ElMessageBox.confirm(`确定删除标签「${tag.tagName}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteTag(tag.id)
  ElMessage.success('标签删除成功')
  fetchTags()
}

onMounted(fetchTags)
</script>

<template>
  <el-card>
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button
        v-permission="'tag:create'"
        type="primary"
        @click="openCreate"
      >
        新增标签
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tags" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="tagName" label="标签名" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            v-permission="'tag:delete'"
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

    <!-- 新增对话框 -->
    <el-dialog v-model="dialogVisible" title="新增标签" width="400px">
      <el-form @submit.prevent>
        <el-form-item label="标签名">
          <el-input v-model="tagName" placeholder="请输入标签名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
