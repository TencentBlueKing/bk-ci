package logs

import (
	"os"
	"time"
)

// StatusLogInterval 返回 ask 和 IP 状态日志共用的汇总间隔，默认 5 分钟。
// 从 Agent 进程环境变量 DEVOPS_AGENT_STATUS_LOG_INTERVAL 读取 Go 时长格式，
// 例如 "5m"、"30s"；不读取服务端下发的构建环境变量。
// 未设置、格式错误、零值或负值均回退到默认值，避免错误配置导致日志持续刷屏。
// 此值仅控制重复状态日志的输出频率，不改变 ask 轮询、IP 缓存或故障退出行为；
// 首次成功、状态变化和恢复事件仍由调用方立即输出。
func StatusLogInterval() time.Duration {
	interval, err := time.ParseDuration(os.Getenv("DEVOPS_AGENT_STATUS_LOG_INTERVAL"))
	if err != nil || interval <= 0 {
		return 5 * time.Minute
	}
	return interval
}
