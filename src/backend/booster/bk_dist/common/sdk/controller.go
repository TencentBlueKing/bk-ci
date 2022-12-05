/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package sdk

import (
	"fmt"
	"net"
	"strconv"
	"strings"
	"time"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	v2 "github.com/Tencent/bk-ci/src/booster/server/pkg/api/v2"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
)

// ControllerSDK describe the controller handler SDK
type ControllerSDK interface {
	EnsureServer() (int, error)
	Register(config ControllerRegisterConfig) (ControllerWorkSDK, error)
	GetWork(workID string) ControllerWorkSDK
	SetConfig(config *CommonControllerConfig) error
}

// ControllerWorkSDK describe the controller work handler SDK
// this is working under a existing work, which is registered by ControllerSDK
type ControllerWorkSDK interface {
	ID() string
	Job(stats *ControllerJobStats) WorkJob
	Unregister(config ControllerUnregisterConfig) error
	LockLocalSlot(usage JobUsage) error
	UnLockLocalSlot(usage JobUsage) error
	Start() error
	End() error
	Status() (*WorkStatusDetail, error)
	SetSettings(settings *ControllerWorkSettings) error
	GetSettings() (*ControllerWorkSettings, error)
	UpdateJobStats(stats *ControllerJobStats) error
	RecordWorkStats(stats *ControllerWorkStats) error
	IsBatchLeader() bool
}

// WorkJob describe the single job handler SDK
// this is working under a single job
type WorkJob interface {
	ExecuteRemoteTask(req *BKDistCommand) (*BKDistResult, error)
	// return http code / http message / execute result / execute error
	ExecuteLocalTask(commands []string, workdir string) (int, string, *LocalTaskResult, error)
	SendRemoteFile2All(req []FileDesc) error
}

const (
	WorkHeartbeatTick    = 5 * time.Second
	WorkHeartbeatTimeout = 10 * WorkHeartbeatTick
)

// GetControllerConfigFromEnv generate the controller config from environment variables
func GetControllerConfigFromEnv() ControllerConfig {
	config := ControllerConfig{
		NoLocal: false,
		Scheme:  "http",
		IP:      "127.0.0.1",
		Port:    30117,
	}

	if tmp := env.GetEnv(env.KeyExecutorControllerNoLocal); tmp != "" {
		config.NoLocal = true
	}

	if tmp := env.GetEnv(env.KeyExecutorControllerScheme); tmp != "" {
		config.Scheme = tmp
	}

	if tmp := env.GetEnv(env.KeyExecutorControllerIP); tmp != "" {
		config.IP = tmp
	}

	if tmp := env.GetEnv(env.KeyExecutorControllerPort); tmp != "" {
		config.Port, _ = strconv.Atoi(tmp)
	}

	return config
}

// GetControllerConfigToEnv encode controller config to environment variables
func GetControllerConfigToEnv(config ControllerConfig) map[string]string {
	result := make(map[string]string)

	if config.NoLocal {
		result[env.KeyExecutorControllerNoLocal] = "1"
	}

	result[env.KeyExecutorControllerScheme] = config.Scheme
	result[env.KeyExecutorControllerIP] = config.IP
	result[env.KeyExecutorControllerPort] = fmt.Sprintf("%d", config.Port)

	return result
}

// ControllerConfig describe the config of controller
type ControllerConfig struct {
	// 需要传递给executor的信息
	NoLocal bool
	Scheme  string
	IP      string
	Port    int

	// controller参数
	Timeout            time.Duration
	LogVerbosity       int
	LogDir             string
	TotalSlots         int
	PreSlots           int
	ExeSlots           int
	PostSlots          int
	RemainTime         int
	Sudo               bool
	NoWait             bool
	UseLocalCPUPercent int
	DisableFileLock    bool
	AutoResourceMgr    bool
	ResIdleSecsForFree int
	SendCork           bool
}

// Target return the server ip and port of controller
func (cc ControllerConfig) Target() string {
	return net.JoinHostPort(cc.IP, fmt.Sprintf("%d", cc.Port))
}

// Address return the http address of controller
func (cc ControllerConfig) Address() string {
	return fmt.Sprintf("%s://%s", cc.Scheme, cc.Target())
}

type CommonConfigKey string

const (
	CommonConfigKeyToolChain CommonConfigKey = "common_config_key_tool_chain"
)

// WorkerKeyConfig describe the worker unique key
type WorkerKeyConfig struct {
	BatchMode bool
	ProjectID string
	Scene     string
}

// CommonControllerConfig describe the common config of controller
// which can be set after controller is launched
type CommonControllerConfig struct {
	Configkey CommonConfigKey `json:"config_key"`
	WorkerKey WorkerKeyConfig `json:"worker_key"`
	//Config    interface{}     `json:"config"`
	Data []byte `json:"data"`
}

// ControllerRegisterConfig describe the register config
type ControllerRegisterConfig struct {
	BatchMode        bool
	ServerHost       string
	SpecificHostList []string
	NeedApply        bool
	Apply            *v2.ParamApply
}

// ControllerUnregisterConfig describe the unregister config
type ControllerUnregisterConfig struct {
	Force   bool
	Release *v2.ParamRelease
}

// ControllerWorkSettings describe the work config
// which can be set after work is registered
type ControllerWorkSettings struct {
	TaskID          string
	ProjectID       string
	Scene           string
	UsageLimit      map[JobUsage]int
	LocalTotalLimit int
	Preload         *PreloadConfig
	FilterRules     []FilterRuleItem
	Degraded        bool
	GlobalSlots     bool
}

// ControllerJobStats describe a single job's stats info
type ControllerJobStats struct {
	ID                       string `json:"id"`
	Pid                      int    `json:"pid"`
	WorkID                   string `json:"work_id"`
	TaskID                   string `json:"task_id"`
	BoosterType              string `json:"booster_type"`
	RemoteWorker             string `json:"remote_worker"`
	RemoteTryTimes           int    `json:"remote_try_times"`
	RemoteWorkTimeoutSec     int    `json:"remote_work_timeout_sec"`
	RemoteWorkTimeoutSetting int    `json:"remote_work_timeout_setting"`

	Success                         bool `json:"success"`
	PreWorkSuccess                  bool `json:"pre_work_success"`
	RemoteWorkSuccess               bool `json:"remote_work_success"`
	PostWorkSuccess                 bool `json:"post_work_success"`
	FinalWorkSuccess                bool `json:"final_work_success"`
	LocalWorkSuccess                bool `json:"local_work_success"`
	RemoteWorkTimeout               bool `json:"remote_work_timeout"`
	RemoteWorkFatal                 bool `json:"remote_work_fatal"`
	RemoteWorkTimeoutUseSuggest     bool `json:"remote_work_timeout_use_suggest"`
	RemoteWorkOftenRetryAndDegraded bool `json:"remote_work_often_retry_and_degraded"`

	OriginArgs         []string `json:"origin_args"`
	RemoteErrorMessage string   `json:"remote_error_message"`

	EnterTime StatsTime `json:"enter_time"`
	LeaveTime StatsTime `json:"leave_time"`

	PreWorkEnterTime  StatsTime `json:"pre_work_enter_time"`
	PreWorkLeaveTime  StatsTime `json:"pre_work_leave_time"`
	PreWorkLockTime   StatsTime `json:"pre_work_lock_time"`
	PreWorkUnlockTime StatsTime `json:"pre_work_unlock_time"`
	PreWorkStartTime  StatsTime `json:"pre_work_start_time"`
	PreWorkEndTime    StatsTime `json:"pre_work_end_time"`

	PostWorkEnterTime  StatsTime `json:"post_work_enter_time"`
	PostWorkLeaveTime  StatsTime `json:"post_work_leave_time"`
	PostWorkLockTime   StatsTime `json:"post_work_lock_time"`
	PostWorkUnlockTime StatsTime `json:"post_work_unlock_time"`
	PostWorkStartTime  StatsTime `json:"post_work_start_time"`
	PostWorkEndTime    StatsTime `json:"post_work_end_time"`

	FinalWorkStartTime StatsTime `json:"final_work_start_time"`
	FinalWorkEndTime   StatsTime `json:"final_work_end_time"`

	RemoteWorkEnterTime           StatsTime `json:"remote_work_enter_time"`
	RemoteWorkLeaveTime           StatsTime `json:"remote_work_leave_time"`
	RemoteWorkLockTime            StatsTime `json:"remote_work_lock_time"`
	RemoteWorkUnlockTime          StatsTime `json:"remote_work_unlock_time"`
	RemoteWorkStartTime           StatsTime `json:"remote_work_start_time"`
	RemoteWorkEndTime             StatsTime `json:"remote_work_end_time"`
	RemoteWorkPackStartTime       StatsTime `json:"remote_work_pack_start_time"`
	RemoteWorkPackEndTime         StatsTime `json:"remote_work_pack_end_time"`
	RemoteWorkSendStartTime       StatsTime `json:"remote_work_send_start_time"`
	RemoteWorkSendEndTime         StatsTime `json:"remote_work_send_end_time"`
	RemoteWorkPackCommonStartTime StatsTime `json:"remote_work_pack_common_start_time"`
	RemoteWorkPackCommonEndTime   StatsTime `json:"remote_work_pack_common_end_time"`
	RemoteWorkSendCommonStartTime StatsTime `json:"remote_work_send_common_start_time"`
	RemoteWorkSendCommonEndTime   StatsTime `json:"remote_work_send_common_end_time"`
	RemoteWorkProcessStartTime    StatsTime `json:"remote_work_process_start_time"`
	RemoteWorkProcessEndTime      StatsTime `json:"remote_work_process_end_time"`
	RemoteWorkReceiveStartTime    StatsTime `json:"remote_work_receive_start_time"`
	RemoteWorkReceiveEndTime      StatsTime `json:"remote_work_receive_end_time"`
	RemoteWorkUnpackStartTime     StatsTime `json:"remote_work_unpack_start_time"`
	RemoteWorkUnpackEndTime       StatsTime `json:"remote_work_unpack_end_time"`

	LocalWorkEnterTime  StatsTime `json:"local_work_enter_time"`
	LocalWorkLeaveTime  StatsTime `json:"local_work_leave_time"`
	LocalWorkLockTime   StatsTime `json:"local_work_lock_time"`
	LocalWorkUnlockTime StatsTime `json:"local_work_unlock_time"`
	LocalWorkStartTime  StatsTime `json:"local_work_start_time"`
	LocalWorkEndTime    StatsTime `json:"local_work_end_time"`
}

// ControllerWorkStats describe the work stats info
type ControllerWorkStats struct {
	Success bool `json:"success"`
}

const (
	ControllerBinary = "bk-dist-controller"
)

// define errors when launch controller
var (
	ErrControllerNotAvailable   = fmt.Errorf("controller not available")
	ErrControllerNeedBeLaunched = fmt.Errorf("controller need be launched")
	ErrControllerNotReady       = fmt.Errorf("controller not ready")
	ErrControllerKilled         = fmt.Errorf("controller killed")
)

// WorkStatusDetail describe the work status
type WorkStatusDetail struct {
	Status  WorkStatus       `json:"status"`
	Message string           `json:"message"`
	Task    *v2.RespTaskInfo `json:"task"`
}

type WorkStatus int

const (
	WorkStatusUnknown WorkStatus = iota
	WorkStatusInit
	WorkStatusRegistered
	WorkStatusResourceApplying
	WorkStatusResourceApplied
	WorkStatusResourceApplyFailed
	WorkStatusWorking
	WorkStatusEnded
	WorkStatusUnregistered
	WorkStatusRemovable
)

var workStatusString = map[WorkStatus]string{
	WorkStatusUnknown:             "unknown",
	WorkStatusInit:                "init",
	WorkStatusRegistered:          "registered",
	WorkStatusResourceApplying:    "resource_applying",
	WorkStatusResourceApplied:     "resource_applied",
	WorkStatusResourceApplyFailed: "resource_failed",
	WorkStatusWorking:             "working",
	WorkStatusEnded:               "ended",
	WorkStatusUnregistered:        "unregistered",
	WorkStatusRemovable:           "removable",
}

// String return the string of work status
func (ws WorkStatus) String() string {
	return workStatusString[ws]
}

// CanBeRegistered check if work can be registered now
func (ws *WorkStatus) CanBeRegistered() bool {
	switch *ws {
	case WorkStatusInit:
		return true
	default:
		return false
	}
}

// CanBeUnregistered check if work can be unregistered now
func (ws *WorkStatus) CanBeUnregistered() bool {
	switch *ws {
	case
		WorkStatusRegistered,
		WorkStatusResourceApplying,
		WorkStatusResourceApplied,
		WorkStatusResourceApplyFailed,
		WorkStatusWorking,
		WorkStatusEnded:
		return true
	default:
		return false
	}
}

// CanBeSetSettings check if work can be set settings now
func (ws *WorkStatus) CanBeSetSettings() bool {
	switch *ws {
	case
		WorkStatusRegistered,
		WorkStatusResourceApplying,
		WorkStatusResourceApplied,
		WorkStatusResourceApplyFailed:
		return true
	default:
		return false
	}
}

// CanBeResourceApplying check if work can apply resource now
func (ws *WorkStatus) CanBeResourceApplying() bool {
	switch *ws {
	case WorkStatusRegistered:
		return true
	default:
		return false
	}
}

// CanBeResourceApplied check if work can be set resource applied now
func (ws *WorkStatus) CanBeResourceApplied() bool {
	switch *ws {
	case WorkStatusRegistered, WorkStatusResourceApplying:
		return true
	default:
		return false
	}
}

// CanBeResourceApplyFailed check if work can be set resource apply failed now
func (ws *WorkStatus) CanBeResourceApplyFailed() bool {
	switch *ws {
	case WorkStatusRegistered, WorkStatusResourceApplying:
		return true
	default:
		return false
	}
}

// CanBeStart check if work can be started now
func (ws *WorkStatus) CanBeStart() bool {
	switch *ws {
	case
		WorkStatusRegistered,
		WorkStatusResourceApplying,
		WorkStatusResourceApplied,
		WorkStatusResourceApplyFailed:
		return true
	default:
		return false
	}
}

// CanBeEnd check if work can be ended now
func (ws *WorkStatus) CanBeEnd() bool {
	switch *ws {
	case WorkStatusWorking:
		return true
	default:
		return false
	}
}

// CanBeRemoved check if work can be removed now
func (ws *WorkStatus) CanBeRemoved() bool {
	switch *ws {
	case WorkStatusRemovable:
		return true
	default:
		return false
	}
}

// CanBeHeartbeat check if work can be updated heartbeat from booster now
func (ws *WorkStatus) CanBeHeartbeat() bool {
	switch *ws {
	case
		WorkStatusRegistered,
		WorkStatusResourceApplying,
		WorkStatusResourceApplied,
		WorkStatusResourceApplyFailed,
		WorkStatusWorking,
		WorkStatusEnded:
		return true
	default:
		return false
	}
}

// IsUnregistered check if work is unregistered
func (ws *WorkStatus) IsUnregistered() bool {
	switch *ws {
	case WorkStatusUnregistered:
		return true
	default:
		return false
	}
}

// IsWorking check if work is working
func (ws *WorkStatus) IsWorking() bool {
	switch *ws {
	case WorkStatusWorking:
		return true
	default:
		return false
	}
}

// IsResourceApplyFailed check if work is under resource apply failed
func (ws *WorkStatus) IsResourceApplyFailed() bool {
	switch *ws {
	case WorkStatusResourceApplyFailed:
		return true
	default:
		return false
	}
}

// IsResourceApplied check if work is under resource applied
func (ws *WorkStatus) IsResourceApplied() bool {
	switch *ws {
	case WorkStatusResourceApplied:
		return true
	default:
		return false
	}
}

// JobUsage desc executor local job usage
type JobUsage string

// const for job usage
const (
	JobUsageRemoteExe JobUsage = "remote_exe"
	JobUsageLocalPre  JobUsage = "local_pre"
	JobUsageLocalExe  JobUsage = "local_exe"
	JobUsageLocalPost JobUsage = "local_post"
	JobUsageDefault   JobUsage = "default"
)

var AllUsageList = []JobUsage{JobUsageRemoteExe, JobUsageLocalPre, JobUsageLocalExe, JobUsageLocalPost, JobUsageDefault}

// String return the string of job usage
func (j JobUsage) String() string {
	return string(j)
}

// PreloadConfig describe the preload config
type PreloadConfig struct {
	Hooks []*HookConfig `json:"hooks"`
}

// String return the preload hooks string
func (pc PreloadConfig) String() string {
	return fmt.Sprintf("%v", pc.Hooks)
}

// GetContentRaw generate the hooks info as string
func (pc PreloadConfig) GetContentRaw() string {
	data := ""
	for _, v := range pc.Hooks {
		data += v.SrcCommand + "," + v.TargetCommand + "|"
	}

	return data
}

// HookConfig describe the single hook config in PreloadConfig
type HookConfig struct {
	SrcCommand    string `json:"src_command"`
	TargetCommand string `json:"target_command"`
}

// String return the string of hook config
func (hc HookConfig) String() string {
	return fmt.Sprintf("src_command: %s, target_command: %s", hc.SrcCommand, hc.TargetCommand)
}

// ----------------------------define send file status------------------------------------------

// ControllerFileSendStatus save file send status
type ControllerFileSendStatus int

// define file send status
const (
	FileSendInit ControllerFileSendStatus = iota
	FileSending
	FileSendSucceed
	FileSendFailed
	FileSendUnknown = 99
)

var (
	filestatusmap = map[ControllerFileSendStatus]string{
		FileSendInit:    "sendinit",
		FileSending:     "sending",
		FileSendSucceed: "sendsucceed",
		FileSendFailed:  "sendfailed",
		FileSendUnknown: "unknown",
	}
)

// String return the string if file send status
func (f ControllerFileSendStatus) String() string {
	if v, ok := filestatusmap[f]; ok {
		return v
	}

	return "unknown"
}

// ControllerFileInfo record file info
type ControllerFileInfo struct {
	Fullpath       string
	Size           int64
	Lastmodifytime int64
	Md5            string
	Sendstatus     ControllerFileSendStatus
}

// Equal compare two ControllerFileInfo and check if it is equal
func (f *ControllerFileInfo) Equal(other *ControllerFileInfo) bool {
	if other == nil {
		return false
	}

	return f.Fullpath == other.Fullpath && f.Size == other.Size && f.Lastmodifytime == other.Lastmodifytime
}

// ControllerFilesByServer record files info to send for one server
type ControllerFilesByServer struct {
	Server string
	Files  []*ControllerFileInfo
}

// ----------------------------define filter rules-------------------------------------------

// deinf vars
var (
	FilterErrorUnknownRuleType     = fmt.Errorf("unknown rule type")
	FilterErrorUnknownOperatorType = fmt.Errorf("unknown operator type")
	FilterErrorFileNotExisted      = fmt.Errorf("file not existed")
	FilterErrorStandardInvalid     = fmt.Errorf("input standard parameter is invalid")
)

// FilterRuleType define filter rule types
type FilterRuleType int

// const vars
const (
	FilterRuleFileSize FilterRuleType = iota
	FilterRuleFileSuffix
	FilterRuleFilePath
)

// FilterRuleOperator define filter rule operators
type FilterRuleOperator int

// const vars
const (
	FilterRuleOperatorEqual FilterRuleOperator = iota
	FilterRuleOperatorLess
	FilterRuleOperatorGreater
)

type FilterRules []FilterRuleItem

// Satisfy check a provided file-path, if it matches the filter rule
func (fr FilterRules) Satisfy(filePath string) (bool, FilterRuleHandle, error) {
	for _, r := range fr {
		ok, err := r.Satisfy(filePath)
		if err != nil {
			return false, FilterRuleHandleDefault, err
		}

		if ok {
			return true, r.HandleType, nil
		}
	}

	return false, FilterRuleHandleDefault, nil
}

type FilterRuleHandle int

const (
	FilterRuleHandleDefault FilterRuleHandle = iota
	FilterRuleHandleDeduplication
	FilterRuleHandleAllDistribution
)

// FilterRuleItem define rule item
type FilterRuleItem struct {
	Rule       FilterRuleType
	Operator   FilterRuleOperator
	Standard   interface{}
	HandleType FilterRuleHandle
}

// Satisfy check target satisfy rules
func (f *FilterRuleItem) Satisfy(filepath string) (bool, error) {
	switch f.Rule {
	case FilterRuleFileSize:
		return isFileSizeSatisfied(filepath, f.Operator, f.Standard)
	case FilterRuleFileSuffix:
		return isFileSuffixSatisfied(filepath, f.Operator, f.Standard)
	case FilterRuleFilePath:
		return isFilePathSatisfied(filepath, f.Operator, f.Standard)
	default:
		return false, FilterErrorUnknownRuleType
	}
}

func isFileSizeSatisfied(filepath string, operator FilterRuleOperator, standard interface{}) (bool, error) {
	targetsize, ok := standard.(int64)
	if !ok {
		return false, FilterErrorStandardInvalid
	}

	f := dcFile.Stat(filepath)
	existed, size := f.Exist(), f.Size()
	if !existed {
		return false, FilterErrorFileNotExisted
	}

	switch operator {
	case FilterRuleOperatorEqual:
		return targetsize == size, nil
	case FilterRuleOperatorLess:
		return targetsize < size, nil
	case FilterRuleOperatorGreater:
		return targetsize > size, nil
	default:
		return false, FilterErrorUnknownOperatorType
	}
}

func isFileSuffixSatisfied(filepath string, operator FilterRuleOperator, standard interface{}) (bool, error) {
	targetsuffix, ok := standard.(string)
	if !ok {
		return false, FilterErrorStandardInvalid
	}

	// ignore operator
	return strings.HasSuffix(filepath, targetsuffix), nil
}

func isFilePathSatisfied(filepath string, operator FilterRuleOperator, standard interface{}) (bool, error) {
	targetpath, ok := standard.(string)
	if !ok {
		return false, FilterErrorStandardInvalid
	}

	// ignore operator
	return filepath == targetpath, nil
}
