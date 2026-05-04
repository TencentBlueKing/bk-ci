//go:build darwin && cgo
// +build darwin,cgo

package agentcli

import (
	"fmt"
	"syscall"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/diskutil"
)

func checkDiskSpace(workDir string) {
	var stat syscall.Statfs_t
	if err := syscall.Statfs(workDir, &stat); err != nil {
		statusLine(msg("  Disk space", "  磁盘空间"), msgf("FAIL: %v ✗", "失败: %v ✗", err))
		return
	}
	total := stat.Blocks * uint64(stat.Bsize)
	if total == 0 {
		statusLine(msg("  Disk space", "  磁盘空间"), msg("unknown ✗", "未知 ✗"))
		return
	}

	// Use macOS Foundation API to get available capacity including purgeable space,
	// which matches what macOS "System Information" (About This Mac) reports.
	var free uint64
	if avail, ok := diskutil.AvailableCapacity(workDir); ok && avail > 0 {
		free = avail
	} else {
		// Fallback to statvfs value
		free = stat.Bavail * uint64(stat.Bsize)
	}

	usedPct := 100 - int(float64(free)/float64(total)*100)
	freeGB := float64(free) / 1024 / 1024 / 1024
	totalGB := float64(total) / 1024 / 1024 / 1024

	status := diagStatusOK
	if freeGB < 1 {
		status = diagStatusLow
	}
	statusLine(msg("  Disk space", "  磁盘空间"),
		fmt.Sprintf("%.1f GB free / %.1f GB (%d%% used) %s", freeGB, totalGB, usedPct, status))
}
