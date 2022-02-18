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
	"github.com/Tencent/bk-ci/src/booster/common/codec"

	"github.com/prometheus/client_golang/prometheus"
)

var (
	defaultHttpRequestResponseTimeWidth = []float64{5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000}
	defaultHttpRequestLabelKeys         = []string{"code", "method", "path"}
)

// HttpRequestLabel define the http request metrics labels.
type HttpRequestLabel struct {
	Code   string `json:"code"`
	Method string `json:"method"`
	Path   string `json:"path"`
}

// GetLabels return the labels data as prometheus.Labels
func (hrl *HttpRequestLabel) GetLabels() prometheus.Labels {
	var json []byte
	_ = codec.EncJSON(*hrl, &json)

	var labels prometheus.Labels
	_ = codec.DecJSON(json, &labels)
	return labels
}

// HttpRequestController define the controller of http request metric.
type HttpRequestController struct {
	histogramVec *prometheus.HistogramVec
}

// NewHttpRequestController get a new HttpRequestController.
func NewHttpRequestController(responseTimeWidth []float64) *HttpRequestController {
	if responseTimeWidth == nil {
		responseTimeWidth = defaultHttpRequestResponseTimeWidth
	}

	return &HttpRequestController{
		histogramVec: prometheus.NewHistogramVec(prometheus.HistogramOpts{
			Name:    "http_request_duration_milliseconds",
			Help:    "Bucketed histogram of http request duration(millisecond).",
			Buckets: responseTimeWidth,
		}, defaultHttpRequestLabelKeys),
	}
}

// Describe provide metrics described by prometheus.
func (hrc *HttpRequestController) Describe(ch chan<- *prometheus.Desc) {
	hrc.histogramVec.Describe(ch)
}

// Collect provide metrics collected by prometheus.
func (hrc *HttpRequestController) Collect(ch chan<- prometheus.Metric) {
	hrc.histogramVec.Collect(ch)
}

// Observe record the data that about a single http request. With duration time and labels.
func (hrc *HttpRequestController) Observe(label HttpRequestLabel, responseTimeMillisecond float64) {
	go hrc.histogramVec.With(label.GetLabels()).Observe(responseTimeMillisecond)
}
