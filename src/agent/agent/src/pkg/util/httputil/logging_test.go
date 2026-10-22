package httputil

import (
	"bytes"
	"context"
	"io"
	"net/http"
	"os"
	"strings"
	"testing"

	"github.com/sirupsen/logrus"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/envs"
)

// logTestTransport 在不访问网络的情况下模拟响应或超时，
// 让测试聚焦日志级别与错误传递，不依赖真实连接和超时等待。
type logTestTransport func(*http.Request) (*http.Response, error)

func (f logTestTransport) RoundTrip(r *http.Request) (*http.Response, error) { return f(r) }

// captureHTTPLogs 隔离 HTTP 测试依赖的日志、配置、环境变量和全局客户端。
// 默认关闭超时退出配置，测试结束后恢复原状态；需要验证计数行为的用例再单独开启。
func captureHTTPLogs(t *testing.T) *bytes.Buffer {
	t.Helper()
	originalLogs, originalConfig, originalEnvs, originalClient := logs.Logs, config.GAgentConfig, envs.GApiEnvVars, client
	buffer := new(bytes.Buffer)
	logger := logrus.New()
	logger.SetOutput(buffer)
	logger.SetFormatter(&logs.MyFormatter{})
	logs.Logs = logrus.NewEntry(logger)
	config.GAgentConfig = &config.AgentConfig{TimeoutSec: 10}
	envs.Init()
	t.Setenv("DEVOPS_AGENT_TIMEOUT_EXIT_TIME", "")
	if err := os.Unsetenv("DEVOPS_AGENT_TIMEOUT_EXIT_TIME"); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		logs.Logs, config.GAgentConfig, envs.GApiEnvVars, client = originalLogs, originalConfig, originalEnvs, originalClient
	})
	return buffer
}

// TestRepeatedHTTPDetailsOnlyInDebug 验证重复请求体和响应体在 INFO 下不输出，
// 开启 DEBUG 后仍可排查细节，而发生内容变化时继续保留原有详细日志。
func TestRepeatedHTTPDetailsOnlyInDebug(t *testing.T) {
	buffer := captureHTTPLogs(t)
	const response = `{"status":0}`
	client = &http.Client{Transport: logTestTransport(func(*http.Request) (*http.Response, error) {
		return &http.Response{StatusCode: 200, Status: "200 OK", Body: io.NopCloser(strings.NewReader(response))}, nil
	})}
	previous := &IgnoreDupLogResp{Status: 200, Resp: response}
	send := func(repeat bool) *HttpResult {
		return NewHttpClient().Post("http://example.invalid/ask").Body(&struct{}{}, repeat).Execute(previous)
	}
	if result := send(true); result.Error != nil || !result.IgnoreDupLog {
		t.Fatalf("unexpected result: %+v", result)
	}
	if buffer.Len() != 0 {
		t.Fatalf("duplicate details leaked at INFO: %s", buffer)
	}
	logs.Logs.Logger.SetLevel(logrus.DebugLevel)
	send(true)
	if !strings.Contains(buffer.String(), "|debug|http://example.invalid/ask|body repeat") ||
		!strings.Contains(buffer.String(), "|debug|http://example.invalid/ask|resp repeat") {
		t.Fatalf("debug details missing: %s", buffer)
	}
	logs.Logs.Logger.SetLevel(logrus.InfoLevel)
	buffer.Reset()
	previous.Resp = "different"
	send(false)
	if !strings.Contains(buffer.String(), "request body:") || !strings.Contains(buffer.String(), "http respBody:") {
		t.Fatalf("changed payload logs lost: %s", buffer)
	}
}

// TestPollingTimeoutDelegatesLoggingToCaller 验证轮询超时由上层统一报告，
// 底层超时提示和退出计数不能绕过限频；普通非轮询请求仍保留原有超时 WARN。
func TestPollingTimeoutDelegatesLoggingToCaller(t *testing.T) {
	buffer := captureHTTPLogs(t)
	newRequest := func() *HttpClient {
		request := NewHttpClient().Get("http://example.invalid/ask")
		request.client = &http.Client{Transport: logTestTransport(func(*http.Request) (*http.Response, error) {
			return nil, context.DeadlineExceeded
		})}
		return request
	}
	if result := newRequest().Execute(&IgnoreDupLogResp{}); result.Error == nil {
		t.Fatal("timeout error was lost")
	}
	if buffer.Len() != 0 {
		t.Fatalf("polling timeout bypassed caller's suppression: %s", buffer)
	}
	newRequest().Execute(nil)
	if !strings.Contains(buffer.String(), "http request time out") {
		t.Fatalf("non-polling timeout warning lost: %s", buffer)
	}
	buffer.Reset()
	t.Setenv("DEVOPS_AGENT_TIMEOUT_EXIT_TIME", "1000")
	newRequest().Execute(&IgnoreDupLogResp{})
	checkTimeOutExit(nil) // 用一次成功观察抵消本用例增加的超时计数，避免影响后续测试。
	if buffer.Len() != 0 {
		t.Fatalf("timeout exit counter bypassed polling suppression: %s", buffer)
	}
}

// TestRepeatedMalformedAgentResult 验证非法 JSON 首次保留错误详情，
// 连续相同响应不重复打印 ERROR，但仍向调用方返回包含 HTTP 状态的解析错误。
func TestRepeatedMalformedAgentResult(t *testing.T) {
	buffer := captureHTTPLogs(t)
	result := &HttpResult{Status: 200, Body: []byte("invalid json")}
	if _, err := result.IntoAgentResult(); err == nil || !strings.Contains(buffer.String(), "parse agent result") {
		t.Fatal("initial parse failure must be reported")
	}
	buffer.Reset()
	result.IgnoreDupLog = true
	if _, err := result.IntoAgentResult(); err == nil || !strings.Contains(err.Error(), "http status 200") {
		t.Fatalf("parse error detail lost: %v", err)
	}
	if buffer.Len() != 0 {
		t.Fatalf("repeated parse failure bypassed suppression: %s", buffer)
	}
}
