/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

import (
	"time"

	commonTypes "github.com/Tencent/bk-ci/src/booster/common/types"
)

// Task : compile task data struct
type Task struct {
	// Unique ID for an independent task
	ID string

	// The data exchange with client
	Client TaskClient

	// The allocated compiling endpoint for this task
	Compilers []TaskCompiler

	// The task status
	Status TaskStatus

	// The data concerned by operator
	Operator TaskOperator
}

// TaskClient : compile task data with client
type TaskClient struct {
	// Client side specific
	ProjectID     string
	BuildID       string
	ClientIP      string
	ClientCPU     int
	GccVersion    string
	User          string
	City          string
	Message       string
	Params        string
	CCacheEnabled bool
	BanDistCC     bool
	BanAllBooster bool
	StageTimeout  int
	RunDir        string
	CommandType   commonTypes.CommandType
	Command       string
	Extra         TaskClientExtra

	// Client updated
	CacheStats commonTypes.Ccache

	// Server side specific
	Cmd        string
	Env        map[string]string
	RequestCPU float64
	LeastCPU   float64
}

// TaskClientExtra : extra param for compile task
type TaskClientExtra struct {
	BazelRC     string `json:"bazelrc"`
	CCCompiler  string `json:"cc_compiler"`
	CXXCompiler string `json:"cxx_compiler"`
}

// TaskStatus : Status has following cases:
//  - staging:            stay in queue and wait for being pick up
//  - starting:           has been picked up and wait for launching the distCC daemon
//  - running(ready):     has successfully launched distCC daemon and it is ready for connecting and compiling
//  - running(working):   has been connected and now is compiling
//  - failed(unreleased): end service abnormally, wait to collect data and release the resource
//  - finish(unreleased): end service normally, wait to collect data and release the resource
//  - failed(released):   all work has done
//  - finished(released): all work has done
type TaskStatus struct {
	// Task status
	Status     commonTypes.ServerStatusType
	StatusCode StatusCode
	Message    string
	Priority   TaskPriority
	Released   bool

	// Task score
	CompileFilesOK       int64
	CompileFilesErr      int64
	CompileFilesTimeout  int64
	CompilerCount        int
	CPUTotal             float64
	MemTotal             float64
	CCacheInfo           string
	CacheDirectHit       int64
	CachePreprocessedHit int64
	CacheMiss            int64
	FilesInCache         int64
	CacheSize            string
	MaxCacheSize         string

	// Task time
	LastHeartBeatTime time.Time
	StatusChangeTime  time.Time
	CreateTime        time.Time
	UpdateTime        time.Time
	LaunchTime        time.Time
	ReadyTime         time.Time
	ShutDownTime      time.Time
	StartTime         time.Time
	EndTime           time.Time
}

// Ready : update status and save ready time
func (ts *TaskStatus) Ready() {
	ts.ReadyTime = now()
	ts.ChangeStatus(commonTypes.ServerStatusRunning)
}

// Create : update status and save create time
func (ts *TaskStatus) Create() {
	ts.CreateTime = now()
	ts.ChangeStatus(commonTypes.ServerStatusStaging)
}

// Update : save update time
func (ts *TaskStatus) Update() {
	ts.UpdateTime = now()
}

// Launch : update status and save launch time
func (ts *TaskStatus) Launch() {
	ts.LaunchTime = now()
	ts.ChangeStatus(commonTypes.ServerStatusStarting)
}

// ShutDown : update status and save shutdown time
func (ts *TaskStatus) ShutDown() {
	ts.ShutDownTime = now()
	ts.Released = true
}

// ChangeStatus : update status and save time
func (ts *TaskStatus) ChangeStatus(status commonTypes.ServerStatusType) {
	ts.StatusChangeTime = now()
	ts.Status = status
}

// Start : save start time
func (ts *TaskStatus) Start() {
	ts.StartTime = now()
}

// End : save end time
func (ts *TaskStatus) End() {
	ts.EndTime = now()
}

// FailedCodeFinished : update status
func (ts *TaskStatus) FailedCodeFinished() {
	ts.StatusCode = StatusCodeFinished
}

// FailedCodeServerDown : update StatusCode
func (ts *TaskStatus) FailedCodeServerDown() {
	var code StatusCode
	switch ts.Status {
	case commonTypes.ServerStatusRunning:
		code = StatusCodeServerFailedInRunning
	default:
		code = StatusCodeUnknown
	}
	ts.StatusCode = code
}

// FailedCodeClientCancel : update StatusCode
func (ts *TaskStatus) FailedCodeClientCancel() {
	var code StatusCode
	switch ts.Status {
	case commonTypes.ServerStatusStaging:
		code = StatusCodeClientCancelInStaging
	case commonTypes.ServerStatusStarting:
		code = StatusCodeClientCancelInStarting
	case commonTypes.ServerStatusRunning:
		code = StatusCodeClientCancelInRunning
	default:
		code = StatusCodeUnknown
	}
	ts.StatusCode = code
}

// FailedCodeClientLost : update StatusCode
func (ts *TaskStatus) FailedCodeClientLost() {
	var code StatusCode
	switch ts.Status {
	case commonTypes.ServerStatusStaging:
		code = StatusCodeClientLostInStaging
	case commonTypes.ServerStatusStarting:
		code = StatusCodeClientLostInStarting
	case commonTypes.ServerStatusRunning:
		code = StatusCodeClientLostInRunning
	default:
		code = StatusCodeUnknown
	}
	ts.StatusCode = code
}

// TaskOperator : task operator
type TaskOperator struct {
	ClusterID string
	AppName   string
	Namespace string
	Image     string

	Instance          int
	LeastInstance     int
	RequestCPUPerUnit float64
	RequestMemPerUnit float64
}

// StatsInfo : stat info
type StatsInfo struct {
	TCPAccept           int64  `json:"dcc_tcp_accept"`
	RejBadReq           int64  `json:"dcc_rej_bad_req"`
	RejOverload         int64  `json:"dcc_rej_overload"`
	CompileOK           int64  `json:"dcc_compile_ok"`
	CompileErr          int64  `json:"dcc_compile_error"`
	CompileTimeout      int64  `json:"dcc_compile_timeout"`
	CliDisconnect       int64  `json:"dcc_cli_disconnect"`
	Other               int64  `json:"dcc_other"`
	LongestJob          string `json:"dcc_longest_job"`
	LongestJobCompiler  string `json:"dcc_longest_job_compiler"`
	LongestJobTimeMsecs string `json:"dcc_longest_job_time_msecs"`
	MaxRSS              int64  `json:"dcc_max_RSS"`
	MaxRSSName          string `json:"dcc_max_RSS_name"`
	IORate              int64  `json:"dcc_io_rate"`
}

// TaskCompiler : resouce struct
type TaskCompiler struct {
	CPU       float64
	Mem       float64
	Disk      float64
	IP        string
	Port      int
	StatsPort int
	AppPort   int
	Message   string
	Stats     *StatsInfo
	ResUnit   int
	ResStatus int
	// 远端关联的资源id，比如docker id，或者拉起的进程id
	RemoteResID string
	RemoteCmd   string
	// 关联的任务id
	TaskID string
	City   string
}

// // define resource status enums
// const (
// 	ResStatusInit int = 0
// 	ResStatusOk   int = 1
// 	ResStatusFail int = 2
// )

// SameEndpointCompiler : whether two task compiler equal
func SameEndpointCompiler(a, b *TaskCompiler) bool {
	return a.IP == b.IP && a.Port == b.Port && a.StatsPort == b.StatsPort
}

// TaskPriority include 1 ~ 20, the greater number will be set to 20.
// If the priority is 0, the task will never be launch.
type TaskPriority uint

// define priority
const (
	MaxTaskPriority TaskPriority = 20
	MinTaskPriority TaskPriority = 1
)

// StatusCode : uint
type StatusCode uint

// define status enums
const (
	StatusCodeFinished StatusCode = iota
	StatusCodeUnknown
	StatusCodeClientCancelInStaging
	StatusCodeClientCancelInStarting
	StatusCodeClientCancelInRunning

	StatusCodeClientLostInStaging
	StatusCodeClientLostInStarting
	StatusCodeClientLostInRunning

	StatusCodeServerFailedInStarting
	StatusCodeServerFailedInRunning
)

var statusCodeMap = map[StatusCode]string{
	StatusCodeFinished: "compile finished successfully",
	StatusCodeUnknown:  "unknown status code",

	StatusCodeClientCancelInStaging:  "canceled by user when staging",
	StatusCodeClientCancelInStarting: "canceled by user when starting",
	StatusCodeClientCancelInRunning:  "canceled by user when running",

	StatusCodeClientLostInStaging:  "lost user when staging",
	StatusCodeClientLostInStarting: "lost user when starting",
	StatusCodeClientLostInRunning:  "lost user when running",

	StatusCodeServerFailedInStarting: "server failed when starting",
	StatusCodeServerFailedInRunning:  "server failed when running",
}

// String : return status as string
func (sc StatusCode) String() string {
	return statusCodeMap[sc]
}

// ServerAlive : if server alive by status code
func (sc StatusCode) ServerAlive() bool {
	switch sc {
	case StatusCodeFinished, StatusCodeClientCancelInRunning, StatusCodeClientLostInRunning,
		StatusCodeClientCancelInStarting, StatusCodeClientLostInStarting:
		return true
	default:
		return false
	}
}

func now() time.Time {
	return time.Now().Local()
}
