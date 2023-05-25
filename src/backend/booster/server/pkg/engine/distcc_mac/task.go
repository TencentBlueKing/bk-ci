/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package distccmac

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
	ds "github.com/Tencent/bk-ci/src/booster/common/store/distcc_server"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc"
)

const (
	envDistCCHostKey    = "DISTCC_HOSTS"
	envDistCCHostValue  = "%s:%d/%d,lzo"
	envDistCCHostPrefix = "--randomize"
	envDistCCHostSplit  = " "

	makeCmd = "__bk_command__ -j__bk_job_server__ BK_CC='__bk_c_compiler__' BK_CXX='__bk_cxx_compiler__' " +
		"BK_JOBS=__bk_job_server__ __bk_param__"
	bazelCmd = "__bk_command__ --bazelrc=__bk_bazelrc__ --noworkspace_rc __bk_param__"
	bladeCmd = "__bk_command__ __bk_param__"
	ninjaCmd = "BK_CC='__bk_c_compiler__' BK_CXX='__bk_cxx_compiler__' __bk_command__ " +
		"-j __bk_job_server__ __bk_param__"
	cmakeArgs = "-DCMAKE_C_COMPILER='__bk_c_compiler_first__' -DCMAKE_C_COMPILER_ARG1='__bk_c_compiler_left__' " +
		"-DCMAKE_CXX_COMPILER='__bk_cxx_compiler_first__' -DCMAKE_CXX_COMPILER_ARG1='__bk_cxx_compiler_left__'"

	commandKey          = "__bk_command__"
	jobServerKey        = "__bk_job_server__"
	cCompilerKey        = "__bk_c_compiler__"
	cxxCompilerKey      = "__bk_cxx_compiler__"
	cCompilerFirstKey   = "__bk_c_compiler_first__"
	cCompilerLeftKey    = "__bk_c_compiler_left__"
	cxxCompilerFirstKey = "__bk_cxx_compiler_first__"
	cxxCompilerLeftKey  = "__bk_cxx_compiler_left__"
	paramKey            = "__bk_param__"
	bazelrcKey          = "__bk_bazelrc__"
	templateJobsKey     = "@BK_JOBS"

	gccCCompiler     = "gcc"
	gccCXXCompiler   = "g++"
	clangCCompiler   = "clang"
	clangCXXCompiler = "clang++"

	ccacheDisableEnvKey = "CCACHE_DISABLE"

	jobServerTimesToCPU = 1.5
)

type distccTask struct {
	// Unique ID for an independent task
	ID string

	// The data exchange with client
	Client taskClient

	// The allocated compiling endpoint for this task
	Compilers []taskCompiler

	// The task status
	Stats taskStats

	// The data concerned by operator
	Operator taskOperator
}

// EnoughAvailableResource check if the task has enough available compiler
func (dt *distccTask) EnoughAvailableResource() bool {
	return dt.Stats.CompilerCount >= dt.Operator.LeastInstance
}

// WorkerList no need
func (dt *distccTask) WorkerList() []string {
	return nil
}

//GetRequestInstance define
func (dt *distccTask) GetRequestInstance() int {
	return dt.Operator.RequestInstance
}

// GetWorkerCount get the worker count.
func (dt *distccTask) GetWorkerCount() int {
	return dt.Stats.CompilerCount
}

// Dump encode task data into json bytes
func (dt *distccTask) Dump() []byte {
	data := dt.CustomData(nil).(distcc.CustomData)
	var tmp []byte
	_ = codec.EncJSON(data, &tmp)
	return tmp
}

// CustomData get task custom data
func (dt *distccTask) CustomData(param interface{}) interface{} {
	jobServer := int(dt.Stats.CPUTotal)
	if 0 < dt.Client.Extra.MaxJobs && dt.Client.Extra.MaxJobs < jobServer {
		jobServer = dt.Client.Extra.MaxJobs
	}

	env := dt.Client.Env
	if env == nil {
		env = map[string]string{}
	}
	unSetEnv := make([]string, 0, 100)
	if dt.Client.CCacheEnabled {
		unSetEnv = append(unSetEnv, ccacheDisableEnvKey)
		_, ok := env[ccacheDisableEnvKey]
		if ok {
			delete(env, ccacheDisableEnvKey)
		}
	} else {
		_, ok := env[ccacheDisableEnvKey]
		if !ok {
			env[ccacheDisableEnvKey] = "true"
		}
	}

	return distcc.CustomData{
		GccVersion:        dt.Client.GccVersion,
		Commands:          dt.Client.Cmd,
		Environments:      env,
		UnsetEnvironments: unSetEnv,
		CCacheEnable:      dt.Client.CCacheEnabled,
		CCCompiler:        dt.Client.Extra.CCCompiler,
		CXXCompiler:       dt.Client.Extra.CXXCompiler,
		JobServer:         jobServer,
		DistCCHosts:       getDistCCHostEnv(dt.Compilers),
	}
}

type taskClient struct {
	// Client side specific
	City          string
	SourceIP      string
	SourceCPU     int
	GccVersion    string
	User          string
	Message       string
	Params        string
	CCacheEnabled bool
	BanDistCC     bool
	BanAllBooster bool
	RunDir        string
	CommandType   CommandType
	Command       string
	Extra         taskClientExtra

	// Server side specific
	Cmd        string
	Env        map[string]string
	RequestCPU float64
	LeastCPU   float64
}

type taskClientExtra struct {
	BazelRC     string `json:"bazelrc"`
	CCCompiler  string `json:"cc_compiler"`
	CXXCompiler string `json:"cxx_compiler"`
	MaxJobs     int    `json:"max_jobs"`
}

type taskStats struct {
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
}

type taskOperator struct {
	ClusterID string
	AppName   string
	Namespace string
	Image     string

	Instance          int
	RequestInstance   int
	LeastInstance     int
	RequestCPUPerUnit float64
	RequestMemPerUnit float64
}

type taskCompiler struct {
	CPU       float64
	Mem       float64
	IP        string
	Port      int
	StatsPort int
	Message   string
	Stats     *ds.StatsInfo
}

type CommandType string

const (
	CommandMake  CommandType = "make"
	CommandCmake CommandType = "cmake"
	CommandBazel CommandType = "bazel"
	CommandBlade CommandType = "blade"
	CommandNinja CommandType = "ninja"
)

// CommandSetting contains command settings
type CommandSetting struct {
	CommandType CommandType
	Command     string

	CompilerVersion string
	CPUNum          int
	CustomParam     string
	LeastJobServer  int

	CCacheEnable  bool
	BanDistCC     bool
	BanAllBooster bool

	Extra taskClientExtra
}

// GetCommand get command or args by settings, this command will be executed directly in client side.
func (cs *CommandSetting) GetCommand() string {
	cCompiler, cxxCompiler := cs.getCompiler()
	cc := strings.Join(cCompiler, " ")
	cxx := strings.Join(cxxCompiler, " ")
	ccFirst := cCompiler[0]
	ccLeft := strings.Join(cCompiler[1:], " ")
	cxxFirst := cxxCompiler[0]
	cxxLeft := strings.Join(cxxCompiler[1:], " ")

	// to be compatible with old client which does not have the command type, and should be all make command
	if cs.CommandType == "" {
		cs.CommandType = CommandMake
	}

	switch cs.CommandType {
	case CommandCmake:
		args := strings.Replace(cmakeArgs, cCompilerFirstKey, ccFirst, -1)
		args = strings.Replace(args, cCompilerLeftKey, ccLeft, -1)
		args = strings.Replace(args, cxxCompilerFirstKey, cxxFirst, -1)
		args = strings.Replace(args, cxxCompilerLeftKey, cxxLeft, -1)
		return args
	case CommandMake:
		if cs.Command == "" {
			cs.Command = "make"
		}
		cmd := strings.Replace(makeCmd, commandKey, cs.Command, -1)
		cmd = strings.Replace(cmd, jobServerKey, strconv.Itoa(cs.getJobs()), -1)
		cmd = strings.Replace(cmd, cCompilerKey, cc, -1)
		cmd = strings.Replace(cmd, cxxCompilerKey, cxx, -1)
		cmd = strings.Replace(cmd, paramKey, cs.CustomParam, -1)
		return cmd
	case CommandBazel:
		if cs.Command == "" {
			cs.Command = "bazel"
		}
		cmd := strings.Replace(bazelCmd, commandKey, cs.Command, -1)
		cmd = strings.Replace(cmd, bazelrcKey, cs.Extra.BazelRC, -1)
		cmd = strings.Replace(cmd, paramKey, cs.CustomParam, -1)
		return cmd
	case CommandBlade:
		if cs.Command == "" {
			cs.Command = "blade"
		}
		cmd := strings.Replace(bladeCmd, commandKey, cs.Command, -1)
		tempParamKey := strings.Replace(cs.CustomParam, templateJobsKey, strconv.Itoa(cs.getJobs()), -1)
		cmd = strings.Replace(cmd, paramKey, tempParamKey, -1)
		return cmd
	case CommandNinja:
		if cs.Command == "" {
			cs.Command = "ninja"
		}
		cmd := strings.Replace(ninjaCmd, commandKey, cs.Command, -1)
		cmd = strings.Replace(cmd, jobServerKey, strconv.Itoa(cs.getJobs()), -1)
		cmd = strings.Replace(cmd, cCompilerKey, cc, -1)
		cmd = strings.Replace(cmd, cxxCompilerKey, cxx, -1)
		cmd = strings.Replace(cmd, paramKey, cs.CustomParam, -1)
		return cmd
	}
	return ""
}

// if client specific the max jobs, then let the jobServer be the less one.
func (cs *CommandSetting) getJobs() int {
	jobServer := cs.CPUNum
	if cs.LeastJobServer > 0 {
		jobServer = cs.LeastJobServer
		if expectJobServer := int(float64(cs.CPUNum) * jobServerTimesToCPU); expectJobServer > jobServer {
			jobServer = expectJobServer
		}
	}

	if 0 < cs.Extra.MaxJobs && cs.Extra.MaxJobs < jobServer {
		return cs.Extra.MaxJobs
	}
	return jobServer
}

// GetCompiler get the compiler param.
func (cs *CommandSetting) GetCompiler() (string, string) {
	cCompiler, cxxCompiler := cs.getCompiler()
	return strings.Join(cCompiler, " "), strings.Join(cxxCompiler, " ")
}

// Decide the compiler from compilerVersion
func (cs *CommandSetting) getCompiler() (cCompiler, cxxCompiler []string) {
	cCompiler = []string{gccCCompiler}
	cxxCompiler = []string{gccCXXCompiler}
	if strings.Contains(cs.CompilerVersion, "gcc") {
		cCompiler = []string{gccCCompiler}
		cxxCompiler = []string{gccCXXCompiler}
	}
	if strings.Contains(cs.CompilerVersion, "clang") {
		cCompiler = []string{clangCCompiler}
		cxxCompiler = []string{clangCXXCompiler}
	}

	if cs.BanAllBooster {
		return
	}

	if !cs.BanDistCC {
		cCompiler = append([]string{"distcc"}, cCompiler...)
		cxxCompiler = append([]string{"distcc"}, cxxCompiler...)
	}

	if cs.CCacheEnable {
		cCompiler = append([]string{"ccache"}, cCompiler...)
		cxxCompiler = append([]string{"ccache"}, cxxCompiler...)
	}

	return
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

func table2Task(tableTask *distcc.TableTask) *distccTask {
	// task client
	var env map[string]string
	_ = codec.DecJSON([]byte(tableTask.Env), &env)

	var extra taskClientExtra
	_ = codec.DecJSON([]byte(tableTask.Extra), &extra)

	client := taskClient{
		City:          tableTask.City,
		SourceIP:      tableTask.SourceIP,
		SourceCPU:     tableTask.SourceCPU,
		GccVersion:    tableTask.GccVersion,
		User:          tableTask.User,
		Params:        tableTask.Params,
		Cmd:           tableTask.Cmd,
		Env:           env,
		RequestCPU:    tableTask.RequestCPU,
		LeastCPU:      tableTask.LeastCPU,
		CCacheEnabled: tableTask.CCacheEnabled,
		BanDistCC:     tableTask.BanDistCC,
		BanAllBooster: tableTask.BanAllBooster,
		RunDir:        tableTask.RunDir,
		CommandType:   CommandType(tableTask.CommandType),
		Command:       tableTask.Command,
		Extra:         extra,
	}

	// task compilers
	var compilers []taskCompiler
	_ = codec.DecJSON([]byte(tableTask.Compilers), &compilers)

	// task stats
	stats := taskStats{
		CompileFilesOK:       tableTask.CompileFilesOK,
		CompileFilesErr:      tableTask.CompileFilesErr,
		CompileFilesTimeout:  tableTask.CompileFilesTimeout,
		CompilerCount:        tableTask.CompilerCount,
		CPUTotal:             tableTask.CPUTotal,
		MemTotal:             tableTask.MemTotal,
		CCacheInfo:           tableTask.CCacheInfo,
		CacheDirectHit:       tableTask.CacheDirectHit,
		CachePreprocessedHit: tableTask.CachePreprocessedHit,
		CacheMiss:            tableTask.CacheMiss,
		FilesInCache:         tableTask.FilesInCache,
		CacheSize:            tableTask.CacheSize,
		MaxCacheSize:         tableTask.MaxCacheSize,
	}

	// task operator
	operator := taskOperator{
		ClusterID:         tableTask.ClusterID,
		AppName:           tableTask.AppName,
		Namespace:         tableTask.Namespace,
		Image:             tableTask.Image,
		Instance:          tableTask.Instance,
		RequestInstance:   tableTask.RequestInstance,
		LeastInstance:     tableTask.LeastInstance,
		RequestCPUPerUnit: tableTask.RequestCPUPerUnit,
		RequestMemPerUnit: tableTask.RequestMemPerUnit,
	}

	return &distccTask{
		ID:        tableTask.TaskID,
		Client:    client,
		Compilers: compilers,
		Stats:     stats,
		Operator:  operator,
	}
}

func task2Table(task *distccTask) *distcc.TableTask {
	var env []byte
	_ = codec.EncJSON(task.Client.Env, &env)

	var compilers []byte
	_ = codec.EncJSON(task.Compilers, &compilers)

	var extra []byte
	_ = codec.EncJSON(task.Client.Extra, &extra)

	return &distcc.TableTask{
		// task client
		City:          task.Client.City,
		SourceIP:      task.Client.SourceIP,
		SourceCPU:     task.Client.SourceCPU,
		GccVersion:    task.Client.GccVersion,
		User:          task.Client.User,
		Params:        task.Client.Params,
		Cmd:           task.Client.Cmd,
		Env:           string(env),
		RequestCPU:    task.Client.RequestCPU,
		LeastCPU:      task.Client.LeastCPU,
		CCacheEnabled: task.Client.CCacheEnabled,
		BanDistCC:     task.Client.BanDistCC,
		BanAllBooster: task.Client.BanAllBooster,
		RunDir:        task.Client.RunDir,
		CommandType:   string(task.Client.CommandType),
		Command:       task.Client.Command,
		Extra:         string(extra),

		// task compilers
		Compilers: string(compilers),

		// task stats
		CompileFilesOK:       task.Stats.CompileFilesOK,
		CompileFilesErr:      task.Stats.CompileFilesErr,
		CompileFilesTimeout:  task.Stats.CompileFilesTimeout,
		CompilerCount:        task.Stats.CompilerCount,
		CPUTotal:             task.Stats.CPUTotal,
		MemTotal:             task.Stats.MemTotal,
		CCacheInfo:           task.Stats.CCacheInfo,
		CacheDirectHit:       task.Stats.CacheDirectHit,
		CachePreprocessedHit: task.Stats.CachePreprocessedHit,
		CacheMiss:            task.Stats.CacheMiss,
		FilesInCache:         task.Stats.FilesInCache,
		CacheSize:            task.Stats.CacheSize,
		MaxCacheSize:         task.Stats.MaxCacheSize,

		// task operator
		ClusterID:         task.Operator.ClusterID,
		AppName:           task.Operator.AppName,
		Namespace:         task.Operator.Namespace,
		Image:             task.Operator.Image,
		Instance:          task.Operator.Instance,
		LeastInstance:     task.Operator.LeastInstance,
		RequestInstance:   task.Operator.RequestInstance,
		RequestCPUPerUnit: task.Operator.RequestCPUPerUnit,
		RequestMemPerUnit: task.Operator.RequestMemPerUnit,
	}
}

func getDistCCHostEnv(compilers []taskCompiler) string {
	hosts := make([]string, 0, 100)
	hosts = append(hosts, envDistCCHostPrefix)
	for _, compiler := range compilers {
		hosts = append(hosts, fmt.Sprintf(envDistCCHostValue, compiler.IP, compiler.Port, int(compiler.CPU)))
	}
	return strings.Join(hosts, envDistCCHostSplit)
}
