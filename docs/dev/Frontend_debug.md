# BK-CI 前端本地调试指南

本文档用于说明 BK-CI 前端需求开发完成后，如何启动对应的本地调试服务，并通过 Chrome 代理和 whistle 将线上或测试环境页面的前端资源映射到本地进行验证。

## 0. 非前端同学快速启动

如果只是为了验证一个前端改动，可以先按下面的顺序走，不需要理解所有构建细节。

1. 确认要验证的页面属于哪个前端工程。
   - URL 或 iframe 资源路径中出现 `/pipeline/`，通常是 `devops-pipeline`。
   - URL 或静态资源中出现 `/console/`，通常是主工程 `devops-nav`。
   - 不确定时，先看本文档的端口表，或在 Chrome Network 中看 JS/CSS 的路径前缀。
2. 安装或刷新前端依赖。每个前端工作开始前都应确认执行过这一步。

```bash
cd src/frontend
pnpm install
```

3. 启动 whistle，并在 Chrome 的 Switchy Sharp 中切到 `auto switch`。
4. 用 Nx 启动目标工程。

```bash
cd src/frontend

# 第一次调试 pipeline 时先构建 DLL
pnpm nx dll devops-pipeline

# 启动 pipeline 本地服务
pnpm nx dev devops-pipeline
```

5. 确认 whistle 中当前服务的静态资源映射已启用，例如 `devops-pipeline` 对应本地 `8006`。
6. 打开 dev 环境真实页面验证，不要直接访问 `localhost`。
7. 在 Chrome Network 中确认静态资源已经命中本地服务。
8. 走一遍本次需求的用户操作，并在不刷新页面的情况下确认页面状态符合预期。

最容易漏掉的是依赖安装、Switchy Sharp 和本地 bundle 命中检查。只要 Switchy Sharp 没切到 `auto switch`，页面就会继续加载远端 bundle，本地改动不会生效。

## 1. 基本原则

### 1.1 环境与依赖安装

前端工程依赖 `Node.js`、`pnpm`、`Nx` 和 whistle。首次拉取仓库、切换分支、更新代码后发现 `pnpm-lock.yaml` 变化、或本地 `node_modules` 异常时，都要先在 `src/frontend` 安装依赖。

```bash
node -v    # 需要 >= 22
pnpm -v    # 需要 >= 11

# 如本机没有 pnpm 或 whistle，可先全局安装
npm install -g pnpm
npm install -g whistle

cd src/frontend
pnpm install
```

注意：

- 依赖安装必须在 `src/frontend` 执行，不要进入某个子工程单独安装。
- 不要混用 `npm install`、`yarn install` 和 `pnpm install`。
- 如果本机 Node.js 或 pnpm 低于上面的版本要求，先升级运行时再执行 `pnpm install`。
- `pnpm install` 是每个前端工作开始前的必检步骤，不只是首次克隆时执行。

### 1.2 Nx 启动原则

BK-CI 前端位于 `src/frontend`，采用 monorepo + pnpm + Nx 管理。调试命令应优先在 `src/frontend` 下通过 Nx 执行，避免绕过子工程之间的依赖关系。

```bash
cd src/frontend

# 常规启动
pnpm nx dev <project-name>

# 等价写法
pnpm nx run <project-name>:dev
```

不建议默认进入子工程后直接执行 `pnpm run dev`。例如 `devops-pipeline` 依赖 `bk-pipeline`、`bk-permission` 等 workspace 包，Nx 会根据 `nx.json` 中的 `dev.dependsOn = ["^public:master"]` 先构建依赖项目的 `public:master` 产物。

## 2. 首次启动前构建 DLL

部分工程把公共依赖抽成了 DLL。首次启动、清理过产物、或遇到 `manifest.json`、`main.dll.js` 缺失时，需要先执行对应工程的 `dll` target。

```bash
cd src/frontend

# 以流水线为例
pnpm nx dll devops-pipeline
pnpm nx dev devops-pipeline
```

当前有独立 `dll` target 的工程包括：

| 工程 | DLL 说明 |
| --- | --- |
| `devops-pipeline` | 生成流水线工程依赖的 DLL 和 manifest |
| `devops-nav` | 生成主入口工程依赖的 DLL |
| `devops-stream` | 通过 `better-npm-run dll` 生成 Stream 依赖产物 |

如果 Nx target 无法识别，先检查对应工程 `package.json` 的 `scripts.dll` 是否存在，再按实际脚本处理。

## 3. 两类前端接入方式

`devops-nav` 是主入口，其他子服务通常以两种方式接入，最终以服务配置中的 `inject_type` 为准。

| 类型 | 机制 | 调试关注点 |
| --- | --- | --- |
| `iframe` | `devops-nav` 根据服务配置中的 `iframe_url` 嵌入子服务页面 | 将 iframe 页面或静态资源映射到本地服务 |
| `amd` | `devops-nav` 根据 `js_url`、`css_url` 动态加载子服务 entry 资源 | 将动态加载的 JS/CSS 资源映射到本地服务 |

`devops-nav` 内部会在初始化服务列表时生成 `serviceMap` 和 `iframeRoutes`；进入路由时，`amd` 服务会动态注入 `js_url/css_url`，`iframe` 服务会进入 `IFrame.vue` 并使用 `iframe_url`。

## 4. 代理分工

本地调试同时涉及两类代理，二者不要混在一起理解。

### 4.1 静态资源代理

Chrome 代理和 whistle 负责把页面上请求的前端静态资源映射到本地 dev server。Webpack 子服务在开发模式下会把非 `devops-nav` 工程的 `publicPath` 指向类似下面的地址：

```text
//dev-static.devops.qq.com/<service>/
```

因此调试某个服务时，需要在 SwitchyOmega/SwitchySharp 和 whistle 中启用对应的端口映射。例如调试 `devops-pipeline` 时，本地服务端口是 `8006`，静态资源请求需要映射到该端口。

### 4.2 Chrome 中启用 Switchy Sharp

只启动 whistle 不够，还需要在 Chrome 的 Switchy Sharp 扩展中打开代理开关，并切换到 `auto switch` 选项。否则页面会继续请求远端静态资源，本地 dev server 即使编译成功也不会生效。

首次配置时，需要在 Switchy Sharp / SwitchyOmega 中导入或创建团队调试配置。已有配置文件时，可直接导入以下 PAC；没有配置文件时，按下方核心行为手动创建同名 profile。

| 文件 | 用途 |
| --- | --- |
| `OmegaProfile_auto_switch.pac` | 日常调试时选择的自动切换配置 |
| `OmegaProfile_proxy.pac` | 将请求转发到本机 whistle，端口 `127.0.0.1:8899` |
| `OmegaProfile_iOA .pac` | 公司内网代理配置，端口 `127.0.0.1:12639` |

导入后，核心行为应满足：

```text
auto switch:
  dev-static.devops.qq.com -> proxy
  其他域名 -> iOA

proxy:
  localhost / 127.0.0.1 / ::1 -> DIRECT
  其他请求 -> PROXY 127.0.0.1:8899

iOA:
  PROXY 127.0.0.1:12639
```

日常调试只需要选择 `auto switch`。不要直接选 `proxy`，否则所有非本机请求都会进 whistle，可能影响其他内网页面；也不要选 `iOA`，否则 `dev-static.devops.qq.com` 不会进入本地映射。

日常验证方式：

1. 打开 Chrome 右上角 Switchy Sharp 扩展。
2. 选择 `auto switch`。
3. 打开目标页面后进入 DevTools Network。
4. 刷新页面，检查当前服务的 JS/CSS 请求是否已通过代理命中本地端口。

以 `devops-pipeline` 为例，正常情况下 `dev-static.devops.qq.com/pipeline/...` 下的资源应被映射到本地 `8006`。如果 iframe 中仍加载远端指纹资源，例如 `/pipeline/pipeline.<hash>.min.js` 来自远端环境，说明 Switchy Sharp 未切到 `auto switch`，或 whistle 中对应服务端口映射未启用。

### 4.3 whistle 规则和 Values

Switchy Sharp 只负责把 `dev-static.devops.qq.com` 转发给本机 whistle。真正把不同服务映射到本地端口的是 whistle 规则。

首次配置时：

1. 启动 whistle。

```bash
w2 start
```

2. 打开 `http://127.0.0.1:8899`。
3. 在 Rules 中导入团队规则文件 `whistle_proxy_map.txt`。
4. 在 Values 中导入 `CORS.txt`。
5. 按当前调试服务启用对应端口映射，例如 `devops-pipeline -> 8006`。

如果没有现成 rules 文件，手工配置时要达到下面的映射效果：

```text
dev-static.devops.qq.com/<service>/... -> http://127.0.0.1:<port>/<service>/...
```

例如调试 `devops-pipeline` 时，`dev-static.devops.qq.com/pipeline/...` 最终应落到本地 `8006` 的 `/pipeline/...` 资源；调试 `devops-nav` 时，主工程静态资源最终应落到本地 `8080`。

`CORS.txt` 中常用 Values 包括：

```json
{
  "CORS": {
    "origin": "https://dev.devops.qq.com",
    "methods": "POST, GET",
    "headers": "x-csrftoken",
    "credentials": true,
    "maxAge": 300000
  },
  "localNAV": {
    "origin": "https://local-devops.qq.com:8080",
    "methods": "POST, GET",
    "headers": "x-csrftoken",
    "credentials": true,
    "maxAge": 300000
  }
}
```

调试时只打开当前服务需要的映射，避免多个服务共用端口或误把其他服务流量映射到当前 dev server。

### 4.4 接口代理

由于页面通过代理访问本地服务，本地 origin 下直接请求接口通常无法复用线上环境能力，页面可能因为接口不通而无法访问。需要在本地 dev server 中将 `/ms` 开头的接口代理到开发环境：

```text
https://dev.devops.qq.com/ms
```

Webpack 示例：

```js
devServer: {
    proxy: {
        '/ms': {
            target: 'https://dev.devops.qq.com',
            changeOrigin: true,
            secure: false
        }
    }
}
```

Vite 示例：

```js
server: {
    proxy: {
        '/ms': {
            target: 'https://dev.devops.qq.com',
            changeOrigin: true,
            secure: false
        }
    }
}
```

`devops-nav` 当前已经配置了 `/ms -> https://dev.devops.qq.com`。其他工程调试前需要按实际请求方式检查，如果页面访问失败，优先看 Network 中 `/ms` 请求是否命中 `dev.devops.qq.com`。

## 5. HTTPS 与 `.qq.com` 本地域名

需要使用 `qq.com` 登录态的页面，本地调试域名必须以 `.qq.com` 结尾。不要使用 `localhost`、`127.0.0.1`、`local-stream.com` 作为需要登录态的调试入口。

推荐默认域名：

```text
local.devops.qq.com
```

特殊工程可使用更明确的域名，例如：

```text
local-ai.devops.qq.com
```

如果本机未解析域名，需要配置 hosts：

```text
127.0.0.1 local.devops.qq.com
127.0.0.1 local-ai.devops.qq.com
```

### 5.1 使用 mkcert 生成证书

```bash
mkcert -install

cd src/frontend
mkcert \
  -key-file local.devops.qq.com.pem \
  -cert-file local.devops.qq.com-crt.pem \
  local.devops.qq.com
```

仓库中已有 `src/frontend/local.devops.qq.com.pem` 和 `src/frontend/local.devops.qq.com-crt.pem`。如果本机浏览器不信任证书，重新执行 `mkcert -install` 并按本机环境重新生成证书。

### 5.2 在 dev server 中启用 HTTPS

Webpack 示例：

```js
const fs = require('fs')
const path = require('path')

devServer: {
    host: 'local.devops.qq.com',
    allowedHosts: 'all',
    server: {
        type: 'https',
        options: {
            key: fs.readFileSync(path.join(__dirname, '../local.devops.qq.com.pem')),
            cert: fs.readFileSync(path.join(__dirname, '../local.devops.qq.com-crt.pem'))
        }
    }
}
```

Vite 示例：

```js
import fs from 'node:fs'
import path from 'node:path'

export default {
    server: {
        host: 'local.devops.qq.com',
        https: {
            key: fs.readFileSync(path.resolve(__dirname, '../local.devops.qq.com.pem')),
            cert: fs.readFileSync(path.resolve(__dirname, '../local.devops.qq.com-crt.pem'))
        }
    }
}
```

如果某个工程仍使用非 `.qq.com` host，需要先改成本地 `.qq.com` 域名再调试依赖登录态的页面。

## 6. 常用工程端口

以下端口来自当前工程配置。端口冲突时，以实际 `webpack.config.js`、`vite.config.*`、`.bk.development.env` 为准。

| 工程 | 端口 | 启动命令 |
| --- | ---: | --- |
| `devops-nav` | 8080 | `pnpm nx dev devops-nav` |
| `devops-pipeline` | 8006 | `pnpm nx dll devops-pipeline && pnpm nx dev devops-pipeline` |
| `devops-environment` | 8001 | `pnpm nx dev devops-environment` |
| `devops-codelib` | 8002 | `pnpm nx dev devops-codelib` |
| `devops-quality` | 8002 | `pnpm nx dev devops-quality` |
| `devops-vs` | 8002 | `pnpm nx dev devops-vs` |
| `devops-atomstore` | 8003 | `pnpm nx dev devops-atomstore` |
| `devops-ticket` | 8004 | `pnpm nx dev devops-ticket` |
| `devops-turbo` | 8008 | `pnpm nx dev devops-turbo` |
| `devops-experience` | 8009 | `pnpm nx dev devops-experience` |
| `devops-platform` | 8010 | `pnpm nx dev devops-platform` |
| `devops-metrics` | 3000 | `pnpm nx dev devops-metrics` |
| `devops-permission` | 8007 | `pnpm nx dev devops-permission` |
| `devops-manage` | 5000 | `pnpm nx dev devops-manage` |

`devops-codelib`、`devops-quality`、`devops-vs` 默认都使用 `8002`，不要同时启动。需要同时调试时，先调整其中一个工程的端口，并同步修改 whistle 映射。

## 7. 调试流程

1. 确认 whistle 已启动。

```bash
w2 start
```

2. 在 Chrome 的 Switchy Sharp 扩展中打开代理开关，并切换到 `auto switch`。
3. 进入 `src/frontend`，安装或刷新依赖。

```bash
cd src/frontend
pnpm install
```

4. 首次调试有 DLL 的工程时先构建 DLL。

```bash
cd src/frontend
pnpm nx dll devops-pipeline
```

5. 启动对应工程的 dev target。

```bash
pnpm nx dev devops-pipeline
```

6. 在 whistle 中启用当前服务的静态资源端口映射。
7. 如果服务需要接口，确认 dev server 已将 `/ms` 代理到 `https://dev.devops.qq.com`。
8. 如果页面依赖登录态，确认访问入口或映射后的页面仍处在 `.qq.com` 域名下。
9. 在 Chrome Network 中验证：
   - JS/CSS 等静态资源命中本地端口。
   - `/ms` 接口请求命中 `https://dev.devops.qq.com/ms`。
   - 页面没有证书拦截、跨域失败、登录态丢失或 HMR 连接失败。

## 8. 如何确认本地代码真的生效

调试时不要只看 dev server 是否 `compiled successfully`。这只能证明本地服务编译成功，不能证明 Chrome 页面已经加载了本地资源。

需要同时确认两件事：

1. 终端中本地 dev server 输出了当前工程的新 bundle。

```text
asset pipeline.<hash>.min.js ... [emitted]
webpack ... compiled successfully
```

2. Chrome 页面实际加载的 iframe 或动态入口中也出现了同一个工程的新 bundle。

以 `devops-pipeline` 为例：

1. 打开 Chrome DevTools Network。
2. 勾选 Disable cache。
3. 刷新页面。
4. 搜索 `pipeline.` 或 `/pipeline/`。
5. 确认 `pipeline.<hash>.min.js`、CSS、hot-update 等资源来自本地映射，而不是远端环境。

如果 iframe 中仍然加载远端指纹资源，例如：

```text
/pipeline/pipeline.<remote-hash>.min.js
```

常见原因是：

- Switchy Sharp 没有切到 `auto switch`。
- whistle 中当前服务的端口映射没有启用。
- 资源路径前缀和 whistle 规则不一致，例如页面请求 `/pipeline/`，但只配了其他服务路径。
- Chrome 缓存未禁用，仍使用旧资源。

本次模板计数问题的经验是：页面删除操作确实成功，但在未切到 `auto switch` 前，Chrome 仍加载远端 `pipeline` bundle，导致本地修复看起来“没有生效”。因此每次开始验证前，都先做本节的本地 bundle 命中检查。

## 9. 开发过程中的自动验证

### 9.1 保存代码后先看编译状态

Webpack/Vite dev server 会在保存后重新编译。继续点页面前，先确认终端没有编译错误。

```text
compiled successfully
```

如果终端出现 loader、ESLint、TypeScript、Sass 或模块解析错误，先修编译，不要继续浏览器验证。

### 9.2 提交前的最小静态校验

在 `src/frontend` 下对改动文件跑最小有效校验。以本次 `devops-pipeline` 模板页改动为例：

```bash
cd src/frontend

./node_modules/.bin/eslint \
  devops-pipeline/src/views/Template/List/index.vue \
  devops-pipeline/src/views/Template/TemplateGroupAside.vue \
  devops-pipeline/src/views/Template/index.vue \
  devops-pipeline/src/views/Template/useTemplateCountRefresh.js
```

回到仓库根目录检查空白和文件尾问题：

```bash
cd ../..
git diff --check
```

如果改动范围更大，优先使用目标工程已有的 lint、test、typecheck 或 build target。可以先查看工程配置：

```bash
cd src/frontend
pnpm nx show project <project-name>
```

### 9.3 回归验证模板

每个需求至少写清楚下面四项，方便自己和评审人复验。

```text
验证页面：
验证数据：
操作路径：
预期结果：
```

例如本次模板计数修复：

```text
验证页面：
https://dev.devops.qq.com/console/pipeline/_lockiechen_personal/list/template/allTemplate?mode=all

验证数据：
临时模板 codex-fix-<timestamp>

操作路径：
新建模板 -> 回到模板列表 -> 删除模板 -> 不刷新页面观察侧边计数

预期结果：
列表中临时模板消失，tab 全部/自定义变为 0，左侧全部模板/流水线模板也同步变为 0。
```

### 9.4 浏览器验证清单

每次说“功能已验证”前，至少确认：

- 页面 URL 和项目符合预期。
- iframe 或动态入口加载的是本地 bundle。
- Network 中静态资源命中本地端口。
- `/ms` 接口命中 `https://dev.devops.qq.com/ms`，没有登录态或跨域失败。
- Console 没有本次改动引入的 error。
- 操作前后的关键 UI 状态有明确对比。
- 涉及删除、创建、编辑等数据操作时，使用临时数据并在验证后清理。

### 9.5 自动化或半自动化验证建议

对于纯逻辑或状态同步问题，可以补一个最小脚本或单测，先让旧实现失败，再让修复后通过。临时脚本不要提交到仓库，除非它能沉淀为正式测试。

适合自动化的场景：

- 某个函数的输入输出稳定。
- 某个 composable、store action、工具函数有明确状态变化。
- 某个修复可以通过静态结构断言覆盖，例如“列表删除后必须调用刷新计数函数”。

仍需要浏览器验证的场景：

- 依赖 Chrome 登录态。
- 依赖 Switchy Sharp/whistle 资源代理。
- 涉及 iframe、动态 entry、HMR 或接口代理。
- 需要确认真实页面视觉状态和交互状态。

建议采用“自动校验 + 浏览器回归”组合：

1. 先跑最小静态或单测，证明代码结构没有退化。
2. 再启动 dev server，确认编译成功。
3. 再通过 Chrome 真实页面验证用户路径。
4. 最后检查 Console 和 Network。

## 10. 常见问题

### 页面空白或资源 404

先确认 whistle 中当前服务的静态资源映射是否启用，并检查资源路径是否与服务目录一致，例如 `/pipeline/`、`/store/`、`/ticket/`。

### `/ms` 接口失败

检查 dev server 的 proxy。静态资源能映射到本地不代表接口可用，`/ms` 仍需要回到 `https://dev.devops.qq.com`。

### 登录态丢失

确认页面访问域名以 `.qq.com` 结尾，并且 HTTPS 证书被 Chrome 信任。不要使用 `localhost` 或 `127.0.0.1` 验证依赖登录态的页面。

### 启动时报 DLL manifest 缺失

先运行对应工程的 `dll` target，例如：

```bash
pnpm nx dll devops-pipeline
```

### 启动时依赖包代码不是最新

不要绕过 Nx 直接进入子工程启动。使用 `pnpm nx dev <project-name>`，让 Nx 根据依赖图先处理上游 workspace 依赖产物。

### 端口被占用

查看端口表中是否有服务共用端口。若临时修改端口，需要同步修改 Chrome/whistle 映射和 dev server 的 HMR 配置。

### 本地编译成功但页面行为没有变化

优先检查 Chrome 是否真的加载了本地 bundle：

- Switchy Sharp 是否是 `auto switch`。
- whistle 当前服务映射是否启用。
- Network 中当前工程 JS/CSS 是否命中本地端口。
- iframe 中的 entry bundle hash 是否和 dev server 终端输出一致。

不要在未确认本地 bundle 命中的情况下继续改代码，否则很容易把代理问题误判成代码问题。
