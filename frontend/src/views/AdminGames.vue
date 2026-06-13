<template>
  <div class="admin-games-page">
    <div class="page-header">
      <h2>游戏库存管理</h2>
    </div>

    <div class="stats-cards">
      <el-card class="stat-card warning-card">
        <div class="stat-content">
          <div class="stat-label">库存预警数量</div>
          <div class="stat-value">{{ lowStockCount }}</div>
          <div class="stat-desc">阈值：{{ stockThreshold }} 件</div>
        </div>
        <el-icon class="stat-icon"><Warning /></el-icon>
      </el-card>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索游戏名称、开发商、发行商"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select v-model="statusFilter" placeholder="全部状态" clearable class="filter-select" @change="handleSearch">
        <el-option label="上架中" :value="1" />
        <el-option label="已下架" :value="0" />
      </el-select>

      <el-checkbox v-model="lowStockOnly" @change="handleSearch">
        仅显示低库存
      </el-checkbox>

      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
    </div>

    <div class="action-bar">
      <el-button type="success" :disabled="selectedIds.length === 0" @click="handleBatchOnSale">
        批量上架
      </el-button>
      <el-button type="info" :disabled="selectedIds.length === 0" @click="handleBatchOffSale">
        批量下架
      </el-button>
      <el-button type="warning" :disabled="selectedIds.length === 0" @click="showBatchStockDialog = true">
        批量调整库存
      </el-button>
      <span class="selected-tip" v-if="selectedIds.length > 0">
        已选择 {{ selectedIds.length }} 项
      </span>
    </div>

    <el-table
      ref="tableRef"
      :data="gameList"
      v-loading="loading"
      row-key="id"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
      :row-class-name="tableRowClassName"
      border
      stripe
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="游戏名称" min-width="180" />
      <el-table-column prop="developer" label="开发商" width="140" />
      <el-table-column prop="publisher" label="发行商" width="140" />
      <el-table-column label="价格" width="120">
        <template #default="{ row }">
          <span v-if="row.discountPrice && row.discountPrice < row.originalPrice" class="price">
            ¥{{ row.discountPrice }}
            <span class="original-price">¥{{ row.originalPrice }}</span>
          </span>
          <span v-else class="price">¥{{ row.originalPrice }}</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="salesCount"
        label="销量"
        width="110"
        sortable="custom"
        :sort-orders="['ascending', 'descending', null]"
      />
      <el-table-column
        prop="stock"
        label="库存"
        width="160"
        sortable="custom"
        :sort-orders="['ascending', 'descending', null]"
      >
        <template #default="{ row }">
          <div class="stock-cell">
            <el-input-number
              v-if="row.editing"
              v-model="row.editStock"
              :min="0"
              size="small"
              class="stock-input"
            />
            <span v-else :class="{ 'low-stock': row.lowStock }">
              {{ row.stock }}
              <el-tag v-if="row.lowStock" type="danger" size="small" class="stock-tag">预警</el-tag>
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <template v-if="row.editing">
            <el-button type="primary" size="small" @click="saveStock(row)">保存</el-button>
            <el-button size="small" @click="cancelEdit(row)">取消</el-button>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="startEdit(row)">编辑库存</el-button>
            <el-button
              :type="row.status === 1 ? 'info' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" @click="openStockLogs(row)">变更记录</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog v-model="showBatchStockDialog" title="批量调整库存" width="400px">
      <el-form label-width="80px">
        <el-form-item label="库存数量">
          <el-input-number v-model="batchStockValue" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchStockDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchStock">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showStockLogsDialog" :title="`库存变更记录 - ${currentLogGame?.title || ''}`" width="700px">
      <el-table
        :data="stockLogList"
        v-loading="logsLoading"
        border
        stripe
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="adminUsername" label="操作人" width="120">
          <template #default="{ row }">
            {{ row.adminUsername || 'ID:' + row.adminId }}
          </template>
        </el-table-column>
        <el-table-column label="变更类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="changeTypeTagType(row.changeType)">
              {{ changeTypeLabel(row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="库存变化" width="180">
          <template #default="{ row }">
            <span class="stock-change">
              <span class="stock-before">{{ row.stockBefore }}</span>
              <el-icon class="arrow-icon"><Right /></el-icon>
              <span class="stock-after">{{ row.stockAfter }}</span>
              <span :class="row.stockAfter >= row.stockBefore ? 'diff-plus' : 'diff-minus'">
                ({{ row.stockAfter >= row.stockBefore ? '+' : '' }}{{ row.stockAfter - row.stockBefore }})
              </span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" />
        <el-table-column prop="createdAt" label="时间" width="170" />
      </el-table>
      <div class="logs-pagination" v-if="stockLogTotal > 20">
        <el-pagination
          v-model:current-page="logsCurrentPage"
          v-model:page-size="20"
          :total="stockLogTotal"
          layout="prev, pager, next, total"
          background
          @current-change="fetchStockLogs"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Warning, Right } from '@element-plus/icons-vue'
import { adminGameApi } from '@/api'
import type { Game, PageResult, StockChangeLog } from '@/types'

const loading = ref(false)
const gameList = ref<Game[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const statusFilter = ref<number | null>(null)
const lowStockOnly = ref(false)
const selectedIds = ref<number[]>([])
const lowStockCount = ref(0)
const stockThreshold = ref(10)
const showBatchStockDialog = ref(false)
const batchStockValue = ref(0)
const sortField = ref('')
const sortOrder = ref('')

const showStockLogsDialog = ref(false)
const currentLogGame = ref<Game | null>(null)
const stockLogList = ref<StockChangeLog[]>([])
const stockLogTotal = ref(0)
const logsCurrentPage = ref(1)
const logsLoading = ref(false)

const tableRowClassName = ({ row }: { row: Game }) => {
  if (row.lowStock) {
    return 'low-stock-row'
  }
  return ''
}

function changeTypeLabel(type: string) {
  const map: Record<string, string> = {
    ADJUST: '单个调整',
    BATCH_ADJUST: '批量调整',
    ORDER_DEDUCT: '订单扣减',
    ORDER_RESTORE: '订单回补'
  }
  return map[type] || type
}

function changeTypeTagType(type: string) {
  const map: Record<string, string> = {
    ADJUST: 'primary',
    BATCH_ADJUST: 'warning',
    ORDER_DEDUCT: 'danger',
    ORDER_RESTORE: 'success'
  }
  return map[type] || 'info'
}

async function fetchStats() {
  try {
    const res = await adminGameApi.getStats()
    const data = res.data.data
    lowStockCount.value = data.lowStockCount
    stockThreshold.value = data.stockThreshold
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

async function fetchGames() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      status: statusFilter.value ?? undefined,
      lowStockOnly: lowStockOnly.value || undefined,
      sortBy: sortField.value || undefined,
      sortOrder: sortOrder.value || undefined
    }
    const res = await adminGameApi.getGames(params)
    const data: PageResult<Game> = res.data.data
    gameList.value = data.list.map(item => ({ ...item, editing: false, editStock: item.stock } as any))
    total.value = data.total
  } catch (error) {
    console.error('获取游戏列表失败', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchGames()
}

function handleSizeChange(val: number) {
  pageSize.value = val
  currentPage.value = 1
  fetchGames()
}

function handleCurrentChange(val: number) {
  currentPage.value = val
  fetchGames()
}

function handleSortChange({ prop, order }: { prop: string, order: string | null }) {
  if (!order) {
    sortField.value = ''
    sortOrder.value = ''
  } else {
    if (prop === 'stock') {
      sortField.value = 'stock'
    } else if (prop === 'salesCount') {
      sortField.value = 'sales'
    } else {
      sortField.value = prop
    }
    sortOrder.value = order === 'ascending' ? 'asc' : 'desc'
  }
  currentPage.value = 1
  fetchGames()
}

function handleSelectionChange(selection: Game[]) {
  selectedIds.value = selection.map(item => item.id)
}

function startEdit(row: any) {
  row.editStock = row.stock
  row.editing = true
}

function cancelEdit(row: any) {
  row.editing = false
  row.editStock = row.stock
}

async function saveStock(row: any) {
  try {
    await adminGameApi.updateStock(row.id, row.editStock)
    row.stock = row.editStock
    row.lowStock = row.stock < stockThreshold.value
    row.editing = false
    ElMessage.success('库存更新成功')
    fetchStats()
  } catch (error) {
    console.error('更新库存失败', error)
  }
}

async function toggleStatus(row: Game) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定要${action}该游戏吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await adminGameApi.batchUpdateStatus([row.id], newStatus)
    row.status = newStatus
    ElMessage.success(`${action}成功`)
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('更新状态失败', error)
    }
  }
}

async function handleBatchOnSale() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要上架选中的 ${selectedIds.value.length} 个游戏吗？`, '批量上架', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await adminGameApi.batchUpdateStatus(selectedIds.value, 1)
    ElMessage.success('批量上架成功')
    fetchGames()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量上架失败', error)
    }
  }
}

async function handleBatchOffSale() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要下架选中的 ${selectedIds.value.length} 个游戏吗？`, '批量下架', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await adminGameApi.batchUpdateStatus(selectedIds.value, 0)
    ElMessage.success('批量下架成功')
    fetchGames()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量下架失败', error)
    }
  }
}

async function confirmBatchStock() {
  if (selectedIds.value.length === 0) return
  try {
    await adminGameApi.batchUpdateStock(selectedIds.value, batchStockValue.value)
    ElMessage.success('批量调整库存成功')
    showBatchStockDialog.value = false
    fetchGames()
    fetchStats()
  } catch (error) {
    console.error('批量调整库存失败', error)
  }
}

async function openStockLogs(row: Game) {
  currentLogGame.value = row
  logsCurrentPage.value = 1
  stockLogList.value = []
  stockLogTotal.value = 0
  showStockLogsDialog.value = true
  await fetchStockLogs()
}

async function fetchStockLogs() {
  if (!currentLogGame.value) return
  logsLoading.value = true
  try {
    const res = await adminGameApi.getStockLogs(currentLogGame.value.id, logsCurrentPage.value, 20)
    const data: PageResult<StockChangeLog> = res.data.data
    stockLogList.value = data.list
    stockLogTotal.value = data.total
  } catch (error) {
    console.error('获取库存变更记录失败', error)
  } finally {
    logsLoading.value = false
  }
}

onMounted(() => {
  fetchStats()
  fetchGames()
})
</script>

<style scoped lang="scss">
.admin-games-page {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 20px;

  h2 {
    margin: 0;
    color: #303133;
  }
}

.stats-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  max-width: 320px;

  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.stat-content {
  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }

  .stat-value {
    font-size: 32px;
    font-weight: bold;
    color: #303133;
    line-height: 1.2;
  }

  .stat-desc {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}

.warning-card {
  .stat-value {
    color: #f56c6c;
  }

  .stat-icon {
    font-size: 48px;
    color: #f56c6c;
    opacity: 0.8;
  }
}

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
}

.search-input {
  flex: 1;
  max-width: 400px;
}

.filter-select {
  width: 140px;
}

.action-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 4px;

  .selected-tip {
    color: #606266;
    font-size: 14px;
    margin-left: auto;
  }
}

.stock-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .stock-input {
    width: 100%;
  }
}

.low-stock {
  color: #f56c6c;
  font-weight: bold;
}

.stock-tag {
  margin-left: 4px;
}

.price {
  color: #f56c6c;
  font-weight: bold;

  .original-price {
    color: #909399;
    text-decoration: line-through;
    font-size: 12px;
    margin-left: 6px;
    font-weight: normal;
  }
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.logs-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.stock-change {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;

  .arrow-icon {
    color: #909399;
    font-size: 12px;
  }

  .stock-before {
    color: #606266;
  }

  .stock-after {
    color: #303133;
    font-weight: bold;
  }

  .diff-plus {
    color: #67c23a;
  }

  .diff-minus {
    color: #f56c6c;
  }
}

:deep(.low-stock-row) {
  --el-table-tr-bg-color: #fef0f0;

  &:hover > td {
    background-color: #fde2e2 !important;
  }
}
</style>
