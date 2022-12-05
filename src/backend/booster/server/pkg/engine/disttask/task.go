/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package disttask

import (
	"fmt"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
)

type distTask struct {
	// Unique ID for an independent task
	ID string

	// The data exchange with client
	Client taskClient

	// The allocated compiling endpoint for this task
	Workers []taskWorker

	// The task status
	Stats taskStats

	// The data concerned by operator
	Operator taskOperator

	// inherit setting
	InheritSetting taskInheritSetting
}

// EnoughAvailableResource check if available cpu num is grater than least cpu.
// if not, means there are no enough available resource.
func (dt *distTask) EnoughAvailableResource() bool {
	return dt.Stats.CPUTotal >= dt.InheritSetting.LeastCPU
}

// WorkerList return worker list
func (dt *distTask) WorkerList() []string {
	workers := make([]string, 0)
	for _, v := range dt.Workers {
		// set overload with cpu number*2 or limit*2
		jobs := v.CPU
		if dt.Operator.RequestProcessPerUnit > 0 {
			jobs = float64(dt.Operator.RequestProcessPerUnit)
		}
		workers = append(workers, fmt.Sprintf("%s:%d/%d", v.IP, v.Port, int(jobs*1.1)))
	}

	return workers
}

func (dt *distTask) GetRequestInstance() int {
	return dt.Operator.RequestInstance
}

// GetWorkerCount get the worker count.
func (dt *distTask) GetWorkerCount() int {
	return dt.Stats.WorkerCount
}

// Dump do not support dump.
func (dt *distTask) Dump() []byte {
	return dt.CustomData(nil).([]byte)
}

// CustomData get task custom data.
func (dt *distTask) CustomData(params interface{}) interface{} {
	jobServer := int(dt.Stats.CPUTotal)

	userspecifiedjobserver := 0
	var extra taskClientExtra
	err := codec.DecJSON([]byte(dt.Client.ExtraClientSetting), &extra)
	if err == nil {
		userspecifiedjobserver = extra.MaxJobs
	}

	if 0 < userspecifiedjobserver && userspecifiedjobserver < jobServer {
		jobServer = userspecifiedjobserver
	}

	env := dt.Client.Env
	if env == nil {
		env = map[string]string{}
	}
	unSetEnv := make([]string, 0, 100)

	dataStruct := CustomData{
		WorkerVersion:     dt.Client.WorkerVersion,
		Environments:      env,
		UnsetEnvironments: unSetEnv,
		JobServer:         jobServer,
		ExtraWorkerData:   dt.InheritSetting.ExtraWorkerSetting,
		ExtraProjectData:  dt.InheritSetting.ExtraProjectSetting,
	}

	var data []byte
	_ = codec.EncJSON(dataStruct, &data)

	return data
}

// CustomData describe the detail data of dist task.
type CustomData struct {
	WorkerVersion     string            `json:"worker_version"`
	Environments      map[string]string `json:"environments"`
	UnsetEnvironments []string          `json:"unset_environments"`
	JobServer         int               `json:"job_server"`
	ExtraWorkerData   string            `json:"extra_worker_data"`
	ExtraProjectData  string            `json:"extra_project_data"`
}

type taskClient struct {
	SourceIP           string
	SourceCPU          int
	WorkerVersion      string
	User               string
	Params             string
	Cmd                string
	Env                map[string]string
	RunDir             string
	BoosterType        string
	ExtraClientSetting string
}

type taskClientExtra struct {
	MaxJobs int `json:"max_jobs"`
}

type taskStats struct {
	WorkerCount int
	CPUTotal    float64
	MemTotal    float64
	SucceedNum  int64
	FailedNum   int64
	StatDetail  string
	ExtraRecord string
}

type taskOperator struct {
	ClusterID string
	AppName   string
	Namespace string
	Image     string

	Instance              int
	RequestInstance       int
	LeastInstance         int
	RequestCPUPerUnit     float64
	RequestMemPerUnit     float64
	RequestProcessPerUnit int
}

type taskWorker struct {
	CPU       float64
	Mem       float64
	IP        string
	Port      int
	StatsPort int
	Message   string
}

type taskInheritSetting struct {
	QueueName           string
	RequestCPU          float64
	LeastCPU            float64
	WorkerVersion       string
	Scene               string
	BanAllBooster       bool
	ExtraProjectSetting string
	ExtraWorkerSetting  string
}

type taskMountsSettings struct {
	Mounts []taskMountsItem `json:"mounts"`
}

type taskMountsItem struct {
	HostDir      string `json:"host_dir"`
	ContainerDir string `json:"container_dir"`
}

func table2Task(tableTask *TableTask) *distTask {
	// task client
	var env map[string]string
	_ = codec.DecJSON([]byte(tableTask.Env), &env)

	client := taskClient{
		SourceIP:           tableTask.SourceIP,
		SourceCPU:          tableTask.SourceCPU,
		User:               tableTask.User,
		Params:             tableTask.Params,
		Cmd:                tableTask.Cmd,
		Env:                env,
		RunDir:             tableTask.RunDir,
		BoosterType:        tableTask.BoosterType,
		ExtraClientSetting: tableTask.ExtraClientSetting,
	}

	// task Workers
	var Workers []taskWorker
	_ = codec.DecJSON([]byte(tableTask.Workers), &Workers)

	// task stats
	stats := taskStats{
		WorkerCount: tableTask.WorkerCount,
		CPUTotal:    tableTask.CPUTotal,
		MemTotal:    tableTask.MemTotal,
		SucceedNum:  tableTask.SucceedNum,
		FailedNum:   tableTask.FailedNum,
		StatDetail:  tableTask.StatDetail,
		ExtraRecord: tableTask.ExtraRecord,
	}

	// task operator
	operator := taskOperator{
		ClusterID:             tableTask.ClusterID,
		AppName:               tableTask.AppName,
		Namespace:             tableTask.Namespace,
		Image:                 tableTask.Image,
		Instance:              tableTask.Instance,
		RequestInstance:       tableTask.RequestInstance,
		LeastInstance:         tableTask.LeastInstance,
		RequestCPUPerUnit:     tableTask.RequestCPUPerUnit,
		RequestMemPerUnit:     tableTask.RequestMemPerUnit,
		RequestProcessPerUnit: tableTask.RequestProcessPerUnit,
	}

	inheritSetting := taskInheritSetting{
		QueueName:           tableTask.QueueName,
		RequestCPU:          tableTask.RequestCPU,
		LeastCPU:            tableTask.LeastCPU,
		WorkerVersion:       tableTask.WorkerVersion,
		Scene:               tableTask.Scene,
		BanAllBooster:       tableTask.BanAllBooster,
		ExtraProjectSetting: tableTask.ExtraProjectSetting,
		ExtraWorkerSetting:  tableTask.ExtraWorkerSetting,
	}

	return &distTask{
		ID:             tableTask.TaskID,
		Client:         client,
		Workers:        Workers,
		Stats:          stats,
		Operator:       operator,
		InheritSetting: inheritSetting,
	}
}

func task2Table(task *distTask) *TableTask {
	var env []byte
	_ = codec.EncJSON(task.Client.Env, &env)

	var workers []byte
	_ = codec.EncJSON(task.Workers, &workers)

	return &TableTask{
		// task client
		SourceIP:           task.Client.SourceIP,
		SourceCPU:          task.Client.SourceCPU,
		User:               task.Client.User,
		Params:             task.Client.Params,
		Cmd:                task.Client.Cmd,
		Env:                string(env),
		RunDir:             task.Client.RunDir,
		BoosterType:        task.Client.BoosterType,
		ExtraClientSetting: task.Client.ExtraClientSetting,

		// task compilers
		Workers: string(workers),

		// task stats
		WorkerCount: task.Stats.WorkerCount,
		CPUTotal:    task.Stats.CPUTotal,
		MemTotal:    task.Stats.MemTotal,
		SucceedNum:  task.Stats.SucceedNum,
		FailedNum:   task.Stats.FailedNum,
		StatDetail:  task.Stats.StatDetail,
		ExtraRecord: task.Stats.ExtraRecord,

		// resource manager
		ClusterID:             task.Operator.ClusterID,
		AppName:               task.Operator.AppName,
		Namespace:             task.Operator.Namespace,
		Image:                 task.Operator.Image,
		Instance:              task.Operator.Instance,
		LeastInstance:         task.Operator.LeastInstance,
		RequestInstance:       task.Operator.RequestInstance,
		RequestCPUPerUnit:     task.Operator.RequestCPUPerUnit,
		RequestMemPerUnit:     task.Operator.RequestMemPerUnit,
		RequestProcessPerUnit: task.Operator.RequestProcessPerUnit,

		// inherit setting
		RequestCPU:          task.InheritSetting.RequestCPU,
		LeastCPU:            task.InheritSetting.LeastCPU,
		WorkerVersion:       task.InheritSetting.WorkerVersion,
		Scene:               task.InheritSetting.Scene,
		BanAllBooster:       task.InheritSetting.BanAllBooster,
		ExtraProjectSetting: task.InheritSetting.ExtraProjectSetting,
		ExtraWorkerSetting:  task.InheritSetting.ExtraWorkerSetting,
	}
}

// MessageRecordStats describe the message data of type record stats.
type MessageRecordStats struct {
	Message     string      `json:"message"`
	CCacheStats CCacheStats `json:"ccache_stats"`
}

// dump the struct data into byte
func (mrd MessageRecordStats) Dump() []byte {
	var data []byte
	_ = codec.EncJSON(mrd, &data)
	return data
}

// CCacheStats describe the ccache stats data from 'ccache -s'.
type CCacheStats struct {
	CacheDir                  string `json:"cache_dir"`
	PrimaryConfig             string `json:"primary_config"`
	SecondaryConfig           string `json:"secondary_config"`
	DirectHit                 int    `json:"cache_direct_hit"`
	PreprocessedHit           int    `json:"cache_preprocessed_hit"`
	CacheMiss                 int    `json:"cache_miss"`
	CalledForLink             int    `json:"called_for_link"`
	CalledForPreProcessing    int    `json:"called_for_processing"`
	UnsupportedSourceLanguage int    `json:"unsupported_source_language"`
	NoInputFile               int    `json:"no_input_file"`
	FilesInCache              int    `json:"files_in_cache"`
	CacheSize                 string `json:"cache_size"`
	MaxCacheSize              string `json:"max_cache_size"`
}

// dump the struct data into byte
func (cs CCacheStats) Dump() []byte {
	var data []byte
	_ = codec.EncJSON(cs, &data)
	return data
}
