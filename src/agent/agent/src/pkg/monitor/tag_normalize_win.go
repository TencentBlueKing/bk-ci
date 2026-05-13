//go:build windows
// +build windows

package monitor

import "strings"

// tag_normalize_win.go 把 monitor 规范 tag 归一为"telegraf win_perf_counters
// 风格"，让内部版 Windows 的 metric 也带 instance + objectname，供后端与旧看
// 板一致查询。
//
// 与 rename_out_win.go 的区别：
//   - rename_out_win.go 只在 out+windows 构建生效，承担 measurement 重命名
//     + 字段名反翻（cpu_detail → win_cpu、user → Percent_User_Time 等）
//   - 本文件在**所有 Windows 构建**生效（含内部版），只处理 tag；不重命名
//     measurement、不改字段名
//
// 产出 tag 语义：
//   cpu_detail : instance=<"cpu0"→"0"、"cpu-total"→"_Total">，objectname=Processor，
//                同时保留原 cpu=<cpu0> 做兼容双写（上游 input_cpu.go 已双写）
//   io         : instance=<name 或 device 原值>，objectname=PhysicalDisk
//   net        : instance=<FriendlyName 原值>，objectname=Network Interface
//
// 非这三类 measurement 原样返回。

// winTagObjectName 给 Windows 下三类 measurement 贴 PDH ObjectName tag。
var winTagObjectName = map[string]string{
	RenamedCPUDetail: "Processor",
	RenamedIO:        "PhysicalDisk",
	MeasurementNet:   "Network Interface",
}

// normalizeWinMetricTags 对 metric 的 Tags 做 Windows 风格归一。
// 返回新 map（不改原 map，便于测试与并发）。
func normalizeWinMetricTags(measurement string, tags map[string]string) map[string]string {
	objectName, isWin := winTagObjectName[measurement]
	if !isWin {
		return tags
	}

	newTags := make(map[string]string, len(tags)+2)
	for k, v := range tags {
		newTags[k] = v
	}
	newTags[TagObjectName] = objectName

	// instance：CPU 从 cpu 归一到 PDH 风格（"cpu0"→"0"、"cpu-total"→"_Total"）；
	// IO 和 Net 从现有 name/device/interface tag 取值直接作为 instance。
	switch measurement {
	case RenamedCPUDetail:
		if v, ok := newTags[TagCPU]; ok {
			newTags[TagInstance] = cpuTagValueToInstance(v)
		} else if v, ok := newTags[TagInstance]; ok {
			// cpu_detail 的 instance 可能已由 input 层写入 cpu0 形式
			newTags[TagInstance] = cpuTagValueToInstance(v)
		}
	case RenamedIO:
		if v, ok := newTags[TagInstance]; ok && v != "" {
			// 已有 instance（可能上游已归一），保留
		} else if v, ok := newTags[TagName]; ok {
			newTags[TagInstance] = v
		} else if v, ok := newTags[TagDevice]; ok {
			newTags[TagInstance] = v
		}
	case MeasurementNet:
		if _, ok := newTags[TagInstance]; !ok {
			if v, ok := newTags[TagInterface]; ok {
				newTags[TagInstance] = v
			}
		}
	}
	return newTags
}

// cpuTagValueToInstance："cpu0"→"0"、"cpu-total"→"_Total"；其它值原样返回。
// 这份实现与 rename_out_win.go 里的 cpuTagToInstance 行为一致，但后者受
// out 构建标签限制，这里为了内部版也能用做一份独立副本（合并不了）。
func cpuTagValueToInstance(v string) string {
	if v == TagValueCPUTotal {
		return "_Total"
	}
	return strings.TrimPrefix(v, "cpu")
}

// init 把 CPU input 的 tag 归一钩子指向 normalizeWinMetricTags。
// 非 Windows 构建下 winTagNormalizerFn 保持 nil，Gather 不会做归一。
// net / diskio 的 Windows 实现在 input_net_win.go / input_diskio_win.go 里直
// 接调用 normalizeWinMetricTags，不依赖这个钩子。
func init() {
	winTagNormalizerFn = normalizeWinMetricTags
}

// init 把 CPU input 的 tag 归一钩子指向 normalizeWinMetricTags。
// 非 Windows 构建下 winTagNormalizerFn 保持 nil，Gather 不会做归一。
// net / diskio 的 Windows 实现在 input_net_win.go / input_diskio_win.go 里直
// 接调用 normalizeWinMetricTags，不依赖这个钩子。
func init() {
	winTagNormalizerFn = normalizeWinMetricTags
}
