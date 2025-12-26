---
name: 04-frontend-vue-development
description: 前端 Vue 开发规范，涵盖 Vue 2/3 组件开发、Vuex 状态管理、路由配置、组件通信、样式规范、国际化。当用户进行前端开发、编写 Vue 组件、处理状态管理或实现页面交互时使用。
---

# Skill 04: 前端 Vue 开发

## 概述
BK-CI 前端采用 Vue 2.7 + Vuex + Vue Router 的技术栈，使用蓝鲸 MagicBox（bk-magic-vue）作为 UI 框架。

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Vue | 2.7.16 | 前端框架 |
| Vuex | 3.6.2 | 状态管理 |
| Vue Router | 3.6.5 | 路由管理 |
| bk-magic-vue | 2.5.10 | UI 框架 |
| Webpack | 5 | 打包工具 |
| Sass/SCSS | - | 样式预处理 |

## 前端应用结构

```
src/frontend/
├── devops-pipeline/      # 流水线应用
├── devops-atomstore/     # 研发商店应用
├── devops-manage/        # 管理应用
├── devops-codelib/       # 代码库应用
├── devops-environment/   # 环境管理应用
├── devops-ticket/        # 凭证管理应用
├── devops-quality/       # 质量红线应用
├── bk-pipeline/          # 流水线组件库
├── bk-permission/        # 权限组件库
└── ...                   # 其他应用
```

## ESLint 核心规则

```javascript
// .eslintrc.js 关键配置
{
    'indent': ['error', 4],                    // 4个空格缩进
    'semi': ['error', 'never'],                // 禁用分号
    'comma-dangle': ['error', 'never'],        // 禁用拖尾逗号
    'vue/html-quotes': ['error', 'double'],    // HTML双引号
    'vue/no-v-html': 'error',                  // 禁止v-html（防XSS）
    'no-console': 'error',                     // 禁止console
    'no-var': 'error',                         // 禁用var
    'prefer-const': 'error',                   // 强制const
    'operator-linebreak': ['error', 'before'], // 操作符前置换行
    'object-curly-spacing': ['error', 'always'] // 对象空格
}
```

## 组件命名规范

### 文件命名

使用 `kebab-case.vue`：
- `no-permission.vue`
- `group-table.vue`
- `permission-manage.vue`
- `apply-dialog.vue`

### 目录组织

```
components/
├── children/              # 子组件目录
│   ├── no-permission/
│   ├── permission-manage/
│   └── widget-components/
└── permission-main.vue    # 主组件
```

## 单文件组件结构

**严格顺序**（遵循 `vue/order-in-components` 规则）：

```vue
<template>
    <!-- HTML 模板内容 -->
</template>

<script>
export default {
    // 1. 组件注册
    components: { ... },
    
    // 2. 混入
    mixins: [ ... ],
    
    // 3. Props 定义
    props: { ... },
    
    // 4. 响应式数据
    data() {
        return { ... }
    },
    
    // 5. 计算属性
    computed: { ... },
    
    // 6. 侦听器
    watch: { ... },
    
    // 7. 生命周期钩子
    created() { ... },
    mounted() { ... },
    
    // 8. 方法
    methods: { ... }
}
</script>

<style lang="scss" scoped>
/* 样式定义 */
</style>
```

## Props 定义标准

```javascript
props: {
    // 基本类型
    resourceType: {
        type: String,
        default: ''
    },
    
    // 布尔类型
    showCreateGroup: {
        type: Boolean,
        default: true
    },
    
    // 对象/数组使用工厂函数
    options: {
        type: Array,
        default: () => []
    },
    
    config: {
        type: Object,
        default: () => ({})
    }
}
```

## 方法命名约定

| 类型 | 命名模式 | 示例 |
|------|---------|------|
| 事件处理 | `handle*`, `on*` | `handleViewDetail()`, `handleApply()` |
| 初始化 | `init*` | `initStatus()` |
| 格式化 | `*Formatter` | `statusFormatter()` |
| 样式计算 | `*Class` | `statusIconClass()` |

## API 调用模式

### 通过全局 vue.$ajax 访问

```javascript
// GET 请求
vue.$ajax.get(`${prefix}/user/market/atoms/${atomCode}/yml/detail`)

// POST 请求
vue.$ajax.post(`${prefix}/user/market/desk/store/member/add`, params)

// PUT 请求
vue.$ajax.put(`${prefix}/user/pipeline/atom/baseInfo/atoms/${atomCode}`, data)

// DELETE 请求
vue.$ajax.delete(`${prefix}/user/market/desk/store/member/delete`, { params })
```

### 错误处理

```javascript
Promise.all([
    ajax.get(`${prefix}/hasManagerPermission`),
    ajax.get(`${prefix}/isEnablePermission`)
])
.then(([hasManagerData, isEnableData]) => {
    this.hasPermission = hasManagerData?.data
})
.catch((err) => {
    if ([404, 403, 2119042].includes(err.code)) {
        this.errorCode = err.code
    }
})
.finally(() => {
    this.isLoading = false
})
```

## Vuex Store 模式

### Actions 定义

```javascript
// store/atom.js
export const actions = {
    // 获取数据：get* / list* / query*
    getAtomYaml({ commit }, { atomCode }) {
        return vue.$ajax.get(`${prefix}/user/market/atoms/${atomCode}/yml/detail`)
    },
    
    // 修改数据：modify* / update*
    modifyAtomDetail({ commit }, { atomCode, data }) {
        return vue.$ajax.put(`${prefix}/user/pipeline/atom/baseInfo/atoms/${atomCode}`, data)
    },
    
    // 删除数据
    deleteSensitiveConf({ commit }, { atomCode, id }) {
        return vue.$ajax.delete(`${prefix}/user/market/ATOM/component/${atomCode}/sensitiveConf?ids=${id}`)
    }
}
```

### 在组件中使用

```javascript
import { mapActions } from 'vuex'

export default {
    methods: {
        ...mapActions('atom', [
            'getAtomYaml',
            'modifyAtomDetail'
        ]),
        
        async fetchData() {
            const data = await this.getAtomYaml({ atomCode: this.atomCode })
            this.yamlData = data
        }
    }
}
```

## 样式组织

```scss
<style lang="scss" scoped>
// BEM 命名约定
.permission-wrapper {
    overflow: auto;
    width: 100%;
    height: 100%;
}

.group-table {
    // 嵌套结构
    .status-content {
        display: flex;
        align-items: center;
        
        .status-icon {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            
            // 状态修饰符
            &.status-normal {
                background-color: #30b26f;
            }
            
            &.status-error {
                background-color: #ea3636;
            }
        }
    }
}
</style>
```

## 安全规范

1. **XSS 防护**：禁止使用 `v-html`（ESLint 已配置）
2. **输入验证**：所有用户输入需前端验证
3. **API 调用**：统一使用 axios 拦截器处理认证 token

## 国际化

```javascript
// 使用 i18n
this.$t('common.confirm')
this.$t('pipeline.create')

// 国际化文件位置
src/frontend/locale/
├── store/
│   ├── zh-CN.json
│   └── en-US.json
```

## 组件通信

### Props 向下传递

```vue
<child-component :data="parentData" @update="handleUpdate" />
```

### Events 向上传递

```javascript
// 子组件
this.$emit('update', newValue)

// 父组件
handleUpdate(value) {
    this.parentData = value
}
```

### Vuex 跨组件通信

```javascript
// 触发 mutation
this.$store.commit('SET_DATA', data)

// 触发 action
this.$store.dispatch('fetchData', params)

// 获取 state
this.$store.state.moduleName.data
```
