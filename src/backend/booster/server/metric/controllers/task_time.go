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

func NewTaskTimeController() *TaskTimeController {
	return &TaskTimeController{
		histogramVec: prometheus.NewHistogramVec(prometheus.HistogramOpts{
			Name:    "task_status_time_duration_histogram",
			Help:    "Bucketed histogram of task status time duration(second).",
			Buckets: defaultTaskStatusDurationTimeWidth,
		}, []string{"status"}),
	}
}

func (ttc *TaskTimeController) Describe(ch chan<- *prometheus.Desc) {
	ttc.histogramVec.Describe(ch)
}

func (ttc *TaskTimeController) Collect(ch chan<- prometheus.Metric) {
	ttc.histogramVec.Collect(ch)
}

func (ttc *TaskTimeController) Observe(status string, durationTimeSecond float64) {
	go ttc.histogramVec.WithLabelValues(status).Observe(durationTimeSecond)
}

// record running duration time
type TaskRunningTimeController struct {
	histogram prometheus.Histogram
}

func NewTaskRunningTimeController() *TaskRunningTimeController {
	return &TaskRunningTimeController{
		histogram: prometheus.NewHistogram(prometheus.HistogramOpts{
			Name:    "task_running_time_duration_histogram",
			Help:    "Bucketed histogram of task running time duration(second).",
			Buckets: defaultTaskRunningDurationTimeWidth,
		}),
	}
}

func (trt *TaskRunningTimeController) Describe(ch chan<- *prometheus.Desc) {
	trt.histogram.Describe(ch)
}

func (trt *TaskRunningTimeController) Collect(ch chan<- prometheus.Metric) {
	trt.histogram.Collect(ch)
}

func (trt *TaskRunningTimeController) Observe(durationTimeSecond float64) {
	go trt.histogram.Observe(durationTimeSecond)
}
