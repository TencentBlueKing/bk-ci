//go:build !loong64
// +build !loong64

package monitor

import (
	"errors"
	"testing"
	"time"
)

func TestKernel_Name(t *testing.T) {
	if n := NewKernel().Name(); n != RenamedEnv {
		t.Errorf("Name() = %q", n)
	}
}

func TestKernel_Gather_Success(t *testing.T) {
	k := &Kernel{
		bootTimeFn: func() (uint64, error) { return 1700000000, nil },
		nowFn:      time.Now,
	}
	metrics, err := k.Gather()
	if err != nil {
		t.Fatal(err)
	}
	if metrics[0].Name != RenamedEnv {
		t.Errorf("measurement = %q, want env", metrics[0].Name)
	}
	if v, _ := metrics[0].Fields[FieldUptime].(uint64); v != 1700000000 {
		t.Errorf("uptime = %v", metrics[0].Fields[FieldUptime])
	}
}

func TestKernel_Gather_Error(t *testing.T) {
	sentinel := errors.New("boom")
	k := &Kernel{bootTimeFn: func() (uint64, error) { return 0, sentinel }, nowFn: time.Now}
	if _, err := k.Gather(); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v", err)
	}
}
