/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package apisjob

import (
	"fmt"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
)

const (
	workerListIPSep = ","
)

type apisTask struct {
	TaskID     string
	ResourceID string
	RequestCPU float64
	LeastCPU   float64
	CPUTotal   float64
	MemTotal   float64

	AgentProjectID string
	WorkerIPList   []string
	Command        string
	ExtraParam     string

	CoordinatorIP            string
	CoordinatorPort          int
	AgentCommandName         string
	AgentCommandPath         string
	AgentCommandDir          string
	AgentPort                int
	AgentFileServerPort      int
	AgentNTriesConnectingRPC int
	AgentCachePath           string
	CacheIP                  string
	CachePort                int
	UseGdt                   bool
	VolumeMounts             map[string]string

	// following fields all deprecated
	AgentMaxTasksPerJob           int
	AgentMaxTasks                 int
	AgentKeepAliveInterval        int
	AgentDependPreparationTimeout int
	AgentClientKeepAliveTimeout   int
	AgentFileRetrievingRetryTimes int
	AgentFileRequestTimeout       int
	AgentMaxWorkingProcesses      int
	AgentMaxWorkingCapability     int
	AgentMaxCores                 int
	AgentMaxFileRetrievingCount   int
	AgentNoCoordinator            bool
	AgentFilePackageSize          int64
	AgentChunkRetryInterval       int
	AgentMaxTransmissionPeers     int
	AgentMaxActiveTransmission    int
	AgentEnableFileCompress       bool
	AgentPrepareTimeout           int
	AgentEnableFileLog            bool

	CompleteTasks int
	FailedTasks   int
	AgentsInfo    string

	// container setting
	ContainerSetting taskContainerSetting
}

type taskContainerSetting struct {
	QueueName       string
	Image           string
	CPUPerInstance  float64
	MemPerInstance  float64
	Instance        int
	LeastInstance   int
	RequestInstance int
}

// EnoughAvailableResource once it is launched, always has available resources.
func (t *apisTask) EnoughAvailableResource() bool {
	return len(t.WorkerList()) > 0
}

// WorkerList get the worker list.
func (t *apisTask) WorkerList() []string {
	return t.WorkerIPList
}

// GetRequestInstance define
func (t *apisTask) GetRequestInstance() int {
	return t.ContainerSetting.RequestInstance
}

// GetWorkerCount get the worker count.
func (t *apisTask) GetWorkerCount() int {
	return len(t.WorkerIPList)
}

// Dump get dump data from task.
func (t *apisTask) Dump() []byte {
	var data []byte
	_ = codec.EncJSON(&ExtraInfo{
		MaxCores: t.AgentMaxCores,
	}, &data)

	return data
}

// ExtraInfo describe the data dump from task.
type ExtraInfo struct {
	MaxCores int `json:"max_cores"`
}

// CustomData do not support custom data.
func (t *apisTask) CustomData(param interface{}) interface{} {
	return nil
}

func (t *apisTask) joinWorkerIPList() string {
	if len(t.WorkerIPList) == 0 {
		return ""
	}
	return strings.Join(t.WorkerIPList, workerListIPSep)
}

// apis agent command options:
// -FileAgentAddress 127.0.0.1:12000                  // the address of FileAgent server.
// -FileAgentFTPUrl ftp://127.0.0.1/                  // the ftp address
// -nTriesConnectingRPC 3                             // the max retry times of RPC connection.
// -CoordinatorUri 127.0.0.1:12008                    // the coordinator server address.
// -LaunchTag untagged                                // the launch tag, specific with agent project id.
// -ServiceUri 127.0.0.1:12009                        // the agent server bind address.
// -LocalFileCachePath E:\cache\                      // the cache dir, same as file agent cache dir
// -FTPUsername Apis                                  // ftp username
// -FileAgentGDTConfigString {"UseGdt":false ...} // the GDT config
// TODO: api direct has been deprecated
func (t *apisTask) getCommandParameters(ip string, resourceCPU int, useGdt bool) []string {
	return generateFlagsPair([]flagsPair{
		{
			key:   "CoordinatorUri",
			value: fmt.Sprintf("%s:%d", t.CoordinatorIP, t.CoordinatorPort),
		},
		{
			key:   "ServiceHost",
			value: ip,
		},
		{
			key:   "LocalFileCachePath",
			value: fmt.Sprintf("\"%s\"", t.AgentCachePath),
		},
		{
			key:   "FileAgentServicePort",
			value: fmt.Sprintf("%d", t.AgentFileServerPort),
		},
		{
			key:   "AgentServicePort",
			value: fmt.Sprintf("%d", t.AgentPort),
		},
		{
			key:   "AvailableCores",
			value: fmt.Sprintf("%d", resourceCPU),
		},
		{
			key:   "GdtServicePort",
			value: "10241",
		},
		{
			key:   "UseGdt",
			value: fmt.Sprintf("%t", t.UseGdt),
		},
	})
}

type flagsPair struct {
	key   string
	value string
}

func generateFlagsPair(l []flagsPair) []string {
	r := make([]string, 0, 10)
	for _, item := range l {
		r = append(r, fmt.Sprintf("-%s", item.key))
		r = append(r, item.value)
	}
	return r
}

func splitWorkerIPList(s string) []string {
	if s == "" {
		return make([]string, 0, 100)
	}

	return strings.Split(s, workerListIPSep)
}

func tableTask2task(taskTable *TableTask) *apisTask {
	containerSetting := taskContainerSetting{
		QueueName:       taskTable.QueueName,
		Image:           taskTable.Image,
		CPUPerInstance:  taskTable.CPUPerInstance,
		MemPerInstance:  taskTable.MemPerInstance,
		Instance:        taskTable.Instance,
		LeastInstance:   taskTable.LeastInstance,
		RequestInstance: taskTable.RequestInstance,
	}

	return &apisTask{
		TaskID:     taskTable.TaskID,
		ResourceID: taskTable.ResourceID,
		RequestCPU: taskTable.RequestCPU,
		LeastCPU:   taskTable.LeastCPU,
		CPUTotal:   taskTable.CPUTotal,
		MemTotal:   taskTable.MemTotal,

		AgentProjectID: taskTable.AgentProjectID,
		WorkerIPList:   splitWorkerIPList(taskTable.WorkerIPList),

		CoordinatorIP:      taskTable.CoordinatorIP,
		CoordinatorPort:    taskTable.CoordinatorPort,
		AgentNoCoordinator: taskTable.AgentNoCoordinator,

		CacheIP:                  taskTable.CacheIP,
		CachePort:                taskTable.CachePort,
		AgentCommandName:         taskTable.AgentCommandName,
		AgentCommandPath:         taskTable.AgentCommandPath,
		AgentCommandDir:          taskTable.AgentCommandDir,
		AgentPort:                taskTable.AgentPort,
		AgentFileServerPort:      taskTable.AgentFileServerPort,
		AgentNTriesConnectingRPC: taskTable.AgentNTriesConnectingRPC,
		AgentCachePath:           taskTable.AgentCachePath,
		UseGdt:                   taskTable.UseGdt,
		VolumeMounts:             volumeMounts2Map(taskTable.VolumeMounts),

		CompleteTasks: taskTable.CompleteTasks,
		FailedTasks:   taskTable.FailedTasks,
		AgentsInfo:    taskTable.AgentsInfo,

		ContainerSetting: containerSetting,
	}
}

func task2tableTask(task *apisTask) *TableTask {
	return &TableTask{
		ResourceID: task.ResourceID,
		RequestCPU: task.RequestCPU,
		LeastCPU:   task.LeastCPU,
		CPUTotal:   task.CPUTotal,
		MemTotal:   task.MemTotal,

		AgentProjectID: task.AgentProjectID,
		WorkerIPList:   task.joinWorkerIPList(),

		Image:           task.ContainerSetting.Image,
		CPUPerInstance:  task.ContainerSetting.CPUPerInstance,
		MemPerInstance:  task.ContainerSetting.MemPerInstance,
		Instance:        task.ContainerSetting.Instance,
		LeastInstance:   task.ContainerSetting.LeastInstance,
		RequestInstance: task.ContainerSetting.RequestInstance,

		CoordinatorIP:            task.CoordinatorIP,
		CoordinatorPort:          task.CoordinatorPort,
		AgentNoCoordinator:       task.AgentNoCoordinator,
		CacheIP:                  task.CacheIP,
		CachePort:                task.CachePort,
		AgentCommandName:         task.AgentCommandName,
		AgentCommandPath:         task.AgentCommandPath,
		AgentCommandDir:          task.AgentCommandDir,
		AgentPort:                task.AgentPort,
		AgentFileServerPort:      task.AgentFileServerPort,
		AgentNTriesConnectingRPC: task.AgentNTriesConnectingRPC,
		AgentCachePath:           task.AgentCachePath,
		UseGdt:                   task.UseGdt,
		VolumeMounts:             map2VolumeMounts(task.VolumeMounts),

		CompleteTasks: task.CompleteTasks,
		FailedTasks:   task.FailedTasks,
		AgentsInfo:    task.AgentsInfo,
	}
}

func volumeMounts2Map(raw string) map[string]string {
	var s map[string]string

	_ = codec.DecJSON([]byte(raw), &s)
	return s
}

func map2VolumeMounts(s map[string]string) string {
	var raw []byte

	_ = codec.EncJSON(s, &raw)
	return string(raw)
}
