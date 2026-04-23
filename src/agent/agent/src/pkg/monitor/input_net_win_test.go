//go:build windows
// +build windows

package monitor

import (
	"errors"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v3/net"
)

func TestNet_Name_WindowsOut(t *testing.T) {
	if n := NewNet().Name(); n != MeasurementNet {
		t.Errorf("Name() = %q, want %q", n, MeasurementNet)
	}
}

// TestNet_TwinSample_BasicRate 覆盖 plan 6.1 同名条目：速率字段按 (c2-c1)/dt
// 计算，err/drop 取第二次累计值原样输出。
func TestNet_TwinSample_BasicRate(t *testing.T) {
	clk := newFakeClock()
	calls := 0
	n := &Net{
		ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) {
			if !pernic {
				t.Errorf("IOCounters called with pernic=false")
			}
			calls++
			if calls == 1 {
				return []net.IOCountersStat{{
					Name: "Ethernet", BytesRecv: 1000, BytesSent: 2000,
					PacketsRecv: 10, PacketsSent: 20,
					Errin: 1, Errout: 2, Dropin: 3, Dropout: 4,
				}}, nil
			}
			return []net.IOCountersStat{{
				Name: "Ethernet", BytesRecv: 1300, BytesSent: 2500,
				PacketsRecv: 15, PacketsSent: 28,
				Errin: 11, Errout: 12, Dropin: 13, Dropout: 14,
			}}, nil
		},
		nowFn:   clk.Now,
		sleepFn: clk.Sleep,
	}

	metrics, err := n.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if len(metrics) != 1 {
		t.Fatalf("want 1 metric, got %d", len(metrics))
	}
	m := metrics[0]
	if m.Name != MeasurementNet {
		t.Errorf("measurement = %q, want %q", m.Name, MeasurementNet)
	}
	if m.Tags[TagInterface] != "Ethernet" {
		t.Errorf("interface tag = %q", m.Tags[TagInterface])
	}

	wantRates := map[string]float64{
		RenamedFieldSpeedRecv:        300,
		RenamedFieldSpeedSent:        500,
		RenamedFieldSpeedPacketsRecv: 5,
		RenamedFieldSpeedPacketsSent: 8,
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
	// err/drop 原始累计值来自第二次采样
	wantCumul := map[string]uint64{
		FieldErrIn: 11, FieldErrOut: 12, FieldDropIn: 13, FieldDropOut: 14,
	}
	for f, want := range wantCumul {
		if got, _ := m.Fields[f].(uint64); got != want {
			t.Errorf("field %s = %v, want %v (second sample)", f, m.Fields[f], want)
		}
	}
	// PDH 兼容别名：同值双写（对齐 telegraf win_perf_counters 字段名）
	wantAlias := map[string]uint64{
		WinFieldPacketsReceivedErrors:    11,
		WinFieldPacketsOutboundErrors:    12,
		WinFieldPacketsReceivedDiscarded: 13,
		WinFieldPacketsOutboundDiscarded: 14,
	}
	for f, want := range wantAlias {
		if got, _ := m.Fields[f].(uint64); got != want {
			t.Errorf("alias field %s = %v, want %v", f, m.Fields[f], want)
		}
	}
}

// TestNet_TwinSample_CounterReset 与 diskio 对应：第二次累计小 → 速率字段缺席。
func TestNet_TwinSample_CounterReset(t *testing.T) {
	clk := newFakeClock()
	calls := 0
	n := &Net{
		ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) {
			calls++
			if calls == 1 {
				return []net.IOCountersStat{{
					Name: "Ethernet", BytesRecv: 9000, BytesSent: 9000, PacketsRecv: 900, PacketsSent: 900,
				}}, nil
			}
			return []net.IOCountersStat{{
				Name: "Ethernet", BytesRecv: 10, BytesSent: 20, PacketsRecv: 1, PacketsSent: 2,
				Errin: 5,
			}}, nil
		},
		nowFn:   clk.Now,
		sleepFn: clk.Sleep,
	}

	metrics, _ := n.Gather()
	if len(metrics) != 1 {
		t.Fatalf("want 1 metric, got %d", len(metrics))
	}
	m := metrics[0]
	for _, f := range []string{
		RenamedFieldSpeedRecv, RenamedFieldSpeedSent,
		RenamedFieldSpeedPacketsRecv, RenamedFieldSpeedPacketsSent,
	} {
		if _, ok := m.Fields[f]; ok {
			t.Errorf("field %s should be absent on reset", f)
		}
	}
	if v, _ := m.Fields[FieldErrIn].(uint64); v != 5 {
		t.Errorf("err_in = %v, want 5 (second sample)", m.Fields[FieldErrIn])
	}
}

// TestNet_TwinSample_NewInterfaceOnlyInSecond 热插拔网卡：第一次无、第二次有
// → 输出 Metric 但无速率字段，累计字段照常输出。
func TestNet_TwinSample_NewInterfaceOnlyInSecond(t *testing.T) {
	clk := newFakeClock()
	calls := 0
	n := &Net{
		ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) {
			calls++
			if calls == 1 {
				return []net.IOCountersStat{
					{Name: "Ethernet", BytesRecv: 100, BytesSent: 200},
				}, nil
			}
			return []net.IOCountersStat{
				{Name: "Ethernet", BytesRecv: 150, BytesSent: 250},
				{Name: "Wi-Fi", BytesRecv: 500, BytesSent: 600, Errin: 9},
			}, nil
		},
		nowFn:   clk.Now,
		sleepFn: clk.Sleep,
	}

	metrics, _ := n.Gather()
	if len(metrics) != 2 {
		t.Fatalf("want 2 metrics, got %d", len(metrics))
	}
	var wifi *Metric
	for i, m := range metrics {
		if m.Tags[TagInterface] == "Wi-Fi" {
			wifi = &metrics[i]
		}
	}
	if wifi == nil {
		t.Fatal("Wi-Fi interface not emitted")
	}
	for _, f := range []string{
		RenamedFieldSpeedRecv, RenamedFieldSpeedSent,
		RenamedFieldSpeedPacketsRecv, RenamedFieldSpeedPacketsSent,
	} {
		if _, ok := wifi.Fields[f]; ok {
			t.Errorf("new interface should have no rate field %s", f)
		}
	}
	if v, _ := wifi.Fields[FieldErrIn].(uint64); v != 9 {
		t.Errorf("err_in = %v, want 9", wifi.Fields[FieldErrIn])
	}
}

// TestNet_TwinSample_FiltersAllPseudoInterface 验证 "all" 汇总伪接口被过滤，
// 与 input_net.go 非 out/windows 分支保持一致（gopsutil 在某些平台会返回）。
func TestNet_TwinSample_FiltersAllPseudoInterface(t *testing.T) {
	clk := newFakeClock()
	calls := 0
	n := &Net{
		ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) {
			calls++
			base := []net.IOCountersStat{
				{Name: "Ethernet", BytesRecv: 100},
				{Name: "all", BytesRecv: 999},
			}
			if calls == 2 {
				base[0].BytesRecv = 200
				base[1].BytesRecv = 9999
			}
			return base, nil
		},
		nowFn:   clk.Now,
		sleepFn: clk.Sleep,
	}
	metrics, _ := n.Gather()
	if len(metrics) != 1 {
		t.Fatalf("want 1 metric (all filtered), got %d", len(metrics))
	}
	if metrics[0].Tags[TagInterface] == "all" {
		t.Error("'all' pseudo-interface should be filtered")
	}
}

// TestNet_TwinSample_NoSleepInTest 配合 diskio 的同名测试：fake sleepFn 必须
// 避免 Gather 真实阻塞 1s。
func TestNet_TwinSample_NoSleepInTest(t *testing.T) {
	clk := newFakeClock()
	n := &Net{
		ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) {
			return []net.IOCountersStat{{Name: "Ethernet"}}, nil
		},
		nowFn:   clk.Now,
		sleepFn: clk.Sleep,
	}
	start := time.Now()
	_, _ = n.Gather()
	if elapsed := time.Since(start); elapsed > 200*time.Millisecond {
		t.Errorf("Gather took %v; fake sleepFn should avoid real 1s sleep", elapsed)
	}
}

func TestNet_TwinSample_FirstSampleErrPropagates(t *testing.T) {
	sentinel := errors.New("boom-first")
	n := &Net{
		ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) { return nil, sentinel },
		nowFn:        time.Now,
		sleepFn:      func(time.Duration) {},
	}
	if _, err := n.Gather(); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v, want wrap of sentinel", err)
	}
}

func TestNet_TwinSample_SecondSampleErrPropagates(t *testing.T) {
	sentinel := errors.New("boom-second")
	calls := 0
	n := &Net{
		ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) {
			calls++
			if calls == 1 {
				return []net.IOCountersStat{{Name: "Ethernet"}}, nil
			}
			return nil, sentinel
		},
		nowFn:   time.Now,
		sleepFn: func(time.Duration) {},
	}
	if _, err := n.Gather(); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v, want wrap of sentinel", err)
	}
}

func TestNet_TwinSample_NonPositiveDt(t *testing.T) {
	frozen := time.Unix(1_700_000_000, 0)
	n := &Net{
		ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) {
			return []net.IOCountersStat{{Name: "Ethernet"}}, nil
		},
		nowFn:   func() time.Time { return frozen },
		sleepFn: func(time.Duration) {},
	}
	if _, err := n.Gather(); err == nil {
		t.Error("want error on non-positive dt, got nil")
	}
}
