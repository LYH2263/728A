<template>
  <div class="gifts-page">
    <h1 class="page-title">
      <el-icon><Present /></el-icon>
      礼物中心
    </h1>

    <div v-if="loading" class="empty-state">
      <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <template v-else>
      <div class="summary-card">
        <div class="summary-item">
          <span class="summary-value pending">{{ pendingReceivedCount }}</span>
          <span class="summary-label">待领取</span>
        </div>
        <div class="divider"></div>
        <div class="summary-item">
          <span class="summary-value">{{ receivedGifts.length }}</span>
          <span class="summary-label">收到的礼物</span>
        </div>
        <div class="divider"></div>
        <div class="summary-item">
          <span class="summary-value">{{ sentGifts.length }}</span>
          <span class="summary-label">送出的礼物</span>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="gifts-tabs">
        <el-tab-pane label="收到的礼物" name="received">
          <div class="filter-bar">
            <el-radio-group v-model="receivedFilter" size="default">
              <el-radio-button value="ALL">全部</el-radio-button>
              <el-radio-button value="PENDING">待领取</el-radio-button>
              <el-radio-button value="CLAIMED">已领取</el-radio-button>
              <el-radio-button value="REJECTED">已拒绝</el-radio-button>
            </el-radio-group>
            <div class="filter-count">共 {{ filteredReceived.length }} 条记录</div>
          </div>

          <div v-if="filteredReceived.length === 0" class="empty-list">
            <el-empty :description="receivedFilter === 'ALL' ? '还没有收到任何礼物' : '暂无该状态的礼物'">
              <router-link to="/store">
                <el-button type="primary">去选购</el-button>
              </router-link>
            </el-empty>
          </div>

          <div v-else class="gift-list">
            <div
              v-for="gift in filteredReceived"
              :key="gift.id"
              class="gift-card received"
              :class="getStatusClass(gift.status)"
            >
              <div class="cover-wrap">
                <img
                  :src="gift.game?.coverImage || gift.gameCover"
                  :alt="gift.gameTitle"
                  class="cover"
                />
                <div class="status-badge">
                  <el-tag v-if="gift.status === 'PENDING'" type="warning" effect="dark" size="large">
                    待领取
                  </el-tag>
                  <el-tag v-else-if="gift.status === 'CLAIMED'" type="success" effect="dark" size="large">
                    已领取
                  </el-tag>
                  <el-tag v-else type="info" effect="dark" size="large">
                    已拒绝
                  </el-tag>
                </div>
              </div>

              <div class="info">
                <div class="title-row">
                  <h3 class="game-title">{{ gift.gameTitle }}</h3>
                </div>

                <div class="sender-row">
                  <el-avatar :src="gift.sender?.avatar" size="small" />
                  <span class="sender-name">
                    来自 <strong>{{ gift.sender?.nickname || gift.sender?.username }}</strong>
                  </span>
                  <span class="price-label">价值 ¥{{ Number(gift.pricePaid).toFixed(2) }}</span>
                </div>

                <div v-if="gift.message" class="message-box">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>"{{ gift.message }}"</span>
                </div>

                <div class="meta-row">
                  <span class="meta-item">
                    <el-icon><Calendar /></el-icon>
                    发送时间：{{ formatDate(gift.createdAt) }}
                  </span>
                  <span v-if="gift.claimedAt" class="meta-item claimed">
                    <el-icon><CircleCheck /></el-icon>
                    领取时间：{{ formatDate(gift.claimedAt) }}
                  </span>
                  <span v-if="gift.rejectedAt" class="meta-item rejected">
                    <el-icon><CloseBold /></el-icon>
                    拒绝时间：{{ formatDate(gift.rejectedAt) }}
                  </span>
                </div>

                <div v-if="gift.status === 'PENDING'" class="action-row">
                  <el-button
                    type="primary"
                    size="large"
                    :loading="claimingGiftId === gift.id"
                    @click="handleClaim(gift)"
                  >
                    <el-icon><CircleCheck /></el-icon>
                    领取礼物
                  </el-button>
                  <el-button
                    size="large"
                    :loading="rejectingGiftId === gift.id"
                    @click="handleReject(gift)"
                  >
                    <el-icon><CloseBold /></el-icon>
                    拒绝并退款
                  </el-button>
                </div>

                <div v-else-if="gift.status === 'CLAIMED'" class="action-row done">
                  <el-button size="small" @click="goToGame(gift.gameId)">
                    <el-icon><Collection /></el-icon>
                    前往游戏库
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="送出的礼物" name="sent">
          <div class="filter-bar">
            <el-radio-group v-model="sentFilter" size="default">
              <el-radio-button value="ALL">全部</el-radio-button>
              <el-radio-button value="PENDING">待领取</el-radio-button>
              <el-radio-button value="CLAIMED">已领取</el-radio-button>
              <el-radio-button value="REJECTED">已拒绝</el-radio-button>
            </el-radio-group>
            <div class="filter-count">共 {{ filteredSent.length }} 条记录</div>
          </div>

          <div v-if="filteredSent.length === 0" class="empty-list">
            <el-empty :description="sentFilter === 'ALL' ? '还没有送出任何礼物' : '暂无该状态的礼物'">
              <router-link to="/store">
                <el-button type="primary">去选购送好友</el-button>
              </router-link>
            </el-empty>
          </div>

          <div v-else class="gift-list">
            <div
              v-for="gift in filteredSent"
              :key="gift.id"
              class="gift-card sent"
              :class="getStatusClass(gift.status)"
            >
              <div class="cover-wrap">
                <img
                  :src="gift.game?.coverImage || gift.gameCover"
                  :alt="gift.gameTitle"
                  class="cover"
                />
                <div class="status-badge">
                  <el-tag v-if="gift.status === 'PENDING'" type="warning" effect="dark" size="large">
                    等待领取
                  </el-tag>
                  <el-tag v-else-if="gift.status === 'CLAIMED'" type="success" effect="dark" size="large">
                    已被领取
                  </el-tag>
                  <el-tag v-else type="info" effect="dark" size="large">
                    已被拒绝
                  </el-tag>
                </div>
              </div>

              <div class="info">
                <div class="title-row">
                  <h3 class="game-title">{{ gift.gameTitle }}</h3>
                </div>

                <div class="recipient-row">
                  <el-avatar :src="gift.recipient?.avatar" size="small" />
                  <span class="recipient-name">
                    送给 <strong>{{ gift.recipient?.nickname || gift.recipient?.username }}</strong>
                  </span>
                  <span class="price-label">花费 ¥{{ Number(gift.pricePaid).toFixed(2) }}</span>
                </div>

                <div v-if="gift.message" class="message-box">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>"{{ gift.message }}"</span>
                </div>

                <div class="meta-row">
                  <span class="meta-item">
                    <el-icon><Calendar /></el-icon>
                    赠送时间：{{ formatDate(gift.createdAt) }}
                  </span>
                  <span v-if="gift.claimedAt" class="meta-item claimed">
                    <el-icon><CircleCheck /></el-icon>
                    领取时间：{{ formatDate(gift.claimedAt) }}
                  </span>
                  <span v-if="gift.rejectedAt" class="meta-item rejected">
                    <el-icon><CloseBold /></el-icon>
                    拒绝时间：{{ formatDate(gift.rejectedAt) }}
                    <el-tag size="small" type="success" style="margin-left: 6px">
                      款项已退回
                    </el-tag>
                  </span>
                </div>

                <div v-if="gift.status === 'PENDING'" class="action-row pending-sent">
                  <el-tag type="warning" effect="light">
                    <el-icon><Timer /></el-icon>
                    等待好友领取中...
                  </el-tag>
                </div>

                <div v-else-if="gift.status === 'CLAIMED'" class="action-row done">
                  <el-tag type="success" effect="light">
                    <el-icon><CircleCheck /></el-icon>
                    好友已成功领取！
                  </el-tag>
                </div>

                <div v-else class="action-row done">
                  <el-tag type="info" effect="light">
                    <el-icon><Wallet /></el-icon>
                    款项 ¥{{ Number(gift.pricePaid).toFixed(2) }} 已退回您的余额
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { giftApi } from '@/api'
import type { Gift } from '@/types'

const router = useRouter()

const loading = ref(true)
const activeTab = ref('received')
const receivedFilter = ref('ALL')
const sentFilter = ref('ALL')
const receivedGifts = ref<Gift[]>([])
const sentGifts = ref<Gift[]>([])
const pendingReceivedCount = ref(0)
const claimingGiftId = ref<number | null>(null)
const rejectingGiftId = ref<number | null>(null)

const filteredReceived = computed(() => {
  if (receivedFilter.value === 'ALL') return receivedGifts.value
  return receivedGifts.value.filter(g => g.status === receivedFilter.value)
})

const filteredSent = computed(() => {
  if (sentFilter.value === 'ALL') return sentGifts.value
  return sentGifts.value.filter(g => g.status === sentFilter.value)
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

function getStatusClass(status: string) {
  return {
    'status-pending': status === 'PENDING',
    'status-claimed': status === 'CLAIMED',
    'status-rejected': status === 'REJECTED'
  }
}

async function fetchGifts() {
  loading.value = true
  try {
    const [receivedRes, sentRes, countRes] = await Promise.all([
      giftApi.getReceivedGifts(),
      giftApi.getSentGifts(),
      giftApi.getPendingCount()
    ])
    receivedGifts.value = receivedRes.data.data || []
    sentGifts.value = sentRes.data.data || []
    pendingReceivedCount.value = countRes.data.data || 0
  } catch (err) {
    ElMessage.error('获取礼物列表失败')
  } finally {
    loading.value = false
  }
}

async function handleClaim(gift: Gift) {
  try {
    await ElMessageBox.confirm(
      `确认领取「${gift.gameTitle}」吗？领取后游戏将添加到您的游戏库。`,
      '领取礼物',
      {
        confirmButtonText: '确认领取',
        cancelButtonText: '取消',
        type: 'success'
      }
    )
    claimingGiftId.value = gift.id
    await giftApi.claimGift(gift.id)
    ElMessage.success('领取成功！游戏已添加到游戏库')
    await fetchGifts()
  } catch (e: any) {
    if (e !== 'cancel') {
      // 非取消操作时显示错误（请求错误已被拦截器处理）
    }
  } finally {
    claimingGiftId.value = null
  }
}

async function handleReject(gift: Gift) {
  try {
    await ElMessageBox.confirm(
      `确认拒绝「${gift.gameTitle}」吗？拒绝后款项将退还给赠送者。`,
      '拒绝礼物',
      {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    rejectingGiftId.value = gift.id
    await giftApi.rejectGift(gift.id)
    ElMessage.success('已拒绝，款项将退还给赠送者')
    await fetchGifts()
  } catch (e: any) {
    if (e !== 'cancel') {
      // 非取消操作时显示错误
    }
  } finally {
    rejectingGiftId.value = null
  }
}

function goToGame(gameId: number) {
  router.push('/library')
}

onMounted(fetchGifts)
</script>

<style lang="scss" scoped>
.gifts-page {
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

  p {
    color: var(--text-secondary);
    margin-top: 12px;
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

      &.pending { color: var(--el-color-warning); }
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

.gifts-tabs {
  :deep(.el-tabs__item) {
    color: var(--text-primary);
    font-size: 16px;
  }

  :deep(.el-tabs__item.is-active) {
    color: var(--steam-light-blue);
  }

  :deep(.el-tabs__active-bar) {
    background-color: var(--steam-light-blue);
  }

  :deep(.el-tabs__nav-wrap::after) {
    background-color: var(--border-color);
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0;

  .filter-count {
    color: var(--text-secondary);
    font-size: 14px;
  }
}

.empty-list {
  padding: 40px 0;
}

.g