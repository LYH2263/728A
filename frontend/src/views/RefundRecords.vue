<template>
  <div class="refunds-page">
    <h1 class="page-title">
      <el-icon><RefreshLeft /></el-icon>
      退款记录
    </h1>

    <el-tabs v-model="activeTab" class="status-tabs" @tab-change="onTabChange">
      <el-tab-pane label="全部" name="ALL">
        <span class="tab-count">{{ refunds.length }}</span>
      </el-tab-pane>
      <el-tab-pane label="待审核" name="PENDING">
        <span class="tab-count">{{ statusCounts.PENDING }}</span>
      </el-tab-pane>
      <el-tab-pane label="处理中" name="APPROVED">
        <span class="tab-count">{{ statusCounts.APPROVED }}</span>
      </el-tab-pane>
      <el-tab-pane label="已退款" name="REFUNDED">
        <span class="tab-count">{{ statusCounts.REFUNDED }}</span>
      </el-tab-pane>
      <el-tab-pane label="已拒绝" name="REJECTED">
        <span class="tab-count">{{ statusCounts.REJECTED }}</span>
      </el-tab-pane>
    </el-tabs>

    <div v-if="loading" class="loading">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else>
      <div v-if="filteredRefunds.length" class="refunds-list">
        <div v-for="refund in filteredRefunds" :key="refund.id" class="refund-card">
          <div class="refund-header">
            <div class="refund-basic">
              <img :src="refund.gameCover" :alt="refund.gameTitle" class="game-cover" />
              <div class="refund-info">
                <h3 class="game-title">{{ refund.gameTitle }}</h3>
                <div class="refund-meta">
                  <span class="refund-no">退款单号：{{ refund.refundNo }}</span>
                  <span class="refund-amount">退款金额：<strong>¥{{ refund.orderItemPrice.toFixed(2) }}</strong></span>
                </div>
              </div>
            </div>
            <el-tag :type="getStatusType(refund.status)" size="large">
              {{ getStatusText(refund.status) }}
            </el-tag>
          </div>

          <div class="refund-reason">
            <span class="reason-label">退款原因：</span>
            <span class="reason-text">{{ refund.reason }}</span>
          </div>

          <div class="refund-order-link">
            <span class="reason-label">关联订单：</span>
            <span class="order-no-link" @click="goToOrder(refund.orderNo)">{{ refund.orderNo }}</span>
          </div>

          <el-steps :active="getTimelineStep(refund.status)" finish-status="success" class="refund-timeline" size="small">
            <el-step title="提交申请" :description="formatDate(refund.createdAt)" icon="Document" />
            <el-step title="审核中" :description="refund.reviewedAt ? formatDate(refund.reviewedAt) : '等待管理员审核'" icon="CircleCheck" />
            <el-step
              :title="refund.status === 'REJECTED' ? '申请拒绝' : '退款到账'"
              :description="getFinalStepDesc(refund)"
              :icon="refund.status === 'REJECTED' ? 'CircleClose' : 'Wallet'"
            />
          </el-steps>

          <div v-if="refund.status === 'REJECTED' && refund.reviewRemark" class="reject-remark">
            <el-alert type="warning" :closable="false">
              <template #title>
                <strong>拒绝原因：</strong>{{ refund.reviewRemark }}
              </template>
            </el-alert>
          </div>

          <div v-if="refund.reviewUser" class="review-info">
            <span class="reviewer">审核人：{{ refund.reviewUser.nickname || refund.reviewUser.username }}</span>
          </div>

          <div v-if="isAdmin && refund.status === 'PENDING'" class="admin-actions">
            <el-button type="danger" size="small" @click="openAdminReview(refund)">拒绝退款</el-button>
            <el-button type="success" size="small" @click="openAdminReview(refund)">通过退款</el-button>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无退款记录">
        <router-link to="/store">
          <el-button type="primary">去选购游戏</el-button>
        </router-link>
      </el-empty>
    </template>

    <el-dialog v-model="adminDialogVisible" v-if="isAdmin" title="审核退款" width="500px">
      <div v-if="adminReviewRefund" class="admin-review">
        <div class="review-game-info">
          <img :src="adminReviewRefund.gameCover" :alt="adminReviewRefund.gameTitle" />
          <div>
            <h4>{{ adminReviewRefund.gameTitle }}</h4>
            <p>退款单号：{{ adminReviewRefund.refundNo }}</p>
            <p>退款金额：<strong class="highlight">¥{{ adminReviewRefund.orderItemPrice.toFixed(2) }}</strong></p>
            <p>申请人：{{ adminReviewRefund.user?.nickname || adminReviewRefund.user?.username }}</p>
          </div>
        </div>
        <el-divider />
        <div class="review-reason">
          <strong>退款原因：</strong>
          <p>{{ adminReviewRefund.reason }}</p>
        </div>
        <el-form label-position="top" style="margin-top: 16px">
          <el-form-item label="审核备注">
            <el-input
              v-model="adminReviewRemark"
              type="textarea"
              :rows="3"
              placeholder="请输入审核备注（可选，最多500字）"
              maxlength="500"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="adminDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="adminSubmitting" @click="adminReview('REJECT')">拒绝</el-button>
        <el-button type="success" :loading="adminSubmitting" @click="adminReview('APPROVE')">通过并退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { refundApi } from '@/api'
import { useUserStore } from '@/store/user'
import type { RefundRequest } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const refunds = ref<RefundRequest[]>([])
const activeTab = ref('ALL')

const adminDialogVisible = ref(false)
const adminReviewRefund = ref<RefundRequest | null>(null)
const adminReviewRemark = ref('')
const adminSubmitting = ref(false)

const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')

const statusCounts = reactive({
  PENDING: 0,
  APPROVED: 0,
  REFUNDED: 0,
  REJECTED: 0
})

const filteredRefunds = computed(() => {
  if (activeTab.value === 'ALL') return refunds.value
  return refunds.value.filter(r => r.status === activeTab.value)
})

onMounted(async () => {
  await fetchRefunds()
  loading.value = false
})

async function fetchRefunds() {
  try {
    const res = isAdmin.value
      ? await refundApi.getAllRefunds()
      : await refundApi.getMyRefunds()
    refunds.value = res.data.data || []
    calcStatusCounts()
  } catch (error) {
    refunds.value = []
  }
}

function calcStatusCounts() {
  statusCounts.PENDING = refunds.value.filter(r => r.status === 'PENDING').length
  statusCounts.APPROVED = refunds.value.filter(r => r.status === 'APPROVED').length
  statusCounts.REFUNDED = refunds.value.filter(r => r.status === 'REFUNDED').length
  statusCounts.REJECTED = refunds.value.filter(r => r.status === 'REJECTED').length
}

function onTabChange() {
  // just triggers computed
}

function formatDate(date: string) {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

function getStatusType(status: string) {
  const types: Record<string, string> = {
    PENDING: 'warning',
    APPROVED: 'primary',
    REFUNDED: 'success',
    REJECTED: 'danger'
  }
  return types[status] || 'info'
}

function getStatusText(status: string) {
  const texts: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '处理中',
    REFUNDED: '已退款',
    REJECTED: '已拒绝'
  }
  return texts[status] || status
}

function getTimelineStep(status: string): number {
  switch (status) {
    case 'PENDING': return 0
    case 'APPROVED': return 1
    case 'REFUNDED': return 2
    case 'REJECTED': return 2
    default: return 0
  }
}

function getFinalStepDesc(refund: RefundRequest): string {
  if (refund.status === 'REJECTED') {
    return refund.reviewedAt ? formatDate(refund.reviewedAt) : ''
  }
  if (refund.status === 'REFUNDED') {
    return refund.refundedAt ? formatDate(refund.refundedAt) : ''
  }
  return '等待处理'
}

function goToOrder(_orderNo: string) {
  router.push('/orders')
}

async function openAdminReview(refund: RefundRequest) {
  adminReviewRefund.value = refund
  adminReviewRemark.value = ''
  adminDialogVisible.value = true
}

async function adminReview(action: 'APPROVE' | 'REJECT') {
  if (!adminReviewRefund.value) return

  try {
    if (action === 'APPROVE') {
      await ElMessageBox.confirm(
        `确认通过退款申请？将退回 ¥${adminReviewRefund.value.orderItemPrice.toFixed(2)} 至用户余额。`,
        '确认退款',
        { type: 'warning' }
      )
    }

    adminSubmitting.value = true
    await refundApi.reviewRefund({
      refundId: adminReviewRefund.value.id,
      action,
      remark: adminReviewRemark.value.trim() || undefined
    })
    ElMessage.success(action === 'APPROVE' ? '退款已通过' : '已拒绝退款')
    adminDialogVisible.value = false
    await fetchRefunds()
  } catch (error) {
    // cancelled or handled
  } finally {
    adminSubmitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.refunds-page {
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

.status-tabs {
  margin-bottom: 24px;

  :deep(.el-tabs__item) {
    color: var(--text-secondary);

    .tab-count {
      margin-left: 4px;
      padding: 0 8px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 10px;
      font-size: 12px;
    }

    &.is-active {
      color: var(--steam-light-blue);
    }
  }
}

.refunds-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.refund-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  padding: 20px;
}

.refund-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;

  .refund-basic {
    display: flex;
    gap: 16px;
    flex: 1;
  }

  .game-cover {
    width: 160px;
    height: 75px;
    object-fit: cover;
    border-radius: var(--radius-sm);
  }

  .refund-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;

    .game-title {
      color: var(--text-white);
      font-size: 16px;
      margin: 0;
    }

    .refund-meta {
      display: flex;
      gap: 24px;
      color: var(--text-secondary);
      font-size: 13px;

      .refund-amount strong {
        color: var(--steam-green);
        font-size: 16px;
      }
    }
  }
}

.refund-reason {
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: var(--radius-sm);
  margin-bottom: 12px;

  .reason-label {
    color: var(--text-secondary);
    font-size: 13px;
  }

  .reason-text {
    color: var(--text-primary);
    font-size: 14px;
  }
}

.refund-order-link {
  margin-bottom: 16px;
  padding: 0 4px;

  .reason-label {
    color: var(--text-secondary);
    font-size: 13px;
  }

  .order-no-link {
    color: var(--steam-light-blue);
    cursor: pointer;
    font-size: 13px;

    &:hover {
      text-decoration: underline;
    }
  }
}

.refund-timeline {
  margin-bottom: 16px;

  :deep(.el-step__title) {
    color: var(--text-white) !important;
  }

  :deep(.el-step__description) {
    color: var(--text-secondary) !important;
  }
}

.reject-remark {
  margin-bottom: 12px;
}

.review-info {
  color: var(--text-secondary);
  font-size: 12px;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}

.admin-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.admin-review {
  .review-game-info {
    display: flex;
    gap: 16px;
    align-items: center;

    img {
      width: 140px;
      height: 65px;
      object-fit: cover;
      border-radius: var(--radius-sm);
    }

    h4 {
      color: var(--text-white);
      margin: 0 0 8px;
    }

    p {
      margin: 4px 0;
      color: var(--text-secondary);
      font-size: 13px;

      .highlight {
        color: var(--steam-green);
        font-size: 16px;
      }
    }
  }

  .review-reason {
    color: var(--text-primary);

    p {
      margin-top: 8px;
      padding: 12px;
      background: rgba(0, 0, 0, 0.2);
      border-radius: var(--radius-sm);
    }
  }
}

@media (max-width: 600px) {
  .refund-header {
    flex-direction: column;

    .refund-basic {
      flex-direction: column;

      .game-cover {
        width: 100%;
        height: auto;
        aspect-ratio: 16 / 7;
      }
    }
  }

  .refund-meta {
    flex-direction: column;
    gap: 4px !important;
  }
}
</style>
