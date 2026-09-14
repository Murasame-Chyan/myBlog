import { useUserStore } from '@/stores/user'

// 组合式函数：在 <script setup> 里判断权限用
// 用法：const { hasPerm } = usePermission()
//      if (hasPerm('blog:delete')) { ... }
export function usePermission() {
  const userStore = useUserStore()

  const hasPerm = (perm) => {
    if (!perm) return true
    return userStore.permissions.includes(perm)
  }

  return { hasPerm }
}
