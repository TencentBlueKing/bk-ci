//go:build out && !windows
// +build out,!windows

package monitor

// rename_out.go（外部版 Linux / macOS，对应 Stream 项目 InfluxDB line protocol）。
//
// 命名反转后，input 层产出的是 BK-CI 后端规范名
// （measurement cpu_detail / io / env / load、field user/system/idle/iowait/
//  rkb_s/wkb_s/speed_*/pct_used/in_use/cur_tcp_*/uptime/procs 等）。
// 外部版 Stream 后端历史上消费的是 telegraf inputs.* 原生名
// （measurement cpu / diskio / kernel / system、field usage_*/read_bytes/
//  write_bytes/bytes_*/packets_*/used_percent/tcp_*/boot_time/processes_forked
//  等）。
//
// 为了保持对老 Stream 后端的兼容，这里做**反向翻译**：规范名 → telegraf 兼容名。
// 翻译只涉及 measurement 名与字段名，不改变字段值语义（值仍是累计 uint64 /
// 百分比，与 telegraf 时代行为一致）。
//
// Windows 的反向翻译（→ telegraf win_perf_counters PDH 风格，
// 并由 input 层 twin-sample 做速率换算）见 rename_out_windows.go。

// reverseMeasurementRenames：规范 measurement → telegraf inputs.* 兼容名。
// 只在 out+!windows 下使用。
var reverseMeasurementRenames = map[string]string{
	RenamedCPUDetail: MeasurementCPU,    // cpu_detail -> cpu
	RenamedIO:        MeasurementDiskIO, // io         -> diskio
	RenamedEnv:       MeasurementKernel, // env        -> kernel
	RenamedLoad:      MeasurementSystem, // load       -> system
}

// reverseGlobalFieldRenames：跨 measurement 的规范字段 → telegraf 字段。
// 对应 rename.go 历史上的 globalFieldRenames（方向取反）。
var reverseGlobalFieldRenames = map[string]string{
	// cpu_detail
	RenamedFieldUser:   FieldUsageUser,
	RenamedFieldSystem: FieldUsageSystem,
	RenamedFieldIdle:   FieldUsageIdle,
	RenamedFieldIowait: FieldUsageIowait,

	// net
	RenamedFieldSpeedRecv:        FieldBytesRecv,
	RenamedFieldSpeedSent:        FieldBytesSent,
	RenamedFieldSpeedPacketsRecv: FieldPacketsRecv,
	RenamedFieldSpeedPacketsSent: FieldPacketsSent,

	// mem / swap
	RenamedFieldPctUsed: FieldUsedPercent,

	// io
	RenamedFieldRkbS: FieldReadBytes,
	RenamedFieldWkbS: FieldWriteBytes,

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

	// env / kernel：uptime 和 procs 仅在 env measurement 下需要反翻。
	// 这里不放 global，因为 load.uptime（system.uptime）不应被翻成 boot_time；
	// 见 measurementReverseSpecific 对 RenamedEnv 的分支。
}

// reverseDiskFieldRenames：disk measurement 专用（覆盖 in_use → used_percent）。
// 注意：global 里 pct_used → used_percent；disk 里 in_use → used_percent。
// 两者都映射到相同的 telegraf 字段名，但命中路径不同；这里独立一张表以对齐
// rename.go 历史上的 disk 特例逻辑。
var reverseDiskFieldRenames = map[string]string{
	RenamedFieldInUse: FieldUsedPercent,
}

// reverseEnvFieldRenames：env (kernel) measurement 专用。
// uptime 在 load (system) 里语义为 host uptime（保持不动）；
// 在 env (kernel) 里对应 kernel.boot_time。用 measurement-specific 表消歧。
var reverseEnvFieldRenames = map[string]string{
	FieldUptime:       FieldBootTime,
	RenamedFieldProcs: FieldProcessesForked,
}

// Rename 反向翻译：规范名 → telegraf inputs.* 兼容名。
// 只在 out+!windows 构建下生效。
func Rename(in []Metric) []Metric {
	if len(in) == 0 {
		return in
	}
	out := make([]Metric, len(in))
	for i, m := range in {
		out[i] = reverseRenameOne(m)
	}
	return out
}

// RenameWindowsFields 外部版 Linux 分支下为 no-op（为保持跨平台符号完整保留）。
func RenameWindowsFields(in []Metric) []Metric { return in }

// reverseRenameOne 对单条 metric 做 field + measurement 的反向翻译。
// 字段表查找优先级：measurement-specific → global；未命中则保持原字段名。
func reverseRenameOne(m Metric) Metric {
	origName := m.Name
	specific := measurementReverseSpecific(origName)

	if len(m.Fields) > 0 {
		var newFields map[string]interface{}
		for k, v := range m.Fields {
			dst := lookupReverseField(k, specific)
			if dst == k {
				continue
			}
			if newFields == nil {
				newFields = make(map[string]interface{}, len(m.Fields))
				for kk, vv := range m.Fields {
					newFields[kk] = vv
				}
			}
			delete(newFields, k)
			newFields[dst] = v
		}
		if newFields != nil {
			m.Fields = newFields
		}
	}

	if dst, ok := reverseMeasurementRenames[origName]; ok {
		m.Name = dst
	}
	return m
}

// lookupReverseField 按 specific → global 优先级查表。
func lookupReverseField(name string, specific map[string]string) string {
	if specific != nil {
		if dst, ok := specific[name]; ok {
			return dst
		}
	}
	if dst, ok := reverseGlobalFieldRenames[name]; ok {
		return dst
	}
	return name
}

// measurementReverseSpecific 返回 measurement 专用的反向表；无则 nil。
func measurementReverseSpecific(measurement string) map[string]string {
	switch measurement {
	case MeasurementDisk:
		return reverseDiskFieldRenames
	case RenamedEnv:
		return reverseEnvFieldRenames
	}
	return nil
}
