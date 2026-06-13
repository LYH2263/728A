<template>
  <div class="achievements-page">
    <h1 class="page-title">
      <el-icon><Trophy /></el-icon>
      成就中心
    </h1>

    <div class="stats-overview">
      <div class="overview-card main-card">
        <div class="overview-icon">
          <el-icon :size="48" color="#ffd700"><Trophy /></el-icon>
        </div>
        <div class="overview-content">
          <span class="overview-value">{{ stats.unlockedCount || 0 }} / {{ stats.totalCount || 0 }}</span>
          <span class="overview-label">已解锁成就</span>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon points">
          <el-icon :size="40" color="#a4d007"><Star /></el-icon>
        </div>
        <div class="overview-content">
          <span class="overview-value">{{ stats.totalPoints || 0 }}</span>
          <span class="overview-label">成就点数</span>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon rate">
          <el-icon :size="40" color="#4fc3f7"><Medal /></el-icon>
        </div>
        <div class="overview-content">
          <span class="overview-value">{{ (stats.completionRate || 0).toFixed(1) }}%</span>
          <span class="overview-label">完成度</span>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-tabs v-model="activeFilter" class="filter-tabs">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="已解锁" name="unlocked" />
        <el-tab-pane label="未解锁" name="locked" />
      </el-tabs>
    </div>

    <div class="achievements-grid">
      <div
        v-for="item in filteredAchievements"
        :key="item.achievementId"
        class="achievement-card"
        :class="{ unlocked: item.isUnlocked === 1 }"
        @mouseenter="hoveredAchievement = item"
        @mouseleave="hoveredAchievement = null"
      >
        <div class="card-inner">
          <div
            class="badge-wrapper"
            :class="getRarityClass(item.achievement.rarity)"
          >
            <div class="badge-glow" v-if="item.isUnlocked === 1"></div>
            <div class="badge-icon">
              <span v-if="item.achievement.icon" class="icon-emoji">
                {{ item.achievement.icon }}
              </span>
              <el-icon v-else :size="48"><Trophy /></el-icon>
            </div>
            <el-badge
              v-if="item.achievement.rarity >= 3"
              :value="getRarityLabel(item.achievement.rarity)"
              :type="item.achievement.rarity === 4 ? 'danger' : 'warning'"
              class="rarity-badge"
            />
          </div>

          <div class="achievement-info">
            <h3 class="achievement-name" :class="{ locked: item.isUnlocked !== 1 }">
              {{ item.achievement.name }}
            </h3>
            <p class="achievement-desc">{{ item.achievement.description }}</p>

            <div class="progress-section" v-if="item.achievement.isProgress === 1 || item.isUnlocked !== 1">
              <div class="progress-header">
                <span class="progress-text">
                  {{ item.isUnlocked === 1 ? item.achievement.targetValue : item.progress }}
                  / {{ item.achievement.targetValue }}
                </span>
                <span class="points-label">+{{ item.achievement.points }}点</span>
              </div>
              <el-progress
                :percentage="getProgressPercent(item)"
                :stroke-width="6"
                :show-text="false"
                :color="item.isUnlocked === 1 ? '#a4d007' : getRarityColor(item.achievement.rarity)"
              />
            </div>
            <div class="unlocked-info" v-else>
              <span class="points-label">+{{ item.achievement.points }}点</span>
            </div>

            <div class="unlock-time" v-if="item.isUnlocked === 1 && item.unlockedAt">
              <el-icon><Clock /></el-icon>
              {{ formatDate(item.unlockedAt) }} 解锁
            </div>
          </div>
        </div>

        <transition name="fade">
          <div v-if="hoveredAchievement?.achievementId === item.achievementId" class="hover-tooltip">
            <div class="tooltip-header">
              <span class="tooltip-icon">{{ item.achievement.icon || '🏆' }}</span>
              <div>
                <h4>{{ item.achievement.name }}</h4>
                <el-tag
                  size="small"
                  :type="getRarityTagType(item.achievement.rarity)"
                >
                  {{ getRarityLabel(item.achievement.rarity) }}
                </el-tag>
              </div>
            </div>
            <p class="tooltip-desc">{{ item.achievement.description }}</p>
            <div class="tooltip-progress">
              进度: {{ item.isUnlocked === 1 ? '✅ 已完成' : `${item.progress} / ${item.achievement.targetValue}` }}
            </div>
            <div class="tooltip-footer">
              <span>分类: {{ getCategoryLabel(item.achievement.category) }}</span>
              <span>奖励: {{ item.achievement.points }}点</span>
            </div>
            <div v-if="item.isUnlocked === 1 && item.unlockedAt" class="tooltip-unlock-time">
              🎉 {{ formatDate(item.unlockedAt) }} 解锁
            </div>
          </div>
        </transition>
      </div>
    </div>

    <el-empty v-if="filteredAchievements.length === 0" description="暂无成就数据" />

    <transition name="celebrate">
      <div v-if="celebration.visible" class="celebration-modal" @click.self="closeCelebration">
        <div class="celebration-content" :class="getRarityClass(celebration.rarity)">
          <div class="confetti-container">
            <div v-for="i in 50" :key="i" class="confetti" :style="getConfettiStyle(i)" />
          </div>
          <div class="celebration-badge">
            <div class="badge-glow-strong"></div>
            <span class="badge-emoji-large">{{ celebration.icon || '🏆' }}</span>
          </div>
          <h2 class="celebration-title">🎉 成就解锁！</h2>
          <h3 class="celebration-name">{{ celebration.name }}</h3>
          <p class="celebration-desc">{{ celebration.description }}</p>
          <div class="celebration-reward">
            <el-tag type="success" size="large">
              <el-icon><Star /></el-icon>
              +{{ celebration.points }} 成就点数
            </el-tag>
          </div>
          <el-button type="primary" size="large" @click="closeCelebration">
            太棒了！
          </el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { achievementApi } from '@/api'
import type { UserAchievement, AchievementStats } from '@/types'
import { ElMessage } from 'element-plus'
import { Trophy, Star, Medal, Clock } from '@element-plus/icons-vue'

const achievements = ref<UserAchievement[]>([])
const stats = ref<AchievementStats>({
  totalCount: 0,
  unlockedCount: 0,
  totalPoints: 0,
  completionRate: 0
})
const activeFilter = ref('all')
const hoveredAchievement = ref<UserAchievement | null>(null)

const celebration = ref({
  visible: false,
  name: '',
  description: '',
  icon: '',
  points: 0,
  rarity: 1
})

const filteredAchievements = computed(() => {
  if (activeFilter.value === 'unlocked') {
    return achievements.value.filter(a => a.isUnlocked === 1)
  } else if (activeFilter.value === 'locked') {
    return achievements.value.filter(a => a.isUnlocked !== 1)
  }
  return achievements.value
})

function getProgressPercent(item: UserAchievement): number {
  if (item.achievement.targetValue <= 0) return 100
  const percent = (item.progress / item.achievement.targetValue) * 100
  return Math.min(Math.round(percent), 100)
}

function getRarityClass(rarity: number): string {
  return `rarity-${rarity}`
}

function getRarityColor(rarity: number): string {
  const colors: Record<number, string> = {
    1: '#b0bec5',
    2: '#4fc3f7',
    3: '#ba68c8',
    4: '#ffb74d'
  }
  return colors[rarity] || '#b0bec5'
}

function getRarityTagType(rarity: number): '' | 'success' | 'warning' | 'info' | 'danger' {
  const types: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    1: 'info',
    2: '',
    3: 'warning',
    4: 'danger'
  }
  return types[rarity] || 'info'
}

function getRarityLabel(rarity: number): string {
  const labels: Record<number, string> = {
    1: '普通',
    2: '稀有',
    3: '史诗',
    4: '传说'
  }
  return labels[rarity] || '普通'
}

function getCategoryLabel(category: string): string {
  const labels: Record<string, string> = {
    GENERAL: '综合',
    PURCHASE: '购买',
    PLAYTIME: '游玩',
    REVIEW: '评论',
    COLLECTION: '收藏'
  }
  return labels[category] || category
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

function getConfettiStyle(index: number): Record<string, string> {
  const colors = ['#ff6b6b', '#ffd93d', '#6bcb77', '#4d96ff', '#ff6b9d', '#c56bff']
  const left = Math.random() * 100
  const animationDelay = `${Math.random() * 3}s`
  const animationDuration = `${2 + Math.random() * 2}s`
  const color = colors[index % colors.length]
  const size = `${5 + Math.random() * 10}px`
  return {
    left: `${left}%`,
    background: color,
    width: size,
    height: size,
    animationDelay,
    animationDuration
  }
}

function showCelebration(achievement: UserAchievement) {
  celebration.value = {
    visible: true,
    name: achievement.achievement.name,
    description: achievement.achievement.description,
    icon: achievement.achievement.icon || '🏆',
    points: achievement.achievement.points,
    rarity: achievement.achievement.rarity
  }
}

function closeCelebration() {
  celebration.value.visible = false
}

async function fetchAchievements() {
  try {
    const [achRes, statsRes] = await Promise.all([
      achievementApi.getMyAchievements(),
      achievementApi.getAchievementStats()
    ])
    achievements.value = achRes.data.data || []
    stats.value = statsRes.data.data || stats.value
    checkNewUnlocks()
  } catch (error) {
    ElMessage.error('获取成就数据失败')
  }
}

function checkNewUnlocks() {
  const lastChecked = localStorage.getItem('achievement_last_check')
  const now = Date.now()

  if (!lastChecked) {
    localStorage.setItem('achievement_last_check', String(now))
    return
  }

  const newlyUnlocked = achievements.value.filter(
    a => a.isUnlocked === 1 && a.unlockedAt
  )

  if (newlyUnlocked.length > 0) {
    const lastUnlocked = newlyUnlocked[0]
    if (lastUnlocked.unlockedAt) {
      const unlockTime = new Date(lastUnlocked.unlockedAt).getTime()
      if (unlockTime > parseInt(lastChecked) - 5000) {
        setTimeout(() => showCelebration(lastUnlocked), 800)
      }
    }
  }
  localStorage.setItem('achievement_last_check', String(now))
}

onMounted(() => {
  fetchAchievements()
  localStorage.setItem('achievement_last_check', String(Date.now()))
})
</script>

<style lang="scss" scoped>
.achievements-page {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  color: var(--text-white);
  margin-bottom: 24px;
}

.stats-overview {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  border: 1px solid var(--border-color);

  &.main-card {
    background: linear-gradient(135deg, rgba(164, 208, 7, 0.1), rgba(255, 215, 0, 0.05));
    border-color: rgba(164, 208, 7, 0.3);
  }

  .overview-icon {
    width: 72px;
    height: 72px;
    border-radius: 16px;
    background: rgba(255, 215, 0, 0.15);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.points {
      background: rgba(164, 208, 7, 0.15);
    }
    &.rate {
      background: rgba(79, 195, 247, 0.15);
    }
  }

  .overview-content {
    display: flex;
    flex-direction: column;
  }

  .overview-value {
    font-size: 32px;
    font-weight: 700;
    color: var(--text-white);
    line-height: 1.2;
  }

  .overview-label {
    font-size: 14px;
    color: var(--text-secondary);
    margin-top: 4px;
  }
}

.filter-bar {
  margin-bottom: 20px;
}

.filter-tabs {
  :deep(.el-tabs__item) {
    color: var(--text-secondary);
  }
  :deep(.el-tabs__item.is-active) {
    color: var(--steam-light-blue);
  }
  :deep(.el-tabs__active-bar) {
    background-color: var(--steam-light-blue);
  }
}

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.achievement-card {
  position: relative;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  border: 2px solid var(--border-color);
  overflow: visible;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-4px);
    border-color: var(--steam-light-blue);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.3);
  }

  &.unlocked {
    border-color: rgba(164, 208, 7, 0.4);
    background: linear-gradient(180deg, rgba(164, 208, 7, 0.05), transparent);

    &:hover {
      border-color: var(--steam-green);
      box-shadow: 0 12px 32px rgba(164, 208, 7, 0.15);
    }
  }
}

.card-inner {
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.badge-wrapper {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  transition: all 0.3s;

  &.rarity-1 {
    background: linear-gradient(145deg, #455a64, #37474f);
    box-shadow: 0 4px 20px rgba(176, 190, 197, 0.2);
  }
  &.rarity-2 {
    background: linear-gradient(145deg, #0288d1, #01579b);
    box-shadow: 0 4px 20px rgba(79, 195, 247, 0.3);
  }
  &.rarity-3 {
    background: linear-gradient(145deg, #8e24aa, #4a148c);
    box-shadow: 0 4px 20px rgba(186, 104, 200, 0.35);
  }
  &.rarity-4 {
    background: linear-gradient(145deg, #ff8f00, #e65100);
    box-shadow: 0 4px 24px rgba(255, 183, 77, 0.4);
  }

  .achievement-card:not(.unlocked) & {
    filter: grayscale(1) brightness(0.5);
  }
}

.badge-glow {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: inherit;
  filter: blur(16px);
  opacity: 0.5;
  animation: glow-pulse 2s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.05); }
}

.badge-icon {
  position: relative;
  z-index: 1;
  color: #fff;

  .icon-emoji {
    font-size: 48px;
    line-height: 1;
  }
}

.rarity-badge {
  position: absolute;
  top: -8px;
  right: -8px;
}

.achievement-info {
  width: 100%;
}

.achievement-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-white);
  margin: 0 0 6px 0;
  line-height: 1.3;

  &.locked {
    color: var(--text-secondary);
  }
}

.achievement-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 12px 0;
  line-height: 1.5;
  min-height: 39px;
}

.progress-section {
  margin-top: auto;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.points-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--steam-green);
}

.unlocked-info {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.unlock-time {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
  color: var(--steam-green);
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed rgba(164, 208, 7, 0.3);
}

.hover-tooltip {
  position: absolute;
  bottom: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%);
  width: 280px;
  background: #1a2332;
  border: 1px solid var(--steam-light-blue);
  border-radius: var(--radius-md);
  padding: 16px;
  z-index: 100;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);

  &::after {
    content: '';
    position: absolute;
    top: 100%;
    left: 50%;
    transform: translateX(-50%);
    border: 8px solid transparent;
    border-top-color: var(--steam-light-blue);
  }
}

.tooltip-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;

  h4 {
    margin: 0 0 6px 0;
    color: var(--text-white);
    font-size: 15px;
  }

  .tooltip-icon {
    font-size: 32px;
  }
}

.tooltip-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.tooltip-progress {
  font-size: 13px;
  color: var(--steam-light-blue);
  font-weight: 600;
  margin-bottom: 10px;
}

.tooltip-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
  padding-top: 10px;
  border-top: 1px solid var(--border-color);
}

.tooltip-unlock-time {
  margin-top: 10px;
  font-size: 12px;
  color: var(--steam-green);
  text-align: center;
  font-weight: 500;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(8px);
}

.celebration-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.celebration-content {
  position: relative;
  background: linear-gradient(180deg, #1a2332, #0d1520);
  border-radius: 24px;
  padding: 48px 40px;
  text-align: center;
  max-width: 420px;
  width: 90%;
  border: 2px solid;
  overflow: hidden;

  &.rarity-1 { border-color: #78909c; }
  &.rarity-2 { border-color: #29b6f6; box-shadow: 0 0 60px rgba(41, 182, 246, 0.3); }
  &.rarity-3 { border-color: #ab47bc; box-shadow: 0 0 60px rgba(171, 71, 188, 0.4); }
  &.rarity-4 { border-color: #ffa726; box-shadow: 0 0 80px rgba(255, 167, 38, 0.5); }
}

.confetti-container {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.confetti {
  position: absolute;
  top: -20px;
  border-radius: 2px;
  animation: confetti-fall linear infinite;
}

@keyframes confetti-fall {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 1;
  }
  100% {
    transform: translateY(500px) rotate(720deg);
    opacity: 0;
  }
}

.celebration-badge {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #ff8f00, #e65100);
  animation: badge-pop 0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

@keyframes badge-pop {
  0% { transform: scale(0) rotate(-180deg); }
  100% { transform: scale(1) rotate(0deg); }
}

.badge-glow-strong {
  position: absolute;
  inset: -10px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 215, 0, 0.4), transparent 70%);
  animation: glow-strong 1.5s ease-in-out infinite;
}

@keyframes glow-strong {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.2); opacity: 1; }
}

.badge-emoji-large {
  font-size: 60px;
  position: relative;
  z-index: 1;
  animation: emoji-bounce 0.8s ease-out 0.3s both;
}

@keyframes emoji-bounce {
  0% { transform: scale(0); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}

.celebration-title {
  font-size: 28px;
  color: var(--text-white);
  margin: 0 0 8px 0;
  animation: slide-up 0.5s ease-out 0.2s both;
}

.celebration-name {
  font-size: 22px;
  color: #ffd700;
  margin: 0 0 12px 0;
  font-weight: 700;
  animation: slide-up 0.5s ease-out 0.3s both;
}

.celebration-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 24px 0;
  line-height: 1.6;
  animation: slide-up 0.5s ease-out 0.4s both;
}

.celebration-reward {
  margin-bottom: 28px;
  animation: slide-up 0.5s ease-out 0.5s both;
}

@keyframes slide-up {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

.celebrate-enter-active {
  transition: opacity 0.3s;
}
.celebrate-leave-active {
  transition: opacity 0.3s;
}
.celebrate-enter-from,
.celebrate-leave-to {
  opacity: 0;

  .celebration-content {
    transform: scale(0.8);
  }
}

@media (max-width: 768px) {
  .stats-overview {
    grid-template-columns: 1fr;
  }
  .achievements-grid {
    grid-template-columns: 1fr;
  }
  .hover-tooltip {
    display: none;
  }
}
</style>
