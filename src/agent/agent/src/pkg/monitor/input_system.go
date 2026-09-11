//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
	"runtime"
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v4/host"
	"github.com/shirou/gopsutil/v4/load"
)

// System 对齐 telegraf plugins/inputs/system。采集 load average / uptime /
// CPU 数 / 在线用户数。
//
// load1/5/15 在 Windows 不可用，gopsutil load 包会返回全 0；我们在 Windows
// 平台省略这三个字段以避免误导（telegraf 的 system plugin 在 Windows 上
// 行为相同）。
type System struct {
	loadAvgFn func(ctx context.Context) (*load.AvgStat, error)
	uptimeFn  func(ctx context.Context) (uint64, error)
	usersFn   func(ctx context.Context) ([]host.UserStat, error)
	nowFn     func() time.Time
}

// NewSystem 返回默认采集器。
func NewSystem() *System {
	return &System{
		loadAvgFn: load.AvgWithContext,
		uptimeFn:  host.UptimeWithContext,
		usersFn:   host.UsersWithContext,
		nowFn:     time.Now,
	}
}

// Name 返回 "load"（规范名：system → load）。
func (s *System) Name() string { return RenamedLoad }

// Gather 采集 system metric。
func (s *System) Gather(ctx context.Context) ([]Metric, error) {
	fields := map[string]interface{}{
		FieldNCPUs: uint64(runtime.NumCPU()),
	}

	// load 仅 Linux/Darwin 有意义
	if runtime.GOOS != "windows" {
		if la, err := s.loadAvgFn(ctx); err == nil && la != nil {
			fields[FieldLoad1] = la.Load1
			fields[FieldLoad5] = la.Load5
			fields[FieldLoad15] = la.Load15
		}
	}

	// uptime 所有平台都支持
	if up, err := s.uptimeFn(ctx); err == nil {
		fields[FieldUptime] = up
	}

	// users 在容器内可能因 utmp 缺失而报错，捕获忽略
	if users, err := s.usersFn(ctx); err == nil {
		fields[FieldNUsers] = int64(len(users))
	}

	if len(fields) == 1 { // 只有 n_cpus 说明采集几乎失败
		return nil, errors.New("system: all sub-queries failed")
	}

	return []Metric{{
		Name:      RenamedLoad,
		Fields:    fields,
		Timestamp: s.nowFn(),
	}}, nil
}
