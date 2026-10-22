//go:build windows
// +build windows

package monitor

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"os"
	"sync/atomic"
	"testing"
	"time"
)

type fakeLHMClient struct {
	readings []lhmReading
	err      error
	block    chan struct{}
	calls    atomic.Int32
}

func (f *fakeLHMClient) Readings(context.Context) ([]lhmReading, error) {
	f.calls.Add(1)
	if f.block != nil {
		<-f.block
	}
	return f.readings, f.err
}

// lhmDataJSONFixture 精简自 LHM 0.9.6 真实 data.json，覆盖：
// 多级硬件、子硬件（SuperIO）、阈值类传感器、非温度传感器、"-" 值、逗号小数、华氏度。
const lhmDataJSONFixture = `{
  "id": 0, "Text": "Sensor", "Children": [
    {"id": 1, "Text": "HOST", "ImageURL": "images_icon/computer.png", "Children": [
      {"id": 2, "Text": "HP 895C", "HardwareId": "/motherboard", "Children": [
        {"id": 3, "Text": "Nuvoton NCT6687D", "HardwareId": "/lpc/nct6687d/0", "Children": [
          {"id": 4, "Text": "Temperatures", "Children": [
            {"id": 5, "Text": "System", "Value": "35,5 °C", "SensorId": "/lpc/nct6687d/0/temperature/0", "Type": "Temperature", "Children": []},
            {"id": 6, "Text": "VRM", "Value": "-", "SensorId": "/lpc/nct6687d/0/temperature/1", "Type": "Temperature", "Children": []}
          ]}
        ]}
      ]},
      {"id": 10, "Text": "Intel Core i9-14900K", "HardwareId": "/intelcpu/0", "Children": [
        {"id": 11, "Text": "Temperatures", "Children": [
          {"id": 12, "Text": "CPU Package", "Value": "67.0 °C", "SensorId": "/intelcpu/0/temperature/26", "Type": "Temperature", "Children": []},
          {"id": 13, "Text": "P-Core #1 Distance to TjMax", "Value": "39.0 °C", "SensorId": "/intelcpu/0/temperature/27", "Type": "Temperature", "Children": []}
        ]},
        {"id": 14, "Text": "Load", "Children": [
          {"id": 15, "Text": "CPU Total", "Value": "12.0 %", "SensorId": "/intelcpu/0/load/0", "Type": "Load", "Children": []}
        ]}
      ]},
      {"id": 20, "Text": "Samsung DIMM (#0)", "HardwareId": "/memory/dimm/0", "Children": [
        {"id": 21, "Text": "Temperatures", "Children": [
          {"id": 22, "Text": "DIMM #0", "Value": "40.5 °C", "SensorId": "/memory/dimm/0/temperature/0", "Type": "Temperature", "Children": []},
          {"id": 23, "Text": "Temperature Sensor Resolution", "Value": "0.3 °C", "SensorId": "/memory/dimm/0/temperature/1", "Type": "Temperature", "Children": []},
          {"id": 24, "Text": "Thermal Sensor High Limit", "Value": "55.0 °C", "SensorId": "/memory/dimm/0/temperature/3", "Type": "Temperature", "Children": []}
        ]}
      ]},
      {"id": 30, "Text": "NVIDIA GeForce RTX 4080 SUPER", "HardwareId": "/gpu-nvidia/0", "Children": [
        {"id": 31, "Text": "Temperatures", "Children": [
          {"id": 32, "Text": "GPU Core", "Value": "102.2 °F", "SensorId": "/gpu-nvidia/0/temperature/0", "Type": "Temperature", "Children": []}
        ]}
      ]},
      {"id": 40, "Text": "KIOXIA", "HardwareId": "/nvme/0", "Children": [
        {"id": 41, "Text": "Temperatures", "Children": [
          {"id": 42, "Text": "Composite Temperature", "Value": "37.0 °C", "SensorId": "/nvme/0/temperature/0", "Type": "Temperature", "Children": []},
          {"id": 43, "Text": "Warning Temperature", "Value": "78.0 °C", "SensorId": "/nvme/0/temperature/10", "Type": "Temperature", "Children": []},
          {"id": 44, "Text": "Critical Temperature", "Value": "83.0 °C", "SensorId": "/nvme/0/temperature/11", "Type": "Temperature", "Children": []}
        ]}
      ]}
    ]}
  ]
}`

func TestHWTemp_GatherHTTP(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/data.json" {
			http.NotFound(w, r)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(lhmDataJSONFixture))
	}))
	defer srv.Close()

	ts := time.Unix(1700000000, 0)
	h := newHWTempWithClient(newHTTPLHMClient(srv.URL+"/data.json"), lhmSourceHTTP)
	h.nowFn = func() time.Time { return ts }

	ms, err := h.Gather(context.Background())
	if err != nil {
		t.Fatalf("unexpected err: %v", err)
	}

	type want struct {
		name, hw, hwType string
		v                float64
	}
	wants := map[string]want{
		"/gpu-nvidia/0/temperature/0":   {"GPU Core", "NVIDIA GeForce RTX 4080 SUPER", "GpuNvidia", 39.0},
		"/intelcpu/0/temperature/26":    {"CPU Package", "Intel Core i9-14900K", "Cpu", 67.0},
		"/lpc/nct6687d/0/temperature/0": {"System", "Nuvoton NCT6687D", "SuperIO", 35.5},
		"/memory/dimm/0/temperature/0":  {"DIMM #0", "Samsung DIMM (#0)", "Memory", 40.5},
		"/nvme/0/temperature/0":         {"Composite Temperature", "KIOXIA", "Storage", 37.0},
	}
	if len(ms) != len(wants) {
		for _, m := range ms {
			t.Logf("got %s %v", m.Tags[TagSensor], m.Fields)
		}
		t.Fatalf("want %d metrics, got %d", len(wants), len(ms))
	}
	for i, m := range ms {
		if i > 0 && ms[i-1].Tags[TagSensor] >= m.Tags[TagSensor] {
			t.Errorf("metrics not sorted at %d", i)
		}
		w, ok := wants[m.Tags[TagSensor]]
		if !ok {
			t.Errorf("unexpected sensor %s", m.Tags[TagSensor])
			continue
		}
		if m.Name != RenamedEnv || !m.Timestamp.Equal(ts) {
			t.Errorf("%s: bad name/ts %s %v", m.Tags[TagSensor], m.Name, m.Timestamp)
		}
		if m.Tags[TagSensorName] != w.name || m.Tags[TagHardware] != w.hw || m.Tags[TagHardwareType] != w.hwType {
			t.Errorf("%s: tags = %+v, want %+v", m.Tags[TagSensor], m.Tags, w)
		}
		if m.Fields[FieldTemperature] != w.v || len(m.Fields) != 1 {
			t.Errorf("%s: fields = %v, want only %s=%v", m.Tags[TagSensor], m.Fields, FieldTemperature, w.v)
		}
	}
}

func TestHTTPLHMClient_Errors(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.URL.Path {
		case "/500":
			w.WriteHeader(http.StatusInternalServerError)
		default:
			_, _ = w.Write([]byte("not json"))
		}
	}))
	defer srv.Close()
	for _, p := range []string{"/500", "/bad"} {
		if _, err := newHTTPLHMClient(srv.URL + p).Readings(context.Background()); err == nil {
			t.Errorf("%s: want error", p)
		}
	}
	// 端口未监听
	addr := srv.URL
	srv.Close()
	if _, err := newHTTPLHMClient(addr + "/data.json").Readings(context.Background()); err == nil {
		t.Error("closed server: want error")
	}
}

func TestParseLHMTemp(t *testing.T) {
	cases := []struct {
		in   string
		want float64
		ok   bool
	}{
		{"64.0 °C", 64.0, true},
		{"64,5 °C", 64.5, true},
		{"212 °F", 100, true},
		{"-5.5 °C", -5.5, true},
		{"-", 0, false},
		{"", 0, false},
		{"abc", 0, false},
	}
	for _, c := range cases {
		v, ok := parseLHMTemp(c.in)
		if ok != c.ok || (ok && roundTo(v, 3) != c.want) {
			t.Errorf("parseLHMTemp(%q) = %v %v, want %v %v", c.in, v, ok, c.want, c.ok)
		}
	}
}

func TestLHMHardwareType(t *testing.T) {
	cases := map[string]string{
		"/intelcpu/0":     "Cpu",
		"/amdcpu/0":       "Cpu",
		"/gpu-nvidia/0":   "GpuNvidia",
		"/nvme/1":         "Storage",
		"/memory/dimm/0":  "Memory",
		"/motherboard":    "Motherboard",
		"/lpc/nct6687d/0": "SuperIO",
		"/foo/0":          "foo",
		"":                "",
	}
	for in, want := range cases {
		if got := lhmHardwareType(in); got != want {
			t.Errorf("lhmHardwareType(%q) = %q, want %q", in, got, want)
		}
	}
}

func TestWMISensorsToReadings(t *testing.T) {
	rs := wmiSensorsToReadings([]lhmWMISensor{
		{Identifier: "/amdcpu/0/temperature/2", Name: "Core (Tctl/Tdie)", SensorType: "Temperature", Parent: "/amdcpu/0", Value: 61.500003},
		{Identifier: "/nvme/0/temperature/0", Name: "Temperature", SensorType: "Temperature", Parent: "/nvme/0", Value: 41},
		{Identifier: "/amdcpu/0/load/0", Name: "CPU Total", SensorType: "Load", Parent: "/amdcpu/0", Value: 12},
	}, map[string]lhmWMIHardware{
		"/amdcpu/0": {Identifier: "/amdcpu/0", Name: "AMD Ryzen 9 5950X", HardwareType: "Cpu"},
	})
	if len(rs) != 2 {
		t.Fatalf("want 2 readings, got %d", len(rs))
	}
	if rs[0].Hardware != "AMD Ryzen 9 5950X" || rs[0].HardwareType != "Cpu" || roundTo(rs[0].Value, 1) != 61.5 {
		t.Errorf("unexpected cpu reading %+v", rs[0])
	}
	if rs[1].Hardware != "" {
		t.Errorf("nvme should have no hardware: %+v", rs[1])
	}
}

func TestHWTemp_Gather_NoHardwareTag(t *testing.T) {
	c := &fakeLHMClient{readings: []lhmReading{{Identifier: "/a/0/temperature/0", Name: "X", Value: 50}}}
	ms, err := newHWTempWithClient(c, "fake").Gather(context.Background())
	if err != nil || len(ms) != 1 {
		t.Fatalf("want 1 metric nil err, got %d %v", len(ms), err)
	}
	if _, ok := ms[0].Tags[TagHardware]; ok {
		t.Errorf("empty hardware should not produce tag: %+v", ms[0].Tags)
	}
	if _, ok := ms[0].Tags[TagHardwareType]; ok {
		t.Errorf("empty hardware_type should not produce tag: %+v", ms[0].Tags)
	}
}

func TestHWTemp_Gather_FailureSuppressed(t *testing.T) {
	c := &fakeLHMClient{err: errors.New("connection refused")}
	h := newHWTempWithClient(c, "fake")
	for i := 0; i < 2; i++ {
		ms, err := h.Gather(context.Background())
		if err != nil || ms != nil {
			t.Fatalf("failure should be swallowed, got %v %v", ms, err)
		}
	}
	if !h.failing {
		t.Fatal("failing flag should be set")
	}
	c.err = nil
	c.readings = []lhmReading{{Identifier: "/a/0/temperature/0", Value: 50}}
	if ms, _ := h.Gather(context.Background()); len(ms) != 1 {
		t.Fatalf("should recover, got %d", len(ms))
	}
	if h.failing {
		t.Fatal("failing flag should be cleared")
	}
}

// 调用方的 ctx 到期后 HTTP 请求应被中断，Gather 及时返回且吞掉错误。
func TestHWTemp_Gather_HTTPHonorsCtx(t *testing.T) {
	release := make(chan struct{})
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		select {
		case <-release:
		case <-r.Context().Done():
		}
	}))
	defer srv.Close()
	defer close(release)

	h := newHWTempWithClient(newHTTPLHMClient(srv.URL+"/data.json"), lhmSourceHTTP)
	ctx, cancel := context.WithTimeout(context.Background(), 50*time.Millisecond)
	defer cancel()
	start := time.Now()
	ms, err := h.Gather(ctx)
	if err != nil || ms != nil {
		t.Fatalf("timeout should be swallowed, got %v %v", ms, err)
	}
	if time.Since(start) > time.Second {
		t.Fatal("HTTP Gather did not honor ctx")
	}
}

func TestReadFiltered(t *testing.T) {
	if _, err := readFiltered(context.Background(), &fakeLHMClient{err: errors.New("x")}); err == nil {
		t.Error("query error should fail")
	}
	if _, err := readFiltered(context.Background(), &fakeLHMClient{}); err == nil {
		t.Error("no sensor should fail")
	}
	onlyThreshold := &fakeLHMClient{readings: []lhmReading{{Identifier: "x", Name: "Warning Temperature"}}}
	if _, err := readFiltered(context.Background(), onlyThreshold); err == nil {
		t.Error("only threshold sensors should fail")
	}
	rs, err := readFiltered(context.Background(), &fakeLHMClient{readings: []lhmReading{{Identifier: "x", Name: "CPU Package"}}})
	if err != nil || len(rs) != 1 {
		t.Errorf("want 1 nil, got %d %v", len(rs), err)
	}
}

func TestHWTemp_Detect(t *testing.T) {
	okReadings := []lhmReading{{Identifier: "x", Name: "CPU Package", Value: 50}}

	t.Run("unavailable twice disables for lifetime", func(t *testing.T) {
		bad1 := &fakeLHMClient{err: errors.New("down")}
		bad2 := &fakeLHMClient{err: errors.New("down")}
		h := newHWTemp([]lhmCandidate{{lhmSourceHTTP, bad1}, {lhmSourceWMI, bad2}})
		now := time.Unix(1700000000, 0)
		h.nowFn = func() time.Time { return now }

		// 第 1 次探测失败：不禁用，安排 10 分钟后重探
		if ms, err := h.Gather(context.Background()); ms != nil || err != nil {
			t.Fatalf("want nil nil, got %v %v", ms, err)
		}
		if h.disabled || h.probes != 1 || !h.nextProbe.Equal(now.Add(lhmReprobeInterval)) {
			t.Fatalf("after 1st probe: disabled=%v probes=%d next=%v", h.disabled, h.probes, h.nextProbe)
		}
		// 未到重探时间的轮次直接跳过，不发起查询
		for i := 0; i < 9; i++ {
			now = now.Add(time.Minute)
			_, _ = h.Gather(context.Background())
		}
		if bad1.calls.Load() != 1 || bad2.calls.Load() != 1 {
			t.Fatalf("should not probe before interval, got %d %d", bad1.calls.Load(), bad2.calls.Load())
		}
		// 到点第 2 次探测仍失败：禁用
		now = now.Add(time.Minute)
		_, _ = h.Gather(context.Background())
		if !h.disabled || h.probes != lhmMaxProbes {
			t.Fatalf("after 2nd probe: disabled=%v probes=%d", h.disabled, h.probes)
		}
		// 之后不再探测
		now = now.Add(time.Hour)
		_, _ = h.Gather(context.Background())
		if bad1.calls.Load() != 2 || bad2.calls.Load() != 2 {
			t.Fatalf("each candidate should be probed twice, got %d %d", bad1.calls.Load(), bad2.calls.Load())
		}
	})

	t.Run("second probe succeeds", func(t *testing.T) {
		httpC := &fakeLHMClient{err: errors.New("down")}
		h := newHWTemp([]lhmCandidate{{lhmSourceHTTP, httpC}, {lhmSourceWMI, &fakeLHMClient{err: errors.New("down")}}})
		now := time.Unix(1700000000, 0)
		h.nowFn = func() time.Time { return now }
		_, _ = h.Gather(context.Background())

		// LHM 在 agent 之后启动
		httpC.err, httpC.readings = nil, okReadings
		now = now.Add(lhmReprobeInterval)
		ms, err := h.Gather(context.Background())
		if err != nil || len(ms) != 1 || h.source != lhmSourceHTTP || h.disabled {
			t.Fatalf("want 1 metric via http, got %d %v %s disabled=%v", len(ms), err, h.source, h.disabled)
		}
		now = now.Add(time.Minute)
		if ms, _ = h.Gather(context.Background()); len(ms) != 1 {
			t.Fatalf("locked source should keep reporting, got %d", len(ms))
		}
	})

	t.Run("fallback to wmi and emit on first gather", func(t *testing.T) {
		ok := &fakeLHMClient{readings: okReadings}
		h := newHWTemp([]lhmCandidate{{lhmSourceHTTP, &fakeLHMClient{err: errors.New("down")}}, {lhmSourceWMI, ok}})
		ms, err := h.Gather(context.Background())
		if err != nil || len(ms) != 1 || h.source != lhmSourceWMI {
			t.Fatalf("want 1 metric via wmi, got %d %v %s", len(ms), err, h.source)
		}
		if ok.calls.Load() != 1 {
			t.Fatalf("first gather should reuse detection readings, calls=%d", ok.calls.Load())
		}
		if ms, _ = h.Gather(context.Background()); len(ms) != 1 || ok.calls.Load() != 2 {
			t.Fatalf("second gather should read locked source, got %d calls=%d", len(ms), ok.calls.Load())
		}
	})

	t.Run("http takes priority", func(t *testing.T) {
		wmiC := &fakeLHMClient{readings: okReadings}
		h := newHWTemp([]lhmCandidate{{lhmSourceHTTP, &fakeLHMClient{readings: okReadings}}, {lhmSourceWMI, wmiC}})
		if _, _ = h.Gather(context.Background()); h.source != lhmSourceHTTP {
			t.Fatalf("want http, got %s", h.source)
		}
		if wmiC.calls.Load() != 0 {
			t.Fatal("wmi should not be probed when http is available")
		}
	})

	t.Run("deadline exhausted counts as a probe", func(t *testing.T) {
		ok := &fakeLHMClient{readings: okReadings}
		h := newHWTemp([]lhmCandidate{{lhmSourceHTTP, ctxAwareClient{}}, {lhmSourceWMI, ok}})
		now := time.Unix(1700000000, 0)
		h.nowFn = func() time.Time { return now }
		expired, cancel := context.WithCancel(context.Background())
		cancel()
		ok.err = expired.Err()
		if ms, _ := h.Gather(expired); ms != nil {
			t.Fatalf("want nil, got %v", ms)
		}
		if h.disabled || h.client != nil || h.probes != 1 || !h.nextProbe.Equal(now.Add(lhmReprobeInterval)) {
			t.Fatalf("deadline should count as one probe: disabled=%v probes=%d next=%v", h.disabled, h.probes, h.nextProbe)
		}
		// 窗口内不再探测，避免慢数据源每分钟被探测一次
		ok.err = nil
		now = now.Add(time.Minute)
		if ms, _ := h.Gather(context.Background()); ms != nil || ok.calls.Load() != 1 {
			t.Fatalf("should skip within interval, got %v calls=%d", ms, ok.calls.Load())
		}
		now = now.Add(lhmReprobeInterval)
		if ms, _ := h.Gather(context.Background()); len(ms) != 1 || h.source != lhmSourceWMI {
			t.Fatalf("should detect on 2nd probe, got %d %s", len(ms), h.source)
		}
	})

	t.Run("deadline exhausted twice disables", func(t *testing.T) {
		h := newHWTemp([]lhmCandidate{{lhmSourceHTTP, ctxAwareClient{}}, {lhmSourceWMI, ctxAwareClient{}}})
		now := time.Unix(1700000000, 0)
		h.nowFn = func() time.Time { return now }
		expired, cancel := context.WithCancel(context.Background())
		cancel()
		_, _ = h.Gather(expired)
		now = now.Add(lhmReprobeInterval)
		_, _ = h.Gather(expired)
		if !h.disabled || h.probes != lhmMaxProbes {
			t.Fatalf("want disabled after %d probes, got disabled=%v probes=%d", lhmMaxProbes, h.disabled, h.probes)
		}
	})
}

// ctxAwareClient 只返回 ctx 错误，模拟被 deadline 打断的 HTTP 请求。
type ctxAwareClient struct{}

func (ctxAwareClient) Readings(ctx context.Context) ([]lhmReading, error) {
	if err := ctx.Err(); err != nil {
		return nil, err
	}
	return nil, errors.New("down")
}

// WMI 查询卡死（不响应 ctx）时：Gather 不返回，inputRunner 保持 running，
// 后续轮次跳过，整个过程只发起一次查询，不会每轮泄漏一个 goroutine。
// 覆盖“已锁定数据源”和“首轮探测”两种路径。
func TestHWTemp_StuckIsNotReentered(t *testing.T) {
	orig := perInputTimeout
	t.Cleanup(func() { perInputTimeout = orig })
	perInputTimeout = 30 * time.Millisecond

	cases := map[string]func(c lhmClient) *HWTemp{
		"locked": func(c lhmClient) *HWTemp { return newHWTempWithClient(c, lhmSourceWMI) },
		"detecting": func(c lhmClient) *HWTemp {
			return newHWTemp([]lhmCandidate{{lhmSourceHTTP, &fakeLHMClient{err: errors.New("down")}}, {lhmSourceWMI, c}})
		},
	}
	for name, mk := range cases {
		t.Run(name, func(t *testing.T) {
			stuck := &fakeLHMClient{block: make(chan struct{})}
			good := &stubInput{
				name:    "mem",
				metrics: []Metric{{Name: "mem", Fields: map[string]interface{}{"x": 1.0}}},
			}
			runners := newInputRunners([]Input{mk(stuck), good})

			for i := 0; i < 3; i++ {
				start := time.Now()
				if ms := gatherAll(context.Background(), runners); len(ms) != 1 {
					t.Fatalf("round %d: healthy input should still report, got %d", i, len(ms))
				}
				if time.Since(start) > time.Second {
					t.Fatalf("round %d blocked by stuck hwtemp", i)
				}
			}
			if got := stuck.calls.Load(); got != 1 {
				t.Fatalf("stuck query should be issued once, got %d", got)
			}
			if !runners[0].running.Load() {
				t.Fatal("stuck runner should stay running")
			}

			close(stuck.block)
			deadline := time.Now().Add(time.Second)
			for runners[0].running.Load() && time.Now().Before(deadline) {
				time.Sleep(time.Millisecond)
			}
			if runners[0].running.Load() {
				t.Fatal("runner did not return to idle after release")
			}
		})
	}
}

// TestHWTemp_SurvivesRename 温度行经过 Rename（内部版恒等；out+windows 下
// env -> kernel）后，temperature 字段与传感器维度保持不变。
func TestHWTemp_SurvivesRename(t *testing.T) {
	c := &fakeLHMClient{readings: []lhmReading{{Identifier: "/intelcpu/0/temperature/26", Name: "CPU Package", Hardware: "CPU", HardwareType: "Cpu", Value: 67}}}
	ms, _ := newHWTempWithClient(c, "fake").Gather(context.Background())
	out := Rename(ms)
	if len(out) != 1 {
		t.Fatalf("want 1, got %d", len(out))
	}
	if out[0].Fields[FieldTemperature] != 67.0 || len(out[0].Fields) != 1 {
		t.Errorf("fields changed by rename: %v", out[0].Fields)
	}
	for _, k := range []string{TagSensor, TagSensorName, TagHardware, TagHardwareType} {
		if out[0].Tags[k] == "" {
			t.Errorf("tag %s dropped by rename: %v", k, out[0].Tags)
		}
	}
}

// TestHWTemp_RealLHM 连接本机真实 LHM，默认跳过。
// 运行：$env:LHM_REAL_TEST="1"; go test ./src/pkg/monitor -run TestHWTemp_RealLHM -v
func TestHWTemp_RealLHM(t *testing.T) {
	if os.Getenv("LHM_REAL_TEST") == "" {
		t.Skip("set LHM_REAL_TEST=1 to run against local LibreHardwareMonitor")
	}
	h := newHWTemp(lhmCandidatesFn())
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	ms, err := h.Gather(ctx)
	if err != nil || len(ms) == 0 {
		t.Fatalf("LibreHardwareMonitor not detected or gather failed: %d %v", len(ms), err)
	}
	t.Logf("source=%s metrics=%d", h.source, len(ms))
	for _, m := range ms {
		t.Logf("%s,%s=%s,%s=%s,%s=%q,%s=%q %s=%v",
			m.Name, TagSensor, m.Tags[TagSensor], TagHardwareType, m.Tags[TagHardwareType],
			TagHardware, m.Tags[TagHardware], TagSensorName, m.Tags[TagSensorName], FieldTemperature, m.Fields[FieldTemperature])
	}
}
