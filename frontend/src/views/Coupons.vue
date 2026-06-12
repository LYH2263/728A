<template>
  <div class="coupons-page">
    <h1 class="page-title">
      <el-icon><Ticket /></el-icon>
      我的优惠券
    </h1>

    <div class="coupon-tabs">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="未使用" name="UNUSED" />
        <el-tab-pane label="已使用" name="USED" />
        <el-tab-pane label="已过期" name="EXPIRED" />
      </el-tabs>
    </div>

    <div v-if="loading" class="loading">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else>
      <div v-if="coupons.length" class="coupon-list">
        <div
          v-for="uc in coupons"
          :key="uc.id"
          class="coupon-card"
          :class="{ used: uc.status === 'USED', expired: uc.status === 'EXPIRED' }"
        >
          <div class="coupon-left">
            <div class="coupon-value">
              <template v-if="uc.coupon?.type === 'FULL_REDUCTION'">
                <span class="currency">¥</span>
                <span class="amount">{{ uc.coupon.value }}</span>
              </template>
              <template v-else-if="uc.coupon?.type === 'DISCOUNT'">
                <span class="amount">{{ (uc.coupon.value / 10).toFixed(1) }}</span>
                <span class="unit">折</span>
              </template>
              <template v-else-if="uc.coupon?.type === 'CATEGORY'">
                <template v-if="uc.coupon.value > 50">
                  <span class="amount">{{ (uc.coupon.value / 10).toFixed(1) }}</span>
                  <span class="unit">折</span>
                </template>
                <template v-else>
                  <span class="currency">¥</span>
                  <span class="amount">{{ uc.coupon.value }}</span>
                </template>
              </template>
            </div>
            <div class="coupon-condition">
              <template v-if="uc.coupon?.minAmount && uc.coupon.minAmount > 0">
                满{{ uc.coupon.minAmount }}可用
              </template>
              <template v-else>
                无门槛
              </template>
            </div>
          </div>

          <div class="coupon-right">
            <div class="coupon-name">{{ uc.coupon?.name }}</div>
            <div class="coupon-type-tag">
              {{ getCouponTypeText(uc.coupon?.type) }}
            </div>
            <div class="coupon-desc" v-if="uc.coupon?.description">
              {{ uc.coupon.description }}
            </div>
            <div class="coupon-time">
              有效期：{{ formatDate(uc.coupon!.validStart) }} ~ {{ formatDate(uc.coupon!.validEnd) }}
            </div>
            <div class="coupon-status">
              <template v-if="uc.status === 'UNUSED'">
                <el-tag type="success" size="small">可使用</el-tag>
              </template>
              <template v-else-if="uc.status === 'USED'">
                <el-tag type="info" size="small">已使用</el-tag>
              </template>
              <template v-else>
                <el-tag type="info" size="small">已过期</el-tag>
              </template>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else :description="getEmptyText()" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { couponApi } from '@/api'
import type { UserCoupon } from '@/types'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const activeTab = ref('UNUSED')
const loading = ref(true)
const coupons = ref<UserCoupon[]>([])

onMounted(() => {
  loadCoupons()
})

function handleTabChange() {
  loading.value = true
  loadCoupons()
}

async function loadCoupons() {
  loading.value = true
  try {
    const status = activeTab.value === 'EXPIRED' ? '' : activeTab.value
    const res = await couponApi.getMyCoupons(status)
    const list = res.data.data || []

    if (activeTab.value === 'EXPIRED') {
      coupons.value = list.filter((uc: UserCoupon) => {
        return uc.status === 'EXPIRED' || (uc.coupon && new Date(uc.coupon.validEnd) < new Date())
      })
    } else if (activeTab.value === 'UNUSED') {
      coupons.value = list.filter((uc: UserCoupon) => {
        return uc.status === 'UNUSED' && uc.coupon && new Date(uc.coupon.validEnd) >= new Date()
      })
    } else {
      coupons.value = list.filter((uc: UserCoupon) => uc.status === 'USED')
    }
  } catch (error) {
    coupons.value = []
  } finally {
    loading.value = false
  }
}

function getCouponTypeText(type?: string): string {
  const types: Record<string, string> = {
    FULL_REDUCTION: '满减券',
    DISCOUNT: '折扣券',
    CATEGORY: '分类券'
  }
  return types[type || ''] || '优惠券'
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function getEmptyText(): string {
  const texts: Record<string, string> = {
    UNUSED: '暂无可用优惠券',
    USED: '暂无已使用优惠券',
    EXPIRED: '暂无已过期优惠券'
  }
  return texts[activeTab.value] || '暂无优惠券'
}
</script>

<style lang="scss" scoped>
.coupons-page {
  max-width: 900px;
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

.coupon-tabs {
  margin-bottom: 24px;

  :deep(.el-tabs__item) {
    color: var(--text-secondary);
  }

  :deep(.el-tabs__item.is-active) {
    color: var(--text-white);
  }

  :deep(.el-tabs__active-bar) {
    background-color: var(--steam-green);
  }
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.coupon-card {
  display: flex;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--border-color);
  position: relative;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  }

  &.used, &.expired {
    opacity: 0.6;

    &:hover {
      transform: none;
      box-shadow: none;
    }
  }
}

.coupon-left {
  width: 140px;
  background: linear-gradient(135deg, var(--steam-green) 0%, #4a7c00 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    right: -8px;
    top: 0;
    bottom: 0;
    width: 16px;
    background: radial-gradient(circle, transparent 4px, var(--bg-card) 4px);
    background-size: 16px 16px;
    background-position: center;
  }

  .used &, .expired & {
    background: linear-gradient(135deg, #666 0%, #444 100%);
  }
}

.coupon-value {
  color: white;
  display: flex;
  align-items: baseline;

  .currency {
    font-size: 16px;
    font-weight: 500;
  }

  .amount {
    font-size: 36px;
    font-weight: 700;
    line-height: 1;
  }

  .unit {
    font-size: 16px;
    font-weight: 500;
  }
}

.coupon-condition {
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  margin-top: 6px;
}

.coupon-right {
  flex: 1;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.coupon-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-white);
}

.coupon-type-tag {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(164, 208, 7, 0.15);
  color: var(--steam-green);
  border-radius: 4px;
  font-size: 12px;
  width: fit-content;
}

.coupon-desc {
  color: var(--text-secondary);
  font-size: 13px;
}

.coupon-time {
  color: var(--text-secondary);
  font-size: 12px;
}

.coupon-status {
  margin-top: auto;
}

@media (max-width: 600px) {
  .coupon-card {
    flex-direction: column;
  }

  .coupon-left {
    width: 100%;
    padding: 16px;

    &::after {
      display: none;
    }
  }

  .coupon-right {
    padding: 16px;
  }
}
</style>
