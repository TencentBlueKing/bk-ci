//go:build linux && !loong64
// +build linux,!loong64

package monitor

import (
	"os"
	"path/filepath"
	"testing"
)

// writeTempFile 把内容写到临时文件并返回路径。t.Cleanup 负责移除。
func writeTempFile(t *testing.T, name, content string) string {
	t.Helper()
	dir := t.TempDir()
	p := filepath.Join(dir, name)
	if err := os.WriteFile(p, []byte(content), 0o644); err != nil {
		t.Fatalf("write %s: %v", p, err)
	}
	return p
}

// withProcPaths 在测试期间替换 procStatPath / procEntropyAvailPath，返回恢复函数。
func withProcPaths(t *testing.T, stat, entropy string) func() {
	t.Helper()
	origStat, origEntropy := procStatPath, procEntropyAvailPath
	procStatPath = stat
	procEntropyAvailPath = entropy
	return func() {
		procStatPath = origStat
		procEntropyAvailPath = origEntropy
	}
}

const sampleProcStat = `cpu  1 2 3 4 5 6 7 8 9 10
cpu0 1 2 3 4 5 6 7 8 9 10
intr 12345 6 7 8 9 10 11 12 13
ctxt 67890
btime 1700000000
processes 111
procs_running 2
procs_blocked 0
softirq 777 1 2 3
page 5 6
`

// TestReadProcStatAndEntropy_Happy 覆盖 telegraf kernel_linux.go 的全部字段
// （intr / ctxt / btime / processes / page / entropy_avail）解析路径。
func TestReadProcStatAndEntropy_Happy(t *testing.T) {
	statPath := writeTempFile(t, "stat", sampleProcStat)
	entropyPath := writeTempFile(t, "entropy", "256\n")
	defer withProcPaths(t, statPath, entropyPath)()

	fields, err := readProcStatAndEntropy()
	if err != nil {
		t.Fatalf("err = %v", err)
	}
	want := map[string]int64{
		FieldInterrupts:      12345,
		FieldContextSwitches: 67890,
		FieldBootTime:        1700000000,
		RenamedFieldProcs:    111,
		FieldDiskPagesIn:     5,
		FieldDiskPagesOut:    6,
		FieldEntropyAvail:    256,
	}
	for k, v := range want {
		got, ok := fields[k].(int64)
		if !ok {
			t.Errorf("field %s: want int64, got %T", k, fields[k])
			continue
		}
		if got != v {
			t.Errorf("field %s = %d, want %d", k, got, v)
		}
	}
}

// TestReadProcStatAndEntropy_MissingStat 覆盖 /proc/stat 读失败 → 整体报错。
func TestReadProcStatAndEntropy_MissingStat(t *testing.T) {
	defer withProcPaths(t, "/no/such/stat", "/no/such/entropy")()
	if _, err := readProcStatAndEntropy(); err == nil {
		t.Error("want error when /proc/stat missing")
	}
}

// TestReadProcStatAndEntropy_MissingEntropy 覆盖 entropy_avail 读失败 → 整体报错。
func TestReadProcStatAndEntropy_MissingEntropy(t *testing.T) {
	statPath := writeTempFile(t, "stat", sampleProcStat)
	defer withProcPaths(t, statPath, "/no/such/entropy")()
	if _, err := readProcStatAndEntropy(); err == nil {
		t.Error("want error when entropy_avail missing")
	}
}

// TestReadProcStatAndEntropy_ParseEntropy 覆盖 entropy_avail 非数字内容。
func TestReadProcStatAndEntropy_ParseEntropy(t *testing.T) {
	statPath := writeTempFile(t, "stat", sampleProcStat)
	entropyPath := writeTempFile(t, "entropy", "NaN")
	defer withProcPaths(t, statPath, entropyPath)()
	if _, err := readProcStatAndEntropy(); err == nil {
		t.Error("want error on non-numeric entropy_avail")
	}
}

// TestReadProcStatAndEntropy_TolerateMissingPrefixes 覆盖 /proc/stat 中某些
// 前缀缺失时不应整体报错——只是该字段不出现（兼容不同内核版本）。
func TestReadProcStatAndEntropy_TolerateMissingPrefixes(t *testing.T) {
	// 只保留 intr 和 btime
	minimal := "cpu 1 2 3\nintr 999\nbtime 2000000000\n"
	statPath := writeTempFile(t, "stat", minimal)
	entropyPath := writeTempFile(t, "entropy", "42")
	defer withProcPaths(t, statPath, entropyPath)()

	fields, err := readProcStatAndEntropy()
	if err != nil {
		t.Fatalf("err = %v", err)
	}
	if v, _ := fields[FieldInterrupts].(int64); v != 999 {
		t.Errorf("interrupts = %v", fields[FieldInterrupts])
	}
	if v, _ := fields[FieldBootTime].(int64); v != 2000000000 {
		t.Errorf("boot_time = %v", fields[FieldBootTime])
	}
	// 缺失前缀不输出字段
	for _, k := range []string{FieldContextSwitches, RenamedFieldProcs, FieldDiskPagesIn, FieldDiskPagesOut} {
		if _, ok := fields[k]; ok {
			t.Errorf("field %q should be absent when prefix missing", k)
		}
	}
	// entropy 仍在
	if v, _ := fields[FieldEntropyAvail].(int64); v != 42 {
		t.Errorf("entropy_avail = %v", fields[FieldEntropyAvail])
	}
}
