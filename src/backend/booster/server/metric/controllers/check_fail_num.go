package controllers

import (
	"github.com/prometheus/client_golang/prometheus"
)

type CheckFailController struct {
	gauge *prometheus.GaugeVec
}

func NewCheckFailController() *CheckFailController {
	return &CheckFailController{
		gauge: prometheus.NewGaugeVec(prometheus.GaugeOpts{
			Name: "check_fail_num_gauge",
			Help: "Gauge of check fail num with reasons.",
		}, []string{"engine", "queue", "reason"}),
	}
}

func (dnc *CheckFailController) Describe(ch chan<- *prometheus.Desc) {
	dnc.gauge.Describe(ch)
}

func (dnc *CheckFailController) Collect(ch chan<- prometheus.Metric) {
	dnc.gauge.Collect(ch)
}

func (dnc *CheckFailController) Inc(engine, queue, reason string) {
	go dnc.gauge.WithLabelValues(engine, queue, reason).Inc()
}

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
