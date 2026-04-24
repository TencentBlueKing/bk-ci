//go:build out && windows
// +build out,windows

package monitor

import (
	"strings"
)

// rename_out_windows.go（外部版 Windows / Stream 项目）。
//
// 命名反转后，input 层直接产出 BK-CI 后端规范名：
// measurement=cpu_detail / io / net / env / load、
// field=user/system/idle/iowait/speed_*/rkb_s/wkb_s/cur_tcp_* 等。
//
// 为了兼容老的 Stream 后端（历史期望 telegraf win_perf_counters 输出风格），
// 本文件做**反向翻译**：规范名 → PDH 风格名。
// 具体来说：
//   cpu_detail -> win_cpu，字段 user -> Percent_User_Time 等；
//   io         -> win_diskio，字段 rkb_s -> Disk_Read_Bytes_persec 等；
//   net        -> win_net，字段 speed_recv -> Bytes_Received_persec 等；
//   其它 measurement（env / load / mem / disk / swap / netstat / processes）
//   走 Linux 反向表（rename_out.go 不在此平台编译，这里内联等价表），
//   只改字段/measurement 名，不带 win_ 前缀。
//
// 速率/累计语义的差异由 input 层 twin-sample（见 input_diskio_windows_out.go /
// input_net_windows_out.go 与 rate_windows.go）在采集阶段解决：io / net 的
// 速率字段在进入本 rename 前已经是 float64 速率；本文件只负责字段名映射
// 与 measurement/tag 结构重塑，不再做数值换算。

// ---------------------------------------------------------------------------
// Windows 专用反向表（规范名 → PDH 名）
// ---------------------------------------------------------------------------

// measurementWinPrefix：规范 measurement → telegraf win_perf_counters 风格。
// 其它 measurement 未列出 → 走 linuxReverseMeasurement 回滚到 telegraf inputs.* 名。
var measurementWinPrefix = map[string]string{
	RenamedCPUDetail: "win_cpu",
	RenamedIO:        "win_diskio",
	MeasurementNet:   "win_net",
}

// objectNameForMeasurement：PDH ObjectName tag，仅对三个 win_* measurement 设置。
// key 用规范 measurement 名（翻译前）。
var objectNameForMeasurement = map[string]string{
	RenamedCPUDetail: "Processor",
	RenamedIO:        "PhysicalDisk",
	MeasurementNet:   "Network Interface",
}

// cpuFieldPDH：cpu_detail 规范字段 → PDH 名。
var cpuFieldPDH = map[string]string{
	RenamedFieldUser:   "Percent_User_Time",
	RenamedFieldSystem: "Percent_Privileged_Time",
	RenamedFieldIdle:   "Percent_Idle_Time",
	FieldUsageIrq:      "Percent_Interrupt_Time",
	FieldUsageSoftirq:  "Percent_DPC_Time",
}

// cpuFieldsDrop：Windows PDH 没有这些字段（Linux 风格的 nice/iowait/guest/steal
// 在 Windows 上无意义），out+windows 下丢弃以贴合 telegraf win_perf_counters
// 的实际输出。
var cpuFieldsDrop = map[string]struct{}{
	FieldUsageNice:      {},
	FieldUsageGuest:     {},
	FieldUsageGuestNice: {},
	RenamedFieldIowait:  {},
	FieldUsageSteal:     {},
}

// netFieldPDH：net 规范字段 → PDH 名。
// 速率 4 项由 input 层 twin-sample 提前换算成 float64 速率值；
// err/drop 4 项为累计值，PDH 原生也是累计（名字不带 persec）。
var netFieldPDH = map[string]string{
	RenamedFieldSpeedRecv:        "Bytes_Received_persec",
	RenamedFieldSpeedSent:        "Bytes_Sent_persec",
	RenamedFieldSpeedPacketsRecv: "Packets_Received_persec",
	RenamedFieldSpeedPacketsSent: "Packets_Sent_persec",
	FieldErrIn:                   "Packets_Received_Errors",
	FieldErrOut:                  "Packets_Outbound_Errors",
	FieldDropIn:                  "Packets_Received_Discarded",
	FieldDropOut:                 "Packets_Outbound_Discarded",
}

// diskioFieldPDH：io 规范字段 → PDH 名。
// rkb_s/wkb_s/reads/writes 由 input 层 twin-sample 提前换算成速率；
// read_time 等其它字段保持第二次采样的累计值，原名上报（PDH 无对应项）。
var diskioFieldPDH = map[string]string{
	RenamedFieldRkbS: "Disk_Read_Bytes_persec",
	RenamedFieldWkbS: "Disk_Write_Bytes_persec",
	FieldReads:       "Disk_Reads_persec",
	FieldWrites:      "Disk_Writes_persec",
}

// ---------------------------------------------------------------------------
// Linux 反向表（规范名 → telegraf inputs.* 名）
// 用于 cpu_detail / io / net 之外的 measurement（env / load / netstat 的 tcp/disk/mem/swap）。
// 与 rename_out.go 里的等价表内联在本文件中，因为构建标签互斥无法跨文件引用。
// ---------------------------------------------------------------------------

// linuxReverseMeasurement：规范 measurement → telegraf inputs.* 名。
// 只用于不走 win_* 前缀的 measurement。
var linuxReverseMeasurement = map[string]string{
	RenamedEnv:  MeasurementKernel, // env  -> kernel
	RenamedLoad: MeasurementSystem, // load -> system
}

// linuxReverseGlobalField：全局规范字段 → telegraf 字段；对不走 PDH 的 measurement 使用。
var linuxReverseGlobalField = map[string]string{
	// mem / swap
	RenamedFieldPctUsed: FieldUsedPercent,

	// netstat
	RenamedFieldCurTCPCloseWait: FieldTCPCloseWait,
	RenamedFieldCurTCPTimeWait:  FieldTCPTimeWait,
	RenamedFieldCurTCPClosed:    FieldTCPClose,
	RenamedFieldCurTCPClosing:   FieldTCPClosing,
	RenamedFieldCurTCPEstab:     FieldTCPEstablished,
	RenamedFieldCurTCPFinWait1:  FieldTCPFinWait1,
	RenamedFieldCurTCPFinWait2:  FieldTCPFinWait2,
	RenamedFieldCurTCPLastAck:   FieldTCPLastAck,
	RenamedFieldCurTCPListen:    FieldTCPListen,
	RenamedFieldCurTCPSynRecv:   FieldTCPSynRecv,
	RenamedFieldCurTCPSynSent:   FieldTCPSynSent,
}

// linuxReverseDiskField：disk measurement 专用 in_use → used_percent。
var linuxReverseDiskField = map[string]string{
	RenamedFieldInUse: FieldUsedPercent,
}

// linuxReverseEnvField：env (kernel) measurement 专用。
var linuxReverseEnvField = map[string]string{
	FieldUptime:       FieldBootTime,
	RenamedFieldProcs: FieldProcessesForked,
}

// ---------------------------------------------------------------------------
// 对外接口
// ---------------------------------------------------------------------------

// Rename 把 input 产出的规范名 metric 反向翻译为 telegraf 兼容名：
// cpu_detail/io/net 走 win_perf_counters 风格；其它 measurement 走
// telegraf inputs.* 风格。速率字段的数值换算已在 input twin-sample 阶段完成。
func Rename(in []Metric) []Metric {
	if len(in) == 0 {
		return in
	}
	out := make([]Metric, 0, len(in))
	for _, m := range in {
		out = append(out, renameWinPDH(m))
	}
	return out
}

// RenameWindowsFields out 模式下直通：renameWinPDH 已完成所有改造。
func RenameWindowsFields(in []Metric) []Metric { return in }

// ---------------------------------------------------------------------------
// 核心实现
// ---------------------------------------------------------------------------

// renameWinPDH 对单条 metric 做 measurement / tag / field 三层改造。
func renameWinPDH(m Metric) Metric {
	origName := m.Name

	// 1. tag：仅对走 win_ 前缀的三个 measurement 做 instance/objectname 归一
	m.Tags = normalizeWinTags(origName, m.Tags)

	// 2. field
	m.Fields = rewriteWinFields(origName, m.Fields)

	// 3. measurement：优先 win_ 前缀；其次 linux 反向表；都没有则保持规范名
	if dst, ok := measurementWinPrefix[origName]; ok {
		m.Name = dst
	} else if dst, ok := linuxReverseMeasurement[origName]; ok {
		m.Name = dst
	}
	return m
}

// normalizeWinTags 仅对走 win_ 前缀的 measurement 做 tag 归一：
//   - 追加 objectname = <ObjectName>
//   - cpu / interface / device / name 四选一合并为 instance
//   - cpu-total 规范化为 _Total（PDH 风格）
//
// 非 win_ 前缀的 measurement 保留原 tag 结构（与 Linux 反向翻译一致）。
func normalizeWinTags(measurement string, tags map[string]string) map[string]string {
	if _, isWin := measurementWinPrefix[measurement]; !isWin {
		return tags
	}

	objectName := objectNameForMeasurement[measurement]
	newTags := make(map[string]string, len(tags)+1)
	for k, v := range tags {
		newTags[k] = v
	}
	if objectName != "" {
		newTags["objectname"] = objectName
	}

	// 若 input 层已经预填了 instance（例如 io 的 "0 C:"、net 的 PDH Description），
	// 就尊重它——只做源 tag（cpu/interface/device/name）的清理，不覆盖 instance。
	existing, hasInstance := newTags[TagInstance]
	hasInstance = hasInstance && existing != ""

	var instance string
	switch measurement {
	case RenamedCPUDetail:
		if v, ok := newTags[TagCPU]; ok {
			instance = cpuTagToInstance(v)
			delete(newTags, TagCPU)
		}
	case MeasurementNet:
		if _, ok := newTags[TagInterface]; ok {
			if !hasInstance {
				instance = newTags[TagInterface]
			}
			delete(newTags, TagInterface)
		}
	case RenamedIO:
		if _, ok := newTags[TagDevice]; ok {
			if !hasInstance {
				instance = newTags[TagDevice]
			}
			delete(newTags, TagDevice)
		}
		if _, ok := newTags[TagName]; ok {
			if !hasInstance && instance == "" {
				instance = newTags[TagName]
			}
			delete(newTags, TagName)
		}
	}
	if hasInstance {
		// 保留 input 层已预填的 instance（"0 C:" / PDH Description 等）
	} else if instance != "" {
		newTags[TagInstance] = instance
	}
	return newTags
}

// cpuTagToInstance："cpu0" -> "0"；"cpu-total" -> "_Total"。
func cpuTagToInstance(cpu string) string {
	if cpu == TagValueCPUTotal {
		return "_Total"
	}
	if strings.HasPrefix(cpu, "cpu") {
		return strings.TrimPrefix(cpu, "cpu")
	}
	return cpu
}

// rewriteWinFields 按 measurement 做字段名反翻：
//   - cpu_detail / io / net 走 PDH 表
//   - 其它 measurement 走 linux 反向表（disk / env 有 measurement-specific 特例）
//
// CPU 额外派生 Percent_Processor_Time = 100 - Percent_Idle_Time。
// 映射表里没有命中的字段保持规范名原样输出（例如 io 的 read_time 等）。
func rewriteWinFields(measurement string, fields map[string]interface{}) map[string]interface{} {
	if len(fields) == 0 {
		return fields
	}

	var (
		table         map[string]string // 主表（win_ 三兄弟）
		specificTable map[string]string // 非 win_ 的 measurement-specific（disk/env）
		useLinux      bool              // 非 win_ 时用 linuxReverseGlobalField 做 fallback
	)

	switch measurement {
	case RenamedCPUDetail:
		table = cpuFieldPDH
	case MeasurementNet:
		table = netFieldPDH
	case RenamedIO:
		table = diskioFieldPDH
	default:
		useLinux = true
		switch measurement {
		case MeasurementDisk:
			specificTable = linuxReverseDiskField
		case RenamedEnv:
			specificTable = linuxReverseEnvField
		}
	}

	out := make(map[string]interface{}, len(fields)+1)
	for k, v := range fields {
		if measurement == RenamedCPUDetail {
			if _, drop := cpuFieldsDrop[k]; drop {
				continue
			}
		}
		dst := k
		if table != nil {
			if d, ok := table[k]; ok {
				dst = d
			}
		} else if useLinux {
			if specificTable != nil {
				if d, ok := specificTable[k]; ok {
					dst = d
				} else if d2, ok2 := linuxReverseGlobalField[k]; ok2 {
					dst = d2
				}
			} else if d, ok := linuxReverseGlobalField[k]; ok {
				dst = d
			}
		}
		out[dst] = v
	}

	// CPU 派生 Percent_Processor_Time = 100 - idle
	if measurement == RenamedCPUDetail {
		if idle, ok := out["Percent_Idle_Time"]; ok {
			if f, ok := toFloat64(idle); ok {
				out["Percent_Processor_Time"] = 100 - f
			}
		}
	}
	return out
}

// toFloat64 把常见数值类型转成 float64。
func toFloat64(v interface{}) (float64, bool) {
	switch n := v.(type) {
	case float64:
		return n, true
	case float32:
		return float64(n), true
	case int:
		return float64(n), true
	case int64:
		return float64(n), true
	case uint64:
		return float64(n), true
	}
	return 0, false
}
