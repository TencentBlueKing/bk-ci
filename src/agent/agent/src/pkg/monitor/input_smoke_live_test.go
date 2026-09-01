//go:build !loong64
// +build !loong64

package monitor

import (
	"testing"
	"time"
)

// TestAllInputs_LiveSmoke 是针对真实系统调用的 smoke 测试。
// 使用 go test -run LiveSmoke 手动触发，默认在 -short 模式下跳过。
// 目的：在开发机上一次性验证所有 input 能在本平台用真实 gopsutil 采到数据。
func TestAllInputs_LiveSmoke(t *testing.T) {
	if testing.Short() {
		t.Skip("skip live smoke in -short mode")
	}
	type entry struct {
		name  string
		input Input
		// needTwoCalls 标记 CPU 这类需要两次采样才能产生 metric 的 input
		needTwoCalls bool
	}
	all := []entry{
		{"cpu", NewCPU(), true},
		{"mem", NewMem(), false},
		{"disk", NewDisk(), false},
		{"diskio", NewDiskIO(), false},
		{"net", NewNet(), false},
		{"netstat", NewNetstat(), false},
		{"swap", NewSwap(), false},
		{"system", NewSystem(), false},
		{"kernel", NewKernel(), false},
		{"processes", NewProcesses(), false},
	}

	for _, e := range all {
		e := e
		t.Run(e.name, func(t *testing.T) {
			if e.needTwoCalls {
				// 预热一次
				if _, err := e.input.Gather(); err != nil {
					t.Fatalf("warmup Gather: %v", err)
				}
				// CPU 时间是累加计数器，相邻两次采样间至少需要一次调度
				// tick 才会有差。sleep 150ms 足够跨一次 Windows 默认调度周期。
				time.Sleep(150 * time.Millisecond)
			}
			metrics, err := e.input.Gather()
			if err != nil {
				t.Fatalf("Gather: %v", err)
			}
			if len(metrics) == 0 {
				t.Fatal("want at least 1 metric")
			}
			for _, m := range metrics {
				if m.Name == "" {
					t.Error("metric with empty Name")
				}
				if len(m.Fields) == 0 {
					t.Errorf("metric %q has no fields", m.Name)
				}
			}
			// 检查 measurement 名与声明一致
			want := e.input.Name()
			for _, m := range metrics {
				if m.Name != want {
					t.Errorf("Name() says %q but metric has %q", want, m.Name)
				}
			}
			_ = Rename(metrics)
			t.Logf("%s: %d metrics, sample=%+v", e.name, len(metrics), metrics[0])
		})
	}
}
