/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package v1

import (
	"os/user"

	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/api"
	v2 "github.com/Tencent/bk-ci/src/booster/server/pkg/api/v2"

	"google.golang.org/protobuf/proto"
)

// AvailableResp describe the response of controller available check
type AvailableResp struct {
	Pid int `json:"pid"`
}

// WorkStatusResp describe the response of getting work status
type WorkStatusResp struct {
	Status *dcSDK.WorkStatusDetail `json:"status"`
}

// WorkRegisterParam describe the param to register work
type WorkRegisterParam struct {
	BatchMode        bool           `json:"batch_mode"`
	ServerHost       string         `json:"server_host"`
	SpecificHostList []string       `json:"specific_host_list"`
	NeedApply        bool           `json:"need_apply"`
	Apply            *v2.ParamApply `json:"apply"`
}

// WorkRegisterResp describe the response of registering work
type WorkRegisterResp struct {
	WorkID      string `json:"work_id"`
	BatchLeader bool   `json:"bath"`
}

// WorkSlotsOccupyParam describe the param to lock remote slots
type WorkSlotsOccupyParam struct {
	Usage dcSDK.JobUsage `json:"usage"`
}

// WorkSlotsOccupyResp describe the response of locking remote slots
type WorkSlotsOccupyResp struct {
	Host *dcProtocol.Host `json:"host_slots"`
}

// WorkSlotsFreeParam describe the param to unlock remote slots
type WorkSlotsFreeParam struct {
	Host  *dcProtocol.Host `json:"host_slots"`
	Usage dcSDK.JobUsage   `json:"usage"`
}

// LocalSlotsOccupyParam describe the param to lock local slots
type LocalSlotsOccupyParam struct {
	Usage  dcSDK.JobUsage `json:"usage"`
	Weight int32          `json:"weight"`
}

// LocalSlotsFreeParam describe the param to unlock local slots
type LocalSlotsFreeParam struct {
	Usage  dcSDK.JobUsage `json:"usage"`
	Weight int32          `json:"weight"`
}

// WorkSettingsParam describe the param to update work settings to controller
type WorkSettingsParam struct {
	TaskID          string                 `json:"task_id"`
	ProjectID       string                 `json:"project_id"`
	Scene           string                 `json:"scene"`
	UsageLimit      map[dcSDK.JobUsage]int `json:"usage_limit"`
	LocalTotalLimit int                    `json:"local_total_limit"`
	Preload         *dcSDK.PreloadConfig   `json:"preload"`
	FilterRules     []dcSDK.FilterRuleItem `json:"filter_rules"`
	Degraded        bool                   `json:"degraded"`
	GlobalSlots     bool                   `json:"global_slots"`
}

// WorkSettingsResp describe the response of updating work settings to controller
type WorkSettingsResp struct {
	TaskID          string                 `json:"task_id"`
	ProjectID       string                 `json:"project_id"`
	Scene           string                 `json:"scene"`
	UsageLimit      map[dcSDK.JobUsage]int `json:"usage_limit"`
	LocalTotalLimit int                    `json:"local_total_limit"`
	Preload         *dcSDK.PreloadConfig   `json:"preload"`
	FilterRules     []dcSDK.FilterRuleItem `json:"filter_rules"`
}

// WorkerKeyConfigParam describe the param of work unique key
type WorkerKeyConfigParam struct {
	BatchMode bool   `json:"batch_mode"`
	ProjectID string `json:"project_id"`
	Scene     string `json:"scene"`
}

// CommonConfigParam describe the param to update common controller config
type CommonConfigParam struct {
	Configkey dcSDK.CommonConfigKey `json:"config_key"`
	WorkerKey WorkerKeyConfigParam  `json:"worker_key"`
	//Config    interface{}           `json:"config"`
	Data []byte `json:"data"`
}

// ToolChainParam describe the param to set toolchain
type ToolChainParam struct {
	ToolKey                string           `json:"tool_key"`
	ToolName               string           `json:"tool_name"`
	ToolLocalFullPath      string           `json:"tool_local_full_path"`
	ToolRemoteRelativePath string           `json:"tool_remote_relative_path"`
	Files                  []dcSDK.ToolFile `json:"files"`
}

// WorkUnregisterParam describe the param to unregister work
type WorkUnregisterParam struct {
	Force   bool             `json:"force"`
	Release *v2.ParamRelease `json:"release"`
}

// JobStatsParam describe the param to update job stats
type JobStatsParam struct {
	dcSDK.ControllerJobStats
}

// WorkStatsParam describe the param to update work stats
type WorkStatsParam struct {
	Success bool `json:"success"`
}

// Message describe the message sent to controller
type Message struct {
	Pid     int          `json:"pid"`
	Level   MessageLevel `json:"level"`
	WorkID  string       `json:"work_id"`
	Message string       `json:"message"`
}

// MessageLevel describe the message level
type MessageLevel string

const (
	MessageInfo  MessageLevel = "INFO"
	MessageWarn  MessageLevel = "WARN"
	MessageError MessageLevel = "ERROR"
)

// RemoteTaskExecuteParam describe the param to do remote task execute directly
type RemoteTaskExecuteParam struct {
	Pid   int                       `json:"pid"`
	Req   *dcSDK.BKDistCommand      `json:"req"`
	Stats *dcSDK.ControllerJobStats `json:"stats"`
}

// RemoteTaskExecuteResp describe the response of doing remote task execute directly
type RemoteTaskExecuteResp struct {
	Result *dcSDK.BKDistResult `json:"result"`
}

// RemoteTaskSendFileParam describe the param to send files to remote
type RemoteTaskSendFileParam struct {
	Pid   int                       `json:"pid"`
	Dir   string                    `json:"dir"`
	Req   []dcSDK.FileDesc          `json:"req"`
	Stats *dcSDK.ControllerJobStats `json:"stats"`
}

// RemoteTaskSendFileResp describe the response of sending files to remote
type RemoteTaskSendFileResp struct {
	Result *dcSDK.BKSendFileResult `json:"result"`
}

// LocalTaskExecuteParam describe the param to do local task execute to controller
type LocalTaskExecuteParam struct {
	Pid          int                       `json:"pid"`
	Dir          string                    `json:"dir"`
	Commands     []string                  `json:"commands"`
	Environments []string                  `json:"environment"`
	Stats        *dcSDK.ControllerJobStats `json:"stats"`
	User         user.User                 `json:"user"`
}

// LocalTaskExecuteResp describe the response of doing local task execute to controller
type LocalTaskExecuteResp struct {
	Result *dcSDK.LocalTaskResult `json:"result"`
}

// Write2Resp write the http response body from LocalTaskExecuteResp
func (l *LocalTaskExecuteResp) Write2Resp(resp *api.RestResponse) {
	resp.Check()
	if l.Result == nil {
		l.Result = &dcSDK.LocalTaskResult{}
	}

	var code = int32(resp.ErrCode.Int())
	var exitCode = int32(l.Result.ExitCode)
	var res = code == 0
	var msg = []byte(resp.Message)

	r, _ := proto.Marshal(&PBLocalExecuteResult{
		Basic: &PBHttpResult{
			Code:    &code,
			Result:  &res,
			Message: msg,
		},
		ExitCode: &exitCode,
		Stdout:   l.Result.Stdout,
		Stderr:   l.Result.Stderr,
		Message:  []byte(l.Result.Message),
	})

	resp.Resp.WriteHeader(resp.HTTPCode)
	_, _ = resp.Resp.Write(resp.Wrap(r))
}

// Read parse LocalTaskExecuteResp from http response body
func (l *LocalTaskExecuteResp) Read(data []byte) (int, string, error) {
	var r PBLocalExecuteResult
	if err := proto.Unmarshal(data, &r); err != nil {
		return 0, "", err
	}

	l.Result = &dcSDK.LocalTaskResult{
		ExitCode: int(r.GetExitCode()),
		Stdout:   r.GetStdout(),
		Stderr:   r.GetStderr(),
		Message:  string(r.GetMessage()),
	}

	return int(r.Basic.GetCode()), string(r.Basic.GetMessage()), nil
}

type WorkerChanged struct {
	OldWorkID string `json:"old_work_id"`
	NewWorkID string `json:"new_work_id"`
}
