<script setup>
// 递归菜单项组件：
// - 有 children → 渲染 el-sub-menu（可展开）
// - 无 children → 渲染 el-menu-item（可点击跳转）
// 组件通过 <sidebar-item> 递归渲染自己
defineProps({
  menu: { type: Object, required: true },
})
</script>

<template>
  <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.path">
    <template #title>
      <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
      <span>{{ menu.title }}</span>
    </template>
    <sidebar-item v-for="child in menu.children" :key="child.id" :menu="child" />
  </el-sub-menu>

  <el-menu-item v-else :index="menu.path">
    <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
    <template #title>{{ menu.title }}</template>
  </el-menu-item>
</template>
