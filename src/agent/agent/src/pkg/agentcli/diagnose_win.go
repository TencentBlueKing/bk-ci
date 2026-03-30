//go:build windows
// +build windows

package agentcli

import (
	"fmt"
	"unsafe"

	"golang.org/x/sys/windows"
)

func checkDiskSpace(workDir string) {
	pathPtr, err := windows.UTF16PtrFromString(workDir)
	if err != nil {
		statusLine(msg("  Disk space", "  磁盘空间"), msgf("FAIL: %v ✗", "失败: %v ✗", err))
		return
	}

	var freeBytesAvailable, totalBytes, totalFreeBytes uint64
	err = windows.GetDiskFreeSpaceEx(
		pathPtr,
		(*uint64)(unsafe.Pointer(&freeBytesAvailable)),
		(*uint64)(unsafe.Pointer(&totalBytes)),
		(*uint64)(unsafe.Pointer(&totalFreeBytes)),
	)
	if err != nil {
		statusLine(msg("  Disk space", "  磁盘空间"), msgf("FAIL: %v ✗", "失败: %v ✗", err))
		return
	}

	if totalBytes == 0 {
		statusLine(msg("  Disk space", "  磁盘空间"), msg("unknown ✗", "未知 ✗"))
		return
	}

	usedPct := 100 - int(float64(freeBytesAvailable)/float64(totalBytes)*100)
	freeGB := float64(freeBytesAvailable) / 1024 / 1024 / 1024
	totalGB := float64(totalBytes) / 1024 / 1024 / 1024

	status := "✓"
	if freeGB < 1 {
		status = "✗ LOW"
	}
	statusLine(msg("  Disk space", "  磁盘空间"),
		fmt.Sprintf("%.1f GB free / %.1f GB (%d%% used) %s", freeGB, totalGB, usedPct, status))
}
