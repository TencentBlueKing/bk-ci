//go:build windows
// +build windows

package monitor

import (
	"context"
	"encoding/json"
	"io"
	"math"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/yusufpapurcu/wmi"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

// input_hwtemp_win.go 通过 LibreHardwareMonitor（下称 LHM）采集 Windows 机器的
// 硬件温度（CPU / GPU / 内存 / 主板 / 磁盘等）。
//
// 为什么依赖 LHM 而不是自己读传感器：Windows 下 CPU 核心温度需要内核驱动
// 读 MSR / SMBus，纯 Go 用户态拿不到。
//
// 数据源（首次 Gather 时按顺序探测，取第一个可用的）：
//  1. HTTP：LHM Remote Web Server 的 /data.json（默认 http://127.0.0.1:8085）。
//     需要用户在 LHM 中开启 Options -> Remote Web Server -> Run。
//     新版 LHM（实测 0.9.6）已不再发布 WMI，这是主路径。
//  2. WMI：root\LibreHardwareMonitor 命名空间，兼容仍发布 WMI 的旧版 LHM。
//
// 启用策略：input 总是注册，在首次 Gather 时探测数据源（不阻塞 agent / monitor
// 启动和其他 input）。探测到可用数据源且至少存在一个温度传感器则锁定该数据源。
// 首次探测所有数据源都明确不可用时，等待 lhmReprobeInterval（10 分钟）再探测
// 一次，覆盖 agent 开机先于 LHM 启动的场景；第二次仍不可用则整个生命周期不再
// 尝试，即最多探测 lhmMaxProbes（2）次。探测时本轮 deadline 被慢数据源耗尽也
// 计为一次失败。两次探测之后 LHM 才运行的，需要重启 agent 才会开始采集。
//
// 卡死防护：所有查询都在 Gather 内同步执行，不另起 goroutine。HTTP 通过 ctx
// 中断；WMI 不支持 context，卡住时 Gather 不返回，由 inputRunner 的不重入
// 状态兜底（该 input 保持 running，后续轮次跳过），进程内最多留一个卡住的
// 采集 goroutine，与其他 input 的设计一致。
//
// 阈值类传感器（Distance to TjMax、Warning/Critical Temperature、
// Thermal Sensor * Limit、Resolution 等）不是实际温度，统一过滤。
//
// 上报格式：作为 env measurement 的子指标 temperature 上报（每个传感器一条，
// 以 sensor 等 tag 区分），复用 env 已有的后端 kafka topic 与监控结果表，后端
// （environment 服务按 measurement 白名单路由 topic）无需改动。温度行只带
// temperature 字段，Kernel input 产出的 env 行（uptime 等）不受影响。

const (
	// lhmHTTPURL LHM Remote Web Server 的 data.json 地址。
	lhmHTTPURL = "http://127.0.0.1:8085/data.json"
	// lhmHTTPTimeout 单次 HTTP 请求超时。本机回环正常在毫秒级；取值需明显小于
	// perInputTimeout（3s），HTTP 卡住时本轮仍有时间回退探测 WMI。
	lhmHTTPTimeout = 1500 * time.Millisecond
	// lhmHTTPMaxBody data.json 最大读取字节数，防止异常响应占用内存。
	// 实测一台 i9 + 4 DIMM + GPU + 2 NVMe 的机器约 90KB。
	lhmHTTPMaxBody = 8 << 20

	// lhmWMINamespace 旧版 LHM 发布的 WMI 命名空间。
	lhmWMINamespace = `root\LibreHardwareMonitor`
	// lhmSensorTypeTemperature LHM 中温度传感器类型的取值（HTTP 与 WMI 一致）。
	lhmSensorTypeTemperature = "Temperature"

	lhmQuerySensors  = "SELECT Identifier, Name, SensorType, Parent, Value FROM Sensor WHERE SensorType = 'Temperature'"
	lhmQueryHardware = "SELECT Identifier, Name, HardwareType FROM Hardware"

	lhmSourceHTTP = "http"
	lhmSourceWMI  = "wmi"

	// lhmMaxProbes 数据源明确不可用时的最大探测次数（首次 + 延迟重探 1 次）。
	lhmMaxProbes = 2
	// lhmReprobeInterval 首次探测失败后到第二次探测的等待时间。
	lhmReprobeInterval = 10 * time.Minute
)

// lhmReading 一个温度传感器的读数，已归一为摄氏度。
type lhmReading struct {
	Identifier   string // 如 /intelcpu/0/temperature/26
	Name         string // 如 CPU Package
	Hardware     string // 如 Intel Core i9-14900K，可能为空
	HardwareType string // 如 Cpu / GpuNvidia / Storage，可能为空
	Value        float64
}

// lhmClient 抽象 LHM 数据源，便于单测注入。
type lhmClient interface {
	// Readings 返回温度传感器读数（未过滤阈值类传感器）。
	Readings(ctx context.Context) ([]lhmReading, error)
}

// ---------------------------------------------------------------------------
// HTTP 数据源
// ---------------------------------------------------------------------------

// lhmNode 对应 data.json 中的树节点。根节点为 Sensor 占位，其下依次为
// 计算机 -> 硬件（带 HardwareId）-> 分组（Temperatures 等）-> 传感器（带 SensorId）。
// 子硬件（如 SuperIO）也带 HardwareId，挂在硬件节点下。
type lhmNode struct {
	Text       string    `json:"Text"`
	Value      string    `json:"Value"`
	SensorID   string    `json:"SensorId"`
	Type       string    `json:"Type"`
	HardwareID string    `json:"HardwareId"`
	Children   []lhmNode `json:"Children"`
}

type httpLHMClient struct {
	url    string
	client *http.Client
}

func newHTTPLHMClient(url string) *httpLHMClient {
	return &httpLHMClient{
		url: url,
		client: &http.Client{
			Timeout: lhmHTTPTimeout,
			// 访问本机回环地址，显式不走代理；每分钟一次，不保留空闲连接
			Transport: &http.Transport{Proxy: nil, DisableKeepAlives: true},
		},
	}
}

func (c *httpLHMClient) Readings(ctx context.Context) ([]lhmReading, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, c.url, nil)
	if err != nil {
		return nil, err
	}
	resp, err := c.client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		_, _ = io.Copy(io.Discard, io.LimitReader(resp.Body, 4096))
		return nil, errors.Errorf("unexpected status %d", resp.StatusCode)
	}
	var root lhmNode
	if err := json.NewDecoder(io.LimitReader(resp.Body, lhmHTTPMaxBody)).Decode(&root); err != nil {
		return nil, errors.Wrap(err, "decode data.json")
	}
	var out []lhmReading
	walkLHMNode(&root, "", "", &out)
	return out, nil
}

// walkLHMNode 深度遍历 data.json，收集温度传感器，hwName/hwID 为最近的硬件祖先。
func walkLHMNode(n *lhmNode, hwName, hwID string, out *[]lhmReading) {
	if n.HardwareID != "" {
		hwName, hwID = n.Text, n.HardwareID
	}
	if n.SensorID != "" && n.Type == lhmSensorTypeTemperature {
		if v, ok := parseLHMTemp(n.Value); ok {
			*out = append(*out, lhmReading{
				Identifier:   n.SensorID,
				Name:         n.Text,
				Hardware:     hwName,
				HardwareType: lhmHardwareType(hwID),
				Value:        v,
			})
		}
	}
	for i := range n.Children {
		walkLHMNode(&n.Children[i], hwName, hwID, out)
	}
}

// parseLHMTemp 解析 "64.0 °C" / "147.2 °F" / "64,0 °C" 这类带单位的字符串，
// 统一返回摄氏度。无法解析（如 "-"、空串）返回 false。
func parseLHMTemp(s string) (float64, bool) {
	s = strings.TrimSpace(s)
	if s == "" {
		return 0, false
	}
	fahrenheit := strings.HasSuffix(s, "F")
	// 取前缀数字部分
	end := 0
	for end < len(s) {
		ch := s[end]
		if (ch >= '0' && ch <= '9') || ch == '.' || ch == ',' || ch == '-' || ch == '+' {
			end++
			continue
		}
		break
	}
	num := s[:end]
	if num == "" {
		return 0, false
	}
	// 部分区域设置下小数点为逗号
	if !strings.Contains(num, ".") {
		num = strings.Replace(num, ",", ".", 1)
	}
	v, err := strconv.ParseFloat(num, 64)
	if err != nil || math.IsNaN(v) || math.IsInf(v, 0) {
		return 0, false
	}
	if fahrenheit {
		v = (v - 32) * 5 / 9
	}
	return v, true
}

// lhmHardwareType 由 HardwareId 首段推导硬件类型，取值尽量与 LHM 的
// HardwareType 枚举（WMI 中的 HardwareType 字段）保持一致。
func lhmHardwareType(hwID string) string {
	seg := strings.TrimPrefix(hwID, "/")
	if i := strings.IndexByte(seg, '/'); i >= 0 {
		seg = seg[:i]
	}
	switch seg {
	case "":
		return ""
	case "intelcpu", "amdcpu", "cpu":
		return "Cpu"
	case "gpu-nvidia":
		return "GpuNvidia"
	case "gpu-amd", "gpu-ati":
		return "GpuAmd"
	case "gpu-intel", "gpu-intel-integrated":
		return "GpuIntel"
	case "nvme", "hdd", "ssd", "storage":
		return "Storage"
	case "memory", "ram", "vram":
		return "Memory"
	case "motherboard", "mainboard":
		return "Motherboard"
	case "lpc", "superio":
		return "SuperIO"
	case "nic":
		return "Network"
	case "battery":
		return "Battery"
	case "psu":
		return "Psu"
	default:
		return seg
	}
}

// ---------------------------------------------------------------------------
// WMI 数据源（兼容旧版 LHM）
// ---------------------------------------------------------------------------

// lhmWMISensor 对应 WMI 类 Sensor 的部分属性。字段名必须与 WMI 属性名一致。
type lhmWMISensor struct {
	Identifier string
	Name       string
	SensorType string
	Parent     string
	Value      float32
}

// lhmWMIHardware 对应 WMI 类 Hardware 的部分属性。
type lhmWMIHardware struct {
	Identifier   string
	Name         string
	HardwareType string
}

type wmiLHMClient struct{}

// Readings WMI 查询不支持 context，只能在发起前检查 ctx；一旦进入查询就
// 同步等待返回，卡住时由 inputRunner 的不重入状态兜底。
func (wmiLHMClient) Readings(ctx context.Context) ([]lhmReading, error) {
	if err := ctx.Err(); err != nil {
		return nil, err
	}
	var sensors []lhmWMISensor
	if err := wmi.QueryNamespace(lhmQuerySensors, &sensors, lhmWMINamespace); err != nil {
		return nil, err
	}
	// 硬件信息只用于补充 tag，查询失败或已超时都不影响温度上报。
	hwByID := make(map[string]lhmWMIHardware)
	if ctx.Err() != nil {
		return wmiSensorsToReadings(sensors, hwByID), nil
	}
	var hws []lhmWMIHardware
	if err := wmi.QueryNamespace(lhmQueryHardware, &hws, lhmWMINamespace); err == nil {
		for _, hw := range hws {
			hwByID[hw.Identifier] = hw
		}
	} else {
		logs.WithError(err).Debug("monitor|hwtemp query LHM wmi hardware failed")
	}
	return wmiSensorsToReadings(sensors, hwByID), nil
}

func wmiSensorsToReadings(sensors []lhmWMISensor, hwByID map[string]lhmWMIHardware) []lhmReading {
	out := make([]lhmReading, 0, len(sensors))
	for _, s := range sensors {
		if s.SensorType != lhmSensorTypeTemperature || s.Identifier == "" {
			continue
		}
		v := float64(s.Value)
		if math.IsNaN(v) || math.IsInf(v, 0) {
			continue
		}
		r := lhmReading{Identifier: s.Identifier, Name: s.Name, Value: v}
		if hw, ok := hwByID[s.Parent]; ok {
			r.Hardware = hw.Name
			r.HardwareType = hw.HardwareType
		}
		out = append(out, r)
	}
	return out
}

// ---------------------------------------------------------------------------
// Input
// ---------------------------------------------------------------------------

// lhmThresholdKeywords 名称包含这些关键字（小写）的温度传感器视为阈值/差值，不上报。
var lhmThresholdKeywords = []string{
	"distance to tjmax",
	"warning temperature",
	"critical temperature",
	"limit",
	"resolution",
}

// isLHMThresholdSensor 判断是否为阈值类（非实际温度）传感器。
func isLHMThresholdSensor(name string) bool {
	n := strings.ToLower(name)
	for _, k := range lhmThresholdKeywords {
		if strings.Contains(n, k) {
			return true
		}
	}
	return false
}

// filterLHMReadings 过滤阈值类传感器和无 Identifier 的读数。
func filterLHMReadings(rs []lhmReading) []lhmReading {
	out := rs[:0:0]
	for _, r := range rs {
		if r.Identifier == "" || isLHMThresholdSensor(r.Name) {
			continue
		}
		out = append(out, r)
	}
	return out
}

// HWTemp 从 LHM 采集温度传感器读数。
//
// 所有字段只在 Gather 中读写；inputRunner 保证同一实例的 Gather 不会并发，
// 因此无需加锁。
type HWTemp struct {
	// cands 待探测的数据源，client 为 nil 时在 Gather 中依次探测。
	cands []lhmCandidate
	// client / source 探测成功后锁定的数据源。
	client lhmClient
	source string
	// disabled 已探测 lhmMaxProbes 次且所有数据源都明确不可用，生命周期内不再尝试。
	disabled bool
	// probes 已完成的失败探测次数（含本轮 deadline 被耗尽的情况）。
	probes int
	// nextProbe 下一次允许探测的时间，零值表示立即探测。
	nextProbe time.Time

	nowFn func() time.Time

	// failing 记录锁定数据源后上一轮采集是否失败，只在状态切换时打日志，
	// 避免 LHM 中途退出后每分钟刷一条 warn。
	failing bool
}

// newHWTemp 返回待探测的 HWTemp，首次 Gather 时按 cands 顺序选择数据源。
func newHWTemp(cands []lhmCandidate) *HWTemp {
	return &HWTemp{cands: cands, nowFn: time.Now}
}

// newHWTempWithClient 返回已锁定数据源的 HWTemp（单测用）。
func newHWTempWithClient(c lhmClient, source string) *HWTemp {
	return &HWTemp{client: c, source: source, nowFn: time.Now}
}

// hwTempInputName input 标识，仅用于日志（runner 跳过 / 失败提示），与
// Kernel input 的 "env" 区分；实际上报的 measurement 是 env。
const hwTempInputName = "hwtemp"

// Name 返回 input 标识 "hwtemp"。
func (h *HWTemp) Name() string { return hwTempInputName }

// Gather 读取 LHM 温度传感器，每个传感器产出一条 env metric（字段 temperature）。
//
// 查询同步执行：HTTP 受 ctx 控制；WMI 卡住时本方法不返回，由 inputRunner
// 保持 running 并在后续轮次跳过，保证最多只有一个卡住的 goroutine。
//
// LHM 不可用或中途退出时返回 (nil, nil)，只在状态切换时记录日志。
func (h *HWTemp) Gather(ctx context.Context) ([]Metric, error) {
	if h.disabled {
		return nil, nil
	}
	if h.client == nil {
		if !h.nextProbe.IsZero() && h.nowFn().Before(h.nextProbe) {
			return nil, nil
		}
		rs, ok := h.detect(ctx)
		if !ok {
			return nil, nil
		}
		// 探测时已拿到读数，直接产出，避免首轮重复查询
		return h.toMetrics(rs), nil
	}

	rs, err := h.read(ctx)
	if err != nil {
		if !h.failing {
			logs.WithError(err).Warnf("monitor|hwtemp gather failed via %s, LibreHardwareMonitor may have exited; suppress until recovered", h.source)
		}
		h.failing = true
		return nil, nil
	}
	if h.failing {
		logs.Infof("monitor|hwtemp gather recovered via %s", h.source)
		h.failing = false
	}
	return h.toMetrics(rs), nil
}

// detect 依次探测数据源，成功时锁定数据源并返回过滤后的读数。
//
// 所有数据源都失败计为一次探测，包括本轮 deadline 被慢数据源耗尽的情况
// （数据源慢到超出 perInputTimeout 本身就视为不健康）：未满 lhmMaxProbes 次时
// 安排 lhmReprobeInterval 后重探，满了才置 disabled，保证生命周期内最多探测
// lhmMaxProbes 次。
func (h *HWTemp) detect(ctx context.Context) ([]lhmReading, bool) {
	var reasons []string
	for _, c := range h.cands {
		rs, err := readFiltered(ctx, c.client)
		if err == nil {
			h.client, h.source, h.failing = c.client, c.source, false
			logs.Infof("monitor|hwtemp enabled: LibreHardwareMonitor detected via %s, temperature sensors=%d", c.source, len(rs))
			return rs, true
		}
		reasons = append(reasons, c.source+": "+err.Error())
	}
	if ctx.Err() != nil {
		reasons = append(reasons, "deadline: "+ctx.Err().Error())
	}
	h.probes++
	if h.probes < lhmMaxProbes {
		h.nextProbe = h.nowFn().Add(lhmReprobeInterval)
		logs.Infof("monitor|hwtemp LibreHardwareMonitor not available (%s), probe again after %s (%d/%d)",
			strings.Join(reasons, "; "), lhmReprobeInterval, h.probes, lhmMaxProbes)
		return nil, false
	}
	h.disabled = true
	logs.Infof("monitor|hwtemp disabled: LibreHardwareMonitor not available after %d probes (%s)", h.probes, strings.Join(reasons, "; "))
	return nil, false
}

// read 从已锁定的数据源读取一次。
func (h *HWTemp) read(ctx context.Context) ([]lhmReading, error) {
	rs, err := readFiltered(ctx, h.client)
	if err != nil {
		return nil, errors.Wrapf(err, "hwtemp: read LHM via %s", h.source)
	}
	return rs, nil
}

// readFiltered 读取并过滤阈值类传感器，过滤后为空视为不可用。
func readFiltered(ctx context.Context, c lhmClient) ([]lhmReading, error) {
	rs, err := c.Readings(ctx)
	if err != nil {
		return nil, err
	}
	rs = filterLHMReadings(rs)
	if len(rs) == 0 {
		return nil, errors.New("no temperature sensor")
	}
	return rs, nil
}

// toMetrics 把读数转换为 metric，按 sensor 排序。
func (h *HWTemp) toMetrics(rs []lhmReading) []Metric {
	now := h.nowFn()
	out := make([]Metric, 0, len(rs))
	for _, r := range rs {
		tags := map[string]string{
			TagSensor:     r.Identifier,
			TagSensorName: r.Name,
		}
		if r.Hardware != "" {
			tags[TagHardware] = r.Hardware
		}
		if r.HardwareType != "" {
			tags[TagHardwareType] = r.HardwareType
		}
		out = append(out, Metric{
			Name:      RenamedEnv,
			Tags:      tags,
			Fields:    map[string]interface{}{FieldTemperature: roundTo(r.Value, 1)},
			Timestamp: now,
		})
	}
	// 输出顺序稳定，便于 dump 对比
	sort.Slice(out, func(i, j int) bool {
		return out[i].Tags[TagSensor] < out[j].Tags[TagSensor]
	})
	return out
}

// roundTo 保留 n 位小数。WMI 的 Value 是 float32，直接转 float64 会带出
// 61.500003 这类噪声；HTTP 的华氏度换算也会产生长尾小数。
func roundTo(v float64, n int) float64 {
	p := math.Pow10(n)
	return math.Round(v*p) / p
}

// lhmCandidate 一个待探测的数据源。
type lhmCandidate struct {
	source string
	client lhmClient
}

// lhmCandidatesFn 返回按优先级排列的数据源，单测可替换。
var lhmCandidatesFn = func() []lhmCandidate {
	return []lhmCandidate{
		{source: lhmSourceHTTP, client: newHTTPLHMClient(lhmHTTPURL)},
		{source: lhmSourceWMI, client: wmiLHMClient{}},
	}
}

// init 把 HWTemp 注册到 Windows 平台额外 input 列表。只构造对象、不做任何
// I/O，探测推迟到首次 Gather。保留已有注册，避免与其他 Windows 专属 input
// 互相覆盖。
func init() {
	prev := platformExtraInputsFn
	platformExtraInputsFn = func() []Input {
		var ins []Input
		if prev != nil {
			ins = append(ins, prev()...)
		}
		return append(ins, newHWTemp(lhmCandidatesFn()))
	}
}
