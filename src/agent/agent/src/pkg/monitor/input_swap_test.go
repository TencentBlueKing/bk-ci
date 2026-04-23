//go:build !loong64
// +build !loong64

package monitor

import (
	"errors"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v3/mem"
)

func TestSwap_Name(t *testing.T) {
	if n := NewSwap().Name(); n != MeasurementSwap {
		t.Errorf("Name() = %q", n)
	}
}

func TestSwap_Gather_Success(t *testing.T) {
	s := &Swap{
		swapMemFn: func() (*mem.SwapMemoryStat, error) {
			return &mem.SwapMemoryStat{Total: 2048, Used: 1024, Free: 1024, UsedPercent: 50, Sin: 10, Sout: 20}, nil
		},
		nowFn: time.Now,
	}
	metrics, err := s.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if len(metrics) != 1 {
		t.Fatal("want 1 metric")
	}
	for _, f := range []string{
		FieldTotal, FieldUsed, FieldFree,
		RenamedFieldPctUsed, FieldSwapIn, FieldSwapOut,
	} {
		if _, ok := metrics[0].Fields[f]; !ok {
			t.Errorf("missing %q", f)
		}
	}
}

func TestSwap_Gather_Error(t *testing.T) {
	sentinel := errors.New("boom")
	s := &Swap{swapMemFn: func() (*mem.SwapMemoryStat, error) { return nil, sentinel }, nowFn: time.Now}
	if _, err := s.Gather(); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v", err)
	}
}

func TestSwap_Gather_Nil(t *testing.T) {
	s := &Swap{swapMemFn: func() (*mem.SwapMemoryStat, error) { return nil, nil }, nowFn: time.Now}
	if _, err := s.Gather(); err == nil {
		t.Error("nil stat should error")
	}
}
