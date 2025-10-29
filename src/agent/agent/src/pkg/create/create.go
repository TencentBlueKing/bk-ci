package create

import "sync/atomic"

type ProcessType string

const (
	DaemonProcess ProcessType = "DAEMON"
	AgentProcess  ProcessType = "AGENT"
)

var createModFlag = atomic.Bool{}

func UpdateCreateModFlag(flag bool) {
	createModFlag.Store(flag)
}

// CheckCreateMod 检测是否是创作流模式
func CheckCreateMod() bool {
	return createModFlag.Load()
}
