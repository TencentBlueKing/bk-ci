//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
	"os"
	"os/exec"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
)

// supervise.go 在主 agent 进程里看护 monitor 子进程。
//
// 隔离目标：monitor 采集在 darwin 休眠等极端场景下可能整进程卡死，绝不能
// 拖累主链路（心跳 / 构建）。因此 monitor 不再在主进程内 goroutine 里跑，
// 而是作为独立子进程（`agent monitor --daemon`）运行，主进程只做：
//   spawn → Wait → 崩溃/退出后退避重启。
//
// 子进程无论卡死、OOM、panic，都只影响它自己；主进程的 Supervise goroutine
// 阻塞在 cmd.Wait() 上，不占用主链路资源，子进程退出后按退避重新拉起。

const (
	// superviseHealthyRun 子进程存活超过该时长即视为一次"健康运行"，
	// 重置连续崩溃计数（避免偶发长期运行后一次崩溃就被当成崩溃风暴）。
	superviseHealthyRun = 5 * time.Minute
	// superviseCrashCapForMaxBackoff 连续崩溃达到该次数后，退避拉到上限。
	// 对齐 daemon 的 panic cap 语义；但 monitor 永不放弃（挂了要能自愈），
	// 只是拉长间隔避免崩溃风暴刷爆日志。
	superviseCrashCapForMaxBackoff = 10
)

// 退避间隔用 var 而非 const，便于单测调小以加速。
var (
	// superviseBaseBackoff 子进程退出后的基础重启间隔。
	superviseBaseBackoff = 10 * time.Second
	// superviseMaxBackoff 连续快速崩溃时的最大重启间隔。
	superviseMaxBackoff = 60 * time.Second
)

// runChildFn 启动一次子进程并阻塞至其退出，返回退出原因。抽成包级变量
// 便于单测注入 fake，避免真的 fork 进程。生产路径为 runChildOnce。
var runChildFn = runChildOnce

// Supervise 是主 agent 用来看护 monitor 子进程的入口，应由 safeGo 包装调起。
// ctx 取消（主进程退出）时，会 kill 子进程并返回。
func Supervise(ctx context.Context) {
	self, err := os.Executable()
	if err != nil {
		logs.WithError(err).Error("monitor supervisor|cannot resolve self executable, monitor disabled")
		return
	}

	logs.Info("monitor supervisor|start, child: ", self, " monitor --daemon")

	crashCount := 0
	for {
		select {
		case <-ctx.Done():
			logs.Info("monitor supervisor|ctx done, stop")
			return
		default:
		}

		start := time.Now()
		runErr := runChildFn(ctx, self)
		ran := time.Since(start)

		// ctx 取消导致的退出：直接结束，不再重启。
		select {
		case <-ctx.Done():
			logs.Info("monitor supervisor|ctx done after child exit, stop")
			return
		default:
		}

		// 存活够久视为健康运行，重置崩溃计数。
		if ran >= superviseHealthyRun {
			crashCount = 0
		} else {
			crashCount++
		}

		backoff := superviseBaseBackoff
		if crashCount >= superviseCrashCapForMaxBackoff {
			backoff = superviseMaxBackoff
		}
		logs.Warnf("monitor supervisor|child exited after %s (err=%v, crashCount=%d), restart in %s",
			ran.Truncate(time.Second), runErr, crashCount, backoff)

		select {
		case <-ctx.Done():
			return
		case <-time.After(backoff):
		}
	}
}

// runChildOnce 启动一次 monitor 子进程并阻塞等待其退出。ctx 取消时 kill 子进程。
func runChildOnce(ctx context.Context, self string) error {
	cmd := exec.Command(self, "monitor", "--daemon")
	cmd.Dir = systemutil.GetExecutableDir()
	// 平台专属：unix 设 Setpgid 让子进程独立进程组（便于整组 kill）；
	// windows 隐藏窗口避免弹控制台。见 supervise_unix.go / supervise_win.go。
	setChildProcAttr(cmd)

	if err := cmd.Start(); err != nil {
		return err
	}
	logs.Infof("monitor supervisor|child launched, pid=%d", cmd.Process.Pid)

	// 等待子进程退出；ctx 取消时主动 kill。
	waitErr := make(chan error, 1)
	go func() {
		waitErr <- cmd.Wait()
	}()

	select {
	case <-ctx.Done():
		killChild(cmd)
		<-waitErr // 回收
		return ctx.Err()
	case err := <-waitErr:
		return err
	}
}
