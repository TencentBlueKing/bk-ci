//go:build !loong64
// +build !loong64

// Package monitor 用纯 Go（gopsutil/v3）实现 telegraf 等价的指标采集，
// 避免嵌入 telegraf 带来的安全扫描告警和 Go 版本升级压力。
//
// 设计对齐 src/pkg/collector 的 telegraf 输出：
//   - measurement 名称、field 名称 / tag 都和 telegraf 相关 plugin 保持一致
//   - 经过 rename 层转成 BK-CI 后端约定的命名后上报
//   - ci 项目走 JSON，stream 项目走 InfluxDB line protocol
//
// 本文件定义跨 input / reporter / dumper 复用的核心数据结构。
package monitor

import "time"

// Metric 与 telegraf.Metric 概念等价，用于在 inputs -> rename -> reporter /
// debug dump 之间传递。字段采用 map 是为了兼容 rename 层按名修改、以及
// JSON / line protocol 两种序列化方式。
//
// Fields 的值类型只允许 float64 / int64 / uint64 / bool / string —— 对齐
// telegraf 的类型约束。reporter 不会尝试反射未知类型。
type Metric struct {
	// Name 即 telegraf 中的 measurement。例如 "cpu"、"mem"、"net"。
	Name string
	// Tags 为字符串键值对（高基数字段除外）。常见的有 host、interface、
	// cpu、device 等。reporter 会把 tags 合并到 projectId/agentId 等
	// 全局 tag 之上。
	Tags map[string]string
	// Fields 承载数值型指标。键是 field 名，值建议是 float64 / int64。
	Fields map[string]interface{}
	// Timestamp 表示采集时间。留空时 reporter 会回退到当前时间。
	Timestamp time.Time
}

// Clone 返回浅拷贝副本，tags/fields map 单独 copy，避免多个消费者
// （rename / dumper / reporter）并发修改相互污染。
func (m Metric) Clone() Metric {
	out := Metric{
		Name:      m.Name,
		Timestamp: m.Timestamp,
	}
	if len(m.Tags) > 0 {
		out.Tags = make(map[string]string, len(m.Tags))
		for k, v := range m.Tags {
			out.Tags[k] = v
		}
	}
	if len(m.Fields) > 0 {
		out.Fields = make(map[string]interface{}, len(m.Fields))
		for k, v := range m.Fields {
			out.Fields[k] = v
		}
	}
	return out
}
