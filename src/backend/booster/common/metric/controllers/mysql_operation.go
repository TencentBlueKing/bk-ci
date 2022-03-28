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
	defaultMySQLOperationTimeWidth = []float64{5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000}
)

// MySQLOperationController define the controller of mysql operation metric.
type MySQLOperationController struct {
	histogramVec *prometheus.HistogramVec
}

// NewMySQLOperationController get a new MySQLOperationController.
func NewMySQLOperationController() *MySQLOperationController {
	return &MySQLOperationController{
		histogramVec: prometheus.NewHistogramVec(prometheus.HistogramOpts{
			Name:    "mysql_operation_time_histogram",
			Help:    "Bucketed histogram of mysql operation time(millisecond).",
			Buckets: defaultMySQLOperationTimeWidth,
		}, []string{"operation"}),
	}
}

// Describe provide metrics described by prometheus.
func (moc *MySQLOperationController) Describe(ch chan<- *prometheus.Desc) {
	moc.histogramVec.Describe(ch)
}

// Collect provide metrics collected by prometheus.
func (moc *MySQLOperationController) Collect(ch chan<- prometheus.Metric) {
	moc.histogramVec.Collect(ch)
}

// Observe record the data that about a single mysql operation. With duration time and operation name.
func (moc *MySQLOperationController) Observe(operation string, operationTimeMillisecond float64) {
	go moc.histogramVec.WithLabelValues(operation).Observe(operationTimeMillisecond)
}
