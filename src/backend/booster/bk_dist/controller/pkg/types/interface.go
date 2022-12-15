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
	"context"
	"time"

	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager/analyser"
	v2 "github.com/Tencent/bk-ci/src/booster/server/pkg/api/v2"
)

// Mgr describe a manager for handling all actions in controller
type Mgr interface {
	// brings up the handler
	Run()

	// register work with config, return the work information,
	// and tell the caller if he is a leader in batch mode(always true in normal mode)
	RegisterWork(config *WorkRegisterConfig) (*WorkInfo, bool, error)

	// unregister specific work
	UnregisterWork(workID string, config *WorkUnregisterConfig) error

	// update heartbeat to work
	Heartbeat(workID string) error

	// start the work
	StartWork(workID string) error

	// end the work
	EndWork(workID string) error

	// update work settings
	SetWorkSettings(workID string, settings *WorkSettings) error

	// get work settings
	GetWorkSettings(workID string) (*WorkSettings, error)

	// get work status
	GetWorkStatus(workID string) (*dcSDK.WorkStatusDetail, error)

	// lock a local slot
	LockLocalSlots(workID string, usage dcSDK.JobUsage, weight int32) error

	// unlock a local slot
	UnlockLocalSlots(workID string, usage dcSDK.JobUsage, weight int32) error

	// do task
	ExecuteLocalTask(workID string, req *LocalTaskExecuteRequest) (*LocalTaskExecuteResult, error)

	// do remote task directly
	ExecuteRemoteTask(workID string, req *RemoteTaskExecuteRequest) (*RemoteTaskExecuteResult, error)

	// send files to remote worker
	SendRemoteFile(workID string, req *RemoteTaskSendFileRequest) error

	// update single job stats
	UpdateJobStats(workID string, stats *dcSDK.ControllerJobStats) error

	// update work stats
	UpdateWorkStats(workID string, stats *WorkStats) error

	// get work list
	GetWorkDetailList() WorkStatsDetailList

	// get work detail
	GetWorkDetail(workID string, index int) (*WorkStatsDetail, error)

	// update common controller config
	SetCommonConfig(config *CommonConfig) error

	// get caches in pump mode
	GetPumpCache(workID string) (*analyser.FileCache, *analyser.RootCache, error)

	// Get first workid
	GetFirstWorkID() (string, error)
}

// RemoteMgr describe a manager for handling all actions with remote workers for work
type RemoteMgr interface {
	// init handler
	Init()

	Start()

	// run task in remote worker
	ExecuteTask(req *RemoteTaskExecuteRequest) (*RemoteTaskExecuteResult, error)

	// send files to remote worker
	SendFiles(req *RemoteTaskSendFileRequest) ([]string, error)

	// get total remote worker slots
	TotalSlots() int

	// inc remote jobs
	IncRemoteJobs()

	// dec remote jobs
	DecRemoteJobs()
}

// LocalMgr describe a manager for handling all actions with local execution for work
type LocalMgr interface {
	// init handler
	Init()

	Start()

	// lock local slot
	LockSlots(usage dcSDK.JobUsage, weight int32) bool

	// unlock local slot
	UnlockSlots(usage dcSDK.JobUsage, weight int32)

	// do task execution
	ExecuteTask(req *LocalTaskExecuteRequest,
		globalWork *Work,
		withlocalresource bool) (*LocalTaskExecuteResult, error)

	// get caches in pump mode
	GetPumpCache() (*analyser.FileCache, *analyser.RootCache)

	// get local slots info
	Slots() (int, int)
}

// CB4ResChanged call back function when remote resource changed
type CB4ResChanged func() error

// ResourceMgr describe a manager for handling all actions with resource and server for work
type ResourceMgr interface {
	// check if there are ready-to-work workers
	HasAvailableWorkers() bool

	// update tbs-server target
	SetServerHost(serverHost string)

	// update specific worker list
	SetSpecificHosts(hostList []string)

	// send stats, if brief true, then will not send the job stats
	SendStats(brief bool) error

	// send stats and reset after sent, if brief true, then will not send the job stats
	// !! this will call m.work.Lock() , to avoid dead lock
	SendAndResetStats(brief bool, resapplytimes []int64) error

	// get resource status
	GetStatus() *v2.RespTaskInfo

	// get worker list
	GetHosts() []*dcProtocol.Host

	// apply resource
	Apply(req *v2.ParamApply, force bool) (*v2.RespTaskInfo, error)

	// release resource
	Release(req *v2.ParamRelease) error

	// register call back function
	RegisterCallback(f CB4ResChanged) error

	// check whether apply finished
	IsApplyFinished() bool
}

// BasicMgr describe a manager for handling all actions with work basic issues
type BasicMgr interface {
	// if there a task running in work
	Alive() int64

	// call it when a task enter
	EnterTask()

	// call it when a task leave
	LeaveTask()

	// get work settings
	Settings() *WorkSettings

	// update work settings
	SetSettings(settings *WorkSettings)

	// get work info
	Info() *WorkInfo

	// update single job stats
	UpdateJobStats(stats *dcSDK.ControllerJobStats)

	// update work stats
	UpdateWorkStats(stats *WorkStats)

	// update work heartbeat
	Heartbeat() error

	// do register work
	Register(config *WorkRegisterConfig) error

	// do apply resource
	ApplyResource(config *WorkRegisterConfig) error

	// do unregister work
	Unregister(config *WorkUnregisterConfig) error

	// start work
	Start() error

	// end work
	End(timeoutBefore time.Duration) error

	// hang until the work is working(by someone else)
	WaitUntilWorking(ctx context.Context) bool

	// get work details
	GetDetails(jobIndex int) *WorkStatsDetail

	// get analysis status
	AnalysisStatus() *WorkAnalysisStatus

	// reset stat
	ResetStat() error

	// update toolchain
	SetToolChain(toolchain *ToolChain) error

	// get toolchain files by key
	GetToolChainFiles(key string) ([]dcSDK.FileDesc, int64, error)

	// get toolchain remote path by key
	GetToolChainRemotePath(key string) (string, error)

	// get toolchain timestamp by key
	GetToolChainTimestamp(key string) (int64, error)

	// add registered count for batch mode
	IncRegistered()

	// minus registered count for batch mode
	DecRegistered()
}
