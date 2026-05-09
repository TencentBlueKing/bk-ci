//go:build windows
// +build windows

package monitor

import "testing"

// TestNormalizeWinMetricTags_CPU 覆盖 Windows 下 cpu_detail 的 tag 归一：
// cpu 保留、instance 归一为 PDH 形式、objectname=Processor。
func TestNormalizeWinMetricTags_CPU(t *testing.T) {
	cases := []struct {
		in   map[string]string
		want map[string]string
	}{
		{
			in:   map[string]string{TagCPU: "cpu0", TagInstance: "cpu0"},
			want: map[string]string{TagCPU: "cpu0", TagInstance: "0", TagObjectName: "Processor"},
		},
		{
			in:   map[string]string{TagCPU: "cpu-total", TagInstance: "cpu-total"},
			want: map[string]string{TagCPU: "cpu-total", TagInstance: "_Total", TagObjectName: "Processor"},
		},
		{
			// 边界：只带 instance 没 cpu 的情况也应归一
			in:   map[string]string{TagInstance: "cpu7"},
			want: map[string]string{TagInstance: "7", TagObjectName: "Processor"},
		},
	}
	for i, tc := range cases {
		got := normalizeWinMetricTags(RenamedCPUDetail, tc.in)
		for k, v := range tc.want {
			if got[k] != v {
				t.Errorf("case %d: %s = %q, want %q (got=%v)", i, k, got[k], v, got)
			}
		}
	}
}

// TestNormalizeWinMetricTags_IO 覆盖 Windows 下 io 的 tag 归一：
// name 保留、instance 从 name 派生、objectname=PhysicalDisk。
func TestNormalizeWinMetricTags_IO(t *testing.T) {
	got := normalizeWinMetricTags(RenamedIO, map[string]string{TagName: "0 C:"})
	if got[TagInstance] != "0 C:" {
		t.Errorf("instance = %q, want %q", got[TagInstance], "0 C:")
	}
	if got[TagObjectName] != "PhysicalDisk" {
		t.Errorf("objectname = %q", got[TagObjectName])
	}
	// name 保留做兼容
	if got[TagName] != "0 C:" {
		t.Errorf("name tag should be preserved, got %q", got[TagName])
	}
}

// TestNormalizeWinMetricTags_Net 覆盖 Windows 下 net 的 tag 归一：
// interface 保留、instance 从 interface 派生、objectname=Network Interface。
func TestNormalizeWinMetricTags_Net(t *testing.T) {
	got := normalizeWinMetricTags(MeasurementNet, map[string]string{TagInterface: "以太网 5"})
	if got[TagInstance] != "以太网 5" {
		t.Errorf("instance = %q", got[TagInstance])
	}
	if got[TagObjectName] != "Network Interface" {
		t.Errorf("objectname = %q", got[TagObjectName])
	}
	if got[TagInterface] != "以太网 5" {
		t.Errorf("interface tag should be preserved, got %q", got[TagInterface])
	}
}

// TestNormalizeWinMetricTags_OtherMeasurementUntouched 覆盖非 cpu/io/net
// 的 measurement 不应被归一（例如 disk / mem / load）。
func TestNormalizeWinMetricTags_OtherMeasurementUntouched(t *testing.T) {
	in := map[string]string{TagDevice: "C:", TagFstype: "NTFS"}
	got := normalizeWinMetricTags(MeasurementDisk, in)
	// 不应出现 objectname / instance
	if _, ok := got[TagObjectName]; ok {
		t.Error("disk measurement should not gain objectname tag")
	}
	if _, ok := got[TagInstance]; ok {
		t.Error("disk measurement should not gain instance tag")
	}
	// 原 tag 保留
	if got[TagDevice] != "C:" || got[TagFstype] != "NTFS" {
		t.Errorf("disk tags lost: %+v", got)
	}
}

func TestCpuTagValueToInstance(t *testing.T) {
	cases := []struct{ in, want string }{
		{"cpu0", "0"},
		{"cpu7", "7"},
		{"cpu-total", "_Total"},
		{"", ""},
		{"random", "random"},
	}
	for _, tc := range cases {
		if got := cpuTagValueToInstance(tc.in); got != tc.want {
			t.Errorf("cpuTagValueToInstance(%q) = %q, want %q", tc.in, got, tc.want)
		}
	}
}

// TestCPU_WinTagNormalizerHookSet 确认 init() 成功挂钩子（非 nil）。
func TestCPU_WinTagNormalizerHookSet(t *testing.T) {
	if winTagNormalizerFn == nil {
		t.Fatal("winTagNormalizerFn must be set on Windows by tag_normalize_win.go init()")
	}
}
