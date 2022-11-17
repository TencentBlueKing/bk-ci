package service

import "disaptch-k8s-manager/pkg/types"

type Job struct {
	CommonWorkLoad
	PodNameSelector       *PodNameSelector `json:"podNameSelector"`       // Pod名称调度选项
	ActiveDeadlineSeconds *int64           `json:"activeDeadlineSeconds"` // Job存活时间
}

type PodNameSelector struct {
	Selector   string `json:"selector" binding:"required"` // Pod名称，调度到指定pod
	UsePodData bool   `json:"usePodData"`                  // 使用和podNameSelector相同的data目录，当挂载目录使用hostPath时依赖PodNameSelector
}

type JobState string

// Job状态枚举
const (
	JobPending   JobState = "pending"
	JobRunning   JobState = "running"
	JobSucceeded JobState = "succeeded"
	JobFailed    JobState = "failed"
	JobUnknown   JobState = "unknown"
)

type JobStatus struct {
	State   JobState `json:"state"`
	Message string   `json:"message"`
	PodIp   string   `json:"podIp"`
}

type BuildAndPushImageInfo struct {
	Name                  string                 `json:"name"  binding:"required,max=32"`    // 唯一名称
	Resource              CommonWorkLoadResource `json:"resource"  binding:"required"`       // 工作负载资源
	PodNameSelector       PodNameSelector        `json:"podNameSelector" binding:"required"` // Pod名称调度
	ActiveDeadlineSeconds *int64                 `json:"activeDeadlineSeconds"`              // Job存活时间
	Info                  *buildImageInfo        `json:"info" binding:"required"`            // 构建并推送镜像的具体信息
}

type buildImageInfo struct {
	DockerFilePath string            `json:"dockerFilePath" binding:"required"` // dockerfile路径
	ContextPath    string            `json:"contextPath" binding:"required"`    // 构建代码上下文路径
	Destinations   []string          `json:"destinations" binding:"required"`   // 推送镜像完整目标包含仓库地址和tag，例如 xxxx/xxx-hub:v1
	BuildArgs      map[string]string `json:"buildArgs"`                         // 构建参数
	Registries     []types.Registry  `json:"registries" binding:"required"`     // 推送镜像需要的凭据
}
