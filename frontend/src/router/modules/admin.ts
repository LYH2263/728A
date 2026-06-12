import type { RouteRecordRaw } from 'vue-router'

const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin/games',
    name: 'AdminGames',
    component: () => import('@/views/AdminGames.vue'),
    meta: { title: '游戏库存管理', requiresAuth: true, requiresAdmin: true }
  }
]

export default adminRoutes
