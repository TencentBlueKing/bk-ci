//go:build !loong64
// +build !loong64

package monitor

import (
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v3/host"
)

// Kernel 对齐 telegraf plugins/inputs/kernel。telegraf 源码直接读
// /proc/stat，但为了 Go 1.19 + 跨平台一致性，这里仅上报 gopsutil 提供的
// BootTime。其他字段（context_switches/interrupts/processes_forked）在
// Linux 上需要解析 /proc/stat，暂不实现 —— 后续如果后端明确依赖，再补
// Linux 专属文件读取逻辑；不影响 rename 流水线（rename 表里这些 field
// 若不存在不会出错）。
type Kernel struct {
	bootTimeFn func() (uint64, error)
	nowFn      func() time.Time
}

// NewKernel 返回默认采集器。
func NewKernel() *Kernel {
	return &Kernel{
		bootTimeFn: host.BootTime,
		nowFn:      time.Now,
	}
}

// Name 返回 "kernel"。
func (k *Kernel) Name() string { return MeasurementKernel }

// Gather 至少返回 boot_time。
func (k *Kernel) Gather() ([]Metric, error) {
	bt, err := k.bootTimeFn()
	if err != nil {
		return nil, errors.Wrap(err, "kernel: BootTime failed")
	}
	return []Metric{{
		Name: MeasurementKernel,
		Fields: map[string]interface{}{
			FieldBootTime: bt,
		},
		Timestamp: k.nowFn(),
	}}, nil
}
