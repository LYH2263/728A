<template>
  <div class="activity-timeline">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="4" animated />
    </div>

    <div v-else-if="activities.length === 0" class="empty-state">
      <el-empty :description="emptyText" />
    </div>

    <div v-else class="timeline-list">
      <div v-for="activity in activities" :key="activity.id" class="timeline-item">
        <div class="timeline-dot" :class="activity.type.toLowerCase()"></div>

        <div class="activity-content">
          <div class="activity-header">
            <el-avatar :size="36" :src="getAvatarUrl(activity.user?.avatar)">
              {{ activity.user?.nickname?.charAt(0) || activity.user?.username?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="activity-info">
              <div class="activity-user">
                <span class="username">{{ activity.user?.nickname || activity.user?.username }}</span>
                <span class="activity-type">
                  <span v-if="activity.type === 'PURCHASE'" class="type-label purchase">购买了游戏</span>
                  <span v-else-if="activity.type === 'ACHIEVEMENT'" class="type-label achievement">解锁了成就</span>
                  <span v-else-if="activity.type === 'REVIEW'" class="type-label review">发表了评论</span>
                </span>
              </div>
              <div class="activity-time">{{ formatTime(activity.createdAt) }}</div>
            </div>
          </div>

          <div class="activity-body">
            <template v-if="activity.type === 'PURCHASE'">
              <div class="game-card" @click="goToGame(activity.gameId)">
                <img v-if="activity.gameCover" :src="activity.gameCover" :alt="activity.gameTitle" class="game-cover" />
                <div class="game-info">
                  <div class="game-title">{{ activity.gameTitle }}</div>
                </div>
              </div>
            </template>

            <template v-else-if="activity.type === 'ACHIEVEMENT'">
              <div class="achievement-card">
                <div class="achievement-icon">🏆</div>
                <div class="achievement-info">
                  <div class="achievement-name">{{ activity.achievementName }}</div>
                </div>
              </div>
            </template>

            <template v-else-if="activity.type === 'REVIEW'">
              <div class="review-card">
                <div class="review-header">
                  <span class="review-game">{{ activity.gameTitle }}</span>
                  <div class="review-rating">
                    <el-rate :model-value="activity.reviewRating" disabled :size="14" />
                  </div>
                </div>
                <div class="review-content">{{ activity.reviewContent }}</div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <div v-if="hasMore" class="load-more">
        <el-button @click="loadMore" :loading="loadingMore">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { activityApi } from '@/api'
import type { Activity, PageResult } from '@/types'

interface Props {
  mode?: 'friends' | 'mine'
  emptyText?: string
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'friends',
  emptyText: '暂无动态'
})

const router = useRouter()

const activities = ref<Activity[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const page = ref(1)
const total = ref(0)

const hasMore = computed(() => activities.value.length < total.value)

function getAvatarUrl(avatar: string | undefined): string {
  if (!avatar || avatar === '/avatars/default.png') {
    return '/avatars/default.svg'
  }
  return avatar
}

function formatTime(time: string | undefined): string {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

function goToGame(gameId: number | undefined) {
  if (gameId) {
    router.push(`/game/${gameId}`)
  }
}

async function loadActivities() {
  loading.value = true
  try {
    const res = props.mode === 'friends'
      ? await activityApi.getFriendActivities(1, 10)
      : await activityApi.getMyActivities(1, 10)
    const data: PageResult<Activity> = res.data.data
    activities.value = data.list
    total.value = data.total
    page.value = 1
  } catch (e) {
    console.error('加载动态失败', e)
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  try {
    const nextPage = page.value + 1
    const res = props.mode === 'friends'
      ? await activityApi.getFriendActivities(nextPage, 10)
      : await activityApi.getMyActivities(nextPage, 10)
    const data: PageResult<Activity> = res.data.data
    activities.value = [...activities.value, ...data.list]
    page.value = nextPage
  } catch (e) {
    console.error('加载更多动态失败', e)
  } finally {
    loadingMore.value = false
  }
}

function refresh() {
  loadActivities()
}

watch(() => props.mode, () => {
  loadActivities()
})

onMounted(() => {
  loadActivities()
})

defineExpose({ refresh })
</script>

<style lang="scss" scoped>
.activity-timeline {
  .loading-state {
    padding: 20px;
  }

  .empty-state {
    padding: 40px 0;
  }
}

.timeline-list {
  position: relative;

  &::before {
    content: '';
    position: absolute;
    left: 18px;
    top: 0;
    bottom: 0;
    width: 2px;
    background: var(--border-color);
  }
}

.timeline-item {
  position: relative;
  padding-left: 50px;
  padding-bottom: 24px;
}

.timeline-dot {
  position: absolute;
  left: 10px;
  top: 18px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 3px solid var(--steam-card);
  z-index: 1;

  &.purchase {
    background: var(--steam-blue);
  }

  &.achievement {
    background: #e6a23c;
  }

  &.review {
    background: #67c23a;
  }
}

.activity-content {
  background: var(--steam-card);
  border-radius: var(--radius-md);
  padding: 16px;
  transition: all 0.3s;

  &:hover {
    box-shadow: var(--shadow-md);
  }
}

.activity-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.activity-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.activity-user {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.username {
  font-weight: 500;
  color: var(--text-white);
  font-size: 14px;
}

.type-label {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: var(--radius-sm);

  &.purchase {
    background: rgba(102, 192, 244, 0.15);
    color: var(--steam-blue);
  }

  &.achievement {
    background: rgba(230, 162, 60, 0.15);
    color: #e6a23c;
  }

  &.review {
    background: rgba(103, 194, 58, 0.15);
    color: #67c23a;
  }
}

.activity-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.activity-body {
  .game-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background: var(--bg-hover);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      background: rgba(102, 192, 244, 0.1);
    }
  }

  .game-cover {
    width: 60px;
    height: 80px;
    object-fit: cover;
    border-radius: var(--radius-sm);
  }

  .game-info {
    flex: 1;
  }

  .game-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-white);
  }

  .achievement-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background: var(--bg-hover);
    border-radius: var(--radius-sm);
  }

  .achievement-icon {
    font-size: 32px;
  }

  .achievement-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-white);
  }

  .review-card {
    padding: 12px;
    background: var(--bg-hover);
    border-radius: var(--radius-sm);
  }

  .review-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  .review-game {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-white);
  }

  .review-rating {
    :deep(.el-rate__item) {
      font-size: 14px;
    }
  }

  .review-content {
    font-size: 13px;
    color: var(--text-primary);
    line-height: 1.6;
  }
}

.load-more {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}
</style>
