//go:build !loong64
// +build !loong64

package monitor

import (
	"errors"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v4/mem"
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
		nowFn:    func() time.Time { return ts },
		platform: "linux", // Linux 分支字段最全，覆盖通用 + 专属字段断言
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

	// 通用必备字段（所有平台都应输出）
	required := []string{
		"total", "available", "used", "pct_used", "available_percent",
	}
	for _, f := range required {
		if _, ok := got.Fields[f]; !ok {
			t.Errorf("missing required field %q", f)
		}
	}
	// Linux 分支：telegraf inputs.mem/linux 的必备字段集合
	linuxRequired := []string{
		"active", "buffered", "cached", "commit_limit", "committed_as",
		"dirty", "free", "high_free", "high_total",
		"huge_pages_free", "huge_page_size", "huge_pages_total",
		"inactive", "low_free", "low_total", "mapped", "page_tables",
		"shared", "slab", "sreclaimable", "sunreclaim",
		"swap_cached", "swap_free", "swap_total",
		"vmalloc_chunk", "vmalloc_total", "vmalloc_used",
		"write_back", "write_back_tmp",
	}
	for _, f := range linuxRequired {
		if _, ok := got.Fields[f]; !ok {
			t.Errorf("missing linux-specific field %q", f)
		}
	}

	// available_percent 应当是 100 * 8000 / 16000 = 50
	if v, ok := got.Fields["available_percent"].(float64); !ok || v != 50.0 {
		t.Errorf("available_percent = %v (%T), want 50.0 (float64)", got.Fields["available_percent"], got.Fields["available_percent"])
	}
	// pct_used 自算 100 * 8000 / 16000 = 50，不是 vm.UsedPercent
	if v, ok := got.Fields["pct_used"].(float64); !ok || v != 50.0 {
		t.Errorf("pct_used = %v (%T), want 50.0 (float64)", got.Fields["pct_used"], got.Fields["pct_used"])
	}
}

// TestMem_Gather_PctUsedIgnoresGopsutil 覆盖 Windows gopsutil v3 UsedPercent
// 整数截断回归：即便 vm.UsedPercent=43（整数），pct_used 也要自算为 43.6...
func TestMem_Gather_PctUsedIgnoresGopsutil(t *testing.T) {
	m := &Mem{
		virtualMemFn: fakeVirtualMem(&mem.VirtualMemoryStat{
			Total: 100, Used: 43, Available: 57,
			UsedPercent: 43, // 被截断的整数值，应被忽略
		}, nil),
		nowFn: time.Now,
	}
	metrics, _ := m.Gather()
	v, _ := metrics[0].Fields["pct_used"].(float64)
	if v != 43.0 {
		// 100 * 43 / 100 = 43，但类型必须是 float64（非 int / uint）
		t.Errorf("pct_used = %v, want 43.0 float64", v)
	}
	// 同理验证：87/200=43.5，这种小数不会被截断
	m2 := &Mem{
		virtualMemFn: fakeVirtualMem(&mem.VirtualMemoryStat{
			Total: 200, Used: 87, UsedPercent: 43,
		}, nil),
		nowFn: time.Now,
	}
	metrics2, _ := m2.Gather()
	v2, _ := metrics2[0].Fields["pct_used"].(float64)
	if v2 != 43.5 {
		t.Errorf("pct_used = %v, want 43.5 (self-computed, not gopsutil's int-truncated 43)", v2)
	}
}

// TestMem_Gather_PlatformBranches 验证各平台字段分支严格对齐 telegraf。
// darwin / freebsd / openbsd / linux 的字段集合彼此不同，空字符串走通用分支。
func TestMem_Gather_PlatformBranches(t *testing.T) {
	cases := map[string]struct {
		platform string
		hasKeys  []string
		noKeys   []string
	}{
		"darwin": {
			platform: "darwin",
			hasKeys:  []string{"active", "free", "inactive", "wired"},
			noKeys:   []string{"buffered", "cached", "slab", "commit_limit", "laundry"},
		},
		"freebsd": {
			platform: "freebsd",
			hasKeys:  []string{"active", "buffered", "cached", "free", "inactive", "laundry", "wired"},
			noKeys:   []string{"slab", "commit_limit", "swap_total"},
		},
		"openbsd": {
			platform: "openbsd",
			hasKeys:  []string{"active", "cached", "free", "inactive", "wired"},
			noKeys:   []string{"buffered", "slab", "commit_limit", "laundry"},
		},
		"windows": {
			platform: "windows",
			hasKeys:  []string{"total", "available", "used", "pct_used", "available_percent"},
			// Windows 走通用分支，没有任何平台专属字段
			noKeys: []string{"active", "free", "buffered", "cached", "slab", "wired", "laundry", "commit_limit"},
		},
	}
	for name, tc := range cases {
		tc := tc
		t.Run(name, func(t *testing.T) {
			m := &Mem{
				virtualMemFn: fakeVirtualMem(&mem.VirtualMemoryStat{
					Total: 1000, Used: 500, Available: 500,
					Active: 1, Free: 2, Inactive: 3, Wired: 4,
					Buffers: 5, Cached: 6, Laundry: 7,
				}, nil),
				nowFn:    time.Now,
				platform: tc.platform,
			}
			metrics, err := m.Gather()
			if err != nil {
				t.Fatal(err)
			}
			got := metrics[0].Fields
			for _, k := range tc.hasKeys {
				if _, ok := got[k]; !ok {
					t.Errorf("[%s] missing %q", tc.platform, k)
				}
			}
			for _, k := range tc.noKeys {
				if _, ok := got[k]; ok {
					t.Errorf("[%s] unexpected %q present", tc.platform, k)
				}
			}
		})
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
