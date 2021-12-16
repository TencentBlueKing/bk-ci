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
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonMySQL "github.com/Tencent/bk-ci/src/booster/common/mysql"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	resource "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct"
	respack "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct"

	"github.com/jinzhu/gorm"
)

// const vars for fastbuild engine
const (
	// EngineName define the engine name
	EngineName = "fastbuild"

	FBCompressResultEnvKey = "FB_COMPRESS_RESULT"
	FBCacheEnableKey       = "FB_CACHE_ENABLE"

	// attrs for project
	attrProjectAllowCross = 1
	// attrs for resource cluster
	attrResourceClusterAllowCross = 1
)

// EngineConfig define config of fastbuild
type EngineConfig struct {
	engine.MySQLConf
	SpecialFBCmd                      string
	TaskMaxRunningSeconds             int32
	TaskBKMainNoSubTaskTimeoutSeconds int32
}

var preferences = engine.Preferences{
	HeartbeatTimeoutTickTimes: 30,
}

// NewFastbuildEngine return new fastbuild engine
func NewFastbuildEngine(conf EngineConfig, mgr resource.HandleWithUser) (engine.Engine, error) {
	m, err := NewMySQL(conf.MySQLConf)
	if err != nil {
		blog.Errorf("engine(%s) get new mysql(%+v) failed: %v", EngineName, conf.MySQLConf, err)
		return nil, err
	}

	return &fastbuildEngine{
		conf:        conf,
		publicQueue: engine.NewStagingTaskQueue(),
		mysql:       m,
		mgr:         mgr,
	}, nil
}

type fastbuildEngine struct {
	conf        EngineConfig
	publicQueue engine.StagingTaskQueue
	mysql       MySQL
	mgr         resource.HandleWithUser
}

// get the engine name
func (fe *fastbuildEngine) Name() engine.TypeName {
	return EngineName
}

// select a task from given task queue group and the request is ask from the specific queue.
// commonly select a task from the queue of the given name
// if the queue if empty, then try get task from the public queue, the task is from other busy queues.
func (fe *fastbuildEngine) SelectFirstTaskBasic(
	tqg *engine.TaskQueueGroup,
	queueName string) (*engine.TaskBasic, error) {
	tb, err := tqg.GetQueue(queueName).First()
	if err == engine.ErrorNoTaskInQueue {
		// 如果公共队列也没有，则直接返回
		if fe.publicQueue.Len() == 0 {
			return nil, engine.ErrorNoTaskInQueue
		}

		// 读取集群属性，看当前集群是否允许跨集群选择任务
		var cluster *TableClusterSetting
		cluster, err = fe.mysql.GetClusterSetting(queueName)
		if err != nil {
			blog.Warnf("engine(%s) try get cluster setting(%s) failed: %v", EngineName, queueName, err)
			return nil, err
		}

		// 如果集群属性不允许跨集群选择任务，则返回
		if cluster != nil && cluster.Attr&attrResourceClusterAllowCross == 0 {
			blog.Debugf("engine(%s) cluster [%s] not allowed to select cross-cluster tasks",
				EngineName, queueName)
			return nil, engine.ErrorNoTaskInQueue
		}

		for {
			tb, err = fe.publicQueue.First()
			if err == engine.ErrorNoTaskInQueue {
				return nil, err
			}

			// 检查任务是否有效
			if !tqg.Exist(tb.ID) {
				blog.Warnf("engine(%s) get task(%s) from public queue is invalid now", EngineName, tb.ID)
				_ = fe.publicQueue.Delete(tb.ID)
				continue
			}

			blog.Debugf("engine(%s) cluster [%s] got task(%s) from public queue", EngineName, queueName, tb.ID)
			return tb, nil
		}
	}

	if err != nil {
		blog.Errorf("engine(%s) get first from queue(%s) failed: %v", EngineName, queueName, err)
		return nil, err
	}

	return tb, nil
}

// create task extension with extra data into db
func (fe *fastbuildEngine) CreateTaskExtension(tb *engine.TaskBasic, extra []byte) error {
	return fe.createTask(tb, extra)
}

// get task extension from db
func (fe *fastbuildEngine) GetTaskExtension(taskID string) (engine.TaskExtension, error) {
	return fe.getTask(taskID)
}

// try launching task from queue.
// first try to consume the resource, if success then launch the workers.
func (fe *fastbuildEngine) LaunchTask(tb *engine.TaskBasic, queueName string) error {
	if err := fe.consumeResource(tb, queueName); err != nil {
		return err
	}

	return fe.launchTask(tb.ID)
}

// check task when running, failed with error, such as running timeout.
func (fe *fastbuildEngine) CheckTask(tb *engine.TaskBasic) error {
	return fe.checkTask(tb)
}

// do not support degrade mode.
func (fe *fastbuildEngine) DegradeTask(taskID string) error {
	return engine.ErrorNoSupportDegrade
}

// check if the launch is done
func (fe *fastbuildEngine) LaunchDone(taskID string) (bool, error) {
	return fe.launchDone(taskID)
}

// do not support project message
func (fe *fastbuildEngine) SendProjectMessage(projectID string, message []byte) ([]byte, error) {
	return nil, nil
}

// deal subtaskdone message here
func (fe *fastbuildEngine) SendTaskMessage(taskID string, extra []byte) ([]byte, error) {
	// decode extra to Message struct
	blog.Debugf("engine(%s) SendTaskMessage received task[%s] with data[%s]", EngineName, taskID, string(extra))

	if extra == nil || len(extra) == 0 {
		return nil, nil
	}

	var data Message
	if err := codec.DecJSON(extra, &data); err != nil {
		blog.Errorf("engine(%s) failed to Decode message for [%v] with data[%s]", EngineName, err, string(extra))
		return nil, err
	}

	switch data.Type {
	case MessageTypeSubTaskDone:
		return nil, fe.onSubTaskDoneMessage(&data)
	}

	return nil, ErrorInvalidMessageType
}

func (fe *fastbuildEngine) onSubTaskDoneMessage(msg *Message) error {
	submsg, err := DecodeSubTaskDone(msg.Data)
	if err != nil {
		blog.Errorf("engine(%s) failed to DecodeSubTaskDone for [%v] with data[%+v]", EngineName, err, msg)
		return err
	}

	// save sub msg now
	if err = fe.mysql.PutSubTask(MessageSubTaskDone2table(submsg)); err != nil {
		blog.Errorf("engine(%s) save sub task(%s) failed: %v", EngineName, submsg.TaskID, err)
		return err
	}

	return nil
}

// collect the task data, record the stats from server side.
func (fe *fastbuildEngine) CollectTaskData(tb *engine.TaskBasic) error {
	return fe.collectTaskData(tb)
}

// release task, shut down workers and free the resources.
func (fe *fastbuildEngine) ReleaseTask(taskID string) error {
	return fe.releaseTask(taskID)
}

func (fe *fastbuildEngine) consumeResource(tb *engine.TaskBasic, queueName string) error {
	taskID := tb.ID
	task, err := fe.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try consuming resource, get task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	blog.Infof("engine(%s) queueName[%s] task[%s] ready consume resource", EngineName, queueName, taskID)

	var allowCrossCluster = task.Attr&attrProjectAllowCross == 1
	condition := &resourceCondition{
		queueName:         queueName,
		leastCPU:          task.LeastCPU,
		maxCPU:            task.RequestCPU,
		allowCrossCluster: allowCrossCluster,
		taskid:            taskID,
	}
	resources, err := fe.mgr.GetFreeResource(taskID, condition, fe.resourceSelector, nil)
	if err != nil {
		// 判断该任务是否允许被其它集群选择（attr属性），如果允许，则放到公共队列里
		blog.Errorf("engine(%s) queueName[%s] task[%s] cosume resource with condtion(%+v) failed: %v",
			EngineName, queueName, taskID, *condition, err)
		if allowCrossCluster {
			blog.Infof("engine(%s) queueName[%s] task[%s] ready add to public queue",
				EngineName, queueName, taskID)
			fe.publicQueue.Add(tb)
		}
		return err
	}
	task.ResourceID = taskID

	// record the cpu/mem used total
	var cpuTotal float64 = 0
	var memTotal float64 = 0
	for _, r := range resources {
		cpuTotal += r.Resource.CPU
		memTotal += r.Resource.Mem
	}
	task.CPUTotal = cpuTotal
	task.MemTotal = memTotal

	blog.Infof("engine(%s) task[%s] success to consume resource", EngineName, taskID)
	return fe.updateTask(task)
}

// return the preferences
func (fe *fastbuildEngine) GetPreferences() engine.Preferences {
	return preferences
}

// get task basic table db operator
func (fe *fastbuildEngine) GetTaskBasicTable() *gorm.DB {
	return fe.mysql.GetDB().Table(TableTask{}.TableName())
}

// get project basic table db operator
func (fe *fastbuildEngine) GetProjectBasicTable() *gorm.DB {
	return fe.mysql.GetDB().Table(TableProjectSetting{}.TableName())
}

// get project info basic table db operator
func (fe *fastbuildEngine) GetProjectInfoBasicTable() *gorm.DB {
	return fe.mysql.GetDB().Table(TableProjectInfo{}.TableName())
}

// get whitelist basic table db operator
func (fe *fastbuildEngine) GetWhitelistBasicTable() *gorm.DB {
	return fe.mysql.GetDB().Table(TableWhitelist{}.TableName())
}

func (fe *fastbuildEngine) launchDone(taskID string) (bool, error) {
	task, err := fe.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try checking if task launch done, get task(%s) failed: %v",
			EngineName, taskID, err)
		return false, err
	}

	infoList, err := fe.mgr.ListCommands(task.ResourceID)
	if err != nil {
		blog.Errorf("engine(%s) try checking if task(%s) launch done, list commands failed: %v",
			EngineName, taskID, err)
		return false, err
	}

	availableList := make([]string, 0, 100)
	for _, info := range infoList {
		switch info.Status {
		case respack.CommandStatusInit:
			return false, nil
		case respack.CommandStatusSucceed:
			availableList = append(availableList, info.IP)
		}
	}

	//remoteRes, _ := fe.res.getAll(taskID)
	remoteRes := task.RemoteResources
	if remoteRes == nil {
		blog.Errorf("engine(%s) try checking if task(%s) launch done, list remote res failed",
			EngineName, taskID)
		return false, err
	}

	if len(availableList) == len(remoteRes) {
		for _, v := range remoteRes {
			task.WorkerIPList = append(task.WorkerIPList, fmt.Sprintf("%s:%d", v.RemoteIP, v.RemotePort))
		}
		blog.Debugf("engine(%s) task(%s) resource list[%+v]", EngineName, taskID, task.WorkerIPList)
		return true, fe.updateTask(task)
	}

	blog.Debugf("engine(%s) not gather all commands info,current(%d),expected(%d)",
		EngineName, len(availableList), len(remoteRes))
	return false, nil
}

func (fe *fastbuildEngine) collectTaskData(tb *engine.TaskBasic) error {
	task, err := fe.getTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try collecting task data, get task(%s) failed: %v", EngineName, tb.ID, err)
		return err
	}

	if err = fe.mysql.AddProjectInfoStats(tb.Client.ProjectID, DeltaInfoStats{
		ServiceUnits: float64(tb.Status.EndTime.Unix()-tb.Status.StartTime.Unix()) * task.CPUTotal,
	}); err != nil {
		blog.Errorf("engine(%s) try collecting task(%s) data, add project(%s) info stats failed: %v",
			EngineName, tb.ID, tb.Client.ProjectID, err)
		return err
	}

	blog.Infof("engine(%s) success to collect task(%s) data", EngineName, tb.ID)
	return nil
}

func (fe *fastbuildEngine) launchTask(taskID string) error {
	blog.Infof("engine(%s) try to launch task(%s)", EngineName, taskID)
	task, err := fe.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try launching task, get task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	resources, err := fe.mgr.ListResource(task.ResourceID)
	if err != nil {
		blog.Errorf("engine(%s) try launching task(%s), list resource(%s) failed: %v",
			EngineName, taskID, task.ResourceID, err)
		return err
	}

	for index, r := range resources {
		_ = fe.launchOneCommand(taskID, task, r, index)
	}

	if err = fe.updateTask(task); err != nil {
		blog.Errorf("engine(%s) try launching task(%s) with resourceID(%s), update task failed: %v",
			EngineName, taskID, task.ResourceID, err)
		return err
	}

	blog.Infof("engine(%s) success to launch task(%s)", EngineName, taskID)
	return nil
}

func (fe *fastbuildEngine) launchOneCommand(
	taskID string,
	task *fastbuildTask,
	r *resource.AgentResourceExternal, index int) error {
	env := map[string]string{}
	if task.FBResultCompress {
		env[FBCompressResultEnvKey] = "true"
		blog.Debugf("engine(%s) set env for agent [%s] to [%s]", EngineName, FBCompressResultEnvKey, "true")
	} else {
		env[FBCompressResultEnvKey] = "false"
		blog.Debugf("engine(%s) set env for agent [%s] to [%s]", EngineName, FBCompressResultEnvKey, "false")
	}

	remotePort, _ := fe.remotePort(taskID, r.Base.IP, task.AgentMinPort, task.AgentMaxPort)
	id := userDefineID(taskID, index)
	task.RemoteResources = append(task.RemoteResources, RemoteResource{
		UserDefineID: id,
		RemoteIP:     r.Base.IP,
		RemotePort:   remotePort,
	})

	go func(taskID string, task *fastbuildTask, r *resource.AgentResourceExternal, id string) {
		if err := fe.mgr.ExecuteCommand(r.Base.IP, task.ResourceID, &respack.Command{
			Cmd:          task.AgentRemoteExe,
			CmdType:      respack.CmdLaunch,
			Parameters:   task.getCommandParameters(task.ClientIP, remotePort),
			Env:          env,
			UserDefineID: id,
			Dir:          "",
			Path:         task.AgentPath,
		}); err != nil {
			blog.Errorf("engine(%s) try launching task(%s), execute command on(%s) failed: %v",
				EngineName, taskID, r.Base.IP, err)
			return
		}
		return
	}(taskID, task, r, id)

	return nil
}

// 根据已分配的端口和端口范围，来选择新的端口； 目前简单实现为独占的方式，直接返回最小端口
func (fe *fastbuildEngine) remotePort(taskID string, remoteIP string, minPort uint32, maxPort uint32) (uint32, error) {
	return minPort, nil
}

func userDefineID(taskID string, index int) string {
	return fmt.Sprintf("%s_%s_%d", EngineName, taskID, index)
}

func (fe *fastbuildEngine) releaseTask(taskID string) error {
	blog.Infof("engine(%s) try to release task(%s)", EngineName, taskID)
	task, err := fe.getTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) try releasing task, get task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	if err = fe.mgr.ReleaseResource(task.ResourceID); err != nil {
		blog.Errorf("engine(%s) try releasing task, release task(%s) failed: %v", EngineName, taskID, err)
		return err
	}

	// 将子任务的统计数据汇总到任务表里，这个功能可以单独实现为一个函数
	summary, err := fe.mysql.GetSubTaskSummary(taskID)
	if err != nil {
		blog.Errorf("engine(%s) failed to get sub task summary for task(%s)", EngineName, taskID)
		return err
	}

	task.FbSummary = *summary
	blog.Infof("engine(%s) success to release task(%s)", EngineName, taskID)
	return fe.updateTask(task)
}

func (fe *fastbuildEngine) createTask(tb *engine.TaskBasic, extra []byte) error {
	project, err := fe.mysql.GetProjectSetting(tb.Client.ProjectID)
	if err != nil {
		blog.Errorf("engine(%s) try creating task(%s), get project setting(%s) failed: %v",
			EngineName, tb.ID, tb.Client.ProjectID, err)
		return err
	}

	task, err := newTask(tb.ID)
	if err != nil {
		blog.Errorf("engine(%s) try creating task(%s), get task failed: %v", EngineName, tb.ID, err)
		return err
	}

	// 将个性化的项目设置赋值给task
	task.RequestCPU = project.RequestCPU
	task.LeastCPU = project.LeastCPU

	task.CacheEnabled = project.CacheEnabled
	task.FBResultCompress = project.FBResultCompress
	task.Attr = project.Attr

	task.AgentMinPort = project.AgentMinPort
	task.AgentMaxPort = project.AgentMaxPort
	task.AgentRemoteExe = project.AgentRemoteExe
	task.AgentWorkerConsole = project.AgentWorkerConsole
	task.AgentWorkerMode = project.AgentWorkerMode
	task.AgentWorkerNosubprocess = project.AgentWorkerNosubprocess
	task.Agent4OneTask = project.Agent4OneTask
	task.AgentWorkerCPU = project.AgentWorkerCPU

	// 设置客户端带上来的信息
	var data TaskExtra
	if err := codec.DecJSON(extra, &data); err != nil {
		blog.Errorf("engine(%s) failed to decode for [%v] with data[%s]", EngineName, err, extra)
		return err
	}
	task.TaskExtra = data
	// 优先使用客户端的cache参数
	if task.TaskExtra.CacheEnabled != "" {
		if task.TaskExtra.CacheEnabled == "true" {
			task.CacheEnabled = true
		} else if task.TaskExtra.CacheEnabled == "false" {
			task.CacheEnabled = false
		}
	}

	// 特殊处理AgentPath
	if data.Path != "" {
		task.AgentPath = data.Path
	} else {
		task.AgentPath = data.Version
	}

	return fe.updateTask(task)
}

func (fe *fastbuildEngine) getTask(taskID string) (*fastbuildTask, error) {
	t, err := fe.mysql.GetTask(taskID)
	if err != nil {
		blog.Errorf("engine(%s) get task(%s) failed: %v", EngineName, taskID, err)
		return nil, err
	}

	return tableTask2task(t), nil
}

func (fe *fastbuildEngine) updateTask(task *fastbuildTask) error {
	blog.Debugf("engine(%s) ready to updateTask with task[%+v]", EngineName, *task)
	data, err := engine.GetMapExcludeTableTaskBasic(task2tableTask(task))
	if err != nil {
		blog.Errorf("engine(%s) update task(%s), get exclude map failed: %v", EngineName, task.TaskID, err)
		return err
	}

	if err = fe.mysql.UpdateTask(task.TaskID, data); err != nil {
		blog.Errorf("engine(%s) update task(%s), update failed: %v", EngineName, task.TaskID, err)
		return err
	}

	return nil
}

func (fe *fastbuildEngine) resourceSelector(
	free []*respack.AgentResourceExternal,
	condition interface{}) ([]*respack.AgentResourceExternal, error) {
	if free == nil || len(free) == 0 {
		err := fmt.Errorf("engine(%s) free resource is empty, do nothing within SelectResource", EngineName)
		blog.Errorf("%v", err)
		return nil, engine.ErrorNoEnoughResources
	}

	c := condition.(*resourceCondition)
	if c == nil {
		err := fmt.Errorf("engine(%s) get nil condition", EngineName)
		blog.Errorf("%v", err)
		return nil, err
	}

	blog.Infof("engine(%s) ready take free resource for task[%s] with condition [%+v] queue[%s], len(free)[%d]",
		EngineName, c.taskid, condition, c.queueName, len(free))

	var cpuTotal float64
	r := make([]*respack.AgentResourceExternal, 0, 100)
	for _, agent := range free {
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

	// 确认资源是否足够
	if cpuTotal < c.leastCPU {
		blog.Errorf("failed to get enought resource with engine(%s) queueName[%s] task[%s] condition [%+v]",
			EngineName, c.queueName, c.taskid, *c)
		return nil, engine.ErrorNoEnoughResources
	}

	blog.Infof("engine(%s) after resourceSelector queue[%s], resource[%+v]", EngineName, c.queueName, r)
	return r, nil
}

type resourceCondition struct {
	queueName         string
	leastCPU          float64
	maxCPU            float64
	allowCrossCluster bool
	taskid            string
}

// 判断任务是否正常：有两种异常情况，1 超过最大任务时长 2 在一定时间内没有创建新的子任务
func (fe *fastbuildEngine) checkTask(tb *engine.TaskBasic) error {
	var err error
	// check running tasks
	if tb.Status.Status == engine.TaskStatusRunning {
		now := time.Now().Local()
		// 是否超过最大运行时长
		if tb.Status.CreateTime.Add(time.Duration(fe.conf.TaskMaxRunningSeconds) * time.Second).Before(now) {
			err = fmt.Errorf("status(%s) over max time(%d) seconds",
				tb.Status.Status, fe.conf.TaskMaxRunningSeconds)
			blog.Errorf("engine(%s) checkTask task[%s] error[%v], will be canceled", EngineName, tb.ID, err)
			return err
		}

		te, err := fe.getTask(tb.ID)
		if err != nil {
			blog.Errorf("engine(%s) checkTask, get task(%s) failed: %v", EngineName, tb.ID, err)
			return err
		}

		// check for cmd "bk-FbMain.exe ping -t 127.0.0.1"
		if te.FullCmd == fe.conf.SpecialFBCmd {
			blog.Debugf("engine(%s) checkTask for [BKMain] check running task for task ID(%s) cmd [%s]",
				EngineName, tb.ID, te.FullCmd)

			var opts commonMySQL.ListOptions
			opts.Equal("task_id", tb.ID)
			subtaskList, _, err := fe.mysql.ListAllSubTask(opts)
			if err != nil {
				blog.Warnf("engine(%s) checkTask for [BKMain] failed to get subtasklist for ID(%s) err[%v]",
					EngineName, tb.ID, err)
				return nil
			}
			if subtaskList != nil && len(subtaskList) > 0 {
				withinwaitduration := false
				for _, subtask := range subtaskList {
					// 判断任务时间是否在等待范围内
					if subtask.StartTime > 0 {
						starttime := time.Unix(subtask.StartTime, 0)
						if starttime.Add(
							time.Duration(fe.conf.TaskBKMainNoSubTaskTimeoutSeconds) * time.Second).After(now) {
							withinwaitduration = true
							break
						}
					}
					if subtask.EndTime > 0 {
						endtime := time.Unix(subtask.EndTime, 0)
						if endtime.Add(
							time.Duration(fe.conf.TaskBKMainNoSubTaskTimeoutSeconds) * time.Second).After(now) {
							withinwaitduration = true
							break
						}
					}
				}

				// if no new sub task created for long time
				if !withinwaitduration {
					err = fmt.Errorf("status(%s) over max time(%d) seconds, has no new sub task",
						tb.Status.Status, fe.conf.TaskBKMainNoSubTaskTimeoutSeconds)
					blog.Errorf("engine(%s) checkTask task[%s] error[%v], will be canceled",
						EngineName, tb.ID, err)
					return err
				}
			} else {
				// if no sub task created for long time
				if tb.Status.CreateTime.Add(
					time.Duration(fe.conf.TaskBKMainNoSubTaskTimeoutSeconds) * time.Second).Before(now) {
					err = fmt.Errorf("status(%s) over max time(%d) seconds, has no new sub task",
						tb.Status.Status, fe.conf.TaskBKMainNoSubTaskTimeoutSeconds)
					blog.Errorf("engine(%s) checkTask task[%s] error[%v], will be canceled",
						EngineName, tb.ID, err)
					return err
				}
			}
		}
	}
	return nil
}
