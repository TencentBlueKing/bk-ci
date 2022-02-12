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
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
	ds "github.com/Tencent/bk-ci/src/booster/common/store/distcc_server"
	"github.com/Tencent/bk-ci/src/booster/server/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/rd"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct"
	respack "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct"

	"github.com/jinzhu/gorm"
)

const (
	// EngineName define the engine name
	EngineName = "distcc_mac"

	envAllow           = "BK_DISTCC_ALLOW"
	envJob             = "BK_DISTCC_JOBS"
	envCompilerVersion = "BK_COMPILER_VERSION"

	daemonStatsURL = "http://%s:%d/"

	agentCommandLaunchName  = "bk_distcc_mac_distccd"
	agentCommandReleaseName = "bk_distcc_mac_release"
	agentCommandDir         = "bk_distcc_mac"
	agentCommandPath        = "bk_distcc_mac"
)

// EngineConfig
type EngineConfig struct {
	engine.MySQLConf
	Rd rd.RegisterDiscover

	ClusterID           string
	LeastJobServer      int
	JobServerTimesToCPU float64
	Brokers             []config.EngineDistCCBrokerConfig
}

const distCCRunningLongestTime = 6 * time.Hour

var preferences = engine.Preferences{
	HeartbeatTimeoutTickTimes: 12,
}

// NewDistccMacEngine get a new distcc_mac engine
// engine distcc_mac which use direct rm is different from engine distcc which use container rm.
// engine distcc_mac use the same tables with engine distcc.
// EngineConfig:   describe the basic config of engine including mysql config
// HandleWithUser: registered from a direct resource manager, used to handle the resources and launch tasks.
func NewDistccMacEngine(conf EngineConfig, mgr direct.HandleWithUser) (engine.Engine, error) {
	m, err := distcc.NewMySQL(conf.MySQLConf)
	if err != nil {
		blog.Errorf("engine(%s) get new mysql(%+v) failed: %v", EngineName, conf.MySQLConf, err)
		return nil, err
	}

	egn := &distccMacEngine{
		conf:        conf,
		publicQueue: engine.NewStagingTaskQueue(),
		mysql:       m,
		mgr:         mgr,
	}

	return egn, nil
}

type distccMacEngine struct {
	conf        EngineConfig
	publicQueue engine.StagingTaskQueue
	mysql       distcc.MySQL

	// distcc mac use direct resource manager
	mgr direct.HandleWithUser
}

// Name get the engine name
func (de *distccMacEngine) Name() engine.TypeName {
	return EngineName
}

// SelectFirstTaskBasic select a task from given task queue group and the request is ask from the specific queue.
// commonly select a task from the queue of the given name
// if the queue if empty, then try get task from the public queue, the task is from other busy queues.
func (de *distccMacEngine) SelectFirstTaskBasic(
	tqg *engine.TaskQueueGroup,
	queueName string) (*engine.TaskBasic, error) {
	tb, err := tqg.GetQueue(queueName).First()
	if err == engine.ErrorNoTaskInQueue {

		// get first valid task from public queue
		// if task not exist in queue group, just delete it from public queue.
		for {
			tb, err = de.publicQueue.First()
			if err == engine.ErrorNoTaskInQueue {
				return nil, err
			}

			if tqg.Exist(tb.ID) {
				break
			}

			_ = de.publicQueue.Delete(tb.ID)
		}
	}

	if err != nil {
		blog.Errorf("engine(%s) get first from queue(%s) failed: %v", EngineName, queueName, err)
		return nil, err
	}

	return tb, nil
}

// CreateTaskExtension create task extension with extra data into db
func (de *distccMacEngine) CreateTaskExtension(tb *engine.TaskBasic, extra []byte) error {
	return de.createTask(tb, extra)
}

// GetTaskExtension get task extension from db
func (de *distccMacEngine) GetTaskExtension(taskID string) (engine.TaskExtension, error) {
	return de.getTask(taskID)
}

// LaunchTask try launching task from queue.
// first try to consume the resource, if success then launch the workers.
func (de *distccMacEngine) LaunchTask(tb *engine.TaskBasic, queueName string) error {
	if err := de.consumeResource(tb, queueName); err != nil {
		return err
	}

	return de.launchTask(tb.ID)
}

// DegradeTask degrade to local compiling, keep running without any workers and set the correct client commands.
func (de *distccMacEngine) DegradeTask(taskID string) error {
	return de.degradeTask(taskID)
}

// LaunchDone check if the launch is done
func (de *distccMacEngine) LaunchDone(taskID string) (bool, error) {
	return de.launchDone(taskID)
}

// CheckTask check task when running, failed with error, such as running timeout.
func (de *distccMacEngine) CheckTask(tb *engine.TaskBasic) error {
	return de.checkTask(tb)
}

// SendProjectMessage send project message usually used to handle the cmake args.
func (de *distccMacEngine) SendProjectMessage(projectID string, extra []byte) ([]byte, error) {
	return de.sendProjectMessage(projectID, extra)
}

// SendTaskMessage send task message, record the stats from client's report.
func (de *distccMacEngine) SendTaskMessage(taskID string, extra []byte) ([]byte, error) {
	return de.sendTaskMessage(taskID, extra)
}

// CollectTaskData collect the task data and the distcc daemon stats, record the stats from server side.
func (de *distccMacEngine) CollectTaskData(tb *engine.TaskBasic) error {
	return de.collectTaskData(tb)
}

// ReleaseTask release task, shut down workers and free the resources.
func (de *distccMacEngine) ReleaseTask(taskID string) error {
	return de.releaseTask(taskID)
}

// GetPreferences return the preferences
func (de *distccMacEngine) GetPreferences() engine.Preferences {
	return preferences
}

// GetTaskBasicTable get task basic table db operator
func (de *distccMacEngine) GetTaskBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(distcc.TableTask{}.TableName())
}

// GetProjectBasicTable get project basic table db operator
func (de *distccMacEngine) GetProjectBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(distcc.TableProjectSetting{}.TableName())
}

// GetProjectInfoBasicTable get project info basic table db operator
func (de *distccMacEngine) GetProjectInfoBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(distcc.TableProjectInfo{}.TableName())
}

// GetWhitelistBasicTable get whitelist basic table db operator
func (de *distccMacEngine) GetWhitelistBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(distcc.TableWhitelist{}.TableName())
}

func (de *distccMacEngine) getClient(timeoutSecond int) *httpclient.HTTPClient {
	client := httpclient.NewHTTPClient()
	client.SetTimeOut(time.Duration(timeoutSecond) * time.Second)
	return client
}

func (de *distccMacEngine) createTask(tb *engine.TaskBasic, extra []byte) error {
	project, err := de.mysql.GetProjectSetting(tb.Client.ProjectID)
	if err != nil {
		blog.Errorf("engine(%s) try creating task(%s), get project setting(%s) failed: %v",
			EngineName, tb.ID, tb.Client.ProjectID, err)
		return err
	}

	var ev ExtraData
	if err = codec.DecJSON(extra, &ev); err != nil {
		blog.Errorf("engine(%s) try creating task(%s), decode extra(%s) failed: %v",
			EngineName, tb.ID, string(extra), err)
		return err
	}

	gccVersion := project.GccVersion
	if ev.GccVersion != "" {
		gccVersion = ev.GccVersion
	}

	task, err := de.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try creating task(%s), get task failed: %v", EngineName, tb.ID, err)
		return err
	}

	task.Client.City = tb.Client.QueueName
	task.Client.GccVersion = gccVersion
	task.Client.SourceIP = tb.Client.ClientIP
	task.Client.SourceCPU = tb.Client.ClientCPU
	task.Client.User = ev.User
	task.Client.Params = ev.Params
	task.Client.CCacheEnabled = project.CCacheEnabled
	if ev.CCacheEnabled != nil {
		task.Client.CCacheEnabled = *ev.CCacheEnabled
	}
	task.Client.BanDistCC = project.BanDistCC
	task.Client.BanAllBooster = project.BanAllBooster
	task.Client.RunDir = ev.RunDir
	task.Client.CommandType = ev.CommandType
	task.Client.Command = ev.Command
	task.Client.Extra.BazelRC = ev.ExtraVars.BazelRC
	task.Client.Extra.MaxJobs = ev.ExtraVars.MaxJobs
	task.Client.RequestCPU = project.RequestCPU
	task.Client.LeastCPU = project.LeastCPU
	if task.Client.LeastCPU > task.Client.RequestCPU {
		task.Client.LeastCPU = task.Client.RequestCPU
	}
	task.Operator.ClusterID = de.conf.ClusterID
	task.Operator.AppName = task.ID
	task.Operator.Namespace = EngineName

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try creating task, update task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}
	blog.Infof("engine(%s) success to create task(%s)", EngineName, tb.ID)
	return nil
}

// ExtraData describe the data in task creation from client.
type ExtraData struct {
	User          string `json:"user"`
	GccVersion    string `json:"gcc_version"`
	RunDir        string `json:"run_dir"`
	Params        string `json:"params"` //自定义参数
	CCacheEnabled *bool  `json:"ccache_enabled"`

	// command define the target to be called, such as make, bazel, /data/custom/make etc.
	Command     string      `json:"command,omitempty"`
	CommandType CommandType `json:"command_type,omitempty"`

	// extra_vars includes the extra params need by client
	ExtraVars ExtraVars `json:"extra_vars,omitempty"`
}

// ExtraVars describe the extra params in ExtraData
type ExtraVars struct {
	// bazelrc define the bazelrc file path
	BazelRC string `json:"bazelrc"`
	MaxJobs int    `json:"max_jobs,omitempty"`
}

func (de *distccMacEngine) getTask(taskID string) (*distccTask, error) {
	t, err := de.mysql.GetTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) get task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	return table2Task(t), nil
}

func (de *distccMacEngine) updateTask(task *distccTask) error {
	data, err := engine.GetMapExcludeTableTaskBasic(task2Table(task))
	if err != nil {
		blog.Errorf("engine(%s) update task(%s), get exclude map failed: %v", EngineName, task.ID, err)
		return err
	}

	if err = de.mysql.UpdateTask(task.ID, data); err != nil {
		blog.Errorf("engine(%s) update task(%s), update failed: %v", EngineName, task.ID, err)
		return err
	}

	return nil
}

func (de *distccMacEngine) consumeResource(tb *engine.TaskBasic, queueName string) error {
	taskID := tb.ID
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try consuming resource, get task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	condition := &resourceCondition{
		queueName: queueName,
		leastCPU:  task.Client.LeastCPU,
		maxCPU:    task.Client.RequestCPU,
	}
	_, err = de.mgr.GetFreeResource(taskID, condition, de.resourceSelector, nil)
	if err == engine.ErrorNoEnoughResources {
		de.publicQueue.Add(tb)
		return err
	}
	if err != nil {
		blog.Errorf("engine(%s) try consuming resource, get free resource(%+v) failed: %v",
			EngineName, condition, err)
		return err
	}

	blog.Infof("engine(%s) success to consume resource for task(%s)", EngineName, taskID)
	return nil
}

func (de *distccMacEngine) launchTask(taskID string) error {
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try launching task, get task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	resources, err := de.mgr.ListResource(task.ID)
	if err != nil {
		blog.Errorf("engine(%s) try launching task(%s), list resource(%s) failed: %v",
			EngineName, task.ID, task.ID, err)
		return err
	}

	for _, r := range resources {
		if err = de.mgr.ExecuteCommand(r.Base.IP, task.ID, &respack.Command{
			Cmd:     agentCommandLaunchName,
			CmdType: respack.CmdLaunch,
			Env: map[string]string{
				envAllow:           strings.Join(append(de.getServerIPList(), task.Client.SourceIP), ","),
				envJob:             fmt.Sprintf("%d", int(r.Resource.CPU)),
				envCompilerVersion: task.Client.GccVersion,
			},
			UserDefineID: task.ID,
			Dir:          agentCommandDir,
			Path:         agentCommandPath,
		}); err != nil {
			blog.Errorf("engine(%s) try launching task(%s), execute command on(%s) failed: %v",
				EngineName, taskID, r.Base.IP, err)
			return err
		}
	}

	blog.Infof("engine(%s) success to launch task(%s)", EngineName, taskID)
	return nil
}

func (de *distccMacEngine) degradeTask(taskID string) error {
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try degrading task, get task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	cs := CommandSetting{
		CommandType:     task.Client.CommandType,
		Command:         task.Client.Command,
		CompilerVersion: task.Client.GccVersion,
		CPUNum:          task.Client.SourceCPU,
		CustomParam:     task.Client.Params,
		CCacheEnable:    task.Client.CCacheEnabled,
		BanDistCC:       true,
		BanAllBooster:   task.Client.BanAllBooster,
		Extra:           task.Client.Extra,
	}
	task.Client.Cmd = cs.GetCommand()
	task.Client.Extra.CCCompiler, task.Client.Extra.CXXCompiler = cs.GetCompiler()

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try degrading task, update task(%s) failed: %v", EngineName, taskID, err)
		return err
	}
	blog.Infof("engine(%s) success to degrade task(%s)", EngineName, taskID)
	return nil
}

func (de *distccMacEngine) launchDone(taskID string) (bool, error) {
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try checking if task launch done, get task(%s) failed: %v",
			EngineName, taskID, err)
		return false, err
	}

	infoList, err := de.mgr.ListCommands(task.ID)
	if err != nil {
		blog.Errorf("engine(%s) try checking if task(%s) launch done, list commands failed: %v",
			EngineName, taskID, err)
		return false, err
	}

	compilerList := make([]taskCompiler, 0, 100)
	for _, info := range infoList {
		switch info.Status {
		case respack.CommandStatusInit:
			return false, nil
		case respack.CommandStatusSucceed:
			compilerList = append(compilerList, taskCompiler{
				CPU:       0,
				Mem:       0,
				IP:        info.IP,
				Port:      31000,
				StatsPort: 31001,
			})
		}
	}

	// get resource info from resource list
	resourceList, err := de.mgr.ListResource(task.ID)
	if err != nil {
		blog.Error("engine(%s) try list resources of task(%s) failed: %v", EngineName, taskID, err)
		return false, err
	}
	for _, r := range resourceList {
		for i := range compilerList {
			if r.Base.IP == compilerList[i].IP {
				compilerList[i].CPU = r.Resource.CPU
				compilerList[i].Mem = r.Resource.Mem
				break
			}
		}
	}

	task.Compilers = compilerList
	task.Stats.CompilerCount = len(task.Compilers)
	task.Client.Env = map[string]string{
		envDistCCHostKey: getDistCCHostEnv(compilerList),
	}

	cs := CommandSetting{
		CommandType:     task.Client.CommandType,
		Command:         task.Client.Command,
		CompilerVersion: task.Client.GccVersion,
		CPUNum:          int(task.Operator.RequestCPUPerUnit) * task.Stats.CompilerCount,
		CustomParam:     task.Client.Params,
		CCacheEnable:    task.Client.CCacheEnabled,
		BanDistCC:       task.Client.BanDistCC,
		BanAllBooster:   task.Client.BanAllBooster,
		LeastJobServer:  de.conf.LeastJobServer,
		Extra:           task.Client.Extra,
	}

	task.Client.Cmd = cs.GetCommand()
	task.Client.Extra.CCCompiler, task.Client.Extra.CXXCompiler = cs.GetCompiler()
	task.Stats.CPUTotal = float64(task.Stats.CompilerCount) * task.Operator.RequestCPUPerUnit
	task.Stats.MemTotal = float64(task.Stats.CompilerCount) * task.Operator.RequestMemPerUnit

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try checking service info, update task(%s) failed: %v", EngineName, taskID, err)
		return false, err
	}
	blog.Infof("engine(%s) success to checking service info, task(%s) launching done", EngineName, taskID)
	return true, nil
}

func (de *distccMacEngine) checkTask(tb *engine.TaskBasic) error {
	if tb.Status.Status == engine.TaskStatusRunning &&
		tb.Status.StartTime.Add(distCCRunningLongestTime).Before(time.Now().Local()) {
		return fmt.Errorf("distcc running since(%s) reaches the timeout(%s)",
			tb.Status.StartTime.String(), distCCRunningLongestTime.String())
	}

	return nil
}

func (de *distccMacEngine) collectTaskData(tb *engine.TaskBasic) error {
	task, err := de.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try collecting task data, get task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	for i, compiler := range task.Compilers {
		uri := fmt.Sprintf(daemonStatsURL, compiler.IP, compiler.StatsPort)
		blog.Infof("collect task ID(%s) stats info: %s", task.ID, uri)

		stats, err := de.collectStatsInfo(uri)
		if err != nil {
			blog.Warnf("collect task ID(%s) stats failed url(%s): %v", task.ID, uri, err)
			task.Compilers[i].Message = err.Error()
		}
		task.Compilers[i].Stats = stats

		if stats != nil {
			task.Stats.CompileFilesOK += stats.CompileOK
			task.Stats.CompileFilesErr += stats.CompileErr
			task.Stats.CompileFilesTimeout += stats.CompileTimeout
		}
	}

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try collecting task data, update task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	if err = de.mysql.AddProjectInfoStats(tb.Client.ProjectID, distcc.DeltaInfoStats{
		CompileFilesOK:      task.Stats.CompileFilesOK,
		CompileFilesErr:     task.Stats.CompileFilesErr,
		CompileFilesTimeout: task.Stats.CompileFilesTimeout,
		CompileUnits:        float64(tb.Status.EndTime.Unix()-tb.Status.StartTime.Unix()) * task.Stats.CPUTotal,
	}); err != nil {
		blog.Errorf("engine(%s) try collecting task(%s) data, add project(%s) info stats failed: %v",
			EngineName, tb.ID, tb.Client.ProjectID, err)
		return err
	}

	blog.Infof("engine(%s) success to collect task(%s) data", EngineName, tb.ID)
	return nil
}

func (de *distccMacEngine) collectStatsInfo(uri string) (*ds.StatsInfo, error) {
	data, err := de.getClient(1).GET(uri, nil, nil)
	if err != nil {
		blog.Errorf("failed to fetch stats %s: %v", uri, err)
		return nil, err
	}

	lines := strings.Split(string(data), "\n")
	stats := new(ds.StatsInfo)
	for _, line := range lines {
		stats.Parse(line)
	}
	return stats, nil
}

func (de *distccMacEngine) releaseTask(taskID string) error {
	blog.Infof("engine(%s) try to release task(%s)", EngineName, taskID)
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try releasing task, get task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	if err = de.mgr.ReleaseResource(task.ID); err != nil {
		blog.Errorf("engine(%s) try releasing task, release task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	blog.Infof("engine(%s) success to release task(%s)", EngineName, taskID)
	return nil
}

func (de *distccMacEngine) sendProjectMessage(projectID string, extra []byte) ([]byte, error) {
	var msg Message
	if err := codec.DecJSON(extra, &msg); err != nil {
		blog.Errorf("engine(%s) try sending project(%s) message, decode message(%s) failed: %v",
			EngineName, projectID, string(extra), err)
		return nil, err
	}

	switch msg.Type {
	case MessageTypeGetCMakeArgs:
		return de.sendMessageGetCMakeArgs(projectID, msg.MessageGetCMakeArgs)
	default:
		err := engine.ErrorUnknownMessageType
		blog.Errorf("engine(%s) try sending project(%s) message type(%d) failed: %v",
			EngineName, projectID, msg.Type, err)
		return nil, err
	}
}

func (de *distccMacEngine) sendTaskMessage(taskID string, extra []byte) ([]byte, error) {
	var msg Message
	if err := codec.DecJSON(extra, &msg); err != nil {
		blog.Errorf("engine(%s) try sending task(%s) message, decode message(%s) failed: %v",
			EngineName, taskID, string(extra), err)
		return nil, err
	}

	switch msg.Type {
	case MessageTypeRecordStats:
		return de.sendMessageRecordStats(taskID, msg.MessageRecordStats)
	default:
		err := engine.ErrorUnknownMessageType
		blog.Errorf("engine(%s) try sending task(%s) message type(%d) failed: %v",
			EngineName, taskID, msg.Type, err)
		return nil, err
	}
}

func (de *distccMacEngine) sendMessageRecordStats(taskID string, msg MessageRecordStats) ([]byte, error) {
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try sending task message, get task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	task.Client.Message = msg.Message
	task.Stats.CCacheInfo = string(msg.CCacheStats.Dump())
	task.Stats.CacheDirectHit = int64(msg.CCacheStats.DirectHit)
	task.Stats.CachePreprocessedHit = int64(msg.CCacheStats.PreprocessedHit)
	task.Stats.CacheMiss = int64(msg.CCacheStats.CacheMiss)
	task.Stats.FilesInCache = int64(msg.CCacheStats.FilesInCache)
	task.Stats.CacheSize = msg.CCacheStats.CacheSize
	task.Stats.MaxCacheSize = msg.CCacheStats.MaxCacheSize

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try sending task message, update task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	blog.Infof("engine(%s) success to sending task(%s) message", EngineName, taskID)
	return (&MessageResponse{}).Dump(), nil
}

func (de *distccMacEngine) sendMessageGetCMakeArgs(projectID string, msg MessageGetCMakeArgs) ([]byte, error) {
	project, err := de.mysql.GetProjectSetting(projectID)
	if err != nil {
		blog.Errorf("engine(%s) try sending task message, get project(%s) settings failed: %v",
			EngineName, projectID, err)
		return nil, err
	}

	cacheEnabled := project.CCacheEnabled
	if msg.CCacheEnabled != nil {
		cacheEnabled = *msg.CCacheEnabled
	}
	cs := CommandSetting{
		CommandType:     CommandCmake,
		CompilerVersion: project.GccVersion,
		CCacheEnable:    cacheEnabled,
		BanDistCC:       project.BanDistCC,
		BanAllBooster:   project.BanAllBooster,
	}

	resp := &MessageResponse{
		CMakeArgs: cs.GetCommand(),
	}
	return resp.Dump(), nil
}

// Message describe the data format from SendMessage caller.
type Message struct {
	Type MessageType `json:"type"`

	MessageRecordStats  MessageRecordStats  `json:"record_stats"`
	MessageGetCMakeArgs MessageGetCMakeArgs `json:"get_cmake_args"`
}

type MessageType int

const (
	// MessageTypeRecordStats means this message is about record stats from client.
	MessageTypeRecordStats MessageType = iota

	// MessageTypeGetCMakeArgs means this message is about to get cmake args.
	MessageTypeGetCMakeArgs
)

// MessageRecordStats describe the message data of type record stats.
type MessageRecordStats struct {
	Message     string      `json:"message"`
	CCacheStats CCacheStats `json:"ccache_stats"`
}

// MessageGetCMakeArgs describe the message data of type cmake args.
type MessageGetCMakeArgs struct {
	CCacheEnabled *bool `json:"ccache_enabled"`
}

// MessageResponse describe the return data of SendMessage
type MessageResponse struct {
	CMakeArgs string `json:"cmake_args"`
}

// Dump dump the struct dat into byte
func (mr *MessageResponse) Dump() []byte {
	var data []byte
	_ = codec.EncJSON(mr, &data)
	return data
}

func (de *distccMacEngine) getServerIPList() []string {
	r := make([]string, 0, 100)
	l, _ := de.conf.Rd.GetServers()
	for _, i := range l {
		r = append(r, i.IP)
	}
	return r
}

func (de *distccMacEngine) resourceSelector(
	freeAgent []*respack.AgentResourceExternal,
	condition interface{}) ([]*respack.AgentResourceExternal, error) {
	if freeAgent == nil || len(freeAgent) == 0 {
		return nil, engine.ErrorNoEnoughResources
	}

	c, ok := condition.(*resourceCondition)
	if !ok {
		blog.Errorf("engine(%s) get resource condition type error", EngineName)
		return nil, engine.ErrorInnerEngineError
	}

	blog.Infof("engine(%s) ready take free resource with condition [%+v] queue[%s], len(free)[%d]",
		EngineName, condition, c.queueName, len(freeAgent))

	var cpuTotal float64 = 0
	r := make([]*respack.AgentResourceExternal, 0, 100)
	for _, agent := range freeAgent {
		if cpuTotal >= c.maxCPU {
			break
		}

		if agent.Base.Cluster != c.queueName {
			continue
		}

		if agent.Resource.CPU <= 0 {
			continue
		}

		cpuTotal += agent.Resource.CPU
		r = append(r, agent)
	}

	if cpuTotal < c.leastCPU {
		return nil, engine.ErrorNoEnoughResources
	}

	return r, nil
}

type resourceCondition struct {
	queueName string
	leastCPU  float64
	maxCPU    float64
}

// GetReleaseCommand get release commands
func GetReleaseCommand() *respack.Command {
	return &respack.Command{
		Cmd:     agentCommandReleaseName,
		CmdType: respack.CmdRelease,
		Dir:     agentCommandDir,
		Path:    agentCommandPath,
	}
}
