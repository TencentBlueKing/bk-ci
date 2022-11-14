/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package common

import (
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// define const vars
const (
	ControllerScheme = "http"
	ControllerIP     = "127.0.0.1"
	ControllerPort   = 30117
)

// AvailableResp describe the response of available api
type AvailableResp struct {
	PID int32 `json:"pid"`
}

// Flags define flags needed by shader tool
type Flags struct {
	ToolDir       string
	JobDir        string
	JobJSONPrefix string
	JobStartIndex int32
	CommitSuicide bool
	Port          int32
}

// Action define shader action
type Action struct {
	Index    uint64 `json:"index"`
	Cmd      string `json:"cmd"`
	Arg      string `json:"arg"`
	Running  bool   `json:"running"`
	Finished bool   `json:"finished"`
}

// UE4Action define ue4 action
type UE4Action struct {
	ToolJSONFile string          `json:"tool_json_file"`
	ToolJSON     dcSDK.Toolchain `json:"tool_json"`
	Actions      []Action        `json:"shaders"`
}

// ApplyParameters define parameters to apply resource
type ApplyParameters struct {
	ProjectID                string            `json:"project_id"`
	Scene                    string            `json:"scene"`
	ServerHost               string            `json:"server_host"`
	BatchMode                bool              `json:"batch_mode"`
	WorkerList               []string          `json:"specific_host_list"`
	NeedApply                bool              `json:"need_apply"`
	BuildID                  string            `json:"build_id"`
	ShaderToolIdleRunSeconds int               `json:"shader_tool_idle_run_seconds"`
	ControllerIdleRunSeconds int               `json:"controller_idle_run_seconds" value:"120" usage:"controller remain time after there is no active work (seconds)"`
	ControllerNoBatchWait    bool              `json:"controller_no_batch_wait" value:"false" usage:"if true, controller will unregister immediately when no more running task"`
	LimitPerWorker           int               `json:"limit_per_worker"`
	MaxLocalTotalJobs        int               `json:"max_Local_total_jobs"`
	MaxLocalPreJobs          int               `json:"max_Local_pre_jobs"`
	MaxLocalExeJobs          int               `json:"max_Local_exe_jobs"`
	MaxLocalPostJobs         int               `json:"max_Local_post_jobs"`
	Env                      map[string]string `json:"env"`
}

// Actionresult define action result
type Actionresult struct {
	Index     uint64
	Finished  bool
	Succeed   bool
	Outputmsg string
	Errormsg  string
	Exitcode  int
}

func uniqueAndCheck(strlist []string, allindex map[string]bool) []string {
	keys := make(map[string]bool)
	list := make([]string, 0, 0)
	for _, entry := range strlist {
		// remove "-1"
		if entry == "-1" {
			continue
		}

		// remove which not in actions list
		if _, ok := allindex[entry]; !ok {
			continue
		}

		if _, ok := keys[entry]; !ok {
			keys[entry] = true
			list = append(list, entry)
		}
	}
	return list
}

// ResourceStatus save resource status
type ResourceStatus int

// define file send status
const (
	ResourceInit ResourceStatus = iota
	ResourceApplying
	ResourceApplySucceed
	ResourceApplyFailed
	ResourceUnknown = 99
)

var (
	fileStatusMap = map[ResourceStatus]string{
		ResourceInit:         "init",
		ResourceApplying:     "applying",
		ResourceApplySucceed: "applysucceed",
		ResourceApplyFailed:  "applyfailed",
		ResourceUnknown:      "unknown",
	}
)

// String return the string of FileSendStatus
func (f ResourceStatus) String() string {
	if v, ok := fileStatusMap[f]; ok {
		return v
	}

	return "unknown"
}

// SetLogLevel to set log level
func SetLogLevel(level string) {
	if level == "" {
		level = env.GetEnv(env.KeyUserDefinedLogLevel)
	}

	switch level {
	case dcUtil.PrintDebug.String():
		blog.SetV(3)
		blog.SetStderrLevel(blog.StderrLevelInfo)
	case dcUtil.PrintInfo.String():
		blog.SetStderrLevel(blog.StderrLevelInfo)
	case dcUtil.PrintWarn.String():
		blog.SetStderrLevel(blog.StderrLevelWarning)
	case dcUtil.PrintError.String():
		blog.SetStderrLevel(blog.StderrLevelError)
	case dcUtil.PrintNothing.String():
		blog.SetStderrLevel(blog.StderrLevelNothing)
	default:
		// default to be error printer.
		blog.SetStderrLevel(blog.StderrLevelInfo)
	}
}
