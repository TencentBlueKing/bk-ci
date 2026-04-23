//go:build !loong64 && !windows
// +build !loong64,!windows

package monitor

import (
	"errors"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v3/net"
)

func TestNet_Name(t *testing.T) {
	if n := NewNet().Name(); n != MeasurementNet {
		t.Errorf("Name() = %q", n)
	}
}

func TestNet_Gather_FiltersAllPseudoInterface(t *testing.T) {
	// 同时验证：gopsutil 伪接口 "all" 被过滤 + 虚接口黑名单（lo/docker0）被过滤
	n := &Net{
		ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) {
			return []net.IOCountersStat{
				{Name: "eth0", BytesRecv: 100, BytesSent: 200},
				{Name: "all", BytesRecv: 300, BytesSent: 400},
				{Name: "lo", BytesRecv: 1, BytesSent: 2},       // 虚接口黑名单
				{Name: "docker0", BytesRecv: 10, BytesSent: 20}, // 虚接口黑名单
				{Name: "ens33", BytesRecv: 5, BytesSent: 6},    // 物理网卡保留
			}, nil
		},
		nowFn: time.Now,
	}
	metrics, err := n.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if len(metrics) != 2 {
		t.Fatalf("want 2 (eth0 + ens33), got %d", len(metrics))
	}
	seen := map[string]bool{}
	for _, m := range metrics {
		seen[m.Tags[TagInterface]] = true
		if m.Tags[TagInterface] == "all" {
			t.Error("'all' should be filtered")
		}
		if m.Tags[TagInterface] == "lo" || m.Tags[TagInterface] == "docker0" {
			t.Errorf("virtual interface %q should be filtered", m.Tags[TagInterface])
		}
	}
	if !seen["eth0"] || !seen["ens33"] {
		t.Errorf("physical interfaces missing: %+v", seen)
	}
}

func TestNet_Gather_FieldsPresent(t *testing.T) {
	n := &Net{
		ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) {
			return []net.IOCountersStat{{
				Name: "eth0", BytesRecv: 100, BytesSent: 200,
				PacketsRecv: 10, PacketsSent: 20,
				Errin: 1, Errout: 2, Dropin: 3, Dropout: 4,
			}}, nil
		},
		nowFn: time.Now,
	}
	metrics, _ := n.Gather()
	required := []string{
		RenamedFieldSpeedRecv, RenamedFieldSpeedSent,
		RenamedFieldSpeedPacketsRecv, RenamedFieldSpeedPacketsSent,
		FieldErrIn, FieldErrOut,
		FieldDropIn, FieldDropOut,
	}
	for _, f := range required {
		if _, ok := metrics[0].Fields[f]; !ok {
			t.Errorf("missing %q", f)
		}
	}
}

func TestNet_Gather_ErrorPropagates(t *testing.T) {
	sentinel := errors.New("boom")
	n := &Net{ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) { return nil, sentinel }, nowFn: time.Now}
	if _, err := n.Gather(); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v", err)
	}
}
