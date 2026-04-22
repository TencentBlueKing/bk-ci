//go:build !loong64
// +build !loong64

package monitor

import (
	"errors"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v3/mem"
)

// fakeVirtualMem 返回预设的 VirtualMemoryStat，用于 Gather 的确定性测试。
func fakeVirtualMem(stat *mem.VirtualMemoryStat, err error) func() (*mem.VirtualMemoryStat, error) {
	return func() (*mem.VirtualMemoryStat, error) {
		return stat, err
	}
}

func TestMem_Name(t *testing.T) {
	if n := NewMem().Name(); n != "mem" {
		t.Fatalf("Name() = %q, want %q", n, "mem")
	}
}

func TestMem_Gather_Success(t *testing.T) {
	ts := time.Date(2026, 4, 22, 10, 0, 0, 0, time.UTC)
	m := &Mem{
		virtualMemFn: fakeVirtualMem(&mem.VirtualMemoryStat{
			Total:       16000,
			Available:   8000,
			Used:        8000,
			Free:        4000,
			UsedPercent: 50,
			Buffers:     100,
			Cached:      500,
			Active:      1000,
			Inactive:    200,
			Slab:        50,
			Wired:       0,
			Shared:      0,
		}, nil),
		nowFn: func() time.Time { return ts },
	}

	metrics, err := m.Gather()
	if err != nil {
		t.Fatalf("Gather() unexpected error: %v", err)
	}
	if len(metrics) != 1 {
		t.Fatalf("want 1 metric, got %d", len(metrics))
	}
	got := metrics[0]
	if got.Name != "mem" {
		t.Errorf("metric name = %q, want mem", got.Name)
	}
	if !got.Timestamp.Equal(ts) {
		t.Errorf("timestamp = %v, want %v", got.Timestamp, ts)
	}

	// 校验 telegraf 对齐的必备 field 都存在
	required := []string{
		"total", "available", "used", "free", "used_percent",
		"buffered", "cached", "active", "inactive", "slab",
		"wired", "shared", "available_percent",
	}
	for _, f := range required {
		if _, ok := got.Fields[f]; !ok {
			t.Errorf("missing required field %q", f)
		}
	}

	// available_percent 应当是 100 * 8000 / 16000 = 50
	if v, ok := got.Fields["available_percent"].(float64); !ok || v != 50.0 {
		t.Errorf("available_percent = %v (%T), want 50.0 (float64)", got.Fields["available_percent"], got.Fields["available_percent"])
	}
}

func TestMem_Gather_ErrorPropagates(t *testing.T) {
	sentinel := errors.New("boom")
	m := &Mem{
		virtualMemFn: fakeVirtualMem(nil, sentinel),
		nowFn:        time.Now,
	}
	_, err := m.Gather()
	if err == nil {
		t.Fatal("Gather() expected error, got nil")
	}
	if !errors.Is(err, sentinel) {
		// errors.Wrap 包装过，errors.Is 能穿透
		t.Errorf("error should wrap sentinel, got %v", err)
	}
}

func TestMem_Gather_NilStat(t *testing.T) {
	m := &Mem{
		virtualMemFn: fakeVirtualMem(nil, nil),
		nowFn:        time.Now,
	}
	_, err := m.Gather()
	if err == nil {
		t.Fatal("Gather() with nil stat should return error")
	}
}

// TestMem_Gather_ZeroTotal 验证 Total=0 时不产生 available_percent 字段，
// 避免除零。
func TestMem_Gather_ZeroTotal(t *testing.T) {
	m := &Mem{
		virtualMemFn: fakeVirtualMem(&mem.VirtualMemoryStat{Total: 0}, nil),
		nowFn:        time.Now,
	}
	metrics, err := m.Gather()
	if err != nil {
		t.Fatalf("Gather() unexpected error: %v", err)
	}
	if _, ok := metrics[0].Fields["available_percent"]; ok {
		t.Error("available_percent should be absent when Total=0")
	}
}
