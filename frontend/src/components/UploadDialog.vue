<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="上传文件"
    width="550px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-upload
      ref="uploadRef"
      drag
      multiple
      :auto-upload="false"
      :show-file-list="true"
      :file-list="fileList"
      :on-change="handleFileChange"
      :on-remove="handleFileRemove"
      list-type="text"
    >
      <el-icon class="el-icon--upload" :size="48"><UploadFilled /></el-icon>
      <div class="el-upload__text">
        将文件拖到此处，或<em>点击选择文件</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          单个文件超过 10MB 将自动使用分片上传
        </div>
      </template>
    </el-upload>

    <div v-if="chunkUploads.length > 0" class="mt-16">
      <div
        v-for="cu in chunkUploads"
        :key="cu.name"
        class="chunk-progress-item"
      >
        <div class="chunk-info">
          <span class="chunk-name">{{ cu.name }}</span>
          <span class="chunk-status">{{ cu.statusText }}</span>
        </div>
        <el-progress
          :percentage="cu.progress"
          :status="cu.progress >= 100 ? 'success' : undefined"
        />
        <div v-if="cu.error" class="chunk-error">{{ cu.error }}</div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="uploading" @click="startUpload">
        {{ uploading ? '上传中...' : '开始上传' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import {
  uploadFile, initChunkUpload, uploadChunk,
  mergeChunks, checkFileExists, cancelChunkUpload
} from '@/utils/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import SparkMD5 from 'spark-md5'

const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB per chunk

const props = defineProps({
  visible: Boolean,
  parentId: { type: Number, default: 0 }
})

const emit = defineEmits(['update:visible', 'success'])

const userStore = useUserStore()

const uploadRef = ref(null)
const uploading = ref(false)
const fileList = ref([])
const selectedFiles = ref([])
const chunkUploads = reactive([])

function handleFileChange(file) {
  selectedFiles.value.push(file.raw)
}

function handleFileRemove(file) {
  const idx = selectedFiles.value.findIndex(f => {
    return f.name === file.name && f.size === file.size
  })
  if (idx >= 0) {
    selectedFiles.value.splice(idx, 1)
  }
}

function handleClose() {
  // Cancel any in-progress chunk uploads
  for (const cu of chunkUploads) {
    if (cu.uploadId && cu.progress < 100) {
      cancelChunkUpload(cu.md5, cu.uploadId).catch(() => {})
    }
  }
  chunkUploads.length = 0
  selectedFiles.value = []
  fileList.value = []
  uploading.value = false
  emit('update:visible', false)
}

async function startUpload() {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  uploading.value = true

  for (const file of selectedFiles.value) {
    if (file.size <= CHUNK_SIZE) {
      // Direct upload for small files
      await doDirectUpload(file)
    } else {
      // Chunk upload for large files
      await doChunkUpload(file)
    }
  }

  uploading.value = false
  ElMessage.success('所有文件上传完成')
  emit('success')
  emit('update:visible', false)
}

async function doDirectUpload(file) {
  try {
    const res = await uploadFile(file, props.parentId)
    if (res.code !== 200) {
      ElMessage.error(`${file.name} 上传失败：${res.message}`)
    }
  } catch (e) {
    ElMessage.error(`${file.name} 上传失败`)
  }
}

async function doChunkUpload(file) {
  const cu = reactive({
    name: file.name,
    progress: 0,
    statusText: '计算MD5...',
    md5: '',
    uploadId: '',
    error: ''
  })
  chunkUploads.push(cu)

  try {
    // Step 1: Compute MD5
    const md5 = await computeMD5(file)
    cu.md5 = md5
    cu.statusText = 'MD5: ' + md5.substring(0, 8) + '...'

    // Step 2: Check if file already exists (instant upload)
    const existRes = await checkFileExists(md5)
    if (existRes.code === 200 && existRes.data) {
      cu.statusText = '文件已存在（秒传）'
      cu.progress = 100
      return
    }

    // Step 3: Init chunk upload
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
    const initRes = await initChunkUpload({
      fileName: file.name,
      md5: md5,
      totalChunks: totalChunks
    })
    if (initRes.code !== 200) {
      cu.error = '初始化上传失败：' + (initRes.message || '')
      return
    }
    cu.uploadId = initRes.data
    cu.statusText = '上传中...'

    // Step 4: Upload each chunk
    for (let i = 1; i <= totalChunks; i++) {
      const start = (i - 1) * CHUNK_SIZE
      const end = Math.min(start + CHUNK_SIZE, file.size)
      const chunk = file.slice(start, end)

      let retries = 3
      let success = false
      while (retries > 0 && !success) {
        try {
          await uploadChunk(md5, cu.uploadId, i, totalChunks, chunk)
          success = true
        } catch (e) {
          retries--
          if (retries === 0) throw e
          await new Promise(r => setTimeout(r, 1000))
        }
      }

      cu.progress = Math.round((i / totalChunks) * 100)
    }

    // Step 5: Merge chunks
    cu.statusText = '合并中...'
    const mergeRes = await mergeChunks(md5, cu.uploadId, file.name, props.parentId)
    if (mergeRes.code === 200) {
      cu.statusText = '上传完成'
      cu.progress = 100
    } else {
      cu.error = '合并失败：' + (mergeRes.message || '')
    }

  } catch (e) {
    cu.error = '上传失败：' + (e.message || '未知错误')
    cu.statusText = '失败'
  }
}

function computeMD5(file) {
  return new Promise((resolve, reject) => {
    const blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice
    const chunkSize = 2 * 1024 * 1024 // 2MB chunks for md5 computation
    const chunks = Math.ceil(file.size / chunkSize)
    const spark = new SparkMD5.ArrayBuffer()
    const reader = new FileReader()
    let currentChunk = 0

    reader.onload = function(e) {
      spark.append(e.target.result)
      currentChunk++

      if (currentChunk < chunks) {
        loadNext()
      } else {
        resolve(spark.end())
      }
    }

    reader.onerror = function() {
      reject(new Error('读取文件失败'))
    }

    function loadNext() {
      const start = currentChunk * chunkSize
      const end = Math.min(start + chunkSize, file.size)
      reader.readAsArrayBuffer(blobSlice.call(file, start, end))
    }

    loadNext()
  })
}
</script>

<style scoped>
.chunk-progress-item {
  margin-bottom: 12px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

.chunk-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 13px;
}

.chunk-name {
  color: #303133;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chunk-status {
  color: #909399;
}

.chunk-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}
</style>
