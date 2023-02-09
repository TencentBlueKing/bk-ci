/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package fastbuild

import (
	"fmt"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
)

const (
	workerListIPSep = ";"
)

// TaskExtra define extra data for fast build
type TaskExtra struct {
	Params       string
	FullCmd      string
	Env          string
	RunDir       string
	CommandType  string
	Command      string
	User         string
	Version      string
	Path         string
	ClientIP     string
	CacheEnabled string
}

// RemoteResource 记录拉起的远端资源信息，userDefineID 保证唯一对应关系
type RemoteResource struct {
	UserDefineID string `json:"user_define_id"`
	RemoteIP     string `json:"remote_ip"`
	RemotePort   uint32 `json:"remote_port"`
}

type fastbuildTask struct {
	TaskID string

	ResourceID   string
	RequestCPU   float64
	LeastCPU     float64
	CPUTotal     float64
	MemTotal     float64
	WorkerIPList []string

	CacheEnabled     bool
	FBResultCompress bool
	Attr             uint32

	TaskExtra

	AgentMinPort            uint32
	AgentMaxPort            uint32
	AgentRemoteExe          string
	AgentPath               string
	AgentWorkerConsole      bool
	AgentWorkerMode         string
	AgentWorkerNosubprocess bool
	Agent4OneTask           bool
	AgentWorkerCPU          string

	RemoteResources []RemoteResource

	CompileResult string
	FbSummary
}

func newTask(id string) (*fastbuildTask, error) {
	return &fastbuildTask{
		TaskID: id,
	}, nil
}

// once it is launched, always has available resources.
func (t *fastbuildTask) EnoughAvailableResource() bool {
	return true
}

// get the worker list.
func (t *fastbuildTask) WorkerList() []string {
	return t.WorkerIPList
}

//GetRequestInstance define
func (t *fastbuildTask) GetRequestInstance() int {
	return 0
}

// GetWorkerCount get the worker count.
func (t *fastbuildTask) GetWorkerCount() int {
	return len(t.WorkerIPList)
}

// do not support dump.
func (t *fastbuildTask) Dump() []byte {
	return []byte("")
}

// get custom data from task.
func (t *fastbuildTask) CustomData(param interface{}) interface{} {
	customData := map[string]string{}
	if t.FBResultCompress {
		customData[FBCompressResultEnvKey] = "true"
	} else {
		customData[FBCompressResultEnvKey] = "false"
	}

	if t.CacheEnabled {
		customData[FBCacheEnableKey] = "true"
	} else {
		customData[FBCacheEnableKey] = "false"
	}

	return customData
}

func (t *fastbuildTask) joinWorkerIPList() string {
	if len(t.WorkerIPList) == 0 {
		return ""
	}
	return strings.Join(t.WorkerIPList, workerListIPSep)
}

func (t *fastbuildTask) getCommandParameters(clientip string, remotePort uint32) []string {

	args := []string{}
	// 端口先简单按独占的方式，即取最小端口
	args = append(args, fmt.Sprintf("-port=%d", remotePort))

	if t.Agent4OneTask {
		args = append(args, "-cpus=-1")
	} else {
		args = append(args, fmt.Sprintf("-cpus=%s", t.AgentWorkerCPU))
	}

	if t.AgentWorkerConsole {
		args = append(args, "-console")
	}

	if t.AgentWorkerMode != "" {
		args = append(args, fmt.Sprintf("-mode=%s", t.AgentWorkerMode))
	} else {
		args = append(args, "-mode=idle")
	}

	if t.AgentWorkerNosubprocess {
		args = append(args, "-nosubprocess")
	}

	if clientip != "" {
		tokens := strings.Split(clientip, "|")
		ips := []string{}
		for _, t := range tokens {
			if strings.Count(t, ".") == 3 {
				ips = append(ips, t)
			}
		}
		args = append(args, fmt.Sprintf("-allowips=%s", strings.Join(ips, ";")))
	}

	return args
}

func splitWorkerIPList(s string) []string {
	if s == "" {
		return make([]string, 0, 100)
	}

	return strings.Split(s, workerListIPSep)
}

func tableTask2task(taskTable *TableTask) *fastbuildTask {
	var RemoteResources []RemoteResource
	if err := codec.DecJSON([]byte(taskTable.RemoteResource), &RemoteResources); err != nil {
		blog.Errorf("engine(%s) failed to decode for [%v] with data[%s]",
			EngineName, err, taskTable.RemoteResource)
	}

	return &fastbuildTask{
		TaskID:       taskTable.TaskID,
		ResourceID:   taskTable.ResourceID,
		RequestCPU:   taskTable.RequestCPU,
		LeastCPU:     taskTable.LeastCPU,
		CPUTotal:     taskTable.CPUTotal,
		MemTotal:     taskTable.MemTotal,
		WorkerIPList: splitWorkerIPList(taskTable.WorkerIPList),

		CacheEnabled:     taskTable.CacheEnabled,
		FBResultCompress: taskTable.FBResultCompress,
		Attr:             taskTable.Attr,

		TaskExtra: TaskExtra{
			Params:      taskTable.Params,
			FullCmd:     taskTable.FullCmd,
			Env:         taskTable.Env,
			RunDir:      taskTable.RunDir,
			CommandType: taskTable.CommandType,
			Command:     taskTable.Command,
			User:        taskTable.User,
			Version:     taskTable.ClientVersion,
			ClientIP:    taskTable.ClientIP,
		},

		AgentMinPort:            taskTable.AgentMinPort,
		AgentMaxPort:            taskTable.AgentMaxPort,
		AgentRemoteExe:          taskTable.AgentRemoteExe,
		AgentPath:               taskTable.AgentPath,
		AgentWorkerConsole:      taskTable.AgentWorkerConsole,
		AgentWorkerMode:         taskTable.AgentWorkerMode,
		AgentWorkerNosubprocess: taskTable.AgentWorkerNosubprocess,
		Agent4OneTask:           taskTable.Agent4OneTask,
		AgentWorkerCPU:          taskTable.AgentWorkerCPU,

		RemoteResources: RemoteResources,

		CompileResult: taskTable.CompileResult,
		FbSummary:     taskTable.FbSummary,
	}
}

func task2tableTask(task *fastbuildTask) *TableTask {
	var RemoteResource []byte
	err := codec.EncJSON(task.RemoteResources, &RemoteResource)
	if err != nil {
		blog.Errorf("engine(%s) failed to encode for [%v] with data[%+v]", EngineName, err, task.RemoteResources)
	}

	return &TableTask{
		ResourceID:   task.ResourceID,
		RequestCPU:   task.RequestCPU,
		LeastCPU:     task.LeastCPU,
		CPUTotal:     task.CPUTotal,
		MemTotal:     task.MemTotal,
		WorkerIPList: task.joinWorkerIPList(),

		CacheEnabled:     task.CacheEnabled,
		FBResultCompress: task.FBResultCompress,
		Attr:             task.Attr,

		Params:      task.Params,
		FullCmd:     task.FullCmd,
		Env:         task.Env,
		RunDir:      task.RunDir,
		CommandType: task.CommandType,
		Command:     task.Command,
		User:        task.User,

		AgentMinPort:            task.AgentMinPort,
		AgentMaxPort:            task.AgentMaxPort,
		AgentRemoteExe:          task.AgentRemoteExe,
		AgentPath:               task.AgentPath,
		AgentWorkerConsole:      task.AgentWorkerConsole,
		AgentWorkerMode:         task.AgentWorkerMode,
		AgentWorkerNosubprocess: task.AgentWorkerNosubprocess,
		Agent4OneTask:           task.Agent4OneTask,
		AgentWorkerCPU:          task.AgentWorkerCPU,

		RemoteResource: string(RemoteResource),

		CompileResult: task.CompileResult,
		FbSummary:     task.FbSummary,
	}
}
