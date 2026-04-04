# macOS 构建环境变量缺失

## 现象

macOS 上安装的 Agent 执行构建任务时，构建进程中缺少用户的 shell 环境变量，例如：

- nvm 相关变量（`NVM_DIR`、`NVM_BIN`）缺失，导致 `node`/`npm` 命令找不到
- homebrew 路径不完整
- 自定义 PATH 条目丢失
- iTerm / oh-my-zsh / powerlevel10k 等终端环境变量缺失

而在终端中手动执行相同命令时一切正常。

## 原因

### 环境变量继承链

```
daemon 进程的环境
  → agent 进程（继承 daemon 的环境）
    → 构建进程（继承 agent 的环境 + login shell 加载的 profile）
```

构建进程的环境变量来自两部分：
1. **daemon 进程继承的环境** — 由「谁启动了 daemon」决定
2. **login shell 加载的 profile** — 构建脚本通过 `exec /bin/zsh -l` 运行，会 source `.zshenv` 和 `.zprofile`

### 关键点：非交互式 login shell 不会 source `.zshrc`

| 文件 | login shell | interactive shell | login + interactive |
|------|:-----------:|:-----------------:|:-------------------:|
| `.zshenv` | ✓ | ✓ | ✓ |
| `.zprofile` | ✓ | - | ✓ |
| `.zshrc` | - | ✓ | ✓ |
| `.zlogin` | ✓ | - | ✓ |

构建进程的 login shell 是**非交互式**的，所以只会 source `.zshenv` + `.zprofile`，**不会 source `.zshrc`**。

许多工具（如 nvm、pyenv、rbenv、conda）的初始化代码通常放在 `.zshrc` 中，因此不会被加载。

### daemon 启动方式的影响

| 启动方式 | 环境变量 | 场景 |
|---------|---------|------|
| 终端中手动启动 | 完整（继承终端所有变量） | `./devopsAgent start` 在 iTerm 中执行 |
| launchd 自动启动 | 极简（仅 PATH=/usr/bin:/bin:/usr/sbin:/sbin） | 系统重启后 launchd 读取 plist 拉起 |

可通过以下命令确认 daemon 的启动来源：

```bash
# 查看 launchd 给 daemon 的环境（如果由 launchd 管理）
launchctl print gui/$(id -u)/devops_agent_<AGENT_ID>
```

如果输出的 `default environment` 中 PATH 只有 `/usr/bin:/bin:/usr/sbin:/sbin`，说明 daemon 是被 launchd 以极简环境启动的。

## 解决方案

### 方案一：将环境配置从 `.zshrc` 移到 `.zprofile`（推荐）

这是最根本的解决方案。将 nvm、pyenv 等工具的初始化移到 `.zprofile`，这样非交互式 login shell 也能加载。

```bash
# ~/.zprofile（login shell 都会加载）

# homebrew
eval "$(/opt/homebrew/bin/brew shellenv)"

# nvm
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

# 其他需要在构建中使用的环境变量
export CUSTOM_VAR=value
```

修改后重启 agent：

```bash
cd <agent安装目录>
./devopsAgent stop
./devopsAgent start
```

### 方案二：在终端中重启 daemon

在已配置好环境的终端（如 iTerm）中重启 agent，daemon 会继承终端的完整环境：

```bash
cd <agent安装目录>
./devopsAgent stop
./devopsAgent start
```

> **注意**：此方案的效果会在系统重启后失效——launchd 会以极简环境重新拉起 daemon。

### 方案三：在流水线脚本中手动 source

在构建步骤的脚本开头显式加载 profile：

```bash
# 流水线 Bash 脚本
source ~/.zprofile 2>/dev/null
source ~/.zshrc 2>/dev/null  # 可能会有交互式判断导致部分内容跳过

# 或者只加载需要的工具
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

node --version
npm install
```

## 诊断方法

在流水线中添加一个 Bash 脚本步骤，打印环境信息用于对比：

```bash
echo "=== 环境诊断 ==="
echo "SHELL=$SHELL"
echo "PATH=$PATH"
echo "NVM_DIR=$NVM_DIR"
echo "HOME=$HOME"
which node 2>/dev/null || echo "node: not found"
which npm 2>/dev/null || echo "npm: not found"
env | sort
```

将输出与终端中 `env | sort` 的结果对比，即可找出缺失的变量。
