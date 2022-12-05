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
	"errors"
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/compress"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
	commonMySQL "github.com/Tencent/bk-ci/src/booster/common/mysql"
	"github.com/Tencent/bk-ci/src/booster/server/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/rd"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm"
	op "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm/operator"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct"
	respack "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct"

	"github.com/jinzhu/gorm"
)

const (
	// EngineName define the engine name
	EngineName = "disttask"

	envAllow = "BK_DIST_WHITE_IP"
	envJob   = "BK_DIST_MAX_JOBS_4_WORKER"

	// for mesos, mesos will generate PORT_SERVICE_PORT env when launch docker
	portsService = "SERVICE_PORT"
	// for mesos, mesos will generate PORT_STATS_PORT env when launch docker
	portsStats = "STATS_PORT"

	// daemonStatsURL = "http://%s:%d/"
)

const (
	queueNameHeaderSymbol = "://"

	workerMacLauncherName = "start.sh"
	workerWinLauncherName = "start.bat"

	workerMacReleaserName = "kill.sh"
	workerWinReleaserName = "kill.bat"

	workerMacPath = "/Users/bcss/bk-dist-worker"
	workerWinPath = "C:\\data\\bcss\\bk-dist-worker"

	workerDirectImageEnvSep = ";"

	workerDirectServicePort = 31000
	workerDirectStatsPort   = 31001
)

type queueNameHeader string

const (
	queueNameHeaderDirectMac  queueNameHeader = "MAC"
	queueNameHeaderDirectWin  queueNameHeader = "WIN"
	queueNameHeaderK8SWin     queueNameHeader = "K8S_WIN"
	queueNameHeaderK8SDefault queueNameHeader = "K8S"
	queueNameHeaderVMMac      queueNameHeader = "VM_MAC"
)

// EngineConfig define engine config
type EngineConfig struct {
	engine.MySQLConf
	Rd                     rd.RegisterDiscover
	QueueResourceAllocater map[string]config.ResourceAllocater

	JobServerTimesToCPU float64
	QueueShareType      map[string]engine.QueueShareType

	// mesos cluster info
	CRMClusterID      string
	CRMCPUPerInstance float64
	CRMMemPerInstance float64

	// k8s cluster info
	K8SCRMClusterID      string
	K8SCRMCPUPerInstance float64
	K8SCRMMemPerInstance float64

	// k8s cluster list info
	K8SClusterList map[string]K8sClusterInfo

	// dc_mac cluster info
	VMCRMClusterID      string
	VMCRMCPUPerInstance float64
	VMCRMMemPerInstance float64

	Brokers []config.EngineDisttaskBrokerConfig
}

//K8sClusterInfo define
type K8sClusterInfo struct {
	K8SCRMClusterID      string
	K8SCRMCPUPerInstance float64
	K8SCRMMemPerInstance float64
}

const distTaskRunningLongestTime = 6 * time.Hour

var preferences = engine.Preferences{
	HeartbeatTimeoutTickTimes: 36,
}

// NewDisttaskEngine get a new distTask engine
func NewDisttaskEngine(
	conf EngineConfig,
	crmMgr, k8sCrmMgr, dcMacMgr crm.HandlerWithUser,
	k8sListCrmMgr map[string]crm.HandlerWithUser,
	directMgr direct.HandleWithUser) (engine.Engine, error) {
	m, err := NewMySQL(conf.MySQLConf)
	if err != nil {
		blog.Errorf("engine(%s) get new mysql(%+v) failed: %v", EngineName, conf.MySQLConf, err)
		return nil, err
	}

	egn := &disttaskEngine{
		conf: conf,
		publicQueueMap: map[string]engine.StagingTaskQueue{
			publicQueueDefault:    engine.NewStagingTaskQueue(),
			publicQueueK8SDefault: engine.NewStagingTaskQueue(),
			publicQueueK8SWindows: engine.NewStagingTaskQueue(),
			publicQueueDirectMac:  engine.NewStagingTaskQueue(),
		},
		mysql:         m,
		crmMgr:        crmMgr,
		k8sCrmMgr:     k8sCrmMgr,
		k8sListCrmMgr: k8sListCrmMgr,
		dcMacMgr:      dcMacMgr,
		directMgr:     directMgr,
	}
	if err = egn.initBrokers(); err != nil {
		blog.Errorf("engine(%s) init brokers failed: %v", EngineName, err)
		return nil, err
	}

	_ = egn.parseAllocateConf()

	return egn, nil
}

const (
	publicQueueDefault    = "default"
	publicQueueK8SDefault = "k8s_default"
	publicQueueK8SWindows = "k8s_windows"
	publicQueueDirectMac  = "direct_mac"
)

type disttaskEngine struct {
	conf           EngineConfig
	mysql          MySQL
	publicQueueMap map[string]engine.StagingTaskQueue

	// container resource manager
	crmMgr crm.HandlerWithUser

	// k8s container resource manager
	k8sCrmMgr crm.HandlerWithUser

	// k8s resource manager list
	k8sListCrmMgr map[string]crm.HandlerWithUser

	// dc_mac container resource manager
	dcMacMgr crm.HandlerWithUser

	// direct resource manager
	directMgr direct.HandleWithUser
}

// Name get the engine name
func (de *disttaskEngine) Name() engine.TypeName {
	return EngineName
}

// SelectFirstTaskBasic select a task from given task queue group and the request is ask from the specific queue.
// commonly select a task from the queue of the given name
// if the queue if empty, then try get task from the public queue, the task is from other busy queues.
func (de *disttaskEngine) SelectFirstTaskBasic(
	tqg *engine.TaskQueueGroup,
	queueName string) (*engine.TaskBasic, error) {
	tb, err := tqg.GetQueue(queueName).First()
	if err == engine.ErrorNoTaskInQueue {

		publicQueue := de.getPublicQueueByQueueName(queueName)
		if publicQueue == nil || !de.canTakeFromPublicQueue(queueName) {
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
func (de *disttaskEngine) CreateTaskExtension(tb *engine.TaskBasic, extra []byte) error {
	return de.createTask(tb, extra)
}

// GetTaskExtension get task extension from db
func (de *disttaskEngine) GetTaskExtension(taskID string) (engine.TaskExtension, error) {
	return de.getTask(taskID)
}

// LaunchTask try launching task from queue.
func (de *disttaskEngine) LaunchTask(tb *engine.TaskBasic, queueName string) error {
	return de.launchTask(tb, queueName)
}

// DegradeTask degrade to local compiling, keep running without any workers and set the correct client commands.
func (de *disttaskEngine) DegradeTask(taskID string) error {
	return de.degradeTask(taskID)
}

// LaunchDone check if the launch is done
func (de *disttaskEngine) LaunchDone(taskID string) (bool, error) {
	return de.launchDone(taskID)
}

// CheckTask check task when running, failed with error, such as running timeout.
func (de *disttaskEngine) CheckTask(tb *engine.TaskBasic) error {
	return de.checkTask(tb)
}

// SendProjectMessage not implement now
func (de *disttaskEngine) SendProjectMessage(projectID string, extra []byte) ([]byte, error) {
	return de.sendProjectMessage(projectID, extra)
}

// SendTaskMessage not implement now
func (de *disttaskEngine) SendTaskMessage(taskID string, extra []byte) ([]byte, error) {
	return de.sendTaskMessage(taskID, extra)
}

// CollectTaskData collect the task data and the disttask daemon stats, record the stats from server side.
func (de *disttaskEngine) CollectTaskData(tb *engine.TaskBasic) error {
	return de.collectTaskData(tb)
}

// ReleaseTask release task, shut down workers and free the resources.
func (de *disttaskEngine) ReleaseTask(taskID string) error {
	return de.releaseTask(taskID)
}

// GetPreferences return the preferences
func (de *disttaskEngine) GetPreferences() engine.Preferences {
	return preferences
}

// GetTaskBasicTable get task basic table db operator
func (de *disttaskEngine) GetTaskBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(TableTask{}.TableName())
}

// GetProjectBasicTable get project basic table db operator
func (de *disttaskEngine) GetProjectBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(TableProjectSetting{}.TableName())
}

// GetProjectInfoBasicTable get project info basic table db operator
func (de *disttaskEngine) GetProjectInfoBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(TableProjectInfo{}.TableName())
}

// GetWhitelistBasicTable get whitelist basic table db operator
func (de *disttaskEngine) GetWhitelistBasicTable() *gorm.DB {
	return de.mysql.GetDB().Table(TableWhitelist{}.TableName())
}

func (de *disttaskEngine) getClient(timeoutSecond int) *httpclient.HTTPClient {
	client := httpclient.NewHTTPClient()
	client.SetTimeOut(time.Duration(timeoutSecond) * time.Second)
	return client
}

// cheack AllocateMap map ,make sure the key is the format of xx:xx:xx-xx:xx:xx and smaller time is ahead
func (de *disttaskEngine) parseAllocateConf() bool {
	timeFmt := regexp.MustCompile(`[0-9]+[0-9]+:+[0-9]+[0-9]+:+[0-9]+[0-9]`)

	for queue, allocater := range de.conf.QueueResourceAllocater {
		for k, v := range allocater.AllocateByTimeMap {
			mid := strings.Index(k, "-")
			if mid == -1 || !timeFmt.MatchString(k[:mid]) || !timeFmt.MatchString(k[mid+1:]) || k[:mid] > k[:mid+1] {
				blog.Errorf("wrong time format:(%s) in [%s] allocate config,expect format 12:30:00-14:00:00,smaller time ahead",
					k, queue)
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

func (de *disttaskEngine) allocate(queueName string) float64 {
	return de.allocateByCurrentTime(queueName)
}

func (de *disttaskEngine) allocateByCurrentTime(queueName string) float64 {
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

func (de *disttaskEngine) createTask(tb *engine.TaskBasic, extra []byte) error {
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

	worker, err := de.mysql.GetWorker(project.WorkerVersion, project.Scene)
	if err != nil {
		blog.Errorf("engine(%s) try create task(%s), get worker(%s %s) failed: %v",
			EngineName, tb.ID, project.WorkerVersion, project.Scene, err)
		return err
	}

	task, err := de.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try creating task(%s), get task failed: %v", EngineName, tb.ID, err)
		return err
	}

	// client
	task.Client.SourceIP = tb.Client.ClientIP
	task.Client.SourceCPU = tb.Client.ClientCPU
	task.Client.User = ev.User
	task.Client.Params = ev.Params
	task.Client.Cmd = ev.Cmd
	task.Client.RunDir = ev.RunDir
	task.Client.BoosterType = ev.BoosterType
	var extraclientdata []byte
	_ = codec.EncJSON(ev.ExtraVars, &extraclientdata)
	task.Client.ExtraClientSetting = string(extraclientdata)

	// project
	task.InheritSetting.BanAllBooster = project.BanAllBooster
	// if ban resources, then request and least cpu is 0
	if !task.InheritSetting.BanAllBooster {
		task.InheritSetting.RequestCPU = project.RequestCPU * de.allocate(tb.Client.QueueName)
		blog.Info("disttask: queue:[%s] project:[%s] request cpu: [%f],actual request cpu:[%f]",
			tb.Client.QueueName,
			project.ProjectID,
			project.RequestCPU,
			task.InheritSetting.RequestCPU)

		task.InheritSetting.LeastCPU = project.LeastCPU
		if task.InheritSetting.LeastCPU > task.InheritSetting.RequestCPU {
			task.InheritSetting.LeastCPU = task.InheritSetting.RequestCPU
		}
	}

	crmMgr := de.getCrMgr(tb.Client.QueueName)
	if crmMgr == nil {
		blog.Errorf("engine(%s) try creating task(%s) failed: crmMgr is null", EngineName, task.ID)
		return errors.New("crmMgr is null")
	}

	task.InheritSetting.WorkerVersion = worker.WorkerVersion
	task.InheritSetting.Scene = project.Scene
	task.InheritSetting.ExtraProjectSetting = project.Extra
	task.InheritSetting.ExtraWorkerSetting = worker.Extra

	task.Operator.AppName = task.ID
	task.Operator.Namespace = crmMgr.GetNamespace()
	task.Operator.Image = worker.Image
	de.setTaskIstResource(task, tb.Client.QueueName)
	task.Operator.RequestProcessPerUnit = ev.ProcessPerUnit

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try creating task, update task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}
	blog.Infof("engine(%s) success to create task(%s)", EngineName, tb.ID)
	return nil
}

func (de *disttaskEngine) setTaskIstResource(task *distTask, queueName string) {
	ist := de.getCrMgr(queueName).GetInstanceType(getPlatform(queueName), getQueueNamePure(queueName))
	cpuPerInstance := ist.CPUPerInstance
	memPerInstance := ist.MemPerInstance
	task.Operator.ClusterID = de.getClusterID(queueName)
	// if ban resources, then request and least instance is 0
	if !task.InheritSetting.BanAllBooster {
		task.Operator.RequestInstance = (int(task.InheritSetting.RequestCPU) + int(cpuPerInstance) - 1) /
			int(cpuPerInstance)
		task.Operator.LeastInstance = (int(task.InheritSetting.LeastCPU) + int(cpuPerInstance) - 1) /
			int(cpuPerInstance)
	}

	task.Operator.RequestCPUPerUnit = cpuPerInstance
	task.Operator.RequestMemPerUnit = memPerInstance
}

func (de *disttaskEngine) getClusterID(queueName string) string {
	switch getQueueNameHeader(queueName) {
	case queueNameHeaderK8SDefault, queueNameHeaderK8SWin:
		for qNames, cluster := range de.conf.K8SClusterList {
			for _, qName := range strings.Split(qNames, ",") {
				if queueName == strings.TrimSpace(qName) {
					return cluster.K8SCRMClusterID
				}
			}
		}
		return de.conf.K8SCRMClusterID
	case queueNameHeaderVMMac:
		return de.conf.VMCRMClusterID
	default:
		return de.conf.CRMClusterID
	}
}

// ExtraData describe the data in task creation from client.
type ExtraData struct {
	User           string `json:"user"`
	RunDir         string `json:"run_dir"`
	Params         string `json:"params"` //自定义参数
	Cmd            string `json:"cmd"`
	ProcessPerUnit int    `json:"process_per_unit"`

	// command define the target to be called, such as make, bazel, /data/custom/make etc.
	BoosterType string `json:"BoosterType,omitempty"`

	// extra_vars includes the extra params need by client
	ExtraVars taskClientExtra `json:"extra_vars,omitempty"`
}

func (de *disttaskEngine) getTask(taskID string) (*distTask, error) {
	t, err := de.mysql.GetTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) get task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	return table2Task(t), nil
}

func (de *disttaskEngine) updateTask(task *distTask) error {
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

func (de *disttaskEngine) launchTask(tb *engine.TaskBasic, queueName string) error {
	task, err := de.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try launching task, get task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	if task.InheritSetting.BanAllBooster {
		blog.Infof("engine(%s) launch task(%s) no use resource for banning", EngineName, tb.ID)
		return nil
	}

	if matchDirectResource(queueName) {
		return de.launchDirectTask(task, tb, queueName)
	}

	return de.launchCRMTask(task, tb, queueName)
}

func (de *disttaskEngine) launchDirectTask(task *distTask, tb *engine.TaskBasic, queueName string) error {
	condition := &resourceCondition{
		queueName: getQueueNamePure(queueName),
		leastCPU:  task.InheritSetting.LeastCPU,
		maxCPU:    task.InheritSetting.RequestCPU,
	}

	_, err := de.directMgr.GetFreeResource(tb.ID, condition, resourceSelector, nil)
	// add task into public queue
	if err == engine.ErrorNoEnoughResources {
		if publicQueue := de.getPublicQueueByQueueName(queueName); publicQueue != nil &&
			de.canGiveToPublicQueue(queueName) {
			publicQueue.Add(tb)
		}
		return err
	}
	if err != nil {
		blog.Errorf("engine(%s) try consuming direct resource, get free resource(%+v) failed: %v",
			EngineName, condition, err)
		return err
	}

	blog.Infof("engine(%s) success to consume resource for direct task(%s)", EngineName, tb.ID)
	workerEnv := make(map[string]string)
	for _, item := range strings.Split(task.Operator.Image, workerDirectImageEnvSep) {
		e := strings.Split(item, "=")
		if len(e) != 2 {
			continue
		}

		workerEnv[e[0]] = e[1]
	}
	workerEnv[env.GetEnvKey(env.KeyWorkerPort)] = fmt.Sprintf("%d", workerDirectServicePort)
	workerEnv[env.GetEnvKey(env.KeyWorkerWhiteIP)] = strings.Join(
		append(de.getServerIPList(), task.Client.SourceIP), " ")

	resources, err := de.directMgr.ListResource(task.ID)
	if err != nil {
		blog.Errorf("engine(%s) try launching direct task(%s), list resource(%s) failed: %v",
			EngineName, task.ID, task.ID, err)
		return err
	}

	for _, r := range resources {
		e := copyEnv(workerEnv)

		jobsInt := int(r.Resource.CPU)
		if task.Operator.RequestProcessPerUnit > 0 {
			jobsInt = task.Operator.RequestProcessPerUnit
		}
		e[env.GetEnvKey(env.KeyWorkerMaxProcess)] = fmt.Sprintf("%d", jobsInt)
		e[env.GetEnvKey(env.KeyWorkerMaxJobs)] = fmt.Sprintf("%d", jobsInt)
		if err = de.directMgr.ExecuteCommand(r.Base.IP, task.ID, &respack.Command{
			Cmd:          getDirectLaunchCommand(queueName),
			CmdType:      respack.CmdLaunch,
			Env:          e,
			UserDefineID: task.ID,
			Dir:          getDirectPath(task.InheritSetting.QueueName),
			Path:         getDirectPath(task.InheritSetting.QueueName),
		}); err != nil {
			blog.Errorf("engine(%s) try launching direct task(%s), execute command on(%s) failed: %v",
				EngineName, tb.ID, r.Base.IP, err)
			// ++ commented by tomtian 20210401, do not return to avoid retry
			// return err
			// --
		}
	}

	blog.Infof("engine(%s) success to launch direct task(%s)", EngineName, tb.ID)
	return nil
}

func (de *disttaskEngine) launchCRMTask(task *distTask, tb *engine.TaskBasic, queueName string) error {
	crmMgr := de.getCrMgr(task.InheritSetting.QueueName)
	if crmMgr == nil {
		blog.Errorf("engine(%s) try launching crm task(%s) failed: crmMgr is null", EngineName, tb.ID)
		return errors.New("crmMgr is null")
	}
	var err error

	envJobInt := int(task.Operator.RequestCPUPerUnit)
	if task.Operator.RequestProcessPerUnit > 0 {
		envJobInt = task.Operator.RequestProcessPerUnit
	}

	pureQueueName := getQueueNamePure(queueName)

	// handle mount settings
	var ms taskMountsSettings
	if err = codec.DecJSON([]byte(task.InheritSetting.ExtraProjectSetting), &ms); err != nil {
		blog.Warnf("engine(%s) try decode mount settings from task(%s) failed: %v", EngineName, tb.ID, err)
	}
	volumes := make(map[string]op.BcsVolume)
	for index, v := range ms.Mounts {
		volumes[fmt.Sprintf("vm-%d", index)] = op.BcsVolume{
			HostDir:      v.HostDir,
			ContainerDir: v.ContainerDir,
		}
	}

	// init resource conditions
	if err = crmMgr.Init(tb.ID, crm.ResourceParam{
		City:     pureQueueName,
		Platform: getPlatform(queueName),
		Env: map[string]string{
			envAllow: strings.Join(append(de.getServerIPList(), task.Client.SourceIP), " "),
			envJob:   strconv.Itoa(envJobInt),
		},
		Ports: map[string]string{
			portsService: "http",
			portsStats:   "http",
		},
		Image:   task.Operator.Image,
		Volumes: volumes,
	}); err != nil && err != crm.ErrorResourceAlreadyInit {
		blog.Errorf("engine(%s) try launching crm task(%s), init resource manager failed: %v",
			EngineName, tb.ID, err)
		return err
	}

	de.setTaskIstResource(task, queueName)

	err = crmMgr.Launch(tb.ID, pureQueueName, func(availableInstance int) (int, error) {
		if availableInstance < task.Operator.LeastInstance {
			return 0, engine.ErrorNoEnoughResources
		}

		if availableInstance > task.Operator.RequestInstance {
			availableInstance = task.Operator.RequestInstance
		}

		task.Operator.Instance = availableInstance
		return availableInstance, nil
	})
	// add task into public queue
	if err == engine.ErrorNoEnoughResources {
		if publicQueue := de.getPublicQueueByQueueName(queueName); publicQueue != nil &&
			de.canGiveToPublicQueue(queueName) {
			publicQueue.Add(tb)
		}
		return err
	}
	if err != nil {
		blog.Errorf("engine(%s) launch crm task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try launching crm task, update task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}
	blog.Infof("engine(%s) success to launch crm task(%s)", EngineName, tb.ID)
	return nil
}

func (de *disttaskEngine) degradeTask(taskID string) error {
	blog.Infof("engine(%s) success to degrade task(%s)", EngineName, taskID)
	return nil
}

func (de *disttaskEngine) launchDone(taskID string) (bool, error) {
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try checking service info, get task(%s) failed: %v", EngineName, taskID, err)
		return false, err
	}

	if task.InheritSetting.BanAllBooster {
		blog.Infof("engine(%s) check launch done task(%s) success immediately for banning", EngineName, taskID)
		return true, nil
	}

	if matchDirectResource(task.InheritSetting.QueueName) {
		return de.launchDirectDone(task)
	}

	return de.launchCRMDone(task)
}

func (de *disttaskEngine) launchDirectDone(task *distTask) (bool, error) {
	infoList, err := de.directMgr.ListCommands(task.ID)
	if err != nil {
		blog.Errorf("engine(%s) try checking if task(%s) launch direct done, list commands failed: %v",
			EngineName, task.ID, err)
		return false, err
	}

	workerList := make([]taskWorker, 0, 100)
	for _, info := range infoList {
		switch info.Status {
		case respack.CommandStatusInit:
			continue
		case respack.CommandStatusSucceed:
			workerList = append(workerList, taskWorker{
				CPU:       0,
				Mem:       0,
				IP:        info.IP,
				Port:      workerDirectServicePort,
				StatsPort: workerDirectStatsPort,
			})
		}
	}

	// get resource info from resource list
	resourceList, err := de.directMgr.ListResource(task.ID)
	if err != nil {
		blog.Error("engine(%s) try list resources of task(%s) failed: %v", EngineName, task.ID, err)
		return false, err
	}

	var cpuTotal, memTotal float64 = 0, 0
	for _, r := range resourceList {
		for i := range workerList {
			if r.Base.IP == workerList[i].IP {
				workerList[i].CPU = r.Resource.CPU
				workerList[i].Mem = r.Resource.Mem
				break
			}
		}
		cpuTotal += r.Resource.CPU
		memTotal += r.Resource.Mem
	}

	task.Workers = workerList
	task.Stats.WorkerCount = len(task.Workers)

	task.Stats.CPUTotal = cpuTotal
	task.Stats.MemTotal = memTotal

	blog.Infof("task(%s) now has workers(%d),CPU(%f),Mem(%f)", task.ID, task.Stats.WorkerCount, cpuTotal, memTotal)
	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try checking service info, update direct task(%s) failed: %v",
			EngineName, task.ID, err)
		return false, err
	}

	for _, info := range infoList {
		if info.Status == respack.CommandStatusInit {
			return false, nil
		}
	}

	blog.Infof("engine(%s) success to checking service info, task(%s) direct launching done",
		EngineName, task.ID)
	return true, nil
}

func (de *disttaskEngine) launchCRMDone(task *distTask) (bool, error) {
	crmMgr := de.getCrMgr(task.InheritSetting.QueueName)
	if crmMgr == nil {
		blog.Errorf("engine(%s) try launch crm task(%s) done failed: crmMgr is null", EngineName, task.ID)
		return false, errors.New("crmMgr is null")
	}
	// if still preparing, then it's not need to get service info
	isPreparing, err := crmMgr.IsServicePreparing(task.ID)
	if err != nil {
		blog.Errorf("engine(%s) try checking service info, check if crm service preparing(%s) failed: %v",
			EngineName, task.ID, err)
		return false, err
	}
	if isPreparing {
		return false, nil
	}

	info, err := crmMgr.GetServiceInfo(task.ID)
	if err != nil {
		blog.Errorf("engine(%s) try checking service info, get crm info(%s) failed: %v",
			EngineName, task.ID, err)
		return false, err
	}

	workerList := make([]taskWorker, 0, 100)
	for _, endpoints := range info.AvailableEndpoints {
		servicePort, _ := endpoints.Ports[portsService]
		statsPort, _ := endpoints.Ports[portsStats]

		workerList = append(workerList, taskWorker{
			CPU:       task.Operator.RequestCPUPerUnit,
			Mem:       task.Operator.RequestMemPerUnit,
			IP:        endpoints.IP,
			Port:      servicePort,
			StatsPort: statsPort,
		})
	}

	task.Workers = workerList
	task.Stats.WorkerCount = len(task.Workers)
	task.Stats.CPUTotal = float64(task.Stats.WorkerCount) * task.Operator.RequestCPUPerUnit
	task.Stats.MemTotal = float64(task.Stats.WorkerCount) * task.Operator.RequestMemPerUnit

	blog.Infof("task(%s) has current cpu(%f), current workers(%d)", task.ID, task.Stats.CPUTotal, task.Stats.WorkerCount)
	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try checking service info, update crm task(%s) failed: %v",
			EngineName, task.ID, err)
		return false, err
	}

	if info.Status == crm.ServiceStatusStaging {
		return false, nil
	}

	blog.Infof("engine(%s) success to checking service info, task(%s) crm launching done", EngineName, task.ID)
	return true, nil
}

func (de *disttaskEngine) collectTaskData(tb *engine.TaskBasic) error {
	task, err := de.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try collecting task data, get task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	opts := commonMySQL.NewListOptions()
	opts.Equal("task_id", tb.ID)
	opts.Limit(1)
	l, _, err := de.mysql.ListWorkStats(opts)
	if err != nil {
		blog.Errorf("engine(%s) try collecting task data for project(%s) taskID(%s), get work stats failed: %v",
			EngineName, tb.Client.ProjectID, tb.ID, err)
		return err
	}
	stats := DeltaInfoStats{}
	for _, work := range l {
		task.Stats.SucceedNum += int64(work.JobRemoteOK)
		task.Stats.FailedNum += int64(work.JobRemoteError)
		stats.CompileFilesOK += int64(work.JobRemoteOK)
		stats.CompileFilesErr += int64(work.JobRemoteError)
		stats.CompileUnits += float64(tb.Status.EndTime.Unix()-tb.Status.StartTime.Unix()) * task.Stats.CPUTotal
	}

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try collecting task data, update task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	if err = de.mysql.AddProjectInfoStats(tb.Client.ProjectID, stats); err != nil {
		blog.Errorf("engine(%s) try collecting task(%s) data, add project(%s) info stats failed: %v",
			EngineName, tb.ID, tb.Client.ProjectID, err)
		return err
	}

	blog.Infof("engine(%s) success to collect task(%s) data", EngineName, tb.ID)
	return nil
}

func (de *disttaskEngine) releaseTask(taskID string) error {
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try release task, get task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	if task.InheritSetting.BanAllBooster {
		blog.Infof("engine(%s) release task(%s) success immediately for banning", EngineName, taskID)
		return nil
	}

	if matchDirectResource(task.InheritSetting.QueueName) {
		return de.releaseDirectTask(task)
	}

	return de.releaseCRMTask(task)
}

func (de *disttaskEngine) releaseDirectTask(task *distTask) error {
	blog.Infof("engine(%s) try to release direct task(%s)", EngineName, task.ID)

	resources, err := de.directMgr.ListResource(task.ID)
	if err != nil {
		blog.Errorf("engine(%s) try to release direct task(%s), list resource(%s) failed: %v",
			EngineName, task.ID, task.ID, err)
		return err
	}

	for _, r := range resources {
		_ = de.directMgr.ExecuteCommand(r.Base.IP, task.ID, &respack.Command{
			Cmd:          getDirectReleaseCommand(task.InheritSetting.QueueName),
			CmdType:      respack.CmdRelease,
			UserDefineID: task.ID,
			Dir:          getDirectPath(task.InheritSetting.QueueName),
			Path:         getDirectPath(task.InheritSetting.QueueName),
		})
	}

	if err := de.directMgr.ReleaseResource(task.ID); err != nil {
		blog.Errorf("engine(%s) try releasing direct task, release task(%s) failed: %v",
			EngineName, task.ID, err)
		return err
	}

	blog.Infof("engine(%s) success to release direct task(%s)", EngineName, task.ID)
	return nil
}

func (de *disttaskEngine) releaseCRMTask(task *distTask) error {
	crmMgr := de.getCrMgr(task.InheritSetting.QueueName)
	if crmMgr == nil {
		blog.Errorf("engine(%s) try releasing crm task, release task(%s) failed: crmMgr is null", EngineName, task.ID)
		return errors.New("crmMgr is null")
	}
	blog.Infof("engine(%s) try to release crm task(%s)", EngineName, task.ID)
	if err := crmMgr.Release(task.ID); err != nil && err != crm.ErrorResourceNoExist {
		blog.Errorf("engine(%s) try releasing crm task, release task(%s) failed: %v", EngineName, task.ID, err)
		return err
	}

	blog.Infof("engine(%s) success to release crm task(%s)", EngineName, task.ID)
	return nil
}

func (de *disttaskEngine) checkTask(tb *engine.TaskBasic) error {
	if tb.Status.Status == engine.TaskStatusRunning &&
		tb.Status.StartTime.Add(distTaskRunningLongestTime).Before(time.Now().Local()) {
		return fmt.Errorf("disttask running since(%s) reaches the timeout(%s)",
			tb.Status.StartTime.String(), distTaskRunningLongestTime.String())
	}

	return nil
}

func (de *disttaskEngine) getServerIPList() []string {
	r := make([]string, 0, 100)
	l, _ := de.conf.Rd.GetServers()
	for _, i := range l {
		r = append(r, i.IP)
	}
	return r
}

func (de *disttaskEngine) initBrokers() error {
	for _, broker := range de.conf.Brokers {
		brokerName := brokerName(broker)

		worker, err := de.mysql.GetWorker(broker.WorkerVersion, broker.Scene)
		if err != nil {
			blog.Errorf("engine(%s) init broker(%s) get worker(%s) failed: %v",
				EngineName, brokerName, broker.WorkerVersion, err)
			return err
		}

		volumes := make(map[string]op.BcsVolume)
		for index, v := range broker.Volumes {
			volumes[fmt.Sprintf("vm-%d", index)] = op.BcsVolume{
				HostDir:      v.HostDir,
				ContainerDir: v.ContainerDir,
			}
		}
		crmMgr := de.getCrMgr(broker.City)
		if crmMgr == nil {
			blog.Errorf("engine(%s) init broker(%s) failed: crmMgr is null", EngineName, brokerName)
			return errors.New("crmMgr is null")
		}
		if err = crmMgr.AddBroker(
			brokerName, crm.StrategyConst, crm.NewConstBrokerStrategy(broker.ConstNum), crm.BrokerParam{
				Param: crm.ResourceParam{
					City:     getQueueNamePure(broker.City),
					Platform: getPlatform(broker.City),
					Env: map[string]string{
						envAllow: broker.Allow,
						envJob:   fmt.Sprintf("%d", broker.JobPerInstance),
					},
					Ports: map[string]string{
						portsService: "http",
						portsStats:   "http",
					},
					Image:      worker.Image,
					BrokerName: brokerName,
					Volumes:    volumes,
				},
				Instance: broker.Instance,
				FitFunc: func(brokerParam, requestParam crm.ResourceParam) bool {
					return brokerParam.City == requestParam.City && brokerParam.Image == requestParam.Image
				},
				IdleKeepSeconds: broker.IdleKeepSeconds,
				ReleaseLoop:     broker.ReleaseLoop,
			}); err != nil {
			blog.Errorf("engine(%s) init broker(%s) add broker failed: %v", EngineName, brokerName, err)
			return err
		}

		blog.Infof("engine(%s) success to init broker(%s)", EngineName, brokerName)
	}

	return nil
}

func (de *disttaskEngine) getCrMgr(queueName string) crm.HandlerWithUser {
	switch getQueueNameHeader(queueName) {
	case queueNameHeaderK8SDefault, queueNameHeaderK8SWin:
		for qNames, mgr := range de.k8sListCrmMgr {
			for _, qName := range strings.Split(qNames, ",") {
				if queueName == strings.TrimSpace(qName) {
					return mgr
				}
			}
		}
		return de.k8sCrmMgr //default k8s cluster
	case queueNameHeaderVMMac:
		return de.dcMacMgr
	default:
		return de.crmMgr
	}
}

func (de *disttaskEngine) canTakeFromPublicQueue(queueName string) bool {
	if de.conf.QueueShareType == nil {
		return true
	}

	t, ok := de.conf.QueueShareType[queueName]
	if !ok {
		return true
	}

	switch t {
	case engine.QueueShareTypeAllAllowed, engine.QueueShareTypeOnlyTakeFromPublic:
		return true
	default:
		return false
	}
}

func (de *disttaskEngine) canGiveToPublicQueue(queueName string) bool {
	if de.conf.QueueShareType == nil {
		return true
	}

	t, ok := de.conf.QueueShareType[queueName]
	if !ok {
		return true
	}

	switch t {
	case engine.QueueShareTypeAllAllowed, engine.QueueShareTypeOnlyGiveToPublic:
		return true
	default:
		return false
	}
}

// Message describe the data format from SendMessage caller.
type Message struct {
	Type MessageType `json:"type"`

	// typo "task_tats", should be "task_stats", but for old version protocol usage, just keep it.
	MessageTaskStats   MessageTaskStats   `json:"task_tats"`
	MessageRecordStats MessageRecordStats `json:"ccache_stats"`
}

//MessageType define
type MessageType int

const (
	// MessageTypeTaskStats means this message is about record task stats from client.
	MessageTypeTaskStats MessageType = iota
	//MessageTypeRecordStats means
	MessageTypeRecordStats
)

// MessageTaskStats describe the message of uploading task stats
type MessageTaskStats struct {
	WorkID           string `json:"work_id"`
	TaskID           string `json:"task_id"`
	Scene            string `json:"scene"`
	Success          bool   `json:"success"`
	JobRemoteOK      int    `json:"job_remote_ok"`
	JobRemoteError   int    `json:"job_remote_error"`
	JobLocalOK       int    `json:"job_local_ok"`
	JobLocalError    int    `json:"job_local_error"`
	StartTime        int64  `json:"start_time"`
	EndTime          int64  `json:"end_time"`
	RegisteredTime   int64  `json:"registered_time"`
	UnregisteredTime int64  `json:"unregistered_time"`
	Jobs             string `json:"jobs"`
}

func (de *disttaskEngine) sendTaskMessage(taskID string, message []byte) ([]byte, error) {
	// if message empty, just ignore it
	if len(message) == 0 {
		return nil, nil
	}

	var msg Message
	if err := codec.DecJSON(message, &msg); err != nil {
		blog.Errorf("engine(%s) try sending task(%s) message, decode message(%s) failed: %v",
			EngineName, taskID, string(message), err)
		return nil, err
	}

	switch msg.Type {
	case MessageTypeRecordStats:
		return de.sendMessageRecordStats(taskID, msg.MessageRecordStats)
	default:
		err := engine.ErrorUnknownMessageType
		blog.Warnf("engine(%s) try sending task(%s) message type(%d) failed: %v",
			EngineName, taskID, msg.Type, err)
		return nil, err
	}
}

func (de *disttaskEngine) sendMessageRecordStats(taskID string, stats MessageRecordStats) ([]byte, error) {
	task, err := de.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try sending task message, get task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	task.Stats.ExtraRecord = string(stats.Dump())

	if err = de.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try sending task message, update task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	blog.Infof("engine(%s) success to sending task(%s) message", EngineName, taskID)
	return nil, nil
}

func (de *disttaskEngine) sendProjectMessage(projectID string, extra []byte) ([]byte, error) {
	var msg Message
	if err := codec.DecJSON(extra, &msg); err != nil {
		blog.Errorf("engine(%s) try sending project(%s) message, decode message(%s) failed: %v",
			EngineName, projectID, string(extra), err)
		return nil, err
	}

	switch msg.Type {
	case MessageTypeTaskStats:
		return de.sendMessageTaskStats(projectID, msg.MessageTaskStats)
	default:
		err := engine.ErrorUnknownMessageType
		blog.Errorf("engine(%s) try sending project(%s) message type(%d) failed: %v",
			EngineName, projectID, msg.Type, err)
		return nil, err
	}
}

//EmptyJobs define
var EmptyJobs = compress.ToBase64String([]byte("[]"))

func (de *disttaskEngine) sendMessageTaskStats(projectID string, stats MessageTaskStats) ([]byte, error) {
	opts := commonMySQL.NewListOptions()
	opts.Equal("task_id", stats.TaskID)
	opts.Equal("work_id", stats.WorkID)
	opts.Limit(1)
	l, _, err := de.mysql.ListWorkStats(opts)
	if err != nil {
		blog.Errorf("engine(%s) try send message task stats for project(%s) taskID(%s) workID(%s), "+
			"get work stats failed: %v", EngineName, projectID, stats.TaskID, stats.WorkID, err)
		return nil, err
	}

	data := &TableWorkStats{
		ProjectID:        projectID,
		TaskID:           stats.TaskID,
		WorkID:           stats.WorkID,
		Scene:            stats.Scene,
		Success:          stats.Success,
		JobRemoteOK:      stats.JobRemoteOK,
		JobRemoteError:   stats.JobRemoteError,
		JobLocalOK:       stats.JobLocalOK,
		JobLocalError:    stats.JobLocalError,
		StartTime:        stats.StartTime,
		EndTime:          stats.EndTime,
		RegisteredTime:   stats.RegisteredTime,
		UnregisteredTime: stats.UnregisteredTime,
		JobStats:         stats.Jobs,
	}

	// if work stats exists, just overwrite it.
	if len(l) != 0 {
		data.ID = l[0].ID
		if l[0].JobStats != EmptyJobs {
			blog.Infof("engine(%s) try send message task stats for project(%s) taskID(%s) workID(%s), "+
				"but the job stats already set, skip put", EngineName, projectID, stats.TaskID, stats.WorkID)
			return nil, nil
		}
	}

	if err = de.mysql.PutWorkStats(data); err != nil {
		blog.Errorf("engine(%s) try send message task stats for project(%s) taskID(%s) workID(%s), "+
			"put work stats failed: %v", EngineName, projectID, stats.TaskID, stats.WorkID, err)
		return nil, err
	}
	blog.Infof("engine(%s) success to send message task stats for project(%s) taskID(%s) workID(%s)",
		EngineName, projectID, stats.TaskID, stats.WorkID)
	return nil, nil
}

func (de *disttaskEngine) getPublicQueueByQueueName(queueName string) engine.StagingTaskQueue {
	key := ""

	switch getQueueNameHeader(queueName) {
	case "":
		key = publicQueueDefault
	case queueNameHeaderK8SDefault:
		key = publicQueueK8SDefault
	case queueNameHeaderK8SWin:
		key = publicQueueK8SWindows
	case queueNameHeaderDirectMac:
		key = publicQueueDirectMac
	default:
		return nil
	}

	if _, ok := de.publicQueueMap[key]; !ok {
		return nil
	}

	return de.publicQueueMap[key]
}

type resourceCondition struct {
	queueName string
	leastCPU  float64
	maxCPU    float64
}

func brokerName(conf config.EngineDisttaskBrokerConfig) string {
	header := ""
	switch getQueueNameHeader(conf.City) {
	case queueNameHeaderK8SDefault:
		header = "k-"
	case queueNameHeaderK8SWin:
		header = "kw-"
	case queueNameHeaderVMMac:
		header = "vm-"
	}

	return strings.ReplaceAll(
		fmt.Sprintf("%s%s-%s", header, getQueueNamePure(conf.City), conf.WorkerVersion), ".", "-")
}

func getPlatform(queueName string) string {
	switch getQueueNameHeader(queueName) {
	case queueNameHeaderDirectWin, queueNameHeaderK8SWin:
		return "windows"
	case queueNameHeaderDirectMac, queueNameHeaderVMMac:
		return "darwin"
	default:
		return "linux"
	}
}

func matchDirectResource(queueName string) bool {
	switch getQueueNameHeader(queueName) {
	case queueNameHeaderDirectWin, queueNameHeaderDirectMac:
		return true
	default:
		return false
	}
}

func getDirectLaunchCommand(queueName string) string {
	switch getQueueNameHeader(queueName) {
	case queueNameHeaderDirectWin:
		return workerWinLauncherName
	case queueNameHeaderDirectMac:
		return workerMacLauncherName
	default:
		return ""
	}
}

func getDirectReleaseCommand(queueName string) string {
	switch getQueueNameHeader(queueName) {
	case queueNameHeaderDirectWin:
		return workerWinReleaserName
	case queueNameHeaderDirectMac:
		return workerMacReleaserName
	default:
		return ""
	}
}

func getDirectPath(queueName string) string {
	switch getQueueNameHeader(queueName) {
	case queueNameHeaderDirectWin:
		return workerWinPath
	case queueNameHeaderDirectMac:
		return workerMacPath
	default:
		return ""
	}
}

func getQueueNamePure(queueName string) string {
	header := getQueueNameHeader(queueName)
	if header == "" {
		return queueName
	}

	return strings.TrimPrefix(queueName, fmt.Sprintf("%s%s", header, queueNameHeaderSymbol))
}

func getQueueNameHeader(queueName string) queueNameHeader {
	index := strings.Index(queueName, queueNameHeaderSymbol)
	if index < 0 {
		return ""
	}

	header := queueNameHeader(queueName[:index])
	switch header {
	case queueNameHeaderDirectMac, queueNameHeaderDirectWin, queueNameHeaderK8SWin,
		queueNameHeaderK8SDefault, queueNameHeaderVMMac:
		return header
	default:
		return ""
	}
}

//GetK8sInstanceKey get instance type from queueName
func GetK8sInstanceKey(queueName string) *config.InstanceType {
	header := getQueueNameHeader(queueName)
	if header == queueNameHeaderK8SDefault || header == queueNameHeaderK8SWin {
		return &config.InstanceType{
			Group:    getQueueNamePure(queueName),
			Platform: getPlatform(queueName),
		}
	}
	return nil
}

func resourceSelector(
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
		blog.Debugf("engine(%s) try to check free agent(%s:%.2f) with cluster(%s), "+
			"current cpu(%.2f) with queue(%s)",
			EngineName, agent.Base.IP, agent.Resource.CPU, agent.Base.Cluster, cpuTotal, c.queueName)

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
		blog.Debugf("engine(%s) select free agent(%s:%.2f) with cluster(%s), current(%.2f), target(%.2f~%.2f)",
			EngineName, agent.Base.IP, agent.Resource.CPU, agent.Base.Cluster, cpuTotal, c.leastCPU, c.maxCPU)
	}

	if cpuTotal < c.leastCPU {
		return nil, engine.ErrorNoEnoughResources
	}

	return r, nil
}

func copyEnv(s map[string]string) map[string]string {
	r := make(map[string]string)
	for k, v := range s {
		r[k] = v
	}

	return r
}
