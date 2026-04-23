//go:build linux && !loong64
// +build linux,!loong64

package monitor

import (
	"errors"
	"testing"
	"time"

	gopsutilnet "github.com/shirou/gopsutil/v3/net"
)

func TestNetProto_Name(t *testing.T) {
	if n := NewNetProto().Name(); n != MeasurementNet {
		t.Errorf("Name() = %q, want %q", n, MeasurementNet)
	}
}

// TestNetProto_Gather_FlattensProtocols 覆盖 plan §2.1 主路径：
// 多协议、多字段合并成单条 interface=all metric，字段名 <proto>_<stat>。
func TestNetProto_Gather_FlattensProtocols(t *testing.T) {
	ts := time.Unix(1700000000, 0)
	np := &NetProto{
		protoCountersFn: func(protocols []string) ([]gopsutilnet.ProtoCountersStat, error) {
			// gopsutil 按 /proc/net/snmp 头部原样给出 Protocol，首字母大写
			return []gopsutilnet.ProtoCountersStat{
				{Protocol: "Icmp", Stats: map[string]int64{
					"InMsgs":  100,
					"OutMsgs": 200,
				}},
				{Protocol: "Tcp", Stats: map[string]int64{
					"ActiveOpens":  50,
					"PassiveOpens": 60,
				}},
			}, nil
		},
		nowFn: func() time.Time { return ts },
	}

	metrics, err := np.Gather()
	if err != nil {
		t.Fatalf("err = %v", err)
	}
	if len(metrics) != 1 {
		t.Fatalf("want 1 metric (single interface=all), got %d", len(metrics))
	}
	m := metrics[0]
	if m.Name != MeasurementNet {
		t.Errorf("measurement = %q, want %q", m.Name, MeasurementNet)
	}
	if m.Tags[TagInterface] != "all" {
		t.Errorf("tag interface = %q, want all", m.Tags[TagInterface])
	}
	// 字段名全小写
	wantFields := map[string]int64{
		"icmp_inmsgs":      100,
		"icmp_outmsgs":     200,
		"tcp_activeopens":  50,
		"tcp_passiveopens": 60,
	}
	for k, want := range wantFields {
		got, _ := m.Fields[k].(int64)
		if got != want {
			t.Errorf("field %s = %v, want %v", k, m.Fields[k], want)
		}
	}
	if !m.Timestamp.Equal(ts) {
		t.Errorf("timestamp = %v, want %v", m.Timestamp, ts)
	}
}

// TestNetProto_Gather_ErrorSwallowed 覆盖 plan §2.1 降级：
// 错误不阻断 monitor 整体 gather——返回 nil, nil 让调用方跳过本条 input。
func TestNetProto_Gather_ErrorSwallowed(t *testing.T) {
	np := &NetProto{
		protoCountersFn: func(_ []string) ([]gopsutilnet.ProtoCountersStat, error) {
			return nil, errors.New("cannot open /proc/net/snmp")
		},
		nowFn: time.Now,
	}
	metrics, err := np.Gather()
	if err != nil {
		t.Errorf("err should be swallowed, got %v", err)
	}
	if metrics != nil {
		t.Errorf("metrics should be nil on error, got %+v", metrics)
	}
}

// TestNetProto_Gather_EmptyInput 覆盖 /proc/net/snmp 存在但解析后协议数为 0
// （罕见：某些轻量容器或裁剪内核）——同样不上报任何 metric。
func TestNetProto_Gather_EmptyInput(t *testing.T) {
	np := &NetProto{
		protoCountersFn: func(_ []string) ([]gopsutilnet.ProtoCountersStat, error) {
			return []gopsutilnet.ProtoCountersStat{}, nil
		},
		nowFn: time.Now,
	}
	metrics, err := np.Gather()
	if err != nil || metrics != nil {
		t.Errorf("empty proto counters should yield nil,nil; got %v, %v", metrics, err)
	}
}

// TestNetProto_Gather_ProtoWithNoStats 覆盖协议存在但 Stats 为空（例如
// udplite 在某些内核）——应该不产生任何字段，此时跳过 metric。
func TestNetProto_Gather_ProtoWithNoStats(t *testing.T) {
	np := &NetProto{
		protoCountersFn: func(_ []string) ([]gopsutilnet.ProtoCountersStat, error) {
			return []gopsutilnet.ProtoCountersStat{
				{Protocol: "UdpLite", Stats: map[string]int64{}},
			}, nil
		},
		nowFn: time.Now,
	}
	metrics, err := np.Gather()
	if err != nil || metrics != nil {
		t.Errorf("empty stats should yield nil,nil; got %v, %v", metrics, err)
	}
}

// TestPlatformExtraInputsFn_LinuxRegistered 确认 init() 在 Linux 构建下
// 成功挂到 platformExtraInputsFn，newDefaultInputs 里会包含 NetProto。
func TestPlatformExtraInputsFn_LinuxRegistered(t *testing.T) {
	if platformExtraInputsFn == nil {
		t.Fatal("platformExtraInputsFn must be set on Linux by input_netproto_linux.go init()")
	}
	extras := platformExtraInputsFn()
	found := false
	for _, in := range extras {
		if _, ok := in.(*NetProto); ok {
			found = true
			break
		}
	}
	if !found {
		t.Errorf("NetProto should be registered as platform extra input on Linux; got %d extras", len(extras))
	}
}
