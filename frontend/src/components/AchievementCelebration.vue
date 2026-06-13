<template>
  <transition name="celebrate">
    <div v-if="achievementStore.visible && achievementStore.current" class="global-celebration" @click.self="achievementStore.dismiss">
      <div class="celebration-content" :class="rarityClass">
        <div class="confetti-container">
          <div v-for="i in 40" :key="i" class="confetti" :style="confettiStyle(i)" />
        </div>
        <div class="celebration-badge" :class="rarityClass">
          <div class="badge-glow-strong"></div>
          <span class="badge-emoji-large">{{ achievementStore.current.icon || '🏆' }}</span>
        </div>
        <h2 class="celebration-title">🎉 成就解锁！</h2>
        <h3 class="celebration-name">{{ achievementStore.current.name }}</h3>
        <p class="celebration-desc">{{ achievementStore.current.description }}</p>
        <div class="celebration-reward">
          <el-tag type="success" size="large">
            <el-icon><Star /></el-icon>
            +{{ achievementStore.current.points }} 成就点数
          </el-tag>
        </div>
        <div class="celebration-queue-hint" v-if="achievementStore.queue.length > 0">
          还有 {{ achievementStore.queue.length }} 个成就待解锁
        </div>
        <el-button type="primary" size="large" @click="achievementStore.dismiss">
          太棒了！
        </el-button>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAchievementStore } from '@/store/achievement'
import { Star } from '@element-plus/icons-vue'

const achievementStore = useAchievementStore()

const rarityClass = computed(() => {
  const r = achievementStore.current?.rarity || 1
  return `rarity-${r}`
})

function confettiStyle(index: number): Record<string, string> {
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
</script>

<style lang="scss" scoped>
.global-celebration {
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
  animation: badge-pop 0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55);

  &.rarity-1 { background: linear-gradient(145deg, #455a64, #37474f); }
  &.rarity-2 { background: linear-gradient(145deg, #0288d1, #01579b); }
  &.rarity-3 { background: linear-gradient(145deg, #8e24aa, #4a148c); }
  &.rarity-4 { background: linear-gradient(145deg, #ff8f00, #e65100); }
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
  color: var(--text-white, #fff);
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
  color: var(--text-secondary, #aaa);
  margin: 0 0 24px 0;
  line-height: 1.6;
  animation: slide-up 0.5s ease-out 0.4s both;
}

.celebration-reward {
  margin-bottom: 20px;
  animation: slide-up 0.5s ease-out 0.5s both;
}

.celebration-queue-hint {
  font-size: 12px;
  color: var(--steam-light-blue, #4fc3f7);
  margin-bottom: 20px;
  animation: slide-up 0.5s ease-out 0.55s both;
}

@keyframes slide-up {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

.celebrate-enter-active { transition: opacity 0.3s; }
.celebrate-leave-active { transition: opacity 0.3s; }
.celebrate-enter-from,
.celebrate-leave-to { opacity: 0; }
</style>
