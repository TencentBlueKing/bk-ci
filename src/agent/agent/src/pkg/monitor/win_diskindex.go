//go:build windows
// +build windows

package monitor

import (
	"fmt"
	"sync"
	"syscall"
	"time"
	"unsafe"

	"github.com/pkg/errors"
	"golang.org/x/sys/windows"
)

// win_diskindex.go 通过 DeviceIoControl(IOCTL_DISK_PERFORMANCE) 查询
// Windows 物理盘号，用于组装与 telegraf win_perf_counters PhysicalDisk
// 一致的 instance tag 格式 "<DiskIndex> <DriveLetter>:"（例如 "0 C:"）。
//
// 为什么要单独查：gopsutil disk.IOCounters() 虽然内部也调同一个 ioctl，
// 但只取了 BytesRead / WriteCount 等性能字段，丢弃了 StorageDeviceNumber。
// 我们按同样流程再查一次，单独把 StorageDeviceNumber 拎出来即可。
//
// 缓存：进程生命周期内，盘符→盘号映射几乎不变（物理盘热插拔极罕见）。
// 加 10 分钟 TTL 缓存，减少每分钟 Gather 的 DeviceIoControl 次数。

// diskIndexCacheTTL 是物理盘号缓存的过期时间。
const diskIndexCacheTTL = 10 * time.Minute

// IOCTL_STORAGE_GET_DEVICE_NUMBER 返回 STORAGE_DEVICE_NUMBER，其中的
// DeviceNumber 等于 \\.\PhysicalDriveN 的 N（与 telegraf win_perf_counters
// PhysicalDisk 的实例前缀一致）。
//
// 注意：早先用的 IOCTL_DISK_PERFORMANCE (0x70020) 返回的
// DISK_PERFORMANCE.StorageDeviceNumber 是存储栈内部递增的"打开计数"，
// 和 PhysicalDrive 号并不相等（典型表现：系统盘 C: 本该是 0，却报 2/3）。
// 正确映射必须走 IOCTL_STORAGE_GET_DEVICE_NUMBER。
const _IOCTL_STORAGE_GET_DEVICE_NUMBER uint32 = 0x2D1080

// storageDeviceNumber 对齐 Windows SDK STORAGE_DEVICE_NUMBER：
//
//	typedef struct _STORAGE_DEVICE_NUMBER {
//	    DEVICE_TYPE DeviceType;       // DWORD
//	    DWORD       DeviceNumber;
//	    DWORD       PartitionNumber;
//	} STORAGE_DEVICE_NUMBER;
type storageDeviceNumber struct {
	DeviceType      uint32
	DeviceNumber    uint32
	PartitionNumber uint32
}

// diskIndexCache 是包级盘符→盘号缓存。由 sync.Mutex 保护，进程启动时为空。
var (
	diskIndexCacheMu  sync.Mutex
	diskIndexCache    map[string]uint32
	diskIndexCachedAt time.Time
)

// queryStorageDeviceNumber 返回给定盘符（"C:"、"D:"）的物理盘索引。
// 进入 10 分钟 TTL 缓存；缓存未命中或过期时全量刷新一次。
// 调用失败（权限、盘不存在）返回 err，不写入缓存；调用方应降级 fallback
// 到 letter-only instance。
func queryStorageDeviceNumber(letter string) (uint32, error) {
	diskIndexCacheMu.Lock()
	defer diskIndexCacheMu.Unlock()

	if diskIndexCache != nil && time.Since(diskIndexCachedAt) < diskIndexCacheTTL {
		if idx, ok := diskIndexCache[letter]; ok {
			return idx, nil
		}
		// 盘符不在缓存里：可能是热插拔，刷新一次试试
	}

	fresh, err := refreshDiskIndexCacheLocked()
	if err != nil {
		return 0, err
	}
	if idx, ok := fresh[letter]; ok {
		return idx, nil
	}
	return 0, errors.Errorf("diskindex: drive letter %q not found after refresh", letter)
}

// refreshDiskIndexCacheLocked 全量查询所有 DRIVE_FIXED 盘符的物理盘号。
// 调用方需持有 diskIndexCacheMu。
func refreshDiskIndexCacheLocked() (map[string]uint32, error) {
	fresh, err := scanAllDriveIndices()
	if err != nil {
		return nil, err
	}
	diskIndexCache = fresh
	diskIndexCachedAt = time.Now()
	return fresh, nil
}

// scanAllDriveIndices 通过 GetLogicalDriveStrings 枚举所有盘符，对每个
// DRIVE_FIXED 盘打开 \\.\<letter>: 设备，调用 IOCTL_DISK_PERFORMANCE
// 读取 StorageDeviceNumber。返回 {"C:": 0, "D:": 1, ...}。
// 部分盘符 ioctl 失败（权限/非 NTFS）时跳过，不让整体失败。
func scanAllDriveIndices() (map[string]uint32, error) {
	lpBuffer := make([]uint16, 254)
	lpBufferLen, err := windows.GetLogicalDriveStrings(uint32(len(lpBuffer)), &lpBuffer[0])
	if err != nil {
		return nil, errors.Wrap(err, "diskindex: GetLogicalDriveStrings failed")
	}
	out := make(map[string]uint32, 8)
	for _, v := range lpBuffer[:lpBufferLen] {
		if v < 'A' || v > 'Z' {
			continue
		}
		letter := string(rune(v)) + ":"
		idx, qerr := ioctlDiskPerformance(letter)
		if qerr != nil {
			// 单盘失败（例如 USB 外接盘被占用）不中断整体
			continue
		}
		out[letter] = idx
	}
	if len(out) == 0 {
		return nil, errors.New("diskindex: no fixed drives could be queried")
	}
	return out, nil
}

// ioctlDiskPerformance 打开 \\.\<letter> 设备并调用
// IOCTL_STORAGE_GET_DEVICE_NUMBER，返回 PhysicalDrive 号（与 PDH
// PhysicalDisk 实例前缀一致）。
func ioctlDiskPerformance(letter string) (uint32, error) {
	szDevice := fmt.Sprintf(`\\.\%s`, letter)
	path, err := syscall.UTF16PtrFromString(szDevice)
	if err != nil {
		return 0, errors.Wrap(err, "diskindex: UTF16PtrFromString")
	}
	h, err := windows.CreateFile(
		path,
		0,
		windows.FILE_SHARE_READ|windows.FILE_SHARE_WRITE,
		nil,
		windows.OPEN_EXISTING,
		0,
		0,
	)
	if err != nil {
		return 0, errors.Wrap(err, "diskindex: CreateFile")
	}
	defer windows.CloseHandle(h)

	var sdn storageDeviceNumber
	var retSize uint32
	err = windows.DeviceIoControl(
		h,
		_IOCTL_STORAGE_GET_DEVICE_NUMBER,
		nil,
		0,
		(*byte)(unsafe.Pointer(&sdn)),
		uint32(unsafe.Sizeof(sdn)),
		&retSize,
		nil,
	)
	if err != nil {
		return 0, errors.Wrap(err, "diskindex: DeviceIoControl STORAGE_GET_DEVICE_NUMBER")
	}
	return sdn.DeviceNumber, nil
}

// resetDiskIndexCacheForTest 供单测清空缓存，避免跨用例污染。
// 不在生产路径调用。
func resetDiskIndexCacheForTest() {
	diskIndexCacheMu.Lock()
	defer diskIndexCacheMu.Unlock()
	diskIndexCache = nil
	diskIndexCachedAt = time.Time{}
}
