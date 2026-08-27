//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
	"os"
	"os/exec"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
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
//
// 配置热更新：主进程回写 .agent.properties（config.SaveConfig）时，若 monitor
// 关心字段变化会发布 config.MonitorConfigEvent，本看护订阅后立即 kill 并重启
// 子进程（不计崩溃、不退避），子进程重启后自然加载新配置——替代子进程轮询。

const (
	// superviseHealthyRun 子进程存活超过该时长即视为一次"健康运行"，
	// 重置连续崩溃计数（避免偶发长期运行后一次崩溃就被当成崩溃风暴）。
	superviseHealthyRun = 5 * time.Minute
	// superviseCrashCapForMaxBackoff 连续崩溃达到该次数后，退避拉到上限。
	// 对齐 daemon 的 panic cap 语义；但 monitor 永不放弃（挂了要能自愈），
	// 只是拉长间隔避免崩溃风暴刷爆日志。
	superviseCrashCapForMaxBackoff = 10
	// monitorSuperviseEBusID 订阅 MonitorConfigEvent 时的订阅者 id。
	monitorSuperviseEBusID = "monitor-supervisor"
)

// 退避间隔用 var 而非 const，便于单测调小以加速。
var (
	// superviseBaseBackoff 子进程退出后的基础重启间隔。
	superviseBaseBackoff = 10 * time.Second
	// superviseMaxBackoff 连续快速崩溃时的最大重启间隔。
	superviseMaxBackoff = 60 * time.Second
)

// runChildFn 启动一次子进程并阻塞至其退出，返回 (是否为配置变更主动重启, 退出原因)。
// 抽成包级变量便于单测注入 fake，避免真的 fork 进程。生产路径为 runChildOnce。
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

	// 订阅 monitor 配置变更事件，转成一个纯重启信号 channel。收到即立即重启
	// 子进程（不计崩溃、不退避）。
	cfgCh := config.EBus.Subscribe(config.MonitorConfigEvent, monitorSuperviseEBusID, 1)
	defer config.EBus.Unsubscribe(config.MonitorConfigEvent, monitorSuperviseEBusID)
	restartCh := make(chan struct{}, 1)
	go func() {
		for {
			select {
			case <-ctx.Done():
				return
			case _, ok := <-cfgCh.DChan:
				if !ok {
					return
				}
				// 非阻塞投递：已有待处理的重启信号时直接合并。
				select {
				case restartCh <- struct{}{}:
				default:
				}
			}
		}
	}()

	crashCount := 0
	for {
		select {
		case <-ctx.Done():
			logs.Info("monitor supervisor|ctx done, stop")
			return
		default:
		}

		start := time.Now()
		restartReq, runErr := runChildFn(ctx, self, restartCh)
		ran := time.Since(start)

		// ctx 取消导致的退出：直接结束，不再重启。
		select {
		case <-ctx.Done():
			logs.Info("monitor supervisor|ctx done after child exit, stop")
			return
		default:
		}

		// 配置变更触发的主动重启：立即拉起、不计崩溃、不退避。
		if restartReq {
			logs.Info("monitor supervisor|config changed, restart child immediately")
			crashCount = 0
			continue
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
		case <-restartCh:
			// 退避等待期间配置变更：立即结束退避、重启。
			logs.Info("monitor supervisor|config changed during backoff, restart now")
			crashCount = 0
		case <-time.After(backoff):
		}
	}
}

// runChildOnce 启动一次 monitor 子进程并阻塞等待其退出。
// 返回 (是否因配置变更被主动重启, 退出原因)：
//   - ctx 取消：kill 子进程，返回 (false, ctx.Err())
//   - 收到 restartCh（配置变更）：kill 子进程，返回 (true, nil)
//   - 子进程自行退出：返回 (false, waitErr)
//
// 父子存活管道：父进程持有子进程 stdin 的写端。父进程一旦退出（正常或崩溃），
// OS 关闭写端，子进程 stdin 读到 EOF 即自退（见 daemon.watchParentPipe），
// 跨平台一致、不依赖 ppid、不受容器 PID 影响。
func runChildOnce(ctx context.Context, self string, restartCh <-chan struct{}) (bool, error) {
	cmd := exec.Command(self, "monitor", "--daemon")
	cmd.Dir = systemutil.GetExecutableDir()
	// 平台专属：unix 设 Setpgid 让子进程独立进程组（便于整组 kill）；
	// windows 隐藏窗口避免弹控制台。见 supervise_unix.go / supervise_win.go。
	setChildProcAttr(cmd)

	// 建立父子存活管道（stdin）。StdinPipe 在 unix / windows 均可用。
	stdinW, err := cmd.StdinPipe()
	if err != nil {
		return false, err
	}

	if err := cmd.Start(); err != nil {
		_ = stdinW.Close()
		return false, err
	}
	logs.Infof("monitor supervisor|child launched, pid=%d", cmd.Process.Pid)

	// 等待子进程退出；ctx 取消或配置变更时主动 kill。
	waitErr := make(chan error, 1)
	go func() {
		waitErr <- cmd.Wait()
	}()

	select {
	case <-ctx.Done():
		_ = stdinW.Close()
		killChild(cmd)
		<-waitErr // 回收
		return false, ctx.Err()
	case <-restartCh:
		_ = stdinW.Close()
		killChild(cmd)
		<-waitErr // 回收
		return true, nil
	case err := <-waitErr:
		_ = stdinW.Close()
		return false, err
	}
}
