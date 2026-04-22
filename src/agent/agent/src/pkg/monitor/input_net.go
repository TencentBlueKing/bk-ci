//go:build !loong64
// +build !loong64

package monitor

import (
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v3/net"
)

// Net 对齐 telegraf plugins/inputs/net。每个网络接口一条 metric，
// 不含 lo / docker* / veth* 等虚接口（与 telegraf 默认行为一致：不过滤，
// 但后端一般靠 projectId 汇总；这里保留全部接口不做过滤，留给后端配置）。
//
// gopsutil IOCounters(true) 返回 pernic=true。
type Net struct {
	ioCountersFn func(pernic bool) ([]net.IOCountersStat, error)
	nowFn        func() time.Time
}

// NewNet 返回默认 net 采集器。
func NewNet() *Net {
	return &Net{
		ioCountersFn: net.IOCounters,
		nowFn:        time.Now,
	}
}

// Name 返回 "net"。
func (n *Net) Name() string { return MeasurementNet }

// Gather 遍历所有 NIC，过滤掉 all 伪接口（gopsutil 在 pernic=true 时也会
// 返回一个 "all" 汇总条目，telegraf 源码中也会过滤）。
func (n *Net) Gather() ([]Metric, error) {
	stats, err := n.ioCountersFn(true)
	if err != nil {
		return nil, errors.Wrap(err, "net: IOCounters failed")
	}
	now := n.nowFn()
	out := make([]Metric, 0, len(stats))
	for _, s := range stats {
		// 丢弃 gopsutil 生成的 "all" 汇总（仅当 Name 恰为 "all" 时；
		// 某些网卡名可能包含 all 子串，不应误伤）
		if strings.EqualFold(s.Name, "all") {
			continue
		}
		out = append(out, Metric{
			Name: MeasurementNet,
			Tags: map[string]string{TagInterface: s.Name},
			Fields: map[string]interface{}{
				FieldBytesSent:   s.BytesSent,
				FieldBytesRecv:   s.BytesRecv,
				FieldPacketsSent: s.PacketsSent,
				FieldPacketsRecv: s.PacketsRecv,
				FieldErrIn:       s.Errin,
				FieldErrOut:      s.Errout,
				FieldDropIn:      s.Dropin,
				FieldDropOut:     s.Dropout,
			},
			Timestamp: now,
		})
	}
	return out, nil
}
