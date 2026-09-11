//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/shirou/gopsutil/v4/process"
)

// fakeProc 构造一个 stub *process.Process。gopsutil 的 process.Process
// 结构体可直接实例化，但 Status()/NumThreads() 通过 statusFn/threadsFn
// 注入更干净。
func fakeProcs(n int) []*process.Process {
	out := make([]*process.Process, n)
	for i := range out {
		out[i] = &process.Process{Pid: int32(i + 1)}
	}
	return out
}

func TestProcesses_Name(t *testing.T) {
	if n := NewProcesses().Name(); n != MeasurementProcesses {
		t.Errorf("Name() = %q", n)
	}
}

func TestProcesses_Gather_CountsByStatus(t *testing.T) {
	procs := fakeProcs(5)
	// 让每个进程有不同状态：R, S, T, Z, I
	statuses := [][]string{{"R"}, {"S"}, {"T"}, {"Z"}, {"I"}}
	p := &Processes{
		processesFn: func(_ context.Context) ([]*process.Process, error) { return procs, nil },
		statusFn: func(_ context.Context, pr *process.Process) ([]string, error) {
			return statuses[pr.Pid-1], nil
		},
		threadsFn: func(_ context.Context, pr *process.Process) (int32, error) { return 2, nil },
		nowFn:     time.Now,
	}
	metrics, err := p.Gather(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	f := metrics[0].Fields
	if v, _ := f[FieldRunning].(int64); v != 1 {
		t.Errorf("running = %v, want 1", f[FieldRunning])
	}
	if v, _ := f[FieldSleeping].(int64); v != 2 {
		t.Errorf("sleeping (S+I) = %v, want 2", f[FieldSleeping])
	}
	if v, _ := f[FieldStopped].(int64); v != 1 {
		t.Errorf("stopped = %v, want 1", f[FieldStopped])
	}
	if v, _ := f[FieldZombies].(int64); v != 1 {
		t.Errorf("zombies = %v, want 1", f[FieldZombies])
	}
	if v, _ := f[FieldTotal].(int64); v != 5 {
		t.Errorf("total = %v, want 5", f[FieldTotal])
	}
	if v, _ := f[FieldTotalThreads].(int64); v != 10 {
		t.Errorf("total_threads = %v, want 10", f[FieldTotalThreads])
	}
}

func TestProcesses_Gather_SkipsErroredProcesses(t *testing.T) {
	// 某进程 Status() 出错（可能已退出）不应影响其他
	procs := fakeProcs(3)
	p := &Processes{
		processesFn: func(_ context.Context) ([]*process.Process, error) { return procs, nil },
		statusFn: func(_ context.Context, pr *process.Process) ([]string, error) {
			if pr.Pid == 2 {
				return nil, errors.New("no such proc")
			}
			return []string{"R"}, nil
		},
		threadsFn: func(_ context.Context, pr *process.Process) (int32, error) { return 1, nil },
		nowFn:     time.Now,
	}
	metrics, _ := p.Gather(context.Background())
	if v, _ := metrics[0].Fields[FieldRunning].(int64); v != 2 {
		t.Errorf("running = %v, want 2 (skipping errored)", v)
	}
}

func TestProcesses_Gather_ListError(t *testing.T) {
	sentinel := errors.New("boom")
	p := &Processes{
		processesFn: func(_ context.Context) ([]*process.Process, error) { return nil, sentinel },
		nowFn:       time.Now,
	}
	if _, err := p.Gather(context.Background()); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v", err)
	}
}

func TestProcesses_Gather_PSOnce(t *testing.T) {
	calls := 0
	p := &Processes{
		execPSFn: func(_ context.Context) ([]byte, error) {
			calls++
			return []byte("STAT\nSs\nR+\nU\nI\nT\nZ\n?\n"), nil
		},
		nowFn: time.Now,
	}

	metrics, err := p.Gather(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	if calls != 1 {
		t.Fatalf("ps calls = %d, want exactly 1", calls)
	}

	fields := metrics[0].Fields
	checks := map[string]int64{
		FieldRunning:  1,
		FieldSleeping: 3,
		FieldStopped:  1,
		FieldZombies:  1,
		FieldTotal:    7,
	}
	for field, want := range checks {
		if got, _ := fields[field].(int64); got != want {
			t.Errorf("%s = %v, want %d", field, fields[field], want)
		}
	}
	if _, exists := fields[FieldTotalThreads]; exists {
		t.Error("Darwin ps path should not emit Linux-only total_threads")
	}
}

func TestProcesses_Gather_PSError(t *testing.T) {
	sentinel := errors.New("ps failed")
	p := &Processes{
		execPSFn: func(_ context.Context) ([]byte, error) {
			return nil, sentinel
		},
		nowFn: time.Now,
	}
	if _, err := p.Gather(context.Background()); err == nil || !errors.Is(err, sentinel) {
		t.Errorf("err = %v", err)
	}
}
