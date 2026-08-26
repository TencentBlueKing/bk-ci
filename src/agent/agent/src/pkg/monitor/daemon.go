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

const (
	// MonitorLogName monitor 子进程的独立日志文件名。与主日志 devopsAgent.log
	// 隔离，且只保留 1 天（见 RunDaemon 里的 InitWithRotate 参数）。
	MonitorLogName = "devopsMonitor.log"
	// parentWatchInterval 轮询父进程存活的间隔。父进程若异常退出（未来得及
	// kill 子进程），子进程会被 init(pid=1) 收养；探测到即自退，避免变成
	// 长期占用资源的孤儿采集进程。
	parentWatchInterval = 10 * time.Second
	// configReloadInterval 周期性重载 agent 配置的间隔。gateway / 鉴权信息
	// 变更后需要刷新，否则上报会持续失败（原本靠父进程 EBus 通知，子进程
	// 没有该通道，改为定时自查）。
	configReloadInterval = 5 * time.Minute
)

// RunDaemon 是 monitor 子进程的主入口，由 `agent monitor --daemon` 调起。
// workDir 为 agent 工作目录（可执行文件所在目录）。
//
// 流程：
//  1. 初始化独立日志 devopsMonitor.log（保留 1 天）
//  2. 加载 agent 配置（gateway / 鉴权），失败重试
//  3. 启动父进程存活看护 + 周期性配置重载
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
	// 等初始化偶发失败时给几次机会。
	loadConfigWithRetry()

	// 父进程存活看护：父死则自退，防止孤儿。
	go watchParent()

	// 周期性重载配置，替代父进程 EBus 通知。
	go reloadConfigLoop()

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

// reloadConfigLoop 周期性重载配置，保证 gateway / 鉴权信息变更后仍能上报。
func reloadConfigLoop() {
	ticker := time.NewTicker(configReloadInterval)
	defer ticker.Stop()
	for range ticker.C {
		if err := config.LoadAgentConfig(); err != nil {
			logs.WithError(err).Warn("monitor daemon|reload agent config failed")
		}
	}
}

// watchParent 轮询父进程 pid。父进程退出后，本子进程会被 init(pid=1) 收养，
// 此时 os.Getppid() 返回 1，探测到即主动退出，避免遗留孤儿采集进程。
//
// 说明：windows 上没有 pid=1 的 init 语义，Getppid 行为不同；但主 agent
// 退出时 supervise_win.go 会显式 kill 子进程，故 windows 下本看护为兜底。
func watchParent() {
	ticker := time.NewTicker(parentWatchInterval)
	defer ticker.Stop()
	for range ticker.C {
		if os.Getppid() == 1 {
			logs.Warn("monitor daemon|parent process gone (ppid=1), exiting")
			logs.Close()
			os.Exit(0)
		}
	}
}
