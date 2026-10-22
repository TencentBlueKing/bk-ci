//go:build !loong64
// +build !loong64

package monitor

import (
	"context"
	"encoding/json"
	"errors"
	"strings"
	"testing"
	"time"
)

func TestStreamAlsoReportsSchedulingSnapshot(t *testing.T) {
	setupTestGAgentConfig(t, "git_demo", "http://example.invalid")
	calls := 0
	r := &Reporter{nowFn: time.Now, doPost: func(
		ctx context.Context, url string, headers map[string]string, body []byte,
	) (int, []byte, error) {
		calls++
		if strings.HasSuffix(url, ReportPathMetrics) {
			var envelope metricsEnvelope
			if err := json.Unmarshal(body, &envelope); err != nil || len(envelope.Metrics) != 1 {
				t.Fatalf("bad scheduling payload: %s", body)
			}
			return 503, nil, errors.New("snapshot unavailable")
		}
		return 204, nil, nil
	}}
	err := r.Report(context.Background(), []Metric{
		{Name: "mem", Fields: map[string]interface{}{"available_percent": 40}},
		{Name: "net", Fields: map[string]interface{}{"bytes_sent": 10}},
	})
	if err != nil || calls != 2 {
		t.Fatalf("snapshot failure must not stop InfluxDB: calls=%d err=%v", calls, err)
	}
}
