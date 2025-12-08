# Royuki Cloud OJ 前端

基于 Vue3 + Element Plus + Monaco Editor 的在线编程实训平台前端项目。

## 技术栈

- ⚡️ **Vite** - 极速构建工具
- 🖖 **Vue 3** - 渐进式JavaScript框架
- 💪 **TypeScript** - 类型安全
- 🎨 **Element Plus** - Vue 3组件库
- 📝 **Monaco Editor** - VS Code同款代码编辑器
- 🛣️ **Vue Router** - 官方路由管理器
- 📦 **Pinia** - 状态管理
- 🔗 **Axios** - HTTP客户端

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API接口
│   │   └── judge.ts      # 评测服务接口
│   ├── components/       # 公共组件
│   │   └── MonacoEditor.vue  # 代码编辑器组件
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── types/            # 类型定义
│   │   └── index.ts
│   ├── utils/            # 工具函数
│   │   └── request.ts    # Axios封装
│   ├── views/            # 页面组件
│   │   ├── JudgePage.vue          # 代码评测页面
│   │   └── SubmissionHistory.vue  # 提交历史页面
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## 快速开始

### 1. 安装依赖

```bash
cd frontend
npm install
```

或使用 pnpm（推荐）：

```bash
pnpm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

### 4. 预览生产构建

```bash
npm run preview
```

## 功能特性

### 🎯 代码评测
- 支持多语言（Java、C、C++、Python）
- VS Code级别的代码编辑体验
- 实时评测反馈
- 评测结果可视化展示

### 📊 提交历史
- 分页查询提交记录
- 查看提交代码
- 重新评测功能
- 评测状态实时展示

### 🎨 用户界面
- 现代化UI设计
- 响应式布局
- 暗色主题代码编辑器
- Element Plus组件库

## API对接

### 基础URL
- 开发环境：`http://localhost:8080/api`
- 生产环境：`/api`（通过网关代理）

### 核心接口

#### 1. 提交代码
```
POST /judge/submit
```

#### 2. 查询评测结果
```
GET /judge/result/{submissionId}
```

#### 3. 查询提交记录
```
GET /judge/submission/{submissionId}
```

#### 4. 分页查询用户提交
```
GET /judge/submissions?userId={userId}&page={page}&size={size}
```

#### 5. 重新评测
```
POST /judge/rejudge/{submissionId}
```

## 配置说明

### Vite代理配置

在 `vite.config.ts` 中配置了API代理：

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

### Element Plus按需导入

使用 `unplugin-vue-components` 和 `unplugin-auto-import` 实现Element Plus组件按需导入：

```typescript
AutoImport({
  resolvers: [ElementPlusResolver()],
}),
Components({
  resolvers: [ElementPlusResolver()],
}),
```

### Monaco Editor配置

Monaco Editor通过Web Worker实现语法高亮和代码提示：

```typescript
self.MonacoEnvironment = {
  getWorker(_, label) {
    // Worker配置
  },
}
```

## 开发指南

### 添加新页面

1. 在 `src/views/` 中创建页面组件
2. 在 `src/router/index.ts` 中添加路由配置
3. 在 `App.vue` 的菜单中添加导航项

### 添加新API

1. 在 `src/types/index.ts` 中定义类型
2. 在 `src/api/` 中添加API函数
3. 在组件中使用API

### 代码规范

- 使用TypeScript编写代码
- 使用Composition API（`<script setup>`）
- 使用Element Plus组件
- 遵循Vue 3最佳实践

## 常见问题

### 1. 依赖安装失败

如果npm install失败，尝试：

```bash
# 清除缓存
npm cache clean --force

# 使用国内镜像
npm install --registry=https://registry.npmmirror.com

# 或使用 pnpm
pnpm install
```

### 2. 启动失败

确保：
- Node.js 版本 >= 16
- 后端服务已启动（端口8080）
- 没有端口冲突（3000）

### 3. Monaco Editor加载慢

Monaco Editor首次加载可能较慢，这是正常现象。可以考虑：
- 使用CDN加速
- 预加载Worker文件
- 懒加载编辑器组件

## 浏览器支持

- Chrome/Edge >= 90
- Firefox >= 88
- Safari >= 14

## License

MIT

## 相关文档

- [Vue 3官方文档](https://cn.vuejs.org/)
- [Element Plus文档](https://element-plus.org/)
- [Monaco Editor文档](https://microsoft.github.io/monaco-editor/)
- [Vite官方文档](https://cn.vitejs.dev/)
