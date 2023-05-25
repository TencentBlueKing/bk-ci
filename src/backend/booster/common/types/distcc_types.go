/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

import "time"

const (
	ClientHeartBeatTickTime = 5 * time.Second
)

// ServerInfo base server information
type ServerInfo struct {
	IP           string `json:"ip"`
	Port         uint   `json:"port"`
	MetricPort   uint   `json:"metric_port"`
	ResourcePort uint   `json:"resource_port"`
	HostName     string `json:"hostname"`
	Scheme       string `json:"scheme"` //http, https
	Version      string `json:"version"`
	Cluster      string `json:"cluster"`
	Pid          int    `json:"pid"`
}

// DistccServerSets define apply for distcc server information
type DistccServerSets struct {
	ProjectId     string `json:"project_id"`
	BuildId       string `json:"build_id"`
	ClientIp      string `json:"client_ip"`
	User          string `json:"user"`
	Message       string `json:"message"`
	ClientCPU     int    `json:"client_cpu"`
	ClientVersion string `json:"client_version"`
	GccVersion    string `json:"gcc_version"`
	City          string `json:"city"`
	RunDir        string `json:"run_dir"`
	Params        string `json:"params"` //自定义参数
	CCacheEnabled *bool  `json:"ccache_enabled"`

	// command define the target to be called, such as make, bazel, /data/custom/make etc.
	Command     string      `json:"command,omitempty"`
	CommandType CommandType `json:"command_type,omitempty"`

	// extra_vars includes the extra params need by client
	ExtraVars ExtraVars `json:"extra_vars,omitempty"`
}

type CommandType string

const (
	CommandMake   CommandType = "make"
	CommandCmake  CommandType = "cmake"
	CommandBazel  CommandType = "bazel"
	CommandBlade  CommandType = "blade"
	CommandNinja  CommandType = "ninja"
	CommandFBuild CommandType = "FBuild.exe"
)

// ExtraVars describe the extra settings in DistccServerSets
type ExtraVars struct {
	// bazelrc define the bazelrc file path
	BazelRC string `json:"bazelrc"`
	MaxJobs int    `json:"max_jobs,omitempty"`
}

// distcc server info
type DistccServerInfo struct {
	TaskID        string            `json:"task_id"`
	Hosts         string            `json:"hosts"`
	Cmds          string            `json:"cmds"`
	GccVersion    string            `json:"gcc_version"`
	Envs          map[string]string `json:"envs"`
	UnsetEnvs     []string          `json:"unset_envs"`
	Status        ServerStatusType  `json:"status"`
	Message       string            `json:"message"`        //if status=StatusFailed, them it is failed message
	QueueNumber   int               `json:"queue_number"`   //current queue number of the task
	CCacheEnabled bool              `json:"ccache_enabled"` //whether open ccache

	// compiler settings
	CCCompiler  string `json:"cc_compiler"`
	CXXCompiler string `json:"cxx_compiler"`
	JobServer   uint   `json:"job_server"`
	DistccHosts string `json:"distcc_hosts"`
}

type ServerStatusType string

const (
	ServerStatusStaging  ServerStatusType = "staging"
	ServerStatusStarting ServerStatusType = "starting"
	ServerStatusRunning  ServerStatusType = "running"
	ServerStatusFailed   ServerStatusType = "failed"
	ServerStatusFinish   ServerStatusType = "finish"
)

var AllServerStatusList = []ServerStatusType{
	ServerStatusStaging,
	ServerStatusStarting,
	ServerStatusRunning,
	ServerStatusFailed,
	ServerStatusFinish,
}

const (
	ClientExtraKeyCompileResult string = "compileresult"
	ClientExtraKeyCmd           string = "cmd"
	ClientExtraKeyRunDir        string = "run_dir"
	ClientExtraKeyStartTime     string = "start_time"
	ClientExtraKeyEndTime       string = "end_time"
)

// DistccClientInfo define distcc client information
type DistccClientInfo struct {
	TaskID  string            `json:"task_id"`
	Status  ClientStatusType  `json:"status"` //only StatusFailed & StatusFinish
	Ccache  *Ccache           `json:"ccache"`
	Message string            `json:"message"` //if status=StatusFailed, them it is failed message
	Extra   map[string]string `json:"envs"`

	// fb summary
	FbSummary FbSummaryInfo
}

// FbSummaryInfo describe the summary information in fastbuild
type FbSummaryInfo struct {
	LibraryBuilt    int     `json:"library_built"`
	LibraryCacheHit int     `json:"library_cache_hit"`
	LibraryCPUTime  float32 `json:"library_cpu_time"`

	ObjectBuilt    int     `json:"object_built"`
	ObjectCacheHit int     `json:"object_cache_hit"`
	ObjectCPUTime  float32 `json:"object_cpu_time"`

	ExeBuilt    int     `json:"exe_built"`
	ExeCacheHit int     `json:"exe_cache_hit"`
	ExeCPUTime  float32 `json:"exe_cpu_time"`

	CacheHits   int `json:"cache_hits"`
	CacheMisses int `json:"cache_misses"`
	CacheStores int `json:"cache_stores"`

	RealCompileTime   float32 `json:"real_compile_time"`
	LocalCompileTime  float32 `json:"local_compile_time"`
	RemoteCompileTime float32 `json:"remote_compile_time"`
}

type ClientStatusType string

const (
	ClientStatusSuccess ClientStatusType = "success"
	ClientStatusFailed  ClientStatusType = "failed"
)

// CMakeArgs describe the args of cmake command return from server
type CMakeArgs struct {
	Args string `json:"args"`
}

// Ccache describe the ccache stats info
type Ccache struct {
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

//client & server heartbeat mechanism
type HeartBeat struct {
	TaskID string        `json:"task_id"`
	Type   HeartBeatType `json:"type"`
}

type HeartBeatType string

const (
	HeartBeatPing HeartBeatType = "ping"
	HeartBeatPong HeartBeatType = "pong"
)

type ServerErrCode int

const (
	ServerErrOK ServerErrCode = iota
	ServerErrInvalidParam
	ServerErrRequestResourceFailed
	ServerErrRequestTaskInfoFailed
	ServerErrUpdateHeartbeatFailed
	ServerErrReleaseResourceFailed
	ServerErrPreProcessFailed
	ServerErrRedirectFailed
	ServerErrEncodeJSONFailed
	ServerErrListTaskFailed
	ServerErrListProjectFailed
	ServerErrListWhiteListFailed
	ServerErrListGccFailed
	ServerErrUpdateProjectFailed
	ServerErrUpdateWhiteListFailed
	ServerErrUpdateGccFailed
	ServerErrDeleteProjectFailed
	ServerErrDeleteWhiteListFailed
	ServerErrDeleteGccFailed
	ServerErrOperatorNoSpecific
	ServerErrGetServersFailed
	ServerErrGetCMakeArgsFailed
	ServerErrServerInternalError
	ServerErrReportResourceError
	ServerErrListWorkerFailed
	ServerErrUpdateWorkerFailed
	ServerErrDeleteWorkerFailed
	ServerErrListWorkStatsFailed
	ServerErrListVersionFailed
)

var serverErrCode = map[ServerErrCode]string{
	ServerErrOK:                    "request OK",
	ServerErrInvalidParam:          "invalid param",
	ServerErrRequestResourceFailed: "request compile resource failed",
	ServerErrRequestTaskInfoFailed: "request task info failed",
	ServerErrUpdateHeartbeatFailed: "update heartbeat failed",
	ServerErrReleaseResourceFailed: "release compile resource failed",
	ServerErrPreProcessFailed:      "pre process failed",
	ServerErrRedirectFailed:        "redirect failed",
	ServerErrEncodeJSONFailed:      "encode json failed",
	ServerErrListTaskFailed:        "list task failed",
	ServerErrListProjectFailed:     "list project failed",
	ServerErrListWhiteListFailed:   "list whitelist failed",
	ServerErrListGccFailed:         "list gcc failed",
	ServerErrUpdateProjectFailed:   "update project failed",
	ServerErrUpdateWhiteListFailed: "update whitelist failed",
	ServerErrUpdateGccFailed:       "update gcc failed",
	ServerErrDeleteProjectFailed:   "delete project failed",
	ServerErrDeleteWhiteListFailed: "delete whitelist failed",
	ServerErrDeleteGccFailed:       "delete gcc failed",
	ServerErrOperatorNoSpecific:    "operator not specific",
	ServerErrGetServersFailed:      "get servers failed",
	ServerErrGetCMakeArgsFailed:    "get cmake args failed",
	ServerErrServerInternalError:   "server internal error",
	ServerErrReportResourceError:   "report resource error",
	ServerErrListWorkerFailed:      "list worker failed",
	ServerErrUpdateWorkerFailed:    "update worker failed",
	ServerErrDeleteWorkerFailed:    "delete worker failed",
	ServerErrListWorkStatsFailed:   "list work stats failed",
}

// String return the string of ServerErrCode
func (sec ServerErrCode) String() string {
	return serverErrCode[sec]
}

// APIResponse define the api response body data.
type APIResponse struct {
	Code    int         `json:"code"`
	Message string      `json:"message"`
	Data    interface{} `json:"data"`
}
