//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
	"sync/atomic"
	"testing"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

// slowInput 模拟一个卡死的 input：Gather 阻塞直到 ctx 超时。
type slowInput struct {
	name       string
	started    atomic.Int32
	ctxExpired atomic.Int32
}

func (s *slowInput) Name() string { return s.name }

func (s *slowInput) Gather(ctx context.Context) ([]Metric, error) {
	s.started.Add(1)
	// 模拟底层 gopsutil *WithContext：阻塞直到 ctx 被取消（超时）。
	<-ctx.Done()
	s.ctxExpired.Add(1)
	return nil, ctx.Err()
}

// 卡死的 input 会在 perInputTimeout 后因 ctx 超时返回，不阻塞整轮。
// 同轮其他正常 input 照常上报。
func TestDoOneGather_PerInputTimeout(t *testing.T) {
	// 把 per-input 超时调小以加速（改包级 var）。
	orig := perInputTimeout
	t.Cleanup(func() { perInputTimeout = orig })
	perInputTimeout = 50 * time.Millisecond

	slow := &slowInput{name: "netstat"}
	good := &stubInput{
		name:    "mem",
		metrics: []Metric{{Name: "mem", Fields: map[string]interface{}{"x": 1.0}}},
	}
	ins := []Input{slow, good}

	origCfg := config.GAgentConfig
	t.Cleanup(func() { config.GAgentConfig = origCfg })
	config.GAgentConfig = &config.AgentConfig{
		ProjectId: "bkci", AgentId: "a", SecretKey: "s",
		Gateway: "http://example.com",
	}

	var posted int32
	reporter := &Reporter{
		nowFn: time.Now,
		doPost: func(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error) {
			atomic.AddInt32(&posted, 1)
			return 200, nil, nil
		},
	}
	dumper := NewMonitorDumper(t.TempDir())

	start := time.Now()
	doOneGather(context.Background(), newInputRunners(ins), reporter, dumper)
	elapsed := time.Since(start)

	// 应在 perInputTimeout 量级返回。
	if elapsed > 5*time.Second {
		t.Fatalf("doOneGather blocked too long (%s), per-input timeout not effective", elapsed)
	}
	if slow.started.Load() != 1 {
		t.Errorf("slow input should have started once, got %d", slow.started.Load())
	}
	if slow.ctxExpired.Load() != 1 {
		t.Errorf("slow input ctx should have expired via per-input timeout, got %d", slow.ctxExpired.Load())
	}
	// 正常 input 的指标照常上报。
	if atomic.LoadInt32(&posted) != 1 {
		t.Errorf("healthy input should still be reported, posted=%d", posted)
	}
}

// stuckInput 模拟不响应 context 的内核级阻塞。只有测试主动释放后才返回。
type stuckInput struct {
	name    string
	started atomic.Int32
	release chan struct{}
}

func (s *stuckInput) Name() string { return s.name }

func (s *stuckInput) Gather(_ context.Context) ([]Metric, error) {
	s.started.Add(1)
	<-s.release
	return nil, nil
}

// 一个 input 永久卡住后，后续轮次必须跳过它，而不是再创建 Gather goroutine。
// 同时其他 input 每轮仍能正常采集和上报。
func TestDoOneGather_StuckInputIsNotReentered(t *testing.T) {
	orig := perInputTimeout
	t.Cleanup(func() { perInputTimeout = orig })
	perInputTimeout = 30 * time.Millisecond

	release := make(chan struct{})
	released := false
	t.Cleanup(func() {
		if !released {
			close(release)
		}
	})

	stuck := &stuckInput{name: "netstat", release: release}
	good := &stubInput{
		name:    "mem",
		metrics: []Metric{{Name: "mem", Fields: map[string]interface{}{"x": 1.0}}},
	}
	runners := newInputRunners([]Input{stuck, good})

	origCfg := config.GAgentConfig
	t.Cleanup(func() { config.GAgentConfig = origCfg })
	config.GAgentConfig = &config.AgentConfig{
		ProjectId: "bkci", AgentId: "a", SecretKey: "s",
		Gateway: "http://example.com",
	}

	var posted int32
	reporter := &Reporter{
		nowFn: time.Now,
		doPost: func(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error) {
			atomic.AddInt32(&posted, 1)
			return 200, nil, nil
		},
	}
	dumper := NewMonitorDumper(t.TempDir())

	doOneGather(context.Background(), runners, reporter, dumper)
	doOneGather(context.Background(), runners, reporter, dumper)

	if got := stuck.started.Load(); got != 1 {
		t.Errorf("stuck input Gather calls = %d, want 1", got)
	}
	if got := good.calls.Load(); got != 2 {
		t.Errorf("healthy input Gather calls = %d, want 2", got)
	}
	if got := atomic.LoadInt32(&posted); got != 2 {
		t.Errorf("healthy metrics reports = %d, want 2", got)
	}

	close(release)
	released = true
	deadline := time.Now().Add(time.Second)
	for runners[0].running.Load() && time.Now().Before(deadline) {
		time.Sleep(time.Millisecond)
	}
	if runners[0].running.Load() {
		t.Fatal("stuck runner did not return to idle after release")
	}
}
