package controllers

import (
	"github.com/prometheus/client_golang/prometheus"
)

// ElectionStatusController define the controller of election metric.
type ElectionStatusController struct {
	gauge prometheus.Gauge
}

// NewElectionStatusController get a new NewElectionStatusController.
func NewElectionStatusController() *ElectionStatusController {
	return &ElectionStatusController{
		gauge: prometheus.NewGauge(prometheus.GaugeOpts{
			Name: "election_status_gauge",
			Help: "Gauge of election status, 1 for leader & -1 for follower.",
		}),
	}
}

// Describe provide metrics described by prometheus.
func (esc *ElectionStatusController) Describe(ch chan<- *prometheus.Desc) {
	esc.gauge.Describe(ch)
}

// Collect provide metrics collected by prometheus.
func (esc *ElectionStatusController) Collect(ch chan<- prometheus.Metric) {
	esc.gauge.Collect(ch)
}

// BecomeLeader set metrics that current server become a leader.
func (esc *ElectionStatusController) BecomeLeader() {
	go esc.gauge.Set(1)
}

// BecomeFollower set metrics that current server become a follower.
func (esc *ElectionStatusController) BecomeFollower() {
	go esc.gauge.Set(-1)
}

// BecomeUnknown set metrics that current server become a unknown role.
func (esc *ElectionStatusController) BecomeUnknown() {
	go esc.gauge.Set(0)
}
