/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package controllers

import (
	"github.com/prometheus/client_golang/prometheus"
)

// CheckFailController 描述了错误类型分布的统计
type CheckFailController struct {
	gauge *prometheus.GaugeVec
}

// NewCheckFailController get a new CheckFailController
func NewCheckFailController() *CheckFailController {
	return &CheckFailController{
		gauge: prometheus.NewGaugeVec(prometheus.GaugeOpts{
			Name: "check_fail_num_gauge",
			Help: "Gauge of check fail num with reasons.",
		}, []string{"engine", "queue", "reason"}),
	}
}

// Describe
func (dnc *CheckFailController) Describe(ch chan<- *prometheus.Desc) {
	dnc.gauge.Describe(ch)
}

// Collect
func (dnc *CheckFailController) Collect(ch chan<- prometheus.Metric) {
	dnc.gauge.Collect(ch)
}

// Inc 增加给定引擎, 队列, 错误类型的计数
func (dnc *CheckFailController) Inc(engine, queue, reason string) {
	go dnc.gauge.WithLabelValues(engine, queue, reason).Inc()
}

// Clean
func (dnc *CheckFailController) Clean() {
	dnc.gauge.Reset()
}

const (
	CheckFailInitTimeout               = "init timeout"
	CheckFailStagingTimeout            = "staging timeout"
	CheckFailStartingTimeout           = "starting timeout"
	CheckFailHeartbeatTimeout          = "heartbeat timeout"
	CheckFailEngineCheckFail           = "engine check fail"
	CheckFailNotEnoughAvailableWorkers = "not enough available workers"
)
