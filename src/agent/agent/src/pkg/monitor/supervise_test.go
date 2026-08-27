//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
	"errors"
	"sync/atomic"
	"testing"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

// 注入 fake runChildFn，断言子进程崩溃后会被按退避重启。
func TestSupervise_RestartsOnCrash(t *testing.T) {
	origRun := runChildFn
	origBase, origMax := superviseBaseBackoff, superviseMaxBackoff
	t.Cleanup(func() {
		runChildFn = origRun
		superviseBaseBackoff, superviseMaxBackoff = origBase, origMax
	})
	// 退避调到极小，加速测试。
	superviseBaseBackoff = time.Millisecond
	superviseMaxBackoff = time.Millisecond

	var runs atomic.Int32
	runChildFn = func(ctx context.Context, self string, restartCh <-chan struct{}) (bool, error) {
		runs.Add(1)
		// 立即"崩溃"返回，模拟子进程退出。
		return false, errors.New("child crashed")
	}

	ctx, cancel := context.WithCancel(context.Background())
	done := make(chan struct{})
	go func() {
		Supervise(ctx)
		close(done)
	}()

	// 等待被重启若干次。
	deadline := time.After(2 * time.Second)
	for runs.Load() < 3 {
		select {
		case <-deadline:
			t.Fatalf("expected >=3 restarts, got %d", runs.Load())
		case <-time.After(2 * time.Millisecond):
		}
	}

	cancel()
	select {
	case <-done:
	case <-time.After(2 * time.Second):
		t.Fatal("Supervise did not stop after ctx cancel")
	}
}

// ctx 取消时 Supervise 应立即停止，不再重启。
func TestSupervise_StopsOnCtxCancel(t *testing.T) {
	origRun := runChildFn
	origBase, origMax := superviseBaseBackoff, superviseMaxBackoff
	t.Cleanup(func() {
		runChildFn = origRun
		superviseBaseBackoff, superviseMaxBackoff = origBase, origMax
	})
	superviseBaseBackoff = time.Millisecond
	superviseMaxBackoff = time.Millisecond

	var runs atomic.Int32
	// 子进程一直运行，直到 ctx 取消才返回（模拟正常常驻）。
	runChildFn = func(ctx context.Context, self string, restartCh <-chan struct{}) (bool, error) {
		runs.Add(1)
		<-ctx.Done()
		return false, ctx.Err()
	}

	ctx, cancel := context.WithCancel(context.Background())
	done := make(chan struct{})
	go func() {
		Supervise(ctx)
		close(done)
	}()

	// 给它起来跑一次。
	time.Sleep(20 * time.Millisecond)
	cancel()

	select {
	case <-done:
	case <-time.After(2 * time.Second):
		t.Fatal("Supervise did not stop after ctx cancel")
	}
	if got := runs.Load(); got != 1 {
		t.Errorf("child should run exactly once before cancel, got %d", got)
	}
}

// 配置变更事件应触发子进程立即重启（不计崩溃、不退避）。
func TestSupervise_RestartsOnConfigChange(t *testing.T) {
	origRun := runChildFn
	origBase, origMax := superviseBaseBackoff, superviseMaxBackoff
	t.Cleanup(func() {
		runChildFn = origRun
		superviseBaseBackoff, superviseMaxBackoff = origBase, origMax
	})
	// 退避拉大，确保观察到的重启只可能来自配置变更（而非崩溃退避）。
	superviseBaseBackoff = time.Hour
	superviseMaxBackoff = time.Hour

	var runs atomic.Int32
	// 子进程常驻，直到收到 restartCh（配置变更）才以 restartReq=true 返回。
	runChildFn = func(ctx context.Context, self string, restartCh <-chan struct{}) (bool, error) {
		runs.Add(1)
		select {
		case <-ctx.Done():
			return false, ctx.Err()
		case <-restartCh:
			return true, nil
		}
	}

	ctx, cancel := context.WithCancel(context.Background())
	done := make(chan struct{})
	go func() {
		Supervise(ctx)
		close(done)
	}()

	// 等第一次拉起。
	deadline := time.After(2 * time.Second)
	for runs.Load() < 1 {
		select {
		case <-deadline:
			t.Fatal("child did not start")
		case <-time.After(2 * time.Millisecond):
		}
	}

	// 发布配置变更事件，应触发立即重启（第二次拉起），无需等 1h 退避。
	config.EBus.Publish(config.MonitorConfigEvent, "changed")
	deadline = time.After(2 * time.Second)
	for runs.Load() < 2 {
		select {
		case <-deadline:
			t.Fatalf("expected restart on config change, runs=%d", runs.Load())
		case <-time.After(2 * time.Millisecond):
		}
	}

	cancel()
	select {
	case <-done:
	case <-time.After(2 * time.Second):
		t.Fatal("Supervise did not stop after ctx cancel")
	}
}
