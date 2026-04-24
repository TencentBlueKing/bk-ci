//go:build !out
// +build !out

package monitor

// rename.go（非 out / 内部版 BK-CI 项目）。
//
// 命名反转：input 层（input_cpu.go / input_diskio.go / ...）现已直接产出
// BK-CI 后端约定的规范字段名：measurement=cpu_detail / io / env / load、
// field=user/system/idle/iowait/rkb_s/wkb_s/speed_*/pct_used/in_use/
// cur_tcp_*/uptime/procs 等。内部版后端看到的名字就是规范名，不再需要
// 在上报前做一次 rename，所以这里退化为直通。
//
// 保留 Rename / RenameWindowsFields 的签名是为了：
//   1. 不破坏 monitor.go 主循环 (doOneGather) 对 Rename 的调用点；
//   2. Windows 下老路径若仍通过 RenameWindowsFields 调用也不致编译失败。
//
// out 分支（外部版 / Stream 项目）的反向翻译见 rename_out.go（Linux）
// 与 rename_out_windows.go（Windows PDH 风格）。

// Rename 直通返回入参。内部版 input 已按规范名产出，无需改名。
func Rename(in []Metric) []Metric { return in }

// RenameWindowsFields 历史遗留接口，内部版直通。
// 对应 out+windows 下的 PDH 反向翻译在 rename_out_windows.go 里单独实现。
func RenameWindowsFields(in []Metric) []Metric { return in }
