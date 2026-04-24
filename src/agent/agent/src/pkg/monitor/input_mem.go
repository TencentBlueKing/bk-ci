//go:build !loong64
// +build !loong64

package monitor

import (
	"runtime"
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v4/mem"
)

// Mem 对齐 telegraf plugins/inputs/mem。
//
// 字段集合按平台分支（对齐 telegraf mem.go 的 switch ms.platform）：
//
//   - 所有平台：total / available / used / used_percent(pct_used) /
//     available_percent
//   - darwin：+ active / free / inactive / wired
//   - freebsd：+ active / buffered / cached / free / inactive / laundry / wired
//   - openbsd：+ active / cached / free / inactive / wired
//   - linux：+ active / buffered / cached / commit_limit / committed_as /
//     dirty / free / high_free / high_total / huge_pages_free / huge_page_size /
//     huge_pages_total / inactive / low_free / low_total / mapped /
//     page_tables / shared / slab / sreclaimable / sunreclaim / swap_cached /
//     swap_free / swap_total / vmalloc_chunk / vmalloc_total / vmalloc_used /
//     write_back / write_back_tmp
//
// 注意 pct_used 不依赖 vm.UsedPercent：gopsutil v3 的 mem_windows.go 把该字段
// 做了整数运算，会得到 43 / 48 这种整数值，和 telegraf win_perf_counters
// 浮点精度对不上（对比 bin/test.log 观察到的回归）。这里一律自算
// 100 * float64(used) / float64(total) 覆盖所有平台。
type Mem struct {
	// virtualMemFn 为 mem.VirtualMemory 的注入点，便于测试替换。
	virtualMemFn func() (*mem.VirtualMemoryStat, error)
	// nowFn 用于测试注入固定时间戳。
	nowFn func() time.Time
	// platform 控制字段分支；默认取 runtime.GOOS，测试可覆盖。
	platform string
}

// NewMem 构造一个 Mem 采集器，使用默认的 gopsutil 实现。
func NewMem() *Mem {
	return &Mem{
		virtualMemFn: mem.VirtualMemory,
		nowFn:        time.Now,
		platform:     runtime.GOOS,
	}
}

// Name 返回 telegraf plugin 名 "mem"。
func (m *Mem) Name() string { return "mem" }

// Gather 采集内存信息并构造 metric 列表。
// 失败时返回非空 error，且不返回部分 metric（mem 采集要么全成功要么失败，
// 不像 disk 那样有多个挂载点可部分降级）。
func (m *Mem) Gather() ([]Metric, error) {
	vm, err := m.virtualMemFn()
	if err != nil {
		return nil, errors.Wrap(err, "mem: VirtualMemory failed")
	}
	if vm == nil {
		return nil, errors.New("mem: VirtualMemory returned nil")
	}

	// 全平台通用字段。
	var pctUsed float64
	if vm.Total > 0 {
		pctUsed = 100 * float64(vm.Used) / float64(vm.Total)
	}
	fields := map[string]interface{}{
		FieldTotal:          vm.Total,
		FieldAvailable:      vm.Available,
		FieldUsed:           vm.Used,
		RenamedFieldPctUsed: pctUsed,
	}
	// available_percent 参照 telegraf 公式：100 * available / total。
	// Total 为 0 时不写字段，避免除零并保留旧行为（单测依赖此不变量）。
	if vm.Total > 0 {
		fields[FieldAvailablePercent] = 100 * float64(vm.Available) / float64(vm.Total)
	}

	// 平台专属字段，严格对齐 telegraf plugins/inputs/mem/memory.go 的 switch。
	switch m.platform {
	case "darwin":
		fields[FieldActive] = vm.Active
		fields[FieldFree] = vm.Free
		fields[FieldInactive] = vm.Inactive
		fields[FieldWired] = vm.Wired
	case "openbsd":
		fields[FieldActive] = vm.Active
		fields[FieldCached] = vm.Cached
		fields[FieldFree] = vm.Free
		fields[FieldInactive] = vm.Inactive
		fields[FieldWired] = vm.Wired
	case "freebsd":
		fields[FieldActive] = vm.Active
		fields[FieldBuffered] = vm.Buffers
		fields[FieldCached] = vm.Cached
		fields[FieldFree] = vm.Free
		fields[FieldInactive] = vm.Inactive
		fields[FieldLaundry] = vm.Laundry
		fields[FieldWired] = vm.Wired
	case "linux":
		fields[FieldActive] = vm.Active
		fields[FieldBuffered] = vm.Buffers
		fields[FieldCached] = vm.Cached
		fields[FieldCommitLimit] = vm.CommitLimit
		fields[FieldCommittedAS] = vm.CommittedAS
		fields[FieldDirty] = vm.Dirty
		fields[FieldFree] = vm.Free
		fields[FieldHighFree] = vm.HighFree
		fields[FieldHighTotal] = vm.HighTotal
		fields[FieldHugePagesFree] = vm.HugePagesFree
		fields[FieldHugePageSize] = vm.HugePageSize
		fields[FieldHugePagesTotal] = vm.HugePagesTotal
		fields[FieldInactive] = vm.Inactive
		fields[FieldLowFree] = vm.LowFree
		fields[FieldLowTotal] = vm.LowTotal
		fields[FieldMapped] = vm.Mapped
		fields[FieldPageTables] = vm.PageTables
		fields[FieldShared] = vm.Shared
		fields[FieldSlab] = vm.Slab
		fields[FieldSreclaimable] = vm.Sreclaimable
		fields[FieldSunreclaim] = vm.Sunreclaim
		fields[FieldSwapCached] = vm.SwapCached
		fields[FieldSwapFree] = vm.SwapFree
		fields[FieldSwapTotal] = vm.SwapTotal
		fields[FieldVmallocChunk] = vm.VmallocChunk
		fields[FieldVmallocTotal] = vm.VmallocTotal
		fields[FieldVmallocUsed] = vm.VmallocUsed
		fields[FieldWriteBack] = vm.WriteBack
		fields[FieldWriteBackTmp] = vm.WriteBackTmp
	}

	return []Metric{
		{
			Name:      MeasurementMem,
			Tags:      nil, // mem 本身无 tag，host / projectId 等由 reporter 附加
			Fields:    fields,
			Timestamp: m.nowFn(),
		},
	}, nil
}
