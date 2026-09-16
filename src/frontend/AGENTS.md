# BK-CI 前端协作规范

本文件适用于 `src/frontend/**`。它用于统一开发代理和团队成员在 BK-CI 前端仓库中的工作方式。若用户当前指令、仓库根目录规范或更靠近目标文件的 `AGENTS.md` 有更具体要求，以更具体的要求为准。

具体的代码风格、组件复用顺序、Vue 2/Vue 3 写法、校验命令和提交信息细则见配套的 [`STYLE_GUIDE.md`](./STYLE_GUIDE.md)；本文件只描述协作流程与工作边界。

## 工作边界

- 默认只修改当前需求涉及的前端代码、前端测试和前端文档。
- 未经明确要求，不修改后端、数据库、接口模型、部署配置或无关子工程。若前端无法独立完成，先说明阻塞点、所需接口变化和可选方案。
- 保持改动小而聚焦；不做无关重构、全仓格式化、依赖升级或文件迁移。
- 不覆盖已有未提交改动。编辑重叠文件前先阅读相关 diff，保留与当前任务无关的内容。
- 先明确并对齐需求范围，再动手改产品代码；范围发生变化时先说明，不在同一次改动里顺带扩大范围。
- 一次交付对应一个工作项和一条开发分支；不在共享分支上边开发边提交，也不提交与当前工作项无关的文件。
- 涉及自动生成产物、锁文件或构建配置时，确认确有必要再改动，并在交付说明中单独指出。

## 开始开发前

1. 阅读仓库根目录 `CONTRIBUTING.md`、目标子工程 README、最近的 `package.json` 和相关构建配置。
2. 查看 `git status --short` 与目标文件相关 diff，确认工作区状态和改动边界。
3. 以最近的 `package.json` 判断 Vue 版本、组件库、构建工具和可用脚本，不根据目录名或其他子工程推断。
4. 查看同目录页面、组件、hooks/composables、utils、store、API 封装、样式和测试，优先沿用已存在的模式。
5. 开始编码前明确验收条件、影响页面、权限场景、接口状态和需要执行的最小验证。

## Monorepo 与技术栈

- 前端工作区位于 `src/frontend`，根配置要求 Node.js `>=22`、pnpm `>=11`；安装和工作区命令统一使用 pnpm。
- 这是 Vue 2.7 与 Vue 3 并存的 monorepo。各子工程的 Vue、TypeScript、ESLint、Stylelint、Webpack/Vite 和组件库版本并不完全一致，禁止用一个子工程的写法批量套用到其他子工程。
- Vue 2 代码中 Options API、Composition API 和少量 `<script setup>` 并存。修改现有 Options API 文件时保持主体风格，不为局部需求重写整份文件；新建组件统一优先使用 Composition API，并从 `vue` 导入组合式 API。只有目标子工程构建链路和同类文件已支持时才使用 `<script setup>`。
- Vue 3 新建组件统一使用 TypeScript + TSX（`.tsx`），优先采用 `defineComponent`、Composition API 和类型明确的 props 与 slots，`emits` 按仓库现状用数组式声明，不新建 `<script setup>` 组件；新增 composable、store、utils 和类型模型也使用 TypeScript。修改历史 `.vue` 或 JavaScript 文件时保持主体风格，不为统一风格迁移无关代码；目标子工程尚未配置 TSX 时，先补齐其既有构建体系所需的最小配置并完成验证。
- `bk-pipeline` 同时提供 Vue 2 与 Vue 3 构建，修改时必须检查其 peer dependencies、对应入口和两套构建影响，不能只验证一个版本。它是流水线组件的唯一源码位置；`bk-pipeline-vue2` 只是产出 Vue 2 产物的构建包装，不要往里加源码。
- `bk-permission` 是同时覆盖 Vue 2/Vue 3 的共享包，不要按某一个 Vue 版本的习惯去改它，先看它自己的 ESLint 基线。
- `devops-repo` 当前被 `pnpm-workspace.yaml` 排除，不属于根工作区递归命令的覆盖范围，`pnpm --filter` 也取不到它；涉及它时进入该目录单独安装依赖，按其独立 README、`package.json` 和现有构建方式处理，并单独验证。
- 子工程可用的脚本差异很大，多数包只有 `dev` 和一组 `public:*`。安排验证前先读目标包 `package.json` 的 `scripts`，不要假设 `lint`、`type-check`、`build` 存在。

## 实现原则

### 复用顺序

1. 当前子工程已有业务组件、hooks/composables、utils、store 和 API 封装。
2. `common-lib`、`bk-pipeline`、`bk-permission`、`locale` 等仓库内共享能力。这些共享层各有固定的引用方式（相对路径、`exports` 子路径、指令、别名），照抄现有调用点，不要凭包名猜。
3. 当前子工程已经安装并实际使用的蓝鲸组件库。
4. 自建组件或工具。

只有前三类都不满足时才新增抽象。不要为了单次使用创建通用层，也不要为使用某个组件主动引入新的 UI 库。各层的判断依据和组件库差异见 [`STYLE_GUIDE.md`](./STYLE_GUIDE.md)。

### 编码与交互

- 以目标子工程的 ESLint、Stylelint、TypeScript、EditorConfig 和相邻代码为准，不凭个人偏好统一格式。仓库里有两套相反的格式基线，跨子工程搬代码要按目标子工程重排，细节见 [`STYLE_GUIDE.md`](./STYLE_GUIDE.md)。
- 沿用现有 router、store、request、权限和消息反馈封装。请求分层、权限落地和导航壳桥接能力（URL 同步、页面标题、全局提示、离开确认）各有既定写法，见 [`STYLE_GUIDE.md`](./STYLE_GUIDE.md)，不要各写一套。
- 用户可见文案不落在子工程内，而是集中在共享的 `locale/<模块>/` 目录，由子工程反向导入。新增文案属于跨子工程改动，在交付说明中单独点出。
- 异步交互按需求覆盖 loading、disabled、防重复提交、空态、错误反馈和恢复路径。
- 涉及权限或只读页面时，同时检查可见性和可操作性，不只隐藏入口。
- 涉及列表、表单、弹窗和长文本时，检查空数据、超长内容、校验失败、关闭/取消和重复操作等边界。
- 样式修改注意构建链路对样式语言的硬限制，避免全局选择器泄漏和无依据的固定尺寸。仓库当前几乎没有无障碍与响应式基线，不要求新代码补齐全站，但新增交互不要破坏键盘可达性，也不要写死会在窄屏溢出的固定尺寸。
- 不静默改变接口契约。若接口字段或返回状态不确定，先从现有 API 封装、类型和调用方确认；仍无法确认时明确标注假设。
- 目录与命名在仓库内多派并存（如 `store` / `stores`，`hook` / `hooks` / `composables`），跟随当前子工程即可，不要顺手统一已有代码。

## 验证流程

验证应与改动风险匹配，并优先使用目标子工程 `package.json` 已声明的脚本。

```bash
cd src/frontend
pnpm --filter <package-name> run <script-name>
```

`--filter` 匹配的是 `package.json` 里的 `name` 字段，不是目录名（例如 `bk-pipeline` 的包名是 `bkui-pipeline`，`locale` 的包名是 `common-locale`）。`devops-repo` 不在工作区内，须进它自己的目录单独安装依赖后运行它自己的脚本。

- 先读目标包 `package.json` 的 `scripts` 确认命令是否存在，再安排验证。webpack 系子工程的构建入口是 `public:*`，ESLint 由构建链路内联执行但只覆盖 `.js` 与 `.vue`；只有个别子工程有独立的 `lint` / `type-check`。
- 当前没有可用的单元测试基线（仓库里只有两个脚手架占位用例，业务源码下没有测试）。验证以 lint、类型检查、目标子工程构建和浏览器实测为主；确需新增测试时放进已有测试目录并沿用其已配置的框架，不引入新测试工具链。
- 纯文案或局部样式：检查相关 diff，并执行目标文件适用的 lint/stylelint；有页面条件时进行浏览器检查。
- 组件或业务逻辑：按已确认存在的命令执行校验与目标子工程构建，并在浏览器里跑通验收路径。
- 路由、状态管理、请求链路或公共组件：验证主要成功路径、失败路径及至少一个关键边界场景。
- 声明了专用脚本的子工程（如 `devops-flow`、`devops-repo`）优先使用自身脚本，不用前端根 ESLint 替代；`test:e2e` 只在交互链路确有需要时运行。
- `bk-pipeline` 的公共逻辑改动至少执行受影响的 Vue 2/Vue 3 构建；无法覆盖其中一端时必须说明。
- `devops-pipeline` 与 `devops-nav` 依赖 DLL 产物且该产物不入库，首次本地验证前先跑这两个子工程自己的 `dll` 脚本再启 `dev`。
- 不默认执行全仓递归构建；只有共享依赖、构建配置或跨多个子工程的改动才扩大验证范围，且走前端根的 `public` 脚本而不是递归 `build`。
- 不使用会自动修复大量无关文件的命令。构建链路和部分脚本自带 `--fix`，运行后必须复查 `git status --short` 与 `git diff`，还原与本任务无关的自动改写，尤其是共享目录。
- 依赖缺失、环境限制或命令耗时导致验证未完成时，记录未执行的命令、原因和剩余风险，不把未运行说成通过。
- 编译成功不等于功能验证通过；涉及页面行为的改动要在本地实际跑通验收路径。CI 只跑安装与构建，不跑 lint、类型检查和测试，CI 绿灯不能替代本地验证。

各命令的具体取值、DLL 与全量构建入口、`--fix` 影响面和 CI 覆盖面见 [`STYLE_GUIDE.md`](./STYLE_GUIDE.md) 的「验证」。

## 提交前检查

1. 再次查看 `git status --short` 和相关 `git diff`，确认只包含当前任务改动。
2. 确认没有调试代码、临时文件、无关锁文件变化、敏感信息或被意外格式化的文件。
3. 汇总已执行的验证及结果，说明未覆盖项和风险。
4. 需要提交时，按前端 `commitlint.config.js` 的规则自行核对提交信息。仓库提供了 `.husky/commit-msg` 与 `.husky/pre-commit` 脚本，但常规安装不会自动挂上，提交前自行确认钩子是否生效，不要把本地没报错当成已通过校验。

本仓库的提交信息格式遵循根目录 `CONTRIBUTING.md`，subject 必须以 `issue #<id>` 结尾：

```text
<type>: <简洁概要> issue #<id>
```

可用 type 以当前 commitlint 配置为准，包括 `feature`、`feat`、`bug`、`fix`、`bugfix`、`refactor`、`perf`、`test`、`docs`、`info`、`format`、`merge`、`depend`、`chore`、`del`。若当前工作项没有对应 ID，提交前先向负责人确认，不编造 ID。

部分内部前端工程树历史上要求 subject 以 `--story=<ID>`、`--bug=<ID>` 或 `--task=<ID>` 结尾，与本仓库规则不兼容。跨树搬运改动时按各自生效的钩子改写 subject，不要为此修改本仓库的 `CONTRIBUTING.md` 或 `commitlint.config.js`；细节见 [`STYLE_GUIDE.md`](./STYLE_GUIDE.md) 的「提交信息与校验」。

## 交付说明

完成后简洁说明：

- 改了什么，以及涉及哪个子工程。
- 执行了哪些验证，结果如何。
- 哪些验证未执行及原因。
- 是否存在接口依赖、兼容性风险或后续事项。
