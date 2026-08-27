//go:build !loong64
// +build !loong64

package monitor

import (
	"bytes"
	"context"
	"os/exec"
	"runtime"
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v4/process"
)

// Processes 对齐 telegraf plugins/inputs/processes。按进程状态分桶计数：
// running / sleeping / stopped / zombies / total / total_threads。
//
// gopsutil 的 Status() 返回 POSIX 兼容的状态字符（"R"/"S"/"T"/"Z"/"I"/"D"
// 等），按 telegraf 的映射：
//
//	R -> running
//	S/I -> sleeping（I 是 Linux 特有的 idle kernel thread，归入 sleeping）
//	D   -> sleeping（Uninterruptible sleep）
//	T -> stopped
//	Z -> zombies
type Processes struct {
	processesFn func(ctx context.Context) ([]*process.Process, error)
	statusFn    func(ctx context.Context, p *process.Process) ([]string, error)
	threadsFn   func(ctx context.Context, p *process.Process) (int32, error)
	execPSFn    func(ctx context.Context) ([]byte, error)
	nowFn       func() time.Time
}

// NewProcesses 返回默认采集器。
func NewProcesses() *Processes {
	p := &Processes{nowFn: time.Now}
	if runtime.GOOS == "darwin" {
		// 与 Telegraf processes input 一致：macOS 一次性读取全部进程状态，
		// 避免 gopsutil 对每个 PID 分别启动一次 ps。
		p.execPSFn = execProcessStates
		return p
	}

	p.processesFn = process.ProcessesWithContext
	p.statusFn = func(ctx context.Context, p *process.Process) ([]string, error) {
		return p.StatusWithContext(ctx)
	}
	p.threadsFn = func(ctx context.Context, p *process.Process) (int32, error) {
		return p.NumThreadsWithContext(ctx)
	}
	return p
}

func execProcessStates(ctx context.Context) ([]byte, error) {
	bin, err := exec.LookPath("ps")
	if err != nil {
		return nil, err
	}
	return exec.CommandContext(ctx, bin, "axo", "state").Output()
}

// Name 返回 "processes"。
func (p *Processes) Name() string { return MeasurementProcesses }

// Gather 遍历所有进程，按状态分桶。
// 个别进程在遍历过程中可能退出导致 Status() 返回 error，忽略该进程继续。
func (p *Processes) Gather(ctx context.Context) ([]Metric, error) {
	if p.execPSFn != nil {
		return p.gatherFromPS(ctx)
	}

	procs, err := p.processesFn(ctx)
	if err != nil {
		return nil, errors.Wrap(err, "processes: Processes failed")
	}
	var running, sleeping, stopped, zombies, totalThreads int64
	total := int64(len(procs))

	for _, proc := range procs {
		statuses, err := p.statusFn(ctx, proc)
		if err != nil || len(statuses) == 0 {
			continue
		}
		// gopsutil 返回切片（可能包含多状态，如 "R" + "+"），取第一个主状态
		switch statuses[0] {
		case "R", "running":
			running++
		case "S", "I", "D", "sleep", "idle":
			sleeping++
		case "T", "stop":
			stopped++
		case "Z", "zombie":
			zombies++
		}

		if n, err := p.threadsFn(ctx, proc); err == nil {
			totalThreads += int64(n)
		}
	}

	return []Metric{{
		Name: MeasurementProcesses,
		Fields: map[string]interface{}{
			FieldRunning:      running,
			FieldSleeping:     sleeping,
			FieldStopped:      stopped,
			FieldZombies:      zombies,
			FieldTotal:        total,
			FieldTotalThreads: totalThreads,
		},
		Timestamp: p.nowFn(),
	}}, nil
}

// gatherFromPS 使用一次 ps axo state 完成 macOS 的进程状态统计。STAT 列可能
// 带有附加字符（例如 Ss、R+），只取首字符，与 Telegraf 的解析方式一致。
// total_threads 在 Telegraf 中仅 Linux 提供，因此 Darwin 结果不输出该字段。
func (p *Processes) gatherFromPS(ctx context.Context) ([]Metric, error) {
	out, err := p.execPSFn(ctx)
	if err != nil {
		return nil, errors.Wrap(err, "processes: ps axo state failed")
	}

	var running, sleeping, stopped, zombies, total int64
	for i, status := range bytes.Fields(out) {
		if i == 0 && string(status) == "STAT" {
			continue
		}
		if len(status) == 0 {
			continue
		}
		total++
		switch status[0] {
		case 'R':
			running++
		case 'S', 'I', 'U', 'D', 'L':
			// 现有上报结构没有 Telegraf 的 blocked 字段，保持历史语义，
			// 将不可中断睡眠归入 sleeping。
			sleeping++
		case 'T':
			stopped++
		case 'Z':
			zombies++
		}
	}

	return []Metric{{
		Name: MeasurementProcesses,
		Fields: map[string]interface{}{
			FieldRunning:  running,
			FieldSleeping: sleeping,
			FieldStopped:  stopped,
			FieldZombies:  zombies,
			FieldTotal:    total,
		},
		Timestamp: p.nowFn(),
	}}, nil
}
