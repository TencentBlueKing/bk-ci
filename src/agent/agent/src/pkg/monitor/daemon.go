//go:build !loong64
// +build !loong64

package monitor

import (
	"os"
	"path/filepath"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
)

// daemon.go 是 monitor 独立子进程的入口。
//
// 背景：monitor 的部分采集（darwin 的 net/netstat/processes）通过 gopsutil
// fork 外部命令（netstat/lsof/ps）实现。机器休眠冻结时，fork 会卡在内核态
// 长时间不返回，且 exec.CommandContext 的超时 kill 对"尚未 exec 成功"的进程
// 无效——ctx 再短也打不断。为保证「监控就算死掉也绝不干扰主链路」，把整个
// monitor 采集放到独立子进程：
//   - 子进程卡死 / OOM / panic 只发生在子进程；
//   - 主 agent 只负责 spawn + Wait + 退避重启（见 supervise.go）。
//
// 子进程自初始化 config 与独立日志（devopsMonitor.log，只保留 1 天），
// 然后进入 Collect 常驻循环。
//
// 存活与配置：
//   - 检活走父子 stdin 管道：父进程退出（正常/崩溃）→ 写端关闭 → 本进程
//     stdin 读到 EOF → 自退（watchParentPipe），跨平台一致、不依赖 ppid。
//   - 配置变更由父进程 SaveConfig 检测并重启本子进程，重启后重新加载新配置，
//     因此这里不再轮询 .agent.properties。

const (
	// MonitorLogName monitor 子进程的独立日志文件名。与主日志 devopsAgent.log
	// 隔离，且只保留 1 天（见 RunDaemon 里的 InitWithRotate 参数）。
	MonitorLogName = "devopsMonitor.log"
)

// RunDaemon 是 monitor 子进程的主入口，由 `agent monitor --daemon` 调起。
// workDir 为 agent 工作目录（可执行文件所在目录）。
//
// 流程：
//  1. 初始化独立日志 devopsMonitor.log（保留 1 天）
//  2. 加载 agent 配置（gateway / 鉴权），失败重试
//  3. 启动父进程存活看护（stdin 管道 EOF 检测）
//  4. 进入 Collect 常驻采集循环（阻塞，直到进程被 kill）
func RunDaemon(workDir string, isDebug bool) error {
	logFilePath := filepath.Join(systemutil.GetLogDir(), MonitorLogName)
	// 只保留 1 天：MaxAge=1、MaxBackups=1。配合 Init 内部启动的
	// DoDailySplitLog 每日 rotate。
	if err := logs.InitWithRotate(logFilePath, isDebug, false, 1, 1); err != nil {
		return err
	}
	defer logs.Close()

	logs.Info("monitor daemon start")
	logs.Info("pid: ", os.Getpid())
	logs.Info("ppid: ", os.Getppid())
	logs.Info("workDir: ", workDir)

	// 加载配置（gateway / 鉴权）。失败重试而非退出：主 agent 刚启动时
	// .agent.properties 一定已存在（父进程先于本子进程启动），但网络证书
	// 等初始化偶发失败时给几次机会。配置后续变更由父进程重启本子进程刷新，
	// 因此这里只需加载一次。
	loadConfigWithRetry()

	// 父进程存活看护：读 stdin 管道，父进程退出后写端关闭、读到 EOF 即自退，
	// 防止遗留孤儿采集进程。
	go watchParentPipe()

	// 进入常驻采集循环（阻塞）。Collect 内部自带 panic 自愈与退避。
	Collect()
	return nil
}

// loadConfigWithRetry 反复尝试加载 agent 配置直到成功。
func loadConfigWithRetry() {
	for {
		if err := config.LoadAgentConfig(); err != nil {
			logs.WithError(err).Warn("monitor daemon|load agent config failed, retry in 5s")
			time.Sleep(5 * time.Second)
			continue
		}
		// 加载 IP / hostname 等运行时信息，供上报 header 与 global tag 使用。
		config.LoadAgentEnv()
		logs.Info("monitor daemon|agent config loaded, gateway: ", config.GAgentConfig.Gateway)
		return
	}
}

// watchParentPipe 通过父子 stdin 管道检测父进程存活。父进程（supervisor）在
// spawn 时持有本进程 stdin 的写端并一直保持打开；父进程一旦退出（正常或崩溃），
// OS 关闭该写端，本处的 Read 立即返回 EOF/错误，据此主动退出，避免变成长期
// 占用资源的孤儿采集进程。
//
// 相比轮询 ppid==1：无轮询延迟、跨平台一致、且不受容器内 init 非 pid=1 影响。
func watchParentPipe() {
	buf := make([]byte, 1)
	for {
		// 正常情况下父进程不会向管道写数据；这里会一直阻塞在 Read。
		// 父进程退出后 Read 返回 EOF（或错误），随即退出。
		if _, err := os.Stdin.Read(buf); err != nil {
			logs.WithError(err).Warn("monitor daemon|parent pipe closed, exiting")
			logs.Close()
			os.Exit(0)
		}
	}
}
