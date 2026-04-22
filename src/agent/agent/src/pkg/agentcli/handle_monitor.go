package agentcli

import (
	"context"
	"flag"
	"fmt"
	"os"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/monitor"
)

// handleMonitor 运行一次 collector(telegraf) 与 monitor 两条采集链路，
// 把各自的指标打到 stdout，用于在机器上人工验证两者输出是否对齐。
//
// 行为与 agent 正常运行时完全一致（同一套 inputs / rename），只是改成
// 跑一次即退出，且不走 reporter 上报、不写 dump 日志。
//
// 用法：
//
//	devopsAgent monitor            # 默认 collector 采样 3s, monitor 输出当前
//	devopsAgent monitor -d 5s      # collector 采样时长改为 5s
//	devopsAgent monitor -only=monitor    # 仅跑 monitor
//	devopsAgent monitor -only=collector  # 仅跑 collector
func handleMonitor(workDir string, args []string) error {
	_ = workDir
	fs := flag.NewFlagSet("monitor", flag.ContinueOnError)
	duration := fs.Duration("d", 3*time.Second, "collector(telegraf) 采样时长，建议 ≥ 2s 保证 cpu 有 delta")
	only := fs.String("only", "", "仅执行其中一条: monitor 或 collector")
	if err := fs.Parse(args); err != nil {
		return err
	}

	runCollector := true
	runMonitor := true
	switch *only {
	case "":
	case "monitor":
		runCollector = false
	case "collector":
		runMonitor = false
	default:
		return fmt.Errorf("unknown -only value: %q (use monitor or collector)", *only)
	}

	ctx := context.Background()

	if runCollector {
		fmt.Fprintln(os.Stdout, "========== collector (telegraf) ==========")
		if err := runCollectorOnceStdout(ctx, os.Stdout, *duration); err != nil {
			// 打印错误但不终止，monitor 侧可能还能正常跑
			fmt.Fprintf(os.Stderr, "[collector] run failed: %v\n", err)
		}
		fmt.Fprintln(os.Stdout)
	}

	if runMonitor {
		fmt.Fprintln(os.Stdout, "========== monitor (gopsutil) ==========")
		n, err := monitor.RunOnceStdout(ctx, os.Stdout)
		if err != nil {
			fmt.Fprintf(os.Stderr, "[monitor] run failed: %v\n", err)
			return err
		}
		fmt.Fprintf(os.Stdout, "# monitor metrics emitted: %d\n", n)
	}
	return nil
}
