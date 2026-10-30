# BK-CI 前端代码风格与组件复用规范

本文件适用于 `src/frontend/**`，是 [`AGENTS.md`](./AGENTS.md) 的配套细则：`AGENTS.md` 描述协作流程与工作边界，本文件描述具体的代码风格、组件复用顺序、各技术栈写法和提交校验要求。团队成员和开发代理都以本文件为准。

## 规则优先级

1. 用户或需求方在当前任务中的明确要求。
2. 更靠近目标文件的 `AGENTS.md`、子工程 README、仓库根目录 `CONTRIBUTING.md`。
3. 目标子工程的实际配置：`package.json`、ESLint、Stylelint、TypeScript、EditorConfig、commitlint、lint-staged、构建配置。
4. 本文件的通用约定。

发生冲突时先服从更高优先级，并在交付说明中点明取舍和可能的校验冲突。本文件描述的是默认做法，不用来推翻某个子工程已经生效的配置。

## 通用工作方式

- 改动保持小而聚焦，只处理与当前需求直接相关的代码；不做顺带重构、全仓格式化、依赖升级或文件搬迁。
- 默认只改前端范围。需要后端、接口模型或部署配合时，先说明阻塞点、所需接口变化和可选方案，不擅自改动前端以外的代码。
- 不覆盖他人未提交的改动。编辑可能重叠的文件前先读相关 diff，保留与当前任务无关的内容。
- 优先沿用项目已有模式和依赖，不为新代码引入新的架构、UI 库或代码风格。
- 新增抽象要有明确收益（消除真实重复、隔离复杂度、复用既有模式）；不为一次性使用创建通用层。
- 注释克制，只解释不明显的业务意图、边界条件或复杂流程。

## 开始编码前

1. 找到待改文件**最近的 `package.json`**，据此判断子工程、Vue 版本、组件库、构建工具和可用脚本。不要用目录名或其他子工程的写法推断。
2. 阅读同目录及相邻的页面、组件、hooks/composables、utils、store、API 封装、样式和测试，沿用已有模式。
3. 确认验收条件、受影响页面、权限场景、接口状态和需要执行的最小验证。

常用探查命令：

```bash
cd src/frontend
rg -n '"vue"|bk-magic-vue|bkui-vue' */package.json
rg -n -A 15 '"scripts"' <sub-project>/package.json
rg -n '\$bkMessage|\$bkInfo|Message\(|InfoBox\(' <sub-project>/src
```

第二条尤其重要：各子工程可用的脚本差别很大，别假设 `lint` / `type-check` / `build` 一定存在，详见「验证」。

## Monorepo 结构与技术栈分布

前端工作区位于 `src/frontend`，根配置要求 Node.js `>=22`、pnpm `>=11`，安装与工作区命令统一使用 pnpm。这是 Vue 2.7 与 Vue 3 并存的 monorepo，各子工程的 Vue、TypeScript、ESLint、Stylelint、构建工具和组件库版本并不一致，**禁止把一个子工程的写法批量套用到另一个子工程**。

- 工作区范围以 `pnpm-workspace.yaml` 为准：包含 `bk-pipeline`、`bk-pipeline-vue2`、`bk-permission`、`locale`、`common-lib` 和 `devops-*`；`devops-repo` 被 `!devops-repo` 显式排除，既不在根递归命令覆盖范围内，也不在锁文件的 importers 里，`pnpm --filter` 取不到它。涉及它时进入 `devops-repo` 目录单独安装依赖，再运行它自己的脚本（它有 `lint`、`test:unit`、`public:master`，没有 `type-check`，也没有 `build`），并单独验证。
- Vue 2.7 子工程示例：`devops-pipeline`、`devops-stream`、`devops-nav`、`devops-turbo`、`devops-quality`、`devops-environment`、`devops-atomstore`、`devops-codelib`、`devops-ticket`、`locale`。
- Vue 3 子工程示例：`devops-flow`、`devops-manage`、`devops-metrics`、`devops-permission`、`devops-platform`、`devops-repo`。
- `bk-permission` 是双版本共享包，不要归进上面任何一侧：它的 peer `vue` 是 `~2.7.16`，同时导出 V2 与 V3 两套权限指令，也被 Vue 3 应用直接消费；而它自己的 `.eslintrc.js` 继承 `@blueking/eslint-config-bk/vue3`，改它的代码按 Vue 3 preset 的格式基线写。
- `bk-pipeline` 声明 `vue` 为 `^2.7.0 || ^3.0.0`，同时提供两套构建与两套组件库 peer dependency。修改它时必须检查目标入口、现有文件写法、peer dependencies，并评估两端构建影响。
- `bk-pipeline-vue2` 不是第二套源码：该目录下只有 `package.json` 与 `vite.config.js`，没有 `src/`，其 `vite.config.js` 把 `@` 指向 `../bk-pipeline/src` 并自注为 single source of truth，包名是 `bkui-pipeline-vue2-build`。它只是产出 Vue 2 产物的构建包装，**改流水线组件一律改 `bk-pipeline/src`**。

以上列举只是当前形态的参考，**最终一律以最近的 `package.json` 为准**。

### 仓库内共享层

复用共享能力时先看这几处：

- `common-lib`：一小组共享资源，目录里实际只有 `docs.js`、`permission-conf.js`、`qrcode.min.js`、`scroll-load-list.vue` 和 `log/`。它虽然在 pnpm 工作区包列表内，但没有任何子工程按包名声明依赖，**按包名 import 会失败**；现有引用一律是相对路径（如 `devops-pipeline/src/main.js:50`、`devops-nav/src/index.ts:17` 都写 `import createDocs from '../../common-lib/docs'`），靠 `webpack.base.js` 的 `include` 参与编译。新增引用照此照抄相对路径。
- `bk-pipeline`：流水线组件的唯一源码位置。它是 ESM 包（`"type": "module"`），通过 `exports` 的 `.` / `./vue2` / `./vue3` 分发，消费方按目标 Vue 版本选子路径；它自身的 ESLint 仍继承 webpack 系的前端根配置，不是 Vue 3 preset。
- `bk-pipeline-vue2`：只是 `bk-pipeline` 的 Vue 2 构建包装，不要往里加源码。
- `bk-permission`：权限指令与组件，同时覆盖 Vue 2/Vue 3，具体用法见「权限落地」。
- `locale`：国际化资源，按模块分目录集中存放，由各子工程反向导入，见「国际化文案」。
- `svg-sprites`：图标雪碧图资源，由构建流程统一处理。

## 组件与逻辑复用顺序

1. 当前子工程已有的业务组件、hooks/composables、utils、store 和 API 封装。
2. 上一节列出的仓库内共享层。
3. **当前子工程实际已安装**的蓝鲸组件库能力。
4. 自建组件或工具。

只有前三类都不满足时才新增抽象，并在交付说明中写明不复用的原因（API 不满足、交互差异明显、历史组件耦合过重等）。不要为了使用某个组件主动引入新的 UI 库，也不要给没有声明该依赖的子工程新增组件库依赖。

### 组件库以最近的 package.json 为准

- Vue 3 子工程普遍使用 `bkui-vue`，但版本跨度较大（不同子工程锁定不同 beta 版本），API 细节以本工程已安装版本和现有用法为准。
- Vue 2 子工程中，部分（如 `devops-pipeline`、`devops-nav`、`devops-stream`、`locale`）本地声明了 `bk-magic-vue`；另一部分（如 `devops-codelib`、`devops-ticket`、`devops-quality`、`devops-environment`、`devops-atomstore`、`devops-turbo`），以及 `bk-permission` 这类共享包，并不本地声明，而是跟随宿主应用的全局注册使用同一套组件。修改这类包时不要顺手补依赖，按现有引用方式书写。
- 内网部署形态可能使用带作用域的组件库包名（例如 `@tencent/bk-magic-vue`）而非公开包名。不要按包名硬编码假设，始终以最近的 `package.json` 与相邻文件的导入方式为准。

## Vue 2.7 子工程

- 优先使用 Composition API，组合式 API 从 `vue` 导入（Vue 2.7 已内置，不使用 `@vue/composition-api`）。
- 新组件优先 `defineComponent` + `setup()`；只有当前子工程构建链路和同类文件已经支持 `<script setup>` 时才使用 `<script setup>`。
- 小范围修改既有 Options API 文件时保持文件主体风格，不为局部需求整文件迁移，除非任务本身就是迁移。
- `setup()` 的第二个参数只提供 `attrs`、`listeners`、`slots`、`emit`、`expose`，**没有 `root`**。需要访问 `$store`、`$router`、`$t`、`$bkMessage` 等实例能力时，使用 `getCurrentInstance()` 取 `proxy`。
- 新增可复用逻辑优先抽成 composable/hook，命名与当前子工程保持一致。部分子工程已封装了实例访问 hook（例如 `devops-pipeline/src/hook/useInstance.js`），本工程已有同类封装时优先复用，不要各写一份。

```vue
<template>
    <bk-dialog
        v-model="isDialogShow"
        :title="$t('确认操作')"
        :loading="isSubmitting"
        @confirm="handleConfirm"
    >
        <bk-form :model="formData">
            <bk-form-item :label="$t('名称')" required>
                <bk-input v-model="formData.name" />
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script>
    import { defineComponent, getCurrentInstance, reactive, ref } from 'vue'

    export default defineComponent({
        name: 'ExampleDialog',
        setup () {
            const { proxy } = getCurrentInstance()
            const isDialogShow = ref(false)
            const isSubmitting = ref(false)
            const formData = reactive({ name: '' })

            async function handleConfirm () {
                isSubmitting.value = true
                try {
                    await proxy.$store.dispatch('example/save', formData)
                    proxy.$bkMessage({ theme: 'success', message: proxy.$t('操作成功') })
                    isDialogShow.value = false
                } finally {
                    isSubmitting.value = false
                }
            }

            return { isDialogShow, isSubmitting, formData, handleConfirm }
        }
    })
</script>
```

示例中的 `v-model`、事件名和实例方法在不同组件与版本间存在差异，落地前请先复制当前子工程同类组件的真实用法。

## Vue 3 子工程

**新建组件统一使用 TypeScript + TSX（`.tsx`）**，采用 `defineComponent`、Composition API 和类型明确的 props 与 slots，不新建 `<script setup>` 组件。新增 composable、store、utils 和类型模型同样使用 TypeScript。

- 既有 `.vue`、`<script setup>` 或 JavaScript 文件按局部维护处理：保持文件主体风格，不为统一风格迁移无关代码。
- 目标子工程尚未配置 TSX 时，先在其**既有构建体系**内补齐最小必要配置并完成验证，不要为此引入新的构建工具或改写共享构建配置。
- 类型跟随当前子工程的严格度，不单方面提高或降低 `tsconfig` 要求。
- `emits` 用数组式声明（仓库里 `.tsx` 的 `emits: [` 有 92 处，对象校验式 0 处），不要引入对象校验写法。事件名大小写跟随同目录既有风格，仓库里 camelCase 与 kebab-case 并存，不要为统一命名去改相邻组件。
- 样式走 CSS Modules：`devops-flow` 全面使用 `*.module.css`，TSX 统一 `import styles from './X.module.css'` 后用 `class={styles.xxx}`，不在 TSX 里写全局类名或行内大段样式。

```tsx
import { defineComponent, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Button } from 'bkui-vue'

export default defineComponent({
  name: 'ExampleAction',
  props: {
    resourceId: {
      type: String,
      required: true,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['submitted'],
  setup(props, { emit, slots }) {
    const { t } = useI18n()
    const isSubmitting = ref(false)

    async function handleClick() {
      isSubmitting.value = true
      try {
        emit('submitted', props.resourceId)
      } finally {
        isSubmitting.value = false
      }
    }

    return () => (
      <Button
        theme="primary"
        loading={isSubmitting.value}
        disabled={props.disabled}
        onClick={handleClick}
      >
        {slots.default?.() ?? t('提交')}
      </Button>
    )
  },
})
```

上面这段按 `devops-flow` 的 Prettier 设置书写（`.prettierrc.json`：`semi: false`、`singleQuote: true`、`printWidth: 100`、`tabWidth: 2`），只是结构示例，不是全仓格式基线。其他 Vue 3 子工程走共享 ESLint preset，要求写分号、多行结构带尾逗号；照抄到这些子工程时按目标子工程实际加载到的配置重排，详见「两套 ESLint 基线」。

## 交互反馈与状态覆盖

- 消息、确认框、通知优先沿用当前子工程已有封装。Vue 2 常见为 `$bkMessage`、`$bkInfo` 或团队二次封装；Vue 3 先找本工程封装，没有封装时再使用已安装 `bkui-vue` 版本导出的 `Message`、`InfoBox` 等能力。带 i18n、错误格式化、权限提示或统一样式的封装必须优先使用。
- 沿用现有 i18n、router、store、request 和权限封装，不硬编码中文到模板；文案落位见「国际化文案」，请求落位见「请求层分层」，权限落位见「权限落地」。
- 异步交互按需求覆盖 loading、disabled、防重复提交、空态、错误反馈和恢复路径。
- 涉及权限或只读页面时同时检查可见性与可操作性，不只隐藏入口。
- 涉及列表、表单、弹窗和长文本时检查空数据、超长内容、校验失败、关闭/取消和重复操作等边界。

## 国际化文案

文案**不在子工程内**，而是集中在共享的 `src/frontend/locale/<模块>/{zh-CN,en-US,ja-JP}.json`，由子工程反向导入：

- 相对路径导入，如 `devops-manage/src/main.ts:25-27`、`devops-flow/src/main.ts:10-12` 的 `../../locale/<模块>/`。
- 目录批量导入，如 `devops-pipeline/src/main.js:57` 的 `require.context('@locale/pipeline/')`；`@locale` 别名定义在 `webpack.base.js:164`。

因此新增文案要改共享 `locale` 目录，属于跨子工程改动，交付说明里单独点出。另外：

- 部分模块目录并不齐三语（例如 `locale/artifactory`、`locale/platform` 缺日文），按该目录现状处理，不要为补齐语言顺手新建文件。
- 仓库**没有**校验三语 key 对齐的脚本，增删 key 后自行逐个语言文件核对，不要只改中文。
- key 命名风格跟随当前模块文件里的既有写法，仓库里不统一，不要为统一命名批量改 key。

## 导航壳与 iframe 桥接

除 `devops-nav` 外的子应用都嵌在导航壳里运行。各子应用在**自己的** `index.html` 里用 `<script>` 引入壳层静态目录下的 `devops-utils.js`（路径形如 `/console/static/devops-utils.js`，前缀按构建变量拼）。这个脚本会按是否存在全局 `Vue` 决定挂载位置：Vue 2 挂到 `Vue.prototype.$xxx`，Vue 3 挂到 `window.$xxx`。实测使用点包括 `devops-flow/src/router/index.ts`、`devops-manage/src/router/index.ts`、`devops-permission/src/router/index.ts`、`devops-platform/src/router/index.ts`、`devops-repo/src/main.ts`、`devops-pipeline/src/App.vue`。

- 新增或调整路由时保留 `router.afterEach` 里的 `$syncUrl` 同步，否则壳层地址栏和子应用路由会脱节。
- 页面标题走 `$updateTabTitle`，全局提示走 `$showTips`，离开确认走 `$leaveConfirm`；不要各自再写一套，也不要直接操作壳层 DOM 或 `document.title`。
- 这些能力是运行期注入的，不要 import；在 TypeScript 子工程里按该工程 `types/global.d.ts` 的既有声明使用。

## 请求层分层

仓库里四种请求分层并存，新接口按**本子工程**的分层落位，不跨子工程搬另一套封装：

- Vue 2 子工程：请求写在 Vuex action 里、经 `$ajax` 实例发起，URL 前缀常量放本工程 `store/constants.js`（参考 `devops-codelib/src/store/actions.js`）。
- `devops-manage`、`devops-metrics`、`devops-permission`、`devops-platform`：用 `src/http/fetch` 封装，接口集中声明在 `src/http/api.ts`。
- `devops-flow`：用 `src/utils/http`，接口按领域拆到 `src/api/*.ts`。
- 少数 Vue 2 包另有自己的 `src/api/`，按目录现状写。

不在组件里直接调 `axios` / `fetch`。接口前缀用构建期注入的全局量（见「构建硬边界与全局量」），不要拼死地址。

## 权限落地

权限相关能力统一走 `bk-permission`，不要自己再实现一套置灰与申请弹窗：

- 「有入口但无权限」用 `v-perm` 指令渲染（置灰 + 悬浮提示 + 点击弹申请），传 `hasPermission`、`permissionData`。
- 后端接口已返回 `permissions` 字段时，置 `disablePermissionApi: true`，避免指令再发一次鉴权请求。
- 资源类型与动作码用本工程已有常量表（如 `devops-pipeline/src/utils/permission.js` 的 `RESOURCE_TYPE`、`RESOURCE_ACTION`），不在模板里写字面量字符串。
- 接口返回 403 时走本工程既有的 `handleXxxNoPermission` 拉起申请流程，不要只弹一个错误提示就结束。
- 整页无权限或权限未开启的展示，用 `bk-permission` 已有的 `no-permission` / `no-enable-permission` 组件，不要自绘空态。

## 代码与样式约定

- 以目标子工程的 ESLint、Stylelint、TypeScript、EditorConfig 和相邻代码为准，不凭个人偏好统一缩进、引号或格式。
- 样式写法跟随当前子工程（SCSS/Less/CSS Modules、BEM、`scoped`、`::v-deep` 等），并注意构建链路对样式语言有硬限制，见「构建硬边界与全局量」。避免全局选择器泄漏和无依据的固定尺寸。
- 仓库当前几乎没有无障碍与响应式基线：`aria-*` / `role=` 和 `@media` 在全仓都极少出现，没有可跟随的既有做法。不要求新代码补齐全站，但新增交互不要破坏键盘可达性，也不要写死会在窄屏溢出的固定尺寸。
- 不静默改变接口契约。字段或返回状态不确定时，先从现有 API 封装、类型定义和调用方确认；仍无法确认时明确标注假设。

### 命名与目录：跟随当前子工程

以下几处仓库内确实多派并存，**跟随当前子工程既有写法**即可，不要为了统一而改动已有代码，也不要在交付说明里把某一派说成规范：

- 状态目录 `src/store` 与 `src/stores` 并存；组合式逻辑目录 `src/hook`、`src/hooks`、`src/composables` 并存。
- 文件名大小写（PascalCase 与 kebab-case）、Pinia store 的导出风格、i18n key 风格、TSX 事件名大小写、列表分页等默认参数，都以同目录相邻文件为准。

### 两套 ESLint 基线

格式规则在仓库里不是一套，跨子工程搬代码**必须按目标子工程重排格式**，否则一定报错：

- **webpack 系**继承 `src/frontend/.eslintrc.js`：`indent` 4、`semi` `never`、`space-before-function-paren` named `always`、`vue/html-indent` 4 且 `baseIndent: 1`、`vue/script-indent` 4 且 `baseIndent: 1`、`vue/max-attributes-per-line` 单行与多行都是 max 1、`vue/no-v-html` 关闭。
- **共享 preset 系**：`devops-manage`、`devops-metrics`、`devops-permission`、`devops-platform` 继承 `@blueking/eslint-config-bk/tsvue3`，`bk-permission` 继承同包的 `vue3`，该包下层再 `extends: ['eslint-config-tencent']`。这几个包**并不锁同一个 preset 版本**（`devops-metrics` 是 `2.1.0-beta.6`，`devops-manage` 与 `devops-permission` 是 `2.1.0-beta.12`），所以取值只能按当前形态参考：`indent` 2、`semi` `always`、`quotes` `single`、`comma-dangle` `always-multiline`、`space-before-function-paren` named `never`、`vue/no-v-html` `error`、`vue/max-attributes-per-line` 关闭。
- `devops-platform` 和 `bk-permission` 的 `.eslintrc.js` 继承了这个共享 preset，却没有在自己的 `package.json` 里声明它。干净安装后这两个包跑 lint 可能直接解析不到配置；遇到就照实反馈，不要顺手补依赖。
- 还有三个包不属于上面任何一套：`devops-repo` 直接用 `eslint-config-tencent` + `/ts`；`devops-flow` 是独立的 ESLint 9 flat config（`eslint.config.ts`）；`devops-stream` 的 `.eslintrc.js` 设了 `root: true`，继承 `plugin:vue/recommended` + `standard` 之后又自带一整套 `rules`，构建链路同样是自己的一套（见「构建硬边界与全局量」）。
- `devops-stream` 这套值得单独看一眼：它自己写死的 `indent` 4、`semi` `never`、`space-before-function-paren` named `always` 与 webpack 系相同，所以在它和 `devops-pipeline` 之间搬代码不会撞缩进；但 `vue/max-attributes-per-line` 在它这里是关闭的（webpack 系是 max 1），而且 `root: true` 意味着根配置以后的调整不会传导过去。另外 `extends` 里的 `standard`（即 `eslint-config-standard`）只在前端根声明、这个包自己没声明，干净安装后可能解析不到。改它之前先读它自己的 `.eslintrc.js`。

也就是说缩进（4 / 2）、分号（不写 / 写）、函数名后空格（有 / 无）、`v-html` 和单行属性数这几处，两套基线是**相反**的。上面的取值只是当前形态参考，不是保证；最终一律以该子工程实际加载到的配置和它实际报出来的错为准，不要把它当成完整规则表来背。

### Stylelint 覆盖面

只有 `bk-permission`、`devops-manage`、`devops-metrics`、`devops-permission` 四个包有 `.stylelintrc.js`，都继承共享的 `@blueking/stylelint-config-bk`，并提供带 `--fix` 的 `lint:style` 脚本。

- 接了共享 preset 的子工程样式约束较强，**具体取值以该子工程实际安装的版本为准**；不要凭印象断定某条写法被禁止，先跑它自己的 `lint:style` 看结果。
- 但这条命令不一定跑得起来：`bk-permission` 有 `.stylelintrc.js` 和 `lint:style`，却既没声明 `stylelint` 也没声明那个 preset。干净安装后命令可能直接解析失败，照实反馈即可，不要为了跑通而加依赖。
- 其余子工程没有配置 Stylelint，样式一致性靠相邻代码保持，改动前先看同目录现有写法。
- `!important` 等强覆盖写法按需克制使用：能靠提升选择器精确度或调整结构解决时优先那样做，确有必要时在相邻代码风格允许的前提下使用，并以本子工程的实际校验结果为准。

## 构建硬边界与全局量

本节说的「webpack 系」指 `webpack.config.js` 里 `require` 了 `../webpack.base` 的那批包，当前是 `devops-atomstore`、`devops-codelib`、`devops-environment`、`devops-nav`、`devops-pipeline`、`devops-quality`、`devops-ticket`、`devops-turbo`。**动手前先确认本包是否在其中**：`devops-stream` 虽然也是 Vue 2.7 子工程，却维护自己的 `build/webpack.*.conf.js` 链路，下面这些限制不能照搬给它。

这批包共享 `src/frontend/webpack.base.js`，其中几条限制会直接让构建失败，写代码前先知道：

- `resolve.extensions` 是 `['.js', '.vue', '.json', '.ts', '.scss', '.css']`（`webpack.base.js:158`），**不含 `.tsx` / `.jsx`**；在这些子工程里新增 TSX 文件必须先补齐其既有构建体系所需的最小配置。
- 样式规则只有 `.css` 与 `.scss`，**没有 less-loader**，webpack 系子工程写 Less 会构建失败。Less 出现在 `devops-repo` 和部分 bkui-cli 子工程，`devops-flow` 用 CSS Modules；样式语言按本子工程的构建工具选。
- scss 链路没有配 `additionalData`，用到 `conf.scss` 里的变量或 mixin 必须在文件内显式 `@import`，不要假设全局可用。
- `vue`、`vue-router`、`vuex` 被设为 externals（`webpack.base.js:167-171`），由壳层提供，子应用不要再自带或重复打包。

构建期注入的全局量与别名：

- webpack 系有一批构建期注入的全局量，清单见前端根 `.eslintrc.js` 的 `globals`（`API_URL_PREFIX`、`WEB_URL_PREFIX`、`PROXY_URL_PREFIX` 等）。直接引用即可，不要 import，也不要新增未在该清单声明的全局量。
- 包内导入用 `@/`，跨包国际化资源用 `@locale/`。
- 运行时环境变量按构建工具区分读法：vite 用 `import.meta.env`，bk-cli / webpack 用 `process.env.BK_*`。

## 验证

验证范围与改动风险匹配，优先使用目标子工程 `package.json` 已声明的脚本：

```bash
cd src/frontend
pnpm --filter <package-name> run <script-name>
```

`--filter` 匹配的是 `package.json` 里的 `name` 字段，不是目录名（例如 `bk-pipeline` 的包名是 `bkui-pipeline`，`locale` 的包名是 `common-locale`）。`devops-repo` 被工作区排除，`--filter` 取不到，须进它自己的目录单独安装依赖后跑它自己的脚本。

**先确认命令存在再说要跑什么。** 多数子工程并没有独立的校验脚本：`lint` 只在 `devops-flow` 与 `devops-repo` 有，`type-check` 只在 `devops-flow` 有，多数包的 `scripts` 只有 `dev` 和一组 `public:*`（`public:dev|test|master|external`），`locale` 和 `common-lib` 的 `scripts` 是空的。因此：

- 动手前先看目标包 `package.json` 的 `scripts`，按实际存在的命令安排验证，不要照搬 `lint` / `type-check` / `build` 这组名字。
- webpack 系子工程的构建入口是 `public:*` 里对应环境的那个（不是 `build`），ESLint 由构建链路内联执行（`webpack.base.js:64-75` 的 `eslint-loader`）。注意它的 `test` 是 `/\.(js|vue)$/`，**`.ts` 文件不会被构建期 lint 到**，这类文件的风格问题得靠该子工程自己的 lint 或人工核对。
- 声明了专用脚本的子工程（`devops-flow` 的 `lint`、`type-check`、`test:unit`、`build`，`devops-repo` 的 `lint`、`test:unit`）优先用自身脚本，不用前端根 ESLint 替代；`test:e2e` 只在交互链路确有需要时运行。

**当前没有可用的单元测试基线。** 全仓只有 `devops-flow/e2e/vue.spec.ts` 与 `devops-repo/tests/unit/example.spec.ts` 两个脚手架占位文件，`src/**` 下没有任何 `*.spec.*`。验证以 lint、类型检查、目标子工程构建和浏览器实测为主；确需新增测试时放进已有测试目录、沿用其已配置的框架，不要为此引入新的测试工具链。

分场景：

- 纯文案或局部样式：检查 diff，执行适用的 lint/stylelint；有页面条件时做浏览器检查。
- 组件或业务逻辑：按上面确认到的命令执行校验与目标子工程构建，并在浏览器里跑通验收路径。
- 路由、状态管理、请求链路或公共组件：验证主要成功路径、失败路径和至少一个关键边界场景。
- `bk-pipeline` 的公共逻辑改动至少执行受影响的 Vue 2/Vue 3 构建；无法覆盖其中一端时必须说明。
- `devops-pipeline` 与 `devops-nav` 的 webpack 配置引用了 DLL manifest，而该产物被 gitignore。首次本地验证前先跑这两个子工程自己的 `dll` 脚本，再跑 `dev`，否则启动会失败。
- 不默认执行全仓递归构建；只有共享依赖、构建配置或跨多个子工程的改动才扩大范围。确需全量时用前端根的 `public` 脚本（`gulp -d frontend`，内部经 nx 编排各子工程的 `public:master`，并带 svg sprite 生成、共享资源拷贝等前置步骤），**不是** `pnpm -r run build`。
- 依赖缺失、环境限制或命令耗时导致验证未完成时，记录未执行的命令、原因和剩余风险，不把未运行说成通过。
- 编译成功不等于功能验证通过；涉及页面行为的改动要在本地实际跑通验收路径。

### `--fix` 会改到本任务之外的文件

自动修复的影响面不止显式的 lint 脚本：

- `webpack.base.js:64-75` 的 `eslint-loader` 带 `fix: true`，`include` 覆盖 `src`、`common-lib`、`locale`。**跑 `dev` 或 `public:*` 之后必须复查 `git status --short`**，还原与本任务无关的自动改写，尤其是 `common-lib`、`locale` 这些共享目录。
- 部分脚本自身就会改文件：`devops-flow` 的 `lint` 串跑两个 linter 且都带 `--fix`，`devops-repo` 的 `serve` 带 `--fix`，四个配了 Stylelint 的包的 `lint:style` 也带 `--fix`。
- 不使用会自动修复大量无关文件的命令；确实跑了带 `--fix` 的脚本，提交前逐文件核对 `git diff`。

### CI 的实际覆盖面

`.github/workflows/frontend.yml` 在 `src/frontend/**` 变更时触发，但只执行 `pnpm install` + `pnpm public`，**不跑 lint、type-check 和测试**。

- CI 绿灯只说明构建通过，不代表风格和逻辑已被检查，本地验证不能省。
- 反过来，构建期内联的 ESLint 报错会直接让 CI 失败，所以格式问题仍会在 CI 暴露。

## 提交信息与校验

提交前先看 `git status --short` 和相关 `git diff`，确认只包含本任务改动，没有调试代码、临时文件、无关锁文件变化或敏感信息。

### 本仓库（公开 GitHub）要求

根目录 `CONTRIBUTING.md` 的 GIT 提交规范要求 subject 以 `issue #<id>` 结尾：

```text
<type>: <简洁概要> issue #<id>
```

`src/frontend/commitlint.config.js` 的 `subject-valid` 插件规则定义了这一点（正则要求 subject 以 `issue #<数字>` 结尾）。例如：

```text
docs: 纳入前端 AGENTS.md 并共享工程代码风格与组件复用规范 issue #13598
```

可用 type 以 commitlint 配置为准，当前包括 `feature`、`feat`、`bug`、`fix`、`bugfix`、`refactor`、`perf`、`test`、`docs`、`info`、`format`、`merge`、`depend`、`chore`、`del`。

### 钩子不一定挂上了

仓库提供了 `.husky/commit-msg` 与 `.husky/pre-commit` 脚本，但**常规安装不会自动挂上**：`src/frontend/package.json` 的 `prepare: husky` 在 `src/frontend` 下执行，而 `.git` 在仓库根目录，实测 `git config core.hooksPath` 未设置、`.git/hooks` 下只有 `*.sample`。

- 提交前自行确认钩子是否生效（看 `git config core.hooksPath` 和 `.git/hooks` 的实际内容），**不要把本地没报错当成已通过校验**。
- 钩子没生效时，提交信息格式和暂存区格式化都得自己对着规则核一遍。

### lint-staged 的跨基线陷阱

前端根 `package.json` 的 `lint-staged` 是 `"*.{js,ts,tsx,vue}": ["eslint --fix"]`，而根 `eslint` 是 `^7.3.1`；`devops-flow` 的 `eslint` 是 `^9.37.0` 且只有 flat config。ESLint 7 读不到 flat config，会向上落到根的 Vue 2 配置，把文件改成另一套格式（缩进、分号都相反，见「两套 ESLint 基线」）。

- 钩子生效的情况下，提交 Vue 3 或 TypeScript 子工程改动后复查 `git diff`，确认没有被改成另一套格式。
- 被改坏时用该子工程自身的 lint 重新格式化，不要手工逐行回改，也不要因此去动根配置。

### 与内部工程树的格式冲突

部分内部前端工程树历史上要求 subject 以工作项 ID 结尾，形如 `--story=<ID>`、`--bug=<ID>` 或 `--task=<ID>`。这与本仓库的 `issue #<id>` 规则不兼容：

- 在本仓库（公开 GitHub）提交时使用 `issue #<id>`；带 `--story|bug|task=` 结尾的 subject 会被本仓库的 commitlint 规则判为不合规。
- 需要把同一改动带入使用旧格式的内部树时，在那一侧按该树生效的钩子改写 subject，不要反过来修改本仓库的 `CONTRIBUTING.md` 或 `commitlint.config.js` 来迁就。
- 不确定当前树用哪种格式时，以该树实际生效的 commitlint 配置与钩子报错信息为准，不编造工作项 ID。

## 交付说明

完成后简洁说明：改了什么、涉及哪个子工程、执行了哪些验证及结果、哪些验证未执行及原因、是否存在接口依赖或兼容性风险。不把未验证的猜测说成事实。
