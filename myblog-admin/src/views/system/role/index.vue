<script setup>
import { ref, onMounted, nextTick } from 'vue'
import {
  listRoles,
  getRole,
  createRole,
  updateRole,
  deleteRole,
  assignPermissions,
  assignMenus,
} from '@/api/role'
import { getPermissionTree } from '@/api/permission'
import { getMenuTree } from '@/api/menu'
import { ElMessage, ElMessageBox } from 'element-plus'

const roles = ref([])
const loading = ref(false)

// 新增/编辑对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: null, name: '', code: '', dataScope: 1, status: 1, sort: 0 })

// 权限分配抽屉
const permDrawerVisible = ref(false)
const permTree = ref([])
const permTreeRef = ref()
const currentRole = ref(null)

// 菜单分配抽屉
const menuDrawerVisible = ref(false)
const menuTree = ref([])
const menuTreeRef = ref()

async function fetchRoles() {
  loading.value = true
  try {
    const res = await listRoles()
    roles.value = res.data
  } finally {
    loading.value = false
  }
}

// ===== 角色 CRUD =====
function openCreate() {
  isEdit.value = false
  form.value = { id: null, name: '', code: '', dataScope: 1, status: 1, sort: 0 }
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.name || !form.value.code) {
    ElMessage.warning('请填写角色名称和编码')
    return
  }
  if (isEdit.value) {
    await updateRole(form.value.id, form.value)
  } else {
    await createRole(form.value)
  }
  ElMessage.success(isEdit.value ? '角色更新成功' : '角色创建成功')
  dialogVisible.value = false
  fetchRoles()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除角色「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteRole(row.id)
  ElMessage.success('角色删除成功')
  fetchRoles()
}

// ===== 分配权限 =====
async function openAssignPerm(row) {
  currentRole.value = row
  const [treeRes, roleRes] = await Promise.all([getPermissionTree(), getRole(row.id)])
  permTree.value = treeRes.data
  permDrawerVisible.value = true
  // 树渲染完成后回显已勾选的权限
  await nextTick()
  permTreeRef.value.setCheckedKeys(roleRes.data.permissionIds)
}

async function submitAssignPerm() {
  // getCheckedKeys 返回勾选的叶子节点 ID（check-strictly 下精确勾选）
  const checked = permTreeRef.value.getCheckedKeys()
  await assignPermissions(currentRole.value.id, checked)
  ElMessage.success('权限分配成功')
  permDrawerVisible.value = false
}

// ===== 分配菜单 =====
async function openAssignMenu(row) {
  currentRole.value = row
  const [treeRes, roleRes] = await Promise.all([getMenuTree(), getRole(row.id)])
  menuTree.value = treeRes.data
  menuDrawerVisible.value = true
  await nextTick()
  menuTreeRef.value.setCheckedKeys(roleRes.data.menuIds)
}

async function submitAssignMenu() {
  const checked = menuTreeRef.value.getCheckedKeys()
  await assignMenus(currentRole.value.id, checked)
  ElMessage.success('菜单分配成功')
  menuDrawerVisible.value = false
}

onMounted(fetchRoles)
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button v-permission="'role:create'" type="primary" @click="openCreate">
        新增角色
      </el-button>
    </div>

    <!-- 角色列表 -->
    <el-table :data="roles" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="角色名称" width="140" />
      <el-table-column prop="code" label="角色编码" width="180" />
      <el-table-column label="数据范围" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.dataScope === 1 ? 'success' : 'info'">
            {{ { 1: '全部', 2: '本人', 3: '自定义' }[row.dataScope] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button v-permission="'role:assign-perm'" size="small" link type="primary" @click="openAssignPerm(row)">
            分配权限
          </el-button>
          <el-button v-permission="'role:assign-menu'" size="small" link type="primary" @click="openAssignMenu(row)">
            分配菜单
          </el-button>
          <el-button v-permission="'role:update'" size="small" link type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-permission="'role:delete'" size="small" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="角色名称">
          <el-input v-model="form.name" placeholder="如：超级管理员" />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="form.code" placeholder="如：ROLE_ADMIN" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-radio-group v-model="form.dataScope">
            <el-radio :value="1">全部</el-radio>
            <el-radio :value="2">本人</el-radio>
            <el-radio :value="3">自定义</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配抽屉 -->
    <el-drawer v-model="permDrawerVisible" title="分配权限" size="320px">
      <el-tree
        ref="permTreeRef"
        :data="permTree"
        node-key="id"
        show-checkbox
        check-strictly
        default-expand-all
        :props="{ label: 'name', children: 'children' }"
      />
      <div class="drawer-footer">
        <el-button type="primary" @click="submitAssignPerm">保存</el-button>
      </div>
    </el-drawer>

    <!-- 菜单分配抽屉 -->
    <el-drawer v-model="menuDrawerVisible" title="分配菜单" size="320px">
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        node-key="id"
        show-checkbox
        check-strictly
        default-expand-all
        :props="{ label: 'title', children: 'children' }"
      />
      <div class="drawer-footer">
        <el-button type="primary" @click="submitAssignMenu">保存</el-button>
      </div>
    </el-drawer>
  </el-card>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
.drawer-footer {
  margin-top: 16px;
  text-align: right;
}
</style>
