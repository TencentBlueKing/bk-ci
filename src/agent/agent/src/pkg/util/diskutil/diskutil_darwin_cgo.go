//go:build darwin && cgo
// +build darwin,cgo

package diskutil

/*
#cgo LDFLAGS: -framework CoreFoundation
#include <CoreFoundation/CoreFoundation.h>

// darwinAvailableCapacityForImportantUsage returns the available capacity
// in bytes consistent with what macOS "System Information" reports
// (includes purgeable space such as Time Machine snapshots, iCloud cache).
// Falls back to -1 on error.
long long darwinAvailableCapacityForImportantUsage(const char *path) {
    CFStringRef pathStr = CFStringCreateWithCString(NULL, path, kCFStringEncodingUTF8);
    if (!pathStr) return -1;

    CFURLRef url = CFURLCreateWithFileSystemPath(NULL, pathStr, kCFURLPOSIXPathStyle, true);
    CFRelease(pathStr);
    if (!url) return -1;

    CFErrorRef err = NULL;
    CFNumberRef val = NULL;

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

import "unsafe"

// AvailableCapacity returns the available capacity in bytes for the volume
// containing path, including purgeable space (Time Machine snapshots,
// iCloud cache, etc.). This matches what macOS "About This Mac" / "System
// Information" reports.
//
// Returns (0, false) if the Foundation API call fails; caller should fall
// back to the statfs / gopsutil value.
func AvailableCapacity(path string) (uint64, bool) {
	cpath := C.CString(path)
	defer C.free(unsafe.Pointer(cpath))
	result := int64(C.darwinAvailableCapacityForImportantUsage(cpath))
	if result <= 0 {
		return 0, false
	}
	return uint64(result), true
}
