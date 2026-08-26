//go:build loong64
// +build loong64

package monitor

import (
	"context"
	"io"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

// loong64 平台下 gopsutil/v3 v3.22.9 没有提供 host_linux_loong64.go
// （缺失 sizeOfUtmp 等架构相关常量），无法编译通过。
// 等 gopsutil 升级后可移除本文件，把 monitor 主实现放开到 loong64。
// 期间 monitor 在该平台打桩，保持跨架构构建不中断。

// Collect 在 loong64 平台为空操作。
func Collect() {
	logs.Info("monitor not supported on loong64, skip")
}

// RunDaemon 在 loong64 平台为空操作。保留签名以便 agentcli monitor --daemon
// 子命令在该架构下仍可链接。
func RunDaemon(workDir string, isDebug bool) error {
	_ = workDir
	_ = isDebug
	logs.Info("monitor daemon not supported on loong64, skip")
	return nil
}

// Supervise 在 loong64 平台为空操作。保留签名以便主 agent 启动流程链接。
func Supervise(ctx context.Context) {
	_ = ctx
	logs.Info("monitor supervisor not supported on loong64, skip")
}

// RunOnceStdout 在 loong64 平台直接返回 0 / nil。
// 保留签名以便 agentcli monitor 子命令在该架构下仍可链接。
func RunOnceStdout(ctx context.Context, out io.Writer, duration time.Duration) (int, error) {
	_ = ctx
	return 0, nil
}
