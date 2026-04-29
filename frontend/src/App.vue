<template>
  <router-view />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', Arial, sans-serif;
}

.app-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.app-header .logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.app-header .logo img {
  height: 32px;
  width: 32px;
}

.app-header .header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.app-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.app-sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
  flex-shrink: 0;
}

.app-main {
  flex: 1;
  background: #f5f7fa;
  overflow-y: auto;
  padding: 20px;
}

.app-main .page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

.app-main .page-toolbar .toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-main .page-toolbar .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Element Plus overrides */
.el-menu--vertical {
  border-right: none;
}

.el-upload-dragger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.file-icon {
  font-size: 24px;
  margin-right: 8px;
}

.text-center {
  text-align: center;
}

.mt-8 { margin-top: 8px; }
.mt-16 { margin-top: 16px; }
.mb-8 { margin-bottom: 8px; }
.mb-16 { margin-bottom: 16px; }
.mr-8 { margin-right: 8px; }
.ml-8 { margin-left: 8px; }
</style>
