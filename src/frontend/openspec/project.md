# Project Context

## Purpose
BK-CI (蓝鲸持续集成平台) is an open-source CI/CD platform developed by Tencent. This frontend workspace contains all the web applications that make up the BK-CI user interface, including pipeline management, code repository integration, environment management, quality gates, artifact store, and administration consoles.

The project serves enterprise-grade CI/CD workflows with features like:
- Visual pipeline editor and execution monitoring
- Multi-project and permission management
- Integration with various code repositories and build environments
- Quality gates and metrics tracking
- Plugin/atom marketplace

## Tech Stack

### Core Frameworks
- **Vue 2** (~2.7.16) - Majority of packages (devops-pipeline, devops-nav, devops-codelib, etc.)
- **Vue 3** (~3.4.31 / ~3.5.13) - Newer packages (devops-manage, devops-platform, devops-metrics)
- **Vue Router** - v3.x for Vue 2, v4.x for Vue 3 packages
- **Vuex** (3.6.2) - State management for Vue 2 packages
- **Pinia** (^2.x) - State management for Vue 3 packages

### Build Tools
- **pnpm** (>=9) - Package manager with workspaces
- **Nx** (19.5.1) - Monorepo build orchestration and caching
- **Webpack 5** (~5.76.1) - Primary bundler for most packages
- **Vite** (^6.x) - Bundler for devops-platform and devops-metrics
- **Gulp** (^4.x) - Build orchestration, asset bundling, SVG sprite generation
- **Babel 7** - JavaScript transpilation

### UI & Styling
- **bk-magic-vue** (2.5.x) - BlueKing UI component library for Vue 2
- **bkui-vue** (2.0.2-beta.x) - BlueKing UI component library for Vue 3
- **SCSS/SASS** - Primary styling preprocessor
- **PostCSS** - Used in Vue 3 packages with various plugins
- **Tailwind CSS** - Used in devops-platform

### Languages
- **JavaScript** (ES2020) - Primary language
- **TypeScript** - Partial adoption (devops-nav, devops-manage, devops-platform, devops-metrics)

### Internationalization
- **vue-i18n** - v8.x for Vue 2, v9.x for Vue 3
- Supported locales: `zh-CN`, `en-US`, `ja-JP`
- Locale files stored in `/locale/[service]/*.json`

### Key Dependencies
- **axios** (1.12.2) - HTTP client
- **Monaco Editor** (^0.40.0) - Code editor in pipeline
- **xterm** (^4.15.0) - Terminal emulator
- **echarts/vue-echarts** - Charts and visualization
- **dayjs** - Date manipulation

## Project Conventions

### Code Style

**Indentation & Formatting:**
- 4 spaces indentation (both JS and Vue templates)
- No semicolons at end of statements
- Single quotes for strings
- No trailing commas in objects/arrays

**Naming Conventions:**
- Components: PascalCase filenames (e.g., `PipelineList.vue`)
- Props: camelCase (e.g., `isLoading`, `pipelineId`)
- CSS classes: kebab-case (e.g., `pipeline-list-container`)
- Backend fields may not be camelCase (destructuring ignores this rule)

**Vue API Preferences:**
- **Prefer Composition API** over Options API for both Vue 2 and Vue 3
- **Before modifying Vue 2 components**, refactor from Options API to Composition API first
- **Avoid mixins** - use composable hooks (`use*.js`) for shared logic instead
- Vue 2 supports Composition API via `@vue/composition-api` or Vue 2.7's built-in support

**Composable Hook Pattern:**
```javascript
// src/hooks/usePipelineList.js
import { ref, computed, onMounted } from 'vue'

export function usePipelineList() {
    const pipelines = ref([])
    const loading = ref(false)
    
    const fetchPipelines = async () => {
        loading.value = true
        // ... fetch logic
        loading.value = false
    }
    
    onMounted(fetchPipelines)
    
    return { pipelines, loading, fetchPipelines }
}
```

**Legacy Vue Component Order (for reference only):**
```javascript
['el', 'name', 'parent', 'functional', 'components', 'directives', 
 'filters', 'extends', 'mixins', 'inheritAttrs', 'model', 'props', 
 'propsData', 'data', 'computed', 'watch', 'LIFECYCLE_HOOKS', 
 'methods', 'template', 'render', 'renderError']
```

**Key ESLint Rules:**
- `prefer-const` - Use const when variable won't be reassigned
- `no-var` - Disallow var, use let/const
- `vue/max-attributes-per-line` - One attribute per line for multiline elements
- `vue/html-indent` - 4 spaces in templates
- `vue/script-indent` - 4 spaces with baseIndent of 1

### Architecture Patterns

**Monorepo Structure:**
```
/
├── devops-* /          # Service packages (pipeline, nav, codelib, etc.)
├── bk-pipeline/        # Shared pipeline visualization component
├── bk-permission/      # Shared permission component
├── common-lib/         # Shared utilities and components
├── locale/             # Internationalization files
├── svg-sprites/        # SVG icons
├── frontend/           # Build output directory
└── webpackPlugin/      # Custom webpack plugins
```

**Service Package Structure:**
```
devops-[service]/
├── src/
│   ├── components/     # Vue components
│   ├── views/          # Page-level components
│   ├── store/          # Vuex/Pinia modules
│   ├── router/         # Route definitions
│   ├── utils/          # Utility functions
│   ├── scss/           # Stylesheets
│   └── images/         # Static assets
├── package.json        # With nx targets configuration
└── webpack.config.js   # Or vite.config.js
```

**State Management:**
- Vue 2 packages: Vuex with modular stores
- Vue 3 packages: Pinia with Composition API

**Shared Logic Pattern:**
- ❌ **Do NOT use mixins** - they cause naming conflicts and unclear data flow
- ✅ **Use composable hooks** (`src/hooks/use*.js`) for reusable logic
- When encountering existing mixins, refactor to hooks before extending functionality

**API Layer:**
- Axios with centralized configuration
- Request interceptors for auth headers
- Accept-Language header for i18n

### Testing Strategy
- Limited test coverage currently
- Jest configured in devops-repo
- Focus on integration testing via manual QA

### Git Workflow

**Commit Convention:**
Uses conventional commits with custom types and issue linking:
```
<type>: <subject> (issue #<issueId>)
```

**Allowed Types:**
`feature`, `feat`, `bug`, `fix`, `bugfix`, `refactor`, `perf`, `test`, `docs`, `info`, `format`, `merge`, `depend`, `chore`, `del`

**Example:**
```
feat: add pipeline template support (issue #1234)
fix: resolve memory leak in log viewer (issue #5678)
```

**Pre-commit Hooks:**
- Husky v9 + lint-staged
- Auto-fix ESLint issues on staged `.js`, `.ts`, `.tsx`, `.vue` files

## Domain Context

**Key Domain Concepts:**
- **Pipeline**: A CI/CD workflow definition with stages, jobs, and tasks
- **Stage**: A phase in a pipeline (e.g., build, test, deploy)
- **Job**: A unit of work within a stage, runs on an agent
- **Task/Atom**: Individual build steps (plugins) within a job
- **Project**: Top-level container for pipelines and resources
- **Code Repository (Codelib)**: Source code connections (Git, SVN, etc.)
- **Environment**: Build agent pools and deployment targets
- **Quality Gate**: Automated quality checks and thresholds
- **Atom Store**: Marketplace for pipeline plugins

**User Roles:**
- Project Admin, Developer, Viewer
- System Admin (platform-level management)

## Important Constraints

**Vue Development Rules:**
- **Always use Composition API** - even in Vue 2 projects (supported in Vue 2.7+)
- **Refactor before extending** - convert Options API to Composition API before adding features
- **No new mixins** - existing mixins should be migrated to composable hooks
- **Hooks location** - place composables in `src/hooks/` or `src/hook/` directory

**Browser Support:**
- Modern browsers (Chrome, Firefox, Safari, Edge)
- No IE11 support

**Node.js Version:**
- Node.js >= 20.17.0 (enforced in package.json engines)

**Package Manager:**
- pnpm >= 9 (enforced via preinstall script)
- Must use `pnpm` exclusively (`npx only-allow pnpm`)

**Vue Version Coexistence:**
- Vue 2 and Vue 3 packages coexist in the monorepo
- Cannot share components directly between Vue 2/3 packages
- Shared logic should be framework-agnostic where possible

**Build Environments:**
- `dev` - Development
- `test` - Testing/Staging
- `master` - Production
- `external` - External/OSS distribution

## External Dependencies

**BlueKing Ecosystem:**
- BlueKing PaaS platform integration
- BlueKing login modal (`@blueking/login-modal`)
- BlueKing platform config (`@blueking/platform-config`)
- Icon library (`@icon-cool/bk-icon-devops`)

**Backend Services:**
- REST APIs under `/ms/[service]/api/`
- WebSocket connections for real-time updates (STOMP over SockJS)
- Static assets served from `__BK_CI_PUBLIC_PATH__` prefix

**Global Variables (window):**
- `INIT_LOCALE` - Initial locale setting
- `PUBLIC_URL_PREFIX` - CDN/asset URL prefix
- `BK_PAAS_PRIVATE_URL` - PaaS platform URL
- `DOCS_URL_PREFIX`, `API_URL_PREFIX`, `LOGIN_SERVICE_URL`, etc.

---

## Team Skills & Rules

This section defines patterns and rules that AI assistants and team members must follow. These are derived from actual codebase patterns and team preferences.

### 2. Composable/Hook Patterns

**Standard Hook Structure:**
```javascript
// src/hooks/useXxx.js or src/hook/useXxx.js
import { ref, computed, onMounted } from 'vue'

export default function useXxx() {
    // 1. Reactive state
    const isLoading = ref(false)
    const data = ref([])
    
    // 2. Computed properties
    const isEmpty = computed(() => data.value.length === 0)
    
    // 3. Methods
    const fetchData = async () => {
        isLoading.value = true
        try {
            data.value = await api.getData()
        } catch (error) {
            // Handle error
        } finally {
            isLoading.value = false
        }
    }
    
    // 4. Lifecycle (optional)
    onMounted(fetchData)
    
    // 5. Return all needed refs and methods
    return {
        isLoading,
        data,
        isEmpty,
        fetchData
    }
}
```

**Instance Helper Hook (Vue 2 bridge pattern):**
```javascript
// src/hook/useInstance.js - Access Vue 2 instance in Composition API
import { getCurrentInstance } from 'vue'
import { useI18n } from 'vue-i18n-bridge'

export default function useInstance() {
    const vm = getCurrentInstance()
    const proxy = vm.proxy
    const { t } = useI18n()
    
    return {
        proxy,
        t,
        bkMessage: proxy.$bkMessage,
        bkInfo: proxy.$bkInfo,
        // ... other instance methods
    }
}
```

**Rules:**
- ✅ Name hooks with `use` prefix (e.g., `useTemplateActions`, `useFilter`)
- ✅ Export a default function that returns reactive state and methods
- ✅ Place hooks in `src/hooks/` or `src/hook/` directory
- ✅ Use `useInstance()` helper to access Vue 2 instance methods in composition API
- ❌ Don't use mixins - convert to hooks

### 3. Pinia Store Patterns (Vue 3)

**Option Store Style:**
```typescript
// src/store/user.ts
import { defineStore } from 'pinia'

export const useUser = defineStore('user', {
    state: () => ({
        user: { username: '', avatar_url: '' }
    }),
    actions: {
        setUser(user: IUser) {
            this.user = user
        }
    }
})
```

**Setup Store Style (Preferred):**
```typescript
// src/store/repoConfig.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'

export default defineStore('repoConfig', () => {
    const isLoading = ref(false)
    const dataList = ref([])
    const pagination = ref({ count: 0, current: 1, limit: 20 })
    
    const fetchList = async () => {
        isLoading.value = true
        try {
            const res = await http.fetchList(pagination.value)
            dataList.value = res.records
            pagination.value.count = res.count
        } finally {
            isLoading.value = false
        }
    }
    
    return { isLoading, dataList, pagination, fetchList }
})
```

**Rules:**
- ✅ Prefer setup store style (function) over option store style
- ✅ Use `storeToRefs()` when destructuring reactive state from store
- ✅ Name store files matching the domain (e.g., `user.ts`, `group.ts`)
- ✅ Include loading states and pagination in stores that fetch data

### 4. Router Patterns

**Lazy Loading with Chunk Names:**
```javascript
// Always use dynamic imports with webpackChunkName
const PipelineList = () => import(/* webpackChunkName: "pipelineList" */'../views/PipelineList')
const TemplateEdit = () => import(/* webpackChunkName: "pipelinesTemplate" */'../views/Template/TemplateEdit.vue')
```

**Route Structure:**
```javascript
{
    path: '/pipeline/:projectId',
    component: PipelineRoot,
    redirect: { name: 'PipelineManageList' },
    children: [
        {
            path: 'list/:viewId?',
            name: 'PipelineManageList',
            component: PipelineList,
            meta: { webSocket: true }
        },
        {
            path: ':pipelineId/detail/:buildNo',
            name: 'pipelinesDetail',
            components: {
                header: DetailHeader,
                default: DetailPage
            },
            meta: { title: 'pipeline', to: 'PipelineManageList' }
        }
    ]
}
```

**Rules:**
- ✅ Use lazy loading for all route components
- ✅ Add `webpackChunkName` comments for meaningful bundle names
- ✅ Use `meta` for route-level configuration (permissions, WebSocket, etc.)
- ✅ Use named views (`components: { header, default }`) for layouts
- ✅ Sync URL with navigation using `window.$syncUrl?.()` after route changes

### 5. Component Patterns

**Vue 3 TSX Components:**
```tsx
// src/views/MyPage.tsx
import { defineComponent, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { Message } from 'bkui-vue'
import http from '@/http/api'

export default defineComponent({
    setup() {
        const { t } = useI18n()
        const route = useRoute()
        const router = useRouter()
        
        const isLoading = ref(false)
        const formData = ref({ name: '', code: '' })
        
        const handleSubmit = async () => {
            try {
                await http.create(formData.value)
                Message({ theme: 'success', message: t('创建成功') })
                router.push({ name: 'List' })
            } catch (e) {
                console.error(e)
            }
        }
        
        onMounted(() => {
            // Init logic
        })
        
        return () => (
            <div class="my-page">
                <bk-form model={formData.value}>
                    <bk-form-item label={t('名称')} required>
                        <bk-input v-model={formData.value.name} />
                    </bk-form-item>
                </bk-form>
                <bk-button theme="primary" onClick={handleSubmit}>
                    {t('提交')}
                </bk-button>
            </div>
        )
    }
})
```

**Vue SFC Composition API:**
```vue
<template>
    <div class="pipeline-list">
        <bk-table :data="pipelines" v-bkloading="{ isLoading }">
            <bk-table-column :label="$t('name')" prop="name" />
        </bk-table>
    </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n-bridge'

export default {
    name: 'PipelineList',
    setup() {
        const { t } = useI18n()
        const isLoading = ref(false)
        const pipelines = ref([])
        
        onMounted(async () => {
            isLoading.value = true
            // fetch data
            isLoading.value = false
        })
        
        return { isLoading, pipelines, t }
    }
}
</script>
```

**Rules:**
- ✅ Use `defineComponent` with `setup()` for TSX components
- ✅ Use Composition API `<script setup>` or `setup()` for SFC
- ✅ Import `useI18n` for translations, use `t()` function
- ✅ Use `bk-loading` directive or `<bk-loading>` component for loading states
- ✅ Handle errors in try-catch, show user-friendly messages

### 6. i18n Patterns

**Usage in Composition API:**
```javascript
import { useI18n } from 'vue-i18n'  // Vue 3
import { useI18n } from 'vue-i18n-bridge'  // Vue 2

const { t } = useI18n()
const message = t('pipeline.createSuccess')
```

**In Templates:**
```vue
<template>
    <span>{{ $t('pipeline.name') }}</span>
    <bk-button>{{ $t('common.submit') }}</bk-button>
</template>
```

**Locale File Structure:**
```json
// locale/pipeline/zh-CN.json
{
    "name": "名称",
    "createSuccess": "创建成功",
    "template": {
        "copyTemplate": "复制模板",
        "deleteStore": "删除研发商店模板"
    }
}
```

**Rules:**
- ✅ All user-facing strings must use i18n
- ✅ Locale files in `/locale/[service]/[locale].json`
- ✅ Use dot notation for nested keys: `$t('template.copyTemplate')`
- ✅ Dynamic locale loading via `dynamicLoadModule()`
- ❌ Don't hardcode Chinese or English strings in code

### 7. Error Handling Patterns

**API Error Handling:**
```javascript
const fetchData = async () => {
    try {
        const res = await api.getData()
        // Handle success
    } catch (error) {
        const message = error.message || error
        bkMessage({ message, theme: 'error' })
    } finally {
        isLoading.value = false
    }
}
```

**Confirmation Dialogs:**
```javascript
import { bkInfo } from './useInstance'

function deleteItem(row) {
    bkInfo({
        title: t('确认删除'),
        subHeader: h('p', `${t('名称')}: ${row.name}`),
        confirmLoading: true,
        confirmFn: async () => {
            await api.delete(row.id)
            bkMessage({ message: t('删除成功'), theme: 'success' })
            fetchList()
        }
    })
}
```

**Rules:**
- ✅ Always wrap async operations in try-catch
- ✅ Use `finally` to reset loading states
- ✅ Show user-friendly error messages via `bkMessage` or `Message`
- ✅ Use `bkInfo` for confirmation dialogs with `confirmLoading: true`
- ✅ Parse error messages: `error.message || error`

### 8. Utility Patterns

**Common Utilities (src/utils/util.js):**
```javascript
// Deep copy (prefer structuredClone when available)
export const deepClone = obj => {
    if (typeof structuredClone === 'function') {
        return structuredClone(obj)
    }
    return JSON.parse(JSON.stringify(obj))
}

// Debounce
export function debounce(fn, interval = 1000) {
    let timer = null
    return (...args) => {
        clearTimeout(timer)
        timer = setTimeout(() => fn(...args), interval)
    }
}

// Time conversion
export function convertTime(ms) {
    if (!ms) return '--'
    const time = new Date(ms)
    return `${time.getFullYear()}-${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ...`
}

// Copy to clipboard
export async function copyToClipboard(text) {
    if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(text)
    } else {
        // Fallback implementation
    }
}
```

**Rules:**
- ✅ Reuse existing utilities from `src/utils/util.js`
- ✅ Use `dayjs` for date manipulation (already in dependencies)
- ✅ Use `uuid` package for generating unique IDs
- ✅ Check for existing utilities before creating new ones
- ❌ Don't duplicate utility functions across packages

### 9. CSS/Styling Patterns

**SCSS in Vue 2 packages:**
```scss
// Scoped styles with BEM-like naming
.pipeline-list {
    &-header {
        display: flex;
        align-items: center;
    }
    &-item {
        padding: 12px 16px;
        &:hover {
            background: #f5f7fa;
        }
    }
}
```

**Tailwind in Vue 3 packages (devops-platform):**
```tsx
<div class="flex items-center p-[24px] bg-white rounded-[2px]">
    <span class="text-[14px] font-bold text-[#4D4F56]">{t('标题')}</span>
</div>
```

**Rules:**
- ✅ Vue 2 packages: Use SCSS with scoped styles
- ✅ devops-platform: Use Tailwind CSS utility classes
- ✅ Use CSS variables for theming when possible
- ✅ Follow BlueKing design system color palette
- ❌ Don't use inline styles except for dynamic values

### 10. Form Patterns

**Form with Validation:**
```tsx
const formRef = ref()
const formData = ref({ name: '', code: '' })
const rules = {
    name: [
        {
            validator: value => /^[\u4e00-\u9fa5\w ]{1,20}$/.test(value),
            message: t('格式错误'),
            trigger: 'change'
        }
    ]
}

const handleSubmit = async () => {
    await formRef.value.validate()
    // Submit logic
}

return () => (
    <bk-form ref={formRef} model={formData.value} rules={rules}>
        <bk-form-item label={t('名称')} property="name" required>
            <bk-input v-model={formData.value.name} />
        </bk-form-item>
    </bk-form>
)
```

**Rules:**
- ✅ Use `bk-form` with `rules` prop for validation
- ✅ Always add `property` attribute matching the model key
- ✅ Call `formRef.value.validate()` before submission
- ✅ Use regex validators for format validation
- ✅ Include `trigger: 'change'` or `'blur'` in rules

---

## Quick Reference Card

| Task | Pattern | Location |
|------|---------|----------|
| HTTP requests | `http.get/post/put/delete` | `src/http/api.ts` or `src/utils/request.js` |
| Shared logic | `useXxx()` composable | `src/hooks/` or `src/hook/` |
| State (Vue 3) | `defineStore()` with setup | `src/store/*.ts` |
| Translations | `const { t } = useI18n()` | Component setup |
| Show message | `bkMessage({ message, theme })` | After API calls |
| Confirm dialog | `bkInfo({ title, confirmFn })` | Delete/destructive actions |
| Loading state | `v-bkloading="{ isLoading }"` | Wrap async content |
| Route lazy load | `() => import(/* webpackChunkName */)` | Router config |
| Deep copy | `deepClone(obj)` | `src/utils/util.js` |
| Clipboard | `copyToClipboard(text)` | `src/utils/util.js` |
