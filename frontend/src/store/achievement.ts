import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UnlockedAchievementVO } from '@/types'

export const useAchievementStore = defineStore('achievement', () => {
  const queue = ref<UnlockedAchievementVO[]>([])
  const current = ref<UnlockedAchievementVO | null>(null)
  const visible = ref(false)

  function pushAchievements(achievements: UnlockedAchievementVO[]) {
    if (!achievements || achievements.length === 0) return
    queue.value.push(...achievements)
    if (!current.value) {
      showNext()
    }
  }

  function showNext() {
    if (queue.value.length === 0) {
      current.value = null
      visible.value = false
      return
    }
    current.value = queue.value.shift()!
    visible.value = true
  }

  function dismiss() {
    visible.value = false
    setTimeout(() => {
      showNext()
    }, 400)
  }

  return {
    queue,
    current,
    visible,
    pushAchievements,
    dismiss
  }
})
