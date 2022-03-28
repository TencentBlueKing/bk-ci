/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package metric

import (
	"fmt"
	"net/http"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

const (
	metricURL = "/metrics"
)

// Config describe the metric server configs.
type Config struct {
	IP         string
	MetricPort uint
}

// NewMetricServer get a new http server for metrics data.
func NewMetricServer(c *Config, collectors ...prometheus.Collector) (*Server, error) {
	registry := prometheus.NewRegistry()
	collectors = append([]prometheus.Collector{prometheus.NewGoCollector()}, collectors...)

	for _, collector := range collectors {
		if err := registry.Register(collector); err != nil {
			return nil, err
		}
	}

	handler := http.NewServeMux()
	handler.Handle(metricURL, promhttp.HandlerFor(registry, promhttp.HandlerOpts{Timeout: 10 * time.Second}))

	return &Server{
		config:   c,
		registry: registry,
		handler:  handler,
	}, nil
}

// Server describe the metric server.
type Server struct {
	config   *Config
	registry *prometheus.Registry
	handler  http.Handler
}

// Start brings up the metric server and bind to address and port.
func (s *Server) Start() error {
	return http.ListenAndServe(fmt.Sprintf("%s:%d", s.config.IP, s.config.MetricPort), s.handler)
}
