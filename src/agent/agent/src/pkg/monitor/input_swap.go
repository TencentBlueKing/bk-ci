//go:build !loong64
// +build !loong64

package monitor

import (
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v3/mem"
)

// Swap 对齐 telegraf plugins/inputs/swap。
// 在没有 swap 分区的机器上（例如容器内），gopsutil 返回 Total=0，
// 本采集器仍上报 0 值以便后端时序曲线连续。
type Swap struct {
	swapMemFn func() (*mem.SwapMemoryStat, error)
	nowFn     func() time.Time
}

// NewSwap 返回默认 swap 采集器。
func NewSwap() *Swap {
	return &Swap{
		swapMemFn: mem.SwapMemory,
		nowFn:     time.Now,
	}
}

// Name 返回 "swap"。
func (s *Swap) Name() string { return MeasurementSwap }

// Gather 返回唯一一条 swap metric。
func (s *Swap) Gather() ([]Metric, error) {
	sm, err := s.swapMemFn()
	if err != nil {
		return nil, errors.Wrap(err, "swap: SwapMemory failed")
	}
	if sm == nil {
		return nil, errors.New("swap: SwapMemory returned nil")
	}
	return []Metric{{
		Name: MeasurementSwap,
		Fields: map[string]interface{}{
			FieldTotal:          sm.Total,
			FieldUsed:           sm.Used,
			FieldFree:           sm.Free,
			RenamedFieldPctUsed: sm.UsedPercent,
			FieldSwapIn:         sm.Sin,
			FieldSwapOut:        sm.Sout,
		},
		Timestamp: s.nowFn(),
	}}, nil
}
