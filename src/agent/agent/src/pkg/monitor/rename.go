//go:build !loong64
// +build !loong64

package monitor

// rename.go 对齐 src/pkg/collector/telegrafConf/telegrafConf.go 中
// processors.rename 的配置，把 gopsutil 原生字段名翻译成 BK-CI 后端约定的
// 字段名（例如 bytes_recv -> speed_recv）。
//
// 原则：
//  1. globalFieldRenames 对所有 measurement 生效（telegraf 里 rename.replace
//     没有 namepass 时等价）
//  2. measurementRenames 替换 measurement 名（cpu -> cpu_detail 等）
//  3. diskFieldRenames 针对 measurement=disk 特例化，覆盖 global 的 pct_used
//     改为 in_use（对齐 telegraf 单独 rename 块的 namepass=["disk"]）
//  4. winFieldRenames 用于 Windows PDH 字段，先于 global 生效
//
// 修改这些表前请同步改 telegrafConf.go，保持 collector 与 monitor 上报的
// 字段名一致，否则服务端看板会同时出现新旧两套字段。

// globalFieldRenames：跨 measurement 的统一字段重命名。
// 若当前 measurement 有 measurement 级特例表，先走特例再走全局。
var globalFieldRenames = map[string]string{
	// cpu
	FieldUsageUser:   RenamedFieldUser,
	FieldUsageSystem: RenamedFieldSystem,
	FieldUsageIdle:   RenamedFieldIdle,
	FieldUsageIowait: RenamedFieldIowait,

	// net
	FieldBytesRecv:   RenamedFieldSpeedRecv,
	FieldBytesSent:   RenamedFieldSpeedSent,
	FieldPacketsRecv: RenamedFieldSpeedPacketsRecv,
	FieldPacketsSent: RenamedFieldSpeedPacketsSent,

	// mem
	FieldUsedPercent: RenamedFieldPctUsed,

	// diskio
	FieldReadBytes:  RenamedFieldRkbS,
	FieldWriteBytes: RenamedFieldWkbS,

	// netstat
	FieldTCPCloseWait:   RenamedFieldCurTCPCloseWait,
	FieldTCPTimeWait:    RenamedFieldCurTCPTimeWait,
	FieldTCPClose:       RenamedFieldCurTCPClosed,
	FieldTCPClosing:     RenamedFieldCurTCPClosing,
	FieldTCPEstablished: RenamedFieldCurTCPEstab,
	FieldTCPFinWait1:    RenamedFieldCurTCPFinWait1,
	FieldTCPFinWait2:    RenamedFieldCurTCPFinWait2,
	FieldTCPLastAck:     RenamedFieldCurTCPLastAck,
	FieldTCPListen:      RenamedFieldCurTCPListen,
	FieldTCPSynRecv:     RenamedFieldCurTCPSynRecv,
	FieldTCPSynSent:     RenamedFieldCurTCPSynSent,

	// kernel
	FieldBootTime:        FieldUptime,
	FieldProcessesForked: RenamedFieldProcs,
}

// measurementRenames：measurement 名重命名（处理器内 `[[replace]] measurement=X dest=Y`）。
var measurementRenames = map[string]string{
	MeasurementCPU:    RenamedCPUDetail,
	MeasurementDiskIO: RenamedIO,
	MeasurementSystem: RenamedLoad,
	MeasurementKernel: RenamedEnv,
}

// diskFieldRenames：measurement=disk 专用，用于覆盖 globalFieldRenames。
// 对应 telegrafConf.go 尾部 `[[processors.rename]] namepass=["disk"]`。
var diskFieldRenames = map[string]string{
	FieldUsedPercent: RenamedFieldInUse, // 覆盖 global 的 pct_used
}

// winFieldRenames：Windows PDH 计数器字段 -> 统一字段。先于 global 生效。
// 对应 telegrafConf_win.go 里的 PDH 重命名规则。
var winFieldRenames = map[string]string{
	WinFieldPercentUserTime:       RenamedFieldUser,
	WinFieldPercentPrivilegedTime: RenamedFieldSystem,
	WinFieldPercentIdleTime:       RenamedFieldIdle,
	WinFieldBytesReceivedPerSec:   RenamedFieldSpeedRecv,
	WinFieldBytesSentPerSec:       RenamedFieldSpeedSent,
	WinFieldPacketsReceivedPerSec: RenamedFieldSpeedPacketsRecv,
	WinFieldPacketsSentPerSec:     RenamedFieldSpeedPacketsSent,
	WinFieldDiskReadBytesPerSec:   RenamedFieldRkbS,
	WinFieldDiskWriteBytesPerSec:  RenamedFieldWkbS,
}

// Rename 对一组 metric 原地应用重命名规则，返回重命名后的新切片。
// 不修改入参（即使底层 Fields map 也会在必要时被替换），以便 monitor 主
// 循环可以把同一批指标既传给 debug dumper 又传给 reporter 而不相互影响。
//
// 实现时对 Fields 做浅 copy：遍历源 map，按规则查目标名，只在发生改名时
// 新建 map。未触发改名的 metric 会复用原 map 引用以节省分配。
func Rename(in []Metric) []Metric {
	if len(in) == 0 {
		return in
	}
	out := make([]Metric, len(in))
	for i, m := range in {
		out[i] = renameOne(m)
	}
	return out
}

// renameOne 对单条 metric 应用规则。
func renameOne(m Metric) Metric {
	origName := m.Name

	// 1. 先选出当前 measurement 的字段级重命名表（disk 特殊）
	specific := diskSpecificRenames(origName)

	// 2. 遍历 Fields，查 specific -> global；仅在命中时新建 map
	if len(m.Fields) > 0 {
		var newFields map[string]interface{}
		for k, v := range m.Fields {
			dst := renameField(k, specific)
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

	// 3. measurement 级改名
	if dst, ok := measurementRenames[origName]; ok {
		m.Name = dst
	}
	return m
}

// renameField 按 specific -> global 的优先级返回字段最终名。
// 命中 specific 后不再走 global，以便 disk 的 used_percent -> in_use
// 能覆盖 global 的 used_percent -> pct_used。
func renameField(name string, specific map[string]string) string {
	if specific != nil {
		if dst, ok := specific[name]; ok {
			return dst
		}
	}
	if dst, ok := globalFieldRenames[name]; ok {
		return dst
	}
	return name
}

// diskSpecificRenames 返回当前 measurement 对应的 measurement-specific 重
// 命名表；无特例则返回 nil。
func diskSpecificRenames(measurement string) map[string]string {
	if measurement == MeasurementDisk {
		return diskFieldRenames
	}
	return nil
}

// RenameWindowsFields 供 Windows PDH 采集器在 Gather 末尾调用，把 telegraf
// 风格的 PDH 字段名（如 Percent_User_Time）先翻译成统一字段名（user）。
// 之后再走 Rename 走 global rename（user 不在 global 里，保持不变）。
//
// 分两步是因为 Windows 的 measurement 和 field 分布与 Linux 的 telegraf 不
// 完全同构；把这一层单独暴露便于 Windows input 实现。
func RenameWindowsFields(in []Metric) []Metric {
	if len(in) == 0 {
		return in
	}
	out := make([]Metric, len(in))
	for i, m := range in {
		if len(m.Fields) == 0 {
			out[i] = m
			continue
		}
		var newFields map[string]interface{}
		for k, v := range m.Fields {
			dst, ok := winFieldRenames[k]
			if !ok {
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
		out[i] = m
	}
	return out
}
