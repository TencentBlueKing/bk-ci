//go:build windows
// +build windows

package monitor

import (
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v3/net"
)

// input_net_windows_out.go 是 out+windows 专用的 Net 采集器。
//
// 与默认 Net（input_net.go）的差别、动机与 input_diskio_windows_out.go 相同：
// 速率字段在 Gather 内 twin-sample，分母对齐 PDH 的 1s 窗口。
//
// 速率字段：
//   speed_recv         ← (s2.BytesRecv   - s1.BytesRecv)   / dt
//   speed_sent         ← (s2.BytesSent   - s1.BytesSent)   / dt
//   speed_packets_recv ← (s2.PacketsRecv - s1.PacketsRecv) / dt
//   speed_packets_sent ← (s2.PacketsSent - s1.PacketsSent) / dt
// 其余字段（err_in / err_out / drop_in / drop_out）取第二次采样的累计值
// （PDH 原生也为累计）。

// Net 对齐 telegraf win_perf_counters 的 Network Interface 速率计数器。
type Net struct {
	ioCountersFn func(pernic bool) ([]net.IOCountersStat, error)
	nowFn        func() time.Time
	sleepFn      func(time.Duration) // 单测注入点
	// adapterDescFn 返回 FriendlyName→Description 映射，用于把 gopsutil
	// 的 FriendlyName（"以太网 5"）升级为 PDH instance（Description）。
	// 默认走 adapterDescriptions（带 10min 缓存）；测试注入 fake。
	// 失败或 nil 时 fallback 到 FriendlyName 作 instance。
	adapterDescFn func() (map[string]string, error)
}

// NewNet 返回默认 Net 采集器。
func NewNet() *Net {
	return &Net{
		ioCountersFn:  net.IOCounters,
		nowFn:         time.Now,
		sleepFn:       time.Sleep,
		adapterDescFn: adapterDescriptions,
	}
}

// Name 返回 "net"。
func (n *Net) Name() string { return MeasurementNet }

// Gather 执行一次 twin-sample 并返回速率字段已换算的 metric。
func (n *Net) Gather() ([]Metric, error) {
	s1, err := n.ioCountersFn(true)
	if err != nil {
		return nil, errors.Wrap(err, "net: IOCounters first sample failed")
	}
	t1 := n.nowFn()

	n.sleepFn(rateSampleInterval)

	s2, err := n.ioCountersFn(true)
	if err != nil {
		return nil, errors.Wrap(err, "net: IOCounters second sample failed")
	}
	t2 := n.nowFn()

	dt := t2.Sub(t1).Seconds()
	if dt <= 0 {
		return nil, errors.New("net: twin-sample non-positive dt")
	}

	// 查一次 FriendlyName→Description 映射（带 10min 缓存）。失败时 descMap=nil。
	var descMap map[string]string
	if n.adapterDescFn != nil {
		descMap, _ = n.adapterDescFn()
	}

	// 第一次采样建索引：按 interface name 查找
	prev := make(map[string]net.IOCountersStat, len(s1))
	for _, s := range s1 {
		prev[s.Name] = s
	}

	out := make([]Metric, 0, len(s2))
	for _, c2 := range s2 {
		if strings.EqualFold(c2.Name, "all") {
			continue
		}
		// 过滤 Loopback / vEthernet / ISATAP / Teredo / VPN 隧道 等伪接口。
		// Windows 上 gopsutil 返回的 Name 是 FriendlyName（中文系统常为
		// "以太网 3" / "本地连接 7"），并不匹配 VPN 驱动的 Description
		// （如 "NGNClient Adapter"）；这里两者都过一遍，任一命中即过滤，
		// 让 telegraf PDH 默认不采集的虚拟适配器保持对齐。
		if shouldSkipNetInterface(c2.Name) {
			continue
		}
		if desc := descMap[c2.Name]; desc != "" && shouldSkipNetInterface(desc) {
			continue
		}
		fields := map[string]interface{}{
			// 规范名（与 Linux / macOS 对齐，内部版后端主用）
			FieldErrIn:   c2.Errin,
			FieldErrOut:  c2.Errout,
			FieldDropIn:  c2.Dropin,
			FieldDropOut: c2.Dropout,
			// PDH 兼容别名（与 telegraf win_perf_counters 对齐，老 Windows 看
			// 板/告警用）。双写带来字段数量略有冗余，但确保按哪个 key 过
			// 滤都能命中——值完全相同，只是 key 名有两种。
			WinFieldPacketsReceivedErrors:    c2.Errin,
			WinFieldPacketsOutboundErrors:    c2.Errout,
			WinFieldPacketsReceivedDiscarded: c2.Dropin,
			WinFieldPacketsOutboundDiscarded: c2.Dropout,
		}
		if c1, ok := prev[c2.Name]; ok {
			if rate, ok := computeRate(c2.BytesRecv, c1.BytesRecv, dt); ok {
				fields[RenamedFieldSpeedRecv] = rate
			}
			if rate, ok := computeRate(c2.BytesSent, c1.BytesSent, dt); ok {
				fields[RenamedFieldSpeedSent] = rate
			}
			if rate, ok := computeRate(c2.PacketsRecv, c1.PacketsRecv, dt); ok {
				fields[RenamedFieldSpeedPacketsRecv] = rate
			}
			if rate, ok := computeRate(c2.PacketsSent, c1.PacketsSent, dt); ok {
				fields[RenamedFieldSpeedPacketsSent] = rate
			}
		}
		// 仅在第二次出现的接口（网卡启用）：无速率字段，仅输出第二次累计字段

		// 预填 instance：优先用 PDH Description（与 telegraf win_perf_counters
		// Network Interface instance 一致）；缺失/空字符串时 fallback 到
		// FriendlyName，由下游 normalizeWinMetricTags 的 net 分支兜底（已
		// 经尊重"existing instance"）。
		tags := map[string]string{TagInterface: c2.Name}
		if desc := descMap[c2.Name]; desc != "" {
			tags[TagInstance] = desc
		}
		out = append(out, Metric{
			Name:      MeasurementNet,
			Tags:      normalizeWinMetricTags(MeasurementNet, tags),
			Fields:    fields,
			Timestamp: t2,
		})
	}
	return out, nil
}
