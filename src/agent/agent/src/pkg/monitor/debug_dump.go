//go:build !loong64
// +build !loong64

package monitor

import (
	"encoding/json"
	"io"
	"os"
	"path/filepath"
	"sync"
	"sync/atomic"
	"time"

	lumberjack "gopkg.in/natefinch/lumberjack.v2"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

// debug_dump.go 在 workdir/.debug 存在时，把每次采集的指标追加写入独立
// 的日志文件，便于线上排查指标为何上报错乱。
//
// 关键选择：
//  1. 仅当 .debug 文件存在时才打开 writer，避免没开 debug 时仍然产生 IO。
//  2. 使用 lumberjack 做日志轮转（MaxAge=7, MaxBackups=7），与
//     logs.Init 里对主日志的配置一致，不需要额外运维清理。
//  3. 每条 metric 一行 JSON，方便 `jq` 过滤。
//  4. 每次 Dump 调用时都 Stat 一次 .debug，支持运行时动态开关。
//
// 对应 collector 侧的等价实现位于 debug_dump_collector.go。

// dumpLineJSON 是写入文件的一行 JSON 结构。
// 字段紧凑，优先排布便于肉眼快速 grep。
type dumpLineJSON struct {
	Timestamp string                 `json:"ts"`     // RFC3339 UTC
	Source    string                 `json:"source"` // monitor | collector
	Name      string                 `json:"name"`
	Tags      map[string]string      `json:"tags,omitempty"`
	Fields    map[string]interface{} `json:"fields"`
}

// Dumper 负责把 metric 序列化后写到独立日志文件。
// 通过 source + filename 区分 monitor 和 collector 两路。
type Dumper struct {
	workDir  string // 用于定位 .debug 标记文件和日志目录
	filename string // logs 目录下的文件名，如 monitor_metrics.log
	source   string // dumpLineJSON.Source 字段值

	mu     sync.Mutex
	writer io.WriteCloser // nil 表示当前未开启 debug
	// lastEnabled 缓存上一次 stat 结果，避免在每条 metric 都 Stat 文件。
	// 0 = unknown, 1 = enabled, 2 = disabled
	lastEnabled   atomic.Int32
	lastCheckedAt time.Time
}

// NewMonitorDumper 返回 monitor 专用 dumper（文件名 monitor_metrics.log）。
func NewMonitorDumper(workDir string) *Dumper {
	return &Dumper{
		workDir:  workDir,
		filename: "monitor_metrics.log",
		source:   TagSourceMonitor,
	}
}

// NewCollectorDumper 返回 collector（telegraf 侧）专用 dumper
// （文件名 collector_metrics.log）。
func NewCollectorDumper(workDir string) *Dumper {
	return &Dumper{
		workDir:  workDir,
		filename: "collector_metrics.log",
		source:   "collector",
	}
}

// Enabled 返回当前是否处于 debug dump 状态（.debug 文件存在）。
// 为避免每次 metric 都触发文件 stat，本方法对 .debug 状态做 1s 缓存。
func (d *Dumper) Enabled() bool {
	const ttl = time.Second
	now := time.Now()
	last := d.lastEnabled.Load()
	if last != 0 && now.Sub(d.lastCheckedAt) < ttl {
		return last == 1
	}
	enabled := debugFileExists(d.workDir)
	if enabled {
		d.lastEnabled.Store(1)
	} else {
		d.lastEnabled.Store(2)
	}
	d.lastCheckedAt = now
	// debug 被关闭时关闭 writer，立即释放句柄（不等 Close 显式调用）
	if !enabled {
		d.closeWriter()
	}
	return enabled
}

// Dump 把一批 metric 追加写入日志文件。
// 当前未开启 debug 时本方法为零开销（Stat 有 1s 缓存）。
// 内部写入错误只记录日志，不返回给上层 —— dump 是辅助功能，不应阻塞主链路。
func (d *Dumper) Dump(metrics []Metric) {
	if !d.Enabled() || len(metrics) == 0 {
		return
	}

	d.mu.Lock()
	defer d.mu.Unlock()
	if d.writer == nil {
		d.writer = &lumberjack.Logger{
			Filename:   filepath.Join(d.workDir, "logs", d.filename),
			MaxSize:    50, // MB
			MaxAge:     7,  // 天
			MaxBackups: 7,
			LocalTime:  true,
		}
	}

	enc := json.NewEncoder(d.writer)
	for _, m := range metrics {
		line := dumpLineJSON{
			Timestamp: m.Timestamp.UTC().Format(time.RFC3339Nano),
			Source:    d.source,
			Name:      m.Name,
			Tags:      m.Tags,
			Fields:    m.Fields,
		}
		if m.Timestamp.IsZero() {
			line.Timestamp = time.Now().UTC().Format(time.RFC3339Nano)
		}
		if err := enc.Encode(line); err != nil {
			logs.WithError(err).Warnf("monitor|dumper encode failed for %s", m.Name)
		}
	}
}

// Close 关闭底层 writer，通常由主循环在退出时调用。
func (d *Dumper) Close() error {
	d.mu.Lock()
	defer d.mu.Unlock()
	return d.closeWriterLocked()
}

// closeWriter 是 Close 的无锁版本，内部调用。
func (d *Dumper) closeWriter() {
	d.mu.Lock()
	defer d.mu.Unlock()
	_ = d.closeWriterLocked()
}

// closeWriterLocked 必须在持锁状态下调用。
func (d *Dumper) closeWriterLocked() error {
	if d.writer == nil {
		return nil
	}
	err := d.writer.Close()
	d.writer = nil
	return err
}

// debugFileExists 检查 workDir/.debug 是否存在。
// 独立实现一份避免引入 agentcli 包（否则形成 agent → agentcli → monitor → agentcli 循环依赖）。
func debugFileExists(workDir string) bool {
	_, err := os.Stat(filepath.Join(workDir, ".debug"))
	return err == nil
}
