package api

import (
	"bytes"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"sync/atomic"
	"testing"

	"github.com/sirupsen/logrus"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/envs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/httputil"
)

// TestAskLoggingAndHTTPStatus 通过本地 HTTP 服务验证真实 Ask 调用链。
// 覆盖首次请求详情、重复内容抑制、HTTP 错误及恢复，确保去重只影响日志输出，
// 不会吞掉请求失败，也不会遗漏响应状态变化时的详细日志。
func TestAskLoggingAndHTTPStatus(t *testing.T) {
	originalConfig, originalEnvs, originalRequest, originalLogs := config.GAgentConfig, envs.GApiEnvVars, askRequest, logs.Logs
	t.Cleanup(func() {
		config.GAgentConfig, envs.GApiEnvVars, askRequest, logs.Logs = originalConfig, originalEnvs, originalRequest, originalLogs
	})
	buffer := new(bytes.Buffer)
	logger := logrus.New()
	logger.SetOutput(buffer)
	logs.Logs = logrus.NewEntry(logger)
	envs.Init()
	t.Setenv("DEVOPS_AGENT_TIMEOUT_EXIT_TIME", "")
	if err := os.Unsetenv("DEVOPS_AGENT_TIMEOUT_EXIT_TIME"); err != nil {
		t.Fatal(err)
	}
	// 清除测试使用的代理配置，确保请求直达本地服务，避免宿主机代理影响结果。
	envs.GApiEnvVars.SetEnvs(map[string]string{"HTTP_PROXY": "", "HTTPS_PROXY": "", "NO_PROXY": "*"})
	t.Setenv("HTTP_PROXY", "")
	t.Setenv("HTTPS_PROXY", "")
	t.Setenv("NO_PROXY", "*")
	type reply struct {
		status int
		body   string
	}
	const success = `{"status":0,"agentStatus":"IMPORT_OK","data":{"heartbeat":{}}}`
	// HTTP 处理器在独立协程中读取响应配置，使用原子值避免切换测试阶段时发生数据竞争。
	var response atomic.Value
	response.Store(reply{200, success})
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/ask" {
			t.Errorf("unexpected path: %s", r.URL.Path)
		}
		current := response.Load().(reply)
		w.WriteHeader(current.status)
		_, _ = io.WriteString(w, current.body)
	}))
	defer server.Close()
	config.GAgentConfig = &config.AgentConfig{Gateway: server.URL, TimeoutSec: 5}
	askRequest.Body, askRequest.Resp = nil, &httputil.IgnoreDupLogResp{}
	info := &AskInfo{}
	if _, err := Ask(info); err != nil {
		t.Fatalf("initial request failed: %v", err)
	}
	if !strings.Contains(buffer.String(), "request body:") || !strings.Contains(buffer.String(), "http respBody:") {
		t.Fatalf("initial payload logs missing: %s", buffer)
	}
	buffer.Reset()
	if _, err := Ask(info); err != nil {
		t.Fatalf("repeated request failed: %v", err)
	}
	if buffer.Len() != 0 {
		t.Fatalf("unchanged ask produced INFO logs: %s", buffer)
	}
	// 即使响应体具有成功格式，只要 HTTP 状态为 503，仍必须返回失败。
	response.Store(reply{503, success})
	if _, err := Ask(info); err == nil || !strings.Contains(err.Error(), "503") {
		t.Fatalf("HTTP failure accepted: %v", err)
	}
	if !strings.Contains(buffer.String(), "503") {
		t.Fatalf("changed response not logged: %s", buffer)
	}
	buffer.Reset()
	if _, err := Ask(info); err == nil {
		t.Fatal("repeated HTTP failure was lost")
	}
	if buffer.Len() != 0 {
		t.Fatalf("repeated HTTP failure logged at INFO: %s", buffer)
	}
	response.Store(reply{200, success})
	if _, err := Ask(info); err != nil {
		t.Fatalf("recovery failed: %v", err)
	}
	if !strings.Contains(buffer.String(), "http respBody:") {
		t.Fatalf("recovered response detail missing: %s", buffer)
	}
}
