//go:build out && !windows
// +build out,!windows

package monitor

// rename_out.go（非 Windows）对齐 collector/telegrafConf/telegrafConf_out.go：
// Linux 版 out 模板里没有 [[processors.rename]]，telegraf 原样输出
// gopsutil 原生 measurement / field，monitor 也保持直通。

// Rename out 模式下不做改名。
func Rename(in []Metric) []Metric { return in }

// RenameWindowsFields out+!windows 下为 no-op（不会被调用，为了符号完整保留）。
func RenameWindowsFields(in []Metric) []Metric { return in }
