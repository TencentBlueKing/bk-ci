//go:build !loong64
// +build !loong64

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
	n := &Net{
		ioCountersFn: func(pernic bool) ([]net.IOCountersStat, error) {
			return []net.IOCountersStat{
				{Name: "eth0", BytesRecv: 100, BytesSent: 200},
				{Name: "all", BytesRecv: 300, BytesSent: 400},
				{Name: "lo", BytesRecv: 1, BytesSent: 2},
			}, nil
		},
		nowFn: time.Now,
	}
	metrics, err := n.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if len(metrics) != 2 {
		t.Fatalf("want 2 (all filtered), got %d", len(metrics))
	}
	for _, m := range metrics {
		if m.Tags[TagInterface] == "all" {
			t.Error("'all' should be filtered")
		}
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
		FieldBytesRecv, FieldBytesSent,
		FieldPacketsRecv, FieldPacketsSent,
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
