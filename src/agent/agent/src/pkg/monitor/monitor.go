//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
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
	// eBusID IP 事件订阅者标识，与 collector 包互不冲突。
	eBusID = "Monitor"
	// restartBackoff 主循环 panic 后重进之前的退避，避免崩溃风暴刷爆日志。
	// 正常路径（IP 变更）走过这里时也会等一下，保持行为一致。
	restartBackoff = 10 * time.Second
)

// perInputTimeout 单个 input 的 Gather 超时。对齐 gopsutil 内部 shell-out
// 命令超时（internal/common.Timeout = 3s）与 telegraf 的预期：正常采集应在
// 毫秒级完成，卡到秒级即视为异常。透传到 gopsutil 的 *WithContext API 后，
// 能打断"已 exec 成功但在跑"的外部命令（netstat/lsof/ps）。对无法被
// context 打断的内核阻塞，由 inputRunner 的不重入状态兜底：该 input 保持
// running，后续轮次持续跳过，直到原 Gather 真正返回。
//
// 用 var 而非 const 便于单测调小以加速。
var perInputTimeout = 3 * time.Second

// Collect 是 monitor 主循环入口，应由 Agent 启动流程用 safeGo 包装调起。
//
// 异常自愈：每一轮 runGatherLoop 的 panic 都被捕获；并在 restartBackoff
// 后重新进入主循环，不让采集因为一次偶发崩溃而永久哑火。IP 变更会触发
// ctx 取消来让 runGatherLoop 退出、主循环重新起新的一轮。
func Collect() {
	logs.Info("monitor|start")

	ipChan := config.EBus.Subscribe(config.IpEvent, eBusID, 1)
	defer config.EBus.Unsubscribe(config.IpEvent, eBusID)

	dumper := NewMonitorDumper(systemutil.GetWorkDir())
	defer func() {
		_ = dumper.Close()
	}()

	reporter := NewReporter()
	// runners 在 Collect 的整个生命周期内复用。即使 IP 变化或某轮 panic 导致
	// gather loop 重启，仍保留每个 input 的 running 状态，避免重新进入一个
	// 已经卡住的 Gather。
	runners := newInputRunners(newDefaultInputs())

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
			runGatherLoop(ctx, runners, reporter, dumper)
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
func runGatherLoop(ctx context.Context, runners []*inputRunner, reporter *Reporter, dumper *Dumper) {
	// 立即采集一次（CPU 首次采样会返回空，正常；后续 ticker 会补齐）
	doOneGather(ctx, runners, reporter, dumper)

	ticker := time.NewTicker(gatherInterval)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			logs.Info("monitor|gather loop exit: ctx done")
			return
		case <-ticker.C:
			doOneGather(ctx, runners, reporter, dumper)
		}
	}
}

// doOneGather 并发调所有 input 的 Gather，聚合后 rename -> dump -> report。
// 单个 input 的 error / panic 只记录日志，不阻断其他 input。
//
// 每个 inputRunner 通过 CAS 保证同一时刻最多执行一次 Gather。某个 Gather
// 超时后仍不返回时，当前轮不再等待；后续轮次只跳过它，健康 input 照常采集。
func doOneGather(ctx context.Context, runners []*inputRunner, reporter *Reporter, dumper *Dumper) {
	gatherCtx, cancel := context.WithTimeout(ctx, perInputTimeout)
	defer cancel()

	// 每个 runner 每轮最多发送一个结果，所以容量等于 runner 数足以容纳轮次
	// 返回后的迟到结果，不会把已完成的 Gather 卡在 channel send 上。
	resCh := make(chan inputResult, len(runners))
	started := 0
	for _, runner := range runners {
		if !runner.start(gatherCtx, resCh) {
			logs.Warnf("monitor|input %s previous collection still running; scheduled collection skipped", runner.input.Name())
			continue
		}
		started++
	}

	all := make([]Metric, 0, 64)
	received := 0
collect:
	for received < started {
		select {
		case result := <-resCh:
			received++
			if result.panicValue != nil {
				logs.Errorf("monitor|input %s panic: %v", result.name, result.panicValue)
				continue
			}
			if result.err != nil {
				logs.WithError(result.err).Warnf("monitor|input %s gather failed", result.name)
				continue
			}
			if len(result.metrics) > 0 {
				all = append(all, result.metrics...)
			}
		case <-gatherCtx.Done():
			logs.Warnf("monitor|gather deadline reached after %s, proceeding with %d/%d started inputs; unfinished inputs will not be started again",
				perInputTimeout, received, started)
			break collect
		}
	}

	if len(all) == 0 {
		logs.Warn("monitor|no metrics gathered this round")
		return
	}

	// rename 后注入 global tags，再写 debug dump + 上报。
	// global tags 在 rename 之后立即注入，保证 dump / cli / report
	// 看到的 metric 完全一致，便于排查。
	renamed := Rename(all)
	injectGlobalTags(renamed)
	dumper.Dump(renamed)

	if err := reporter.Report(ctx, renamed); err != nil {
		logs.WithError(err).Warn("monitor|report failed")
	}
}
