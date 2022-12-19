package service

type Builder struct {
	CommonWorkLoad
	PrivateBuilder *DedicatedBuilder `json:"privateBuilder"` // 私有构建机配置
	SpecialBuilder *DedicatedBuilder `json:"specialBuilder"` // 特殊构建机配置
}

// DedicatedBuilder 一些特殊机器的配置
type DedicatedBuilder struct {
	Name string `json:"name"`
}

type BuilderStart struct {
	Env     map[string]string `json:"env"`
	Command []string          `json:"command"`
}

type BuilderState string

// 构建机状态枚举, 参考自 Kubernetes Pod Lifecycle
// https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/
const (
	// BuilderReadyToRun 准备启动
	BuilderReadyToRun BuilderState = "readyToRun"
	// BuilderNotExist 构建机不存在，可能已被删除或者未创建
	BuilderNotExist BuilderState = "notExist"
	// BuilderPending 等待运行中
	BuilderPending BuilderState = "pending"
	// BuilderRunning 运行中
	BuilderRunning BuilderState = "running"
	// BuilderSucceeded 运行成功
	BuilderSucceeded BuilderState = "succeeded"
	// BuilderFailed 运行失败，容器以非0退出
	BuilderFailed BuilderState = "failed"
	// BuilderUnknown 各类未知状态
	BuilderUnknown BuilderState = "unknown"
)

type BuilderStatus struct {
	Status  BuilderState `json:"status"`
	Message string       `json:"message"`
}
