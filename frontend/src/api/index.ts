import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'
import { useUserStore } from '@/store/user'

// 创建axios实例
const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return response
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        const userStore = useUserStore()
        userStore.logout()
        ElMessage.error('登录已过期，请重新登录')
        window.location.href = '/login'
      } else if (status === 403) {
        ElMessage.error('没有权限访问')
      } else if (status === 500) {
        ElMessage.error('服务器错误')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default api

// 认证相关
export const authApi = {
  login: (data: { username: string; password: string }) =>
    api.post('/auth/login', data),
  register: (data: { username: string; password: string; email?: string; nickname?: string }) =>
    api.post('/auth/register', data)
}

// 用户相关
export const userApi = {
  getProfile: () => api.get('/user/profile'),
  updateProfile: (data: Partial<{ nickname: string; email: string; avatar: string }>) =>
    api.put('/user/profile', data),
  recharge: (amount: number) => api.post('/user/recharge', { amount })
}

// 游戏相关
export const gameApi = {
  getGame: (id: number) => api.get(`/games/${id}`),
  searchGames: (params: Record<string, any>, signal?: AbortSignal) =>
    api.get('/games/search', { params, signal }),
  getHomeData: () => api.get('/games/home'),
  getFeatured: (limit?: number) => api.get('/games/featured', { params: { limit } }),
  getOnSale: (limit?: number) => api.get('/games/on-sale', { params: { limit } }),
  getBestSellers: (limit?: number) => api.get('/games/best-sellers', { params: { limit } }),
  getNewReleases: (limit?: number) => api.get('/games/new-releases', { params: { limit } }),
  getCategories: (gameId: number) => api.get(`/games/${gameId}/categories`)
}

// 分类相关
export const categoryApi = {
  getAll: () => api.get('/categories')
}

// 购物车相关
export const cartApi = {
  getCart: () => api.get('/cart'),
  addToCart: (gameId: number) => api.post('/cart', { gameId }),
  removeFromCart: (gameId: number) => api.delete(`/cart/${gameId}`),
  clearCart: () => api.delete('/cart'),
  getCartCount: () => api.get('/cart/count')
}

// 愿望单相关
export const wishlistApi = {
  getWishlist: () => api.get('/wishlist'),
  addToWishlist: (gameId: number) => api.post('/wishlist', { gameId }),
  removeFromWishlist: (gameId: number) => api.delete(`/wishlist/${gameId}`),
  checkWishlist: (gameId: number) => api.get(`/wishlist/check/${gameId}`)
}

// 优惠券相关
export const couponApi = {
  claimCoupon: (code: string) => api.post('/coupons/claim', { code }),
  getMyCoupons: (status?: string) => api.get('/coupons/my', { params: { status } }),
  getAvailableCoupons: () => api.get('/coupons/available'),
  getApplicableCoupons: (gameIds: number[]) => api.post('/coupons/applicable', { gameIds } as { gameIds: number[] }),
  getCouponByCode: (code: string) => api.get(`/coupons/${code}`)
}

// 游戏库相关
export const libraryApi = {
  getLibrary: () => api.get('/library'),
  checkOwnership: (gameId: number) => api.get(`/library/check/${gameId}`),
  getLibraryCount: () => api.get('/library/count')
}

// 评论相关
export const reviewApi = {
  getGameReviews: (gameId: number, page: number = 1, size: number = 10) =>
    api.get(`/reviews/game/${gameId}`, { params: { page, size } }),
  createReview: (data: { gameId: number; rating: number; content: string; isRecommend?: boolean }) =>
    api.post('/reviews', data),
  markHelpful: (reviewId: number) => api.post(`/reviews/${reviewId}/helpful`)
}

// 成就相关
export const achievementApi = {
  getMyAchievements: () => api.get('/achievements/my'),
  getAchievementStats: () => api.get('/achievements/stats'),
  getRecentUnlocked: (limit: number = 5) => api.get('/achievements/recent', { params: { limit } })
}

// 游戏库扩展
export const libraryExtApi = {
  updatePlayTime: (gameId: number, minutes: number) =>
    api.post(`/library/${gameId}/playtime`, { minutes })
}

// 退款相关
export const refundApi = {
  applyRefund: (data: { orderItemId: number; reason: string }) =>
    api.post('/refunds/apply', data),
  checkEligibility: (orderItemId: number) =>
    api.get(`/refunds/check/${orderItemId}`),
  getMyRefunds: () => api.get('/refunds/my'),
  getRefundDetail: (id: number) => api.get(`/refunds/${id}`),
  getAllRefunds: (status?: string) =>
    api.get('/refunds/admin/list', { params: status ? { status } : {} }),
  reviewRefund: (data: { refundId: number; action: string; remark?: string }) =>
    api.post('/refunds/admin/review', data)
}

// 好友相关
export const friendApi = {
  getFriends: () => api.get('/friends/list'),
  getPendingRequests: () => api.get('/friends/pending'),
  getPendingCount: () => api.get('/friends/pending/count'),
  getBlockedUsers: () => api.get('/friends/blocked'),
  searchUsers: (keyword: string, page: number = 1, size: number = 10) =>
    api.get('/friends/search', { params: { keyword, page, size } }),
  sendRequest: (friendId: number) => api.post(`/friends/request/${friendId}`),
  acceptRequest: (friendId: number) => api.post(`/friends/accept/${friendId}`),
  rejectRequest: (friendId: number) => api.post(`/friends/reject/${friendId}`),
  deleteFriend: (friendId: number) => api.delete(`/friends/${friendId}`),
  blockUser: (friendId: number) => api.post(`/friends/block/${friendId}`),
  unblockUser: (friendId: number) => api.post(`/friends/unblock/${friendId}`),
  getFriendshipStatus: (targetUserId: number) =>
    api.get(`/friends/status/${targetUserId}`)
}

// 动态相关
export const activityApi = {
  getFriendActivities: (page: number = 1, size: number = 10) =>
    api.get('/activities/friends', { params: { page, size } }),
  getMyActivities: (page: number = 1, size: number = 10) =>
    api.get('/activities/mine', { params: { page, size } })
}

// 预购相关
export const preorderApi = {
  getMyPreorders: () => api.get('/preorders'),
  getPreorderCount: () => api.get('/preorders/count'),
  checkPreordered: (gameId: number) => api.get(`/preorders/check/${gameId}`),
  getPreorderSummary: () => api.get('/preorders/summary')
}

// 管理员 - 游戏管理
export const adminGameApi = {
  getGames: (params: Record<string, any>) =>
    api.get('/admin/games', { params }),
  updateStock: (id: number, stock: number) =>
    api.put(`/admin/games/${id}/stock`, { stock }),
  batchUpdateStock: (gameIds: number[], stock: number) =>
    api.put('/admin/games/stock/batch', { gameIds, stock }),
  batchUpdateStatus: (gameIds: number[], status: number) =>
    api.put('/admin/games/status/batch', { gameIds, status }),
  getStats: () => api.get('/admin/games/stats')
}

// 钱包相关
export const walletApi = {
  getOverview: () => api.get('/wallet/overview'),
  getTransactions: (params?: { type?: string; month?: string; page?: number; size?: number }) =>
    api.get('/wallet/transactions', { params }),
  getMonthlySummary: (month?: string) =>
    api.get('/wallet/summary', { params: month ? { month } : {} }),
  getTrendData: () => api.get('/wallet/trend')
}

// 订单相关（支持赠送）
export const orderApi = {
  createOrder: (data: { gameIds: number[]; userCouponId?: number; recipientId?: number; giftMessage?: string }) =>
    api.post('/orders', data),
  payOrder: (orderNo: string) => api.post(`/orders/${orderNo}/pay`),
  cancelOrder: (orderNo: string) => api.post(`/orders/${orderNo}/cancel`),
  getOrders: () => api.get('/orders'),
  getOrderDetail: (orderNo: string) => api.get(`/orders/${orderNo}`)
}

// 礼物相关
export const giftApi = {
  getReceivedGifts: (status?: string) =>
    api.get('/gifts/received', { params: status ? { status } : {} }),
  getSentGifts: (status?: string) =>
    api.get('/gifts/sent', { params: status ? { status } : {} }),
  getPendingCount: () => api.get('/gifts/pending/count'),
  getGiftDetail: (giftId: number) => api.get(`/gifts/${giftId}`),
  claimGift: (giftId: number) => api.post(`/gifts/${giftId}/claim`),
  rejectGift: (giftId: number) => api.post(`/gifts/${giftId}/reject`)
}
