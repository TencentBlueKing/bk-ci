//go:build windows
// +build windows

package monitor

import (
	"errors"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v4/disk"
)

// fakeClock 通过 nowFn/sleepFn 成对注入，保证 Gather 内两次 now 间隔正好是
// sleepFn 推进的时间，避免真实 time.Sleep(1s) 拖慢单测。
type fakeClock struct {
	now time.Time
}

func (c *fakeClock) Now() time.Time { return c.now }

func (c *fakeClock) Sleep(d time.Duration) { c.now = c.now.Add(d) }

func newFakeClock() *fakeClock { return &fakeClock{now: time.Unix(1_700_000_000, 0)} }

func TestDiskIO_Name_WindowsOut(t *testing.T) {
	if n := NewDiskIO().Name(); n != RenamedIO {
		t.Errorf("Name() = %q, want %q", n, RenamedIO)
	}
}

// TestDiskIO_TwinSample_BasicRate 覆盖 plan 6.1 同名条目：两次累计差 100、
// dt=1s → rkb_s=100；读取 ReadTime 等累计字段时以第二次采样为准。
func TestDiskIO_TwinSample_BasicRate(t *testing.T) {
	clk := newFakeClock()
	calls := 0
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			calls++
			if calls == 1 {
				return map[string]disk.IOCountersStat{
					"0 C:": {
						Name: "0 C:", ReadBytes: 1000, WriteBytes: 2000,
						ReadCount: 10, WriteCount: 20, ReadTime: 111, WriteTime: 222,
					},
				}, nil
			}
			return map[string]disk.IOCountersStat{
				"0 C:": {
					Name: "0 C:", ReadBytes: 1100, WriteBytes: 2300,
					ReadCount: 15, WriteCount: 26, ReadTime: 333, WriteTime: 444,
					IoTime: 555, WeightedIO: 666, IopsInProgress: 7,
					MergedReadCount: 8, MergedWriteCount: 9,
				},
			}, nil
		},
		nowFn:   clk.Now,
		sleepFn: clk.Sleep,
	}

	metrics, err := d.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if len(metrics) != 1 {
		t.Fatalf("want 1 metric, got %d", len(metrics))
	}
	m := metrics[0]
	if m.Name != RenamedIO {
		t.Errorf("measurement = %q, want %q", m.Name, RenamedIO)
	}
	if m.Tags[TagName] != "0 C:" {
		t.Errorf("tag name = %q", m.Tags[TagName])
	}

	// 速率字段：(c2-c1)/dt，dt=1s
	wantRates := map[string]float64{
		RenamedFieldRkbS: 100,
		RenamedFieldWkbS: 300,
		FieldReads:       5,
		FieldWrites:      6,
	}
	for f, want := range wantRates {
		got, ok := m.Fields[f].(float64)
		if !ok {
			t.Errorf("field %s: want float64, got %T", f, m.Fields[f])
			continue
		}
		if got != want {
			t.Errorf("field %s = %v, want %v", f, got, want)
		}
	}
	// 累计字段：以第二次采样为准
	if v, _ := m.Fields[FieldReadTime].(uint64); v != 333 {
		t.Errorf("read_time = %v, want 333 (second sample)", m.Fields[FieldReadTime])
	}
	if v, _ := m.Fields[FieldWriteTime].(uint64); v != 444 {
		t.Errorf("write_time = %v, want 444", m.Fields[FieldWriteTime])
	}
	if v, _ := m.Fields[FieldIOTime].(uint64); v != 555 {
		t.Errorf("io_time = %v, want 555", m.Fields[FieldIOTime])
	}
	if v, _ := m.Fields[FieldWeightedIOTime].(uint64); v != 666 {
		t.Errorf("weighted_io_time = %v", m.Fields[FieldWeightedIOTime])
	}
	// timestamp 对齐第二次采样时间点
	if !m.Timestamp.Equal(clk.now) {
		t.Errorf("timestamp = %v, want second-sample time %v", m.Timestamp, clk.now)
	}
}

// TestDiskIO_TwinSample_CounterReset 覆盖 plan 6.1：第二次累计 < 第一次 →
// 该速率字段缺席（delete），但累计字段仍输出。
func TestDiskIO_TwinSample_CounterReset(t *testing.T) {
	clk := newFakeClock()
	calls := 0
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			calls++
			if calls == 1 {
				return map[string]disk.IOCountersStat{
					"0 C:": {Name: "0 C:", ReadBytes: 9999, WriteBytes: 9999, ReadCount: 99, WriteCount: 99},
				}, nil
			}
			// 第二次所有累计都小于第一次 → 视作 counter reset
			return map[string]disk.IOCountersStat{
				"0 C:": {Name: "0 C:", ReadBytes: 10, WriteBytes: 20, ReadCount: 1, WriteCount: 2, ReadTime: 7},
			}, nil
		},
		nowFn:   clk.Now,
		sleepFn: clk.Sleep,
	}

	metrics, err := d.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if len(metrics) != 1 {
		t.Fatalf("want 1 metric, got %d", len(metrics))
	}
	m := metrics[0]
	for _, f := range []string{RenamedFieldRkbS, RenamedFieldWkbS, FieldReads, FieldWrites} {
		if _, ok := m.Fields[f]; ok {
			t.Errorf("field %s should be absent on reset, got %v", f, m.Fields[f])
		}
	}
	// 累计字段仍输出
	if v, _ := m.Fields[FieldReadTime].(uint64); v != 7 {
		t.Errorf("read_time absent/wrong on reset: %v", m.Fields[FieldReadTime])
	}
}

// TestDiskIO_TwinSample_NewDeviceOnlyInSecond 覆盖 plan 6.1：热插拔场景，
// 第一次无该盘、第二次有 → 输出 Metric 但无速率字段。
func TestDiskIO_TwinSample_NewDeviceOnlyInSecond(t *testing.T) {
	clk := newFakeClock()
	calls := 0
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			calls++
			if calls == 1 {
				return map[string]disk.IOCountersStat{
					"0 C:": {Name: "0 C:", ReadBytes: 1000, WriteBytes: 2000},
				}, nil
			}
			return map[string]disk.IOCountersStat{
				"0 C:": {Name: "0 C:", ReadBytes: 1050, WriteBytes: 2100},
				"1 D:": {Name: "1 D:", ReadBytes: 500, WriteBytes: 800, ReadTime: 42},
			}, nil
		},
		nowFn:   clk.Now,
		sleepFn: clk.Sleep,
	}

	metrics, err := d.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if len(metrics) != 2 {
		t.Fatalf("want 2 metrics, got %d", len(metrics))
	}
	var newDisk *Metric
	for i, m := range metrics {
		if m.Tags[TagName] == "1 D:" {
			newDisk = &metrics[i]
		}
	}
	if newDisk == nil {
		t.Fatal("new disk '1 D:' not emitted")
	}
	for _, f := range []string{RenamedFieldRkbS, RenamedFieldWkbS, FieldReads, FieldWrites} {
		if _, ok := newDisk.Fields[f]; ok {
			t.Errorf("new disk should have no rate field %s", f)
		}
	}
	// 但累计字段输出
	if v, _ := newDisk.Fields[FieldReadTime].(uint64); v != 42 {
		t.Errorf("new disk read_time = %v, want 42", newDisk.Fields[FieldReadTime])
	}
}

// TestDiskIO_TwinSample_NoSleepInTest 覆盖 plan 6.1：fake sleepFn 不应调用
// 真实 time.Sleep。通过测量真实墙钟耗时验证。
func TestDiskIO_TwinSample_NoSleepInTest(t *testing.T) {
	clk := newFakeClock()
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			return map[string]disk.IOCountersStat{
				"0 C:": {Name: "0 C:", ReadBytes: 1, WriteBytes: 2},
			}, nil
		},
		nowFn:   clk.Now,
		sleepFn: clk.Sleep,
	}
	start := time.Now()
	_, _ = d.Gather()
	// 留足余量；关键是远小于 rateSampleInterval(1s)
	if elapsed := time.Since(start); elapsed > 200*time.Millisecond {
		t.Errorf("Gather took %v; fake sleepFn should avoid real 1s sleep", elapsed)
	}
}

func TestDiskIO_TwinSample_FirstSampleErrPropagates(t *testing.T) {
	sentinel := errors.New("boom-first")
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) { return nil, sentinel },
		nowFn:        time.Now,
		sleepFn:      func(time.Duration) {},
	}
	if _, err := d.Gather(); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v, want wrap of sentinel", err)
	}
}

func TestDiskIO_TwinSample_SecondSampleErrPropagates(t *testing.T) {
	sentinel := errors.New("boom-second")
	calls := 0
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			calls++
			if calls == 1 {
				return map[string]disk.IOCountersStat{"0 C:": {Name: "0 C:"}}, nil
			}
			return nil, sentinel
		},
		nowFn:   time.Now,
		sleepFn: func(time.Duration) {},
	}
	if _, err := d.Gather(); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v, want wrap of sentinel", err)
	}
}

// TestDiskIO_TwinSample_NonPositiveDt 覆盖 dt<=0 防御分支（时钟倒流）。
func TestDiskIO_TwinSample_NonPositiveDt(t *testing.T) {
	frozen := time.Unix(1_700_000_000, 0)
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			return map[string]disk.IOCountersStat{"0 C:": {Name: "0 C:"}}, nil
		},
		nowFn:   func() time.Time { return frozen },
		sleepFn: func(time.Duration) {},
	}
	if _, err := d.Gather(); err == nil {
		t.Error("want error on non-positive dt, got nil")
	}
}

// TestDiskIO_TwinSample_InstanceWithDiskIndex 覆盖 plan P2-2 主路径：
// deviceNumberFn 成功返回盘号 → instance = "<idx> <letter>"。
func TestDiskIO_TwinSample_InstanceWithDiskIndex(t *testing.T) {
	clk := newFakeClock()
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			return map[string]disk.IOCountersStat{
				"C:": {Name: "C:", ReadBytes: 1, WriteBytes: 2},
				"D:": {Name: "D:", ReadBytes: 3, WriteBytes: 4},
			}, nil
		},
		nowFn:   clk.Now,
		sleepFn: clk.Sleep,
		deviceNumberFn: func(letter string) (uint32, error) {
			switch letter {
			case "C:":
				return 0, nil
			case "D:":
				return 1, nil
			}
			return 0, errors.New("unknown")
		},
	}

	metrics, err := d.Gather()
	if err != nil {
		t.Fatal(err)
	}
	want := map[string]string{"C:": "0 C:", "D:": "1 D:"}
	for _, m := range metrics {
		letter := m.Tags[TagName]
		if got, expected := m.Tags[TagInstance], want[letter]; got != expected {
			t.Errorf("letter=%q instance=%q want %q", letter, got, expected)
		}
	}
}

// TestDiskIO_TwinSample_InstanceFallbackOnError 覆盖 deviceNumberFn 失败 →
// fallback instance=letter。不中断 metric 产出。
func TestDiskIO_TwinSample_InstanceFallbackOnError(t *testing.T) {
	clk := newFakeClock()
	d := &DiskIO{
		ioCountersFn: func() (map[string]disk.IOCountersStat, error) {
			return map[string]disk.IOCountersStat{
				"C:": {Name: "C:", ReadBytes: 1, WriteBytes: 2},
			}, nil
		},
		nowFn:   clk.Now,
		sleepFn: clk.Sleep,
		deviceNumberFn: func(letter string) (uint32, error) {
			return 0, errors.New("access denied")
		},
	}
	metrics, _ := d.Gather()
	if len(metrics) != 1 {
		t.Fatalf("want 1 metric, got %d", len(metrics))
	}
	if got := metrics[0].Tags[TagInstance]; got != "C:" {
		t.Errorf("instance on failure = %q, want fallback %q", got, "C:")
	}
	if got := metrics[0].Tags[TagName]; got != "C:" {
		t.Errorf("name should still be preserved, got %q", got)
	}
}
