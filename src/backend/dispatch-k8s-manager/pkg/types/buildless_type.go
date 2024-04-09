package types

import "fmt"
import "strconv"

// BuildLessStartInfo 结构体用于保存构建启动信息
type BuildLessStartInfo struct {
	ProjectId             string                `json:"projectId"`
	AgentId               string                `json:"agentId"`
	PipelineId            string                `json:"pipelineId"`
	BuildId               string                `json:"buildId"`
	VmSeqId               int                   `json:"vmSeqId"`
	SecretKey             string                `json:"secretKey"`
	ExecutionCount        int                   `json:"executionCount"`
	RejectedExecutionType RejectedExecutionType `json:"rejectedExecutionType"`
}

// BuildLessEndInfo 结构体用于保存构建结束信息
type BuildLessEndInfo struct {
	ProjectId  string `json:"projectId"`
	PipelineId string `json:"pipelineId"`
	BuildId    string `json:"buildId"`
	VmSeqId    int    `json:"vmSeqId"`
	PodName    string `json:"podName"`
	PoolNo     int    `json:"poolNo"`
}

// BuildLessTask 结构体用于保存构建信息
type BuildLessTask struct {
	ProjectId      string `json:"projectId"`
	AgentId        string `json:"agentId"`
	PipelineId     string `json:"pipelineId"`
	BuildId        string `json:"buildId"`
	VmSeqId        int    `json:"vmSeqId"`
	SecretKey      string `json:"secretKey"`
	ExecutionCount int    `json:"executionCount"`
}

func (n BuildLessTask) String() string {
	return fmt.Sprintf("BuildLessTask[ProjectId=%s, PipelineId=%s, BuildId=%s, VmSeqId=%s, ExecutionCount=%s]", n.ProjectId, n.PipelineId, n.BuildId, strconv.Itoa(n.VmSeqId), strconv.Itoa(n.ExecutionCount))
}

// RejectedExecutionType 类型用于表示拒绝策略
type RejectedExecutionType string

// 用 iota 定义 RejectedExecutionType 的枚举值
const (
	// 表示可以拒绝的策略
	AbortPolicy RejectedExecutionType = "abort"

	// 表示不可拒绝，必须执行的策略
	FollowPolicy RejectedExecutionType = "follow"

	// 表示插队执行的策略
	JumpPolicy RejectedExecutionType = "jump"
)

type BuildLessTaskState string

// Task状态枚举
const (
	BuildLessTaskRunning   BuildLessTaskState = "running"
	BuildLessTaskSucceeded BuildLessTaskState = "succeeded"
	BuildLessTaskFailed    BuildLessTaskState = "failed"
)
