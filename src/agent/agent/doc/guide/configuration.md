# Agent配置参考

本文档详细说明 BK-CI Agent 的所有配置项，包括核心配置文件 `.agent.properties`、辅助文件和环境变量开关。

## .agent.properties

Agent 的核心配置文件，使用 INI 格式（`key=value`），位于 Agent 安装目录根路径。Agent 启动时加载此文件，运行时会回写更新。

### 身份与连接（服务端下发，不可手动修改）

| 参数 | 含义 | 默认值 |
|------|------|--------|
| `devops.project.id` | 项目 ID | — |
| `devops.agent.id` | Agent 唯一标识 | — |
| `devops.agent.secret.key` | 鉴权密钥 | — |
| `landun.gateway` | 蓝盾流水线 API 网关地址 | — |
| `landun.fileGateway` | 蓝盾制品库 API 网关地址 | ） |
| `landun.env` | 环境类型标识 | — |

> 这些参数在首次安装时由服务端生成写入，是 Agent 的"身份证"。手动修改可能会导致 Agent 无法正常与服务端通信。

### 构建运行参数

| 参数 | 含义 | 可配置性 | 默认值 |
|------|------|---------|--------|
| `devops.parallel.task.count` | 普通构建最大并发数 | 节点页面配置 | 4 |
| `devops.docker.parallel.task.count` | Docker 构建最大并发数 | 节点页面配置 | 4 |
| `devops.slave.user` | 构建进程以哪个系统用户运行 | 本地可配 | 系统用户（仅linux） |
| `devops.agent.detect.shell` | 是否使用 `$SHELL` 环境变量指定的 shell | 本地可配 | `false`（使用 `/bin/bash`） |
| `devops.docker.enable` | 是否启用 Docker 构建功能 | 流水线勾选 使用docker构建 开启 | `false` |

**说明：**
- `devops.slave.user` 仅对 Linux/macOS 有效，指定构建进程运行的系统用户。如果设置为与 Agent 运行用户不同的用户，需要确保 Agent 有切换用户的权限。

### JDK 路径

| 参数 | 含义 | 默认值 |
|------|------|--------|
| `devops.agent.jdk17.dir.path` | JDK 17 安装路径 | `<安装目录>/jdk17` |
| `devops.agent.jdk.dir.path` | JDK 8 安装路径 | `<安装目录>/jdk` 或 `<安装目录>/jre` |

通常不需要手动修改。如果需要使用自定义 JDK，可以指定绝对路径。

### 运维参数

| 参数 | 含义 | 可配置性 | 默认值 |
|------|------|---------|--------|
| `devops.agent.request.timeout.sec` | HTTP 请求超时（秒） | 本地可配 | `5` |
| `devops.agent.logs.keep.hours` | 日志文件保留时长（小时） | 本地可配 | `96`（4 天） |
| `devops.agent.ignoreLocalIps` | 忽略的本地 IP（逗号分隔） | 本地可配 | `127.0.0.1` |
| `devops.agent.collectorOn` | 是否启用主机指标采集 | 本地可配 | `true` |
| `devops.language` | Agent 界面/CLI 语言 | 本地可配 | `zh_CN` |

**说明：**
- `devops.agent.ignoreLocalIps` 用于过滤 VPN 软件产生的虚拟网卡 IP，避免多台机器上报相同的 IP 地址。
- `devops.language` 支持标准语言标签，如 `zh_CN`（中文）、`en_US`（英文）。

### 代理设置

| 参数 | 含义 | 可配置性 |
|------|------|---------|
| `HTTP_PROXY` | HTTP 代理地址 | 本地可配 + 心跳同步 |
| `HTTPS_PROXY` | HTTPS 代理地址 | 本地可配 + 心跳同步 |
| `NO_PROXY` | 不使用代理的地址列表 | 本地可配 + 心跳同步 |

代理设置同时会被注入到构建进程的环境变量中。服务端也可以通过心跳下发代理配置，会与本地配置合并。

### 扩展功能

| 参数 | 含义 | 可配置性 | 默认值 |
|------|------|---------|--------|
| `devops.imagedebug.portrange` | 镜像调试端口范围 | 本地可配 | `30000-32767` |
| `devops.pipeline.enable` | 启用 Pipeline 功能 | 服务端下发 | `false` |
| `devops.mcp.server.port` | MCP Server 监听端口 | 自动分配并持久化 | `0`（未启用） |

## 辅助文件

除 `.agent.properties` 外，Agent 还使用以下辅助文件：

### .cert — 自定义 TLS 证书

| 项目 | 说明 |
|------|------|
| 路径 | `<安装目录>/.cert` |
| 格式 | PEM 格式的 CA 证书 |
| 用途 | 当服务端使用自签名证书或企业内部 CA 签发的证书时，将 CA 证书放入此文件 |
| 效果 | Agent 启动时加载到 Go 默认 HTTP 传输层的 TLS 信任池中 |
| 可选 | 如果服务端使用公开 CA 签发的证书（如 Let's Encrypt），不需要此文件 |

### .install_type — 安装模式标记

| 项目 | 说明 |
|------|------|
| 路径 | `<安装目录>/.install_type` |
| 格式 | 纯文本，内容为模式名称（如 `SERVICE`、`USER`、`DIRECT`、`LOGIN`、`BACKGROUND`） |
| 用途 | 记录当前的安装模式，`start`/`stop` 命令据此选择启停方式 |
| 管理 | 由 `install`/`uninstall` 命令自动管理，不建议手动修改 |

### .env — 环境变量快照（Linux/macOS）

| 项目 | 说明 |
|------|------|
| 路径 | `<安装目录>/.env` |
| 格式 | `KEY=VALUE` 格式，每行一个，`#` 开头为注释 |
| 用途 | 解决 systemd/launchd 服务模式下构建进程环境变量缺失的问题 |
| 采集时机 | `install` 和 `start` 命令执行时，从当前 shell 环境快照 |
| 加载时机 | daemon 启动时，由 `envs.LoadEnvFiles()` 读取并通过 `os.Setenv()` 设置到进程环境 |
| 采集变量 | `JAVA_HOME`、`GRADLE_HOME`、`GOROOT`、`GOPATH`、`NVM_DIR`、`CARGO_HOME` 等常用开发变量 |
| 可编辑 | 支持手动添加自定义变量，`stop` + `start` 后生效（手动添加的变量不会被自动采集覆盖） |
| Windows | 不需要（Windows 已有注册表轮询机制自动同步环境变量） |

### .path — PATH 快照（Linux/macOS）

| 项目 | 说明 |
|------|------|
| 路径 | `<安装目录>/.path` |
| 格式 | 单行文本，完整的 PATH 值 |
| 用途 | 保存用户交互式 shell 的 PATH，服务模式下恢复 |
| 采集时机 | 与 `.env` 相同 |
| 加载时机 | daemon 启动时，与当前 PATH 去重合并（`.path` 中的路径优先） |
| Windows | 不需要 |

## 构建环境变量机制

Agent 执行构建任务时，会为构建进程组装一套环境变量。不同平台使用不同的机制来确保构建进程能获取到完整的环境：

### 各平台机制对比

| 平台 | 机制 | 实时性 | 更新方式 |
|------|------|--------|---------|
| **Windows** | 注册表轮询（每 3 秒） | 自动刷新，无需重启 | 安装软件或修改系统环境变量后，3 秒内自动生效 |
| **Linux** | `.env`/`.path` 文件快照 | 需要 `stop` + `start` | 在完整环境的终端中执行 `stop` + `start` |
| **macOS** | `.env`/`.path` 文件快照 | 需要 `stop` + `start` | 在完整环境的终端中执行 `stop` + `start` |

### Linux/macOS：`.env` 和 `.path` 文件

当 Agent 以 systemd 服务（Linux）或 launchd 服务（macOS）运行时，daemon 进程继承的是系统提供的极简环境（通常只有基础 PATH），用户在 `.bashrc`/`.zshrc` 中配置的开发工具路径（如 nvm、pyenv、Go）不会自动生效。

`.env`/`.path` 文件机制解决了这个问题：

1. `install` 或 `start` 命令在用户终端中执行时，Agent 自动快照当前 shell 的 PATH 和常用开发变量
2. daemon 启动时读取这些文件，合并到进程环境
3. 构建进程通过 `envs.Envs()` 继承这些环境变量

**自动采集的变量列表：**

`LANG`、`LC_ALL`、`JAVA_HOME`、`JRE_HOME`、`ANT_HOME`、`M2_HOME`、`MAVEN_HOME`、`ANDROID_HOME`、`ANDROID_SDK_ROOT`、`GRADLE_HOME`、`NVM_DIR`、`NVM_BIN`、`LD_LIBRARY_PATH`、`GOPATH`、`GOROOT`、`PYENV_ROOT`、`CARGO_HOME`、`RUSTUP_HOME`、`NODE_PATH`、`PYTHONPATH`

### 何时需要手动更新环境变量

以下操作后需要在终端中执行 `stop` + `start` 以刷新 `.env`/`.path` 快照：

- 安装了新的开发工具（如 nvm、pyenv、Go、Rust）
- 修改了 `.bashrc`/`.zshrc`/`.profile` 中的 PATH 或环境变量
- 升级了已有工具的版本（如 nvm 切换了 Node.js 版本）

```bash
cd <agent安装目录>
./devopsAgent stop
./devopsAgent start   # 自动重新快照当前 shell 环境
```

### Windows：注册表自动轮询

Windows 平台不使用 `.env`/`.path` 文件。Agent 每 3 秒自动轮询系统注册表，获取最新的环境变量：

- 用户级：`HKEY_CURRENT_USER\Environment`
- 系统级：`HKEY_LOCAL_MACHINE\...\Session Manager\Environment`

安装新软件或修改环境变量后，无需重启 Agent，3 秒内自动生效。详见 [Windows 平台指南](platform-windows.md)。

### Docker 构建不受影响

Docker 构建的容器环境变量由 Agent 通过 `-e` 参数显式传递（`project_id`、`agent_id` 等），不继承宿主机的 `.env`/`.path` 文件内容。

## 环境变量开关

以下环境变量可以通过操作系统环境变量或 BK-CI 服务端下发来控制 Agent 行为：

### 进程管理

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `DEVOPS_AGENT_ENABLE_EXIT_GROUP` | 构建结束后杀死整个进程组（Unix） | `false`（macOS arm64 默认 `true`） |
| `DEVOPS_AGENT_CLOSE_FD_INHERIT` | 构建进程不继承 Agent 的文件描述符（Unix） | `false` |
| `DEVOPS_AGENT_ENABLE_NEW_CONSOLE` | 构建进程使用新控制台窗口（Windows） | `false` |
| `DEVOPS_AGENT_TIMEOUT_EXIT_TIME` | 连续 HTTP 超时 N 次后 Agent 自动退出并由 Daemon 重启 | — |

### 容器运行时

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `DEVOPS_AGENT_CONTAINER_RUNTIME` | 容器运行时命令（`docker` 或 `podman`） | `docker` |
| `DEVOPS_AGENT_DOCKER_CAP_ADD` | 启动 Docker 容器时额外添加的 Linux capabilities | 空 |

### MCP Server

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `DEVOPS_AGENT_ENABLE_MCP` | 启用 MCP Server（在 127.0.0.1 上暴露 Agent 信息给 AI 工具） | `false` |

## 配置的优先级

对于同时支持本地配置和服务端下发的参数：

1. Agent 启动时从 `.agent.properties` 文件加载
2. 运行期间每次心跳会收到服务端的最新值
3. 服务端值会覆盖本地值并回写到文件

如果需要强制使用本地值，需要在服务端对应位置也做相应修改。

