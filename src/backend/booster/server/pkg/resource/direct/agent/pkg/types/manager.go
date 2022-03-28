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
)

// define const vars
const (
	URLLaunchApplication  = "http://%s:%d/api/v1/fb/launchappliation"
	URLReleaseApplication = "http://%s:%d/api/v1/fb/releaseappliation"

	URLExecuteCommand = "http://%s:%d/api/v1/build/executecommand"
	URLReportResource = "http://%s:%d/api/v1/build/reportresource"

	DistCCDaemonMemPerCPU = 2048 // MB

	AgentResourceCheckTime  = 20 * time.Second
	AgentTaskCheckTime      = 10 * time.Second
	AgentReportIntervalTime = 5 * time.Second

	FbuildVersionEnvKey = "FB_VERSION"
)

// define cpu num per unit
var (
	DistCCDaemonCPUPerUnit = 4 // jobs per container, 4 for default, can be changed by config
)

// AllocateServerResourceType : struct data to notify fast build agent to launch application
type AllocateServerResourceType struct {
	ReadyAllocateResource TaskCompiler
	RemoteCmd             string
	Parameter             string
	Envs                  map[string]string `json:"envs"`
}

// ReportAgentResource : struct of report resource
type ReportAgentResource struct {
	AgentInfo
}

// define cmd type
const (
	CmdLaunch  = "launch"
	CmdRelease = "release"
)

// NotifyAgentData : struct to notify agent
type NotifyAgentData struct {
	// 资源使用者的ID
	UserID     string `json:"user_id"`
	ResBatchID string `json:"res_batch_id"`
	// 用户自定义ID，具体含义由资源使用者自己解释
	UserDefineID string `json:"user_define_id"`
	// 命令的工作目录
	Dir string `json:"dir"`
	// 可执行文件路径，如果是相对路径，则相对于Dir
	Path       string            `json:"path"`
	Cmd        string            `json:"cmd"`
	Parameters []string          `json:"parameters"`
	Env        map[string]string `json:"env"`
	Additional map[string]string `json:"additional"`
	CmdType    string            `json:"cmd_type"`
	// 保存关联的命令字和id，比如执行释放命令时，需要带上启动命令和进程id，便于agent侧执行相应的释放
	ReferCmd string `json:"refer_cmd"`
	ReferID  string `json:"refer_id"`
}

func max(a float64, b float64) float64 {
	if a > b {
		return a
	}

	return b
}

// Dec : to dec resource
func (a *Resource) Dec(other *Resource) error {
	if other == nil {
		return ErrNilObject
	}

	a.CPU = max(a.CPU-other.CPU, 0)
	a.Mem = max(a.Mem-other.Mem, 0)
	a.Disk = max(a.Disk-other.Disk, 0)

	return nil
}

// Dec2 : to dec resource
func (a *Resource) Dec2(cpu float64, memory float64, disk float64) error {
	a.CPU = max(a.CPU-cpu, 0)
	a.Mem = max(a.Mem-memory, 0)
	a.Disk = max(a.Disk-disk, 0)

	return nil
}

// Add : to add resource
func (a *Resource) Add(other *Resource) error {
	if other == nil {
		return ErrNilObject
	}

	a.CPU = a.CPU + other.CPU
	a.Mem = a.Mem + other.Mem
	a.Disk = a.Disk + other.Disk

	return nil
}

// Resource : 资源信息
type Resource struct {
	CPU  float64 `json:"cpu"`
	Mem  float64 `json:"mem"`
	Disk float64 `json:"disk"`
}

// AgentBase : agent info
type AgentBase struct {
	IP      string            `json:"ip"`
	Port    int               `json:"port"`
	Message string            `json:"message"`
	Cluster string            `json:"cluster"`
	Labels  map[string]string `json:"labels"`
}

// AgentInfo : agent info
type AgentInfo struct {
	Base      AgentBase        `json:"base"`
	Total     Resource         `json:"total"`
	Free      Resource         `json:"free"`
	Allocated []*AllocatedInfo `json:"allocated"`
}

// AllocatedInfo ： 已分配的资源信息
type AllocatedInfo struct {
	AllocatedResource Resource `json:"allocated_resource"`
	// 资源使用者id
	UserID string `json:"user_id"`
	// 资源分配id（分配资源时生成的id）
	ResBatchID string         `json:"res_batch_id"`
	Commands   []*CommandInfo `json:"commands"`
}

// CommandInfo ： 命令信息
type CommandInfo struct {
	// 远端关联的id，比如拉起的进程id
	ID   string `json:"id"`
	Cmd  string `json:"cmd"`
	Port int    `json:"port"`
	// 状态
	Status CommandStatusType `json:"status"`
	// 用户自定义ID，具体含义由资源使用者自己解释
	UserDefineID string `json:"user_define_id"`
}

// CommandStatusType : type for worker status
type CommandStatusType int

// define resource status
const (
	// 已分配，等待worker拉起
	CommandStatusInit CommandStatusType = iota
	// Command成功
	CommandStatusSucceed
	// Command失败
	CommandStatusFail
)

// String return the string of CommandStatusType
func (rst CommandStatusType) String() string {
	if s, ok := commandStatusTypeMap[rst]; ok {
		return s
	}

	return "unknown status"
}

var commandStatusTypeMap = map[CommandStatusType]string{
	CommandStatusInit:    "init",
	CommandStatusSucceed: "succeed",
	CommandStatusFail:    "fail",
}
