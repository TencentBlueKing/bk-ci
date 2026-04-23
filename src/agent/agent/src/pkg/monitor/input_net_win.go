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
}

// NewNet 返回默认 Net 采集器。
func NewNet() *Net {
	return &Net{
		ioCountersFn: net.IOCounters,
		nowFn:        time.Now,
		sleepFn:      time.Sleep,
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
		fields := map[string]interface{}{
			FieldErrIn:   c2.Errin,
			FieldErrOut:  c2.Errout,
			FieldDropIn:  c2.Dropin,
			FieldDropOut: c2.Dropout,
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

		out = append(out, Metric{
			Name:      MeasurementNet,
			Tags:      map[string]string{TagInterface: c2.Name},
			Fields:    fields,
			Timestamp: t2,
		})
	}
	return out, nil
}
