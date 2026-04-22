//go:build out && windows
// +build out,windows

package monitor

import (
	"strings"
)

// rename_out_windows.go 只对 Windows 专用语义的三个 measurement 做"PDH 风格化"
// 重塑：cpu -> win_cpu、diskio -> win_diskio、net -> win_net。
// 字段也只在这三者上改成 telegraf win_perf_counters 的 PDH 命名
// （Percent_*_Time / Bytes_*_persec / Disk_*_Bytes_persec 等）。
//
// system / mem / disk / swap / kernel / netstat / processes 一律保持
// gopsutil 原生 measurement 与字段名不动 —— 外部查询脚本在这几类指标上
// Linux/macOS/Windows 共用同一条 InfluxQL，rename 必须透传。
//
// 样例（对齐 telegraf win_perf_counters 输出）：
//   win_cpu,instance=16,objectname=Processor Percent_User_Time=0,
//   Percent_Processor_Time=1.64,Percent_Idle_Time=96.80 <ts>
//
// 一旦 telegrafConf_out_win.go 里的 win_perf_counters counter 列表变动，
// 要同步回这里。

// measurementWinPrefix：只对 Windows 专用语义的 measurement 加 win_ 前缀。
// system / mem / disk / swap 保持原名 —— 外部场景下前端的 "SELECT ... FROM
// mem/system/disk" 这类跨平台查询要求 Windows 也走 Linux 的 measurement 名。
// 相应的字段名也保持 gopsutil 原生（used_percent / n_cpus / total），不改成
// telegraf win_perf_counters 的 PDH 风格。
var measurementWinPrefix = map[string]string{
	MeasurementCPU:    "win_cpu",
	MeasurementDiskIO: "win_diskio",
	MeasurementNet:    "win_net",
}

// objectNameForMeasurement：PDH ObjectName tag。
// 只给改名到 win_* 的 measurement 打 objectname，其他保持原生 tag 结构。
var objectNameForMeasurement = map[string]string{
	MeasurementCPU:    "Processor",
	MeasurementDiskIO: "PhysicalDisk",
	MeasurementNet:    "Network Interface",
}

// cpuFieldPDH：cpu measurement 的 gopsutil 字段 -> PDH 名。
var cpuFieldPDH = map[string]string{
	FieldUsageUser:    "Percent_User_Time",
	FieldUsageSystem:  "Percent_Privileged_Time",
	FieldUsageIdle:    "Percent_Idle_Time",
	FieldUsageIrq:     "Percent_Interrupt_Time",
	FieldUsageSoftirq: "Percent_DPC_Time",
}

// cpuFieldsDrop：Windows PDH 并不输出这些字段（Linux 风格的 nice/iowait/
// guest/steal 在 Windows 采样里始终是 0 或无意义），out+windows 下丢掉以
// 贴合 telegraf win_perf_counters 的实际输出。
var cpuFieldsDrop = map[string]struct{}{
	FieldUsageNice:      {},
	FieldUsageGuest:     {},
	FieldUsageGuestNice: {},
	FieldUsageIowait:    {},
	FieldUsageSteal:     {},
}

// netFieldPDH：net measurement 字段 -> PDH 名。
var netFieldPDH = map[string]string{
	FieldBytesRecv:   "Bytes_Received_persec",
	FieldBytesSent:   "Bytes_Sent_persec",
	FieldPacketsRecv: "Packets_Received_persec",
	FieldPacketsSent: "Packets_Sent_persec",
	FieldErrIn:       "Packets_Received_Errors",
	FieldErrOut:      "Packets_Outbound_Errors",
	FieldDropIn:      "Packets_Received_Discarded",
	FieldDropOut:     "Packets_Outbound_Discarded",
}

// diskioFieldPDH：diskio measurement 字段 -> PDH 名。
var diskioFieldPDH = map[string]string{
	FieldReadBytes: "Disk_Read_Bytes_persec",
	FieldWriteBytes: "Disk_Write_Bytes_persec",
	FieldReads:      "Disk_Readspersec",
	FieldWrites:     "Disk_Writespersec",
}

// Rename 把 gopsutil 原生 metric 重塑成 telegraf win_perf_counters 输出风格。
// 只在 out+windows 构建下生效，其他平台见 rename_out.go / rename.go。
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

// RenameWindowsFields 在 out 模式下同样直通：renameWinPDH 已把字段改成 PDH 名。
func RenameWindowsFields(in []Metric) []Metric { return in }

// renameWinPDH 对单条 metric 做 measurement/tag/field 三层改造。
func renameWinPDH(m Metric) Metric {
	origName := m.Name

	// 1. measurement
	if dst, ok := measurementWinPrefix[origName]; ok {
		m.Name = dst
	}

	// 2. tag：补 objectname、把 cpu=/interface=/device=/name= 改成 instance=
	m.Tags = normalizeWinTags(origName, m.Tags)

	// 3. field：按 measurement 选对应映射表
	m.Fields = rewriteWinFields(origName, m.Fields)
	return m
}

// normalizeWinTags 统一 tag 结构：
//   - 追加 objectname = <ObjectName>
//   - cpu / interface / device / name 四选一 -> instance
//     （telegraf win_perf_counters 产出的 tag 就叫 instance）
//   - cpu-total 在 PDH 里记作 _Total
func normalizeWinTags(measurement string, tags map[string]string) map[string]string {
	objectName := objectNameForMeasurement[measurement]
	// 预期最终长度 = 原 tag 数 + 1 (objectname)，-1 (被合并的 cpu/interface/...)
	newTags := make(map[string]string, len(tags)+1)
	for k, v := range tags {
		newTags[k] = v
	}
	if objectName != "" {
		newTags["objectname"] = objectName
	}

	// 合并 instance 来源。顺序：cpu -> interface -> device -> name
	// 任一命中就消费掉，后续不再重复处理（每个 measurement 只有其中一种）
	var instance string
	switch {
	case measurement == MeasurementCPU:
		if v, ok := newTags[TagCPU]; ok {
			instance = cpuTagToInstance(v)
			delete(newTags, TagCPU)
		}
	case measurement == MeasurementNet:
		if v, ok := newTags[TagInterface]; ok {
			instance = v
			delete(newTags, TagInterface)
		}
		// disk 用 device / path；diskio 用 name（对齐 telegraf inputs.diskio）
	case measurement == MeasurementDiskIO:
		if v, ok := newTags[TagDevice]; ok {
			instance = v
			delete(newTags, TagDevice)
		} else if v, ok := newTags[TagName]; ok {
			instance = v
			delete(newTags, TagName)
		}
	}
	if instance != "" {
		newTags["instance"] = instance
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

// rewriteWinFields 按 measurement 选映射表，做 field key 替换。
// 不在映射表里的字段按原样保留（例如 read_time / io_time 等 PDH 也没有的字段）。
// CPU 额外派生 Percent_Processor_Time = 100 - Percent_Idle_Time。
func rewriteWinFields(measurement string, fields map[string]interface{}) map[string]interface{} {
	if len(fields) == 0 {
		return fields
	}
	var table map[string]string
	switch measurement {
	case MeasurementCPU:
		table = cpuFieldPDH
	case MeasurementNet:
		table = netFieldPDH
	case MeasurementDiskIO:
		table = diskioFieldPDH
	}

	out := make(map[string]interface{}, len(fields)+1)
	for k, v := range fields {
		if measurement == MeasurementCPU {
			if _, drop := cpuFieldsDrop[k]; drop {
				continue
			}
		}
		if dst, ok := table[k]; ok {
			out[dst] = v
		} else {
			out[k] = v
		}
	}

	// CPU：派生 Percent_Processor_Time = 100 - idle
	if measurement == MeasurementCPU {
		if idle, ok := out["Percent_Idle_Time"]; ok {
			if f, ok := toFloat64(idle); ok {
				out["Percent_Processor_Time"] = 100 - f
			}
		}
	}
	return out
}

// toFloat64 把常见数值类型转成 float64（仅覆盖 gopsutil 产出的类型）。
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
