---
name: frontend-vue-development
description: 前端 Vue 开发规范，涵盖 Vue 2/3 组件开发、Vuex 状态管理、路由配置、组件通信、样式规范、国际化。当用户进行前端开发、编写 Vue 组件、处理状态管理或实现页面交互时使用。
core_files:
  - "src/frontend/devops-pipeline/"
  - "src/frontend/bk-pipeline/"
related_skills:
  - 01-backend-microservice-development
token_estimate: 3000
---

# 前端 Vue 开发

## Quick Reference

```
技术栈：Vue 2.7 + Vuex 3.6 + Vue Router 3.6 + bk-magic-vue 2.5
文件命名：kebab-case.vue（如 group-table.vue）
缩进：4 空格 | 无分号 | 无拖尾逗号 | HTML 双引号
API 调用：vue.$ajax.get/post/put/delete
```

### 最简示例

```vue
<template>
    <div class="pipeline-list">
        <bk-table :data="pipelines" v-loading="isLoading">
            <bk-table-column prop="name" label="名称"></bk-table-column>
        </bk-table>
    </div>
</template>

<script>
export default {
    data() {
        return {
            pipelines: [],
            isLoading: false
        }
    },
    created() {
        this.fetchPipelines()
    },
    methods: {
        async fetchPipelines() {
            this.isLoading = true
            try {
                const res = await this.$ajax.get('/api/user/pipelines')
                this.pipelines = res.data || []
            } finally {
                this.isLoading = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.pipeline-list {
    padding: 20px;
}
</style>
```

## When to Use

- 开发 Vue 组件
- 管理 Vuex 状态
- 调用后端 API
- 处理页面交互

## When NOT to Use

- 后端 API 开发 → 使用 `01-backend-microservice-development`
- 构建机 Agent → 使用 `05-go-agent-development`

---

## 前端应用结构

```
src/frontend/
├── devops-pipeline/      # 流水线应用
├── devops-atomstore/     # 研发商店应用
├── devops-manage/        # 管理应用
├── bk-pipeline/          # 流水线组件库
└── bk-permission/        # 权限组件库
```

## ESLint 核心规则

```javascript
{
    'indent': ['error', 4],                // 4 空格缩进
    'semi': ['error', 'never'],            // 禁用分号
    'comma-dangle': ['error', 'never'],    // 禁用拖尾逗号
    'vue/html-quotes': ['error', 'double'],// HTML 双引号
    'vue/no-v-html': 'error',              // 禁止 v-html（防 XSS）
    'no-console': 'error'                  // 禁止 console
}
```

## 组件选项顺序

```javascript
export default {
    components: { },  // 1. 组件注册
    mixins: [ ],      // 2. 混入
    props: { },       // 3. Props
    data() { },       // 4. 响应式数据
    computed: { },    // 5. 计算属性
    watch: { },       // 6. 侦听器
    created() { },    // 7. 生命周期钩子
    mounted() { },
    methods: { }      // 8. 方法
}
```

## Props 定义

```javascript
props: {
    resourceType: {
        type: String,
        default: ''
    },
    options: {
        type: Array,
        default: () => []  // 对象/数组使用工厂函数
    }
}
```

## API 调用模式

```javascript
// GET
this.$ajax.get(`${prefix}/user/pipelines`)

// POST
this.$ajax.post(`${prefix}/user/pipelines`, data)

// 错误处理
this.$ajax.get(url)
    .then(res => this.data = res.data)
    .catch(err => {
        if ([404, 403].includes(err.code)) {
            this.errorCode = err.code
        }
    })
    .finally(() => this.isLoading = false)
```

## Vuex 使用

```javascript
// store/pipeline.js
export const actions = {
    getPipelineList({ commit }, { projectId }) {
        return vue.$ajax.get(`/api/user/projects/${projectId}/pipelines`)
    }
}

// 组件中使用
import { mapActions } from 'vuex'

methods: {
    ...mapActions('pipeline', ['getPipelineList']),
    async fetchData() {
        this.list = await this.getPipelineList({ projectId: this.projectId })
    }
}
```

## 方法命名约定

| 类型 | 命名模式 | 示例 |
|------|---------|------|
| 事件处理 | `handle*` | `handleClick()` |
| 初始化 | `init*` | `initData()` |
| 格式化 | `*Formatter` | `statusFormatter()` |

---

## Checklist

开发组件前确认：
- [ ] 文件命名使用 kebab-case
- [ ] 遵循 ESLint 规则（4 空格、无分号）
- [ ] Props 使用完整定义（type + default）
- [ ] 对象/数组 Props 使用工厂函数
- [ ] 禁止使用 v-html
- [ ] 使用 scoped 样式
