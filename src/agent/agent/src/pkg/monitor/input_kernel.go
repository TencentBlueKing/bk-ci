//go:build !loong64
// +build !loong64

package monitor

import (
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v4/host"
)

// Kernel 对齐 telegraf plugins/inputs/kernel。
//
// telegraf 源码（plugins/inputs/kernel/kernel.go，linux build tag）直接读
// /proc/stat 与 /proc/sys/kernel/random/entropy_avail，产出
// interrupts / context_switches / processes_forked / boot_time / entropy_avail。
// 我们在 Linux 下实现同样的字段解析（见 input_kernel_linux.go），其他平台
// 只输出 uptime（由 gopsutil host.BootTime 提供）。
//
// 这么拆分是为了：
//   - Linux-only 的 /proc 文件读取与平台无关代码解耦，交叉编译 darwin/windows
//     不需要存根
//   - linuxExtraFn 注入点方便单测 fake /proc 数据，避免真实环境依赖
type Kernel struct {
	bootTimeFn func() (uint64, error)
	nowFn      func() time.Time
	// linuxExtraFn 读取 Linux 专属字段（/proc/stat + entropy_avail）。
	// 非 Linux 平台为 nil。Linux build tag 下由 input_kernel_linux.go
	// 的 init() / NewKernel 赋值。返回的 map 会合并到 env metric 的 fields 中。
	linuxExtraFn func() (map[string]interface{}, error)
}

// NewKernel 返回默认采集器。
func NewKernel() *Kernel {
	k := &Kernel{
		bootTimeFn: host.BootTime,
		nowFn:      time.Now,
	}
	// Linux build tag 下通过 input_kernel_linux.go 注入。
	k.linuxExtraFn = defaultLinuxExtraFn
	return k
}

// Name 返回 "env"（规范名：kernel → env）。
func (k *Kernel) Name() string { return RenamedEnv }

// Gather 至少返回 uptime（规范名；由 kernel.boot_time 改名而来）；
// 在 Linux 平台额外返回 interrupts / context_switches / procs / entropy_avail。
// Linux 专属字段读取失败不会阻断其他字段上报，仅记为 warn（调用方可按需处理）。
func (k *Kernel) Gather() ([]Metric, error) {
	bt, err := k.bootTimeFn()
	if err != nil {
		return nil, errors.Wrap(err, "kernel: BootTime failed")
	}
	fields := map[string]interface{}{
		FieldUptime: bt,
	}
	if k.linuxExtraFn != nil {
		extra, xerr := k.linuxExtraFn()
		if xerr != nil {
			// /proc 读取失败不致命——可能是权限 / /proc 被 chroot 隐藏。
			// 丢掉 Linux 专属字段但继续上报 uptime，保持和旧行为向下兼容。
			return []Metric{{
				Name:      RenamedEnv,
				Fields:    fields,
				Timestamp: k.nowFn(),
			}}, nil
		}
		for name, v := range extra {
			fields[name] = v
		}
	}
	return []Metric{{
		Name:      RenamedEnv,
		Fields:    fields,
		Timestamp: k.nowFn(),
	}}, nil
}
