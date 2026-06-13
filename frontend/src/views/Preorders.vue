<template>
  <div class="preorders-page">
    <h1 class="page-title">
      <el-icon><Clock /></el-icon>
      我的预购库
    </h1>

    <div v-if="loading" class="empty-state">
      <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="preorders.length === 0" class="empty-state">
      <el-icon :size="64" color="var(--text-secondary)"><ShoppingBag /></el-icon>
      <h3>暂无预购游戏</h3>
      <p class="hint">预购的游戏将在此展示，发售日自动转入你的游戏库</p>
      <el-button type="primary" size="large" @click="goHome">去商城看看</el-button>
    </div>

    <template v-else>
      <!-- 汇总统计卡 -->
      <div class="summary-card">
        <div class="summary-item">
          <span class="summary-value">{{ summary.pendingCount || 0 }}</span>
          <span class="summary-label">待解锁</span>
        </div>
        <div class="divider"></div>
        <div class="summary-item">
          <span class="summary-value released">{{ summary.releasedCount || 0 }}</span>
          <span class="summary-label">已转正</span>
        </div>
        <div class="divider"></div>
        <div class="summary-item">
          <span class="summary-value price">¥{{ (summary.totalPaid || 0).toFixed(2) }}</span>
          <span class="summary-label">已付金额</span>
        </div>
      </div>

      <!-- 过滤切换 -->
      <div class="filter-bar">
        <el-radio-group v-model="statusFilter" size="default">
          <el-radio-button value="ALL">全部</el-radio-button>
          <el-radio-button value="PENDING_RELEASE">待解锁</el-radio-button>
          <el-radio-button value="RELEASED">已转正</el-radio-button>
        </el-radio-group>
        <div class="filter-count">共 {{ filteredPreorders.length }} 条记录</div>
      </div>

      <!-- 预购列表 -->
      <div class="preorder-list">
        <div
          v-for="item in filteredPreorders"
          :key="item.id"
          class="preorder-card"
          :class="{ released: item.status === 'RELEASED' }"
          @click="goToGameDetail(item.gameId)"
        >
          <div class="cover-wrap">
            <img
              :src="item.game?.coverImage || '/images/default-game.svg'"
              :alt="item.game?.title"
              class="cover"
            />
            <div class="cover-overlay">
              <el-tag v-if="item.releaseStatus === 'PREORDER'" type="warning" size="small">
                预购中
              </el-tag>
              <el-tag v-else-if="item.releaseStatus === 'CROWDFUNDING'" type="danger" size="small">
                众筹中
              </el-tag>
              <el-tag v-else type="success" size="small">已发售</el-tag>
            </div>
          </div>

          <div class="info">
            <div class="title-row">
              <h3 class="game-title">{{ item.game?.title }}</h3>
              <el-tag
                v-if="item.status === 'RELEASED'"
                type="success"
                effect="light"
                size="large"
              >
                <el-icon><CircleCheck /></el-icon>
                已转入游戏库
              </el-tag>
              <el-tag
                v-else
                :type="item.releaseStatus === 'CROWDFUNDING' ? 'danger' : 'warning'"
                effect="light"
                size="large"
              >
                等待发售
              </el-tag>
            </div>

            <div class="meta-row">
              <span class="meta-item">
                <el-icon><Money /></el-icon>
                实付 ¥{{ Number(item.pricePaid).toFixed(2) }}
              </span>
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                下单时间：{{ formatDate(item.createdAt) }}
              </span>
              <span v-if="item.convertedAt" class="meta-item released">
                <el-icon><CircleCheck /></el-icon>
                转正时间：{{ formatDate(item.convertedAt) }}
              </span>
            </div>

            <!-- 倒计时或进度条 -->
            <div v-if="item.status !== 'RELEASED' && item.game" class="progress-area">
              <!-- 预购倒计时 -->
              <template v-if="item.releaseStatus === 'PREORDER'">
                <div class="countdown-row">
                  <div class="countdown-label">
                    <el-icon><Timer /></el-icon>
                    距发售解锁还有：
                  </div>
                  <div class="countdown-blocks">
                    <div class="cd-block">
                      <span class="cd-num">{{ getItemCountdown(item).days }}</span>
                      <span class="cd-unit">天</span>
                    </div>
                    <span class="cd-sep">:</span>
                    <div class="cd-block">
                      <span class="cd-num">{{ getItemCountdown(item).hours }}</span>
                      <span class="cd-unit">时</span>
                    </div>
                    <span class="cd-sep">:</span>
                    <div class="cd-block">
                      <span class="cd-num">{{ getItemCountdown(item).minutes }}</span>
                      <span class="cd-unit">分</span>
                    </div>
                    <span class="cd-sep">:</span>
                    <div class="cd-block">
                      <span class="cd-num">{{ getItemCountdown(item).seconds }}</span>
                      <span class="cd-unit">秒</span>
                    </div>
                  </div>
                  <div class="countdown-date">
                    预计解锁：{{ formatDate(item.game.preorderUnlockDate) }}
                  </div>
                </div>
              </template>

              <!-- 众筹进度 -->
              <template v-else-if="item.releaseStatus === 'CROWDFUNDING'">
                <div class="crowdfunding-row">
                  <div class="cf-stats">
                    <div class="cf-stat">
                      <span class="cf-label">已筹金额</span>
                      <span class="cf-value main">
                        ¥{{ Number(item.game.currentFunding || 0).toFixed(2) }}
                      </span>
                    </div>
                    <div class="cf-stat">
                      <span class="cf-label">目标金额</span>
                      <span class="cf-value">
                        ¥{{ Number(item.game.crowdfundingGoal || 0).toFixed(2) }}
                      </span>
                    </div>
                    <div class="cf-stat">
                      <span class="cf-label">完成度</span>
                      <span class="cf-value" :class="{ reached: getFundingPercent(item) >= 100 }">
                        {{ getFundingPercent(item) }}%
                      </span>
                    </div>
                    <div class="cf-stat">
                      <span class="cf-label">支持者</span>
                      <span class="cf-value">{{ item.game.supporterCount || 0 }} 人</span>
                    </div>
                  </div>
                  <el-progress
                    :percentage="getFundingPercent(item)"
                    :color="getFundingPercent(item) >= 100 ? '#a4d007' : '#e6a23c'"
                    :stroke-width="10"
                    :show-text="false"
                  />
                  <div v-if="getFundingPercent(item) < 100" class="cf-remain">
                    还需 ¥{{ (Number(item.game.crowdfundingGoal || 0) - Number(item.game.currentFunding || 0)).toFixed(2) }} 达成
                  </div>
                  <div v-else class="cf-success">
                    <el-icon><Promotion /></el-icon>
                    众筹目标已达成！游戏将很快转入正式库
                  </div>
                </div>
              </template>
            </div>

            <div class="action-row">
              <el-button size="small" @click.stop="goToGameDetail(item.gameId)">
                <el-icon><View /></el-icon>
                查看详情
              </el-button>
              <el-button v-if="item.status === 'RELEASED'" size="small" type="primary" @click.stop="goToLibrary">
                <el-icon><Collection /></el-icon>
                前往游戏库
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { preorderApi } from '@/api'
import type { UserPreorder, PreorderSummary } from '@/types'

const router = useRouter()

const loading = ref(true)
const preorders = ref<UserPreorder[]>([])
const summary = ref<PreorderSummary>({
  totalCount: 0,
  pendingCount: 0,
  releasedCount: 0,
  totalPaid: 0
})
const statusFilter = ref('ALL')

// 倒计时缓存（按预购ID缓存计算结果）
const countdownCache = reactive<Record<number, { days: number; hours: number; minutes: number; seconds: number }>>({})
let timer: ReturnType<typeof setInterval> | null = null

const filteredPreorders = computed(() => {
  if (statusFilter.value === 'ALL') return preorders.value
  return preorders.value.filter((p) => p.status === statusFilter.value)
})

function formatDate(d: string | Date | undefined | null): string {
  if (!d) return '-'
  const date = typeof d === 'string' ? new Date(d) : d
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getItemCountdown(item: UserPreorder) {
  if (countdownCache[item.id]) return countdownCache[item.id]
  return { days: 0, hours: 0, minutes: 0, seconds: 0 }
}

function getFundingPercent(item: UserPreorder) {
  const goal = Number(item.game?.crowdfundingGoal || 0)
  const cur = Number(item.game?.currentFunding || 0)
  if (!goal) return 0
  return Math.min(100, Math.round((cur / goal) * 10000) / 100)
}

function updateCountdowns() {
  const now = Date.now()
  preorders.value.forEach((item) => {
    if (item.status === 'RELEASED' || !item.game?.preorderUnlockDate) {
      countdownCache[item.id] = { days: 0, hours: 0, minutes: 0, seconds: 0 }
      return
    }
    const target = new Date(item.game.preorderUnlockDate).getTime()
    let diff = target - now
    if (diff < 0) diff = 0
    const days = Math.floor(diff / 86400000)
    const hours = Math.floor((diff % 86400000) / 3600000)
    const minutes = Math.floor((diff % 3600000) / 60000)
    const seconds = Math.floor((diff % 60000) / 1000)
    countdownCache[item.id] = { days, hours, minutes, seconds }
  })
}

async function fetchPreorders() {
  loading.value = true
  try {
    const [listRes, summaryRes] = await Promise.all([
      preorderApi.getMyPreorders(),
      preorderApi.getPreorderSummary()
    ])
    preorders.value = listRes.data.data || []
    summary.value = summaryRes.data.data || summary.value
  } catch (err) {
    ElMessage.error('获取预购列表失败')
  } finally {
    loading.value = false
  }
}

function startTimer() {
  stopTimer()
  updateCountdowns()
  timer = setInterval(updateCountdowns, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

watch(
  () => preorders.value.length,
  () => startTimer()
)

onMounted(async () => {
  await fetchPreorders()
  startTimer()
})

onBeforeUnmount(() => stopTimer())

function goToGameDetail(gameId: number) {
  router.push(`/game/${gameId}`)
}

function goToLibrary() {
  router.push('/library')
}

function goHome() {
  router.push('/')
}
</script>

<style lang="scss" scoped>
.preorders-page {
  max-width: 1100px;
  margin: 0 auto;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  color: var(--text-white);
  margin-bottom: 24px;
}

.empty-state {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 80px 32px;
  text-align: center;

  h3 {
    color: var(--text-white);
    margin: 16px 0 8px;
    font-size: 20px;
  }

  .hint {
    color: var(--text-secondary);
    margin-bottom: 24px;
  }

  .loading-icon {
    color: var(--steam-light-blue);
    animation: rotate 1s linear infinite;
  }

  @keyframes rotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }
}

.summary-card {
  background: linear-gradient(135deg, var(--bg-card) 0%, #1b2838 100%);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px 32px;
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin-bottom: 20px;

  .summary-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    .summary-value {
      font-size: 28px;
      font-weight: 700;
      color: var(--text-white);

      &.released { color: var(--steam-green); }
      &.price { color: #ffd700; }
    }

    .summary-label {
      font-size: 13px;
      color: var(--text-secondary);
    }
  }

  .divider {
    width: 1px;
    height: 48px;
    background: var(--border-color);
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  .filter-count {
    color: var(--text-secondary);
    font-size: 14px;
  }
}

.preorder-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preorder-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
  display: flex;
  cursor: pointer;
  transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    border-color: var(--steam-light-blue);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  }

  &.released {
    border-left: 4px solid var(--steam-green);
  }

  .cover-wrap {
    position: relative;
    width: 300px;
    flex-shrink: 0;

    .cover {
      width: 100%;
      height: 100%;
      min-height: 180px;
      object-fit: cover;
      display: block;
    }

    .cover-overlay {
      position: absolute;
      top: 12px;
      left: 12px;
    }
  }

  .info {
    flex: 1;
    padding: 20px 24px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;

    .game-title {
      font-size: 20px;
      color: var(--text-white);
      margin: 0;
    }
  }

  .meta-row {
    display: flex;
    flex-wrap: wrap;
    gap: 16px 24px;
    color: var(--text-secondary);
    font-size: 14px;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;

      &.released { color: var(--steam-green); }
    }
  }

  .progress-area {
    background: rgba(0, 0, 0, 0.2);
    border-radius: var(--radius-md);
    padding: 16px;
  }

  .countdown-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;

    .countdown-label {
      color: var(--text-secondary);
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .countdown-blocks {
      display: flex;
      align-items: center;
      gap: 6px;

      .cd-block {
        background: #1b2838;
        border: 1px solid var(--border-color);
        border-radius: 8px;
        padding: 6px 10px;
        text-align: center;
        min-width: 50px;

        .cd-num {
          display: block;
          font-size: 20px;
          font-weight: 700;
          color: var(--steam-light-blue);
          line-height: 1.2;
        }

        .cd-unit {
          font-size: 11px;
          color: var(--text-secondary);
        }
      }

      .cd-sep {
        color: var(--steam-light-blue);
        font-weight: 700;
        font-size: 18px;
      }
    }

    .countdown-date {
      margin-left: auto;
      color: var(--text-secondary);
      font-size: 13px;
    }
  }

  .crowdfunding-row {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .cf-stats {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;

      .cf-stat {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .cf-label {
          font-size: 12px;
          color: var(--text-secondary);
        }

        .cf-value {
          font-size: 16px;
          font-weight: 600;
          color: var(--text-white);

          &.main { color: #e6a23c; }
          &.reached { color: var(--steam-green); }
        }
      }
    }

    .cf-remain {
      font-size: 13px;
      color: var(--text-secondary);
    }

    .cf-success {
      display: flex;
      align-items: center;
      gap: 6px;
      color: var(--steam-green);
      font-size: 14px;
      font-weight: 600;
    }
  }

  .action-row {
    margin-top: auto;
    display: flex;
    gap: 10px;
  }
}

@media (max-width: 768px) {
  .summary-card {
    flex-wrap: wrap;
    gap: 16px;
    padding: 20px;

    .divider {
      display: none;
    }
  }

  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .preorder-card {
    flex-direction: column;

    .cover-wrap {
      width: 100%;

      .cover {
        min-height: 140px;
      }
    }

    .title-row {
      flex-direction: column;
      align-items: flex-start;
    }

    .crowdfunding-row .cf-stats {
      grid-template-columns: repeat(2, 1fr);
    }

    .countdown-row {
      flex-direction: column;
      align-items: flex-start;

      .countdown-date {
        margin-left: 0;
      }
    }
  }
}
</style>
