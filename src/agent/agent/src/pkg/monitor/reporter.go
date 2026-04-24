//go:build !loong64
// +build !loong64

package monitor

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

// reporter.go 负责把一批 metric 序列化并 POST 到 BK-CI 后端。
//
// 对齐 telegraf 的双输出通道（见 src/pkg/collector/telegrafConf/telegrafConf.go）：
//
//   - stream 项目（projectId 以 git_ 前缀） → /agents/metrix, InfluxDB line protocol
//   - ci 项目（其余）                       → /agents/metrics, JSON
//
// 认证 Header 与现有 api 包统一走 config.GAgentConfig.GetAuthHeaderMap()，
// 保证 agent 所有上报路径的鉴权一致。

// metricJSON 是发给 /agents/metrics 的 JSON 条目。字段名和 telegraf 默认
// serializer（data_format = "json"）输出对齐：name / timestamp / tags / fields。
type metricJSON struct {
	Name      string                 `json:"name"`
	Timestamp int64                  `json:"timestamp"` // Unix 秒（与 telegraf 默认精度一致）
	Tags      map[string]string      `json:"tags,omitempty"`
	Fields    map[string]interface{} `json:"fields"`
}

// metricsEnvelope 对应后端 TelegrafMulData（environment 模块）:
//
//	data class TelegrafMulData(val metrics: List<TelegrafStandData>?)
//
// 后端只认 {"metrics":[...]} 这种对象包装；直接发 JSON 数组会触发
// Jackson MismatchedInputException("from Array value")。
type metricsEnvelope struct {
	Metrics []metricJSON `json:"metrics"`
}

// Reporter 负责上报指标到 BK-CI 后端。
//
// 可注入 doPost 以便测试替换 HTTP 请求逻辑；生产路径使用 defaultDoPost。
type Reporter struct {
	// doPost 执行一次 HTTP POST 请求并返回 (statusCode, body, error)。
	// 注入点使得我们可以在单元测试里断言 URL / Headers / Body 而不发起真实请求。
	doPost func(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error)

	// nowFn 在 metric.Timestamp 为零值时提供兜底时间。
	nowFn func() time.Time
}

// NewReporter 构造默认 Reporter。
func NewReporter() *Reporter {
	return &Reporter{
		doPost: defaultDoPost,
		nowFn:  time.Now,
	}
}

// Report 把一组已经过 rename 的 metric 上报到后端。
// 根据 projectId 判定 stream / ci 分支，序列化后 POST。
//
// metrics 为空时直接返回 nil，不产生 HTTP 调用。
func (r *Reporter) Report(ctx context.Context, metrics []Metric) error {
	if len(metrics) == 0 {
		return nil
	}

	gateway := buildGateway()
	if gateway == "" {
		return errors.New("reporter: empty gateway, config not ready")
	}

	// projectType 判定规则与 collector.genTelegrafConfig 一致。
	isStream := strings.HasPrefix(config.GAgentConfig.ProjectId, StreamProjectPrefix)

	var (
		url         string
		body        []byte
		contentType string
		err         error
	)
	if isStream {
		url = gateway + ReportPathMetrix
		body = r.encodeLineProtocol(metrics)
		contentType = ContentTypeLineProtocol
	} else {
		url = gateway + ReportPathMetrics
		body, err = r.encodeJSON(metrics)
		if err != nil {
			return errors.Wrap(err, "reporter: encode JSON")
		}
		contentType = ContentTypeJSON
	}

	headers := r.buildHeaders(contentType)

	status, respBody, err := r.doPost(ctx, url, headers, body)
	if err != nil {
		return errors.Wrap(err, "reporter: POST failed")
	}
	if status < 200 || status >= 300 {
		return fmt.Errorf("reporter: POST %s non-2xx status=%d body=%s",
			url, status, truncateForLog(respBody, 256))
	}
	logs.Debugf("monitor|report ok: metrics=%d, status=%d, stream=%v",
		len(metrics), status, isStream)
	return nil
}

// encodeJSON 把 metrics 序列化成后端 TelegrafMulData 期望的对象结构
// {"metrics":[{name,timestamp,tags,fields}, ...]}。
// timestamp 走 Unix 秒，与 telegraf `json_timestamp_units = "1s"` 默认行为一致。
func (r *Reporter) encodeJSON(metrics []Metric) ([]byte, error) {
	items := make([]metricJSON, 0, len(metrics))
	for _, m := range metrics {
		if len(m.Fields) == 0 {
			continue
		}
		ts := m.Timestamp
		if ts.IsZero() {
			ts = r.nowFn()
		}
		items = append(items, metricJSON{
			Name:      m.Name,
			Timestamp: ts.Unix(),
			Tags:      m.Tags,
			Fields:    m.Fields,
		})
	}
	return json.Marshal(metricsEnvelope{Metrics: items})
}

// encodeLineProtocol 输出 InfluxDB line protocol 文本：
//
//	measurement,tag1=v1,tag2=v2 field1=v1,field2=v2 timestamp_ns\n
//
// 排序规则（保证确定性）：tag / field 均按 key 升序。
// 特殊字符转义严格参照 InfluxDB 文档：measurement/tag key/value 里的
// 空格、逗号、等号（tag/field key 额外）需要反斜杠转义。
func (r *Reporter) encodeLineProtocol(metrics []Metric) []byte {
	var buf bytes.Buffer
	for _, m := range metrics {
		if len(m.Fields) == 0 {
			continue
		}
		ts := m.Timestamp
		if ts.IsZero() {
			ts = r.nowFn()
		}

		buf.WriteString(escapeMeasurement(m.Name))

		// tags，按 key 升序
		if len(m.Tags) > 0 {
			keys := make([]string, 0, len(m.Tags))
			for k := range m.Tags {
				keys = append(keys, k)
			}
			sort.Strings(keys)
			for _, k := range keys {
				buf.WriteByte(',')
				buf.WriteString(escapeTag(k))
				buf.WriteByte('=')
				buf.WriteString(escapeTag(m.Tags[k]))
			}
		}

		// fields，按 key 升序
		buf.WriteByte(' ')
		fKeys := make([]string, 0, len(m.Fields))
		for k := range m.Fields {
			fKeys = append(fKeys, k)
		}
		sort.Strings(fKeys)
		for i, k := range fKeys {
			if i > 0 {
				buf.WriteByte(',')
			}
			buf.WriteString(escapeTag(k))
			buf.WriteByte('=')
			buf.WriteString(formatFieldValue(m.Fields[k]))
		}

		// timestamp：纳秒
		buf.WriteByte(' ')
		buf.WriteString(strconv.FormatInt(ts.UnixNano(), 10))
		buf.WriteByte('\n')
	}
	return buf.Bytes()
}

// buildHeaders 构造认证 + content-type header。
// 认证 header 复用 config.GAgentConfig.GetAuthHeaderMap()，与 api 包一致。
func (r *Reporter) buildHeaders(contentType string) map[string]string {
	h := config.GAgentConfig.GetAuthHeaderMap()
	if h == nil {
		h = make(map[string]string, 5)
	}
	h["Content-Type"] = contentType
	return h
}

// buildGateway 从配置读取 gateway 并补齐 http:// 前缀，逻辑与
// collector.genTelegrafConfig (collector.go:154-157) 保持一致。
func buildGateway() string {
	gw := strings.TrimSpace(config.GAgentConfig.Gateway)
	if gw == "" {
		return ""
	}
	if !strings.HasPrefix(gw, "http") {
		gw = "http://" + gw
	}
	return gw
}

// defaultDoPost 使用 http.DefaultClient 发起一次请求。超时走 ctx。
// 不复用 httputil.NewHttpClient 的原因：它强制对 body 做 json.Marshal，
// 不兼容 line protocol 的 text/plain body。
func defaultDoPost(ctx context.Context, url string, headers map[string]string, body []byte) (int, []byte, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, bytes.NewReader(body))
	if err != nil {
		return 0, nil, err
	}
	for k, v := range headers {
		req.Header.Set(k, v)
	}
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return 0, nil, err
	}
	defer resp.Body.Close()

	// 读一小段响应即可，避免后端异常时回传大 body 浪费内存
	var respBuf bytes.Buffer
	_, _ = respBuf.ReadFrom(resp.Body)
	return resp.StatusCode, respBuf.Bytes(), nil
}

// --- line protocol 转义辅助函数 ---

// escapeMeasurement 按 InfluxDB 线协议规则转义 measurement 名：
// 需要转义的字符有 逗号(,) 和 空格( )，反斜杠用 \ 开头。
func escapeMeasurement(s string) string {
	return replacePairs(s, ",", `\,`, " ", `\ `)
}

// escapeTag 按线协议规则转义 tag key / tag value / field key：
// 需要转义的字符为 逗号(,)、等号(=)、空格( )。
func escapeTag(s string) string {
	return replacePairs(s, ",", `\,`, "=", `\=`, " ", `\ `)
}

// replacePairs 连续应用多对 (old, new) 替换。
// 为了避免分配 strings.Replacer（每次调用成本高），用最简单的
// strings.ReplaceAll 链。线协议里这几类字符都罕见，性能非瓶颈。
func replacePairs(s string, pairs ...string) string {
	out := s
	for i := 0; i+1 < len(pairs); i += 2 {
		out = strings.ReplaceAll(out, pairs[i], pairs[i+1])
	}
	return out
}

// formatFieldValue 把 field value 按线协议格式格式化：
//   - 整数（int64/uint64）后缀 i（InfluxDB 整型约定）
//   - float64 用 strconv.FormatFloat，保持最少必要精度
//   - bool / string 走对应语法
//
// 未知类型退回为 fmt.Sprintf("%v")，保证不 panic。
func formatFieldValue(v interface{}) string {
	switch x := v.(type) {
	case float64:
		return strconv.FormatFloat(x, 'f', -1, 64)
	case float32:
		return strconv.FormatFloat(float64(x), 'f', -1, 32)
	case int:
		return strconv.FormatInt(int64(x), 10) + "i"
	case int32:
		return strconv.FormatInt(int64(x), 10) + "i"
	case int64:
		return strconv.FormatInt(x, 10) + "i"
	case uint:
		return strconv.FormatUint(uint64(x), 10) + "i"
	case uint32:
		return strconv.FormatUint(uint64(x), 10) + "i"
	case uint64:
		return strconv.FormatUint(x, 10) + "i"
	case bool:
		if x {
			return "true"
		}
		return "false"
	case string:
		// string 值需要双引号并转义
		return `"` + strings.ReplaceAll(x, `"`, `\"`) + `"`
	default:
		return fmt.Sprintf("%v", v)
	}
}

// truncateForLog 截断过长的 body，避免日志里出现 MB 级响应。
func truncateForLog(b []byte, max int) string {
	if len(b) <= max {
		return string(b)
	}
	return string(b[:max]) + "...(truncated)"
}
