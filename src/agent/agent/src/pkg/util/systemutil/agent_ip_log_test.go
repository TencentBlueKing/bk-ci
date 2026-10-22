package systemutil

import (
	"bytes"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

// captureIPLogs 收集 IP 日志并固定五分钟汇总间隔，测试结束后恢复全局日志实例。
func captureIPLogs(t *testing.T) *bytes.Buffer {
	t.Helper()
	t.Setenv("DEVOPS_AGENT_STATUS_LOG_INTERVAL", "5m")
	original := logs.Logs
	buffer := new(bytes.Buffer)
	logger := logrus.New()
	logger.SetOutput(buffer)
	logger.SetFormatter(&logs.MyFormatter{})
	logs.Logs = logrus.NewEntry(logger)
	t.Cleanup(func() { logs.Logs = original })
	return buffer
}

// TestAgentIPLogSummaryAndChanges 验证首次输出和重复汇总，
// 并分别改变 IP、路由、网卡名称/描述、虚拟标记和评分，确认变化都不会被限频隐藏。
func TestAgentIPLogSummaryAndChanges(t *testing.T) {
	buffer := captureIPLogs(t)
	now := time.Now()
	selection := agentIPSelection{
		candidate: agentIPCandidate{ip: "10.0.0.1", ifaceName: "Ethernet0", description: "Physical NIC", score: 380},
		routeIP:   "10.0.0.1",
	}
	var state agentIPLogState
	state.record(now, selection)
	if !strings.Contains(buffer.String(), "|info|select agent ip=10.0.0.1") || !strings.Contains(buffer.String(), "event=selected") {
		t.Fatalf("missing initial selection: %s", buffer)
	}
	buffer.Reset()
	for i := 1; i < 5; i++ {
		state.record(now.Add(time.Duration(i)*time.Minute), selection)
	}
	if buffer.Len() != 0 {
		t.Fatalf("unchanged selection produced INFO logs: %s", buffer)
	}
	state.record(now.Add(5*time.Minute), selection)
	if !strings.Contains(buffer.String(), "event=summary") || !strings.Contains(buffer.String(), "unchanged_checks=5") {
		t.Fatalf("incorrect summary: %s", buffer)
	}
	changes := []struct {
		name   string
		change func(*agentIPSelection)
	}{
		{"IP", func(s *agentIPSelection) { s.candidate.ip = "10.0.0.2" }},
		{"route", func(s *agentIPSelection) { s.routeIP = "10.0.0.2" }},
		{"interface", func(s *agentIPSelection) { s.candidate.ifaceName = "Ethernet1" }},
		{"description", func(s *agentIPSelection) { s.candidate.description = "USB NIC" }},
		{"virtual", func(s *agentIPSelection) { s.candidate.isVirtual = true }},
		{"score", func(s *agentIPSelection) { s.candidate.score = 180 }},
	}
	for i, tt := range changes {
		buffer.Reset()
		tt.change(&selection)
		state.record(now.Add(5*time.Minute+time.Duration(i+1)*time.Second), selection)
		if !strings.Contains(buffer.String(), "event=changed") {
			t.Fatalf("%s change suppressed: %s", tt.name, buffer)
		}
	}
	buffer.Reset()
	state.record(now.Add(6*time.Minute), selection)
	if buffer.Len() != 0 {
		t.Fatalf("unchanged state after change should be quiet: %s", buffer)
	}
	logs.Logs.Logger.SetLevel(logrus.DebugLevel)
	state.record(now.Add(7*time.Minute), selection)
	if !strings.Contains(buffer.String(), "|debug|select agent ip=") {
		t.Fatalf("debug detail missing: %s", buffer)
	}
}

// TestAgentIPLogFallbackAndRecovery 验证最终 IP 相同时仍能识别回退和恢复，
// 防止只比较 IP 字符串而漏掉选址健康状态的变化。
func TestAgentIPLogFallbackAndRecovery(t *testing.T) {
	buffer := captureIPLogs(t)
	now := time.Now()
	selection := agentIPSelection{candidate: agentIPCandidate{ip: "10.0.0.1"}, routeIP: "10.0.0.1"}
	var state agentIPLogState
	state.record(now, selection)
	buffer.Reset()
	fallback := selection
	fallback.fallback, fallback.reason = true, "no eligible interface addresses"
	state.record(now.Add(time.Second), fallback)
	if !strings.Contains(buffer.String(), "|warning|") || !strings.Contains(buffer.String(), "fallback=true") {
		t.Fatalf("fallback not reported: %s", buffer)
	}
	buffer.Reset()
	state.record(now.Add(time.Minute), fallback)
	if buffer.Len() != 0 {
		t.Fatalf("repeated fallback not suppressed: %s", buffer)
	}
	state.record(now.Add(2*time.Minute), selection)
	if !strings.Contains(buffer.String(), "event=recovered") {
		t.Fatalf("same-IP recovery suppressed: %s", buffer)
	}
}

// TestAgentIPLogConcurrentObservers 模拟多个协程同时观察同一选址结果。
// 只有一个协程应输出首次日志，其余观察合并为重复计数；同时供 race 检测验证锁保护。
func TestAgentIPLogConcurrentObservers(t *testing.T) {
	buffer := captureIPLogs(t)
	var state agentIPLogState
	selection := agentIPSelection{candidate: agentIPCandidate{ip: "10.0.0.1"}}
	now := time.Now()
	var wg sync.WaitGroup
	for i := 0; i < 20; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			state.record(now, selection)
		}()
	}
	wg.Wait()
	if strings.Count(buffer.String(), "select agent ip=") != 1 {
		t.Fatalf("concurrent observers produced duplicate logs: %s", buffer)
	}
}
