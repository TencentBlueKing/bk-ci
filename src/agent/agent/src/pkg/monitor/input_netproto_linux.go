//go:build linux && !loong64
// +build linux,!loong64

package monitor

import (
	"strings"
	"time"

	gopsutilnet "github.com/shirou/gopsutil/v3/net"
)

// input_netproto_linux.go 对齐 telegraf plugins/inputs/net 在
// IgnoreProtocolStats=false 下产出的 net,interface=all metric：
// 读取 /proc/net/snmp（由 gopsutil net.ProtoCounters 封装），把
// icmp / icmpmsg / ip / tcp / udp / udplite 六个协议的所有字段
// 扁平化为 <proto>_<stat>，合并到单条 metric。
//
// 不在 input_net.go 中合并实现的原因：
//  - input_net.go 是跨平台（!windows）文件，NetProto 仅 Linux 有意义
//  - 职责分离：input_net.go 产出每网卡的 metric；本文件只产出 all 汇总条
//  - 失败降级策略不同：NetProto 失败时应静默（telegraf 源码里也是 _, _ =）
//    而 IOCounters 失败应传播

// NetProto 对齐 telegraf inputs.net 的协议汇总分支。
type NetProto struct {
	// protoCountersFn 为 gopsutil net.ProtoCounters 的注入点，便于测试。
	// 传 nil 给 protocols 参数等价于采所有支持的协议。
	protoCountersFn func(protocols []string) ([]gopsutilnet.ProtoCountersStat, error)
	nowFn           func() time.Time
}

// NewNetProto 返回默认 NetProto 采集器。
func NewNetProto() *NetProto {
	return &NetProto{
		protoCountersFn: gopsutilnet.ProtoCounters,
		nowFn:           time.Now,
	}
}

// Name 返回 "net"——与 input_net.go 的 measurement 相同；后端靠 tag
// interface='all' 区分本条 metric 与每网卡 metric。
func (np *NetProto) Name() string { return MeasurementNet }

// Gather 读 /proc/net/snmp 并平铺为单条 interface=all metric。
//
// 任何错误（文件不可读、解析失败）都吞掉返回 nil, nil——对齐 telegraf
// 源码 `netprotos, _ := n.ps.NetProto()` 的忽略策略，避免因 snmp 不可用
// 而阻断整个 monitor gather。
func (np *NetProto) Gather() ([]Metric, error) {
	stats, err := np.protoCountersFn(nil)
	if err != nil || len(stats) == 0 {
		return nil, nil
	}

	fields := make(map[string]interface{})
	for _, proto := range stats {
		// gopsutil 在 Linux 上按 /proc/net/snmp 表头原样返回 Protocol 名
		// ("Ip","Icmp","IcmpMsg","Tcp","Udp","UdpLite"）以及 stat key
		// ("InReceives","InEchoReps",...)。为与 telegraf 一致全部小写。
		protoLower := strings.ToLower(proto.Protocol)
		for stat, v := range proto.Stats {
			statLower := strings.ToLower(stat)
			fields[protoLower+"_"+statLower] = v
		}
	}
	if len(fields) == 0 {
		return nil, nil
	}

	return []Metric{{
		Name:      MeasurementNet,
		Tags:      map[string]string{TagInterface: "all"},
		Fields:    fields,
		Timestamp: np.nowFn(),
	}}, nil
}

// init 注册 NetProto 到 monitor 默认 input 列表（仅 Linux 构建生效）。
func init() {
	platformExtraInputsFn = func() []Input {
		return []Input{NewNetProto()}
	}
}
