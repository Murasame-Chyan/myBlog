import { useUserStore } from '@/stores/user'

// 自定义指令 v-permission
// 用法：<el-button v-permission="'blog:delete'">删除</el-button>
// 效果：当前用户没有该权限码时，该元素从 DOM 中移除
export default {
  install(app) {
    app.directive('permission', {
      mounted(el, binding) {
        const userStore = useUserStore()
        const perm = binding.value
        // 有权限码但用户无此权限 → 移除元素
        if (perm && !userStore.permissions.includes(perm)) {
          el.parentNode && el.parentNode.removeChild(el)
        }
      },
    })
  },
}
