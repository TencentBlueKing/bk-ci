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

var (
	defaultTaskStatusDurationTimeWidth  = []float64{.01, .05, .1, .5, 1, 2, 5, 10, 20, 50}
	defaultTaskRunningDurationTimeWidth = []float64{10, 30, 60, 120, 300, 600, 1200, 1800, 2400, 3000, 3600, 5400, 7200}
)

// record all status duration time
type TaskTimeController struct {
	histogramVec *prometheus.HistogramVec
}

// NewTaskTimeController get a new TaskTimeController
func NewTaskTimeController() *TaskTimeController {
	return &TaskTimeController{
		histogramVec: prometheus.NewHistogramVec(prometheus.HistogramOpts{
			Name:    "task_status_time_duration_histogram",
			Help:    "Bucketed histogram of task status time duration(second).",
			Buckets: defaultTaskStatusDurationTimeWidth,
		}, []string{"status"}),
	}
}

// Describe
func (ttc *TaskTimeController) Describe(ch chan<- *prometheus.Desc) {
	ttc.histogramVec.Describe(ch)
}

// Collect
func (ttc *TaskTimeController) Collect(ch chan<- prometheus.Metric) {
	ttc.histogramVec.Collect(ch)
}

// Observe 增加一次统计, 对给定状态的持续时间
func (ttc *TaskTimeController) Observe(status string, durationTimeSecond float64) {
	go ttc.histogramVec.WithLabelValues(status).Observe(durationTimeSecond)
}

// record running duration time
type TaskRunningTimeController struct {
	histogram prometheus.Histogram
}

// NewTaskRunningTimeController get a new TaskRunningTimeController
func NewTaskRunningTimeController() *TaskRunningTimeController {
	return &TaskRunningTimeController{
		histogram: prometheus.NewHistogram(prometheus.HistogramOpts{
			Name:    "task_running_time_duration_histogram",
			Help:    "Bucketed histogram of task running time duration(second).",
			Buckets: defaultTaskRunningDurationTimeWidth,
		}),
	}
}

// Describe
func (trt *TaskRunningTimeController) Describe(ch chan<- *prometheus.Desc) {
	trt.histogram.Describe(ch)
}

// Collect
func (trt *TaskRunningTimeController) Collect(ch chan<- prometheus.Metric) {
	trt.histogram.Collect(ch)
}

// Observe 增加一次统计, 对running状态的持续时间
func (trt *TaskRunningTimeController) Observe(durationTimeSecond float64) {
	go trt.histogram.Observe(durationTimeSecond)
}
