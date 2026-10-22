package agent

import (
	"bytes"
	"context"
	"errors"
	"net"
	"net/url"
	"strings"
	"testing"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/httputil"
)

// captureAskLogs 使用独立日志实例收集输出，并固定汇总间隔。
// 测试结束后恢复全局日志，防止当前用例的输出级别和缓冲区影响其他测试。
func captureAskLogs(t *testing.T) *bytes.Buffer {
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

// TestAskLogHealthySummary 验证首次成功立即输出、窗口内保持安静，
// 并在五分钟边界准确汇总所有成功请求。
func TestAskLogHealthySummary(t *testing.T) {
	buffer := captureAskLogs(t)
	now := time.Date(2026, 10, 3, 10, 0, 0, 0, time.UTC)
	var state askLogState
	state.success(now)
	if !strings.Contains(buffer.String(), "ask connected") {
		t.Fatalf("missing initial success: %s", buffer)
	}
	buffer.Reset()
	for i := 1; i < 60; i++ {
		state.success(now.Add(time.Duration(i) * 5 * time.Second))
	}
	if buffer.Len() != 0 {
		t.Fatalf("healthy polls produced logs: %s", buffer)
	}
	state.success(now.Add(5 * time.Minute))
	if !strings.Contains(buffer.String(), "ask healthy window=5m0s success=60 failed=0") ||
		!strings.Contains(buffer.String(), "last_success=2026-10-03T10:05:00Z") {
		t.Fatalf("incorrect summary: %s", buffer)
	}
	buffer.Reset()
	// 即使请求很稀疏，也应按实际经过的时间输出，不能依赖固定请求次数。
	state.success(now.Add(11 * time.Minute))
	if !strings.Contains(buffer.String(), "window=6m0s success=1 failed=0") {
		t.Fatalf("summary depends on request count: %s", buffer)
	}
}

// TestAskLogFailureAndRecovery 覆盖同一故障的完整生命周期：
// 首次失败、重复抑制、原因变化、边界汇总、恢复，以及恢复后再次发生相同错误。
// 原因变化不应清空故障累计次数，恢复后则必须重新计数并立即报告下一次失败。
func TestAskLogFailureAndRecovery(t *testing.T) {
	buffer := captureAskLogs(t)
	now := time.Date(2026, 10, 3, 10, 0, 0, 0, time.UTC)
	var state askLogState
	state.success(now)
	buffer.Reset()
	state.failure(now.Add(5*time.Second), "timeout", errors.New("ask request failed: timeout"))
	if !strings.Contains(buffer.String(), "consecutive_failures=1") {
		t.Fatalf("first failure not logged: %s", buffer)
	}
	buffer.Reset()
	state.failure(now.Add(10*time.Second), "timeout", errors.New("ask request failed: timeout"))
	if buffer.Len() != 0 {
		t.Fatalf("duplicate failure logged: %s", buffer)
	}
	state.failure(now.Add(15*time.Second), "http:503", errors.New("ask request failed: status 503"))
	if !strings.Contains(buffer.String(), "consecutive_failures=3 failures_since_last_log=2 duration=10s") {
		t.Fatalf("changed error not logged immediately: %s", buffer)
	}
	buffer.Reset()
	state.failure(now.Add(314*time.Second), "http:503", errors.New("ask request failed: status 503"))
	if buffer.Len() != 0 {
		t.Fatalf("summary printed early: %s", buffer)
	}
	state.failure(now.Add(315*time.Second), "http:503", errors.New("ask request failed: status 503"))
	if !strings.Contains(buffer.String(), "consecutive_failures=5 failures_since_last_log=2 duration=5m10s last_success=2026-10-03T10:00:00Z") {
		t.Fatalf("incorrect failure summary: %s", buffer)
	}
	if strings.Contains(buffer.String(), "healthy") {
		t.Fatalf("failed polls reported healthy: %s", buffer)
	}
	buffer.Reset()
	state.success(now.Add(320 * time.Second))
	if !strings.Contains(buffer.String(), "ask recovered failed_count=5 outage=5m15s") ||
		!strings.Contains(buffer.String(), "success=1 failed=5") {
		t.Fatalf("incorrect recovery: %s", buffer)
	}
	buffer.Reset()
	state.success(now.Add(325 * time.Second))
	if buffer.Len() != 0 {
		t.Fatalf("recovery repeated: %s", buffer)
	}
	state.failure(now.Add(330*time.Second), "http:503", errors.New("ask request failed: status 503"))
	if !strings.Contains(buffer.String(), "consecutive_failures=1") {
		t.Fatalf("new outage was suppressed: %s", buffer)
	}
}

// TestAskLogStartsWithFailure 验证启动后尚未成功的情况：不能虚构最后成功时间，
// 第一次实际成功应作为恢复事件输出，并包含此前的失败次数和持续时间。
func TestAskLogStartsWithFailure(t *testing.T) {
	buffer := captureAskLogs(t)
	now := time.Now()
	var state askLogState
	state.failure(now, "timeout", context.DeadlineExceeded)
	if !strings.Contains(buffer.String(), "last_success=never") {
		t.Fatalf("invented last success: %s", buffer)
	}
	buffer.Reset()
	state.success(now.Add(time.Minute))
	if !strings.Contains(buffer.String(), "ask recovered failed_count=1 outage=1m0s") {
		t.Fatalf("initial outage recovery missing: %s", buffer)
	}
}

// TestParseAskResponse 验证各层失败都返回非空错误分类键，供上层限频使用。
// 特别覆盖 data=null：JSON 解析本身可能成功，但该响应不能被统计为健康。
func TestParseAskResponse(t *testing.T) {
	for _, tt := range []struct {
		name   string
		result *httputil.AgentResult
		err    error
		valid  bool
	}{
		{name: "transport", err: context.DeadlineExceeded},
		{name: "nil result"},
		{name: "business failure", result: &httputil.AgentResult{DevopsResult: httputil.DevopsResult{Status: 1}}},
		{name: "agent deleted", result: &httputil.AgentResult{AgentStatus: config.AgentStatusDelete}},
		{name: "null data", result: &httputil.AgentResult{AgentStatus: config.AgentStatusImportOk}},
		{name: "malformed data", result: &httputil.AgentResult{AgentStatus: config.AgentStatusImportOk, DevopsResult: httputil.DevopsResult{Data: "invalid"}}},
		{name: "valid", valid: true, result: &httputil.AgentResult{AgentStatus: config.AgentStatusImportOk, DevopsResult: httputil.DevopsResult{Data: map[string]interface{}{"heartbeat": map[string]interface{}{}}}}},
	} {
		t.Run(tt.name, func(t *testing.T) {
			resp, key, err := parseAskResponse(tt.result, tt.err)
			if tt.valid {
				if err != nil || resp == nil || key != "" {
					t.Fatalf("valid response rejected: %v %s", err, key)
				}
			} else if err == nil || resp != nil || key == "" {
				t.Fatalf("invalid response accepted: resp=%v key=%q err=%v", resp, key, err)
			}
		})
	}
}

// TestAskRequestErrorKey 验证连接源端口变化不会把同一故障误判为新故障，
// 同时确保连接重置和请求超时这类不同原因仍能分别被识别。
func TestAskRequestErrorKey(t *testing.T) {
	makeError := func(port int) error {
		return &url.Error{Op: "Post", URL: "http://gateway/ask", Err: &net.OpError{
			Op: "read", Net: "tcp", Source: &net.TCPAddr{Port: port}, Err: errors.New("connection reset"),
		}}
	}
	if askRequestErrorKey(makeError(12345)) != askRequestErrorKey(makeError(12346)) {
		t.Fatal("changing source port bypasses failure suppression")
	}
	if askRequestErrorKey(makeError(12345)) == askRequestErrorKey(context.DeadlineExceeded) {
		t.Fatal("different error kinds must be logged immediately")
	}
}
