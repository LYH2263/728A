// API 响应
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  unlockedAchievements?: UnlockedAchievementVO[]
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
  status?: number
  isFeatured?: number
  lowStock?: boolean
  releaseStatus?: 'RELEASED' | 'PREORDER' | 'CROWDFUNDING'
  crowdfundingGoal?: number
  currentFunding?: number
  supporterCount?: number
  preorderUnlockDate?: string
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
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'COMPLETED' | 'PARTIAL_REFUND' | 'FULL_REFUND'
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
  refundable?: boolean
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
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'COMPLETED' | 'PARTIAL_REFUND' | 'FULL_REFUND'
  payTime?: string
  createdAt: string
  orderItems?: OrderItem[]
  userCoupon?: UserCoupon
}

// 退款申请
export interface RefundRequest {
  id: number
  refundNo: string
  orderId: number
  orderNo: string
  userId: number
  gameId: number
  gameTitle: string
  gameCover?: string
  orderItemId: number
  orderItemPrice: number
  reason: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REFUNDED'
  reviewUserId?: number
  reviewRemark?: string
  reviewedAt?: string
  refundedAt?: string
  createdAt: string
  updatedAt?: string
  user?: User
  reviewUser?: User
  order?: Order
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

// 解锁成就VO（后端接口返回的轻量对象）
export interface UnlockedAchievementVO {
  achievementId: number
  code: string
  name: string
  description: string
  icon?: string
  category: string
  rarity: number
  points: number
  unlockedAt?: string
}

// 好友关系
export interface Friendship {
  id: number
  userId: number
  friendId: number
  status: 'PENDING' | 'ACCEPTED' | 'BLOCKED'
  actionUserId: number
  createdAt: string
  updatedAt: string
  friendUser?: User
}

// 好友动态
export interface Activity {
  id: number
  userId: number
  type: 'PURCHASE' | 'ACHIEVEMENT' | 'REVIEW'
  gameId?: number
  gameTitle?: string
  gameCover?: string
  achievementId?: number
  achievementName?: string
  reviewId?: number
  reviewRating?: number
  reviewContent?: string
  metadata?: string
  createdAt: string
  user?: User
}

// 用户预购记录
export interface UserPreorder {
  id: number
  userId: number
  gameId: number
  orderId: number
  orderItemId: number
  pricePaid: number
  releaseStatus: 'PREORDER' | 'CROWDFUNDING'
  status: 'PENDING_RELEASE' | 'RELEASED' | 'CANCELLED'
  convertedAt?: string
  createdAt: string
  updatedAt?: string
  game: Game
}

// 预购汇总
export interface PreorderSummary {
  totalCount: number
  pendingCount: number
  releasedCount: number
  totalPaid: number
}

// 钱包交易类型
export type WalletTransactionType = 'RECHARGE' | 'PURCHASE' | 'REFUND' | 'GIFT'

// 钱包流水
export interface WalletTransaction {
  id: number
  userId: number
  type: WalletTransactionType
  amount: number
  balanceBefore: number
  balanceAfter: number
  orderNo?: string
  description?: string
  createdAt: string
}

// 月度汇总
export interface WalletMonthlySummary {
  month: string
  income: number
  expense: number
  netIncome: number
}

// 趋势数据
export interface WalletTrendItem {
  month: string
  income: number
  expense: number
}

// 钱包概览
export interface WalletOverview {
  balance: number
  monthlySummary: WalletMonthlySummary
  trendData: WalletTrendItem[]
}

// 礼物
export interface Gift {
  id: number
  giftNo: string
  senderId: number
  recipientId: number
  gameId: number
  gameTitle: string
  gameCover?: string
  orderId: number
  orderItemId: number
  pricePaid: number
  status: 'PENDING' | 'CLAIMED' | 'REJECTED'
  message?: string
  claimedAt?: string
  rejectedAt?: string
  createdAt: string
  updatedAt?: string
  sender?: User
  recipient?: User
  game?: Game
}
