//go:build !loong64
// +build !loong64

package monitor

import (
	"runtime"
	"strconv"
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v3/cpu"
)

// CPU 对齐 telegraf plugins/inputs/cpu。
//
// 产生的 metric 与 telegraf 的默认配置（PerCPU=true、TotalCPU=true、
// ReportActive=false）一致：每个 logical CPU 一条 metric + 一条
// cpu-total 汇总。CollectCPUTime 在 telegrafConf 中为 false，故只上报百分比。
//
// Field 单位为百分比（0-100），基于两次采样间的 delta 计算。首次 Gather
// 由于无上一次数据，会返回空 metric 列表（telegraf 行为一致）。
type CPU struct {
	// timesFn 获取当前 cpu 时间统计。percpu=true 返回每核；
	// 额外再单独调一次 percpu=false 获取汇总。
	timesFn func(percpu bool) ([]cpu.TimesStat, error)
	nowFn   func() time.Time

	// last 保存上次采样，用于计算 delta。
	// key 为 TimesStat.CPU（"cpu0"、"cpu-total" 等）。
	last map[string]cpu.TimesStat
}

// winTagNormalizerFn 是 Windows 构建下的 tag 归一钩子（见
// tag_normalize_win.go 的 init）；非 Windows 为 nil。Gather 循环中调用，
// 不存在时保留原 Tags。以包级变量而非 runtime.GOOS 判断，避免通用 input
// 文件引用 Windows-only 函数导致 linker 失败。
var winTagNormalizerFn func(measurement string, tags map[string]string) map[string]string

// NewCPU 返回默认 CPU 采集器。
func NewCPU() *CPU {
	return &CPU{
		timesFn: cpu.Times,
		nowFn:   time.Now,
		last:    make(map[string]cpu.TimesStat),
	}
}

// Name 返回 measurement 名 "cpu_detail"（规范名，对齐 BK-CI 后端）。
func (c *CPU) Name() string { return RenamedCPUDetail }

// Gather 采集每核 + total 的 cpu 使用率 metric。
//
// 采样算法：对同一个 CPU id，diff 当前 total 时间与上次 total 时间，
// 再按分类 diff 除以 total diff 得到百分比。若两次采样间 total 时间未前进
// （可能发生于虚拟机暂停），跳过该 CPU。
func (c *CPU) Gather() ([]Metric, error) {
	percpu, err := c.timesFn(true)
	if err != nil {
		return nil, errors.Wrap(err, "cpu: Times(per) failed")
	}
	total, err := c.timesFn(false)
	if err != nil {
		return nil, errors.Wrap(err, "cpu: Times(total) failed")
	}

	now := c.nowFn()
	all := append([]cpu.TimesStat{}, percpu...)
	all = append(all, total...)

	out := make([]Metric, 0, len(all))
	for _, cur := range all {
		id := cur.CPU
		if id == "" {
			continue
		}
		// gopsutil 在 total 模式下返回 CPU="cpu-total"，与 telegraf 一致。
		prev, ok := c.last[id]
		// 记录本次采样，供下次 diff 使用（无论能否输出本次）。
		c.last[id] = cur
		if !ok {
			continue // 首次采样无 delta，跳过
		}

		fields := diffCPUToFields(prev, cur)
		if fields == nil {
			continue
		}
		tags := map[string]string{TagInstance: id, TagCPU: id}
		if winTagNormalizerFn != nil {
			tags = winTagNormalizerFn(RenamedCPUDetail, tags)
		}
		out = append(out, Metric{
			Name: RenamedCPUDetail,
			// 双写 tag：instance 和 cpu 都保留同一个值。
			//   - instance：与 Windows PDH win_cpu 对齐（telegraf win_perf_counters
			//     产出 instance=0/_Total/objectname=Processor）；monitor 三平台
			//     统一用 instance 便于跨平台看板查询
			//   - cpu：与 Linux/Mac 下 telegraf inputs.cpu 对齐（cpu=cpu0 / cpu-total）
			// 后端无论按哪个 tag key 过滤都能命中，做兼容冗余。
			// Windows 构建下还会额外注入 objectname 并把 instance 从 "cpu0"
			// 归一到 "0"（见 tag_normalize_win.go）。
			Tags:      tags,
			Fields:    fields,
			Timestamp: now,
		})
	}
	return out, nil
}

// cpuTotalTime 汇总 telegraf plugins/inputs/cpu 里一样的 7 个大类时间总和
// （不含 Guest/GuestNice，因为它们已经包含在 User/Nice 里；对齐 telegraf
// 的 totalCPUTime 逻辑）。
func cpuTotalTime(t cpu.TimesStat) float64 {
	return t.User + t.System + t.Idle + t.Nice +
		t.Iowait + t.Irq + t.Softirq + t.Steal
}

// diffCPUToFields 计算两次采样间的百分比，返回 nil 表示 delta 非正。
func diffCPUToFields(prev, cur cpu.TimesStat) map[string]interface{} {
	totalDelta := cpuTotalTime(cur) - cpuTotalTime(prev)
	if totalDelta <= 0 {
		return nil
	}
	pct := func(a, b float64) float64 {
		d := a - b
		if d < 0 {
			d = 0 // 时钟回拨等异常保护
		}
		return 100 * d / totalDelta
	}
	fields := map[string]interface{}{
		RenamedFieldUser:    round4(pct(cur.User, prev.User)),
		RenamedFieldSystem:  round4(pct(cur.System, prev.System)),
		RenamedFieldIdle:    round4(pct(cur.Idle, prev.Idle)),
		FieldUsageNice:      round4(pct(cur.Nice, prev.Nice)),
		RenamedFieldIowait:  round4(pct(cur.Iowait, prev.Iowait)),
		FieldUsageIrq:       round4(pct(cur.Irq, prev.Irq)),
		FieldUsageSoftirq:   round4(pct(cur.Softirq, prev.Softirq)),
		FieldUsageSteal:     round4(pct(cur.Steal, prev.Steal)),
		FieldUsageGuest:     round4(pct(cur.Guest, prev.Guest)),
		FieldUsageGuestNice: round4(pct(cur.GuestNice, prev.GuestNice)),
	}
	// Windows 专属派生字段：Percent_Processor_Time = 100 - idle。
	// 兼容 telegraf win_perf_counters 时代的看板/告警（Linux/macOS 的 telegraf
	// inputs.cpu 原生没有该字段，保持一致不输出）。
	if runtime.GOOS == "windows" {
		idle := round4(pct(cur.Idle, prev.Idle))
		fields[WinFieldPercentProcessorTime] = round4(100 - idle)
	}
	return fields
}

// round4 把浮点数保留 4 位小数，避免 JSON 中 0.12345678 这种冗长输出。
// 与 telegraf 的默认精度行为接近。
func round4(v float64) float64 {
	s := strconv.FormatFloat(v, 'f', 4, 64)
	r, _ := strconv.ParseFloat(s, 64)
	return r
}
