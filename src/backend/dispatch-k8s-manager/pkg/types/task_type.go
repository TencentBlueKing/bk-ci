package types

import "time"

type TaskState string

// Task状态枚举
const (
	TaskWaiting   TaskState = "waiting"
	TaskRunning   TaskState = "running"
	TaskSucceeded TaskState = "succeeded"
	TaskFailed    TaskState = "failed"
	TaskUnknown   TaskState = "unknown"
)

type TaskAction string

// Task操作枚举 会用于与kubernetes交互，需要注意命名格式
const (
	TaskActionCreate TaskAction = "create"
	TaskActionStop   TaskAction = "stop"
	TaskActionStart  TaskAction = "start"
	TaskActionDelete TaskAction = "delete"
)

type TaskLabelType string

const (
	JobTaskLabel     TaskLabelType = "job"
	BuilderTaskLabel TaskLabelType = "builder"
)

// TaskBelong 任务所属资源
type TaskBelong string

const (
	TaskBelongBuilder = "builder"
	TaskBelongJob     = "job"
)

type Task struct {
	TaskId     string
	TaskKey    string
	TaskBelong TaskBelong
	Action     TaskAction
	Status     TaskState
	Message    []byte
	Props      *interface{}
	ActionTime time.Time
	UpdateTime time.Time
}

type TaskStatus struct {
	Status  *TaskState
	Message []byte
}
