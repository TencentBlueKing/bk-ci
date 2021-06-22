package controllers

import (
	"github.com/prometheus/client_golang/prometheus"
)

type TaskNumController struct {
	gauge *prometheus.GaugeVec
}

func NewTaskNumController() *TaskNumController {
	return &TaskNumController{
		gauge: prometheus.NewGaugeVec(prometheus.GaugeOpts{
			Name: "task_status_num_gauge",
			Help: "Gauge of task status num with status.",
		}, []string{"engine", "queue", "status", "reason"}),
	}
}

func (tnc *TaskNumController) Describe(ch chan<- *prometheus.Desc) {
	tnc.gauge.Describe(ch)
}

func (tnc *TaskNumController) Collect(ch chan<- prometheus.Metric) {
	tnc.gauge.Collect(ch)
}

func (tnc *TaskNumController) Inc(engine, queue, status, reason string) {
	go tnc.gauge.WithLabelValues(engine, queue, status, reason).Inc()
}

func (tnc *TaskNumController) Dec(engine, queue, status, reason string) {
	go tnc.gauge.WithLabelValues(engine, queue, status, reason).Dec()
}

func (tnc *TaskNumController) Clean() {
	tnc.gauge.Reset()
}
