//go:build !loong64 && !windows
// +build !loong64,!windows

package monitor

import (
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v3/disk"
)

// DiskIO 对齐 telegraf plugins/inputs/diskio。
// 每个物理磁盘一条 metric，tag name 为 device 名（sda、nvme0n1），数值
// 为累计计数器（自内核启动起的 reads/writes/bytes），由后端按相邻采样做差。
type DiskIO struct {
	ioCountersFn func() (map[string]disk.IOCountersStat, error)
	nowFn        func() time.Time
}

// NewDiskIO 返回默认 DiskIO 采集器。
// 注意 disk.IOCounters 在 Windows 上通过 PDH 查询 PhysicalDisk 计数器。
func NewDiskIO() *DiskIO {
	return &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			return disk.IOCounters()
		},
		nowFn: time.Now,
	}
}

// Name 返回 measurement 名 "io"（规范名）。
func (d *DiskIO) Name() string { return RenamedIO }

// Gather 每个磁盘产出一条 metric。没有 IO 统计时返回空列表，不报错。
func (d *DiskIO) Gather() ([]Metric, error) {
	counters, err := d.ioCountersFn()
	if err != nil {
		return nil, errors.Wrap(err, "diskio: IOCounters failed")
	}
	now := d.nowFn()
	out := make([]Metric, 0, len(counters))
	for name, c := range counters {
		out = append(out, Metric{
			Name: RenamedIO,
			Tags: map[string]string{TagName: name},
			Fields: map[string]interface{}{
				FieldReads:          c.ReadCount,
				FieldWrites:         c.WriteCount,
				RenamedFieldRkbS:    c.ReadBytes,
				RenamedFieldWkbS:    c.WriteBytes,
				FieldReadTime:       c.ReadTime,
				FieldWriteTime:      c.WriteTime,
				FieldIOTime:         c.IoTime,
				FieldWeightedIOTime: c.WeightedIO,
				FieldIOPSInProgress: c.IopsInProgress,
				FieldMergedReads:    c.MergedReadCount,
				FieldMergedWrites:   c.MergedWriteCount,
			},
			Timestamp: now,
		})
	}
	return out, nil
}
