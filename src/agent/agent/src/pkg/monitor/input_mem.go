//go:build !loong64
// +build !loong64

package monitor

import (
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v3/mem"
)

// Mem 对齐 telegraf plugins/inputs/mem。
//
// field 集合（来自 telegraf 源码 plugins/inputs/mem/memory.go 的 gather 方法）：
//
//	total, available, used, used_percent, free,
//	buffered, cached, active, inactive, slab, sreclaimable, sunreclaim,
//	wired, commit_limit, committed_as, dirty, high_free, high_total,
//	huge_pages_free, huge_page_size, huge_pages_total, low_free, low_total,
//	mapped, page_tables, shared, swap_cached, swap_free, swap_total,
//	vmalloc_chunk, vmalloc_total, vmalloc_used, write_back, write_back_tmp,
//	available_percent
//
// 实际各平台可用字段不同，gopsutil 返回结构体里缺失的字段会是零值；
// 我们只上报非零或本平台确定可采的字段，避免污染后端看板。
// 为与 telegraf 默认行为对齐，未实现的平台特有字段（如 Linux 的 slab 细分）
// 暂不上报，等线上需要时再补。
type Mem struct {
	// virtualMemFn 为 mem.VirtualMemory 的注入点，便于测试替换。
	virtualMemFn func() (*mem.VirtualMemoryStat, error)
	// nowFn 用于测试注入固定时间戳。
	nowFn func() time.Time
}

// NewMem 构造一个 Mem 采集器，使用默认的 gopsutil 实现。
func NewMem() *Mem {
	return &Mem{
		virtualMemFn: mem.VirtualMemory,
		nowFn:        time.Now,
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

	// pct_used 自算 100 * used / total。不能直接用 gopsutil 的 vm.UsedPercent：
	// 在 Windows 上 gopsutil v3 mem_windows.go 对 UsedPercent 做了整数运算，
	// 会得到 43/48 这种整数值而非 43.64 的小数，和 telegraf win_perf_counters
	// 的浮点精度对不上（issue: 对比 bin/test.log 观察到）。
	var pctUsed float64
	if vm.Total > 0 {
		pctUsed = 100 * float64(vm.Used) / float64(vm.Total)
	}
	fields := map[string]interface{}{
		FieldTotal:          vm.Total,
		FieldAvailable:      vm.Available,
		FieldUsed:           vm.Used,
		FieldFree:           vm.Free,
		RenamedFieldPctUsed: pctUsed,
		FieldBuffered:       vm.Buffers,
		FieldCached:         vm.Cached,
		FieldActive:         vm.Active,
		FieldInactive:       vm.Inactive,
		FieldSlab:           vm.Slab,
		FieldWired:          vm.Wired,
		FieldShared:         vm.Shared,
	}
	// available_percent 不是 gopsutil 直接字段，参照 telegraf 的公式：
	// 100 * available / total（total 为 0 时跳过）。
	if vm.Total > 0 {
		fields[FieldAvailablePercent] = 100 * float64(vm.Available) / float64(vm.Total)
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
