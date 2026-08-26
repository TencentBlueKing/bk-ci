package agentcli

import (
	"context"
	"flag"
	"fmt"
	"os"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/monitor"
)

// handleMonitor 运行 monitor 采集链路。
//
// 两种模式：
//
//	devopsAgent monitor            # 一次性采集，把指标打到 stdout，供人工排查
//	devopsAgent monitor -d 5s      # collector 采样时长改为 5s
//	devopsAgent monitor --daemon   # 常驻子进程模式，由主 agent 拉起，
//	                               # 日志写 logs/devopsMonitor.log
//
// --daemon 模式是 monitor 进程隔离的核心：主 agent 通过 monitor.Supervise
// 以子进程方式拉起本命令，采集卡死/崩溃只影响该子进程，不干扰主链路。
func handleMonitor(workDir string, args []string) error {
	fs := flag.NewFlagSet("monitor", flag.ContinueOnError)
	duration := fs.Duration("d", 1*time.Second, "collector(telegraf) 采样时长，建议 ≥ 2s 保证 cpu 有 delta")
	daemon := fs.Bool("daemon", false, "常驻子进程模式，由主 agent 拉起，日志写 devopsMonitor.log")
	if err := fs.Parse(args); err != nil {
		return err
	}

	if *daemon {
		// 常驻模式：自初始化独立日志与配置，进入 Collect 循环（阻塞）。
		return monitor.RunDaemon(workDir, DebugFileExists(workDir))
	}

	n, err := monitor.RunOnceStdout(context.Background(), os.Stdout, *duration)
	if err != nil {
		fmt.Fprintf(os.Stderr, "[monitor] run failed: %v\n", err)
		return err
	}
	fmt.Fprintf(os.Stdout, "# monitor metrics emitted: %d\n", n)
	return nil
}
