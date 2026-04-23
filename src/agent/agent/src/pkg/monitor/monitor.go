//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
)

// monitor.go 是 monitor 包的主循环入口。结构与 collector.Collect 对称
// （src/pkg/collector/collector.go:57）：
//
//  1. 启动时订阅 IP 变更事件（config.EBus），IP 变化时取消当前 context
//     触发一次重启，保证上报 header 带到最新的 agent IP
//  2. 每分钟触发一次 Gather，把所有 input 并发采集到的 metric 合并
//  3. Rename -> Debug Dump（可选） -> Reporter 上报
//
// 和 collector 的差异：
//  - 不再通过 template 文本配置，入参直接代码构造
//  - 上报失败会在日志中降级，不影响下次采集
//  - 支持通过 dumper 把采集结果追加写到 logs/monitor_metrics.log 便于排查

const (
	// gatherInterval 和 telegraf agent.interval = "1m" 对齐。
	gatherInterval = time.Minute
	// gatherTimeout 单轮 Gather 最长耗时；超过则强制取消，避免堆积。
	// 留足 45s 给上报（reporter 超时由全局 config.TimeoutSec 控制）。
	gatherTimeout = 45 * time.Second
	// eBusID IP 事件订阅者标识，与 collector 包互不冲突。
	eBusID = "Monitor"
)

// Collect 是 monitor 主循环入口，应由 Agent 启动流程用 safeGo 包装调起。
//
// 配置 MonitorOn=false 时立即返回。
func Collect() {
	if !config.GAgentConfig.MonitorOn {
		logs.Info("monitor|disabled by MonitorOn=false")
		return
	}
	logs.Info("monitor|start")

	ipChan := config.EBus.Subscribe(config.IpEvent, eBusID, 1)
	defer config.EBus.Unsubscribe(config.IpEvent, eBusID)

	defer func() {
		if r := recover(); r != nil {
			logs.Errorf("monitor|panic recovered: %v", r)
		}
	}()

	dumper := NewMonitorDumper(systemutil.GetWorkDir())
	defer func() {
		_ = dumper.Close()
	}()

	reporter := NewReporter()
	ins := newDefaultInputs()

	for {
		ctx, cancel := context.WithCancel(context.Background())
		go func() {
			select {
			case ipData := <-ipChan.DChan:
				logs.Infof("monitor|ip change, restart: %s", ipData.Data)
				cancel()
			case <-ctx.Done():
			}
		}()
		runGatherLoop(ctx, ins, reporter, dumper)
		cancel()
	}
}

// platformExtraInputsFn 由平台专属文件通过 init() 注入，返回该平台需要
// 额外添加的 Input（例如 Linux 的 NetProto 读 /proc/net/snmp 产出
// net,interface=all 汇总）。非该平台为 nil，跳过。
var platformExtraInputsFn func() []Input

// newDefaultInputs 返回默认 input 列表。顺序对齐 telegrafConf.go 中
// [[inputs.*]] 出现的顺序，便于 debug 时肉眼对照。
func newDefaultInputs() []Input {
	ins := []Input{
		NewCPU(),
		NewDisk(),
		NewDiskIO(),
		NewMem(),
		NewNet(),
		NewSystem(),
		NewNetstat(),
		NewSwap(),
		NewKernel(),
		NewProcesses(),
	}
	if platformExtraInputsFn != nil {
		ins = append(ins, platformExtraInputsFn()...)
	}
	return ins
}

// runGatherLoop 在 ctx 有效期间按 gatherInterval 周期采集上报。
// 首次采集不等待 ticker，以便 agent 启动后 1 分钟内就有一条指标落盘。
func runGatherLoop(ctx context.Context, ins []Input, reporter *Reporter, dumper *Dumper) {
	// 立即采集一次（CPU 首次采样会返回空，正常；后续 ticker 会补齐）
	doOneGather(ctx, ins, reporter, dumper)

	ticker := time.NewTicker(gatherInterval)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			logs.Info("monitor|gather loop exit: ctx done")
			return
		case <-ticker.C:
			doOneGather(ctx, ins, reporter, dumper)
		}
	}
}

// doOneGather 并发调所有 input 的 Gather，聚合后 rename -> dump -> report。
// 单个 input 的 error 只记录日志，不阻断其他 input。
func doOneGather(ctx context.Context, ins []Input, reporter *Reporter, dumper *Dumper) {
	gatherCtx, cancel := context.WithTimeout(ctx, gatherTimeout)
	defer cancel()

	var wg sync.WaitGroup
	var mu sync.Mutex
	all := make([]Metric, 0, 64)

	for _, in := range ins {
		in := in
		wg.Add(1)
		go func() {
			defer wg.Done()
			defer func() {
				if r := recover(); r != nil {
					logs.Errorf("monitor|input %s panic: %v", in.Name(), r)
				}
			}()
			metrics, err := in.Gather()
			if err != nil {
				logs.WithError(err).Warnf("monitor|input %s gather failed", in.Name())
				return
			}
			mu.Lock()
			all = append(all, metrics...)
			mu.Unlock()
		}()
	}

	// 等待所有 input 完成或超时
	done := make(chan struct{})
	go func() {
		wg.Wait()
		close(done)
	}()
	select {
	case <-done:
	case <-gatherCtx.Done():
		logs.Warn("monitor|gather timed out, proceeding with partial data")
	}

	if len(all) == 0 {
		logs.Warn("monitor|no metrics gathered this round")
		return
	}

	// rename 后先写 debug dump（便于排查时看到最终上报值），再上报
	renamed := Rename(all)
	dumper.Dump(renamed)

	if err := reporter.Report(ctx, renamed); err != nil {
		logs.WithError(err).Warn("monitor|report failed")
	}
}
