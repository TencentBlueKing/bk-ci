//go:build windows && !loong64
// +build windows,!loong64

package monitor

import (
	"context"
	"errors"
	"strings"

	"github.com/shirou/gopsutil/v4/disk"
	"golang.org/x/sys/windows"
)

// windowsDiskAPI 把 Win32 调用抽象出来，便于验证 DRIVE_REMOTE 一定在
// GetVolumeInformation 之前被过滤。
type windowsDiskAPI interface {
	logicalDrives() ([]string, error)
	driveType(root string) uint32
	volumeInformation(root string) (fstype string, flags uint32, err error)
}

type nativeWindowsDiskAPI struct{}

// safeDiskPartitions 同步枚举 Windows 本地盘符，不创建内部 goroutine。
//
// gopsutil v4.24.5 的 PartitionsWithContext 为了给不可取消的
// GetVolumeInformation 套 context，会在内部启动 goroutine。context 超时
// 后外层返回，但 Win32 调用可能继续永久阻塞，导致 inputRunner 清除 running
// 后下一轮再次创建隐藏 goroutine。
//
// 这里先用 GetDriveType 判断盘符，DRIVE_REMOTE 直接跳过；其余 Win32 调用
// 保持同步。如果某个本地设备异常阻塞，阻塞会留在 Disk.Gather 本身，runner
// 会保持 running 并跳过后续轮次，满足“每个 Disk 最多卡一个 Gather”。
func safeDiskPartitions(ctx context.Context, _ bool) ([]disk.PartitionStat, error) {
	return collectWindowsPartitions(ctx, nativeWindowsDiskAPI{})
}

func collectWindowsPartitions(ctx context.Context, api windowsDiskAPI) ([]disk.PartitionStat, error) {
	roots, err := api.logicalDrives()
	if err != nil {
		return nil, err
	}

	partitions := make([]disk.PartitionStat, 0, len(roots))
	for _, root := range roots {
		if err := ctx.Err(); err != nil {
			return partitions, err
		}

		driveType := api.driveType(root)
		if driveType == windows.DRIVE_REMOTE {
			continue
		}
		switch driveType {
		case windows.DRIVE_REMOVABLE, windows.DRIVE_FIXED, windows.DRIVE_CDROM:
		default:
			continue
		}

		// GetVolumeInformation 没有可取消版本。必须保持同步调用，不能用
		// goroutine + select(ctx.Done()) 包装，否则外层返回后会留下隐藏 goroutine。
		fstype, flags, err := api.volumeInformation(root)
		if err != nil {
			// 空光驱、未格式化卷、热拔插等只跳过当前盘，不影响其他本地盘。
			continue
		}

		opts := []string{"rw"}
		if flags&windows.FILE_READ_ONLY_VOLUME != 0 {
			opts = []string{"ro"}
		}
		if flags&windows.FILE_FILE_COMPRESSION != 0 {
			opts = append(opts, "compress")
		}

		device := strings.TrimRight(root, `\/`)
		partitions = append(partitions, disk.PartitionStat{
			Device:     device,
			Mountpoint: root,
			Fstype:     fstype,
			Opts:       opts,
		})
	}
	return partitions, nil
}

func (nativeWindowsDiskAPI) logicalDrives() ([]string, error) {
	required, err := windows.GetLogicalDriveStrings(0, nil)
	if err != nil {
		return nil, err
	}
	if required == 0 {
		return nil, errors.New("GetLogicalDriveStrings returned an empty buffer")
	}

	buffer := make([]uint16, required)
	written, err := windows.GetLogicalDriveStrings(uint32(len(buffer)), &buffer[0])
	if err != nil {
		return nil, err
	}
	if written > uint32(len(buffer)) {
		return nil, errors.New("GetLogicalDriveStrings buffer changed during enumeration")
	}

	roots := make([]string, 0, written/4)
	for start := 0; start < int(written); {
		end := start
		for end < int(written) && buffer[end] != 0 {
			end++
		}
		if end == start {
			break
		}
		roots = append(roots, windows.UTF16ToString(buffer[start:end]))
		start = end + 1
	}
	return roots, nil
}

func (nativeWindowsDiskAPI) driveType(root string) uint32 {
	rootPtr, err := windows.UTF16PtrFromString(root)
	if err != nil {
		return windows.DRIVE_UNKNOWN
	}
	return windows.GetDriveType(rootPtr)
}

func (nativeWindowsDiskAPI) volumeInformation(root string) (string, uint32, error) {
	rootPtr, err := windows.UTF16PtrFromString(root)
	if err != nil {
		return "", 0, err
	}

	var (
		volumeName             [windows.MAX_PATH + 1]uint16
		fileSystemName         [windows.MAX_PATH + 1]uint16
		volumeSerialNumber     uint32
		maximumComponentLength uint32
		fileSystemFlags        uint32
	)
	err = windows.GetVolumeInformation(
		rootPtr,
		&volumeName[0],
		uint32(len(volumeName)),
		&volumeSerialNumber,
		&maximumComponentLength,
		&fileSystemFlags,
		&fileSystemName[0],
		uint32(len(fileSystemName)),
	)
	if err != nil {
		return "", 0, err
	}
	return windows.UTF16ToString(fileSystemName[:]), fileSystemFlags, nil
}
