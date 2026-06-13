<template>
  <div class="wallet-page">
    <h1 class="page-title">
      <el-icon><Wallet /></el-icon>
      我的钱包
    </h1>

    <!-- 余额卡片 -->
    <div class="balance-card">
      <div class="balance-info">
        <span class="balance-label">当前余额</span>
        <span class="balance-amount">¥{{ (overview?.balance || 0).toFixed(2) }}</span>
      </div>
      <div class="monthly-stats">
        <div class="stat-item income">
          <span class="stat-label">本月收入</span>
          <span class="stat-value">+¥{{ (overview?.monthlySummary?.income || 0).toFixed(2) }}</span>
        </div>
        <div class="stat-item expense">
          <span class="stat-label">本月支出</span>
          <span class="stat-value">-¥{{ (overview?.monthlySummary?.expense || 0).toFixed(2) }}</span>
        </div>
        <div class="stat-item net">
          <span class="stat-label">本月净收入</span>
          <span class="stat-value" :class="netIncomeClass">
            {{ netIncomePrefix }}¥{{ Math.abs(overview?.monthlySummary?.netIncome || 0).toFixed(2) }}
          </span>
        </div>
      </div>
      <el-button type="success" class="recharge-btn" @click="showRechargeDialog = true">
        <el-icon><Plus /></el-icon>
        充值
      </el-button>
    </div>

    <!-- 筛选区 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item">
          <span class="filter-label">交易类型</span>
          <el-select v-model="filterType" placeholder="全部类型" clearable @change="fetchTransactions(1)">
            <el-option label="充值" value="RECHARGE" />
            <el-option label="消费" value="PURCHASE" />
            <el-option label="退款" value="REFUND" />
            <el-option label="赠送" value="GIFT" />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">月份</span>
          <el-date-picker
            v-model="filterMonth"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            clearable
            @change="handleMonthChange"
          />
        </div>
      </div>
    </div>

    <!-- 趋势图表 -->
    <div class="trend-card">
      <div class="card-header">
        <h3>
          <el-icon><TrendCharts /></el-icon>
          近6个月收支趋势
        </h3>
      </div>
      <div class="chart-container">
        <canvas ref="chartCanvas" width="800" height="300"></canvas>
      </div>
      <div class="chart-legend">
        <span class="legend-item"><span class="legend-color income"></span>收入</span>
        <span class="legend-item"><span class="legend-color expense"></span>支出</span>
      </div>
    </div>

    <!-- 流水列表 -->
    <div class="transactions-card">
      <div class="card-header">
        <h3>
          <el-icon><List /></el-icon>
          交易明细
        </h3>
        <span class="record-count">共 {{ pagination.total }} 条记录</span>
      </div>

      <div v-loading="loading" class="transaction-list">
        <div
          v-for="item in transactions"
          :key="item.id"
          class="transaction-item"
        >
          <div class="transaction-icon" :class="getTypeClass(item.type)">
            <el-icon>{{ getTypeIcon(item.type) }}</el-icon>
          </div>
          <div class="transaction-info">
            <div class="transaction-title">
              {{ getTypeName(item.type) }}
            </div>
            <div class="transaction-desc">
              {{ item.description || getTypeDesc(item.type) }}
            </div>
            <div class="transaction-meta">
              <span v-if="item.orderNo" class="order-no">订单号: {{ item.orderNo }}</span>
              <span class="transaction-time">{{ formatDate(item.createdAt) }}</span>
            </div>
          </div>
          <div class="transaction-amount" :class="getAmountClass(item.type)">
            {{ getAmountPrefix(item.type) }}¥{{ Math.abs(item.amount).toFixed(2) }}
          </div>
        </div>

        <el-empty v-if="!loading && transactions.length === 0" description="暂无交易记录" />
      </div>

      <div class="pagination-section">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="fetchTransactions"
        />
      </div>
    </div>

    <!-- 充值对话框 -->
    <el-dialog v-model="showRechargeDialog" title="充值余额" width="400px">
      <div class="recharge-options">
        <div
          v-for="amount in rechargeAmounts"
          :key="amount"
          class="recharge-option"
          :class="{ active: selectedAmount === amount }"
          @click="selectedAmount = amount"
        >
          ¥{{ amount }}
        </div>
      </div>
      <el-input
        v-model.number="customAmount"
        placeholder="自定义金额"
        type="number"
        :min="1"
        style="margin-top: 16px"
        @focus="selectedAmount = 0"
      >
        <template #prepend>¥</template>
      </el-input>
      <template #footer>
        <el-button @click="showRechargeDialog = false">取消</el-button>
        <el-button type="primary" :loading="rechargeLoading" @click="handleRecharge">
          充值 ¥{{ finalAmount }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useUserStore } from '@/store/user'
import { walletApi } from '@/api'
import { ElMessage } from 'element-plus'
import type { WalletOverview, WalletTransaction, WalletTrendItem } from '@/types'

const userStore = useUserStore()

const overview = ref<WalletOverview | null>(null)
const transactions = ref<WalletTransaction[]>([])
const loading = ref(false)
const rechargeLoading = ref(false)

const filterType = ref<string>('')
const filterMonth = ref<string>('')

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
  totalPages: 0
})

const showRechargeDialog = ref(false)
const rechargeAmounts = [50, 100, 200, 500, 1000]
const selectedAmount = ref(100)
const customAmount = ref<number | null>(null)

const finalAmount = computed(() => {
  return selectedAmount.value > 0 ? selectedAmount.value : (customAmount.value || 0)
})

const netIncomeClass = computed(() => {
  const net = overview.value?.monthlySummary?.netIncome || 0
  return net >= 0 ? 'positive' : 'negative'
})

const netIncomePrefix = computed(() => {
  const net = overview.value?.monthlySummary?.netIncome || 0
  return net >= 0 ? '+' : '-'
})

const chartCanvas = ref<HTMLCanvasElement | null>(null)

onMounted(async () => {
  await Promise.all([
    fetchOverview(),
    fetchTransactions(1)
  ])
})

async function fetchOverview() {
  try {
    const res = await walletApi.getOverview()
    overview.value = res.data.data
    await nextTick()
    drawChart()
  } catch (error) {
    console.error('获取钱包概览失败:', error)
  }
}

async function fetchTransactions(page: number) {
  loading.value = true
  try {
    const res = await walletApi.getTransactions({
      type: filterType.value || undefined,
      month: filterMonth.value || undefined,
      page,
      size: pagination.size
    })
    const data = res.data.data
    transactions.value = data.list
    pagination.page = data.page
    pagination.size = data.size
    pagination.total = data.total
    pagination.totalPages = data.totalPages
  } catch (error) {
    console.error('获取交易记录失败:', error)
  } finally {
    loading.value = false
  }
}

function handleMonthChange() {
  fetchTransactions(1)
}

function handleSizeChange(size: number) {
  pagination.size = size
  fetchTransactions(1)
}

function getTypeClass(type: string) {
  const map: Record<string, string> = {
    RECHARGE: 'type-recharge',
    PURCHASE: 'type-purchase',
    REFUND: 'type-refund',
    GIFT: 'type-gift'
  }
  return map[type] || ''
}

function getTypeIcon(type: string) {
  const map: Record<string, any> = {
    RECHARGE: 'TopUp',
    PURCHASE: 'ShoppingCart',
    REFUND: 'RefreshLeft',
    GIFT: 'Present'
  }
  return map[type] || 'Wallet'
}

function getTypeName(type: string) {
  const map: Record<string, string> = {
    RECHARGE: '账户充值',
    PURCHASE: '游戏购买',
    REFUND: '退款到账',
    GIFT: '赠送金额'
  }
  return map[type] || type
}

function getTypeDesc(type: string) {
  const map: Record<string, string> = {
    RECHARGE: '余额充值',
    PURCHASE: '购买游戏商品',
    REFUND: '游戏退款',
    GIFT: '平台赠送'
  }
  return map[type] || ''
}

function getAmountClass(type: string) {
  if (type === 'PURCHASE') return 'amount-negative'
  return 'amount-positive'
}

function getAmountPrefix(type: string) {
  if (type === 'PURCHASE') return '-'
  return '+'
}

function formatDate(dateStr: string) {
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

async function handleRecharge() {
  if (finalAmount.value <= 0) {
    ElMessage.warning('请选择或输入充值金额')
    return
  }

  rechargeLoading.value = true
  try {
    await userStore.recharge(finalAmount.value)
    ElMessage.success(`充值成功，当前余额 ¥${userStore.userInfo?.balance?.toFixed(2)}`)
    showRechargeDialog.value = false
    selectedAmount.value = 100
    customAmount.value = null
    await fetchOverview()
    await fetchTransactions(1)
  } catch (error) {
    // 错误已处理
  } finally {
    rechargeLoading.value = false
  }
}

function drawChart() {
  const canvas = chartCanvas.value
  if (!canvas || !overview.value?.trendData) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const dpr = window.devicePixelRatio || 1
  const rect = canvas.getBoundingClientRect()
  canvas.width = rect.width * dpr
  canvas.height = rect.height * dpr
  ctx.scale(dpr, dpr)

  const width = rect.width
  const height = rect.height
  const padding = { top: 30, right: 30, bottom: 50, left: 60 }
  const chartWidth = width - padding.left - padding.right
  const chartHeight = height - padding.top - padding.bottom

  ctx.clearRect(0, 0, width, height)

  const data: WalletTrendItem[] = overview.value.trendData
  if (data.length === 0) return

  const maxValue = Math.max(
    ...data.map(d => Math.max(d.income, d.expense)),
    100
  )
  const barWidth = (chartWidth / data.length) * 0.35
  const gap = (chartWidth / data.length) * 0.1

  ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)'
  ctx.lineWidth = 1
  for (let i = 0; i <= 4; i++) {
    const y = padding.top + (chartHeight / 4) * i
    ctx.beginPath()
    ctx.moveTo(padding.left, y)
    ctx.lineTo(width - padding.right, y)
    ctx.stroke()

    const value = maxValue - (maxValue / 4) * i
    ctx.fillStyle = 'rgba(255, 255, 255, 0.5)'
    ctx.font = '12px -apple-system, BlinkMacSystemFont, sans-serif'
    ctx.textAlign = 'right'
    ctx.fillText(`¥${value.toFixed(0)}`, padding.left - 10, y + 4)
  }

  data.forEach((item, index) => {
    const x = padding.left + (chartWidth / data.length) * index + gap
    const incomeHeight = (item.income / maxValue) * chartHeight
    const expenseHeight = (item.expense / maxValue) * chartHeight

    const incomeGradient = ctx.createLinearGradient(0, padding.top + chartHeight - incomeHeight, 0, padding.top + chartHeight)
    incomeGradient.addColorStop(0, 'rgba(103, 194, 58, 0.8)')
    incomeGradient.addColorStop(1, 'rgba(103, 194, 58, 0.3)')
    ctx.fillStyle = incomeGradient
    ctx.fillRect(x, padding.top + chartHeight - incomeHeight, barWidth, incomeHeight)

    const expenseGradient = ctx.createLinearGradient(0, padding.top + chartHeight - expenseHeight, 0, padding.top + chartHeight)
    expenseGradient.addColorStop(0, 'rgba(245, 108, 108, 0.8)')
    expenseGradient.addColorStop(1, 'rgba(245, 108, 108, 0.3)')
    ctx.fillStyle = expenseGradient
    ctx.fillRect(x + barWidth + gap, padding.top + chartHeight - expenseHeight, barWidth, expenseHeight)

    ctx.fillStyle = 'rgba(255, 255, 255, 0.7)'
    ctx.font = '12px -apple-system, BlinkMacSystemFont, sans-serif'
    ctx.textAlign = 'center'
    const monthLabel = item.month.split('-')[1] + '月'
    ctx.fillText(monthLabel, x + barWidth + gap / 2, height - padding.bottom + 25)
  })
}

watch(() => overview.value?.trendData, () => {
  nextTick(() => drawChart())
})
</script>

<style lang="scss" scoped>
.wallet-page {
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

.balance-card {
  background: linear-gradient(135deg, #1a2332 0%, #151b28 100%);
  border-radius: var(--radius-lg);
  padding: 32px;
  margin-bottom: 24px;
  border: 1px solid var(--border-color);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    right: -20%;
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba(164, 208, 7, 0.1) 0%, transparent 70%);
    border-radius: 50%;
  }

  .balance-info {
    position: relative;
    z-index: 1;
    margin-bottom: 24px;

    .balance-label {
      display: block;
      color: var(--text-secondary);
      font-size: 14px;
      margin-bottom: 8px;
    }

    .balance-amount {
      font-size: 48px;
      font-weight: 700;
      color: var(--steam-green);
      letter-spacing: -1px;
    }
  }

  .monthly-stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 24px;
    position: relative;
    z-index: 1;
    padding-top: 24px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);

    .stat-item {
      .stat-label {
        display: block;
        color: var(--text-secondary);
        font-size: 13px;
        margin-bottom: 6px;
      }

      .stat-value {
        font-size: 20px;
        font-weight: 600;

        &.positive {
          color: var(--steam-green);
        }

        &.negative {
          color: #f56c6c;
        }
      }

      &.income .stat-value {
        color: var(--steam-green);
      }

      &.expense .stat-value {
        color: #f56c6c;
      }
    }
  }

  .recharge-btn {
    position: absolute;
    top: 32px;
    right: 32px;
    z-index: 1;
  }
}

.filter-section {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px 24px;
  margin-bottom: 24px;
  border: 1px solid var(--border-color);

  .filter-row {
    display: flex;
    gap: 24px;
    align-items: center;
  }

  .filter-item {
    display: flex;
    align-items: center;
    gap: 12px;

    .filter-label {
      color: var(--text-secondary);
      font-size: 14px;
      white-space: nowrap;
    }

    :deep(.el-select),
    :deep(.el-date-editor) {
      width: 180px;
    }
  }
}

.trend-card,
.transactions-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid var(--border-color);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      color: var(--text-white);
      margin: 0;
    }

    .record-count {
      color: var(--text-secondary);
      font-size: 14px;
    }
  }
}

.chart-container {
  width: 100%;
  height: 300px;
  margin-bottom: 16px;

  canvas {
    width: 100% !important;
    height: 100% !important;
  }
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 32px;

  .legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--text-secondary);
    font-size: 14px;

    .legend-color {
      width: 16px;
      height: 16px;
      border-radius: 4px;

      &.income {
        background: rgba(103, 194, 58, 0.8);
      }

      &.expense {
        background: rgba(245, 108, 108, 0.8);
      }
    }
  }
}

.transaction-list {
  min-height: 200px;
}

.transaction-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);

  &:last-child {
    border-bottom: none;
  }

  .transaction-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;

    &.type-recharge {
      background: rgba(103, 194, 58, 0.15);
      color: var(--steam-green);
    }

    &.type-purchase {
      background: rgba(245, 108, 108, 0.15);
      color: #f56c6c;
    }

    &.type-refund {
      background: rgba(64, 158, 255, 0.15);
      color: #409eff;
    }

    &.type-gift {
      background: rgba(230, 162, 60, 0.15);
      color: #e6a23c;
    }
  }

  .transaction-info {
    flex: 1;
    min-width: 0;

    .transaction-title {
      font-size: 15px;
      color: var(--text-white);
      font-weight: 500;
      margin-bottom: 4px;
    }

    .transaction-desc {
      font-size: 13px;
      color: var(--text-secondary);
      margin-bottom: 6px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .transaction-meta {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: var(--text-tertiary);

      .order-no {
        font-family: monospace;
      }
    }
  }

  .transaction-amount {
    font-size: 18px;
    font-weight: 600;
    flex-shrink: 0;

    &.amount-positive {
      color: var(--steam-green);
    }

    &.amount-negative {
      color: #f56c6c;
    }
  }
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);

  :deep(.el-pagination) {
    --el-pagination-bg-color: transparent;
    --el-pagination-text-color: var(--text-secondary);
    --el-pagination-hover-text-color: var(--text-white);
    --el-pagination-button-bg-color: rgba(255, 255, 255, 0.05);
    --el-pagination-button-hover-bg-color: rgba(255, 255, 255, 0.1);
    --el-pagination-button-disabled-bg-color: rgba(255, 255, 255, 0.03);
  }
}

.recharge-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;

  .recharge-option {
    padding: 16px;
    text-align: center;
    background: rgba(0, 0, 0, 0.2);
    border: 2px solid var(--border-color);
    border-radius: var(--radius-md);
    cursor: pointer;
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    transition: all 0.3s;

    &:hover {
      border-color: var(--steam-light-blue);
    }

    &.active {
      border-color: var(--steam-green);
      background: rgba(164, 208, 7, 0.1);
      color: var(--steam-green);
    }
  }
}

@media (max-width: 768px) {
  .balance-card {
    padding: 24px 20px;

    .balance-info .balance-amount {
      font-size: 36px;
    }

    .monthly-stats {
      grid-template-columns: 1fr;
      gap: 16px;
    }

    .recharge-btn {
      position: static;
      margin-top: 16px;
      width: 100%;
    }
  }

  .filter-section .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-item {
    flex-direction: column;
    align-items: stretch;

    :deep(.el-select),
    :deep(.el-date-editor) {
      width: 100%;
    }
  }

  .transaction-item {
    flex-wrap: wrap;

    .transaction-amount {
      width: 100%;
      text-align: right;
      padding-left: 64px;
    }
  }
}
</style>
