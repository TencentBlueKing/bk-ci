//go:build !loong64
// +build !loong64

package monitor

import (
	"errors"
	"testing"
	"time"
)

func TestKernel_Name(t *testing.T) {
	if n := NewKernel().Name(); n != RenamedEnv {
		t.Errorf("Name() = %q", n)
	}
}

func TestKernel_Gather_Success(t *testing.T) {
	k := &Kernel{
		bootTimeFn:   func() (uint64, error) { return 1700000000, nil },
		nowFn:        time.Now,
		linuxExtraFn: nil, // 非 Linux 场景，不产出 Linux 专属字段
	}
	metrics, err := k.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if metrics[0].Name != RenamedEnv {
		t.Errorf("measurement = %q, want env", metrics[0].Name)
	}
	if v, _ := metrics[0].Fields[FieldUptime].(uint64); v != 1700000000 {
		t.Errorf("uptime = %v", metrics[0].Fields[FieldUptime])
	}
	// 非 Linux 场景不应有 Linux 专属字段
	for _, f := range []string{FieldInterrupts, FieldContextSwitches, RenamedFieldProcs, FieldEntropyAvail} {
		if _, ok := metrics[0].Fields[f]; ok {
			t.Errorf("unexpected linux-specific field %q in non-linux gather", f)
		}
	}
}

func TestKernel_Gather_Error(t *testing.T) {
	sentinel := errors.New("boom")
	k := &Kernel{bootTimeFn: func() (uint64, error) { return 0, sentinel }, nowFn: time.Now}
	if _, err := k.Gather(); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v", err)
	}
}

// TestKernel_Gather_LinuxExtraMerged 模拟 Linux 分支：linuxExtraFn 返回
// interrupts/context_switches/procs/entropy_avail 等字段，应合并到 env metric。
func TestKernel_Gather_LinuxExtraMerged(t *testing.T) {
	k := &Kernel{
		bootTimeFn: func() (uint64, error) { return 1700000000, nil },
		nowFn:      time.Now,
		linuxExtraFn: func() (map[string]interface{}, error) {
			return map[string]interface{}{
				FieldInterrupts:      int64(12345),
				FieldContextSwitches: int64(67890),
				RenamedFieldProcs:    int64(111),
				FieldEntropyAvail:    int64(256),
				FieldBootTime:        int64(1700000100), // 即使与 uptime 同名域也应被透传
			}, nil
		},
	}
	metrics, err := k.Gather()
	if err != nil {
		t.Fatal(err)
	}
	want := map[string]int64{
		FieldInterrupts:      12345,
		FieldContextSwitches: 67890,
		RenamedFieldProcs:    111,
		FieldEntropyAvail:    256,
		FieldBootTime:        1700000100,
	}
	for k, v := range want {
		got, ok := metrics[0].Fields[k].(int64)
		if !ok || got != v {
			t.Errorf("field %s = %v (%T), want %v int64", k, metrics[0].Fields[k], metrics[0].Fields[k], v)
		}
	}
	// uptime 来源于 bootTimeFn，仍保留
	if v, _ := metrics[0].Fields[FieldUptime].(uint64); v != 1700000000 {
		t.Errorf("uptime = %v", metrics[0].Fields[FieldUptime])
	}
}

// TestKernel_Gather_LinuxExtraError 确认 /proc 读取失败会降级——仅返回
// uptime，不报错、不丢 env metric。
func TestKernel_Gather_LinuxExtraError(t *testing.T) {
	k := &Kernel{
		bootTimeFn: func() (uint64, error) { return 1700000000, nil },
		nowFn:      time.Now,
		linuxExtraFn: func() (map[string]interface{}, error) {
			return nil, errors.New("permission denied /proc/stat")
		},
	}
	metrics, err := k.Gather()
	if err != nil {
		t.Fatalf("extra fn error should be swallowed, got %v", err)
	}
	if len(metrics) != 1 {
		t.Fatalf("want 1 metric, got %d", len(metrics))
	}
	if v, _ := metrics[0].Fields[FieldUptime].(uint64); v != 1700000000 {
		t.Errorf("uptime = %v", metrics[0].Fields[FieldUptime])
	}
	if _, ok := metrics[0].Fields[FieldEntropyAvail]; ok {
		t.Error("entropy_avail should be absent on linuxExtraFn error")
	}
}
