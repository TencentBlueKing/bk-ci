# 进程树 & 阻塞诊断工具

一个 Windows 原生的进程分析工具，能够构建进程树、检测阻塞进程、扫描管道句柄继承关系，并生成交互式 HTML 报告。

主要用于诊断 **CI/CD 构建卡死** 问题——由孤儿子进程持有继承的管道句柄导致的典型场景，如 `commons-exec` 的 StreamPumper 阻塞在 EOF 等待上。

## 功能特性

- **进程树可视化** — 向上追溯父链 + 向下展示子树
- **管道句柄继承扫描** — 找出继承了目标进程管道句柄的"孙进程"，精确定位阻塞管道关闭的进程
- **线程级阻塞诊断** — 分析线程等待状态，检测阻塞/挂起的进程
- **逐进程管道句柄枚举** — 使用 `NtQuerySystemInformation` 枚举进程树中每个进程持有的管道句柄
- **自动 jstack 抓取** — 自动捕获 `java.exe` 目标进程的线程转储
- **丰富的 HTML 报告** — 暗色主题交互式报告，包含可折叠区块、概要卡片、进程树和一键复制命令
- **控制台输出** — 彩色终端输出，包含进程树、问题汇总和操作建议

## 环境要求

- **Windows**（使用 Windows 特定 API：WMI、NtQuerySystemInformation 等）
- **管理员权限**（如未以管理员运行会自动通过 UAC 提权）
- **Go 1.21+**（从源码编译时需要）

## 编译

```bash
# 进入项目目录并编译
cd process
go build -o process-tree.exe .

# 或指定输出路径
go build -o bin/process-tree.exe .
```

## 使用方法

```
process-tree.exe [选项]

选项:
  -buildid <字符串>   构建 ID，用于匹配进程命令行（必填）
  -name <字符串>      目标进程名称（默认: java.exe）
  -i                  交互模式（引导式输入 buildId 和选项）
  -diag               启用线程级阻塞诊断（较慢）
  -pipe               启用逐进程管道句柄扫描（最慢）
  -html               仅生成 HTML 报告，跳过控制台输出
  -h                  显示帮助信息并退出
  -v                  显示版本号并退出
```

### 使用示例

```bash
# 基本用法：查找匹配构建 ID 的 java.exe 进程
process-tree.exe -buildid b-ec8dfe3da2174a219d04907dd791479e

# 完整诊断：线程分析 + 管道扫描
process-tree.exe -buildid b-ec8dfe3da2174a219d04907dd791479e -diag -pipe

# 指定其他进程名
process-tree.exe -buildid b-ec8dfe3da2174a219d04907dd791479e -name node.exe

# 仅生成 HTML 报告
process-tree.exe -buildid b-ec8dfe3da2174a219d04907dd791479e -html

# 交互模式
process-tree.exe -i
```

## 输出说明

### 控制台输出

彩色进程树，包含：
- 目标进程信息（PID、命令行、运行时长）
- 父进程链（谁启动了这个进程）
- 子进程树（所有后代进程）
- 管道句柄继承警告
- 问题汇总和操作建议

### HTML 报告

保存为 `process_tree_report_<时间戳>.html`，包含：
- 概要卡片（PID、子进程数、阻塞数、管道句柄数、管道继承者数）
- 可折叠的父进程链和子进程树
- **管道继承者分析** — 持有共享管道句柄的进程列表，附带可操作命令
- 线程级诊断详情（使用 `-diag` 时）
- 逐进程管道句柄列表（使用 `-pipe` 时）
- Java 进程的 jstack 输出
- 所有命令支持一键复制到剪贴板

## 工作原理

### 管道继承检测

1. 调用 `NtQuerySystemInformation(SystemHandleInformation)` 获取全系统句柄表
2. 找到目标进程持有的所有 Pipe 类型文件句柄，记录其内核对象地址
3. 扫描所有其他进程的句柄，查找引用**相同内核对象**的句柄
4. 匹配到的 = "管道继承者"——通过 `CreateProcess` 的句柄继承机制获得管道句柄的进程

### 为什么这很重要

当构建工具（如 Maven 通过 `commons-exec`）启动子进程时，会创建 stdout/stderr 的管道。如果子进程又启动了孙进程（守护进程、后台任务等），这些孙进程会**继承管道句柄**。即使直接子进程已经退出，只要这些孙进程还持有管道的写端，管道就无法关闭。构建工具的 `StreamPumper` 会一直阻塞等待 EOF，导致整个构建卡死。

本工具能精确定位持有继承句柄的孙进程，并提供解决命令。

## 项目结构

```
process/
├── main.go                      # 单文件 Go 应用（所有逻辑）
├── go.mod                       # Go 模块定义
├── go.sum                       # 依赖校验和
├── run_as_admin_interactive.bat # 以管理员权限运行交互模式的辅助脚本
├── Show-ProcessTree.ps1         # PowerShell 版基础进程树查看脚本
└── README.md                    # 本文件
```

## 架构说明 (main.go)

应用在单个文件中按逻辑区块组织：

| 区块 | 说明 |
|------|------|
| **数据类型** | WMI 结构体、PipeHolder、ProcessDiag、ProcessNode、TargetReport |
| **主入口 & CLI** | 参数解析、交互模式、入口函数 |
| **WMI 查询** | 通过 WMI 枚举进程/线程 |
| **进程树构建** | buildTree、buildParentChain — 递归构建树 |
| **句柄扫描** | 基于 NtQuerySystemInformation 的管道句柄枚举 |
| **管道继承检测** | findPipeHolders — 跨进程管道句柄匹配 |
| **Jstack** | Java 线程转储捕获 |
| **诊断分析** | 线程等待状态分析、阻塞检测 |
| **报告收集** | collectReport — 并行数据采集 |
| **HTML 生成** | generateHTMLReport — 完整交互式 HTML 报告 |
| **控制台输出** | printReport — 彩色终端输出 |
| **工具函数** | 颜色打印、时间格式化、管理员提权 |

## 许可证

内部工具 — 未公开许可。
