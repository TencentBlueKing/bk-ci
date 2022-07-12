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
	"regexp"
	"strings"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/server/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm/operator"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct"

	"github.com/jinzhu/gorm"
)

const (
	// define the engine name
	EngineName = "apisjob"

	envAgentCores      = "AGENT_CORES"
	envCoordinatorIP   = "COORD_IP"
	envCoordinatorPort = "COORD_PORT"
	envCacheIP         = "CACHE_IP"
	envCachePort       = "CACHE_PORT"
	portAgent          = "AGENT"
	portFileAgent      = "FILEAGENT"
	portGdtCS          = "GDT_CS"
	portLogDownLoader  = "LOG_DOWNLOADER"
	portExporter       = "EXPORTER"
	portTaskDebug      = "TASK_DEBUG"

	queueNameHeaderSymbol = "://"
)

type queueNameHeader string

const (
	queueNameHeaderDirectWin queueNameHeader = "WIN"
	queueNameHeaderK8SWin    queueNameHeader = "K8S_WIN"
)

// EngineConfig
type EngineConfig struct {
	engine.MySQLConf
	QueueResourceAllocater map[string]config.ResourceAllocater

	// k8s cluster info
	K8SCRMClusterID      string
	K8SCRMCPUPerInstance float64
	K8SCRMMemPerInstance float64
}

var preferences = engine.Preferences{
	HeartbeatTimeoutTickTimes: 12,
}

// NewApisEngine get a new apis engine
// EngineConfig:   describe the basic config of engine including mysql config
// HandleWithUser: registered from a direct resource manager, used to handle the resources and launch tasks.
func NewApisEngine(
	conf EngineConfig,
	k8sCrmMgr crm.HandlerWithUser,
	directMgr direct.HandleWithUser) (engine.Engine, error) {
	m, err := NewMySQL(conf.MySQLConf)
	if err != nil {
		blog.Errorf("engine(%s) get new mysql(%+v) failed: %v", EngineName, conf.MySQLConf, err)
		return nil, err
	}
	egn := &apisEngine{
		conf: conf,
		publicQueueMap: map[string]engine.StagingTaskQueue{
			publicQueueDirectWindows: engine.NewStagingTaskQueue(),
			publicQueueK8SWindows:    engine.NewStagingTaskQueue(),
		},
		mysql:     m,
		k8sCrmMgr: k8sCrmMgr,
		directMgr: directMgr,
	}

	_ = egn.parseAllocateConf()
	return egn, nil
}

const (
	publicQueueDirectWindows = "direct_windows"
	publicQueueK8SWindows    = "k8s_windows"
)

type apisEngine struct {
	conf           EngineConfig
	mysql          MySQL
	publicQueueMap map[string]engine.StagingTaskQueue

	// k8s container resource manager
	k8sCrmMgr crm.HandlerWithUser

	// direct resource manager
	directMgr direct.HandleWithUser
}

// Name get the engine name
func (ae *apisEngine) Name() engine.TypeName {
	return EngineName
}

// SelectFirstTaskBasic select a task from given task queue group and the request is ask from the specific queue.
// commonly select a task from the queue of the given name
// if the queue if empty, then try get task from the public queue, the task is from other busy queues.
func (ae *apisEngine) SelectFirstTaskBasic(tqg *engine.TaskQueueGroup, queueName string) (*engine.TaskBasic, error) {
	tb, err := tqg.GetQueue(queueName).First()
	if err == engine.ErrorNoTaskInQueue {

		publicQueue := ae.getPublicQueueByQueueName(queueName)
		if publicQueue == nil {
			// some queue should not share resources with others.
			return nil, engine.ErrorNoTaskInQueue
		}

		// get first valid task from public queue
		// if task not exist in queue group, just delete it from public queue.
		for {
			tb, err = publicQueue.First()
			if err == engine.ErrorNoTaskInQueue {
				return nil, err
			}

			if tqg.Exist(tb.ID) {
				break
			}

			_ = publicQueue.Delete(tb.ID)
		}
	}

	if err != nil {
		blog.Errorf("engine(%s) get first from queue(%s) failed: %v", EngineName, queueName, err)
		return nil, err
	}

	return tb, nil
}

// CreateTaskExtension create task extension with extra data into db
func (ae *apisEngine) CreateTaskExtension(tb *engine.TaskBasic, extra []byte) error {
	return ae.createTask(tb, extra)
}

// GetTaskExtension get task extension from db
func (ae *apisEngine) GetTaskExtension(taskID string) (engine.TaskExtension, error) {
	return ae.getTask(taskID)
}

// LaunchTask try launching task from queue.
// first try to consume the resource, if success then launch the workers.
func (ae *apisEngine) LaunchTask(tb *engine.TaskBasic, queueName string) error {
	task, err := ae.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try launching task, get task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	if matchDirectResource(queueName) {
		return ae.launchDirectTask(task, tb, queueName)
	}

	return ae.launchCRMTask(task, tb, queueName)
}

// DegradeTask do not support degrade mode.
func (ae *apisEngine) DegradeTask(taskID string) error {
	return engine.ErrorNoSupportDegrade
}

// LaunchDone check if the launch is done
func (ae *apisEngine) LaunchDone(taskID string) (bool, error) {
	return ae.launchDone(taskID)
}

// CheckTask do not support task check.
func (ae *apisEngine) CheckTask(tb *engine.TaskBasic) error {
	return nil
}

// SendProjectMessage do not support project message
func (ae *apisEngine) SendProjectMessage(projectID string, message []byte) ([]byte, error) {
	return nil, nil
}

// SendTaskMessage send task message, record the stats from client's report.
func (ae *apisEngine) SendTaskMessage(taskID string, extra []byte) ([]byte, error) {
	return ae.sendTaskMessage(taskID, extra)
}

// CollectTaskData collect the task data, record the stats from server side.
func (ae *apisEngine) CollectTaskData(tb *engine.TaskBasic) error {
	return ae.collectTaskData(tb)
}

// ReleaseTask release task, shut down workers and free the resources.
func (ae *apisEngine) ReleaseTask(taskID string) error {
	return ae.releaseTask(taskID)
}

// GetPreferences return the preferences
func (ae *apisEngine) GetPreferences() engine.Preferences {
	return preferences
}

// GetTaskBasicTable get task basic table db operator
func (ae *apisEngine) GetTaskBasicTable() *gorm.DB {
	return ae.mysql.GetDB().Table(TableTask{}.TableName())
}

// GetProjectBasicTable get project basic table db operator
func (ae *apisEngine) GetProjectBasicTable() *gorm.DB {
	return ae.mysql.GetDB().Table(TableProjectSetting{}.TableName())
}

// GetProjectInfoBasicTable get project info basic table db operator
func (ae *apisEngine) GetProjectInfoBasicTable() *gorm.DB {
	return ae.mysql.GetDB().Table(TableProjectInfo{}.TableName())
}

// GetWhitelistBasicTable get whitelist basic table db operator
func (ae *apisEngine) GetWhitelistBasicTable() *gorm.DB {
	return ae.mysql.GetDB().Table(TableWhitelist{}.TableName())
}

func (ae *apisEngine) launchDone(taskID string) (bool, error) {
	task, err := ae.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try checking if task launch done, get task(%s) failed: %v",
			EngineName, taskID, err)
		return false, err
	}

	if matchDirectResource(task.ContainerSetting.QueueName) {
		return ae.launchDirectDone(task)
	}

	return ae.launchCRMDone(task)
}

func (ae *apisEngine) launchDirectDone(task *apisTask) (bool, error) {
	if ae.directMgr == nil {
		return false, fmt.Errorf("engine(%s) direct mgr not init", EngineName)
	}

	infoList, err := ae.directMgr.ListCommands(task.ResourceID)
	if err != nil {
		blog.Errorf("engine(%s) try checking if task(%s) launch direct done, list commands failed: %v",
			EngineName, task.TaskID, err)
		return false, err
	}

	availableList := make([]string, 0, 100)
	for _, info := range infoList {
		switch info.Status {
		case direct.CommandStatusInit:
			return false, nil
		case direct.CommandStatusSucceed:
			availableList = append(availableList, fmt.Sprintf("%s:%d", info.IP, task.AgentPort))
		}
	}

	task.WorkerIPList = availableList
	if err = ae.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try checking service info, update direct task(%s) failed: %v",
			EngineName, task.TaskID, err)
		return false, err
	}
	blog.Infof("engine(%s) success to checking service info, task(%s) direct launching done",
		EngineName, task.TaskID)
	return true, nil
}

func (ae *apisEngine) launchCRMDone(task *apisTask) (bool, error) {
	crmMgr := ae.getCrMgr(task.ContainerSetting.QueueName)
	if crmMgr == nil {
		return false, fmt.Errorf("engine(%s) container mgr not init", EngineName)
	}

	// if still preparing, then it's not need to get service info
	isPreparing, err := crmMgr.IsServicePreparing(task.TaskID)
	if err != nil {
		blog.Errorf("engine(%s) try checking service info, check if crm service preparing(%s) failed: %v",
			EngineName, task.TaskID, err)
		return false, err
	}
	if isPreparing {
		return false, nil
	}

	info, err := crmMgr.GetServiceInfo(task.TaskID)
	if err != nil {
		blog.Errorf("engine(%s) try checking service info, get crm info(%s) failed: %v",
			EngineName, task.TaskID, err)
		return false, err
	}

	if info.Status == crm.ServiceStatusStaging {
		return false, nil
	}

	availableList := make([]string, 0, 100)
	for _, endpoints := range info.AvailableEndpoints {
		port, _ := endpoints.Ports[portAgent]

		availableList = append(availableList, fmt.Sprintf("%s:%d", endpoints.IP, port))
	}

	task.WorkerIPList = availableList
	if err = ae.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try checking service info, update crm task(%s) failed: %v",
			EngineName, task.TaskID, err)
		return false, err
	}
	blog.Infof("engine(%s) success to checking service info, task(%s) crm launching done",
		EngineName, task.TaskID)
	return true, nil
}

func (ae *apisEngine) collectTaskData(tb *engine.TaskBasic) error {
	task, err := ae.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try collecting task data, get task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	if err = ae.mysql.AddProjectInfoStats(tb.Client.ProjectID, DeltaInfoStats{
		ServiceUnits: float64(tb.Status.EndTime.Unix()-tb.Status.StartTime.Unix()) * task.CPUTotal,
	}); err != nil {
		blog.Errorf("engine(%s) try collecting task(%s) data, add project(%s) info stats failed: %v",
			EngineName, tb.ID, tb.Client.ProjectID, err)
		return err
	}

	blog.Infof("engine(%s) success to collect task(%s) data", EngineName, tb.ID)
	return nil
}

// TODO: api direct has been deprecated
func (ae *apisEngine) launchDirectTask(task *apisTask, tb *engine.TaskBasic, queueName string) error {
	if ae.directMgr == nil {
		return fmt.Errorf("engine(%s) direct mgr not init", EngineName)
	}

	condition := &resourceCondition{
		queueName: queueName,
		leastCPU:  task.LeastCPU,
		maxCPU:    task.RequestCPU,
	}
	resources, err := ae.directMgr.GetFreeResource(tb.ID, condition, ae.resourceSelector, nil)
	if err == engine.ErrorNoEnoughResources {
		return err
	}
	if err != nil {
		blog.Errorf("engine(%s) try consuming direct resource, get free resource(%+v) failed: %v",
			EngineName, condition, err)
		return err
	}
	task.ResourceID = tb.ID

	// record the cpu/mem used total
	var cpuTotal float64 = 0
	var memTotal float64 = 0
	for _, r := range resources {
		cpuTotal += r.Resource.CPU
		memTotal += r.Resource.Mem
	}
	task.CPUTotal = cpuTotal
	task.MemTotal = memTotal

	if err = ae.updateTask(task); err != nil {
		blog.Errorf("engine(%s) consume direct resource failed for task(%s): %v", EngineName, tb.ID, err)
		return err
	}
	blog.Infof("engine(%s) success to consume direct resource for task(%s)", EngineName, tb.ID)

	blog.Infof("engine(%s) try to launch direct task(%s)", EngineName, tb.ID)
	var wg sync.WaitGroup
	for _, res := range resources {
		wg.Add(1)

		go func(r *direct.AgentResourceExternal) {
			defer wg.Done()
			if err = ae.directMgr.ExecuteCommand(r.Base.IP, task.ResourceID, &direct.Command{
				Cmd:          task.AgentCommandName,
				CmdType:      direct.CmdLaunch,
				Parameters:   task.getCommandParameters(r.Base.IP, int(r.Resource.CPU), task.UseGdt),
				UserDefineID: tb.ID,
				Dir:          task.AgentCommandDir,
				Path:         task.AgentCommandPath,
			}); err != nil {
				blog.Errorf("engine(%s) try launching direct task(%s), execute command on(%s) failed: %v",
					EngineName, tb.ID, r.Base.IP, err)
			}
		}(res)
	}
	wg.Wait()

	blog.Infof("engine(%s) success to launch direct task(%s)", EngineName, tb.ID)
	return nil
}

func (ae *apisEngine) launchCRMTask(task *apisTask, tb *engine.TaskBasic, queueName string) error {
	crmMgr := ae.getCrMgr(task.ContainerSetting.QueueName)
	if crmMgr == nil {
		return fmt.Errorf("engine(%s) container mgr not init", EngineName)
	}
	var err error

	pureQueueName := getQueueNamePure(queueName)

	// generate volumes settings
	volumes := make(map[string]operator.BcsVolume)
	index := 0
	for k, v := range task.VolumeMounts {
		volumes[fmt.Sprintf("vm-%d", index)] = operator.BcsVolume{
			HostDir:      k,
			ContainerDir: v,
		}
		index += 1
	}

	// init resource conditions
	if err = crmMgr.Init(tb.ID, crm.ResourceParam{
		City:     pureQueueName,
		Platform: getPlatform(queueName),
		Env: map[string]string{
			envAgentCores:      fmt.Sprintf("%0.f", task.ContainerSetting.CPUPerInstance),
			envCoordinatorIP:   task.CoordinatorIP,
			envCoordinatorPort: fmt.Sprintf("%d", task.CoordinatorPort),
			envCacheIP:         task.CacheIP,
			envCachePort:       fmt.Sprintf("%d", task.CachePort),
		},
		Ports: map[string]string{
			portAgent:         "http",
			portFileAgent:     "http",
			portGdtCS:         "http",
			portLogDownLoader: "http",
			portExporter:      "http",
			portTaskDebug:     "http",
		},
		Volumes: volumes,
		Image:   task.ContainerSetting.Image,
	}); err != nil && err != crm.ErrorResourceAlreadyInit {
		blog.Errorf("engine(%s) try launching crm task(%s), init resource manager failed: %v",
			EngineName, tb.ID, err)
		return err
	}

	err = crmMgr.Launch(tb.ID, pureQueueName, func(availableInstance int) (int, error) {
		if availableInstance < task.ContainerSetting.LeastInstance {
			return 0, engine.ErrorNoEnoughResources
		}

		if availableInstance > task.ContainerSetting.RequestInstance {
			availableInstance = task.ContainerSetting.RequestInstance
		}

		task.ContainerSetting.Instance = availableInstance
		return availableInstance, nil
	})
	// add task into public queue
	if err == engine.ErrorNoEnoughResources {
		return err
	}
	if err != nil {
		blog.Errorf("engine(%s) launch crm task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	if err = ae.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try launching crm task, update task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}
	blog.Infof("engine(%s) success to launch crm task(%s)", EngineName, tb.ID)
	return nil
}

func (ae *apisEngine) releaseTask(taskID string) error {
	task, err := ae.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try release task, get task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	if matchDirectResource(task.ContainerSetting.QueueName) {
		return ae.releaseDirectTask(task)
	}

	return ae.releaseCRMTask(task)
}

func (ae *apisEngine) releaseDirectTask(task *apisTask) error {
	if ae.directMgr == nil {
		return fmt.Errorf("engine(%s) direct mgr not init", EngineName)
	}

	blog.Infof("engine(%s) try to release direct task(%s)", EngineName, task.TaskID)
	task, err := ae.getTask(task.TaskID)
	if err != nil {
		blog.Errorf("engine(%s) try releasing direct task, get task(%s) failed: %v",
			EngineName, task.TaskID, err)
		return err
	}

	if err = ae.directMgr.ReleaseResource(task.ResourceID); err != nil {
		blog.Errorf("engine(%s) try releasing task, release direct task(%s) failed: %v",
			EngineName, task.TaskID, err)
		return err
	}

	blog.Infof("engine(%s) success to release direct task(%s)", EngineName, task.TaskID)
	return nil
}

func (ae *apisEngine) releaseCRMTask(task *apisTask) error {
	crmMgr := ae.getCrMgr(task.ContainerSetting.QueueName)
	if crmMgr == nil {
		return fmt.Errorf("engine(%s) container mgr not init", EngineName)
	}

	blog.Infof("engine(%s) try to release crm task(%s)", EngineName, task.TaskID)
	if err := crmMgr.Release(task.TaskID); err != nil && err != crm.ErrorResourceNoExist {
		blog.Errorf("engine(%s) try releasing crm task, release task(%s) failed: %v",
			EngineName, task.TaskID, err)
		return err
	}

	blog.Infof("engine(%s) success to release crm task(%s)", EngineName, task.TaskID)
	return nil
}

// cheack AllocateMap map ,make sure the key is the format of xx:xx:xx-xx:xx:xx and smaller time is ahead
func (de *apisEngine) parseAllocateConf() bool {
	time_fmt := regexp.MustCompile(`[0-9]+[0-9]+:+[0-9]+[0-9]+:+[0-9]+[0-9]`)

	for queue, allocater := range de.conf.QueueResourceAllocater {
		for k, v := range allocater.AllocateByTimeMap {
			mid := strings.Index(k, "-")
			if mid == -1 || !time_fmt.MatchString(k[:mid]) || !time_fmt.MatchString(k[mid+1:]) || k[:mid] > k[:mid+1] {
				blog.Errorf("wrong time format:(%s) in [%s] allocate config, expect format like 12:30:00-14:00:00,smaller time ahead", k, queue)
				return false
			}
			allocater.TimeSlot = append(allocater.TimeSlot, config.TimeSlot{
				StartTime: k[:mid],
				EndTime:   k[mid+1:],
				Value:     v,
			},
			)
		}
		de.conf.QueueResourceAllocater[queue] = allocater
	}
	return true
}

func (de *apisEngine) allocate(queueName string) float64 {
	return de.allocateByCurrentTime(queueName)
}

func (de *apisEngine) allocateByCurrentTime(queueName string) float64 {
	if allocater, ok := de.conf.QueueResourceAllocater[queueName]; !ok {
		return 1.0
	} else {
		now := time.Now().Format("15:04:05")
		for _, slot := range allocater.TimeSlot {
			if now >= slot.StartTime && now < slot.EndTime {
				return slot.Value
			}
		}
	}
	return 1.0
}

func (ae *apisEngine) createTask(tb *engine.TaskBasic, extra []byte) error {
	project, err := ae.mysql.GetProjectSetting(tb.Client.ProjectID)
	if err != nil {
		blog.Errorf("engine(%s) try creating task(%s), get project setting(%s) failed: %v",
			EngineName, tb.ID, tb.Client.ProjectID, err)
		return err
	}

	task, err := ae.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try creating task(%s), get task failed: %v", EngineName, tb.ID, err)
		return err
	}

	var ev ExtraData
	if err = codec.DecJSON(extra, &ev); err != nil {
		blog.Errorf("engine(%s) try creating task(%s), decode extra(%s) failed: %v",
			EngineName, tb.ID, string(extra), err)
		return err
	}

	// get information from extra data sent by client
	task.AgentProjectID = ev.ProjectID

	// get resources settings
	task.RequestCPU = project.RequestCPU * ae.allocate(tb.Client.QueueName)
	blog.Info("apisjob: queue:[%s] project:[%s] request cpu: [%f],actual request cpu:[%f]",
		tb.Client.QueueName,
		project.ProjectID,
		project.RequestCPU,
		task.RequestCPU)

	task.LeastCPU = project.LeastCPU

	// get agents' parameters
	task.CoordinatorIP = project.CoordinatorIP
	task.CoordinatorPort = project.CoordinatorPort
	task.AgentNoCoordinator = project.AgentNoCoordinator
	task.CacheIP = project.CacheIP
	task.CachePort = project.CachePort
	task.AgentCommandName = project.AgentCommandName
	task.AgentCommandPath = project.AgentCommandPath
	task.AgentCommandDir = project.AgentCommandDir
	task.AgentPort = project.AgentPort
	task.AgentFileServerPort = project.AgentFileServerPort
	task.AgentNTriesConnectingRPC = project.AgentNTriesConnectingRPC
	task.AgentCachePath = project.AgentCachePath
	task.UseGdt = project.UseGdt
	task.VolumeMounts = volumeMounts2Map(project.VolumeMounts)

	// get container settings if exists
	task.ContainerSetting.QueueName = tb.Client.QueueName
	task.ContainerSetting.CPUPerInstance = ae.conf.K8SCRMCPUPerInstance
	task.ContainerSetting.MemPerInstance = ae.conf.K8SCRMMemPerInstance
	task.ContainerSetting.RequestInstance = (int(task.RequestCPU) + int(ae.conf.K8SCRMCPUPerInstance) - 1) /
		int(ae.conf.K8SCRMCPUPerInstance)
	task.ContainerSetting.LeastInstance = (int(task.LeastCPU) + int(ae.conf.K8SCRMCPUPerInstance) - 1) /
		int(ae.conf.K8SCRMCPUPerInstance)
	task.ContainerSetting.Image = project.Image

	return ae.updateTask(task)
}

func (ae *apisEngine) sendTaskMessage(taskID string, extra []byte) ([]byte, error) {
	if string(extra) == "" {
		return nil, nil
	}

	var msg Message
	if err := codec.DecJSON(extra, &msg); err != nil {
		blog.Errorf("engine(%s) try sending task(%s) message, decode message(%s) failed: %v",
			EngineName, taskID, string(extra), err)
		return nil, err
	}

	switch msg.Type {
	case MessageTypeRecordStats:
		return ae.sendMessageRecordStats(taskID, msg.RecordStats)
	default:
		err := engine.ErrorUnknownMessageType
		blog.Errorf("engine(%s) try sending task(%s) message type(%s) failed: %v",
			EngineName, taskID, msg.Type, err)
		return nil, err
	}
}

func (ae *apisEngine) sendMessageRecordStats(taskID string, stats MessageRecordStats) ([]byte, error) {
	task, err := ae.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try sending task message, get task(%s) failed: %v",
			EngineName, taskID, err)
		return nil, err
	}

	task.CompleteTasks = stats.CompleteTasks
	task.FailedTasks = stats.FailedTasks
	var data []byte
	if err := codec.EncJSON(stats.Agents, &data); err != nil {
		blog.Errorf("engine(%s) try code task(%s) message failed: %v, message: %+v",
			EngineName, taskID, err, stats)
		return nil, err
	}
	task.AgentsInfo = string(data)

	if err = ae.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try sending task message, update task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	blog.Infof("engine(%s) success to sending task(%s) message", EngineName, taskID)
	return nil, nil
}

// extra data protocol, should be decode when creating task
type ExtraData struct {
	ProjectID string `json:"project_id"`
}

func (ae *apisEngine) getTask(taskID string) (*apisTask, error) {
	t, err := ae.mysql.GetTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) get task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	return tableTask2task(t), nil
}

func (ae *apisEngine) updateTask(task *apisTask) error {
	data, err := engine.GetMapExcludeTableTaskBasic(task2tableTask(task))
	if err != nil {
		blog.Errorf("engine(%s) update task(%s), get exclude map failed: %v", EngineName, task.TaskID, err)
		return err
	}

	if err = ae.mysql.UpdateTask(task.TaskID, data); err != nil {
		blog.Errorf("engine(%s) update task(%s), update failed: %v", EngineName, task.TaskID, err)
		return err
	}

	return nil
}

func (ae *apisEngine) resourceSelector(
	freeAgent []*direct.AgentResourceExternal,
	condition interface{}) ([]*direct.AgentResourceExternal, error) {
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
	r := make([]*direct.AgentResourceExternal, 0, 100)
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

func (ae *apisEngine) getPublicQueueByQueueName(queueName string) engine.StagingTaskQueue {
	key := ""

	switch getQueueNameHeader(queueName) {
	case "":
		// default to use direct windows
		key = publicQueueDirectWindows
	case queueNameHeaderK8SWin:
		key = publicQueueK8SWindows
	default:
		return nil
	}

	if _, ok := ae.publicQueueMap[key]; !ok {
		return nil
	}

	return ae.publicQueueMap[key]
}

func (ae *apisEngine) getCrMgr(queueName string) crm.HandlerWithUser {
	switch getQueueNameHeader(queueName) {
	case queueNameHeaderK8SWin:
		return ae.k8sCrmMgr
	default:
		return ae.k8sCrmMgr
	}
}

type resourceCondition struct {
	queueName string
	leastCPU  float64
	maxCPU    float64
}

// Message describe the data format from SendTaskMessage caller.
type Message struct {
	Type        MessageType        `json:"type"`
	RecordStats MessageRecordStats `json:"stats"`
}

type MessageType string

const (
	// MessageTypeRecordStats means this message is about stats info from client.
	MessageTypeRecordStats MessageType = "stats"
)

// MessageRecordStats describe the stats data from client's report
type MessageRecordStats struct {
	CompleteTasks int           `json:"complete_tasks"`
	FailedTasks   int           `json:"failed_tasks"`
	Agents        []interface{} `json:"agents"`
}

func getQueueNameHeader(queueName string) queueNameHeader {
	index := strings.Index(queueName, queueNameHeaderSymbol)
	if index < 0 {
		return ""
	}

	header := queueNameHeader(queueName[:index])
	switch header {
	case queueNameHeaderDirectWin, queueNameHeaderK8SWin:
		return header
	default:
		return ""
	}
}

func matchDirectResource(queueName string) bool {
	switch getQueueNameHeader(queueName) {
	case queueNameHeaderDirectWin:
		return true
	case queueNameHeaderK8SWin:
		return false
	default:
		return true
	}
}

func getPlatform(queueName string) string {
	switch getQueueNameHeader(queueName) {
	case queueNameHeaderDirectWin, queueNameHeaderK8SWin:
		return "windows"
	default:
		return "windows"
	}
}

func getQueueNamePure(queueName string) string {
	header := getQueueNameHeader(queueName)
	if header == "" {
		return queueName
	}

	return strings.TrimPrefix(queueName, fmt.Sprintf("%s%s", header, queueNameHeaderSymbol))
}
