//go:build !loong64
// +build !loong64

package monitor

import (
	"errors"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v3/disk"
)

func TestDisk_Name(t *testing.T) {
	if n := NewDisk().Name(); n != MeasurementDisk {
		t.Errorf("Name() = %q", n)
	}
}

func TestDisk_Gather_IgnoreFilesystems(t *testing.T) {
	// 构造 3 个 partition：ext4 正常、tmpfs 需忽略、overlay 需忽略
	d := &Disk{
		partitionsFn: func(all bool) ([]disk.PartitionStat, error) {
			return []disk.PartitionStat{
				{Device: "/dev/sda1", Fstype: "ext4", Mountpoint: "/", Opts: []string{"rw"}},
				{Device: "tmpfs", Fstype: "tmpfs", Mountpoint: "/tmp"},
				{Device: "overlay", Fstype: "overlay", Mountpoint: "/var/lib/docker/overlay"},
			}, nil
		},
		usageFn: func(path string) (*disk.UsageStat, error) {
			return &disk.UsageStat{Total: 1000, Used: 300, Free: 700, UsedPercent: 30}, nil
		},
		nowFn: time.Now,
	}
	metrics, err := d.Gather()
	if err != nil {
		t.Fatalf("Gather: %v", err)
	}
	if len(metrics) != 1 {
		t.Fatalf("want 1 metric (only ext4), got %d", len(metrics))
	}
	if metrics[0].Tags[TagFstype] != "ext4" {
		t.Errorf("fstype = %q, want ext4", metrics[0].Tags[TagFstype])
	}
	if metrics[0].Tags[TagMode] != "rw" {
		t.Errorf("mode = %q, want rw", metrics[0].Tags[TagMode])
	}
}

func TestDisk_Gather_SkipsUsageError(t *testing.T) {
	// /a 的 Usage 失败 → 跳过；/b 成功 → 保留
	d := &Disk{
		partitionsFn: func(all bool) ([]disk.PartitionStat, error) {
			return []disk.PartitionStat{
				{Device: "/dev/sda1", Fstype: "ext4", Mountpoint: "/a"},
				{Device: "/dev/sda2", Fstype: "ext4", Mountpoint: "/b"},
			}, nil
		},
		usageFn: func(path string) (*disk.UsageStat, error) {
			if path == "/a" {
				return nil, errors.New("stale nfs")
			}
			return &disk.UsageStat{Total: 100, Used: 10, Free: 90, UsedPercent: 10}, nil
		},
		nowFn: time.Now,
	}
	metrics, err := d.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if len(metrics) != 1 || metrics[0].Tags[TagPath] != "/b" {
		t.Errorf("want only /b, got %+v", metrics)
	}
}

func TestDisk_Gather_PartitionsErrorPropagates(t *testing.T) {
	sentinel := errors.New("boom")
	d := &Disk{
		partitionsFn: func(all bool) ([]disk.PartitionStat, error) { return nil, sentinel },
		usageFn:      func(path string) (*disk.UsageStat, error) { return nil, nil },
		nowFn:        time.Now,
	}
	_, err := d.Gather()
	if err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v", err)
	}
}

func TestDisk_Gather_ZeroTotalSkipped(t *testing.T) {
	d := &Disk{
		partitionsFn: func(all bool) ([]disk.PartitionStat, error) {
			return []disk.PartitionStat{{Device: "x", Fstype: "ext4", Mountpoint: "/"}}, nil
		},
		usageFn: func(path string) (*disk.UsageStat, error) {
			return &disk.UsageStat{Total: 0}, nil
		},
		nowFn: time.Now,
	}
	metrics, _ := d.Gather()
	if len(metrics) != 0 {
		t.Errorf("zero-total mount should be skipped, got %d metrics", len(metrics))
	}
}

func TestDisk_Gather_FieldsPresent(t *testing.T) {
	d := &Disk{
		partitionsFn: func(all bool) ([]disk.PartitionStat, error) {
			return []disk.PartitionStat{{Device: "x", Fstype: "ext4", Mountpoint: "/"}}, nil
		},
		usageFn: func(path string) (*disk.UsageStat, error) {
			return &disk.UsageStat{
				Total: 1000, Used: 300, Free: 700, UsedPercent: 30,
				InodesTotal: 100, InodesUsed: 20, InodesFree: 80, InodesUsedPercent: 20,
			}, nil
		},
		nowFn: time.Now,
	}
	metrics, _ := d.Gather()
	required := []string{
		FieldTotal, FieldFree, FieldUsed, RenamedFieldInUse,
		FieldInodesTotal, FieldInodesFree, FieldInodesUsed,
		FieldInodesUsedPercent,
	}
	for _, f := range required {
		if _, ok := metrics[0].Fields[f]; !ok {
			t.Errorf("missing field %q", f)
		}
	}
}
