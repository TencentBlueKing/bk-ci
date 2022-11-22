/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package engine

import (
	"time"

	selfMetric "github.com/Tencent/bk-ci/src/booster/server/pkg/metric"
)

// TaskExtension describe the extension part beyond task basic according to different engines
type TaskExtension interface {
	// if task get enough available resource
	EnoughAvailableResource() bool

	// get worker list from task
	WorkerList() []string

	GetRequestInstance() int

	GetWorkerCount() int

	// dump the task data
	Dump() []byte

	// return the engine custom data from task
	CustomData(interface{}) interface{}
}

type TaskStatusType string

const (
	TaskStatusInit     TaskStatusType = "init"
	TaskStatusStaging  TaskStatusType = "staging"
	TaskStatusStarting TaskStatusType = "starting"
	TaskStatusRunning  TaskStatusType = "running"
	TaskStatusFailed   TaskStatusType = "failed"
	TaskStatusFinish   TaskStatusType = "finish"
)

// Terminated check if the task status is in terminated
func (tst TaskStatusType) Terminated() bool {
	switch tst {
	case TaskStatusFinish, TaskStatusFailed:
		return true
	default:
		return false
	}
}

// TaskListOptions describe tasks list filter conditions
// get all status if len(Status) is 0
// get all no matter released or not if Released is nil
type TaskListOptions struct {
	Status   []TaskStatusType
	Released *bool
}

// NewTaskListOptions get a new task list options
func NewTaskListOptions(released *bool, statusList ...TaskStatusType) TaskListOptions {
	return TaskListOptions{
		Status:   statusList,
		Released: released,
	}
}

type TaskStatusCode uint

const (
	TaskStatusCodeFinished TaskStatusCode = iota
	TaskStatusCodeUnknown
	TaskStatusCodeClientCancelInStaging
	TaskStatusCodeClientCancelInStarting
	TaskStatusCodeClientCancelInRunning

	TaskStatusCodeClientLostInStaging
	TaskStatusCodeClientLostInStarting
	TaskStatusCodeClientLostInRunning

	TaskStatusCodeServerFailedInStaging
	TaskStatusCodeServerFailedInStarting
	TaskStatusCodeServerFailedInRunning
)

var statusCodeMap = map[TaskStatusCode]string{
	TaskStatusCodeFinished: "compile finished successfully",
	TaskStatusCodeUnknown:  "unknown status code",

	TaskStatusCodeClientCancelInStaging:  "canceled by user when staging",
	TaskStatusCodeClientCancelInStarting: "canceled by user when starting",
	TaskStatusCodeClientCancelInRunning:  "canceled by user when running",

	TaskStatusCodeClientLostInStaging:  "lost user when staging",
	TaskStatusCodeClientLostInStarting: "lost user when starting",
	TaskStatusCodeClientLostInRunning:  "lost user when running",

	TaskStatusCodeServerFailedInStaging:  "server failed when staging",
	TaskStatusCodeServerFailedInStarting: "server failed when starting",
	TaskStatusCodeServerFailedInRunning:  "server failed when running",
}

// String get task status code string
func (sc TaskStatusCode) String() string {
	t, ok := statusCodeMap[sc]
	if ok {
		return t
	}
	return "unknown"
}

// ServerAlive if task server still alive in current status
func (sc TaskStatusCode) ServerAlive() bool {
	switch sc {
	case TaskStatusCodeFinished, TaskStatusCodeClientCancelInRunning, TaskStatusCodeClientLostInRunning,
		TaskStatusCodeClientCancelInStarting, TaskStatusCodeClientLostInStarting:
		return true
	default:
		return false
	}
}

// TaskPriority include 1 ~ 20, the greater number will be set to 20.
// If the priority is 0, the task will never be launch.
type TaskPriority uint

const (
	MaxTaskPriority     TaskPriority = 20
	MinTaskPriority     TaskPriority = 1
	DefaultTaskPriority TaskPriority = 10

	DefaultTaskStageTimeout = 60
)

// TaskBasic describe task basic struct data
type TaskBasic struct {
	// the primary key of tasks
	ID string

	// settings when created
	Client TaskBasicClient

	// status after created
	Status TaskBasicStatus
}

// Check check if the task basic valid
func (tb *TaskBasic) Check() error {
	if tb.Client.QueueName == "" {
		return ErrorNoQueueNameSpecified
	}

	if tb.Client.Priority < MinTaskPriority || tb.Client.Priority > MaxTaskPriority {
		tb.Client.Priority = DefaultTaskPriority
	}

	if tb.Client.StageTimeout <= 0 {
		tb.Client.StageTimeout = DefaultTaskStageTimeout
	}

	return nil
}

// CopyTaskBasic deep copy a new task basic
func CopyTaskBasic(task *TaskBasic) *TaskBasic {
	t := new(TaskBasic)
	*t = *task
	return t
}

// TaskBasicClient describe task basic settings
type TaskBasicClient struct {
	EngineName    TypeName
	QueueName     string
	ProjectID     string
	BuildID       string
	Priority      TaskPriority
	ClientIP      string
	ClientCPU     int
	ClientVersion string
	StageTimeout  int
	Message       string
}

// TaskBasicStatus describe task basic status
type TaskBasicStatus struct {
	Status     TaskStatusType
	StatusCode TaskStatusCode
	Message    string

	Released bool

	// Task time
	LastHeartBeatTime time.Time
	StatusChangeTime  time.Time
	InitTime          time.Time
	CreateTime        time.Time
	UpdateTime        time.Time
	LaunchTime        time.Time
	ReadyTime         time.Time
	ShutDownTime      time.Time
	StartTime         time.Time
	EndTime           time.Time
}

// Init make task init, right after basic created.
func (tbs *TaskBasicStatus) Init() {
	tbs.InitTime = now()
	tbs.ChangeStatus(TaskStatusInit)
}

// Ready make task ready, right after task server is running
func (tbs *TaskBasicStatus) Ready() {
	tbs.ReadyTime = now()
	tbs.ChangeStatus(TaskStatusRunning)
}

// Create make task created, right after basic and extension are all created.
func (tbs *TaskBasicStatus) Create() {
	tbs.CreateTime = now()
	tbs.ChangeStatus(TaskStatusStaging)
}

// Update record task update
func (tbs *TaskBasicStatus) Update() {
	tbs.UpdateTime = now()
}

// Launch make task launched, right after task server is starting
func (tbs *TaskBasicStatus) Launch() {
	tbs.LaunchTime = now()
	tbs.ChangeStatus(TaskStatusStarting)
}

// ShutDown make task shutdown, right after task server is released
func (tbs *TaskBasicStatus) ShutDown() {
	tbs.ShutDownTime = now()
	tbs.Released = true
}

// ChangeStatus change task status
func (tbs *TaskBasicStatus) ChangeStatus(status TaskStatusType) {
	if tbs.Status == status {
		return
	}

	statusChangeTime := now()

	// record status change metric data
	if tbs.Status != "" {

		// separate running status with other status, because running status is up to different projects and it's long-span
		// but other status duration are more stable
		if tbs.Status == TaskStatusRunning {
			selfMetric.TaskRunningTimeController.Observe(statusChangeTime.Sub(tbs.StatusChangeTime).Seconds())
		} else {
			selfMetric.TaskTimeController.Observe(string(tbs.Status), statusChangeTime.Sub(tbs.StatusChangeTime).Seconds())
		}
	}

	tbs.StatusChangeTime = statusChangeTime
	tbs.Status = status
}

// Start record the task start time, right after the task server begins to provide service.
func (tbs *TaskBasicStatus) Start() {
	tbs.StartTime = now()
}

// End record the task end time, right after the task server end the service.
func (tbs *TaskBasicStatus) End() {
	tbs.EndTime = now()
}

// Finish make task finish, right after the task finish without any error.
func (tbs *TaskBasicStatus) Finish() {
	tbs.StatusCode = TaskStatusCodeFinished
	tbs.ChangeStatus(TaskStatusFinish)
}

// Beats record the heartbeat time.
func (tbs *TaskBasicStatus) Beats() {
	tbs.LastHeartBeatTime = now()
}

// FailWithServerDown make task failed by server
func (tbs *TaskBasicStatus) FailWithServerDown() {
	var code TaskStatusCode
	switch tbs.Status {
	case TaskStatusStaging:
		code = TaskStatusCodeServerFailedInStaging
	case TaskStatusStarting:
		code = TaskStatusCodeServerFailedInStarting
	case TaskStatusRunning:
		code = TaskStatusCodeServerFailedInRunning
	default:
		code = TaskStatusCodeUnknown
	}
	tbs.StatusCode = code
	tbs.ChangeStatus(TaskStatusFailed)
}

// FailWithClientCancel make task failed by client side request
func (tbs *TaskBasicStatus) FailWithClientCancel() {
	var code TaskStatusCode
	switch tbs.Status {
	case TaskStatusStaging:
		code = TaskStatusCodeClientCancelInStaging
	case TaskStatusStarting:
		code = TaskStatusCodeClientCancelInStarting
	case TaskStatusRunning:
		code = TaskStatusCodeClientCancelInRunning
	default:
		code = TaskStatusCodeUnknown
	}
	tbs.StatusCode = code
	tbs.ChangeStatus(TaskStatusFailed)
}

// FailWithClientLost make task failed by client lost
func (tbs *TaskBasicStatus) FailWithClientLost() {
	var code TaskStatusCode
	switch tbs.Status {
	case TaskStatusStaging:
		code = TaskStatusCodeClientLostInStaging
	case TaskStatusStarting:
		code = TaskStatusCodeClientLostInStarting
	case TaskStatusRunning:
		code = TaskStatusCodeClientLostInRunning
	default:
		code = TaskStatusCodeUnknown
	}
	tbs.StatusCode = code
	tbs.ChangeStatus(TaskStatusFailed)
}

func now() time.Time {
	return time.Now().Local()
}
