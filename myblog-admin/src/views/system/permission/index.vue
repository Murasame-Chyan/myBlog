<script setup>
import { ref, onMounted } from 'vue'
import {
  listPermissions,
  getPermissionTree,
  createPermission,
  updatePermission,
  deletePermission,
} from '@/api/permission'
import { ElMessage, ElMessageBox } from 'element-plus'

const treeData = ref([]) // 树形数据（表格展示）
const flatList = ref([]) // 平铺列表（父级下拉选择用）
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: null, code: '', name: '', type: 3, parentId: 0 })

// 同时拉树形 + 平铺（树形展示，平铺做父级下拉）
async function fetchData() {
  const [treeRes, listRes] = await Promise.all([getPermissionTree(), listPermissions()])
  treeData.value = treeRes.data
  flatList.value = listRes.data
}

function openCreate() {
  isEdit.value = false
  form.value = { id: null, code: '', name: '', type: 3, parentId: 0 }
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = {
    id: row.id,
    code: row.code,
    name: row.name,
    type: row.type,
    parentId: row.parentId ?? 0,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning('请填写权限标识和名称')
    return
  }
  if (isEdit.value) {
    await updatePermission(form.value.id, form.value)
  } else {
    await createPermission(form.value)
  }
  ElMessage.success(isEdit.value ? '权限更新成功' : '权限创建成功')
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除权限「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deletePermission(row.id)
  ElMessage.success('权限删除成功')
  fetchData()
}

// 类型显示标签
function typeTag(type) {
  return { 1: '菜单', 2: '按钮', 3: '接口' }[type] || '接口'
}

onMounted(fetchData)
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button v-permission="'permission:create'" type="primary" @click="openCreate">
        新增权限
      </el-button>
    </div>

    <!-- 树形表格：row-key + tree-props 实现父子层级 -->
    <el-table
      :data="treeData"
      row-key="id"
      :tree-props="{ children: 'children' }"
      default-expand-all
      border
    >
      <el-table-column prop="name" label="权限名称" min-width="180" />
      <el-table-column prop="code" label="权限标识" min-width="180" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 1 ? 'success' : row.type === 2 ? 'warning' : 'info'">
            {{ typeTag(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button v-permission="'permission:update'" type="primary" size="small" link @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'permission:delete'" type="danger" size="small" link @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑权限' : '新增权限'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="权限名称">
          <el-input v-model="form.name" placeholder="如：删除博客" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.code" placeholder="如：blog:delete" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
            <el-radio :value="3">接口</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="父级权限">
          <el-select v-model="form.parentId" placeholder="选择父级（顶级为无）" style="width: 100%">
            <el-option :value="0" label="（顶级）" />
            <el-option
              v-for="p in flatList"
              :key="p.id"
              :value="p.id"
              :label="`${p.name}（${p.code}）`"
            />
          </el-select>
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
}
</style>
