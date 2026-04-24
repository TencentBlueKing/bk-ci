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

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

// setupTestGAgentConfig 用测试配置填充全局 config.GAgentConfig；
// 注意：这些测试会短暂污染全局状态，t.Cleanup 恢复。
func setupTestGAgentConfig(t *testing.T, projectID, gateway string) {
	t.Helper()
	orig := config.GAgentConfig
	t.Cleanup(func() {
		config.GAgentConfig = orig
	})
	config.GAgentConfig = &config.AgentConfig{
		ProjectId: projectID,
		AgentId:   "agt-001",
		SecretKey: "secret-xyz",
		BuildType: "THIRD_PARTY",
		Gateway:   gateway,
	}
}

// fixedNow 用固定时间注入到 Reporter / Metric，避免测试受时钟影响。
var fixedNow = time.Date(2026, 4, 22, 10, 0, 0, 0, time.UTC)

func TestReporter_EncodeJSON_BasicShape(t *testing.T) {
	r := &Reporter{nowFn: func() time.Time { return fixedNow }}
	metrics := []Metric{{
		Name:      MeasurementMem,
		Tags:      map[string]string{TagHost: "h1"},
		Fields:    map[string]interface{}{RenamedFieldPctUsed: 42.5},
		Timestamp: fixedNow,
	}}
	data, err := r.encodeJSON(metrics)
	if err != nil {
		t.Fatalf("encodeJSON: %v", err)
	}

	var got metricsEnvelope
	if err := json.Unmarshal(data, &got); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if len(got.Metrics) != 1 {
		t.Fatalf("want 1 entry, got %d", len(got.Metrics))
	}
	if got.Metrics[0].Name != MeasurementMem {
		t.Errorf("name = %q", got.Metrics[0].Name)
	}
	if got.Metrics[0].Timestamp != fixedNow.Unix() {
		t.Errorf("timestamp = %d, want %d", got.Metrics[0].Timestamp, fixedNow.Unix())
	}
	if got.Metrics[0].Tags[TagHost] != "h1" {
		t.Errorf("tag host missing")
	}
	if v, _ := got.Metrics[0].Fields[RenamedFieldPctUsed].(float64); v != 42.5 {
		t.Errorf("pct_used = %v", got.Metrics[0].Fields[RenamedFieldPctUsed])
	}
}

func TestReporter_EncodeJSON_ZeroTimestampFallsBackToNow(t *testing.T) {
	r := &Reporter{nowFn: func() time.Time { return fixedNow }}
	data, err := r.encodeJSON([]Metric{{
		Name:   "x",
		Fields: map[string]interface{}{"a": 1.0},
	}})
	if err != nil {
		t.Fatal(err)
	}
	var got metricsEnvelope
	_ = json.Unmarshal(data, &got)
	if got.Metrics[0].Timestamp != fixedNow.Unix() {
		t.Errorf("zero ts should fall back to nowFn, got %d", got.Metrics[0].Timestamp)
	}
}

func TestReporter_EncodeJSON_EmptyFieldsSkipped(t *testing.T) {
	r := &Reporter{nowFn: func() time.Time { return fixedNow }}
	data, _ := r.encodeJSON([]Metric{
		{Name: "a", Fields: nil},
		{Name: "b", Fields: map[string]interface{}{}},
		{Name: "c", Fields: map[string]interface{}{"x": 1.0}},
	})
	var got metricsEnvelope
	_ = json.Unmarshal(data, &got)
	if len(got.Metrics) != 1 || got.Metrics[0].Name != "c" {
		t.Errorf("should skip empty-field metrics, got %v", got.Metrics)
	}
}

func TestReporter_EncodeLineProtocol_Deterministic(t *testing.T) {
	r := &Reporter{nowFn: func() time.Time { return fixedNow }}
	metrics := []Metric{{
		Name: "cpu",
		Tags: map[string]string{
			"cpu":  "cpu0",
			"host": "h1",
		},
		Fields: map[string]interface{}{
			"user": 1.5,
			"idle": 97.0,
		},
		Timestamp: fixedNow,
	}}
	got := string(r.encodeLineProtocol(metrics))
	// 期望：measurement + 按 key 升序 tag + 空格 + 按 key 升序 field + 空格 + ns
	want := "cpu,cpu=cpu0,host=h1 idle=97,user=1.5 " + itoa(fixedNow.UnixNano()) + "\n"
	if got != want {
		t.Errorf("line protocol mismatch\n got  %q\n want %q", got, want)
	}
}

func TestReporter_EncodeLineProtocol_IntegerSuffixI(t *testing.T) {
	r := &Reporter{nowFn: func() time.Time { return fixedNow }}
	got := string(r.encodeLineProtocol([]Metric{{
		Name:      "m",
		Fields:    map[string]interface{}{"a": int64(5), "b": uint64(7), "c": 1.5},
		Timestamp: fixedNow,
	}}))
	if !strings.Contains(got, "a=5i") {
		t.Errorf("int should have i suffix, got %q", got)
	}
	if !strings.Contains(got, "b=7i") {
		t.Errorf("uint should have i suffix, got %q", got)
	}
	if strings.Contains(got, "c=1.5i") || !strings.Contains(got, "c=1.5") {
		t.Errorf("float should NOT have i suffix, got %q", got)
	}
}

func TestReporter_EncodeLineProtocol_EscapesSpecialChars(t *testing.T) {
	r := &Reporter{nowFn: func() time.Time { return fixedNow }}
	got := string(r.encodeLineProtocol([]Metric{{
		Name:      "a,b c",
		Tags:      map[string]string{"k=1": "v,v"},
		Fields:    map[string]interface{}{"f": 1.0},
		Timestamp: fixedNow,
	}}))
	// measurement 转义 , 和 空格
	if !strings.HasPrefix(got, `a\,b\ c,`) {
		t.Errorf("measurement escape wrong, got %q", got)
	}
	// tag key 的 = 和 value 的 , 都要转义
	if !strings.Contains(got, `k\=1=v\,v`) {
		t.Errorf("tag escape wrong, got %q", got)
	}
}

func TestReporter_Report_PostCIProjectJSON(t *testing.T) {
	setupTestGAgentConfig(t, "bkci-demo", "http://bkci.example.com")

	var capturedURL string
	var capturedHeaders map[string]string
	var capturedBody []byte
	r := &Reporter{
		nowFn: func() time.Time { return fixedNow },
		doPost: func(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error) {
			capturedURL = url
			capturedHeaders = headers
			capturedBody = body
			return 200, nil, nil
		},
	}
	err := r.Report(context.Background(), []Metric{{
		Name:      MeasurementCPU,
		Fields:    map[string]interface{}{RenamedFieldUser: 1.0},
		Timestamp: fixedNow,
	}})
	if err != nil {
		t.Fatalf("Report: %v", err)
	}

	if !strings.HasSuffix(capturedURL, ReportPathMetrics) {
		t.Errorf("ci project should POST to %s, got %s", ReportPathMetrics, capturedURL)
	}
	if capturedHeaders["Content-Type"] != ContentTypeJSON {
		t.Errorf("Content-Type = %q", capturedHeaders["Content-Type"])
	}
	// 认证 header 必须在
	for _, k := range []string{HeaderBuildType, HeaderProjectID, HeaderAgentID, HeaderSecretKey} {
		if _, ok := capturedHeaders[k]; !ok {
			t.Errorf("missing header %q, have %v", k, capturedHeaders)
		}
	}
	// body 合法 JSON 对象 {"metrics":[...]}（对齐后端 TelegrafMulData）
	var env metricsEnvelope
	if err := json.Unmarshal(capturedBody, &env); err != nil {
		t.Errorf("body not JSON envelope: %v, body=%s", err, string(capturedBody))
	}
	if len(env.Metrics) != 1 || env.Metrics[0].Name != MeasurementCPU {
		t.Errorf("envelope.metrics unexpected: %+v", env.Metrics)
	}
}

func TestReporter_Report_PostStreamProjectLineProtocol(t *testing.T) {
	setupTestGAgentConfig(t, StreamProjectPrefix+"stream-demo", "http://bkci.example.com")

	var capturedURL string
	var capturedContentType string
	var capturedBody []byte
	r := &Reporter{
		nowFn: func() time.Time { return fixedNow },
		doPost: func(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error) {
			capturedURL = url
			capturedContentType = headers["Content-Type"]
			capturedBody = body
			return 204, nil, nil
		},
	}
	err := r.Report(context.Background(), []Metric{{
		Name:      MeasurementCPU,
		Fields:    map[string]interface{}{RenamedFieldUser: 1.0},
		Timestamp: fixedNow,
	}})
	if err != nil {
		t.Fatalf("Report: %v", err)
	}
	if !strings.HasSuffix(capturedURL, ReportPathMetrix) {
		t.Errorf("stream project should POST to %s, got %s", ReportPathMetrix, capturedURL)
	}
	if capturedContentType != ContentTypeLineProtocol {
		t.Errorf("Content-Type = %q", capturedContentType)
	}
	if !strings.HasPrefix(string(capturedBody), "cpu ") {
		t.Errorf("line protocol body should start with 'cpu ', got %q", string(capturedBody))
	}
}

func TestReporter_Report_NonOKStatusReturnsError(t *testing.T) {
	setupTestGAgentConfig(t, "bkci", "http://bkci.example.com")
	r := &Reporter{
		nowFn: func() time.Time { return fixedNow },
		doPost: func(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error) {
			return 500, []byte("oops"), nil
		},
	}
	err := r.Report(context.Background(), []Metric{{
		Name:   "cpu",
		Fields: map[string]interface{}{"user": 1.0},
	}})
	if err == nil {
		t.Fatal("expected error on 500")
	}
	if !strings.Contains(err.Error(), "non-2xx") {
		t.Errorf("error should mention non-2xx, got %v", err)
	}
}

func TestReporter_Report_TransportErrorPropagates(t *testing.T) {
	setupTestGAgentConfig(t, "bkci", "http://bkci.example.com")
	sentinel := errors.New("dial tcp: boom")
	r := &Reporter{
		nowFn: func() time.Time { return fixedNow },
		doPost: func(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error) {
			return 0, nil, sentinel
		},
	}
	err := r.Report(context.Background(), []Metric{{
		Name:   "cpu",
		Fields: map[string]interface{}{"user": 1.0},
	}})
	if err == nil || !errors.Is(err, sentinel) {
		t.Errorf("transport error should propagate, got %v", err)
	}
}

func TestReporter_Report_EmptyMetricsSkipsHTTP(t *testing.T) {
	setupTestGAgentConfig(t, "bkci", "http://bkci.example.com")
	called := false
	r := &Reporter{
		nowFn: func() time.Time { return fixedNow },
		doPost: func(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error) {
			called = true
			return 200, nil, nil
		},
	}
	if err := r.Report(context.Background(), nil); err != nil {
		t.Fatalf("Report(nil): %v", err)
	}
	if err := r.Report(context.Background(), []Metric{}); err != nil {
		t.Fatalf("Report([]): %v", err)
	}
	if called {
		t.Error("doPost should not be called for empty metrics")
	}
}

func TestReporter_Report_EmptyGatewayReturnsError(t *testing.T) {
	setupTestGAgentConfig(t, "bkci", "")
	r := &Reporter{
		nowFn: func() time.Time { return fixedNow },
		doPost: func(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error) {
			t.Fatal("should not POST when gateway empty")
			return 0, nil, nil
		},
	}
	err := r.Report(context.Background(), []Metric{{Name: "cpu", Fields: map[string]interface{}{"x": 1.0}}})
	if err == nil {
		t.Fatal("expected error when gateway empty")
	}
}

func TestBuildGateway_AddsHTTPPrefix(t *testing.T) {
	setupTestGAgentConfig(t, "bkci", "bkci.example.com")
	if gw := buildGateway(); gw != "http://bkci.example.com" {
		t.Errorf("buildGateway = %q, want http:// prefix", gw)
	}
}

func TestBuildGateway_KeepsHTTPSPrefix(t *testing.T) {
	setupTestGAgentConfig(t, "bkci", "https://bkci.example.com")
	if gw := buildGateway(); gw != "https://bkci.example.com" {
		t.Errorf("buildGateway = %q, must not double-prefix", gw)
	}
}

func TestFormatFieldValue(t *testing.T) {
	cases := []struct {
		in   interface{}
		want string
	}{
		{1.5, "1.5"},
		{float32(2.5), "2.5"},
		{int(3), "3i"},
		{int64(-4), "-4i"},
		{uint64(5), "5i"},
		{true, "true"},
		{false, "false"},
		{"hi", `"hi"`},
		{`a"b`, `"a\"b"`},
	}
	for _, c := range cases {
		if got := formatFieldValue(c.in); got != c.want {
			t.Errorf("formatFieldValue(%v) = %q, want %q", c.in, got, c.want)
		}
	}
}

// itoa 因为 reporter_test 里 import strconv 会与 reporter.go 重复，这里
// 给出一个简单的 helper，避免测试文件重复 import 时触发 goimports 警告。
func itoa(v int64) string {
	if v == 0 {
		return "0"
	}
	buf := [20]byte{}
	i := len(buf)
	neg := v < 0
	if neg {
		v = -v
	}
	for v > 0 {
		i--
		buf[i] = byte('0' + v%10)
		v /= 10
	}
	if neg {
		i--
		buf[i] = '-'
	}
	return string(buf[i:])
}
