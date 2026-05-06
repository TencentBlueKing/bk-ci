//go:build !loong64
// +build !loong64

// Package monitor 下 input 接口定义。每个 input 对应 telegraf 的一个
// plugin（cpu / mem / disk / diskio / net / netstat / swap / system /
// kernel / processes）。
//
// 采集器之间互不依赖；每个 Gather 调用应该在一分钟以内完成，避免被主循环
// 中的 ticker 拖慢（主循环并发调用所有 input）。
package monitor

// Input 是所有采集器的统一接口。
//
// Name 返回 measurement 名（对齐 telegraf plugin 名称，例如 "cpu"、"mem"），
// 失败时 reporter 会用它作为错误上下文。
//
// Gather 每次调用执行一次完整采集，返回本次所有 metric。
// 错误应该只在"本次采集整体失败"时返回；局部缺失建议在实现内部降级（例如
// 跳过某个 interface 但继续返回其他 interface 的指标），避免一个网卡异常
// 导致整个 net measurement 丢失。
type Input interface {
	Name() string
	Gather() ([]Metric, error)
}
