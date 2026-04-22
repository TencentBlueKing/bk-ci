//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"sort"
	"sync"
	"time"
)

// cli_runner.go 提供供 agentcli 直接调用的"一次性采集并打印"入口，用于在
// 机器上人工排查指标是否准确。与 Collect() 使用同一套 input / rename 流水线，
// 但不走 reporter 上报，结果按 InfluxDB line protocol 打印到 out。
//
// 设计取舍：
//  - 不走 reporter：不需要 Gateway 配置就能用，方便在未 init config 的环境跑
//  - 不走 Dumper：本命令就是为了看输出，没必要再落盘
//  - 允许传入 writer 供单测替换 os.Stdout

// RunOnceStdout 执行一次完整的 Gather -> Rename -> 打印 流程。
//
// 为了让 CPU 这类需要相邻两次采样差分才能产出 metric 的 input 也能输出数据，
// 方法内会先做一次 warmup Gather（忽略结果），再间隔 warmupGap 后做正式采集。
//
// 返回值是本次输出的 metric 总数，方便调用方打印摘要。
func RunOnceStdout(ctx context.Context, out io.Writer, duration time.Duration) (int, error) {
	return runOnceWithInputs(ctx, out, newDefaultInputs(), duration)
}

// runOnceWithInputs 提取出来便于测试注入短 warmup。
func runOnceWithInputs(ctx context.Context, out io.Writer, ins []Input, warmupGap time.Duration) (int, error) {
	if out == nil {
		out = os.Stdout
	}

	// 1. warmup：让 CPU 采集器填充 "last" 缓存
	gatherAll(ctx, ins)
	if warmupGap > 0 {
		select {
		case <-ctx.Done():
			return 0, ctx.Err()
		case <-time.After(warmupGap):
		}
	}

	// 2. 正式采集
	all := gatherAll(ctx, ins)
	if len(all) == 0 {
		fmt.Fprintln(out, "[monitor] no metrics gathered")
		return 0, nil
	}

	renamed := Rename(all)

	// 3. 按 measurement 排序输出，与 telegraf 输出一致
	sort.SliceStable(renamed, func(i, j int) bool {
		if renamed[i].Name != renamed[j].Name {
			return renamed[i].Name < renamed[j].Name
		}
		return tagString(renamed[i].Tags) < tagString(renamed[j].Tags)
	})

	fmt.Fprintf(out, "# source=monitor, metrics=%d\n", len(renamed))
	for _, m := range renamed {
		fmt.Fprintln(out, formatMetricLine(m))
	}
	return len(renamed), nil
}

// gatherAll 与 doOneGather 结构一致但不做上报 / dump，便于 CLI 复用。
func gatherAll(ctx context.Context, ins []Input) []Metric {
	var wg sync.WaitGroup
	var mu sync.Mutex
	all := make([]Metric, 0, 64)
	for _, in := range ins {
		in := in
		wg.Add(1)
		go func() {
			defer wg.Done()
			defer func() {
				// cli 路径下 panic 直接打印到 stderr，不抢占 stdout
				if r := recover(); r != nil {
					fmt.Fprintf(os.Stderr, "[monitor] input %s panic: %v\n", in.Name(), r)
				}
			}()
			metrics, err := in.Gather()
			if err != nil {
				fmt.Fprintf(os.Stderr, "[monitor] input %s gather failed: %v\n", in.Name(), err)
				return
			}
			mu.Lock()
			all = append(all, metrics...)
			mu.Unlock()
		}()
	}
	done := make(chan struct{})
	go func() {
		wg.Wait()
		close(done)
	}()
	select {
	case <-done:
	case <-ctx.Done():
	}
	return all
}

// formatMetricLine 把 Metric 打印成 line protocol。复用 reporter 里的
// encodeLineProtocol 会带上多余换行；这里定制一行版本，更适合终端阅读。
func formatMetricLine(m Metric) string {
	r := &Reporter{nowFn: time.Now}
	raw := r.encodeLineProtocol([]Metric{m})
	// encodeLineProtocol 每条以 \n 结尾，去掉以便上层 Println 控制
	s := string(raw)
	if len(s) > 0 && s[len(s)-1] == '\n' {
		s = s[:len(s)-1]
	}
	return s
}

// tagString 输出稳定的 tag key=value 串，仅用于 metric 排序。
func tagString(tags map[string]string) string {
	if len(tags) == 0 {
		return ""
	}
	keys := make([]string, 0, len(tags))
	for k := range tags {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	buf, _ := json.Marshal(map[string]string{
		"__keys": fmt.Sprint(keys),
	})
	return string(buf)
}
