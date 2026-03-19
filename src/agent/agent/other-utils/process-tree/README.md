# 诊断工具集：`agent-util`

`other-utils/process-tree` 目录下的这个工具现在统一对外叫做 **`agent-util`**。

它通过不同子命令承载两类能力：

- `tree`：Windows 进程树 / 阻塞诊断
- `shell-check`：Linux / macOS login shell 启动链路检测

其中 `shell-check` 主要用于排查 Agent 在 Unix 上通过两层脚本启动 worker 时，是否会被用户的 `profile` / `rc` 配置干扰，例如 `exec zsh`、`exit`、`read`、`tmux` 等语句。

## 子命令概览

| 子命令 | 平台 | 用途 |
|---|---|---|
| `tree` | Windows | 构建进程树、检测阻塞进程、扫描管道句柄继承并生成 HTML 报告 |
| `shell-check` | Linux / macOS | 真实复现 Agent 的 login shell 启动链路，检测 shell 初始化文件是否吞掉真正脚本 |

## 编译

### 直接编译

#### Windows

```bash
go build -o agent-util.exe .
```

#### Linux / macOS

```bash
go build -o agent-util .
```

### 使用编译脚本

目录里新增了 `build.sh`，会一次编译出 `linux` 和 `windows` 两个版本：

```bash
bash build.sh
```

默认产物：

- `dist/agent-util_linux_amd64`
- `dist/agent-util_windows_amd64.exe`

## 使用方法

### `tree` 子命令（Windows）

```bash
agent-util.exe tree -buildid <build-id>
agent-util.exe tree -buildid <build-id> -diag -pipe
agent-util.exe tree -buildid <build-id> -name node.exe
agent-util.exe tree -html -buildid <build-id>
```

为了兼容旧用法，Windows 下仍然支持：

```bash
agent-util.exe -buildid <build-id>
```

### `shell-check` 子命令（Linux / macOS）

```bash
./agent-util shell-check
./agent-util shell-check -shell /bin/bash
./agent-util shell-check -shell /bin/zsh -timeout 12s -verbose
./agent-util shell-check -keep -verbose
```

`shell-check` 会执行下面这条思路与 Agent 当前 Unix worker 启动方式一致的探测链路：

1. 生成真正的 `start` 脚本
2. 再生成一层 `prepare` 脚本
3. 用 login shell 执行 `prepare -> start`
4. 同时扫描常见 `profile` / `rc` 文件中的高风险语句

如果目标脚本没有真正执行到，工具会明确告诉你是：

- **超时卡住**
- **shell 提前退出**
- **脚本根本没被执行**
- **初始化文件中有明显可疑语句**

## `shell-check` 关注的问题类型

工具默认会重点检查常见初始化文件中的这几类语句：

- `exec ...`
- `exit` / `logout`
- `read ...`
- `stty ...`
- `tmux` / `screen` / `zellij`

其中最典型的问题就是：

```bash
exec zsh
```

如果这类语句出现在 `bash` 的登录初始化链路中，就可能导致 Agent 原本想执行的 `start.sh` 被新 shell 替换掉，最终 worker 起不来。

## `tree` 功能特性（Windows）

- 进程树可视化
- 管道句柄继承扫描
- 线程级阻塞诊断
- 逐进程管道句柄枚举
- 自动 `jstack` 抓取
- 交互式 HTML 报告

## 输出说明

### `tree`

- 控制台输出：彩色进程树、问题汇总、建议操作
- HTML 报告：`process-report_<timestamp>.html`

### `shell-check`

会输出：

- 最终解析到的 shell
- 实际执行的 `exec` 命令行
- 是否超时
- 是否真正执行到目标脚本
- 命中的初始化文件与可疑语句
- 建议处理方式

加上 `-verbose` 后，还会显示 `stdout` / `stderr` 以及临时脚本路径；加上 `-keep` 后，会保留临时探测文件，便于二次分析。

## 项目结构

```text
other-utils/process-tree/
├── build.sh                  # 一次构建 windows + linux 产物
├── main.go                   # Windows 入口 + tree 子命令
├── main_unix.go              # Unix 入口 + shell-check 子命令分发
├── shell_check_unix.go       # Unix shell 启动链路探测逻辑
├── shell_check_unix_test.go  # shell-check 单测
├── go.mod
├── go.sum
└── README.md
```

## 说明

- `tree` 依赖 Windows API、WMI 和管理员权限
- `shell-check` 设计目标是 **检测问题**，不是直接修改用户 shell 配置
- 如果你想让它尽量保留现场，建议使用 `-keep -verbose`
