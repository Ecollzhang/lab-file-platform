<template>
  <div class="breadcrumb-nav mb-16">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item
        v-for="(item, index) in path"
        :key="item.id"
      >
        <span
          :class="{ 'breadcrumb-link': index < path.length - 1 }"
          @click="handleClick(item, index)"
        >
          <el-icon v-if="index === 0" style="vertical-align: middle; margin-right: 4px;">
            <HomeFilled />
          </el-icon>
          {{ item.name }}
        </span>
      </el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<script setup>
import { HomeFilled } from '@element-plus/icons-vue'

const props = defineProps({
  path: {
    type: Array,
    default: () => [{ id: 0, name: '全部文件' }]
  }
})

const emit = defineEmits(['navigate'])

function handleClick(item, index) {
  // Don't navigate if clicking the last (current) item
  if (index < props.path.length - 1) {
    emit('navigate', item)
  }
}
</script>

<style scoped>
.breadcrumb-nav {
  padding: 0;
}

.breadcrumb-link {
  color: #409eff;
  cursor: pointer;
}

.breadcrumb-link:hover {
  text-decoration: underline;
}
</style>
