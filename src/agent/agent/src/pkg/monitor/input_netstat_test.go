//go:build !loong64
// +build !loong64

package monitor

import (
	"errors"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v4/net"
)

func TestNetstat_Name(t *testing.T) {
	if n := NewNetstat().Name(); n != MeasurementNetstat {
		t.Errorf("Name() = %q", n)
	}
}

func TestNetstat_Gather_CountsByState(t *testing.T) {
	n := &Netstat{
		connsFn: func(kind string) ([]net.ConnectionStat, error) {
			return []net.ConnectionStat{
				{Type: 1, Status: "ESTABLISHED"},
				{Type: 1, Status: "ESTABLISHED"},
				{Type: 1, Status: "TIME_WAIT"},
				{Type: 1, Status: "LISTEN"},
				{Type: 1, Status: "CLOSE_WAIT"},
				{Type: 2}, // UDP
				{Type: 2},
			}, nil
		},
		nowFn: time.Now,
	}
	metrics, err := n.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if len(metrics) != 1 {
		t.Fatalf("want 1 metric, got %d", len(metrics))
	}
	f := metrics[0].Fields
	if v, _ := f[RenamedFieldCurTCPEstab].(int64); v != 2 {
		t.Errorf("established = %v, want 2", f[RenamedFieldCurTCPEstab])
	}
	if v, _ := f[RenamedFieldCurTCPTimeWait].(int64); v != 1 {
		t.Errorf("time_wait = %v", f[RenamedFieldCurTCPTimeWait])
	}
	if v, _ := f[RenamedFieldCurTCPListen].(int64); v != 1 {
		t.Errorf("listen = %v", f[RenamedFieldCurTCPListen])
	}
	if v, _ := f[RenamedFieldCurTCPCloseWait].(int64); v != 1 {
		t.Errorf("close_wait = %v", f[RenamedFieldCurTCPCloseWait])
	}
	if v, _ := f[FieldUDPSocket].(int64); v != 2 {
		t.Errorf("udp = %v", f[FieldUDPSocket])
	}
}

func TestNetstat_Gather_ErrorPropagates(t *testing.T) {
	sentinel := errors.New("boom")
	n := &Netstat{connsFn: func(kind string) ([]net.ConnectionStat, error) { return nil, sentinel }, nowFn: time.Now}
	if _, err := n.Gather(); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v", err)
	}
}

func TestNetstat_Gather_AllStateFieldsPresent(t *testing.T) {
	// 即使没有该状态的连接，field 也应存在（值为 0），便于后端时序图初始化
	n := &Netstat{
		connsFn: func(kind string) ([]net.ConnectionStat, error) { return nil, nil },
		nowFn:   time.Now,
	}
	metrics, _ := n.Gather()
	required := []string{
		RenamedFieldCurTCPEstab, RenamedFieldCurTCPSynSent, RenamedFieldCurTCPSynRecv,
		RenamedFieldCurTCPFinWait1, RenamedFieldCurTCPFinWait2, RenamedFieldCurTCPTimeWait,
		RenamedFieldCurTCPClosed, RenamedFieldCurTCPCloseWait, RenamedFieldCurTCPLastAck,
		RenamedFieldCurTCPListen, RenamedFieldCurTCPClosing, FieldTCPNone,
		FieldUDPSocket,
	}
	for _, f := range required {
		if _, ok := metrics[0].Fields[f]; !ok {
			t.Errorf("missing %q", f)
		}
	}
}
