import request from './request'

// ========== User API ==========
export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function getUserById(id) {
  return request.get(`/user/${id}`)
}

export function updateUserInfo(id, data) {
  return request.put(`/user/${id}`, data)
}

export function logout() {
  return request.post('/user/logout')
}

// ========== File API ==========
export function uploadFile(file, parentId = 0, description = '') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('parentId', parentId)
  if (description) formData.append('description', description)
  return request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  })
}

export function createFolder(folderName, parentId = 0) {
  return request.post('/file/folder', null, {
    params: { folderName, parentId }
  })
}

export function getFileList(userId, parentId = 0) {
  return request.get('/file/list', { params: { userId, parentId } })
}

export function deleteFile(fileId) {
  return request.delete(`/file/${fileId}`)
}

export function renameFile(fileId, newName) {
  return request.put(`/file/${fileId}/rename`, null, {
    params: { newName }
  })
}

export function moveFile(fileId, newParentId) {
  return request.put(`/file/${fileId}/move`, null, {
    params: { newParentId }
  })
}

export function downloadFile(fileId) {
  return request({
    url: `/file/${fileId}/download`,
    method: 'GET',
    responseType: 'blob'
  })
}

export function getPreviewUrl(fileId) {
  const stored = localStorage.getItem('lab-file-user')
  let token = ''
  if (stored) {
    try { token = JSON.parse(stored).token } catch (e) {}
  }
  return `/api/file/${fileId}/preview?token=${token}`
}

// ========== Chunk Upload API ==========
export function initChunkUpload(data) {
  return request.post('/file/chunk/init', data)
}

export function uploadChunk(md5, uploadId, partNumber, totalChunks, chunk) {
  const formData = new FormData()
  formData.append('md5', md5)
  formData.append('uploadId', uploadId)
  formData.append('partNumber', partNumber)
  formData.append('totalChunks', totalChunks)
  formData.append('chunk', chunk)
  return request.post('/file/chunk/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  })
}

export function getUploadedChunks(md5, uploadId) {
  return request.get('/file/chunk/uploaded', { params: { md5, uploadId } })
}

export function getMissingChunks(md5, uploadId, totalChunks) {
  return request.get('/file/chunk/missing-chunks', { params: { md5, uploadId, totalChunks } })
}

export function mergeChunks(md5, uploadId, fileName, parentId = 0, description = '') {
  return request.post('/file/chunk/merge', null, {
    params: { md5, uploadId, fileName, parentId, description }
  })
}

export function cancelChunkUpload(md5, uploadId) {
  return request.post('/file/chunk/cancel', null, {
    params: { md5, uploadId }
  })
}

export function checkFileExists(md5) {
  return request.get('/file/chunk/exists', { params: { md5 } })
}

// ========== Share API ==========
export function getAllUsers() {
  return request.get('/file/share/users')
}

export function createShare(params) {
  return request.post('/file/share/create', null, { params })
}

export function getUserShares() {
  return request.get('/file/share/list')
}

export function getShareInfo(shareCode) {
  return request.get(`/file/share/${shareCode}`)
}

export function getShareTargetUsers(shareCode) {
  return request.get(`/file/share/${shareCode}/target-users`)
}

export function shareDownload(shareCode, fileId) {
  return request({
    url: `/file/share/${shareCode}/download`,
    method: 'GET',
    params: fileId ? { fileId } : {},
    responseType: 'blob'
  })
}

export function getSharePreviewUrl(shareCode, fileId) {
  const stored = localStorage.getItem('lab-file-user')
  let token = ''
  if (stored) {
    try { token = JSON.parse(stored).token } catch (e) {}
  }
  let url = `/api/file/share/${shareCode}/preview?token=${token}`
  if (fileId) url += `&fileId=${fileId}`
  return url
}

export function getShareFileList(shareCode, parentId = 0) {
  return request.get(`/file/share/${shareCode}/list`, { params: { parentId } })
}

export function shareUpload(shareCode, file, parentId = 0, description = '') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('parentId', parentId)
  if (description) formData.append('description', description)
  return request.post(`/file/share/${shareCode}/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  })
}

export function cancelShare(shareId) {
  return request.delete(`/share/${shareId}`)
}

// ========== Utility ==========
export function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

export function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function getFileIcon(extension, isDirectory) {
  if (isDirectory) return 'folder'
  const extMap = {
    'pdf': 'document',
    'doc': 'document',
    'docx': 'document',
    'xls': 'document',
    'xlsx': 'document',
    'ppt': 'document',
    'pptx': 'document',
    'txt': 'document',
    'md': 'document',
    'jpg': 'picture',
    'jpeg': 'picture',
    'png': 'picture',
    'gif': 'picture',
    'bmp': 'picture',
    'svg': 'picture',
    'webp': 'picture',
    'mp4': 'video-camera',
    'avi': 'video-camera',
    'mov': 'video-camera',
    'wmv': 'video-camera',
    'mkv': 'video-camera',
    'flv': 'video-camera',
    'mp3': 'headset',
    'wav': 'headset',
    'wma': 'headset',
    'flac': 'headset',
    'aac': 'headset',
    'zip': 'folder-opened',
    'rar': 'folder-opened',
    '7z': 'folder-opened',
    'tar': 'folder-opened',
    'gz': 'folder-opened',
    'exe': 'tools',
    'apk': 'tools',
    'iso': 'disc',
    'js': 'code',
    'ts': 'code',
    'py': 'code',
    'java': 'code',
    'cpp': 'code',
    'c': 'code',
    'html': 'code',
    'css': 'code',
    'json': 'code',
    'xml': 'code',
    'sql': 'code',
    'sh': 'code',
    'bat': 'code',
  }
  const ext = extension ? extension.toLowerCase() : ''
  return extMap[ext] || 'question-filled'
}

export function canPreview(extension) {
  if (!extension) return false
  const previewExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp',
    'txt', 'md', 'log', 'json', 'xml', 'yaml', 'yml', 'ini', 'cfg', 'conf',
    'js', 'ts', 'py', 'java', 'cpp', 'c', 'h', 'html', 'css',
    'pdf', 'mp4', 'avi', 'mov', 'wmv', 'mkv', 'flv',
    'mp3', 'wav', 'flac', 'aac', 'wma']
  return previewExts.includes(extension.toLowerCase())
}

export const ROLE_MAP = {
  1: { label: '管理员', type: 'danger' },
  2: { label: '导师', type: 'warning' },
  3: { label: '研究生', type: 'primary' }
}

// ========== Authenticated Blob Fetch (for preview/download) ==========
function getAuthHeaders() {
  const stored = localStorage.getItem('lab-file-user')
  if (stored) {
    try {
      const parsed = JSON.parse(stored)
      if (parsed.token) {
        return { 'Authorization': `Bearer ${parsed.token}` }
      }
    } catch (e) {}
  }
  return {}
}

export async function fetchPreviewBlob(fileId, shareCode) {
  let url = shareCode
    ? `/api/share/${shareCode}/preview?fileId=${fileId}`
    : `/api/file/${fileId}/preview`
  const response = await fetch(url, {
    headers: getAuthHeaders()
  })
  if (!response.ok) throw new Error('预览加载失败')
  return response.blob()
}

export async function fetchDownloadBlob(url, filename) {
  const response = await fetch(url, {
    headers: getAuthHeaders()
  })
  if (!response.ok) throw new Error('下载失败')
  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = objectUrl
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(objectUrl), 60000)
}
