//go:build !windows && !loong64
// +build !windows,!loong64

package monitor

import (
	"context"

	"github.com/shirou/gopsutil/v4/disk"
)

// safeDiskPartitions 在非 Windows 平台保持 gopsutil 原有实现。
// 这些实现不会像 gopsutil v4.24.5 的 Windows 版本一样，为不可取消的
// GetVolumeInformation 调用额外创建内部 goroutine。
func safeDiskPartitions(ctx context.Context, all bool) ([]disk.PartitionStat, error) {
	return disk.PartitionsWithContext(ctx, all)
}
