<template>
  <div class="game-detail-page" v-if="game">
    <!-- 头部横幅 -->
    <div class="hero-banner" :style="{ backgroundImage: `url(${game.bannerImage || game.coverImage})` }">
      <div class="hero-overlay"></div>
      <!-- 发售状态徽章 -->
      <div class="status-banner" v-if="releaseStatus !== 'RELEASED'">
        <template v-if="releaseStatus === 'PREORDER'">
          <el-tag type="warning" effect="dark" size="large" round>
            <el-icon><Clock /></el-icon>
            <span style="margin-left: 6px">预购中 · 发售倒计时</span>
          </el-tag>
          <span class="countdown-text" v-if="countdownText">{{ countdownText }}</span>
        </template>
        <template v-else-if="releaseStatus === 'CROWDFUNDING'">
          <el-tag type="danger" effect="dark" size="large" round>
            <el-icon><Promotion /></el-icon>
            <span style="margin-left: 6px">众筹中 · 募集进度</span>
          </el-tag>
          <span class="crowdfund-text" v-if="game.crowdfundingGoal">
            已筹 ¥{{ formatNumber(game.currentFunding || 0) }} / 目标 ¥{{ formatNumber(game.crowdfundingGoal) }}
          </span>
        </template>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="content-container">
      <!-- 左侧：游戏信息 -->
      <div class="main-content">
        <!-- 基本信息 -->
        <div class="game-header">
          <h1>{{ game.title }}</h1>
          <p class="description">{{ game.description }}</p>

          <!-- 标签 -->
          <div class="tags" v-if="parsedTags.length">
            <el-tag v-for="tag in parsedTags" :key="tag" effect="plain">{{ tag }}</el-tag>
          </div>
        </div>

        <!-- 众筹进度卡（CROWDFUNDING） -->
        <div class="crowdfunding-card" v-if="releaseStatus === 'CROWDFUNDING' && game.crowdfundingGoal">
          <div class="cf-row">
            <div class="cf-stat">
              <span class="cf-value big">¥{{ formatNumber(game.currentFunding || 0) }}</span>
              <span class="cf-label">已筹金额</span>
            </div>
            <div class="cf-stat">
              <span class="cf-value">{{ fundingPercent }}%</span>
              <span class="cf-label">完成度</span>
            </div>
            <div class="cf-stat">
              <span class="cf-value">{{ game.supporterCount || 0 }}</span>
              <span class="cf-label">支持者</span>
            </div>
            <div class="cf-stat">
              <span class="cf-value" :class="fundingGoalReached ? 'success' : 'warning'">
                {{ fundingGoalReached ? '已达成' : '募集中' }}
              </span>
              <span class="cf-label">状态</span>
            </div>
          </div>
          <el-progress
            :percentage="fundingPercent"
            :color="fundingGoalReached ? '#a4d007' : '#e6a23c'"
            :stroke-width="16"
            :show-text="false"
          />
          <div class="cf-goal-row">
            <span>目标：¥{{ formatNumber(game.crowdfundingGoal) }}</span>
            <span>还需：¥{{ formatNumber(Math.max(0, (game.crowdfundingGoal || 0) - (game.currentFunding || 0))) }}</span>
          </div>
        </div>

        <!-- 预购倒计时卡（PREORDER） -->
        <div class="preorder-card" v-if="releaseStatus === 'PREORDER' && game.preorderUnlockDate">
          <div class="po-header">
            <el-icon :size="28" color="#e6a23c"><Clock /></el-icon>
            <h3>距离发售还剩</h3>
          </div>
          <div class="po-countdown">
            <div class="cd-item">
              <span class="cd-num">{{ countdown.days }}</span>
              <span class="cd-label">天</span>
            </div>
            <div class="cd-sep">:</div>
            <div class="cd-item">
              <span class="cd-num">{{ countdown.hours }}</span>
              <span class="cd-label">时</span>
            </div>
            <div class="cd-sep">:</div>
            <div class="cd-item">
              <span class="cd-num">{{ countdown.minutes }}</span>
              <span class="cd-label">分</span>
            </div>
            <div class="cd-sep">:</div>
            <div class="cd-item">
              <span class="cd-num">{{ countdown.seconds }}</span>
              <span class="cd-label">秒</span>
            </div>
          </div>
          <p class="po-date">
            正式解锁：{{ formatDateTime(game.preorderUnlockDate) }}
          </p>
          <div class="po-tips">
            <el-icon><InfoFilled /></el-icon>
            预购成功后，游戏将在发售日自动加入您的游戏库
          </div>
        </div>

        <!-- 截图轮播 -->
        <div class="screenshots-section" v-if="parsedScreenshots.length">
          <h2 class="section-title">游戏截图</h2>
          <el-carousel height="360px" :interval="4000">
            <el-carousel-item v-for="(img, index) in parsedScreenshots" :key="index">
              <img
                v-lazy="img"
                :alt="`截图 ${index + 1}`"
                class="screenshot-img"
                @error="handleImageError($event, index)"
              />
            </el-carousel-item>
          </el-carousel>
        </div>

        <!-- 详细介绍（富文本渲染） -->
        <div class="detail-section" v-if="game.detailDescription">
          <h2 class="section-title">关于游戏</h2>
          <RichText :content="game.detailDescription" />
        </div>

        <!-- 系统需求 -->
        <div class="requirements-section" v-if="minReq || recReq">
          <h2 class="section-title">系统需求</h2>
          <div class="requirements-grid">
            <div class="requirement-card" v-if="minReq">
              <h4>最低配置</h4>
              <ul>
                <li v-if="minReq.os"><strong>操作系统：</strong>{{ minReq.os }}</li>
                <li v-if="minReq.cpu"><strong>处理器：</strong>{{ minReq.cpu }}</li>
                <li v-if="minReq.memory"><strong>内存：</strong>{{ minReq.memory }}</li>
                <li v-if="minReq.gpu"><strong>显卡：</strong>{{ minReq.gpu }}</li>
                <li v-if="minReq.storage"><strong>存储空间：</strong>{{ minReq.storage }}</li>
              </ul>
            </div>
            <div class="requirement-card" v-if="recReq">
              <h4>推荐配置</h4>
              <ul>
                <li v-if="recReq.os"><strong>操作系统：</strong>{{ recReq.os }}</li>
                <li v-if="recReq.cpu"><strong>处理器：</strong>{{ recReq.cpu }}</li>
                <li v-if="recReq.memory"><strong>内存：</strong>{{ recReq.memory }}</li>
                <li v-if="recReq.gpu"><strong>显卡：</strong>{{ recReq.gpu }}</li>
                <li v-if="recReq.storage"><strong>存储空间：</strong>{{ recReq.storage }}</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 评论区 -->
        <div class="reviews-section">
          <div class="reviews-header">
            <h2 class="section-title">玩家评测</h2>
            <div class="review-restriction" v-if="releaseStatus !== 'RELEASED'">
              <el-alert
                type="warning"
                :closable="false"
                show-icon
                title="游戏尚未正式发售，暂无法发表评论"
              />
            </div>
          </div>
          <div v-if="reviews.length" class="reviews-list">
            <div v-for="review in reviews" :key="review.id" class="review-item">
              <div class="review-header">
                <el-avatar :size="40" :src="getAvatarUrl(review.user?.avatar)">
                  {{ review.user?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="review-user">
                  <span class="username">{{ review.user?.nickname || review.user?.username }}</span>
                  <span class="recommend" :class="{ positive: review.isRecommend }">
                    {{ review.isRecommend ? '👍 推荐' : '👎 不推荐' }}
                  </span>
                </div>
                <el-rate :model-value="review.rating" disabled size="small" />
              </div>
              <p class="review-content">{{ review.content }}</p>
              <div class="review-footer">
                <span class="time">{{ formatDate(review.createdAt) }}</span>
                <el-button text size="small" @click="handleHelpful(review.id)">
                  <el-icon><Pointer /></el-icon>
                  有帮助 ({{ review.helpfulCount }})
                </el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无评测" />
        </div>
      </div>

      <!-- 右侧：购买信息 -->
      <aside class="sidebar">
        <div class="purchase-card">
          <img v-lazy="game.coverImage" :alt="game.title" class="cover-img" />

          <!-- 状态徽章 -->
          <div class="status-chip-row">
            <el-tag v-if="releaseStatus === 'RELEASED'" type="success" effect="light" round>
              <el-icon><CircleCheck /></el-icon>
              <span style="margin-left:4px">已发售</span>
            </el-tag>
            <el-tag v-else-if="releaseStatus === 'PREORDER'" type="warning" effect="light" round>
              <el-icon><Clock /></el-icon>
              <span style="margin-left:4px">预购中</span>
            </el-tag>
            <el-tag v-else type="danger" effect="light" round>
              <el-icon><Promotion /></el-icon>
              <span style="margin-left:4px">众筹中</span>
            </el-tag>
          </div>

          <!-- 评分 -->
          <div class="rating-info" v-if="game.rating && (game.ratingCount ?? 0) > 0">
            <el-rate :model-value="game.rating" disabled :max="5" />
            <span class="rating-text">{{ game.rating }} ({{ game.ratingCount }}条评测)</span>
          </div>
          <div class="rating-info" v-else>
            <span class="rating-text muted">暂无评测</span>
          </div>

          <!-- 游戏信息 -->
          <div class="game-meta">
            <div class="meta-item" v-if="game.developer">
              <span class="label">开发商</span>
              <span class="value">{{ game.developer }}</span>
            </div>
            <div class="meta-item" v-if="game.publisher">
              <span class="label">发行商</span>
              <span class="value">{{ game.publisher }}</span>
            </div>
            <div class="meta-item">
              <span class="label">{{ releaseStatus === 'RELEASED' ? '发行日期' : '预计发售' }}</span>
              <span class="value">{{ game.releaseDate || '待定' }}</span>
            </div>
          </div>

          <!-- 价格 -->
          <div class="price-section">
            <div class="preorder-note" v-if="releaseStatus === 'PREORDER'">
              <el-icon><Star /></el-icon> 预购特惠价
            </div>
            <div class="preorder-note crowdfund" v-else-if="releaseStatus === 'CROWDFUNDING'">
              <el-icon><Promotion /></el-icon> 众筹早鸟价
            </div>
            <template v-if="isFree">
              <span class="price free">免费游玩</span>
            </template>
            <template v-else-if="hasDiscount">
              <div class="discount-info">
                <span class="discount-tag">-{{ game.discountPercent }}%</span>
                <span class="original-price">¥{{ game.originalPrice }}</span>
              </div>
              <span class="final-price">¥{{ game.discountPrice }}</span>
            </template>
            <template v-else>
              <span class="final-price">¥{{ game.originalPrice }}</span>
            </template>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <template v-if="ownsGame">
              <el-button type="success" size="large" disabled style="width: 100%">
                <el-icon><Check /></el-icon>
                已拥有
              </el-button>
            </template>
            <template v-else-if="inPreorder">
              <el-button type="warning" size="large" disabled style="width: 100%">
                <el-icon><Clock /></el-icon>
                {{ releaseStatus === 'CROWDFUNDING' ? '已支持众筹' : '已预购' }}
              </el-button>
            </template>
            <template v-else>
              <el-button
                :type="releaseStatus === 'RELEASED' ? 'primary' : 'warning'"
                size="large"
                style="width: 100%"
                @click="handleAddToCart"
                :disabled="inCart"
              >
                <el-icon><ShoppingCart /></el-icon>
                <template v-if="inCart">已在购物车</template>
                <template v-else-if="releaseStatus === 'PREORDER'">立即预购</template>
                <template v-else-if="releaseStatus === 'CROWDFUNDING'">支持众筹</template>
                <template v-else>加入购物车</template>
              </el-button>
              <el-button
                size="large"
                style="width: 100%;margin-left:0;"
                @click="handleWishlist"
              >
                <el-icon><Star /></el-icon>
                {{ inWishlist ? '移出愿望单' : '加入愿望单' }}
              </el-button>
            </template>
          </div>

          <!-- 底部说明 -->
          <div class="purchase-note" v-if="releaseStatus !== 'RELEASED'">
            <el-icon><InfoFilled /></el-icon>
            <span v-if="releaseStatus === 'PREORDER'">
              预购后将进入"预购库"，游戏发售后自动转入正式游戏库
            </span>
            <span v-else>
              众筹成功后，游戏发售后将自动转入正式游戏库
            </span>
          </div>
        </div>
      </aside>
    </div>
  </div>

  <!-- 加载状态 -->
  <div v-else-if="loading" class="loading-container">
    <el-skeleton :rows="10" animated />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gameApi, reviewApi, wishlistApi, libraryApi, preorderApi } from '@/api'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import type { Game, GameReview } from '@/types'
import { ElMessage } from 'element-plus'
import RichText from '@/components/RichText.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const loading = ref(true)
const game = ref<Game | null>(null)
const reviews = ref<GameReview[]>([])
const ownsGame = ref(false)
const inWishlist = ref(false)
const inPreorder = ref(false)

const gameId = computed(() => Number(route.params.id))
const isFree = computed(() => game.value?.originalPrice === 0)
const releaseStatus = computed(() => (game.value?.releaseStatus || 'RELEASED') as 'RELEASED' | 'PREORDER' | 'CROWDFUNDING')

// 获取头像URL，处理默认头像
function getAvatarUrl(avatar: string | undefined): string {
  if (!avatar || avatar === '/avatars/default.png') {
    return '/avatars/default.svg'
  }
  return avatar
}

// 默认截图占位图
const defaultScreenshot = 'data:image/svg+xml,' + encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="640" height="360" viewBox="0 0 640 360">
  <rect fill="#1b2838" width="640" height="360"/>
  <text x="320" y="170" text-anchor="middle" fill="#66c0f4" font-size="24" font-family="Arial">
    图片加载失败
  </text>
  <text x="320" y="200" text-anchor="middle" fill="#8f98a0" font-size="14" font-family="Arial">
    Screenshot unavailable
  </text>
</svg>
`)

// 截图加载失败处理
function handleImageError(event: Event, _index: number) {
  const img = event.target as HTMLImageElement
  if (img && img.src !== defaultScreenshot) {
    img.src = defaultScreenshot
  }
}

const hasDiscount = computed(() => game.value?.discountPercent && game.value.discountPercent > 0)
const inCart = computed(() => cartStore.isInCart(gameId.value))

const parsedTags = computed(() => {
  if (!game.value?.tags) return []
  try { return JSON.parse(game.value.tags) } catch { return [] }
})

const parsedScreenshots = computed(() => {
  if (!game.value?.screenshots) return []
  try { return JSON.parse(game.value.screenshots) } catch { return [] }
})

const minReq = computed(() => {
  if (!game.value?.minRequirements) return null
  try { return JSON.parse(game.value.minRequirements) } catch { return null }
})

const recReq = computed(() => {
  if (!game.value?.recRequirements) return null
  try { return JSON.parse(game.value.recRequirements) } catch { return null }
})

// ================== 倒计时（预购/众筹） ==================
const countdown = ref({ days: 0, hours: 0, minutes: 0, seconds: 0 })
const countdownText = computed(() => {
  const c = countdown.value
  return `${c.days}天 ${c.hours}时 ${c.minutes}分 ${c.seconds}秒`
})
let countdownTimer: ReturnType<typeof setInterval> | null = null

function updateCountdown() {
  if (!game.value?.preorderUnlockDate) return
  const target = new Date(game.value.preorderUnlockDate).getTime()
  const now = Date.now()
  const diff = Math.max(0, target - now)
  const d = Math.floor(diff / 86400000)
  const h = Math.floor((diff % 86400000) / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  countdown.value = { days: d, hours: h, minutes: m, seconds: s }
}

function startCountdown() {
  stopCountdown()
  updateCountdown()
  countdownTimer = setInterval(updateCountdown, 1000)
}
function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// ================== 众筹进度 ==================
const fundingPercent = computed(() => {
  if (!game.value?.crowdfundingGoal) return 0
  const goal = game.value.crowdfundingGoal
  const cur = game.value.currentFunding || 0
  const pct = Math.min(100, Math.round((cur / goal) * 10000) / 100)
  return pct
})
const fundingGoalReached = computed(() => {
  if (!game.value?.crowdfundingGoal) return false
  return (game.value.currentFunding || 0) >= game.value.crowdfundingGoal
})

// ================== 工具方法 ==================
function formatNumber(n: number) {
  if (n == null) return '0'
  return Number(n).toLocaleString('zh-CN')
}
function formatDate(date: string) {
  return new Date(date).toLocaleDateString('zh-CN')
}
function formatDateTime(date: string) {
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// ================== 生命周期 ==================
onMounted(async () => {
  await fetchGame()
  await Promise.all([
    fetchReviews(),
    checkOwnership(),
    checkWishlist(),
    checkPreorder()
  ])
  loading.value = false
  if (game.value && releaseStatus.value !== 'RELEASED') {
    startCountdown()
  }
})

onBeforeUnmount(() => {
  stopCountdown()
})

async function fetchGame() {
  try {
    const res = await gameApi.getGame(gameId.value)
    game.value = res.data.data
  } catch (error) {
    ElMessage.error('游戏不存在')
    router.push('/store')
  }
}

async function fetchReviews() {
  try {
    const res = await reviewApi.getGameReviews(gameId.value, 1, 10)
    reviews.value = res.data.data.list || []
  } catch (error) {
    console.error('Failed to fetch reviews')
  }
}

async function checkOwnership() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await libraryApi.checkOwnership(gameId.value)
    ownsGame.value = res.data.data
  } catch (error) {
    ownsGame.value = false
  }
}

async function checkWishlist() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await wishlistApi.checkWishlist(gameId.value)
    inWishlist.value = res.data.data
  } catch (error) {
    inWishlist.value = false
  }
}

async function checkPreorder() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await preorderApi.checkPreordered(gameId.value)
    inPreorder.value = res.data.data
  } catch (error) {
    inPreorder.value = false
  }
}

async function handleAddToCart() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  await cartStore.addToCart(gameId.value)
}

async function handleWishlist() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  try {
    if (inWishlist.value) {
      await wishlistApi.removeFromWishlist(gameId.value)
      inWishlist.value = false
      ElMessage.success('已从愿望单移除')
    } else {
      await wishlistApi.addToWishlist(gameId.value)
      inWishlist.value = true
      ElMessage.success('已加入愿望单')
    }
  } catch (error) {
    // 错误已处理
  }
}

async function handleHelpful(reviewId: number) {
  try {
    await reviewApi.markHelpful(reviewId)
    ElMessage.success('感谢您的反馈')
  } catch (error) {
    // 错误已处理
  }
}
</script>

<style lang="scss" scoped>
.game-detail-page {
  position: relative;
}

// 头部横幅
.hero-banner {
  height: 440px;
  background-size: cover;
  background-position: center;
  position: absolute;
  top: -20px;
  left: -20px;
  right: -20px;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, transparent 0%, var(--steam-dark) 100%);
}

.status-banner {
  position: absolute;
  bottom: 24px;
  left: 40px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .countdown-text {
    color: #e6a23c;
    font-weight: 600;
    font-size: 16px;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8);
  }
  .crowdfund-text {
    color: #f56c6c;
    font-weight: 600;
    font-size: 15px;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8);
  }
}

// 内容容器
.content-container {
  position: relative;
  z-index: 1;
  padding-top: 220px;
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 30px;
}

// 主要内容
.main-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.game-header {
  h1 {
    font-size: 36px;
    font-weight: 700;
    color: var(--text-white);
    margin-bottom: 16px;
  }

  .description {
    color: var(--text-light);
    font-size: 16px;
    line-height: 1.6;
    margin-bottom: 16px;
  }

  .tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

// 众筹进度卡
.crowdfunding-card {
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.12), rgba(230, 162, 60, 0.08));
  border: 1px solid rgba(245, 108, 108, 0.3);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  .cf-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
  }
  .cf-stat {
    display: flex;
    flex-direction: column;
    text-align: center;

    .cf-value {
      color: var(--text-white);
      font-weight: 700;
      font-size: 20px;

      &.big {
        color: #f56c6c;
        font-size: 22px;
      }
      &.success {
        color: var(--steam-green);
      }
      &.warning {
        color: #e6a23c;
      }
    }
    .cf-label {
      color: var(--text-secondary);
      font-size: 12px;
      margin-top: 4px;
    }
  }
  .cf-goal-row {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    color: var(--text-secondary);
    margin-top: -8px;
  }
}

// 预购倒计时卡
.preorder-card {
  background: linear-gradient(135deg, rgba(230, 162, 60, 0.12), rgba(102, 192, 244, 0.06));
  border: 1px solid rgba(230, 162, 60, 0.35);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  .po-header {
    display: flex;
    align-items: center;
    gap: 10px;
    h3 {
      margin: 0;
      font-size: 16px;
      color: var(--text-white);
    }
  }
  .po-countdown {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 12px;
    background: rgba(0, 0, 0, 0.3);
    border-radius: var(--radius-md);

    .cd-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      min-width: 56px;

      .cd-num {
        font-size: 32px;
        font-weight: 700;
        color: #e6a23c;
        font-family: 'Courier New', monospace;
        line-height: 1;
      }
      .cd-label {
        font-size: 12px;
        color: var(--text-secondary);
        margin-top: 6px;
      }
    }
    .cd-sep {
      font-size: 24px;
      color: var(--text-secondary);
      font-weight: 700;
      padding-bottom: 18px;
    }
  }
  .po-date {
    text-align: center;
    color: var(--text-light);
    font-size: 13px;
    margin: 0;
  }
  .po-tips {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 12px;
    background: rgba(102, 192, 244, 0.08);
    color: var(--steam-light-blue);
    font-size: 13px;
    border-radius: var(--radius-sm);
  }
}

// 截图
.screenshots-section {
  .screenshot-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: var(--radius-md);
  }
}

// 详细介绍
.detail-section {
  .detail-content {
    color: var(--text-light);
    line-height: 1.8;
    white-space: pre-wrap;
  }
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-white);
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color);
}

// 系统需求
.requirements-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.requirement-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  border: 1px solid var(--border-color);

  h4 {
    color: var(--text-white);
    margin-bottom: 16px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--border-color);
  }

  ul {
    list-style: none;

    li {
      color: var(--text-light);
      font-size: 14px;
      margin-bottom: 8px;

      strong {
        color: var(--text-primary);
      }
    }
  }
}

// 评论区
.reviews-header {
  margin-bottom: 16px;
  .review-restriction {
    margin-top: 12px;
  }
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  border: 1px solid var(--border-color);
}

.review-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;

  .review-user {
    flex: 1;

    .username {
      display: block;
      color: var(--text-white);
      font-weight: 500;
    }

    .recommend {
      font-size: 12px;
      color: var(--text-secondary);

      &.positive {
        color: var(--steam-green);
      }
    }
  }
}

.review-content {
  color: var(--text-light);
  line-height: 1.6;
  margin-bottom: 12px;
}

.review-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .time {
    color: var(--text-secondary);
    font-size: 12px;
  }
}

// 侧边栏
.sidebar {
  position: sticky;
  top: 84px;
  height: fit-content;
}

.purchase-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  border: 1px solid var(--border-color);

  .cover-img {
    width: 100%;
    aspect-ratio: 460 / 215;
    object-fit: cover;
    border-radius: var(--radius-md);
    margin-bottom: 14px;
  }
}

.status-chip-row {
  margin-bottom: 12px;
  display: flex;
  justify-content: center;
}

.rating-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  justify-content: center;

  .rating-text {
    color: var(--text-secondary);
    font-size: 12px;

    &.muted {
      color: var(--text-secondary);
      opacity: 0.7;
    }
  }
}

.game-meta {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);

  .meta-item {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 14px;

    .label {
      color: var(--text-secondary);
    }

    .value {
      color: var(--text-primary);
      text-align: right;
      max-width: 60%;
      word-break: break-word;
    }
  }
}

.price-section {
  margin-bottom: 20px;
  text-align: center;

  .preorder-note {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    color: #e6a23c;
    font-size: 13px;
    margin-bottom: 10px;

    &.crowdfund {
      color: #f56c6c;
    }
  }

  .free {
    color: var(--steam-green);
    font-size: 24px;
    font-weight: 600;
  }

  .discount-info {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    .discount-tag {
      background: var(--steam-green);
      color: var(--steam-darker);
      padding: 4px 8px;
      border-radius: var(--radius-sm);
      font-weight: 700;
    }

    .original-price {
      color: var(--text-secondary);
      text-decoration: line-through;
    }
  }

  .final-price {
    font-size: 28px;
    font-weight: 700;
    color: var(--text-white);
  }
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.purchase-note {
  margin-top: 16px;
  padding: 10px 12px;
  background: rgba(102, 192, 244, 0.08);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--steam-light-blue);
  line-height: 1.5;
  display: flex;
  align-items: flex-start;
  gap: 6px;

  :deep(svg) {
    flex-shrink: 0;
    margin-top: 1px;
  }
}

.loading-container {
  padding: 40px;
}

// 响应式
@media (max-width: 900px) {
  .content-container {
    grid-template-columns: 1fr;
    padding-top: 160px;
  }

  .sidebar {
    position: static;
  }

  .requirements-grid {
    grid-template-columns: 1fr;
  }

  .crowdfunding-card .cf-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .hero-banner {
    height: 280px;
  }

  .content-container {
    padding-top: 120px;
  }

  .game-header h1 {
    font-size: 24px;
  }

  .po-countdown {
    flex-wrap: wrap;
    .cd-item {
      min-width: 44px;
      .cd-num {
        font-size: 24px;
      }
    }
  }

  .status-banner {
    left: 20px;
    right: 20px;
    bottom: 16px;
  }
}
</style>
