<template>
  <div class="shared-with-me">
    <div class="page-toolbar">
      <div class="toolbar-left">
        <h3 style="margin: 0">与我共享</h3>
      </div>
      <div class="toolbar-right">
        <el-input
          v-model="shareCodeQuery"
          placeholder="输入分享码查看..."
          style="width: 200px"
          @keyup.enter="goToShareView"
        >
          <template #append>
            <el-button @click="goToShareView">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
    </div>

    <el-card shadow="never">
      <el-empty description="暂无与我共享的文件" />
      <div class="share-hint">
        <p>通过分享码查看共享文件：</p>
        <p>您可以在上方输入别人分享给您的分享码，查看共享的文件。</p>
        <p>或者点击左侧菜单中的"分享链接"直接访问。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const shareCodeQuery = ref('')

function goToShareView() {
  if (shareCodeQuery.value.trim()) {
    router.push(`/share/view/${shareCodeQuery.value.trim()}`)
  }
}
</script>

<style scoped>
.share-hint {
  margin-top: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  text-align: center;
  line-height: 2;
  color: #606266;
}
</style>
