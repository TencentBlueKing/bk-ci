//go:build windows
// +build windows

package monitor

import (
	"fmt"
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v3/disk"
)

// input_diskio_windows_out.go 是 out+windows 专用的 DiskIO 采集器。
//
// 与默认 DiskIO（input_diskio.go）的差别：
//   - 默认实现返回累计计数器（ReadBytes / WriteBytes 的 uint64 值），由后端按
//     相邻采样做差分。
//   - 本实现在一次 Gather 内连采两次（twin-sample），中间 sleep
//     rateSampleInterval（默认 1s，见 rate_windows.go），用差值除以实际
//     经过时间直接产出瞬时速率，分母对齐 PDH win_perf_counters 的
//     CounterRefreshInterval，便于 Stream 后端 Disk_*_Bytes_persec 曲线
//     与 telegraf 时代一致。
//
// 速率字段：
//   rkb_s  ← (s2.ReadBytes  - s1.ReadBytes)  / dt
//   wkb_s  ← (s2.WriteBytes - s1.WriteBytes) / dt
//   reads  ← (s2.ReadCount  - s1.ReadCount)  / dt
//   writes ← (s2.WriteCount - s1.WriteCount) / dt
// 其余字段（read_time / write_time / io_time / weighted_io_time /
// iops_in_progress / merged_reads / merged_writes）取第二次采样的累计值。
//
// 首次 / counter reset：该字段从输出 fields 中缺席（delete）而不是输出 0，
// 避免在后端曲线上出现启动瞬间吞吐掉底的毛刺。

// DiskIO 对齐 telegraf win_perf_counters 的 PhysicalDisk 速率计数器。
type DiskIO struct {
	ioCountersFn func() (map[string]disk.IOCountersStat, error)
	nowFn        func() time.Time
	sleepFn      func(time.Duration) // 单测注入点，默认 time.Sleep
	// deviceNumberFn 把盘符（"C:"）映射到物理盘号（0, 1, ...），用于
	// 组装 telegraf PDH PhysicalDisk 风格的 instance tag "0 C:"。
	// 默认走 queryStorageDeviceNumber（带 10min 缓存）；测试注入 fake。
	// 返回错误时降级使用 letter-only instance。
	deviceNumberFn func(letter string) (uint32, error)
}

// NewDiskIO 返回默认 DiskIO 采集器。
// disk.IOCounters 在 Windows 底层走 PDH 查 PhysicalDisk 累计计数。
func NewDiskIO() *DiskIO {
	return &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			return disk.IOCounters()
		},
		nowFn:          time.Now,
		sleepFn:        time.Sleep,
		deviceNumberFn: queryStorageDeviceNumber,
	}
}

// Name 返回 "io"（规范名）。
func (d *DiskIO) Name() string { return RenamedIO }

// Gather 执行一次 twin-sample 并返回速率字段已换算的 metric。
func (d *DiskIO) Gather() ([]Metric, error) {
	s1, err := d.ioCountersFn()
	if err != nil {
		return nil, errors.Wrap(err, "diskio: IOCounters first sample failed")
	}
	t1 := d.nowFn()

	d.sleepFn(rateSampleInterval)

	s2, err := d.ioCountersFn()
	if err != nil {
		return nil, errors.Wrap(err, "diskio: IOCounters second sample failed")
	}
	t2 := d.nowFn()

	dt := t2.Sub(t1).Seconds()
	if dt <= 0 {
		return nil, errors.New("diskio: twin-sample non-positive dt")
	}

	out := make([]Metric, 0, len(s2))
	for name, c2 := range s2 {
		// 过滤伪磁盘（Windows 默认 PhysicalDisk 不会命中，兜底一层统一）
		if shouldSkipDiskIO(name) {
			continue
		}
		fields := map[string]interface{}{
			FieldReadTime:       c2.ReadTime,
			FieldWriteTime:      c2.WriteTime,
			FieldIOTime:         c2.IoTime,
			FieldWeightedIOTime: c2.WeightedIO,
			FieldIOPSInProgress: c2.IopsInProgress,
			FieldMergedReads:    c2.MergedReadCount,
			FieldMergedWrites:   c2.MergedWriteCount,
		}

		if c1, ok := s1[name]; ok {
			if rate, ok := computeRate(c2.ReadBytes, c1.ReadBytes, dt); ok {
				fields[RenamedFieldRkbS] = rate
			}
			if rate, ok := computeRate(c2.WriteBytes, c1.WriteBytes, dt); ok {
				fields[RenamedFieldWkbS] = rate
			}
			if rate, ok := computeRate(c2.ReadCount, c1.ReadCount, dt); ok {
				fields[FieldReads] = rate
			}
			if rate, ok := computeRate(c2.WriteCount, c1.WriteCount, dt); ok {
				fields[FieldWrites] = rate
			}
		}
		// 仅在第二次采样出现的磁盘（热插拔）：没有速率字段，仅输出第二次累计字段

		// 构造 tags：instance 尽量对齐 telegraf PhysicalDisk 的
		// "<DiskIndex> <Letter>" 格式；失败时 fallback 到 letter。
		instance := name
		if d.deviceNumberFn != nil {
			if idx, err := d.deviceNumberFn(name); err == nil {
				instance = fmt.Sprintf("%d %s", idx, name)
			}
		}
		tags := map[string]string{
			TagName:     name,
			TagInstance: instance,
		}
		out = append(out, Metric{
			Name:      RenamedIO,
			Tags:      normalizeWinMetricTags(RenamedIO, tags),
			Fields:    fields,
			Timestamp: t2,
		})
	}
	return out, nil
}
