package systemutil

import (
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

// agentIPSelection 是一次 IP 探测结果的日志快照。
// 不仅比较最终 IP，也比较路由、网卡属性以及回退原因；即使最终 IP 相同，
// 更换网卡或从回退状态恢复也应立即记录。所有字段保持可比较，以支持整体比较。
type agentIPSelection struct {
	candidate agentIPCandidate // 选中的 IP、网卡名称/描述、虚拟网卡标记和评分。
	routeIP   string           // 与服务端通信时使用的出站 IP，用于定位路由变化。
	fallback  bool             // 是否因候选地址不可用而采用回退地址。
	reason    string           // 路由探测或候选地址获取的异常原因；空字符串表示无异常。
}

// agentIPStatus 在当前进程内保留最近一次选址日志状态，不随 IP 缓存到期而清空。
var agentIPStatus agentIPLogState

// agentIPLogState 控制选址日志的首次输出、变化输出和低频汇总。
// IP 探测既可能由主循环调用，也可能由心跳配置处理协程调用，必须保护并发访问。
// 日志状态与 IP 缓存分离，避免缓存每次到期重新探测时都输出一条 INFO。
type agentIPLogState struct {
	mu        sync.Mutex       // 同时保护状态更新和日志输出，避免并发观察重复打印。
	previous  agentIPSelection // 上一次实际观察到的选址结果，用于识别变化和恢复。
	lastLog   time.Time        // 最近一次 INFO/WARN 的时间；DEBUG 不推迟汇总时间。
	unchanged uint64           // 自最近一次 INFO/WARN 或状态变化以来，结果不变的探测次数。
}

// record 记录一次真实 IP 探测的结果；命中 IP 缓存时不会调用本方法。
// 首次、变化和汇总到期时输出 INFO，存在异常或回退时使用 WARN，其余仅输出 DEBUG。
// unchanged_checks 统计的是实际探测次数，不是 ask 请求数，也不包含缓存命中次数。
func (s *agentIPLogState) record(now time.Time, selection agentIPSelection) {
	s.mu.Lock()
	defer s.mu.Unlock()

	first := s.lastLog.IsZero()
	changed := selection != s.previous
	if changed {
		// 结果已经变化，旧结果的连续重复次数不能计入新结果。
		s.unchanged = 0
	} else {
		s.unchanged++
	}
	write := logs.Debugf
	event := "unchanged"
	if first || changed || now.Sub(s.lastLog) >= logs.StatusLogInterval() {
		write = logs.Infof
		if selection.fallback || selection.reason != "" {
			write = logs.Warnf
		}
		switch {
		case first:
			event = "selected"
		case changed && (s.previous.fallback || s.previous.reason != "") && !selection.fallback && selection.reason == "":
			// 即便 IP 没变，只要回退或异常状态解除，也立即记录恢复事件。
			event = "recovered"
		case changed:
			event = "changed"
		default:
			event = "summary"
		}
	}
	c := selection.candidate
	write("select agent ip=%s routeIp=%s iface=%s desc=%s virtual=%t score=%d fallback=%t reason=%q event=%s previous_ip=%s unchanged_checks=%d",
		c.ip, selection.routeIP, c.ifaceName, c.description, c.isVirtual, c.score,
		selection.fallback, selection.reason, event, s.previous.candidate.ip, s.unchanged)
	s.previous = selection
	if event != "unchanged" {
		// 重复的 DEBUG 日志不更新 lastLog，否则高频探测会不断推迟汇总。
		s.lastLog, s.unchanged = now, 0
	}
}
