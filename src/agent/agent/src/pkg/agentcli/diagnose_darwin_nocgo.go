//go:build darwin && !cgo
// +build darwin,!cgo

package agentcli

import (
	"fmt"
	"syscall"
)

// checkDiskSpace reports disk space using statvfs (CGO disabled).
// Note: this reports only truly unallocated space and does NOT include
// purgeable space (local Time Machine snapshots, iCloud cache, etc.).
// For accurate macOS "System Information"-consistent values, use the CGO build.
func checkDiskSpace(workDir string) {
	var stat syscall.Statfs_t
	if err := syscall.Statfs(workDir, &stat); err != nil {
		statusLine(msg("  Disk space", "  磁盘空间"), msgf("FAIL: %v ✗", "失败: %v ✗", err))
		return
	}
	free := stat.Bavail * uint64(stat.Bsize)
	total := stat.Blocks * uint64(stat.Bsize)
	if total == 0 {
		statusLine(msg("  Disk space", "  磁盘空间"), msg("unknown ✗", "未知 ✗"))
		return
	}
	usedPct := 100 - int(float64(free)/float64(total)*100)
	freeGB := float64(free) / 1024 / 1024 / 1024
	totalGB := float64(total) / 1024 / 1024 / 1024

	status := "✓"
	if freeGB < 1 {
		status = "✗ LOW"
	}
	statusLine(msg("  Disk space", "  磁盘空间"),
		fmt.Sprintf("%.1f GB free / %.1f GB (%d%% used) %s", freeGB, totalGB, usedPct, status))
}
