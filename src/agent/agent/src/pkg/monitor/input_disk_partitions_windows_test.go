//go:build windows && !loong64
// +build windows,!loong64

package monitor

import (
	"context"
	"errors"
	"reflect"
	"testing"

	"golang.org/x/sys/windows"
)

type fakeWindowsDiskInfo struct {
	fstype string
	flags  uint32
	err    error
}

type fakeWindowsDiskAPI struct {
	roots       []string
	logicalErr  error
	driveTypes  map[string]uint32
	infos       map[string]fakeWindowsDiskInfo
	typeCalls   []string
	volumeCalls []string
}

func (f *fakeWindowsDiskAPI) logicalDrives() ([]string, error) {
	return f.roots, f.logicalErr
}

func (f *fakeWindowsDiskAPI) driveType(root string) uint32 {
	f.typeCalls = append(f.typeCalls, root)
	return f.driveTypes[root]
}

func (f *fakeWindowsDiskAPI) volumeInformation(root string) (string, uint32, error) {
	f.volumeCalls = append(f.volumeCalls, root)
	info := f.infos[root]
	return info.fstype, info.flags, info.err
}

func TestCollectWindowsPartitions_SkipsRemoteBeforeVolumeInformation(t *testing.T) {
	api := &fakeWindowsDiskAPI{
		roots: []string{"C:\\", "Z:\\", "D:\\", "E:\\", "R:\\"},
		driveTypes: map[string]uint32{
			"C:\\": windows.DRIVE_FIXED,
			"Z:\\": windows.DRIVE_REMOTE,
			"D:\\": windows.DRIVE_CDROM,
			"E:\\": windows.DRIVE_REMOVABLE,
			"R:\\": windows.DRIVE_RAMDISK,
		},
		infos: map[string]fakeWindowsDiskInfo{
			"C:\\": {fstype: "NTFS", flags: windows.FILE_FILE_COMPRESSION},
			// 如果实现错误地查询远程盘，这个条目会进入 volumeCalls。
			"Z:\\": {fstype: "NTFS"},
			"D:\\": {err: errors.New("device not ready")},
			"E:\\": {fstype: "exFAT", flags: windows.FILE_READ_ONLY_VOLUME},
		},
	}

	partitions, err := collectWindowsPartitions(context.Background(), api)
	if err != nil {
		t.Fatalf("collectWindowsPartitions: %v", err)
	}

	if got, want := api.volumeCalls, []string{"C:\\", "D:\\", "E:\\"}; !reflect.DeepEqual(got, want) {
		t.Fatalf("GetVolumeInformation calls = %v, want %v; DRIVE_REMOTE must be skipped first", got, want)
	}
	if len(partitions) != 2 {
		t.Fatalf("partitions = %d, want 2 local readable drives: %+v", len(partitions), partitions)
	}
	if got := partitions[0]; got.Device != "C:" || got.Mountpoint != "C:\\" || got.Fstype != "NTFS" {
		t.Fatalf("first partition = %+v", got)
	}
	if got, want := partitions[0].Opts, []string{"rw", "compress"}; !reflect.DeepEqual(got, want) {
		t.Fatalf("C opts = %v, want %v", got, want)
	}
	if got := partitions[1]; got.Device != "E:" || got.Mountpoint != "E:\\" || got.Fstype != "exFAT" {
		t.Fatalf("second partition = %+v", got)
	}
	if got, want := partitions[1].Opts, []string{"ro"}; !reflect.DeepEqual(got, want) {
		t.Fatalf("E opts = %v, want %v", got, want)
	}
}

func TestCollectWindowsPartitions_ContextCancellationStopsSynchronously(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	api := &fakeWindowsDiskAPI{
		roots:      []string{"C:\\"},
		driveTypes: map[string]uint32{"C:\\": windows.DRIVE_FIXED},
		infos:      map[string]fakeWindowsDiskInfo{"C:\\": {fstype: "NTFS"}},
	}

	partitions, err := collectWindowsPartitions(ctx, api)
	if !errors.Is(err, context.Canceled) {
		t.Fatalf("err = %v, want context.Canceled", err)
	}
	if len(partitions) != 0 || len(api.typeCalls) != 0 || len(api.volumeCalls) != 0 {
		t.Fatalf("canceled collection called Win32 APIs: partitions=%v typeCalls=%v volumeCalls=%v",
			partitions, api.typeCalls, api.volumeCalls)
	}
}

func TestCollectWindowsPartitions_LogicalDriveError(t *testing.T) {
	sentinel := errors.New("logical drives failed")
	api := &fakeWindowsDiskAPI{logicalErr: sentinel}

	partitions, err := collectWindowsPartitions(context.Background(), api)
	if !errors.Is(err, sentinel) {
		t.Fatalf("err = %v, want %v", err, sentinel)
	}
	if partitions != nil {
		t.Fatalf("partitions = %v, want nil", partitions)
	}
}
