//go:build windows
// +build windows

package monitor

import "time"

// rate_windows.go 提供 out+windows 构建下的速率换算工具。
//
// 背景：PDH win_perf_counters 的 *_persec 是"即时速率"，分母约 1s；若
// 我们用 monitor gatherInterval (60s) 做分母会得到 60s 均值，瞬时毛刺被
// 稀释，告警阈值与 telegraf 时代的历史曲线对不上。所以 input 层采用
// twin-sample：一次 Gather 内连采两次累计值、中间 sleep rateSampleInterval、
// 用相邻两次累计值的差值除以实际经过的时间得到瞬时速率（见
// input_diskio_windows_out.go / input_net_windows_out.go）。
//
// 本文件仅提供纯函数 + 常量，不持有状态，因此无需 mutex。

// rateSampleInterval 是 twin-sample 两次累计采样之间的间隔。
// 对齐 telegraf 默认 CounterRefreshInterval 和 PDH 默认 SampleInterval。
// 单元测试注入 sleepFn 可绕过真实 sleep。
const rateSampleInterval = time.Second

// computeRate 返回 (cur - prev) / dtSec。
//
// 返回 ok=false 的情形：
//   - dtSec <= 0：时钟异常 / 两次采样时间戳相同
//   - cur < prev：counter reset（磁盘热插拔 / 网卡重启 / 不可能的 uint64 wrap）
//
// 正常返回 float64 速率（单位与累计字段一致，除以秒）。
func computeRate(cur, prev uint64, dtSec float64) (float64, bool) {
	if dtSec <= 0 {
		return 0, false
	}
	if cur < prev {
		return 0, false
	}
	return float64(cur-prev) / dtSec, true
}
