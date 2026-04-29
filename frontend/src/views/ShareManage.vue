<template>
  <div class="share-manage">
    <div class="page-toolbar">
      <div class="toolbar-left">
        <h3 style="margin: 0">我的分享</h3>
      </div>
      <div class="toolbar-right">
        <el-button @click="loadShares">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="shares" stripe v-loading="loading" empty-text="暂无分享">
        <el-table-column label="分享码" width="130">
          <template #default="{ row }">
            <el-tag>{{ row.shareCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.shareType === 1" type="success">公开</el-tag>
            <el-tag v-else type="warning">私密</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分享链接" min-width="300">
          <template #default="{ row }">
            <div class="share-url-row">
              <span class="share-url-text">{{ row.shareUrl || getShareUrl(row.shareCode) }}</span>
              <el-button
                link
                type="primary"
                size="small"
                @click="copyShareLink(row)"
              >
                复制
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                @click="previewShare(row)"
              >
                预览
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="下载次数" width="100">
          <template #default="{ row }">
            {{ row.downloadCount }}{{ row.maxDownloadCount > 0 ? '/' + row.maxDownloadCount : '' }}
          </template>
        </el-table-column>
        <el-table-column label="过期时间" width="170">
          <template #default="{ row }">
            {{ row.expireTime ? formatDateTime(row.expireTime) : '永不过期' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">有效</el-tag>
            <el-tag v-else type="info" size="small">失效</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定取消此分享？" @confirm="handleCancel(row)">
              <template #reference>
                <el-button
                  v-if="row.status === 1"
                  link
                  type="danger"
                  size="small"
                >
                  取消分享
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserShares, cancelShare, formatDateTime } from '@/utils/api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const shares = ref([])

function getShareUrl(shareCode) {
  return `${window.location.origin}/share/view/${shareCode}`
}

async function loadShares() {
  loading.value = true
  try {
    const res = await getUserShares()
    if (res.code === 200) {
      shares.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取分享列表失败')
    }
  } catch (e) {
    ElMessage.error('获取分享列表失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel(row) {
  try {
    const res = await cancelShare(row.id)
    if (res.code === 200) {
      ElMessage.success('取消分享成功')
      loadShares()
    } else {
      ElMessage.error(res.message || '取消分享失败')
    }
  } catch (e) {
    ElMessage.error('取消分享失败')
  }
}

function copyShareLink(row) {
  const url = row.shareUrl || `${window.location.origin}/share/view/${row.shareCode}`
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('已复制分享链接')
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制')
  })
}

function previewShare(row) {
  router.push(`/share/view/${row.shareCode}`)
}

onMounted(() => {
  loadShares()
})
</script>

<style scoped>
.share-url-row {
  display: flex;
  align-items: center;
  gap: 4px;
}
.share-url-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: #909399;
}
</style>
