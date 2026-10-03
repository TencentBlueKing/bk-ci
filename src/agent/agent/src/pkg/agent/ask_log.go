package agent

import (
	"errors"
	"fmt"
	"net"
	"net/url"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/httputil"
)

// askStatus 保存当前进程的 ask 日志状态，由主循环中的 doAsk 串行访问，因此不单独加锁。
// 成功记录必须放在 HTTP 请求、业务状态检查和响应解析均通过之后，
// 不能根据“请求已经发出”或“请求/响应内容与上次相同”判断心跳成功。
var askStatus askLogState

// askLogState 分别维护统计窗口与连续故障状态，二者的重置时机不同：
// 成功日志输出后开启新的统计窗口；故障持续期间即使错误原因改变，
// 也保留最早失败时间和连续失败总数，直到真正恢复成功。
type askLogState struct {
	windowStart         time.Time // 当前统计窗口的起点，用于成功汇总中的 window 字段。
	lastSuccess         time.Time // 最近一次实际成功的时间；失败时不更新，零值表示从未成功。
	successes           uint64    // 当前统计窗口内的成功次数，包含未输出 INFO 的成功请求。
	failures            uint64    // 当前统计窗口内的失败次数，恢复日志会一并输出。
	failureStart        time.Time // 本轮连续故障中第一次失败的时间，用于计算故障持续时间。
	lastFailureLog      time.Time // 最近一次输出 ERROR 的时间，独立控制重复错误的汇总间隔。
	lastFailureKey      string    // 最近一次输出 ERROR 对应的错误分类键，用于识别错误原因变化。
	consecutiveFailures uint64    // 自上一次成功以来的失败总数，错误原因变化时继续累计。
	failuresSinceLog    uint64    // 自上一次 ERROR 日志以来的失败次数，包含当前待输出的失败。
}

// success 记录一次完成校验的成功请求。
// 首次成功、故障恢复和汇总间隔到期都会立即输出 INFO，其余成功只更新统计。
// 由请求完成路径主动调用，而不是由独立定时器输出，避免请求卡住时仍报告健康。
// now 由调用方传入，便于测试精确覆盖时间边界，无需真实等待数分钟。
func (s *askLogState) success(now time.Time) {
	if s.windowStart.IsZero() {
		s.windowStart = now
	}
	first := s.lastSuccess.IsZero()
	// 即使本次不输出日志，也必须更新最后成功时间和成功次数。
	s.lastSuccess = now
	s.successes++
	event := "ask healthy"
	if s.consecutiveFailures > 0 {
		// 恢复事件优先于首次成功：启动后一直失败再成功，也应报告此前的故障。
		event = fmt.Sprintf("ask recovered failed_count=%d outage=%s",
			s.consecutiveFailures, now.Sub(s.failureStart).Round(time.Millisecond))
	} else if first {
		event = "ask connected"
	} else if now.Sub(s.windowStart) < logs.StatusLogInterval() {
		return
	}
	logs.Infof("%s window=%s success=%d failed=%d last_success=%s", event,
		now.Sub(s.windowStart).Round(time.Millisecond), s.successes, s.failures, now.Format(time.RFC3339))
	// 本次成功日志已包含整个窗口的统计；随后开始新窗口，并清除故障状态。
	// 清除错误分类键后，同一种错误再次出现时仍会作为新故障立即输出。
	s.windowStart, s.successes, s.failures = now, 0, 0
	s.failureStart, s.lastFailureLog = time.Time{}, time.Time{}
	s.lastFailureKey, s.consecutiveFailures, s.failuresSinceLog = "", 0, 0
}

// failure 记录一次失败。key 用于判断错误是否相同，err 保留本次错误的完整说明。
// 第一次失败、错误分类键变化或重复错误达到汇总间隔时输出 ERROR；
// 窗口内的相同错误仅输出 DEBUG，但所有计数仍会累计，不会因日志限频漏计。
func (s *askLogState) failure(now time.Time, key string, err error) {
	if s.windowStart.IsZero() {
		s.windowStart = now
	}
	if s.consecutiveFailures == 0 {
		// 故障期间错误原因变化不重置起点，恢复日志需要反映整段中断时长。
		s.failureStart = now
	}
	s.failures++
	s.consecutiveFailures++
	s.failuresSinceLog++
	if s.consecutiveFailures == 1 || key != s.lastFailureKey || now.Sub(s.lastFailureLog) >= logs.StatusLogInterval() {
		lastSuccess := "never"
		if !s.lastSuccess.IsZero() {
			lastSuccess = s.lastSuccess.Format(time.RFC3339)
		}
		logs.Errorf("%s consecutive_failures=%d failures_since_last_log=%d duration=%s last_success=%s",
			err, s.consecutiveFailures, s.failuresSinceLog, now.Sub(s.failureStart).Round(time.Millisecond), lastSuccess)
		// 仅重置当前错误日志窗口，不清空连续失败总数和最后成功时间。
		s.lastFailureKey, s.lastFailureLog, s.failuresSinceLog = key, now, 0
	} else {
		logs.Debugf("%s consecutive_failures=%d", err, s.consecutiveFailures)
	}
}

// parseAskResponse 统一校验 ask 结果，返回业务响应、错误分类键和错误说明。
// 成功时分类键为空且错误为 nil；失败时响应为 nil，分类键供上层日志限频使用。
// 此处只做校验，不执行卸载或任务调度；删除 Agent 等业务动作仍由 doAsk 处理。
// 非 2xx 的 HTTP 响应已由 api.Ask 转为 requestErr，不能只凭响应体判断成功。
func parseAskResponse(result *httputil.AgentResult, requestErr error) (*api.AskResp, string, error) {
	if requestErr != nil {
		return nil, askRequestErrorKey(requestErr), fmt.Errorf("ask request failed: %w", requestErr)
	}
	if result == nil {
		return nil, "result:nil", errors.New("ask request result failed: empty result")
	}
	if result.IsNotOk() {
		return nil, fmt.Sprintf("result:%d:%s", result.Status, result.Message),
			fmt.Errorf("ask request result failed: status=%d message=%s", result.Status, result.Message)
	}
	if result.AgentStatus != config.AgentStatusImportOk {
		return nil, "agent_status:" + result.AgentStatus, fmt.Errorf("agent status [%s] not ok", result.AgentStatus)
	}
	var resp *api.AskResp
	// JSON 的 null 可以被正常反序列化，因此还需要检查解析后的指针，
	// 防止空响应被当作成功并更新 last_success。
	if err := util.ParseJsonToData(result.Data, &resp); err != nil {
		return nil, "parse:" + err.Error(), fmt.Errorf("parse ask resp failed: %w", err)
	}
	if resp == nil {
		return nil, "parse:nil", errors.New("parse ask resp failed: empty response")
	}
	return resp, "", nil
}

// askRequestErrorKey 生成适合重复判断的请求错误分类键。
// 日志内容仍使用原始错误；这里只移除影响归类的易变字段，避免同一故障反复刷屏。
func askRequestErrorKey(err error) string {
	var netErr net.Error
	if errors.As(err, &netErr) && netErr.Timeout() {
		// 各类网络超时统一归类，避免底层包装或超时文案的差异绕过限频。
		return "request:timeout"
	}
	// 每次连接的本地端口可能变化。先拆掉 URL 包装，再对网络操作错误
	// 保留操作类型与底层原因，去掉动态地址，确保持续的拨号/读取故障可被汇总。
	var urlErr *url.Error
	if errors.As(err, &urlErr) {
		err = urlErr.Err
	}
	var opErr *net.OpError
	if errors.As(err, &opErr) {
		return "request:" + opErr.Op + ":" + opErr.Err.Error()
	}
	return "request:" + err.Error()
}
