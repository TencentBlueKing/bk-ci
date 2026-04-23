package agentcli

import (
	"context"
	"flag"
	"fmt"
	"os"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/monitor"
)

// handleMonitor 运行一次monitor 采集链路，
// 把指标打到 stdout，用于在机器上直接查看
//
// 行为与 agent 正常运行时完全一致（同一套 inputs / rename），只是改成
// 跑一次即退出，且不走 reporter 上报、不写 dump 日志。
//
// 用法：
//
//	devopsAgent monitor            # 默认 collector 采样 3s, monitor 输出当前
//	devopsAgent monitor -d 5s      # collector 采样时长改为 5s
func handleMonitor(workDir string, args []string) error {
	_ = workDir
	fs := flag.NewFlagSet("monitor", flag.ContinueOnError)
	duration := fs.Duration("d", 1*time.Second, "collector(telegraf) 采样时长，建议 ≥ 2s 保证 cpu 有 delta")
	if err := fs.Parse(args); err != nil {
		return err
	}
	fmt.Fprintln(os.Stdout, "========== monitor (gopsutil) ==========")
	n, err := monitor.RunOnceStdout(context.Background(), os.Stdout, *duration)
	if err != nil {
		fmt.Fprintf(os.Stderr, "[monitor] run failed: %v\n", err)
		return err
	}
	fmt.Fprintf(os.Stdout, "# monitor metrics emitted: %d\n", n)
	return nil
}
