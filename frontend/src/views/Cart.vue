<template>
  <div class="cart-page">
    <h1 class="page-title">
      <el-icon><ShoppingCart /></el-icon>
      我的购物车
    </h1>

    <div v-if="loading" class="loading">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else>
      <div v-if="cartStore.items.length" class="cart-content">
        <div class="cart-list">
          <div v-for="item in cartStore.items" :key="item.id" class="cart-item">
            <img
              :src="item.game.coverImage"
              :alt="item.game.title"
              class="game-cover"
              @click="goToGame(item.gameId)"
            />
            <div class="item-info">
              <h3 @click="goToGame(item.gameId)">{{ item.game.title }}</h3>
              <div class="item-price">
                <template v-if="item.game.discountPercent">
                  <span class="discount-tag">-{{ item.game.discountPercent }}%</span>
                  <span class="original-price">¥{{ item.game.originalPrice }}</span>
                  <span class="final-price">¥{{ item.game.discountPrice }}</span>
                </template>
                <template v-else>
                  <span class="final-price">¥{{ item.game.originalPrice }}</span>
                </template>
              </div>
            </div>
            <el-button text type="danger" @click="handleRemove(item.gameId)">
              <el-icon><Delete /></el-icon>
              移除
            </el-button>
          </div>
        </div>

        <div class="checkout-card">
          <h3>订单摘要</h3>

          <div class="gift-section">
            <div class="gift-toggle-row">
              <span class="gift-label">
                <el-icon><Present /></el-icon>
                作为礼物赠送
              </span>
              <el-switch
                v-model="isGift"
                :disabled="!canGift"
                active-color="#67c23a"
              />
            </div>
            <el-tooltip
              v-if="!canGift"
              content="赠送礼物每次只能选择一款游戏"
              placement="top"
            >
              <div class="gift-tip">请先移除多余商品，仅保留一款游戏即可赠送</div>
            </el-tooltip>

            <div v-if="isGift" class="gift-form">
              <el-form-item label="收礼好友" required>
                <el-select
                  v-model="selectedRecipientId"
                  placeholder="请选择收礼好友"
                  style="width: 100%"
                  :disabled="friendsList.length === 0"
                >
                  <el-option
                    v-for="f in friendsList"
                    :key="f.friendId"
                    :value="f.friendId"
                  >
                    <div class="friend-option">
                      <el-avatar :src="f.friendUser?.avatar" size="small" />
                      <span>{{ f.friendUser?.nickname || f.friendUser?.username }}</span>
                    </div>
                  </el-option>
                </el-select>
                <router-link
                  v-if="friendsList.length === 0"
                  to="/friends"
                  class="no-friends-tip"
                >
                  还没有好友？去添加好友 →
                </router-link>
              </el-form-item>
              <el-form-item label="赠言">
                <el-input
                  v-model="giftMessage"
                  type="textarea"
                  :rows="2"
                  maxlength="200"
                  show-word-limit
                  placeholder="写一句赠言送给TA（可选）"
                />
              </el-form-item>
              <el-alert
                type="info"
                :closable="false"
                show-icon
                title="游戏将作为礼物发送给好友"
                description="支付成功后，您的好友可在「礼物中心」领取或拒绝，若被拒绝，款项将自动退回您的余额。"
              />
            </div>
          </div>

          <el-divider />

          <div class="coupon-section">
            <div class="coupon-label">
              <span>优惠券</span>
              <span v-if="applicableCoupons.length" class="coupon-count">
                可用 {{ applicableCoupons.length }} 张
              </span>
            </div>
            <el-select
              v-model="selectedCouponId"
              placeholder="请选择优惠券"
              style="width: 100%"
              class="coupon-select"
              @change="onCouponChange"
            >
              <el-option :value="null" label="不使用优惠券" />
              <el-option
                v-for="uc in applicableCoupons"
                :key="uc.id"
                :value="uc.id"
                :label="getCouponLabel(uc)"
              >
                <div class="coupon-option">
                  <span class="coupon-option-name">{{ uc.coupon?.name || '' }}</span>
                  <span class="coupon-option-desc">
                    {{ uc.coupon ? getCouponValueText(uc.coupon) : '' }}
                  </span>
                </div>
              </el-option>
            </el-select>
            <div v-if="couponDiscount > 0" class="coupon-discount">
              已优惠：-¥{{ couponDiscount.toFixed(2) }}
            </div>
          </div>

          <el-divider />

          <div class="summary-row">
            <span>商品总价</span>
            <span>¥{{ cartStore.originalAmount.toFixed(2) }}</span>
          </div>
          <div v-if="cartStore.discountAmount > 0" class="summary-row discount">
            <span>商品折扣</span>
            <span>-¥{{ cartStore.discountAmount.toFixed(2) }}</span>
          </div>
          <div v-if="couponDiscount > 0" class="summary-row coupon-row">
            <span>优惠券抵扣</span>
            <span>-¥{{ couponDiscount.toFixed(2) }}</span>
          </div>
          <el-divider />
          <div class="summary-row total">
            <span>应付金额</span>
            <span class="pay-amount">¥{{ finalAmount.toFixed(2) }}</span>
          </div>
          <el-button
            type="success"
            size="large"
            style="width: 100%"
            @click="handleCheckout"
            :disabled="!cartStore.items.length"
          >
            结算 ({{ cartStore.count }}件商品)
          </el-button>
          <el-button text style="width: 100%; margin-left:0; margin-top: 8px" @click="handleClear">
            清空购物车
          </el-button>
        </div>
      </div>

      <el-empty v-else description="购物车是空的">
        <router-link to="/store">
          <el-button type="primary">去选购</el-button>
        </router-link>
      </el-empty>
    </template>

    <el-dialog v-model="showCheckout" :title="isGift ? '确认赠送礼物' : '确认订单'" width="500px" class="checkout-dialog-wrapper">
      <div class="checkout-dialog">
        <div v-if="isGift" class="gift-info-dialog">
          <el-alert type="success" :closable="false" show-icon>
            <template #title>🎁 您正在赠送礼物给</template>
            <div class="recipient-info">
              <el-avatar :src="getSelectedFriend()?.friendUser?.avatar" size="large" />
              <div class="recipient-text">
                <div class="recipient-name">
                  {{ getSelectedFriend()?.friendUser?.nickname || getSelectedFriend()?.friendUser?.username }}
                </div>
                <div v-if="giftMessage" class="recipient-message">"{{ giftMessage }}"</div>
              </div>
            </div>
          </el-alert>
        </div>

        <div class="order-items">
          <div v-for="item in cartStore.items" :key="item.id" class="order-item">
            <img :src="item.game.coverImage" :alt="item.game.title" />
            <span>{{ item.game.title }}</span>
            <span class="price">¥{{ (item.game.discountPrice ?? item.game.originalPrice).toFixed(2) }}</span>
          </div>
        </div>

        <div v-if="selectedCoupon" class="coupon-info">
          <el-tag type="success" size="small">优惠券</el-tag>
          <span class="coupon-name">{{ selectedCoupon.coupon?.name }}</span>
          <span class="coupon-save">-¥{{ couponDiscount.toFixed(2) }}</span>
        </div>

        <el-divider />
        <div class="order-total">
          <span>应付金额：</span>
          <span class="amount">¥{{ finalAmount.toFixed(2) }}</span>
        </div>
        <div class="balance-info">
          <span>账户余额：</span>
          <span :class="{ insufficient: userBalance < finalAmount }">
            ¥{{ userBalance.toFixed(2) }}
          </span>
        </div>
        <el-alert
          v-if="userBalance < finalAmount"
          type="warning"
          :closable="false"
          style="margin-top: 16px"
        >
          余额不足，请先充值
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="showCheckout = false">取消</el-button>
        <el-button
          v-if="userBalance >= finalAmount"
          type="primary"
          :loading="checkoutLoading"
          @click="confirmCheckout"
          :disabled="isGift && !selectedRecipientId"
        >
          {{ isGift ? '确认赠送' : '确认支付' }}
        </el-button>
        <el-button v-else type="warning" @click="goToRecharge">去充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'
import { orderApi, couponApi, friendApi } from '@/api'
import type { UserCoupon, User } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const loading = ref(true)
const showCheckout = ref(false)
const checkoutLoading = ref(false)
const applicableCoupons = ref<UserCoupon[]>([])
const selectedCouponId = ref<number | null>(null)
const couponDiscount = ref(0)

const isGift = ref(false)
const friendsList = ref<Array<{ friendUser: User; friendId: number }>>([])
const selectedRecipientId = ref<number | null>(null)
const giftMessage = ref('')

const userBalance = computed(() => userStore.userInfo?.balance || 0)

const isSingleItem = computed(() => cartStore.items.length === 1)
const canGift = computed(() => isSingleItem.value)

const selectedCoupon = computed(() => {
  if (!selectedCouponId.value) return null
  return applicableCoupons.value.find(uc => uc.id === selectedCouponId.value) || null
})

const finalAmount = computed(() => {
  return Math.max(0, cartStore.totalAmount - couponDiscount.value)
})

onMounted(async () => {
  await cartStore.fetchCart()
  await Promise.all([loadApplicableCoupons(), loadFriends()])
  loading.value = false
})

async function loadFriends() {
  try {
    const res = await friendApi.getFriends()
    friendsList.value = res.data.data || []
  } catch (e) {
    friendsList.value = []
  }
}

watch(
  () => cartStore.items.length,
  () => {
    if (cartStore.items.length > 0) {
      loadApplicableCoupons()
    } else {
      applicableCoupons.value = []
      selectedCouponId.value = null
      couponDiscount.value = 0
    }
  }
)

async function loadApplicableCoupons() {
  if (cartStore.items.length === 0) {
    applicableCoupons.value = []
    selectedCouponId.value = null
    couponDiscount.value = 0
    return
  }
  try {
    const gameIds = cartStore.items.map(item => Number(item.gameId))
    const res = await couponApi.getApplicableCoupons(gameIds)
    const list = (res.data.data || []) as UserCoupon[]
    applicableCoupons.value = list.filter(uc => uc && uc.coupon)

    if (applicableCoupons.value.length > 0 && !selectedCouponId.value) {
      const best = applicableCoupons.value[0]
      selectedCouponId.value = best.id
      calculateCouponDiscount(best)
    } else if (selectedCouponId.value) {
      const current = applicableCoupons.value.find(uc => uc.id === selectedCouponId.value)
      if (current) {
        calculateCouponDiscount(current)
      } else {
        selectedCouponId.value = null
        couponDiscount.value = 0
      }
    }
  } catch (error) {
    applicableCoupons.value = []
    selectedCouponId.value = null
    couponDiscount.value = 0
  }
}

function calculateCouponDiscount(userCoupon: UserCoupon) {
  const coupon = userCoupon.coupon
  if (!coupon) {
    couponDiscount.value = 0
    return
  }

  const totalPrice = cartStore.totalAmount

  if (coupon.type === 'FULL_REDUCTION') {
    if (totalPrice >= coupon.minAmount) {
      couponDiscount.value = coupon.value
    } else {
      couponDiscount.value = 0
    }
  } else if (coupon.type === 'DISCOUNT') {
    if (totalPrice >= coupon.minAmount) {
      couponDiscount.value = Math.round((totalPrice * (1 - coupon.value / 100)) * 100) / 100
    } else {
      couponDiscount.value = 0
    }
  } else if (coupon.type === 'CATEGORY') {
    couponDiscount.value = 0
  }
}

function onCouponChange() {
  if (selectedCouponId.value) {
    const uc = applicableCoupons.value.find(c => c.id === selectedCouponId.value)
    if (uc) {
      calculateCouponDiscount(uc)
    }
  } else {
    couponDiscount.value = 0
  }
}

function getCouponLabel(uc: UserCoupon): string {
  const coupon = uc.coupon
  if (!coupon) return ''
  return `${coupon.name} - ${getCouponValueText(coupon)}`
}

function getCouponValueText(coupon: any): string {
  if (!coupon) return ''
  if (coupon.type === 'FULL_REDUCTION') {
    return `满${coupon.minAmount || 0}减${coupon.value || 0}`
  } else if (coupon.type === 'DISCOUNT') {
    return `${(coupon.value || 0) / 10}折优惠`
  } else if (coupon.type === 'CATEGORY') {
    if ((coupon.value || 0) > 50) {
      return `指定分类${(coupon.value || 0) / 10}折`
    } else {
      return `指定分类减${coupon.value || 0}`
    }
  }
  return ''
}

function goToGame(gameId: number) {
  router.push(`/game/${gameId}`)
}

async function handleRemove(gameId: number) {
  await cartStore.removeFromCart(gameId)
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cartStore.clearCart()
  } catch {
    // 取消
  }
}

function getSelectedFriend() {
  if (!selectedRecipientId.value) return null
  return friendsList.value.find(f => f.friendId === selectedRecipientId.value) || null
}

function handleCheckout() {
  if (isGift.value && !selectedRecipientId.value) {
    ElMessage.warning('请选择收礼好友')
    return
  }
  showCheckout.value = true
}

async function confirmCheckout() {
  if (isGift.value && !selectedRecipientId.value) {
    ElMessage.warning('请选择收礼好友')
    return
  }
  checkoutLoading.value = true
  try {
    const gameIds = cartStore.items.map(item => item.gameId)
    const res = await orderApi.createOrder({
      gameIds,
      userCouponId: selectedCouponId.value || undefined,
      recipientId: isGift.value ? selectedRecipientId.value || undefined : undefined,
      giftMessage: isGift.value ? giftMessage.value || undefined : undefined
    })
    const order = res.data.data

    await orderApi.payOrder(order.orderNo)

    const wasGift = isGift.value
    if (wasGift) {
      ElMessage.success('赠送成功！好友可在「礼物中心」领取')
    } else {
      ElMessage.success('支付成功！游戏已添加到您的游戏库')
    }
    showCheckout.value = false

    await Promise.all([
      cartStore.fetchCart(),
      userStore.fetchUserInfo(),
      loadApplicableCoupons()
    ])

    selectedCouponId.value = null
    couponDiscount.value = 0
    isGift.value = false
    selectedRecipientId.value = null
    giftMessage.value = ''

    if (wasGift) {
      router.push('/gifts')
    } else {
      router.push('/library')
    }
  } catch (error) {
    // 错误已处理
  } finally {
    checkoutLoading.value = false
  }
}

function goToRecharge() {
  showCheckout.value = false
  router.push('/profile')
}
</script>

<style lang="scss" scoped>
.cart-page {
  max-width: 1000px;
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

.cart-content {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
}

.cart-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);

  .game-cover {
    width: 120px;
    height: 56px;
    object-fit: cover;
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: opacity 0.3s;

    &:hover {
      opacity: 0.8;
    }
  }

  .item-info {
    flex: 1;

    h3 {
      font-size: 16px;
      color: var(--text-white);
      margin-bottom: 8px;
      cursor: pointer;

      &:hover {
        color: var(--steam-light-blue);
      }
    }
  }

  .item-price {
    display: flex;
    align-items: center;
    gap: 8px;

    .discount-tag {
      background: var(--steam-green);
      color: var(--steam-darker);
      padding: 2px 6px;
      border-radius: var(--radius-sm);
      font-weight: 600;
      font-size: 12px;
    }

    .original-price {
      color: var(--text-secondary);
      text-decoration: line-through;
      font-size: 14px;
    }

    .final-price {
      color: var(--text-primary);
      font-weight: 600;
    }
  }
}

.checkout-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 24px;
  border: 1px solid var(--border-color);
  height: fit-content;
  position: sticky;
  top: 84px;

  h3 {
    font-size: 18px;
    color: var(--text-white);
    margin-bottom: 16px;
  }

  .summary-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    color: var(--text-primary);

    &.discount {
      color: var(--steam-green);
    }

    &.coupon-row {
      color: var(--steam-light-blue);
    }

    &.total {
      font-size: 18px;
      font-weight: 600;
      color: var(--text-white);

      .pay-amount {
        color: var(--steam-green);
        font-size: 22px;
      }
    }
  }
}

.coupon-section {
  margin-bottom: 8px;

  .coupon-label {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    color: var(--text-primary);

    .coupon-count {
      color: var(--steam-green);
      font-size: 12px;
    }
  }

  .coupon-select {
    :deep(.el-select__wrapper) {
      background: rgba(0, 0, 0, 0.2);
    }
  }

  .coupon-discount {
    margin-top: 8px;
    color: var(--steam-green);
    font-size: 13px;
    text-align: right;
  }
}

.coupon-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .coupon-option-name {
    color: var(--text-white);
  }

  .coupon-option-desc {
    color: var(--steam-green);
    font-size: 12px;
  }
}

// 礼物相关
.gift-section {
  margin-bottom: 16px;

  .gift-toggle-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }

  .gift-label {
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--text-white);
    font-weight: 500;
    font-size: 14px;
  }

  .gift-tip {
    font-size: 12px;
    color: var(--text-secondary);
    padding: 4px 0;
  }

  .gift-form {
    margin-top: 12px;
    padding: 12px;
    background: rgba(103, 194, 58, 0.05);
    border: 1px solid rgba(103, 194, 58, 0.2);
    border-radius: var(--radius-sm);

    :deep(.el-form-item__label) {
      color: var(--text-primary);
    }

    :deep(.el-select__wrapper) {
      background: rgba(0, 0, 0, 0.2);
    }

    :deep(.el-textarea__inner) {
      background: rgba(0, 0, 0, 0.2);
      color: var(--text-primary);
    }
  }

  .friend-option {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;

    span {
      color: var(--text-white);
    }
  }

  .no-friends-tip {
    display: block;
    margin-top: 4px;
    font-size: 12px;
    color: var(--steam-light-blue);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

// 礼物对话框
.gift-info-dialog {
  margin-bottom: 16px;

  .recipient-info {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 8px;
  }

  .recipient-text {
    flex: 1;
  }

  .recipient-name {
    color: var(--text-white);
    font-weight: 600;
    font-size: 16px;
  }

  .recipient-message {
    margin-top: 4px;
    color: var(--text-primary);
    font-size: 13px;
    font-style: italic;
    opacity: 0.9;
  }
}

// 结算对话框
:deep(.checkout-dialog-wrapper) {
  .el-dialog {
    background: var(--bg-card);

    .el-dialog__title {
      color: var(--text-white);
    }
  }
}

.checkout-dialog {
  .order-items {
    max-height: 300px;
    overflow-y: auto;
  }

  .order-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 0;

    img {
      width: 60px;
      height: 28px;
      object-fit: cover;
      border-radius: var(--radius-sm);
    }

    span:first-of-type {
      flex: 1;
      color: var(--text-primary);
    }

    .price {
      color: var(--text-white);
      font-weight: 500;
    }
  }

  .coupon-info {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 0;
    margin-top: 8px;

    .coupon-name {
      flex: 1;
      color: var(--text-primary);
    }

    .coupon-save {
      color: var(--steam-green);
      font-weight: 600;
    }
  }

  .order-total, .balance-info {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;

    span:first-child {
      color: var(--text-secondary);
    }
  }

  .order-total .amount {
    font-size: 20px;
    font-weight: 600;
    color: var(--steam-green);
  }

  .insufficient {
    color: var(--el-color-danger);
  }
}

@media (max-width: 768px) {
  .cart-content {
    grid-template-columns: 1fr;
  }

  .checkout-card {
    position: static;
  }

  .cart-item {
    flex-wrap: wrap;

    .game-cover {
      width: 80px;
      height: 37px;
    }
  }
}
</style>
