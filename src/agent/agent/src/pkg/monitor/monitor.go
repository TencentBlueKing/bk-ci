//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
	"sync/atomic"
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
	// restartBackoff 主循环 panic 后重进之前的退避，避免崩溃风暴刷爆日志。
	// 正常路径（IP 变更）走过这里时也会等一下，保持行为一致。
	restartBackoff = 10 * time.Second
	// inflightHardCap 上限保护：正在 pending 的 input goroutine 总数
	// 超过该值时跳过当轮，只记日志。防止某个 input 长期卡死导致
	// goroutine 持续累积。典型值：input 数 × 并发轮数裕度。
	inflightHardCap = 64
)

// inflightInputs 跨轮累计的"未完成 input goroutine"计数。每个 input goroutine
// 起时 +1、返回时 -1。若 input 因底层 syscall 卡死而永不返回，该计数
// 会持续增长；doOneGather 发现超出 inflightHardCap 时直接跳过本轮，
// 等待卡死的 goroutine 自然完成或进程退出。
var inflightInputs atomic.Int32

// Collect 是 monitor 主循环入口，应由 Agent 启动流程用 safeGo 包装调起。
//
// 配置 MonitorOn=false 时立即返回。
//
// 异常自愈：每一轮 runGatherLoop 的 panic 都被捕获；并在 restartBackoff
// 后重新进入主循环，不让采集因为一次偶发崩溃而永久哑火。IP 变更会触发
// ctx 取消来让 runGatherLoop 退出、主循环重新起新的一轮。
func Collect() {
	if !config.GAgentConfig.MonitorOn {
		logs.Info("monitor|disabled by MonitorOn=false")
		return
	}
	logs.Info("monitor|start")

	ipChan := config.EBus.Subscribe(config.IpEvent, eBusID, 1)
	defer config.EBus.Unsubscribe(config.IpEvent, eBusID)

	dumper := NewMonitorDumper(systemutil.GetWorkDir())
	defer func() {
		_ = dumper.Close()
	}()

	reporter := NewReporter()
	ins := newDefaultInputs()

	for {
		// 每一轮都在 closure 里跑，保证 panic 只杀掉当前一轮、不会让整个
		// Collect 函数退出。恢复后 sleep restartBackoff 再进入下一轮，
		// 避免崩溃风暴。
		func() {
			defer func() {
				if r := recover(); r != nil {
					logs.Errorf("monitor|round panic recovered: %v", r)
				}
			}()
			ctx, cancel := context.WithCancel(context.Background())
			defer cancel()
			go func() {
				select {
				case ipData := <-ipChan.DChan:
					logs.Infof("monitor|ip change, restart: %s", ipData.Data)
					cancel()
				case <-ctx.Done():
				}
			}()
			runGatherLoop(ctx, ins, reporter, dumper)
		}()
		// 轮次间隔：正常退出（IP 变更）几乎立即重进；panic 后给点缓冲
		time.Sleep(restartBackoff)
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
// 单个 input 的 error / panic 只记录日志，不阻断其他 input。
//
// goroutine 泄漏保护：
//   - 每个 input goroutine 进入时对 inflightInputs +1、退出时 -1。某个
//     input 若因底层 syscall 永久卡死，该计数会持续增长；下一轮检查发现
//     超出 inflightHardCap 时跳过整轮，防止卡死的 input 被一遍遍触发新
//     goroutine 累积。
//   - wg.Wait 的 watcher goroutine 在本轮返回前不会新产生：收集结果走一个
//     缓冲 channel，超时路径直接读已到的部分，剩下的 input goroutine 继续
//     后台跑完后把结果发到同一 channel（channel 容量 = len(ins) 确保不阻塞），
//     不再额外新建"等待者"。
func doOneGather(ctx context.Context, ins []Input, reporter *Reporter, dumper *Dumper) {
	if n := inflightInputs.Load(); n >= inflightHardCap {
		logs.Warnf("monitor|skip round: inflight input goroutines=%d >= cap=%d (some inputs may be stuck)", n, inflightHardCap)
		return
	}

	gatherCtx, cancel := context.WithTimeout(ctx, gatherTimeout)
	defer cancel()

	// 缓冲足够大，保证超时后仍在跑的 goroutine 能把结果塞进来而不阻塞
	// （它们的 send 不会因 receiver 走开而 block；本方法返回后 channel
	// 被 GC，只要没人再读，发送端继续 runtime 不会泄漏 — 唯一的"尾巴"
	// 是 input goroutine 本身，它必然会走完）。
	resCh := make(chan []Metric, len(ins))

	for _, in := range ins {
		in := in
		inflightInputs.Add(1)
		go func() {
			defer inflightInputs.Add(-1)
			defer func() {
				if r := recover(); r != nil {
					logs.Errorf("monitor|input %s panic: %v", in.Name(), r)
					// 非阻塞 send 保证 panic 路径也能尝试喂结果（空切片）
					select {
					case resCh <- nil:
					default:
					}
				}
			}()
			metrics, err := in.Gather()
			if err != nil {
				logs.WithError(err).Warnf("monitor|input %s gather failed", in.Name())
				resCh <- nil
				return
			}
			resCh <- metrics
		}()
	}

	all := make([]Metric, 0, 64)
	received := 0
collect:
	for received < len(ins) {
		select {
		case m := <-resCh:
			received++
			if len(m) > 0 {
				all = append(all, m...)
			}
		case <-gatherCtx.Done():
			logs.Warnf("monitor|gather timed out after %s, proceeding with %d/%d inputs",
				gatherTimeout, received, len(ins))
			break collect
		}
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
