<template>
  <div class="friends-page">
    <div class="page-container">
      <h1 class="page-title">好友</h1>

      <el-tabs v-model="activeTab" class="friends-tabs">
        <el-tab-pane label="好友列表" name="friends">
          <div class="friend-list">
            <div v-if="friends.length === 0" class="empty-state">
              <el-empty description="暂无好友，快去添加吧">
                <el-button type="primary" @click="activeTab = 'search'">搜索用户</el-button>
              </el-empty>
            </div>
            <div v-for="friendship in friends" :key="friendship.id" class="friend-item">
              <div class="friend-info">
                <el-avatar :size="48" :src="getAvatarUrl(friendship.friendUser?.avatar)">
                  {{ friendship.friendUser?.nickname?.charAt(0) || friendship.friendUser?.username?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="friend-detail">
                  <div class="friend-name">{{ friendship.friendUser?.nickname || friendship.friendUser?.username }}</div>
                  <div class="friend-status">
                    <span class="status-dot offline"></span>
                    <span class="status-text">离线</span>
                  </div>
                </div>
              </div>
              <div class="friend-actions">
                <el-button size="small" @click="handleViewProfile(friendship.friendId)">查看主页</el-button>
                <el-dropdown @command="(cmd: string) => handleFriendAction(cmd, friendship.friendId!)">
                  <el-button size="small" type="danger" plain>
                    更多 <el-icon><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="delete">删除好友</el-dropdown-item>
                      <el-dropdown-item command="block">拉黑</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="待处理请求" name="pending">
          <div class="pending-list">
            <div v-if="pendingRequests.length === 0" class="empty-state">
              <el-empty description="暂无待处理的好友请求" />
            </div>
            <div v-for="request in pendingRequests" :key="request.id" class="pending-item">
              <div class="friend-info">
                <el-avatar :size="48" :src="getAvatarUrl(request.friendUser?.avatar)">
                  {{ request.friendUser?.nickname?.charAt(0) || request.friendUser?.username?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="friend-detail">
                  <div class="friend-name">{{ request.friendUser?.nickname || request.friendUser?.username }}</div>
                  <div class="request-time">{{ formatTime(request.createdAt) }}</div>
                </div>
              </div>
              <div class="pending-actions">
                <el-button size="small" type="primary" @click="handleAccept(request.friendId!)">接受</el-button>
                <el-button size="small" @click="handleReject(request.friendId!)">拒绝</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="搜索用户" name="search">
          <div class="search-section">
            <el-input
              v-model="searchKeyword"
              placeholder="输入用户名或昵称搜索"
              size="large"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
          </div>

          <div v-if="searchResults.length > 0" class="search-results">
            <h3>搜索结果</h3>
            <div v-for="user in searchResults" :key="user.id" class="search-item">
              <div class="friend-info">
                <el-avatar :size="48" :src="getAvatarUrl(user.avatar)">
                  {{ user.nickname?.charAt(0) || user.username?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="friend-detail">
                  <div class="friend-name">{{ user.nickname || user.username }}</div>
                  <div class="user-username">@{{ user.username }}</div>
                </div>
              </div>
              <div class="search-actions">
                <el-button
                  v-if="getFriendshipStatus(user.id!) === 'none'"
                  size="small"
                  type="primary"
                  @click="handleSendRequest(user.id!)"
                >
                  加好友
                </el-button>
                <el-button
                  v-else-if="getFriendshipStatus(user.id!) === 'pending'"
                  size="small"
                  disabled
                >
                  已发送请求
                </el-button>
                <el-button
                  v-else-if="getFriendshipStatus(user.id!) === 'accepted'"
                  size="small"
                  type="success"
                  disabled
                >
                  已是好友
                </el-button>
                <el-button
                  v-else-if="getFriendshipStatus(user.id!) === 'blocked'"
                  size="small"
                  type="danger"
                  disabled
                >
                  已拉黑
                </el-button>
              </div>
            </div>

            <div v-if="hasMoreSearchResults" class="load-more">
              <el-button @click="loadMoreSearchResults">加载更多</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="黑名单" name="blocked">
          <div class="blocked-list">
            <div v-if="blockedUsers.length === 0" class="empty-state">
              <el-empty description="暂无拉黑用户" />
            </div>
            <div v-for="blocked in blockedUsers" :key="blocked.id" class="blocked-item">
              <div class="friend-info">
                <el-avatar :size="48" :src="getAvatarUrl(blocked.friendUser?.avatar)">
                  {{ blocked.friendUser?.nickname?.charAt(0) || blocked.friendUser?.username?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="friend-detail">
                  <div class="friend-name">{{ blocked.friendUser?.nickname || blocked.friendUser?.username }}</div>
                  <div class="blocked-time">拉黑时间：{{ formatTime(blocked.updatedAt) }}</div>
                </div>
              </div>
              <div class="blocked-actions">
                <el-button size="small" type="primary" @click="handleUnblock(blocked.friendId!)">解除拉黑</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowDown } from '@element-plus/icons-vue'
import { friendApi } from '@/api'
import type { Friendship, User, PageResult } from '@/types'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('friends')
const friends = ref<Friendship[]>([])
const pendingRequests = ref<Friendship[]>([])
const blockedUsers = ref<Friendship[]>([])

const searchKeyword = ref('')
const searchResults = ref<User[]>([])
const searchPage = ref(1)
const searchTotal = ref(0)
const hasMoreSearchResults = computed(() => searchResults.value.length < searchTotal.value)

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

function getFriendshipStatus(userId: number): string {
  const friendIds = friends.value.map(f => f.friendId)
  if (friendIds.includes(userId)) return 'accepted'

  const pendingIds = pendingRequests.value.map(r => r.friendId)
  if (pendingIds.includes(userId)) return 'pending'

  const blockedIds = blockedUsers.value.map(b => b.friendId)
  if (blockedIds.includes(userId)) return 'blocked'

  return 'none'
}

async function loadFriends() {
  try {
    const res = await friendApi.getFriends()
    friends.value = res.data.data
  } catch (e) {
    console.error('加载好友列表失败', e)
  }
}

async function loadPendingRequests() {
  try {
    const res = await friendApi.getPendingRequests()
    pendingRequests.value = res.data.data
  } catch (e) {
    console.error('加载待处理请求失败', e)
  }
}

async function loadBlockedUsers() {
  try {
    const res = await friendApi.getBlockedUsers()
    blockedUsers.value = res.data.data
  } catch (e) {
    console.error('加载黑名单失败', e)
  }
}

async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  searchPage.value = 1
  searchResults.value = []
  await doSearch()
}

async function doSearch() {
  try {
    const res = await friendApi.searchUsers(searchKeyword.value.trim(), searchPage.value, 10)
    const data: PageResult<User> = res.data.data
    if (searchPage.value === 1) {
      searchResults.value = data.list
    } else {
      searchResults.value = [...searchResults.value, ...data.list]
    }
    searchTotal.value = data.total
  } catch (e) {
    console.error('搜索用户失败', e)
  }
}

async function loadMoreSearchResults() {
  searchPage.value++
  await doSearch()
}

async function handleSendRequest(friendId: number) {
  try {
    await friendApi.sendRequest(friendId)
    ElMessage.success('好友请求已发送')
    await loadFriends()
    await loadPendingRequests()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '发送失败')
  }
}

async function handleAccept(friendId: number) {
  try {
    await friendApi.acceptRequest(friendId)
    ElMessage.success('已接受好友请求')
    await loadFriends()
    await loadPendingRequests()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function handleReject(friendId: number) {
  try {
    await ElMessageBox.confirm('确定拒绝该好友请求吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await friendApi.rejectRequest(friendId)
    ElMessage.success('已拒绝好友请求')
    await loadPendingRequests()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
}

async function handleDeleteFriend(friendId: number) {
  try {
    await ElMessageBox.confirm('确定要删除该好友吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await friendApi.deleteFriend(friendId)
    ElMessage.success('已删除好友')
    await loadFriends()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
}

async function handleBlock(friendId: number) {
  try {
    await ElMessageBox.confirm('确定要拉黑该用户吗？拉黑后将无法收到对方的好友请求。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await friendApi.blockUser(friendId)
    ElMessage.success('已拉黑用户')
    await loadFriends()
    await loadBlockedUsers()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
}

async function handleUnblock(friendId: number) {
  try {
    await friendApi.unblockUser(friendId)
    ElMessage.success('已解除拉黑')
    await loadBlockedUsers()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

function handleViewProfile(userId: number) {
  router.push(`/profile?userId=${userId}`)
}

function handleFriendAction(command: string, friendId: number) {
  switch (command) {
    case 'delete':
      handleDeleteFriend(friendId)
      break
    case 'block':
      handleBlock(friendId)
      break
  }
}

onMounted(() => {
  loadFriends()
  loadPendingRequests()
  loadBlockedUsers()
})
</script>

<style lang="scss" scoped>
.friends-page {
  min-height: calc(100vh - 64px);
  background: var(--steam-bg);
  padding: 24px 0;
}

.page-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-title {
  font-size: 28px;
  color: var(--text-white);
  margin-bottom: 24px;
  font-weight: 600;
}

.friends-tabs {
  background: var(--steam-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-md);

  :deep(.el-tabs__header) {
    margin-bottom: 24px;
    border-bottom-color: var(--border-color);
  }

  :deep(.el-tabs__item) {
    color: var(--text-secondary);
    font-size: 16px;

    &.is-active {
      color: var(--steam-blue);
    }
  }
}

.friend-list,
.pending-list,
.search-results,
.blocked-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.friend-item,
.pending-item,
.search-item,
.blocked-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  transition: all 0.3s;

  &:hover {
    background: rgba(102, 192, 244, 0.1);
  }
}

.friend-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.friend-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.friend-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-white);
}

.friend-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.offline {
    background: #666;
  }

  &.online {
    background: #67c23a;
  }
}

.status-text {
  font-size: 12px;
}

.request-time,
.blocked-time,
.user-username {
  font-size: 13px;
  color: var(--text-secondary);
}

.friend-actions,
.pending-actions,
.search-actions,
.blocked-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-state {
  padding: 60px 0;
}

.search-section {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;

  .el-input {
    flex: 1;
  }
}

.search-results h3 {
  font-size: 18px;
  color: var(--text-white);
  margin-bottom: 16px;
}

.load-more {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
