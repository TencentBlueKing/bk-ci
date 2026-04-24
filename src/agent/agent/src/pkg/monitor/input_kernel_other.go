//go:build !linux && !loong64
// +build !linux,!loong64

package monitor

// 非 Linux 平台不解析 /proc/stat 与 entropy_avail；把 defaultLinuxExtraFn
// 设为 nil，让 Kernel.Gather 跳过专属字段。
// 这里不做 "提供 Darwin/Windows 兼容字段" 的 best-effort——telegraf 在这两
// 个平台也不上报 interrupts/context_switches/procs/entropy_avail，保持一致。
var defaultLinuxExtraFn func() (map[string]interface{}, error)
