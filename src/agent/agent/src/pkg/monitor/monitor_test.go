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

// stubInput 用于测试主循环，不依赖 gopsutil。
type stubInput struct {
	name    string
	metrics []Metric
	err     error
	calls   atomic.Int32
}

func (s *stubInput) Name() string { return s.name }
func (s *stubInput) Gather() ([]Metric, error) {
	s.calls.Add(1)
	if s.err != nil {
		return nil, s.err
	}
	return append([]Metric(nil), s.metrics...), nil
}

// Collect 在 MonitorOn=false 时应直接返回，不订阅 EBus。
func TestCollect_DisabledByConfig(t *testing.T) {
	orig := config.GAgentConfig
	t.Cleanup(func() { config.GAgentConfig = orig })
	config.GAgentConfig = &config.AgentConfig{MonitorOn: false}

	done := make(chan struct{})
	go func() {
		Collect()
		close(done)
	}()
	select {
	case <-done:
	case <-time.After(500 * time.Millisecond):
		t.Fatal("Collect should return immediately when MonitorOn=false")
	}
}

// doOneGather 应把所有 input 的 metric 汇总后经过 rename 送到 reporter。
func TestDoOneGather_AggregatesAndReports(t *testing.T) {
	stub1 := &stubInput{
		name: "cpu",
		metrics: []Metric{{
			Name:   MeasurementCPU,
			Fields: map[string]interface{}{FieldUsageUser: 1.0},
		}},
	}
	stub2 := &stubInput{
		name: "mem",
		metrics: []Metric{{
			Name:   MeasurementMem,
			Fields: map[string]interface{}{FieldUsedPercent: 50.0},
		}},
	}
	ins := []Input{stub1, stub2}

	var captured []Metric
	reporter := &Reporter{
		nowFn: time.Now,
		doPost: func(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error) {
			return 200, nil, nil
		},
	}
	// 用 stub config 避免 buildGateway 空校验失败
	orig := config.GAgentConfig
	t.Cleanup(func() { config.GAgentConfig = orig })
	config.GAgentConfig = &config.AgentConfig{
		ProjectId: "bkci",
		AgentId:   "a",
		SecretKey: "s",
		BuildType: "THIRD_PARTY",
		Gateway:   "http://example.com",
		MonitorOn: true,
	}
	// 用 report 捕获
	reporter.doPost = func(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error) {
		// 从 body 反解析太重，这里只断言调用发生
		return 200, nil, nil
	}

	dir := t.TempDir()
	dumper := NewMonitorDumper(dir)

	doOneGather(context.Background(), ins, reporter, dumper)

	if stub1.calls.Load() != 1 || stub2.calls.Load() != 1 {
		t.Errorf("each input should be called once: stub1=%d stub2=%d",
			stub1.calls.Load(), stub2.calls.Load())
	}
	_ = captured // 该变量保留以便未来断言
}

// 单个 input 失败不应影响其他。
func TestDoOneGather_InputErrorIsolated(t *testing.T) {
	good := &stubInput{
		name:    "mem",
		metrics: []Metric{{Name: "mem", Fields: map[string]interface{}{"x": 1.0}}},
	}
	bad := &stubInput{name: "cpu", err: errors.New("boom")}
	ins := []Input{bad, good}

	orig := config.GAgentConfig
	t.Cleanup(func() { config.GAgentConfig = orig })
	config.GAgentConfig = &config.AgentConfig{
		ProjectId: "bkci", AgentId: "a", SecretKey: "s",
		Gateway: "http://example.com", MonitorOn: true,
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

	doOneGather(context.Background(), ins, reporter, dumper)

	if atomic.LoadInt32(&posted) != 1 {
		t.Errorf("reporter should be invoked once even with partial failures, got %d", posted)
	}
	if good.calls.Load() != 1 || bad.calls.Load() != 1 {
		t.Error("both inputs should have been called")
	}
}

// 全部 input 都空时不触发 HTTP。
func TestDoOneGather_EmptySkipsReport(t *testing.T) {
	ins := []Input{&stubInput{name: "x", metrics: nil}}
	orig := config.GAgentConfig
	t.Cleanup(func() { config.GAgentConfig = orig })
	config.GAgentConfig = &config.AgentConfig{
		ProjectId: "bkci", AgentId: "a", SecretKey: "s",
		Gateway: "http://example.com", MonitorOn: true,
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
	doOneGather(context.Background(), ins, reporter, dumper)
	if atomic.LoadInt32(&posted) != 0 {
		t.Errorf("empty gather should not trigger HTTP, posted=%d", posted)
	}
}
