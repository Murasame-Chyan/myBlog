<script setup>
import { ref, onMounted } from 'vue'
import { listMenus, getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/menu'
import { ElMessage, ElMessageBox } from 'element-plus'

const treeData = ref([]) // 树形数据（表格展示）
const flatList = ref([]) // 平铺列表（父级下拉用）
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  parentId: 0,
  name: '',
  title: '',
  path: '',
  component: '',
  icon: '',
  sort: 0,
  hidden: 0,
  permissionId: null,
})

// 同时拉树形 + 平铺
async function fetchData() {
  const [treeRes, listRes] = await Promise.all([getMenuTree(), listMenus()])
  treeData.value = treeRes.data
  flatList.value = listRes.data
}

function openCreate() {
  isEdit.value = false
  form.value = {
    id: null, parentId: 0, name: '', title: '', path: '',
    component: '', icon: '', sort: 0, hidden: 0, permissionId: null,
  }
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.title || !form.value.name || !form.value.path) {
    ElMessage.warning('请填写菜单标题、路由名称和路径')
    return
  }
  if (isEdit.value) {
    await updateMenu(form.value.id, form.value)
  } else {
    await createMenu(form.value)
  }
  ElMessage.success(isEdit.value ? '菜单更新成功' : '菜单创建成功')
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除菜单「${row.title}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteMenu(row.id)
  ElMessage.success('菜单删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button v-permission="'menu:create'" type="primary" @click="openCreate">
        新增菜单
      </el-button>
    </div>

    <!-- 树形表格 -->
    <el-table
      :data="treeData"
      row-key="id"
      :tree-props="{ children: 'children' }"
      default-expand-all
      border
    >
      <el-table-column label="菜单标题" min-width="200">
        <template #default="{ row }">
          <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          <span style="margin-left: 6px">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="路由名称" width="150" />
      <el-table-column prop="path" label="路径" width="140" />
      <el-table-column prop="component" label="组件路径" min-width="180" show-overflow-tooltip />
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column label="隐藏" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.hidden === 1" type="info" size="small">隐藏</el-tag>
          <el-tag v-else type="success" size="small">显示</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button v-permission="'menu:update'" type="primary" size="small" link @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'menu:delete'" type="danger" size="small" link @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜单' : '新增菜单'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="菜单标题">
          <el-input v-model="form.title" placeholder="侧边栏显示的中文名，如 用户管理" />
        </el-form-item>
        <el-form-item label="路由名称">
          <el-input v-model="form.name" placeholder="英文路由名，如 UserManage" />
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="form.path" placeholder="如 /user" />
        </el-form-item>
        <el-form-item label="组件路径">
          <el-input v-model="form.component" placeholder="如 views/user/index.vue" />
        </el-form-item>
        <el-form-item label="图标名">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 User" />
        </el-form-item>
        <el-form-item label="父级菜单">
          <el-select v-model="form.parentId" placeholder="顶级菜单选无" style="width: 100%">
            <el-option :value="0" label="（顶级）" />
            <el-option v-for="m in flatList" :key="m.id" :value="m.id" :label="m.title" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="是否隐藏">
          <el-switch v-model="form.hidden" :active-value="1" :inactive-value="0" />
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
