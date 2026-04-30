//go:build !loong64
// +build !loong64

package monitor

import (
	"errors"
	"runtime"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v4/cpu"
)

// fakeCPUTimes 按 percpu 参数返回预设数据。seq 用来模拟两次调用返回不同值。
func fakeCPUTimes(pcpu, total []cpu.TimesStat, err error) func(bool) ([]cpu.TimesStat, error) {
	return func(percpu bool) ([]cpu.TimesStat, error) {
		if err != nil {
			return nil, err
		}
		if percpu {
			return pcpu, nil
		}
		return total, nil
	}
}

func TestCPU_Name(t *testing.T) {
	if n := NewCPU().Name(); n != RenamedCPUDetail {
		t.Errorf("Name() = %q", n)
	}
}

func TestCPU_Gather_FirstCallNoMetrics(t *testing.T) {
	// 首次采样无上次数据，应返回空列表
	c := &CPU{
		timesFn: fakeCPUTimes(
			[]cpu.TimesStat{{CPU: "cpu0", User: 100, Idle: 900}},
			[]cpu.TimesStat{{CPU: "cpu-total", User: 100, Idle: 900}},
			nil,
		),
		nowFn: time.Now,
		last:  make(map[string]cpu.TimesStat),
	}
	metrics, err := c.Gather()
	if err != nil {
		t.Fatalf("Gather: %v", err)
	}
	if len(metrics) != 0 {
		t.Errorf("first Gather should return 0 metrics, got %d", len(metrics))
	}
	if len(c.last) != 2 {
		t.Errorf("first Gather should populate last cache, got %d", len(c.last))
	}
}

func TestCPU_Gather_SecondCallProducesMetrics(t *testing.T) {
	// 第一轮
	pcpu1 := []cpu.TimesStat{{CPU: "cpu0", User: 100, Idle: 900}}
	total1 := []cpu.TimesStat{{CPU: "cpu-total", User: 100, Idle: 900}}
	// 第二轮：user +50, idle +50 → 各占 50%
	pcpu2 := []cpu.TimesStat{{CPU: "cpu0", User: 150, Idle: 950}}
	total2 := []cpu.TimesStat{{CPU: "cpu-total", User: 150, Idle: 950}}

	calls := 0
	c := &CPU{
		timesFn: func(percpu bool) ([]cpu.TimesStat, error) {
			calls++
			// Gather 内部对每次 Gather 会调用 timesFn 两次（percpu + total），
			// 所以第一轮用 1/2 两次调用，第二轮用 3/4 两次调用。
			if calls <= 2 {
				if percpu {
					return pcpu1, nil
				}
				return total1, nil
			}
			if percpu {
				return pcpu2, nil
			}
			return total2, nil
		},
		nowFn: time.Now,
		last:  make(map[string]cpu.TimesStat),
	}
	if _, err := c.Gather(); err != nil {
		t.Fatalf("warmup: %v", err)
	}
	metrics, err := c.Gather()
	if err != nil {
		t.Fatalf("second Gather: %v", err)
	}
	if len(metrics) != 2 {
		t.Fatalf("want 2 metrics (cpu0 + cpu-total), got %d", len(metrics))
	}

	// 找 cpu0 —— 用 cpu tag 定位（双写且不受 Windows 归一影响）。
	var cpu0 *Metric
	for i := range metrics {
		if metrics[i].Tags[TagCPU] == "cpu0" {
			cpu0 = &metrics[i]
			break
		}
	}
	if cpu0 == nil {
		t.Fatal("cpu0 metric missing")
	}
	if v, ok := cpu0.Fields[RenamedFieldUser].(float64); !ok || v != 50 {
		t.Errorf("user = %v, want 50", cpu0.Fields[RenamedFieldUser])
	}
	if v, ok := cpu0.Fields[RenamedFieldIdle].(float64); !ok || v != 50 {
		t.Errorf("idle = %v, want 50", cpu0.Fields[RenamedFieldIdle])
	}
	// 双写 tag：instance 和 cpu 必须都存在。
	//   - Linux / macOS 下 instance 保持 "cpu0"
	//   - Windows 下 instance 被 normalizeWinMetricTags 归一为 "0"，并多出 objectname=Processor
	if cpu0.Tags[TagCPU] != "cpu0" {
		t.Errorf("tag cpu = %q, want cpu0 (compat alias)", cpu0.Tags[TagCPU])
	}
	wantInstance := "cpu0"
	if runtime.GOOS == "windows" {
		wantInstance = "0"
		if cpu0.Tags[TagObjectName] != "Processor" {
			t.Errorf("windows: tag objectname = %q, want Processor", cpu0.Tags[TagObjectName])
		}
	}
	if cpu0.Tags[TagInstance] != wantInstance {
		t.Errorf("tag instance = %q, want %q", cpu0.Tags[TagInstance], wantInstance)
	}
}

func TestCPU_Gather_ErrorPropagates(t *testing.T) {
	sentinel := errors.New("boom")
	c := &CPU{timesFn: fakeCPUTimes(nil, nil, sentinel), nowFn: time.Now, last: map[string]cpu.TimesStat{}}
	if _, err := c.Gather(); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v, want wrapping sentinel", err)
	}
}

func TestCPU_Gather_ZeroTotalDeltaSkipped(t *testing.T) {
	// 两次采样完全相同 → totalDelta=0 → 跳过
	pcpu := []cpu.TimesStat{{CPU: "cpu0", User: 100, Idle: 900}}
	total := []cpu.TimesStat{{CPU: "cpu-total", User: 100, Idle: 900}}
	c := &CPU{
		timesFn: fakeCPUTimes(pcpu, total, nil),
		nowFn:   time.Now,
		last:    make(map[string]cpu.TimesStat),
	}
	_, _ = c.Gather()
	metrics, _ := c.Gather()
	if len(metrics) != 0 {
		t.Errorf("identical samples should yield 0 metrics, got %d", len(metrics))
	}
}
