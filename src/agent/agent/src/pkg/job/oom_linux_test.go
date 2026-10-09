//go:build linux

package job

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"os/exec"
	"sync/atomic"
	"testing"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/envs"
	exitcode "github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/oomprotect"
)

// 保护分数仅在独立测试子进程中设置；测试 HTTP 服务只绑定本机随机端口。
func TestOOMFinishRetryAndRecoverableProbe(t *testing.T) {
	if os.Getenv("BK_CI_OOM_JOB_TEST") != "1" {
		cmd := exec.Command(os.Args[0], "-test.run=^TestOOMFinishRetryAndRecoverableProbe$", "-test.timeout=30s")
		cmd.Env = append(os.Environ(), "BK_CI_OOM_JOB_TEST=1")
		out, err := cmd.CombinedOutput()
		if ee, ok := err.(*exec.ExitError); ok && ee.ExitCode() == 77 {
			t.Skipf("CAP_SYS_RESOURCE unavailable: %s", out)
		}
		if err != nil {
			t.Fatalf("protected job test: %v: %s", err, out)
		}
		return
	}
	logs.UNTestDebugInit()
	envs.Init()
	t.Setenv(oomprotect.EnvKey, "true")
	if err := oomprotect.Init(t.TempDir()); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(77)
	}

	exitcode.GetAndResetExitError()
	exitcode.AddExitError(exitcode.ExitJdkError, "signal: killed")
	if got := exitcode.GetAndResetExitError(); got != nil {
		t.Fatalf("resource failure would exit daemon: %+v", got)
	}
	if oomprotect.Ready() == nil {
		t.Fatal("resource failure did not pause new builds")
	}
	exitcode.AddExitError(exitcode.ExitNotWorker, "worker missing")
	if got := exitcode.GetAndResetExitError(); got == nil || got.ErrorEnum != exitcode.ExitNotWorker {
		t.Fatal("non-resource exit semantics changed")
	}

	var calls atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		if calls.Add(1) == 1 {
			fmt.Fprint(w, "{\"status\":1,\"message\":\"temporary failure\"}")
			return
		}
		fmt.Fprint(w, "{\"status\":0,\"data\":true}")
	}))
	defer server.Close()
	config.GAgentConfig = &config.AgentConfig{Gateway: server.URL, TimeoutSec: 5}
	count := 1
	info := &api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: api.ThirdPartyBuildInfo{ExecuteCount: &count}}
	start := time.Now()
	reportWorkerFinish(info)
	if calls.Load() != 2 {
		t.Fatalf("finish calls = %d, want 2", calls.Load())
	}
	if time.Since(start) < 5*time.Second {
		t.Fatal("retry did not back off")
	}
	// 旧协议没有执行次数时只能单次上报，避免补发结果误伤新的构建重试。
	calls.Store(0)
	info.ExecuteCount = nil
	reportWorkerFinish(info)
	if calls.Load() != 1 {
		t.Fatal("unfenced old-protocol result retried")
	}
}
