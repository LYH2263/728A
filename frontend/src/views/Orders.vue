<template>
  <div class="orders-page">
    <h1 class="page-title">
      <el-icon><Document /></el-icon>
      我的订单
    </h1>
    
    <div v-if="loading" class="loading">
      <el-skeleton :rows="5" animated />
    </div>
    
    <template v-else>
      <div v-if="orders.length" class="orders-list">
        <div v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-header" @click="toggleExpand(order.id)">
            <div class="order-info">
              <span class="order-no">订单号：{{ order.orderNo }}</span>
              <span class="order-time">{{ formatDate(order.createdAt) }}</span>
              <span v-if="order.payTime" class="order-time">支付时间：{{ formatDate(order.payTime) }}</span>
            </div>
            <div class="header-right">
              <el-tag :type="getStatusType(order.status)">{{ getStatusText(order.status) }}</el-tag>
              <el-icon class="expand-icon" :class="{ expanded: expandedOrders.includes(order.id) }">
                <ArrowDown />
              </el-icon>
            </div>
          </div>
          
          <div class="order-items">
            <div v-for="item in order.orderItems" :key="item.id" class="order-item">
              <img :src="item.gameCover" :alt="item.gameTitle" @click="goToGame(item.gameId)" />
              <div class="item-info" @click="goToGame(item.gameId)">
                <h4>{{ item.gameTitle }}</h4>
                <span class="price">¥{{ item.price.toFixed(2) }}</span>
              </div>
              <div class="item-actions">
                <el-button
                  v-if="canApplyRefund(order, item)"
                  type="warning"
                  size="small"
                  :loading="checkingRefund[item.id] || refundLoading === item.id"
                  @click.stop="openRefundDialog(order, item)"
                >
                  申请退款
                </el-button>
              </div>
            </div>
          </div>
          
          <div v-if="expandedOrders.includes(order.id)" class="order-detail">
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item label="商品原价">¥{{ order.totalAmount.toFixed(2) }}</el-descriptions-item>
              <el-descriptions-item label="优惠金额">
                <span class="discount-text">¥{{ (order.discountAmount || 0).toFixed(2) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="优惠券抵扣" v-if="order.couponDiscount && order.couponDiscount > 0">
                <span class="coupon-text">
                  {{ order.userCoupon?.coupon?.name }} -¥{{ order.couponDiscount.toFixed(2) }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="优惠券抵扣" v-else>-</el-descriptions-item>
              <el-descriptions-item label="实付金额" :span="2">
                <span class="pay-amount">¥{{ order.payAmount.toFixed(2) }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          
          <div class="order-footer">
            <div class="order-total">
              <span>实付：<strong>¥{{ order.payAmount.toFixed(2) }}</strong></span>
            </div>
            <div class="order-actions" v-if="order.status === 'PENDING'">
              <el-button type="primary" size="small" @click="handlePay(order.orderNo)">去支付</el-button>
              <el-button size="small" @click="handleCancel(order.orderNo)">取消订单</el-button>
            </div>
          </div>
        </div>
      </div>
      
      <el-empty v-else description="暂无订单">
        <router-link to="/store">
          <el-button type="primary">去选购</el-button>
        </router-link>
      </el-empty>
    </template>

    <el-dialog v-model="refundDialogVisible" title="申请退款" width="500px" @close="resetRefundForm">
      <div class="refund-form">
        <div class="refund-game-info">
          <img :src="refundForm.gameCover" :alt="refundForm.gameTitle" />
          <div>
            <h4>{{ refundForm.gameTitle }}</h4>
            <p class="refund-price">退款金额：<span class="highlight">¥{{ refundForm.price?.toFixed(2) }}</span></p>
          </div>
        </div>

        <el-alert
          title="退款政策"
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
        >
          <ul class="policy-list">
            <li>购买后14天内可申请退款</li>
            <li>游玩时长不超过2小时</li>
            <li>退款将原路退回至账户余额</li>
          </ul>
        </el-alert>

        <el-form :model="refundForm" label-position="top">
          <el-form-item label="退款原因" required>
            <el-select v-model="refundForm.reasonTemplate" placeholder="请选择退款原因" @change="onReasonTemplateChange">
              <el-option label="游戏体验不佳" value="游戏体验不佳" />
              <el-option label="无法正常运行" value="无法正常运行" />
              <el-option label="与描述不符" value="与描述不符" />
              <el-option label="误购买" value="误购买" />
              <el-option label="其他原因" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="详细说明" required>
            <el-input
              v-model="refundForm.reason"
              type="textarea"
              :rows="4"
              placeholder="请详细描述您的退款原因（最多500字）"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingRefund" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi, refundApi } from '@/api'
import { useUserStore } from '@/store/user'
import type { Order, OrderItem } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const orders = ref<Order[]>([])
const expandedOrders = ref<number[]>([])
const checkingRefund = reactive<Record<number, boolean>>({})
const refundLoading = ref<number | null>(null)

const refundDialogVisible = ref(false)
const submittingRefund = ref(false)
const refundForm = reactive({
  orderId: 0,
  orderItemId: 0,
  gameId: 0,
  gameTitle: '',
  gameCover: '',
  price: 0,
  reasonTemplate: '',
  reason: ''
})

onMounted(async () => {
  await fetchOrders()
  loading.value = false
})

async function fetchOrders() {
  try {
    const res = await orderApi.getOrders()
    orders.value = res.data.data || []
    checkRefundEligibilityForAll()
  } catch (error) {
    orders.value = []
  }
}

async function checkRefundEligibilityForAll() {
  for (const order of orders.value) {
    if (!order.orderItems) continue
    for (const item of order.orderItems) {
      if (canRefundStatus(order.status)) {
        checkingRefund[item.id] = true
        try {
          const res = await refundApi.checkEligibility(item.id)
          ;(item as any).refundable = res.data.data?.eligible === true
        } catch {
          ;(item as any).refundable = false
        } finally {
          checkingRefund[item.id] = false
        }
      } else {
        ;(item as any).refundable = false
      }
    }
  }
}

function canRefundStatus(status: string) {
  return ['PAID', 'COMPLETED', 'PARTIAL_REFUND'].includes(status)
}

function canApplyRefund(order: Order, item: OrderItem) {
  if (!canRefundStatus(order.status)) return false
  if ((item as any).refundable === true) return true
  return false
}

function toggleExpand(orderId: number) {
  const idx = expandedOrders.value.indexOf(orderId)
  if (idx > -1) {
    expandedOrders.value.splice(idx, 1)
  } else {
    expandedOrders.value.push(orderId)
  }
}

function goToGame(gameId: number) {
  router.push(`/game/${gameId}`)
}

function formatDate(date: string) {
  return new Date(date).toLocaleString('zh-CN')
}

function getStatusType(status: string) {
  const types: Record<string, string> = {
    PENDING: 'warning',
    PAID: 'success',
    CANCELLED: 'info',
    COMPLETED: 'success',
    PARTIAL_REFUND: 'warning',
    FULL_REFUND: 'info'
  }
  return types[status] || 'info'
}

function getStatusText(status: string) {
  const texts: Record<string, string> = {
    PENDING: '待支付',
    PAID: '已支付',
    CANCELLED: '已取消',
    COMPLETED: '已完成',
    PARTIAL_REFUND: '部分退款',
    FULL_REFUND: '全额退款'
  }
  return texts[status] || status
}

async function handlePay(orderNo: string) {
  const order = orders.value.find(o => o.orderNo === orderNo)
  if (!order) return
  
  const balance = userStore.userInfo?.balance || 0
  if (balance < order.payAmount) {
    ElMessage.warning('余额不足，请先充值')
    router.push('/profile')
    return
  }
  
  try {
    await ElMessageBox.confirm(`确认支付 ¥${order.payAmount.toFixed(2)}？`, '确认支付', {
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    
    await orderApi.payOrder(orderNo)
    ElMessage.success('支付成功')
    await Promise.all([fetchOrders(), userStore.fetchUserInfo()])
  } catch (error) {
    // 取消或错误
  }
}

async function handleCancel(orderNo: string) {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await orderApi.cancelOrder(orderNo)
    ElMessage.success('订单已取消')
    await fetchOrders()
  } catch (error) {
    // 取消
  }
}

function openRefundDialog(order: Order, item: OrderItem) {
  refundForm.orderId = order.id
  refundForm.orderItemId = item.id
  refundForm.gameId = item.gameId
  refundForm.gameTitle = item.gameTitle
  refundForm.gameCover = item.gameCover || ''
  refundForm.price = item.price
  refundForm.reasonTemplate = ''
  refundForm.reason = ''
  refundDialogVisible.value = true
}

function onReasonTemplateChange(val: string) {
  if (val && val !== '其他') {
    refundForm.reason = val
  }
}

function resetRefundForm() {
  refundForm.reasonTemplate = ''
  refundForm.reason = ''
}

async function submitRefund() {
  if (!refundForm.reason.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }

  submittingRefund.value = true
  try {
    await refundApi.applyRefund({
      orderItemId: refundForm.orderItemId,
      reason: refundForm.reason.trim()
    })
    ElMessage.success('退款申请已提交，请等待审核')
    refundDialogVisible.value = false
    await fetchOrders()
  } catch (error) {
    // error already handled by interceptor
  } finally {
    submittingRefund.value = false
  }
}
</script>

<style lang="scss" scoped>
.orders-page {
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

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.2);
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: rgba(0, 0, 0, 0.3);
  }

  .order-info {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .order-no {
      color: var(--text-primary);
      font-size: 14px;
    }

    .order-time {
      color: var(--text-secondary);
      font-size: 12px;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .expand-icon {
      transition: transform 0.2s;
      color: var(--text-secondary);
      font-size: 18px;

      &.expanded {
        transform: rotate(180deg);
      }
    }
  }
}

.order-items {
  padding: 16px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;

  &:not(:last-child) {
    border-bottom: 1px solid var(--border-color);
  }

  img {
    width: 80px;
    height: 37px;
    object-fit: cover;
    border-radius: var(--radius-sm);
    cursor: pointer;
  }

  .item-info {
    flex: 1;
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;

    h4 {
      font-size: 14px;
      color: var(--text-white);

      &:hover {
        color: var(--steam-light-blue);
      }
    }

    .price {
      color: var(--text-primary);
    }
  }

  .item-actions {
    margin-left: 12px;
  }
}

.order-detail {
  padding: 0 16px 16px;
  border-top: 1px solid var(--border-color);
  padding-top: 16px;

  .discount-text {
    color: var(--steam-green);
  }

  .coupon-text {
    color: var(--steam-light-blue);
  }

  .pay-amount {
    color: var(--text-white);
    font-size: 20px;
    font-weight: 600;
  }
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-top: 1px solid var(--border-color);

  .order-total {
    display: flex;
    flex-direction: column;
    gap: 6px;

    span {
      color: var(--text-secondary);

      strong {
        color: var(--text-white);
        font-size: 18px;
      }
    }
  }

  .order-actions {
    display: flex;
    gap: 8px;
  }
}

.refund-form {
  .refund-game-info {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: rgba(0, 0, 0, 0.2);
    border-radius: var(--radius-md);
    margin-bottom: 16px;

    img {
      width: 120px;
      height: 56px;
      object-fit: cover;
      border-radius: var(--radius-sm);
    }

    h4 {
      color: var(--text-white);
      margin-bottom: 8px;
      font-size: 16px;
    }

    .refund-price {
      color: var(--text-secondary);
      margin: 0;

      .highlight {
        color: var(--steam-green);
        font-size: 20px;
        font-weight: 600;
      }
    }
  }

  .policy-list {
    margin: 8px 0 0;
    padding-left: 20px;
    color: var(--text-secondary);
    font-size: 12px;
    line-height: 1.8;
  }
}

@media (max-width: 600px) {
  .order-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;

    .header-right {
      width: 100%;
      justify-content: space-between;
    }
  }

  .order-item {
    flex-wrap: wrap;

    .item-info {
      flex: 1;
      min-width: 200px;
    }

    .item-actions {
      width: 100%;
      margin-left: 0;

      .el-button {
        width: 100%;
      }
    }
  }

  .order-footer {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;

    .order-actions {
      width: 100%;

      .el-button {
        flex: 1;
      }
    }
  }
}
</style>
