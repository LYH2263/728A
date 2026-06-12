// API 响应
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分页响应
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
  totalPages: number
}

// 用户
export interface User {
  id: number
  username: string
  email?: string
  nickname?: string
  avatar?: string
  balance?: number
  role?: string
}

// 登录响应
export interface LoginResponse {
  token: string
  userInfo: User
}

// 游戏
export interface Game {
  id: number
  title: string
  description?: string
  detailDescription?: string
  coverImage?: string
  bannerImage?: string
  screenshots?: string
  videoUrl?: string
  originalPrice: number
  discountPrice?: number
  discountPercent?: number
  developer?: string
  publisher?: string
  releaseDate?: string
  minRequirements?: string
  recRequirements?: string
  tags?: string
  stock?: number
  salesCount?: number
  rating?: number
  ratingCount?: number
  isFeatured?: number
}

// 分类
export interface Category {
  id: number
  name: string
  description?: string
  icon?: string
  sortOrder?: number
}

// 购物车项
export interface CartItem {
  id: number
  userId: number
  gameId: number
  quantity: number
  game: Game
}

// 愿望单项
export interface WishlistItem {
  id: number
  userId: number
  gameId: number
  createdAt: string
  game: Game
}

// 订单
export interface Order {
  id: number
  orderNo: string
  userId: number
  totalAmount: number
  payAmount: number
  discountAmount?: number
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'COMPLETED'
  payTime?: string
  createdAt: string
  orderItems?: OrderItem[]
}

// 订单项
export interface OrderItem {
  id: number
  orderId: number
  gameId: number
  gameTitle: string
  gameCover?: string
  price: number
  quantity: number
}

// 用户游戏库
export interface UserLibrary {
  id: number
  userId: number
  gameId: number
  orderId?: number
  playTime?: number
  lastPlayedAt?: string
  createdAt: string
  game: Game
}

// 评论
export interface GameReview {
  id: number
  userId: number
  gameId: number
  rating: number
  content?: string
  isRecommend?: number
  helpfulCount?: number
  createdAt: string
  user?: User
}

// 游戏查询参数
export interface GameQueryParams {
  keyword?: string
  categoryId?: number
  priceRange?: string
  sortBy?: string
  sortOrder?: string
  page?: number
  size?: number
  onSale?: boolean
  featured?: boolean
}

// 优惠券
export interface Coupon {
  id: number
  name: string
  code: string
  type: 'FULL_REDUCTION' | 'DISCOUNT' | 'CATEGORY'
  value: number
  minAmount: number
  categoryId?: number
  totalCount: number
  claimedCount: number
  perUserLimit: number
  validStart: string
  validEnd: string
  description?: string
  status: number
  createdAt: string
}

// 用户优惠券
export interface UserCoupon {
  id: number
  userId: number
  couponId: number
  orderId?: number
  status: 'UNUSED' | 'USED' | 'EXPIRED'
  usedAt?: string
  createdAt: string
  coupon?: Coupon
}

// 订单
export interface Order {
  id: number
  orderNo: string
  userId: number
  userCouponId?: number
  totalAmount: number
  payAmount: number
  discountAmount?: number
  couponDiscount?: number
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'COMPLETED'
  payTime?: string
  createdAt: string
  orderItems?: OrderItem[]
  userCoupon?: UserCoupon
}

// 成就定义
export interface Achievement {
  id: number
  code: string
  name: string
  description: string
  icon?: string
  category: 'GENERAL' | 'PURCHASE' | 'PLAYTIME' | 'REVIEW' | 'COLLECTION'
  targetValue: number
  isProgress: number
  rarity: 1 | 2 | 3 | 4
  points: number
  eventType: string
  ruleConfig?: string
  sortOrder: number
  status: number
}

// 用户成就进度
export interface UserAchievement {
  id?: number
  userId: number
  achievementId: number
  progress: number
  targetValue: number
  isUnlocked: number
  unlockedAt?: string
  createdAt?: string
  achievement: Achievement
}

// 成就统计
export interface AchievementStats {
  totalCount: number
  unlockedCount: number
  totalPoints: number
  completionRate: number
}
