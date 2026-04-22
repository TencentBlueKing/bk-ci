//go:build !out
// +build !out

package monitor

import (
	"reflect"
	"testing"
)

func TestRename_Empty(t *testing.T) {
	if got := Rename(nil); got != nil {
		t.Errorf("Rename(nil) = %v, want nil", got)
	}
	if got := Rename([]Metric{}); len(got) != 0 {
		t.Errorf("Rename(empty) = %v, want empty", got)
	}
}

func TestRename_CPU(t *testing.T) {
	in := []Metric{{
		Name: "cpu",
		Fields: map[string]interface{}{
			"usage_user":   1.0,
			"usage_system": 2.0,
			"usage_idle":   97.0,
			"usage_iowait": 0.0,
		},
	}}
	got := Rename(in)[0]
	if got.Name != "cpu_detail" {
		t.Errorf("measurement should be cpu_detail, got %q", got.Name)
	}
	wantFields := map[string]interface{}{
		"user":   1.0,
		"system": 2.0,
		"idle":   97.0,
		"iowait": 0.0,
	}
	if !reflect.DeepEqual(got.Fields, wantFields) {
		t.Errorf("cpu fields mismatch:\n got  %v\n want %v", got.Fields, wantFields)
	}
}

func TestRename_DiskSpecialCase(t *testing.T) {
	// disk measurement 下 used_percent 应该 → in_use，而不是 global 的 pct_used
	in := []Metric{{
		Name: "disk",
		Fields: map[string]interface{}{
			"used_percent": 80.0,
			"total":        100.0,
		},
	}}
	got := Rename(in)[0]
	if got.Name != "disk" {
		t.Errorf("disk measurement should stay 'disk', got %q", got.Name)
	}
	if _, ok := got.Fields["pct_used"]; ok {
		t.Error("disk.used_percent must not map to pct_used")
	}
	if v, ok := got.Fields["in_use"]; !ok || v != 80.0 {
		t.Errorf("disk.used_percent should map to in_use=80.0, got %v", got.Fields["in_use"])
	}
	if v, ok := got.Fields["total"]; !ok || v != 100.0 {
		t.Errorf("disk.total should stay unchanged, got %v", got.Fields)
	}
}

func TestRename_MemUsedPercent(t *testing.T) {
	// mem measurement 下 used_percent → pct_used（走 global）
	in := []Metric{{
		Name:   "mem",
		Fields: map[string]interface{}{"used_percent": 42.5, "total": uint64(16000)},
	}}
	got := Rename(in)[0]
	if got.Name != "mem" {
		t.Errorf("mem measurement should stay, got %q", got.Name)
	}
	if v, ok := got.Fields["pct_used"]; !ok || v != 42.5 {
		t.Errorf("mem.used_percent should map to pct_used=42.5, got %v", got.Fields["pct_used"])
	}
	if _, ok := got.Fields["used_percent"]; ok {
		t.Error("original used_percent should be removed")
	}
}

func TestRename_DiskioMeasurementAndFields(t *testing.T) {
	in := []Metric{{
		Name: "diskio",
		Fields: map[string]interface{}{
			"read_bytes":  int64(100),
			"write_bytes": int64(200),
			"reads":       int64(10),
		},
	}}
	got := Rename(in)[0]
	if got.Name != "io" {
		t.Errorf("diskio -> io, got %q", got.Name)
	}
	if _, ok := got.Fields["rkb_s"]; !ok {
		t.Error("read_bytes should map to rkb_s")
	}
	if _, ok := got.Fields["wkb_s"]; !ok {
		t.Error("write_bytes should map to wkb_s")
	}
	if _, ok := got.Fields["reads"]; !ok {
		t.Error("reads is not in rename table and should be preserved")
	}
}

func TestRename_SystemToLoad(t *testing.T) {
	in := []Metric{{
		Name:   "system",
		Fields: map[string]interface{}{"load1": 0.5},
	}}
	got := Rename(in)[0]
	if got.Name != "load" {
		t.Errorf("system -> load, got %q", got.Name)
	}
}

func TestRename_KernelFields(t *testing.T) {
	in := []Metric{{
		Name: "kernel",
		Fields: map[string]interface{}{
			"boot_time":        uint64(1700000000),
			"processes_forked": uint64(99),
			"context_switches": uint64(1000), // 不在 rename 表中，保持原样
		},
	}}
	got := Rename(in)[0]
	if got.Name != "env" {
		t.Errorf("kernel -> env, got %q", got.Name)
	}
	if _, ok := got.Fields["uptime"]; !ok {
		t.Error("boot_time should map to uptime")
	}
	if _, ok := got.Fields["procs"]; !ok {
		t.Error("processes_forked should map to procs")
	}
	if _, ok := got.Fields["context_switches"]; !ok {
		t.Error("context_switches should be preserved")
	}
}

func TestRename_NetstatTcpFields(t *testing.T) {
	in := []Metric{{
		Name: "netstat",
		Fields: map[string]interface{}{
			"tcp_established": int64(100),
			"tcp_time_wait":   int64(50),
			"tcp_close_wait":  int64(10),
		},
	}}
	got := Rename(in)[0]
	if _, ok := got.Fields["cur_tcp_estab"]; !ok {
		t.Error("tcp_established should map to cur_tcp_estab")
	}
	if _, ok := got.Fields["cur_tcp_timewait"]; !ok {
		t.Error("tcp_time_wait should map to cur_tcp_timewait")
	}
	if _, ok := got.Fields["cur_tcp_closewait"]; !ok {
		t.Error("tcp_close_wait should map to cur_tcp_closewait")
	}
}

func TestRename_PreservesInputSlice(t *testing.T) {
	// Rename 不得改 input
	in := []Metric{{
		Name:   "cpu",
		Fields: map[string]interface{}{"usage_user": 1.0},
	}}
	Rename(in)
	if in[0].Name != "cpu" {
		t.Error("input measurement must not be mutated")
	}
	if _, ok := in[0].Fields["usage_user"]; !ok {
		t.Error("input fields must not be mutated")
	}
}

func TestRenameWindowsFields(t *testing.T) {
	in := []Metric{{
		Name: "cpu",
		Fields: map[string]interface{}{
			"Percent_User_Time":       5.0,
			"Percent_Privileged_Time": 2.0,
			"Percent_Idle_Time":       93.0,
		},
	}}
	got := RenameWindowsFields(in)[0]
	// measurement 不变，仍然 cpu（等下一步 Rename 处理）
	if got.Name != "cpu" {
		t.Errorf("measurement should remain cpu at this stage, got %q", got.Name)
	}
	for _, want := range []string{"user", "system", "idle"} {
		if _, ok := got.Fields[want]; !ok {
			t.Errorf("expected win->unified field %q missing, fields=%v", want, got.Fields)
		}
	}
}
