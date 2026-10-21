package agentcli

import (
	"context"
	"flag"
	"fmt"
	"os"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/monitor"
)

// handleMonitor 运行一次 monitor 采集链路。
//
//	devopsAgent monitor            # 一次性采集，把指标打到 stdout，供人工排查
//	devopsAgent monitor -d 5s      # collector 采样时长改为 5s
func handleMonitor(workDir string, args []string) error {
	_ = workDir
	fs := flag.NewFlagSet("monitor", flag.ContinueOnError)
	duration := fs.Duration("d", 1*time.Second, "collector(telegraf) 采样时长，建议 ≥ 2s 保证 cpu 有 delta")
	if err := fs.Parse(args); err != nil {
		return err
	}

	n, err := monitor.RunOnceStdout(context.Background(), os.Stdout, *duration)
	if err != nil {
		fmt.Fprintf(os.Stderr, "[monitor] run failed: %v\n", err)
		return err
	}
	fmt.Fprintf(os.Stdout, "# monitor metrics emitted: %d\n", n)
	return nil
}
