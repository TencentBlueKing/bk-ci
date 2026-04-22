//go:build !loong64
// +build !loong64

package monitor

import (
	"bufio"
	"encoding/json"
	"os"
	"path/filepath"
	"testing"
	"time"
)

// newDebugDir 创建临时 workDir 并按需 touch 一个 .debug 文件。
func newDebugDir(t *testing.T, withDebug bool) string {
	t.Helper()
	dir := t.TempDir()
	if err := os.MkdirAll(filepath.Join(dir, "logs"), 0o700); err != nil {
		t.Fatal(err)
	}
	if withDebug {
		if err := os.WriteFile(filepath.Join(dir, ".debug"), nil, 0o600); err != nil {
			t.Fatal(err)
		}
	}
	return dir
}

func TestDumper_NoDebugFile_SkipsWrite(t *testing.T) {
	dir := newDebugDir(t, false)
	d := NewMonitorDumper(dir)
	d.Dump([]Metric{{Name: "cpu", Fields: map[string]interface{}{"user": 1.0}, Timestamp: fixedNow}})
	if _, err := os.Stat(filepath.Join(dir, "logs", "monitor_metrics.log")); !os.IsNotExist(err) {
		t.Error("dump file should not be created when .debug absent")
	}
}

func TestDumper_WithDebugFile_WritesJSONLine(t *testing.T) {
	dir := newDebugDir(t, true)
	d := NewMonitorDumper(dir)
	d.Dump([]Metric{{
		Name:      MeasurementCPU,
		Tags:      map[string]string{TagCPU: "cpu0"},
		Fields:    map[string]interface{}{RenamedFieldUser: 1.5},
		Timestamp: fixedNow,
	}})
	if err := d.Close(); err != nil {
		t.Fatalf("Close: %v", err)
	}

	logPath := filepath.Join(dir, "logs", "monitor_metrics.log")
	f, err := os.Open(logPath)
	if err != nil {
		t.Fatalf("open: %v", err)
	}
	defer f.Close()

	sc := bufio.NewScanner(f)
	if !sc.Scan() {
		t.Fatal("no line in dump file")
	}
	var line dumpLineJSON
	if err := json.Unmarshal(sc.Bytes(), &line); err != nil {
		t.Fatalf("unmarshal: %v, raw=%s", err, sc.Text())
	}
	if line.Source != TagSourceMonitor {
		t.Errorf("source = %q, want %q", line.Source, TagSourceMonitor)
	}
	if line.Name != MeasurementCPU {
		t.Errorf("name = %q", line.Name)
	}
	if line.Tags[TagCPU] != "cpu0" {
		t.Errorf("tag cpu missing: %v", line.Tags)
	}
	if v, _ := line.Fields[RenamedFieldUser].(float64); v != 1.5 {
		t.Errorf("field user = %v", line.Fields[RenamedFieldUser])
	}
	if line.Timestamp == "" {
		t.Error("timestamp empty")
	}
}

func TestDumper_MultipleLinesAppended(t *testing.T) {
	dir := newDebugDir(t, true)
	d := NewMonitorDumper(dir)
	for i := 0; i < 3; i++ {
		d.Dump([]Metric{{Name: "m", Fields: map[string]interface{}{"x": float64(i)}, Timestamp: fixedNow}})
	}
	_ = d.Close()

	data, _ := os.ReadFile(filepath.Join(dir, "logs", "monitor_metrics.log"))
	lines := 0
	for _, b := range data {
		if b == '\n' {
			lines++
		}
	}
	if lines != 3 {
		t.Errorf("want 3 lines, got %d, data=%q", lines, string(data))
	}
}

func TestDumper_DisabledAfterRemoval(t *testing.T) {
	dir := newDebugDir(t, true)
	d := NewMonitorDumper(dir)

	// 先写入一条
	d.Dump([]Metric{{Name: "x", Fields: map[string]interface{}{"a": 1.0}, Timestamp: fixedNow}})
	if !d.Enabled() {
		t.Fatal("should be enabled")
	}

	// 删除 .debug，等待缓存过期（1s），再次检查应关闭
	if err := os.Remove(filepath.Join(dir, ".debug")); err != nil {
		t.Fatal(err)
	}
	time.Sleep(1100 * time.Millisecond)
	if d.Enabled() {
		t.Error("should be disabled after .debug removed")
	}
}

func TestDumper_EmptyMetricsNoOp(t *testing.T) {
	dir := newDebugDir(t, true)
	d := NewMonitorDumper(dir)
	d.Dump(nil)
	d.Dump([]Metric{})
	// 文件不应被创建
	if _, err := os.Stat(filepath.Join(dir, "logs", "monitor_metrics.log")); !os.IsNotExist(err) {
		t.Error("empty Dump must not create file")
	}
}

func TestDumper_ZeroTimestampFallback(t *testing.T) {
	dir := newDebugDir(t, true)
	d := NewMonitorDumper(dir)
	d.Dump([]Metric{{Name: "m", Fields: map[string]interface{}{"a": 1.0}}}) // no timestamp
	_ = d.Close()
	data, _ := os.ReadFile(filepath.Join(dir, "logs", "monitor_metrics.log"))
	var line dumpLineJSON
	if err := json.Unmarshal(data, &line); err != nil {
		t.Fatal(err)
	}
	if line.Timestamp == "" {
		t.Error("zero ts should fall back to now, got empty")
	}
}

func TestCollectorDumper_UsesCollectorSource(t *testing.T) {
	dir := newDebugDir(t, true)
	d := NewCollectorDumper(dir)
	d.Dump([]Metric{{Name: "m", Fields: map[string]interface{}{"a": 1.0}, Timestamp: fixedNow}})
	_ = d.Close()
	data, _ := os.ReadFile(filepath.Join(dir, "logs", "collector_metrics.log"))
	var line dumpLineJSON
	if err := json.Unmarshal(data, &line); err != nil {
		t.Fatal(err)
	}
	if line.Source != "collector" {
		t.Errorf("source = %q, want collector", line.Source)
	}
}
