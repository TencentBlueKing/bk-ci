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
	defaultResourceStatusLabelKeys = []string{"ip", "zone", "type"}

	resourceStatusTypeTotal = "total"
	resourceStatusTypeUsed  = "used"
	resourceStatusTypeLeft  = "left"
)

// ResourceStatusLabels define the http resource status metrics labels.
type ResourceStatusLabels struct {
	IP   string
	Zone string
}

// ResourceStatusController define the controller of resource status metric.
type ResourceStatusController struct {
	cpuGaugeVec  *prometheus.GaugeVec
	memGaugeVec  *prometheus.GaugeVec
	diskGaugeVec *prometheus.GaugeVec
}

// NewResourceStatusController get a new ResourceStatusController.
func NewResourceStatusController() *ResourceStatusController {
	return &ResourceStatusController{
		cpuGaugeVec: prometheus.NewGaugeVec(prometheus.GaugeOpts{
			Name: "resource_status_cpu_gauge",
			Help: "Gauge of resource status cpu",
		}, defaultResourceStatusLabelKeys),
		memGaugeVec: prometheus.NewGaugeVec(prometheus.GaugeOpts{
			Name: "resource_status_mem_gauge",
			Help: "Gauge of resource status mem(MB)",
		}, defaultResourceStatusLabelKeys),
		diskGaugeVec: prometheus.NewGaugeVec(prometheus.GaugeOpts{
			Name: "resource_status_disk_gauge",
			Help: "Gauge of resource status disk(MB)",
		}, defaultResourceStatusLabelKeys),
	}
}

// Describe provide metrics described by prometheus.
func (rsc *ResourceStatusController) Describe(ch chan<- *prometheus.Desc) {
	rsc.cpuGaugeVec.Describe(ch)
	rsc.memGaugeVec.Describe(ch)
	rsc.diskGaugeVec.Describe(ch)
}

// Collect provide metrics collected by prometheus.
func (rsc *ResourceStatusController) Collect(ch chan<- prometheus.Metric) {
	rsc.cpuGaugeVec.Collect(ch)
	rsc.memGaugeVec.Collect(ch)
	rsc.diskGaugeVec.Collect(ch)
}

// UpdateCPUTotal set cpu total status with newest data and labels.
func (rsc *ResourceStatusController) UpdateCPUTotal(label ResourceStatusLabels, value float64) {
	rsc.cpuGaugeVec.WithLabelValues(label.IP, label.Zone, resourceStatusTypeTotal).Set(value)
}

// UpdateCPUUsed set cpu used status with newest data and labels.
func (rsc *ResourceStatusController) UpdateCPUUsed(label ResourceStatusLabels, value float64) {
	rsc.cpuGaugeVec.WithLabelValues(label.IP, label.Zone, resourceStatusTypeUsed).Set(value)
}

// UpdateCPULeft set cpu left status with newest data and labels.
func (rsc *ResourceStatusController) UpdateCPULeft(label ResourceStatusLabels, value float64) {
	rsc.cpuGaugeVec.WithLabelValues(label.IP, label.Zone, resourceStatusTypeLeft).Set(value)
}

// UpdateMemTotal set mem total status with newest data and labels.
func (rsc *ResourceStatusController) UpdateMemTotal(label ResourceStatusLabels, value float64) {
	rsc.memGaugeVec.WithLabelValues(label.IP, label.Zone, resourceStatusTypeTotal).Set(value)
}

// UpdateMemUsed set mem used status with newest data and labels.
func (rsc *ResourceStatusController) UpdateMemUsed(label ResourceStatusLabels, value float64) {
	rsc.memGaugeVec.WithLabelValues(label.IP, label.Zone, resourceStatusTypeUsed).Set(value)
}

// UpdateMemLeft set mem left status with newest data and labels.
func (rsc *ResourceStatusController) UpdateMemLeft(label ResourceStatusLabels, value float64) {
	rsc.memGaugeVec.WithLabelValues(label.IP, label.Zone, resourceStatusTypeLeft).Set(value)
}

// UpdateDiskTotal set disk total status with newest data and labels.
func (rsc *ResourceStatusController) UpdateDiskTotal(label ResourceStatusLabels, value float64) {
	rsc.diskGaugeVec.WithLabelValues(label.IP, label.Zone, resourceStatusTypeTotal).Set(value)
}

// UpdateDiskUsed set disk used status with newest data and labels.
func (rsc *ResourceStatusController) UpdateDiskUsed(label ResourceStatusLabels, value float64) {
	rsc.diskGaugeVec.WithLabelValues(label.IP, label.Zone, resourceStatusTypeUsed).Set(value)
}

// UpdateDiskLeft set disk left status with newest data and labels.
func (rsc *ResourceStatusController) UpdateDiskLeft(label ResourceStatusLabels, value float64) {
	rsc.diskGaugeVec.WithLabelValues(label.IP, label.Zone, resourceStatusTypeLeft).Set(value)
}
