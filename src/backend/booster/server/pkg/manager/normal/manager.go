/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package normal

import (
	"context"
	"encoding/binary"
	"fmt"
	"net"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/util"
	"github.com/Tencent/bk-ci/src/booster/server/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	mgr "github.com/Tencent/bk-ci/src/booster/server/pkg/manager"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/types"
)

type manager struct {
	ctx       context.Context
	cancel    context.CancelFunc
	roleEvent types.RoleChangeEvent
	running   bool

	layer    TaskBasicLayer
	selector Selector
	tracker  Tracker
	keeper   Keeper
	cleaner  Cleaner

	engines map[engine.TypeName]engine.Engine
}

// NewManager get a new manager instance
// roleEvent is a channel which receive role message when current server role changed. Manager starts when it is
//  master and stops when it is not master.
// debug decide if debug mode is set. If true, it will disabled some checks such as keeper checks.
// queueBriefInfoList contains a list of queue brief info(queue-engine pair). It decide how many workers should be
//  launched in selector.
// engineList contains all engine instances the manager supported, it is used to init the layer cache.
func NewManager(
	roleEvent types.RoleChangeEvent,
	debug bool,
	queueBriefInfoList []engine.QueueBriefInfo,
	serverConf config.ServerConfig,
	engineList ...engine.Engine) mgr.Manager {
	engines := make(map[engine.TypeName]engine.Engine, 10)
	enl := make([]string, 0, 10)
	for _, egn := range engineList {
		engines[egn.Name()] = egn
		enl = append(enl, egn.Name().String())
	}

	blog.Infof("manager: create a new manager with engines: %v", enl)
	layer := NewDefaultTaskBasicLayer(engines)
	mgr := manager{
		roleEvent: roleEvent,
		engines:   engines,
		layer:     layer,
		keeper:    NewKeeper(layer, debug, serverConf.CommonEngineConfig),
		cleaner:   NewCleaner(layer),
	}
	selector := NewSelector(layer, &mgr, queueBriefInfoList...)
	tracker := NewTracker(layer, &mgr)
	mgr.selector = selector
	mgr.tracker = tracker
	return &mgr
}

// Run keep listening to the role change event, start manager when master, stop manager when no-master.
func (m *manager) Run() {
	blog.Infof("manager: begin to watch RoleChangeEvent")

	for {
		select {
		case e := <-m.roleEvent:
			blog.Infof("manager: receive new role change event: %s", e)
			switch e {
			case types.ServerMaster:
				m.start()
			case types.ServerSlave, types.ServerUnknown:
				m.stop()
			default:
				blog.Warnf("manager: unknown role to manager, will not change manager state: %s", e)
			}
		}
	}
}

// CreateTask receive TaskCreateParam and create a new task.
func (m *manager) CreateTask(param *mgr.TaskCreateParam) (*engine.TaskBasic, error) {
	if !m.running {
		blog.Errorf("manager: create task failed: manager is not running")
		return nil, types.ErrorManagerNotRunning
	}

	return m.createTask(param)
}

// SendProjectMessage send message data to the project with given projectID.
func (m *manager) SendProjectMessage(projectID string, data []byte) ([]byte, error) {
	if !m.running {
		blog.Errorf("manager: send project message failed: manager is not running")
		return nil, types.ErrorManagerNotRunning
	}

	return m.sendProjectMessage(projectID, data)
}

// SendTaskMessage send message data to the task with given taskID.
func (m *manager) SendTaskMessage(taskID string, data []byte) ([]byte, error) {
	if !m.running {
		blog.Errorf("manager: send task message: manager is not running")
		return nil, types.ErrorManagerNotRunning
	}

	return m.sendTaskMessage(taskID, data)
}

// ReleaseTask receive TaskReleaseParam and release a existing task.
func (m *manager) ReleaseTask(param *mgr.TaskReleaseParam) error {
	if !m.running {
		blog.Errorf("manager: release task failed: manager is not running")
		return types.ErrorManagerNotRunning
	}

	return m.releaseTask(param)
}

// GetTaskRank get the task rank in its queue.
func (m *manager) GetTaskRank(taskID string) (int, error) {
	if !m.running {
		blog.Errorf("manager: get task rank(%s) failed: manager is not running", taskID)
		return -1, types.ErrorManagerNotRunning
	}

	tb, err := m.getTaskBasic(taskID)
	if err != nil {
		blog.Errorf("manager: try getting task rank, get task basic(%s) failed: %v", taskID, err)
		return -1, err
	}

	qg, err := m.layer.GetTaskQueueGroup(tb.Client.EngineName)
	if err != nil {
		blog.Errorf("manager: try getting task rank, get task(%s) queue group from engine(%s) failed: %v",
			taskID, tb.Client.EngineName, err)
		return -1, err
	}

	rank, err := qg.GetQueue(tb.Client.QueueName).Rank(taskID)
	if err != nil {
		blog.Warnf("manager: try getting task rank, get task(%s) rank from engine(%s) queue(%s) failed: %v",
			taskID, tb.Client.EngineName, tb.Client.QueueName, err)
		return -1, err
	}

	return rank, nil
}

// GetTaskBasic get task basic from layer cache.
func (m *manager) GetTaskBasic(taskID string) (*engine.TaskBasic, error) {
	if !m.running {
		blog.Errorf("manager: get task basic(%s) failed: manager is not running", taskID)
		return nil, types.ErrorManagerNotRunning
	}

	return m.getTaskBasic(taskID)
}

// GetTaskExtension get task extension from engine database.
func (m *manager) GetTaskExtension(taskID string) (engine.TaskExtension, error) {
	tb, err := m.getTaskBasic(taskID)
	if err != nil {
		blog.Errorf("manager: get task basic(%s) failed: %v", taskID, err)
		return nil, err
	}

	engineName := tb.Client.EngineName.String()
	egn, err := m.layer.GetEngineByTypeName(engine.TypeName(engineName))
	if err != nil {
		blog.Errorf("manager: try getting task extension, get engine(%s) of task(%s) failed: %v",
			engineName, taskID, err)
		return nil, err
	}

	task, err := egn.GetTaskExtension(taskID)
	if err != nil {
		blog.Errorf("manager: try getting task extension, get task(%s) from engine(%s) failed: %v",
			taskID, engineName, err)
		return nil, err
	}

	return task, nil
}

// GetTask return both TaskBasic and TaskExtension.
func (m *manager) GetTask(taskID string) (*engine.TaskBasic, engine.TaskExtension, error) {
	tb, err := m.getTaskBasic(taskID)
	if err != nil {
		blog.Errorf("manager: get task basic(%s) failed: %v", taskID, err)
		return nil, nil, err
	}

	egn, err := m.layer.GetEngineByTypeName(tb.Client.EngineName)
	if err != nil {
		blog.Errorf("manager: try getting task extension, get engine(%s) of task(%s) failed: %v",
			tb.Client.EngineName, taskID, err)
		return nil, nil, err
	}

	task, err := egn.GetTaskExtension(taskID)
	if err != nil {
		blog.Errorf("manager: try getting task extension, get task(%s) from engine(%s) failed: %v",
			taskID, tb.Client.EngineName, err)
		return nil, nil, err
	}

	return tb, task, nil
}

// UpdateHeartbeat update the task heartbeat.
func (m *manager) UpdateHeartbeat(taskID string) error {
	if !m.running {
		blog.Errorf("manager: update heartbeat failed: manager is not running")
		return types.ErrorManagerNotRunning
	}

	if err := m.layer.UpdateHeartbeat(taskID); err != nil {
		blog.Warnf("manager: try updating heartbeat, get task basic(%s) failed: %v", taskID, err)
		return err
	}

	return nil
}

func (m *manager) start() {
	blog.Infof("manager: start handler")
	if m.running {
		blog.Errorf("manager: handler has already started")
		return
	}

	if err := m.recover(); err != nil {
		blog.Errorf("manager: handler recover failed: %v", err)
		return
	}

	m.running = true
	m.ctx, m.cancel = context.WithCancel(context.Background())
	go func() {
		_ = m.selector.Run(m.ctx)
	}()
	go func() {
		_ = m.tracker.Run(m.ctx)
	}()
	go func() {
		_ = m.keeper.Run(m.ctx)
	}()
	go func() {
		_ = m.cleaner.Run(m.ctx)
	}()
}

func (m *manager) stop() {
	blog.Infof("manager: stop handler")
	if !m.running {
		blog.Errorf("manager: handler has already stopped")
		return
	}

	m.cancel()
	m.running = false
}

// recover cache when manager start.
func (m *manager) recover() error {
	blog.Infof("manager: try to recover layer")
	if err := m.layer.Recover(); err != nil {
		blog.Errorf("manager: master recover layer failed: %v", err)
		return err
	}
	blog.Infof("manager: recover layer successfully")
	return nil
}

func (m *manager) getTaskBasic(taskID string) (*engine.TaskBasic, error) {
	tb, err := m.layer.GetTaskBasic(taskID)
	if err != nil {
		blog.Errorf("manager: get task basic(%s) failed: %v", taskID, err)
		return nil, err
	}

	return tb, nil
}

func (m *manager) createTask(param *mgr.TaskCreateParam) (*engine.TaskBasic, error) {
	blog.Infof("manager: create task: %+v", param)

	pb, egn, err := m.getBasicProject(param.ProjectID)
	if err != nil {
		blog.Errorf("manager: try creating task, get project(%s) failed: %v", param.ProjectID, err)
		return nil, err
	}

	ok, err := m.validWhitelist(egn, param.ProjectID, param.ClientIP)
	if err != nil {
		blog.Errorf("manager: try creating task, check whitelist for project(%s) in engine(%s) failed: %v",
			param.ProjectID, pb.EngineName.String(), err)
		return nil, err
	}

	// whitelist check failed
	if !ok {
		blog.Errorf("manager: try creating task, check whitelist, ip(%s) invalid for project(%s) in engine(%s)",
			param.ClientIP, param.ProjectID, pb.EngineName.String())
		return nil, fmt.Errorf("%v for [ip: %s][project_id: %s]",
			types.ErrorIPNotAllowed, param.ClientIP, param.ProjectID)
	}

	// lock project when creating task for controlling concurrency
	m.layer.LockProject(param.ProjectID)
	defer m.layer.UnLockProject(param.ProjectID)

	if err = m.invalidConcurrency(pb); err != nil {
		blog.Errorf("manager: try creating task, check concurrency for project(%s) in engine(%s) failed: %v",
			param.ProjectID, pb.EngineName.String(), err)
		return nil, err
	}

	taskID, err := m.generateTaskID(egn, param.ProjectID)
	if err != nil {
		blog.Errorf("manager: try creating task, generate taskID for project(%s) in engine(%s) failed: %v",
			param.ProjectID, pb.EngineName.String(), err)
		return nil, err
	}

	m.layer.LockTask(taskID)
	defer m.layer.UnLockTask(taskID)

	tb := &engine.TaskBasic{
		ID: taskID,
		Client: engine.TaskBasicClient{
			EngineName:    pb.EngineName,
			QueueName:     pb.QueueName,
			Priority:      pb.Priority,
			StageTimeout:  pb.StageTimeout,
			ProjectID:     param.ProjectID,
			BuildID:       param.BuildID,
			ClientVersion: param.ClientVersion,
			ClientIP:      param.ClientIP,
			ClientCPU:     param.ClientCPU,
			Message:       param.Message,
		},
		Status: engine.TaskBasicStatus{
			LastHeartBeatTime: time.Now().Local(),
			StatusChangeTime:  time.Unix(0, 0),
			InitTime:          time.Unix(0, 0),
			CreateTime:        time.Unix(0, 0),
			UpdateTime:        time.Unix(0, 0),
			LaunchTime:        time.Unix(0, 0),
			ReadyTime:         time.Unix(0, 0),
			ShutDownTime:      time.Unix(0, 0),
			StartTime:         time.Unix(0, 0),
			EndTime:           time.Unix(0, 0),
		},
	}
	if err = tb.Check(); err != nil {
		blog.Errorf("manager: create task basic(%s) check failed: %v", taskID, err)
		return nil, err
	}
	tb.Status.Init()
	tb.Status.Message = messageTaskInit

	if err = m.layer.InitTaskBasic(tb); err != nil {
		blog.Errorf("manager: create task basic(%s) for project(%s) in engine(%s) failed: %v",
			taskID, param.ProjectID, pb.EngineName.String(), err)
		return nil, err
	}
	if err = egn.CreateTaskExtension(tb, []byte(param.Extra)); err != nil {
		blog.Errorf("manager: create task extension(%s) for project(%s) in engine(%s) failed: %v",
			taskID, param.ProjectID, pb.EngineName.String(), err)
		return nil, err
	}
	tb.Status.Create()
	tb.Status.Message = messageTaskStaging

	if err = m.layer.UpdateTaskBasic(tb); err != nil {
		blog.Errorf("manager: update task basic(%s) for project(%s) in engine(%s) failed: %v",
			taskID, param.ProjectID, pb.EngineName.String(), err)
		return nil, err
	}

	// notify next step immediately
	m.onTaskStatus(tb, engine.TaskStatusStaging)

	blog.Infof("manager: success to create task(%s) for project(%s) in engine(%s), param: %+v",
		taskID, param.ProjectID, pb.EngineName.String(), param)
	return tb, nil
}

func (m *manager) sendProjectMessage(projectID string, data []byte) ([]byte, error) {
	m.layer.LockProject(projectID)
	defer m.layer.UnLockProject(projectID)

	_, egn, err := m.getBasicProject(projectID)
	if err != nil {
		blog.Errorf("manager: try sending project message, get project(%s) failed: %v", projectID, err)
		return nil, err
	}

	var result []byte
	if result, err = egn.SendProjectMessage(projectID, data); err != nil {
		blog.Errorf("manager: send project(%s) message to engine(%s) failed: %v",
			projectID, egn.Name().String(), err)
		return nil, err
	}

	blog.Infof("manager: success to send project(%s) message", projectID)
	return result, nil
}

func (m *manager) sendTaskMessage(taskID string, data []byte) ([]byte, error) {
	m.layer.LockTask(taskID)
	defer m.layer.UnLockTask(taskID)

	tb, err := m.layer.GetTaskBasic(taskID)
	if err != nil {
		blog.Errorf("manager: try sending task message, get task basic(%s) failed: %v", taskID, err)
		return nil, err
	}

	egn, err := m.layer.GetEngineByTypeName(tb.Client.EngineName)
	if err != nil {
		blog.Errorf("manager: try send task message, get engine(%s) of task(%s) failed: %v",
			tb.Client.EngineName, taskID, err)
		return nil, err
	}

	var result []byte
	if result, err = egn.SendTaskMessage(taskID, data); err != nil {
		blog.Errorf("manager: send task(%s) message to engine(%s) failed: %v",
			taskID, tb.Client.EngineName, err)
		return nil, err
	}

	blog.Infof("manager: success to send task(%s) message", taskID)
	return result, nil
}

func (m *manager) releaseTask(param *mgr.TaskReleaseParam) error {
	blog.Infof("manager: release task(%s), will make it to terminated status finish/failed, %+v",
		param.TaskID, param)
	m.layer.LockTask(param.TaskID)
	defer m.layer.UnLockTask(param.TaskID)

	tb, err := m.layer.GetTaskBasic(param.TaskID)
	if err != nil {
		blog.Errorf("manager: try releasing task, get task basic(%s) failed: %v",
			param.TaskID, err)
		return err
	}

	if tb.Status.Status.Terminated() {
		blog.Warnf("manager: release task(%s) already in terminated status", tb.ID)
		return types.ErrorTaskAlreadyTerminated
	}

	blog.Infof("manager: release task(%s) with success=%t from status(%s)",
		tb.ID, param.Success, tb.Status.Status)
	if tb.Status.Status == engine.TaskStatusRunning {
		tb.Status.End()
	}
	if param.Success {
		tb.Status.Finish()
		tb.Status.Message = messageTaskFinishSuccessfully
	} else {
		tb.Status.FailWithClientCancel()
		tb.Status.Message = messageTaskCanceledByClient
	}
	tb.Client.Message = param.Message

	egn, err := m.layer.GetEngineByTypeName(tb.Client.EngineName)
	if err != nil {
		blog.Errorf("manager: try releasing task, get engine(%s) of task(%s) failed: %v",
			tb.Client.EngineName.String(), param.TaskID, err)
		return err
	}

	if _, err = egn.SendTaskMessage(param.TaskID, []byte(param.Extra)); err != nil {
		blog.Warnf("manager: try releasing task, send task(%s) message via engine(%s) failed: %v",
			param.TaskID, tb.Client.EngineName.String(), err)
	}

	if err = m.layer.UpdateTaskBasic(tb); err != nil {
		blog.Errorf("manager: update basic task(%s) failed: %v", param.TaskID, err)
		return err
	}

	blog.Infof("manager: success to set task(%s) terminated, waiting for cleaner releasing", param.TaskID)
	return nil
}

func (m *manager) getBasicProject(projectID string) (*engine.ProjectBasic, engine.Engine, error) {
	for egnName, egn := range m.engines {
		pb, err := engine.GetProjectBasic(egn, projectID)
		if err == engine.ErrorProjectNoFound {
			continue
		}
		if err != nil {
			blog.Errorf("manager: get project(%s) from engine(%s) failed: %v", projectID, egnName, err)
			continue
		}
		if pb.EngineName != egn.Name() {
			continue
		}

		blog.Infof("manager: success to get project basic(%s) from engine(%s)", projectID, egnName)
		return pb, egn, nil
	}

	blog.Errorf("manager: get project(%s) no found", projectID)
	return nil, nil, engine.ErrorProjectNoFound
}

func (m *manager) validWhitelist(egn engine.Engine, projectID string, ip string) (bool, error) {
	blog.Infof("manager: check whitelist with projectID[%s] ip[%s]", projectID, ip)
	wll, err := engine.ListWhitelistBasic(egn, projectID)
	if err == engine.ErrorWhitelistNoFound {
		return false, nil
	}
	if err != nil {
		blog.Errorf("manager: get whitelist for project(%s) from engine(%s) failed: %v",
			projectID, egn.Name(), err)
		return false, err
	}

	return m.checkWhitelist(ip, projectID, wll), nil
}

func (m *manager) checkWhitelist(ipStr, projectID string, whitelist []*engine.WhitelistBasic) bool {
	iptokens := strings.Split(ipStr, "|")
	if len(iptokens) > 0 {
		for _, ip := range iptokens {
			for _, wl := range whitelist {
				if strings.Index(wl.IP, "~") != -1 { // ip range
					ips := strings.Split(wl.IP, "~")
					if len(ips) == 2 {
						start, err := ip2long(ips[0])
						if err != nil {
							blog.Warnf("manager: parse ip(%s) in whitelist of project(%s) failed: %v",
								ips[0], projectID, err)
							continue
						}

						end, err := ip2long(ips[1])
						if err != nil {
							blog.Warnf("manager: parse ip(%s) in whitelist of project(%s) failed: %v",
								ips[1], projectID, err)
							continue
						}

						current, err := ip2long(ip)
						if err != nil {
							blog.Warnf("manager: parse ip(%s) in whitelist of project(%s) failed: %v",
								ip, projectID, err)
							continue
						}

						if current >= start && current <= end {
							return true
						}
					}
				} else {
					// ip or mac addr
					if wl.IP == ip || wl.IP == "0.0.0.0" {
						return true
					}
				}
			}
		}
	}
	return false
}

func (m *manager) invalidConcurrency(pb *engine.ProjectBasic) error {
	currentCCY := m.layer.GetConcurrency(pb.ProjectID)
	blog.Infof("manager: project(%s) has current concurrency(%d)", pb.ProjectID, currentCCY)

	ccy := pb.Concurrency
	if ccy < 1 {
		ccy = defaultMaxParallel
	}

	if currentCCY >= ccy {
		blog.Errorf("manager: project(%s) has current concurrency(%d) and is over limit(%d)",
			pb.ProjectID, currentCCY, ccy)
		return fmt.Errorf("%v for [project_id: %s] current(%d) over limit(%d)",
			types.ErrorConcurrencyLimit, pb.ProjectID, currentCCY, ccy)
	}

	return nil
}

func (m *manager) generateTaskID(egn engine.Engine, projectID string) (string, error) {
	for i := 0; i < 3; i++ {
		taskID := generateTaskID(egn.Name().String(), projectID)

		ok, err := engine.CheckTaskIDValid(egn, taskID)
		if err != nil {
			return "", err
		}

		if ok {
			return taskID, nil
		}
	}

	return "", types.ErrorGenerateTaskIDFailed
}

func generateTaskID(egnName string, projectID string) string {
	return fmt.Sprintf(
		taskIDFormat, egnName, projectID, time.Now().Unix(), strings.ToLower(util.RandomString(taskIDRandomLength)))
}

//IsOldTaskType check if the task id type is old
func IsOldTaskType(id string) bool {
	idx := strings.LastIndex(id, "-")
	if idx == len(id)-taskIDRandomLength-1 { //old task Id
		return true
	}
	return false
}

func ip2long(ipStr string) (uint32, error) {
	ip := net.ParseIP(ipStr)
	if ip == nil {
		return 0, types.ErrorInvalidIPV4
	}
	ip = ip.To4()
	return binary.BigEndian.Uint32(ip), nil
}

// to notify next step quickly
func (m *manager) onTaskStatus(tb *engine.TaskBasic, curstatus engine.TaskStatusType) {
	blog.Infof("manager: task(%s) status changed to %s", tb.ID, curstatus)

	switch curstatus {
	case engine.TaskStatusStaging:
		// notify selector
		blog.Infof("manager: task(%s) current status %s, ready notify selector", tb.ID, curstatus)
		m.selector.OnTaskStatus(tb, curstatus)
	case engine.TaskStatusStarting:
		// notify tracker
		blog.Infof("manager: task(%s) current status %s, ready notify traker", tb.ID, curstatus)
		m.tracker.OnTaskStatus(tb, curstatus)
	default:
		blog.Infof("manager: task(%s) current status %s, do nothing", tb.ID, curstatus)
	}
}
