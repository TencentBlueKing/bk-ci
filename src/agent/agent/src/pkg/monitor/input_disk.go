//go:build !loong64
// +build !loong64

package monitor

import (
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v3/disk"
)

// defaultDiskIgnoreFS 与 telegrafConf 中 inputs.disk.ignore_fs 完全一致。
// 这些文件系统要么是虚拟的（tmpfs / devtmpfs / devfs）要么是容器层叠
// （overlay / aufs / squashfs），上报容量没有实际意义。
var defaultDiskIgnoreFS = map[string]struct{}{
	"tmpfs":     {},
	"devtmpfs":  {},
	"devfs":     {},
	"overlay":   {},
	"aufs":      {},
	"squashfs":  {},
}

// Disk 对齐 telegraf plugins/inputs/disk。每个 physical mountpoint 产出
// 一条 metric，tag 含 device / fstype / path / mode。
type Disk struct {
	partitionsFn func(all bool) ([]disk.PartitionStat, error)
	usageFn      func(path string) (*disk.UsageStat, error)
	nowFn        func() time.Time

	// IgnoreFS 为 nil 时使用 defaultDiskIgnoreFS。
	IgnoreFS map[string]struct{}
}

// NewDisk 返回默认 disk 采集器。
func NewDisk() *Disk {
	return &Disk{
		partitionsFn: disk.Partitions,
		usageFn:      disk.Usage,
		nowFn:        time.Now,
	}
}

// Name 返回 measurement 名 "disk"。
func (d *Disk) Name() string { return MeasurementDisk }

// Gather 遍历所有 partition，跳过 ignore_fs 列表中的文件系统后调用 Usage
// 取容量/inode 信息。
//
// 部分挂载点可能 Usage 失败（权限、NFS 挂死等）；这类挂载点会记 Debug 日志
// 并跳过，不会影响其他挂载点的上报 —— 对齐 telegraf 的降级行为。
func (d *Disk) Gather() ([]Metric, error) {
	parts, err := d.partitionsFn(false)
	if err != nil {
		return nil, errors.Wrap(err, "disk: Partitions failed")
	}
	ignore := d.IgnoreFS
	if ignore == nil {
		ignore = defaultDiskIgnoreFS
	}

	now := d.nowFn()
	out := make([]Metric, 0, len(parts))
	for _, p := range parts {
		if _, skip := ignore[p.Fstype]; skip {
			continue
		}
		usage, uerr := d.usageFn(p.Mountpoint)
		if uerr != nil || usage == nil {
			// 挂载点不可读（例如 NFS 卡死）跳过
			continue
		}
		if usage.Total == 0 {
			continue
		}
		fields := map[string]interface{}{
			FieldTotal:        usage.Total,
			FieldFree:         usage.Free,
			FieldUsed:         usage.Used,
			FieldUsedPercent:  usage.UsedPercent,
			FieldInodesTotal:  usage.InodesTotal,
			FieldInodesFree:   usage.InodesFree,
			FieldInodesUsed:   usage.InodesUsed,
		}
		if usage.InodesTotal > 0 {
			fields[FieldInodesUsedPercent] = usage.InodesUsedPercent
		}

		tags := map[string]string{
			TagDevice: p.Device,
			TagFstype: p.Fstype,
			TagPath:   p.Mountpoint,
		}
		if p.Opts != nil {
			// telegraf 把 opts 的第一个元素作为 mode（ro/rw），这里保持同样的风格
			if len(p.Opts) > 0 {
				tags[TagMode] = p.Opts[0]
			}
		}
		out = append(out, Metric{
			Name:      MeasurementDisk,
			Tags:      tags,
			Fields:    fields,
			Timestamp: now,
		})
	}
	return out, nil
}
