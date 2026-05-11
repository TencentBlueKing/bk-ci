//go:build !loong64
// +build !loong64

package monitor

import (
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
	processesFn func() ([]*process.Process, error)
	statusFn    func(*process.Process) ([]string, error)
	threadsFn   func(*process.Process) (int32, error)
	nowFn       func() time.Time
}

// NewProcesses 返回默认采集器。
func NewProcesses() *Processes {
	return &Processes{
		processesFn: process.Processes,
		statusFn:    func(p *process.Process) ([]string, error) { return p.Status() },
		threadsFn:   func(p *process.Process) (int32, error) { return p.NumThreads() },
		nowFn:       time.Now,
	}
}

// Name 返回 "processes"。
func (p *Processes) Name() string { return MeasurementProcesses }

// Gather 遍历所有进程，按状态分桶。
// 个别进程在遍历过程中可能退出导致 Status() 返回 error，忽略该进程继续。
func (p *Processes) Gather() ([]Metric, error) {
	procs, err := p.processesFn()
	if err != nil {
		return nil, errors.Wrap(err, "processes: Processes failed")
	}
	var running, sleeping, stopped, zombies, totalThreads int64
	total := int64(len(procs))

	for _, proc := range procs {
		statuses, err := p.statusFn(proc)
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

		if n, err := p.threadsFn(proc); err == nil {
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
