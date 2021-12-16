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

// TaskNumController 描述了task状态分布的统计
type TaskNumController struct {
	gauge *prometheus.GaugeVec
}

// NewTaskNumController get a new TaskNumController
func NewTaskNumController() *TaskNumController {
	return &TaskNumController{
		gauge: prometheus.NewGaugeVec(prometheus.GaugeOpts{
			Name: "task_status_num_gauge",
			Help: "Gauge of task status num with status.",
		}, []string{"engine", "queue", "status", "reason"}),
	}
}

// Describe
func (tnc *TaskNumController) Describe(ch chan<- *prometheus.Desc) {
	tnc.gauge.Describe(ch)
}

// Collect
func (tnc *TaskNumController) Collect(ch chan<- prometheus.Metric) {
	tnc.gauge.Collect(ch)
}

// Inc 增加给定引擎, 队列, 状态的task计数
func (tnc *TaskNumController) Inc(engine, queue, status, reason string) {
	go tnc.gauge.WithLabelValues(engine, queue, status, reason).Inc()
}

// Dec 减少给定引擎, 队列, 状态的task计数
func (tnc *TaskNumController) Dec(engine, queue, status, reason string) {
	go tnc.gauge.WithLabelValues(engine, queue, status, reason).Dec()
}

// Clean
func (tnc *TaskNumController) Clean() {
	tnc.gauge.Reset()
}
