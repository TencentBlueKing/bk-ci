---
name: process-tree
description: >
  本 Skill 提供进程树 & 阻塞诊断工具的开发和维护指南——一个单文件 Windows Go 应用，
  用于分析进程树、检测阻塞进程、扫描管道句柄继承关系并生成交互式 HTML 报告。
  在修改、扩展或调试 process-tree 工具时应使用此 Skill。
---

# 进程树 & 阻塞诊断工具 — 开发指南

## 项目概览

一个 Windows 原生的进程分析工具（`main.go`，单文件架构），使用 Go 构建。
用于诊断 CI/CD 构建卡死——由孤儿子进程持有继承的管道句柄导致。

- **语言**: Go 1.21+
- **平台**: 仅 Windows（WMI、NtQuerySystemInformation、Win32 API）
- **架构**: 单文件（`main.go`，约 1700 行），无内部包
- **依赖**: `github.com/yusufpapurcu/wmi`、`golang.org/x/sys/windows`

## 代码组织

代码在 `main.go` 中按逻辑区块组织。导航或修改时参考以下地图：

| 行号范围（约） | 区块 | 关键函数 |
|---|---|---|
| 1–125 | **数据类型 & 常量** | `Win32Process`、`Win32Thread`、`PipeHolder`、`ProcessDiag`、`TargetReport`、`ProcessNode`、线程等待原因映射、Windows API 常量、`systemHandleEntryInfo` |
| 127–220 | **CLI & 入口** | `main()`、`printUsage()`、`interactiveMode()` — 参数解析、`-h`、`-v`、`-i`、管理员权限检查 |
| 220–350 | **WMI 查询** | `getAllProcesses()`、`getProcessByPid()`、`getThreads()` — 基于 WMI 的进程/线程枚举 |
| 350–450 | **进程树构建** | `buildTree()`、`buildParentChain()`、`countNodes()` — 递归树构建 |
| 450–600 | **句柄扫描** | `scanProcessPipeHandles()` — 基于 `NtQuerySystemInformation` + `NtQueryObject` 的逐进程管道句柄枚举（带 goroutine 超时） |
| 600–780 | **管道继承检测** | `findPipeHolders()`、`getHandlePipeName()` — 通过内核对象地址进行跨进程管道句柄匹配 |
| 780–830 | **Jstack** | `runJstack()` — Java 线程转储捕获 |
| 830–950 | **诊断分析** | `diagnoseSingle()`、`diagnoseChildren()` — 线程等待状态分析、阻塞检测 |
| 950–1050 | **报告收集** | `collectReport()`、`queryAndReport()` — 并行数据采集编排 |
| 1050–1550 | **HTML 报告生成** | `generateHTMLReport()` — 内嵌 HTML/CSS/JS 暗色主题、`JSONReport`、`JSONDiag`、`JSONPipeHolder` |
| 1550–1600 | **控制台输出** | `printReport()`、`printTree()` — 彩色终端输出 |
| 1600–1667 | **工具函数** | `formatDuration()`、`isAdmin()`、`runAsAdmin()`、`enableVirtualTerminal()`、`waitExit()` |

## 关键设计决策

### 单文件架构
所有代码都在 `main.go` 中。这是刻意为之——工具作为单个可执行文件分发，保持单文件简化了向 CI Agent 的部署。除非文件超过约 3000 行，否则不要拆分为多个包。

### HTML 报告内嵌字符串
HTML 报告通过 Go 中的字符串拼接构建（非模板）。这避免了外部文件依赖。修改报告时注意：
- JavaScript 和 CSS 内联在 HTML 中
- 数据通过 `json.Marshal` 作为 JSON blob 注入到 `<script>` 标签中
- 报告使用与 DevOps 仪表盘一致的暗色主题

### NtQueryObject 的 Goroutine 超时
`NtQueryObject(ObjectNameInformation)` 在某些句柄类型上会死锁（如有待读取的同步命名管道）。必须始终在 goroutine 中调用，配合 `select`/`time.After` 超时（200ms）。永远不要在主 goroutine 上同步查询 ObjectNameInformation。

### 并行数据收集
`collectReport()` 使用 `sync.WaitGroup` 并行收集数据：
- 进程树构建
- 管道继承者扫描
- Jstack 捕获
- 线程诊断（启用 `-diag` 时）
- 逐进程管道扫描（启用 `-pipe` 时）

添加新数据源时应保持这种并行模式。

### 管理员权限
工具需要管理员权限才能跨进程调用 `NtQuerySystemInformation` 和 `DuplicateHandle`。已内置通过 `ShellExecuteW("runas")` 的自动提权，不要删除此功能。

## 开发工作流

### 编译
```bash
cd process
go build -o process-tree.exe .
```

### 测试
目前没有自动化测试。手动测试步骤：
1. 启动一个进程树（如 `cmd /c start /b ping localhost -t`）
2. 运行 `process-tree.exe -buildid <子串> -name cmd.exe -diag -pipe`
3. 验证控制台输出和 HTML 报告

### 添加新功能

1. **添加数据类型** — 如需要，在文件顶部添加
2. **添加采集逻辑** — 创建新函数，通过 `wg.Add(1)` + goroutine 集成到 `collectReport()` 中
3. **添加到 JSON 报告** — 扩展 `JSONReport` 结构体并在 `generateHTMLReport()` 中填充
4. **添加 HTML 区块** — 插入到 `generateHTMLReport()` 的 `panelsHtml` 构建器中
5. **添加控制台输出** — 插入到 `printReport()` 中
6. **更新 CLI** — 在 `main()` 中添加 flag，更新 `printUsage()`
7. **编译并测试**

### 添加新 CLI 参数

1. 在 `main()` 的 `flag.Parse()` 之前添加 `flag.Type()` 调用
2. 在 `printUsage()` 中添加描述行
3. 将参数值传递给 `queryAndReport()` 及下游函数
4. 更新 README.md

## Windows API 参考

### 使用的关键 API

| API | 用途 |
|---|---|
| `NtQuerySystemInformation(SystemHandleInformation)` | 枚举全系统句柄 |
| `NtQueryObject(ObjectTypeInformation)` | 获取句柄类型名（如 "File"） |
| `NtQueryObject(ObjectNameInformation)` | 获取句柄对象名（如管道路径）— **必须使用超时** |
| `DuplicateHandle` | 将目标进程的句柄复制到当前进程以供检查 |
| `WMI Win32_Process` | 获取进程元数据（名称、PID、命令行、创建时间） |
| `WMI Win32_Thread` | 获取线程状态和等待原因 |
| `ShellExecuteW("runas")` | UAC 提权 |

### 句柄条目结构
```go
type systemHandleEntryInfo struct {
    UniqueProcessId       uint16  // 所属进程 PID
    CreatorBackTraceIndex uint16
    ObjectTypeIndex       uint8   // 类型索引（跨重启不稳定）
    HandleAttributes      uint8
    HandleValue           uint16  // 在所属进程中的句柄值
    Object                uintptr // 内核对象地址（唯一标识）
    GrantedAccess         uint32
}
```

`Object` 字段（内核对象地址）是跨进程管道匹配的关键——不同进程中指向相同 `Object` 地址的两个句柄引用的是同一个内核对象。

## 常见坑点

1. **NtQueryObject 死锁**: 必须使用 goroutine + 超时。永远不要同步查询 ObjectNameInformation。
2. **句柄泄漏**: 必须关闭复制的句柄（`CloseHandle`），即使在错误路径上也要关闭。
3. **缓冲区大小**: `NtQuerySystemInformation` 的缓冲区必须在 `STATUS_INFO_LENGTH_MISMATCH` 时动态增长。上限设为 512MB 以防 OOM。
4. **WMI 性能**: WMI 查询很慢。尽量减少查询次数（批量查询、缓存结果）。
5. **句柄条目中的 16 位 PID**: `systemHandleEntryInfo` 中的 `UniqueProcessId` 是 `uint16`——这是遗留结构。对于 PID > 65535 的情况，应考虑使用 `SystemExtendedHandleInformation`（class 64）。
6. **指针运算**: 遍历句柄条目时，必须验证 `entryAddr + entrySize <= bufEnd` 以防止访问越界。

## 版本管理

版本号定义在 `main.go` 顶部的 `const version` 中。发布时更新：
- **Patch** (1.2.x): Bug 修复、小改进
- **Minor** (1.x.0): 新功能（如新的扫描能力、新的报告区块）
- **Major** (x.0.0): CLI 接口或报告格式的破坏性变更
