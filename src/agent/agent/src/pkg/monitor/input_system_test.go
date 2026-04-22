//go:build !loong64
// +build !loong64

package monitor

import (
	"errors"
	"runtime"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v3/host"
	"github.com/shirou/gopsutil/v3/load"
)

func TestSystem_Name(t *testing.T) {
	if n := NewSystem().Name(); n != MeasurementSystem {
		t.Errorf("Name() = %q", n)
	}
}

func TestSystem_Gather_HasNCPUsAndUptime(t *testing.T) {
	s := &System{
		loadAvgFn: func() (*load.AvgStat, error) {
			return &load.AvgStat{Load1: 0.5, Load5: 0.3, Load15: 0.1}, nil
		},
		uptimeFn: func() (uint64, error) { return 3600, nil },
		usersFn:  func() ([]host.UserStat, error) { return []host.UserStat{{}, {}}, nil },
		nowFn:    time.Now,
	}
	metrics, err := s.Gather()
	if err != nil {
		t.Fatal(err)
	}
	f := metrics[0].Fields
	if _, ok := f[FieldNCPUs]; !ok {
		t.Error("n_cpus missing")
	}
	if _, ok := f[FieldUptime]; !ok {
		t.Error("uptime missing")
	}
	if _, ok := f[FieldNUsers]; !ok {
		t.Error("n_users missing")
	}
	if runtime.GOOS != "windows" {
		for _, k := range []string{FieldLoad1, FieldLoad5, FieldLoad15} {
			if _, ok := f[k]; !ok {
				t.Errorf("non-windows must have %q", k)
			}
		}
	}
}

func TestSystem_Gather_AllSubQueriesFail(t *testing.T) {
	e := errors.New("nope")
	s := &System{
		loadAvgFn: func() (*load.AvgStat, error) { return nil, e },
		uptimeFn:  func() (uint64, error) { return 0, e },
		usersFn:   func() ([]host.UserStat, error) { return nil, e },
		nowFn:     time.Now,
	}
	_, err := s.Gather()
	if err == nil {
		t.Error("all-failure should return error")
	}
}

func TestSystem_Gather_PartialFailureStillProduces(t *testing.T) {
	// load 失败但 uptime 成功 → 保留 uptime
	s := &System{
		loadAvgFn: func() (*load.AvgStat, error) { return nil, errors.New("x") },
		uptimeFn:  func() (uint64, error) { return 12345, nil },
		usersFn:   func() ([]host.UserStat, error) { return nil, errors.New("x") },
		nowFn:     time.Now,
	}
	metrics, err := s.Gather()
	if err != nil {
		t.Fatalf("partial failure should succeed: %v", err)
	}
	if v, _ := metrics[0].Fields[FieldUptime].(uint64); v != 12345 {
		t.Errorf("uptime = %v", metrics[0].Fields[FieldUptime])
	}
}
