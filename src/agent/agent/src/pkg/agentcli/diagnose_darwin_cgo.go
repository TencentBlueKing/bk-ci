//go:build darwin && cgo
// +build darwin,cgo

package agentcli

/*
#cgo LDFLAGS: -framework CoreFoundation
#include <CoreFoundation/CoreFoundation.h>

// getAvailableCapacityForImportantUsage returns the available capacity in bytes
// consistent with what macOS "System Information" reports (includes purgeable space).
// Falls back to -1 on error.
long long getAvailableCapacityForImportantUsage(const char *path) {
    CFStringRef pathStr = CFStringCreateWithCString(NULL, path, kCFStringEncodingUTF8);
    if (!pathStr) return -1;

    CFURLRef url = CFURLCreateWithFileSystemPath(NULL, pathStr, kCFURLPOSIXPathStyle, true);
    CFRelease(pathStr);
    if (!url) return -1;

    CFErrorRef err = NULL;
    CFNumberRef val = NULL;

    // kCFURLVolumeAvailableCapacityForImportantUsageKey
    // Equivalent to NSURLVolumeAvailableCapacityForImportantUsageKey
    CFStringRef key = CFSTR("NSURLVolumeAvailableCapacityForImportantUsageKey");
    CFArrayRef keys = CFArrayCreate(NULL, (const void **)&key, 1, &kCFTypeArrayCallBacks);
    if (!keys) { CFRelease(url); return -1; }

    CFDictionaryRef props = CFURLCopyResourcePropertiesForKeys(url, keys, &err);
    CFRelease(keys);
    CFRelease(url);

    if (!props) {
        if (err) CFRelease(err);
        return -1;
    }

    val = CFDictionaryGetValue(props, key);
    long long result = -1;
    if (val) {
        CFNumberGetValue(val, kCFNumberLongLongType, &result);
    }
    CFRelease(props);
    return result;
}
*/
import "C"
import (
	"fmt"
	"syscall"
	"unsafe"
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
	cpath := C.CString(workDir)
	defer C.free(unsafe.Pointer(cpath))
	importantFree := int64(C.getAvailableCapacityForImportantUsage(cpath))

	var free uint64
	if importantFree > 0 {
		free = uint64(importantFree)
	} else {
		// Fallback to statvfs value
		free = stat.Bavail * uint64(stat.Bsize)
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
