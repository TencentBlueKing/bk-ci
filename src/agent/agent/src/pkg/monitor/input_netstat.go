//go:build !loong64
// +build !loong64

package monitor

import (
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v4/net"
)

// Netstat 对齐 telegraf plugins/inputs/netstat。
// 遍历所有 TCP/UDP 连接，按状态分桶计数，输出一条聚合 metric。
//
// 状态名对齐 Linux /proc/net/tcp 协议栈：ESTABLISHED / SYN_SENT / SYN_RECV /
// FIN_WAIT1 / FIN_WAIT2 / TIME_WAIT / CLOSE / CLOSE_WAIT / LAST_ACK /
// LISTEN / CLOSING / NONE，field 名在 names.go 中定义。
type Netstat struct {
	connsFn func(kind string) ([]net.ConnectionStat, error)
	nowFn   func() time.Time
}

// NewNetstat 返回默认 netstat 采集器。
// gopsutil ConnectionsWithoutUids 在某些平台有性能优势，但 "all" 已经
// 覆盖 tcp/udp 全部，简单起见直接用 Connections("all")。
func NewNetstat() *Netstat {
	return &Netstat{
		connsFn: net.Connections,
		nowFn:   time.Now,
	}
}

// Name 返回 "netstat"。
func (n *Netstat) Name() string { return MeasurementNetstat }

// Gather 统计每种状态的连接数。
func (n *Netstat) Gather() ([]Metric, error) {
	conns, err := n.connsFn("all")
	if err != nil {
		return nil, errors.Wrap(err, "netstat: Connections failed")
	}

	var (
		tcpEstablished int64
		tcpSynSent     int64
		tcpSynRecv     int64
		tcpFinWait1    int64
		tcpFinWait2    int64
		tcpTimeWait    int64
		tcpClose       int64
		tcpCloseWait   int64
		tcpLastAck     int64
		tcpListen      int64
		tcpClosing     int64
		tcpNone        int64
		udpSocket      int64
	)
	for _, c := range conns {
		// gopsutil 的 Type: 1=TCP, 2=UDP（对齐 syscall.SOCK_STREAM / SOCK_DGRAM）
		if c.Type == 2 {
			udpSocket++
			continue
		}
		switch strings.ToUpper(c.Status) {
		case "ESTABLISHED":
			tcpEstablished++
		case "SYN_SENT":
			tcpSynSent++
		case "SYN_RECV":
			tcpSynRecv++
		case "FIN_WAIT1", "FIN_WAIT_1":
			tcpFinWait1++
		case "FIN_WAIT2", "FIN_WAIT_2":
			tcpFinWait2++
		case "TIME_WAIT":
			tcpTimeWait++
		case "CLOSE":
			tcpClose++
		case "CLOSE_WAIT":
			tcpCloseWait++
		case "LAST_ACK":
			tcpLastAck++
		case "LISTEN":
			tcpListen++
		case "CLOSING":
			tcpClosing++
		case "NONE", "":
			tcpNone++
		}
	}

	return []Metric{{
		Name: MeasurementNetstat,
		Fields: map[string]interface{}{
			RenamedFieldCurTCPEstab:     tcpEstablished,
			RenamedFieldCurTCPSynSent:   tcpSynSent,
			RenamedFieldCurTCPSynRecv:   tcpSynRecv,
			RenamedFieldCurTCPFinWait1:  tcpFinWait1,
			RenamedFieldCurTCPFinWait2:  tcpFinWait2,
			RenamedFieldCurTCPTimeWait:  tcpTimeWait,
			RenamedFieldCurTCPClosed:    tcpClose,
			RenamedFieldCurTCPCloseWait: tcpCloseWait,
			RenamedFieldCurTCPLastAck:   tcpLastAck,
			RenamedFieldCurTCPListen:    tcpListen,
			RenamedFieldCurTCPClosing:   tcpClosing,
			FieldTCPNone:                tcpNone,
			FieldUDPSocket:              udpSocket,
		},
		Timestamp: n.nowFn(),
	}}, nil
}
