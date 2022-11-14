/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package manager

import (
	"fmt"
	"os"
	"runtime"
	"sort"
	"sync"
	"sync/atomic"
	"time"

	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager/analyser"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"

	"github.com/shirou/gopsutil/net"
)

// NewMgr get a new Work Manager
func NewMgr(conf *config.ServerConfig) types.Mgr {
	return &mgr{
		conf:              conf,
		worksPool:         newWorksPool(conf),
		commonconfigs:     make([]*types.CommonConfig, 0),
		globalWork:        newGlobalWork(conf),
		netCounters:       make(map[string]net.IOCountersStat),
		hasWorkUnregisted: false,
	}
}

const (
	workCheckIntervalTime = 3 * time.Second
	netCheckIntervalTime  = 5 * time.Second
)

type mgr struct {
	registerMutex sync.Mutex
	conf          *config.ServerConfig
	worksPool     *worksPool

	commonConfigMutex sync.Mutex
	commonconfigs     []*types.CommonConfig

	netCounters map[string]net.IOCountersStat

	globalWork *types.Work

	hasWorkUnregisted bool

	localResourceTaskMutex sync.Mutex
	localResourceTaskNum   int32
}

// Run brings up the manager handler
func (m *mgr) Run() {
	blog.Infof("mgr: controller manager start with config: %s", m.conf.Dump())

	go m.doCheckWork()
	go m.doCheckNet()
}

// RegisterWork 注册一个work, 返回其info信息, 是否为leader, 或错误信息
func (m *mgr) RegisterWork(config *types.WorkRegisterConfig) (*types.WorkInfo, bool, error) {
	blog.Infof("mgr: try to register a new work for project(%s) scene(%s)",
		config.Apply.ProjectID, config.Apply.Scene)

	// 保证同一时间只有一个work被注册
	m.registerMutex.Lock()
	defer m.registerMutex.Unlock()

	// BatchMode下, 找到已经存在的work, 并等待其达到working状态
	if config.BatchMode {
		blog.Infof("mgr: try to get a existing work with batch mode for project(%s) scene(%s)",
			config.Apply.ProjectID, config.Apply.Scene)
		if work := m.worksPool.find(config.Apply.ProjectID, config.Apply.Scene, config.BatchMode); work != nil {
			work.Lock()
			info := work.Basic().Info()
			if info.CanBeHeartbeat() {
				defer work.Unlock()
				_ = work.Basic().Heartbeat()
				work.Basic().IncRegistered()
				blog.Infof("mgr: success get a existing work(%s) with batch mode for project(%s) scene(%s)",
					work.ID(), config.Apply.ProjectID, config.Apply.Scene)

				// TOOD : apply resource if need
				if !work.Resource().HasAvailableWorkers() {
					work.Basic().ApplyResource(config)
				}
				return info, false, nil
			}
			work.Unlock()
		}
		blog.Infof("mgr: batch mode working work not found for project(%s) scene(%s), "+
			"current work is the first one",
			config.Apply.ProjectID, config.Apply.Scene)
	}

	commonconfigs4worker := m.getCommonSetting(config.Apply.ProjectID, config.Apply.Scene, config.BatchMode)
	work, err := m.worksPool.initWork(commonconfigs4worker)
	if err != nil {
		blog.Errorf("mgr: init work failed: %v", err)
		return nil, false, err
	}

	work.Lock()
	defer work.Unlock()

	if err := work.Basic().Register(config); err != nil {
		work.Basic().Info().SetRemovable()
		blog.Errorf("mgr: register new work(%s) basic process failed: %v", work.ID(), err)
		return nil, false, err
	}

	blog.Infof("mgr: success to register a new work(%s) for project(%s) scene(%s)",
		work.ID(), config.Apply.ProjectID, config.Apply.Scene)
	work.Basic().IncRegistered()
	return work.Basic().Info(), true, nil
}

// UnregisterWork do the unregister for specific workID
func (m *mgr) UnregisterWork(workID string, config *types.WorkUnregisterConfig) error {
	blog.Infof("mgr: try to unregister work(%s)", workID)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return err
	}

	work.Basic().DecRegistered()
	m.hasWorkUnregisted = true
	blog.Infof("mgr: try to unregister work(%s) and set m.hasWorkUnregisted to true", workID)

	if !config.Force && work.Basic().Info().IsBatchMode() && !m.conf.NoWait {
		blog.Infof("mgr: unregister work(%s) skipped, because of batch mode", workID)
		return nil
	}

	work.Lock()
	defer work.Unlock()

	if err = work.Basic().Unregister(config); err != nil {
		blog.Errorf("mgr: unregister work(%s)(%s) failed: %v", workID, work.Basic().Info().Status(), err)
		return err
	}

	blog.Infof("mgr: success to unregister work(%s), info: %s", workID, work.Basic().Info().Dump())
	return nil
}

// Heartbeat update the heartbeat for specific workID
func (m *mgr) Heartbeat(workID string) error {
	blog.Debugf("mgr: try to update heartbeat for work(%s)", workID)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return err
	}

	work.Lock()
	defer work.Unlock()

	if err = work.Basic().Heartbeat(); err != nil {
		blog.Errorf("mgr: update heartbeat for work(%s) failed: %v", workID, err)
		return err
	}

	blog.Debugf("mgr: success to update heartbeat for work(%s)", workID)
	return nil
}

// StartWork do the start for specific workID
func (m *mgr) StartWork(workID string) error {
	blog.Infof("mgr: try to start work(%s)", workID)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return err
	}

	work.Lock()
	defer work.Unlock()

	if err = work.Basic().Start(); err != nil {
		blog.Errorf("mgr: start work(%s) with status(%s) failed: %v", workID, work.Basic().Info().Status(), err)
		return err
	}

	blog.Infof("mgr: success to start work(%s) with settings(%s)", workID, work.Basic().Settings().Dump())
	return nil
}

// EndWork do the end for specific workID
func (m *mgr) EndWork(workID string) error {
	blog.Infof("mgr: try to end work(%s)", workID)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return err
	}

	work.Lock()
	defer work.Unlock()

	if err = work.Basic().End(0); err != nil {
		blog.Errorf("mgr: end work(%s) with status(%s) failed: %v", workID, work.Basic().Info().Status(), err)
		return err
	}

	blog.Infof("mgr: success to end work(%s)", workID)
	return nil
}

// SetWorkSettings update the work settings for specific workID
func (m *mgr) SetWorkSettings(workID string, settings *types.WorkSettings) error {
	blog.Infof("mgr: try to set settings for work(%s), settings: %s", workID, settings.Dump())

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return err
	}

	work.Lock()
	defer work.Unlock()

	work.Basic().SetSettings(settings)
	blog.Infof("mgr: success to set settings for work(%s), settings: %s", workID, settings.Dump())
	return nil
}

// GetWorkSettings return work settings for specific workID
func (m *mgr) GetWorkSettings(workID string) (*types.WorkSettings, error) {
	blog.Infof("mgr: try to get settings for work(%s)", workID)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return nil, err
	}

	work.Lock()
	defer work.Unlock()

	settings := work.Basic().Settings()
	blog.Infof("mgr: success to get settings for work(%s), settings: %s", workID, settings.Dump())
	return settings, nil
}

// SetCommonConfig update the common controller settings
func (m *mgr) SetCommonConfig(config *types.CommonConfig) error {
	blog.Debugf("mgr: try to set common config: %+v", *config)

	return m.setCommonConfig(config)
}

// GetWorkStatus return the work status for specific workID
func (m *mgr) GetWorkStatus(workID string) (*dcSDK.WorkStatusDetail, error) {
	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return nil, err
	}

	work.RLock()
	defer work.RUnlock()

	s := work.Basic().Info()
	ts := work.Resource().GetStatus()

	message := fmt.Sprintf("work(%s) current status(%s)", work.ID(), s.Status())
	if msg := s.StatusMessage(); msg != "" {
		message += fmt.Sprintf(": %s", msg)
	}
	if ts != nil {
		message = fmt.Sprintf("task(%s) status(%s): %s", ts.TaskID, ts.Status, ts.Message)
	}

	return &dcSDK.WorkStatusDetail{
		Status:  s.Status(),
		Task:    ts,
		Message: message,
	}, nil
}

// LockLocalSlots lock local slots for work
func (m *mgr) LockLocalSlots(workID string, usage dcSDK.JobUsage, weight int32) error {
	blog.Debugf("mgr: try to lock a local slot(%s) weight(%d) for work(%s)", usage, weight, workID)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return err
	}

	if !work.Basic().Info().IsWorking() {
		blog.Errorf("mgr: lock local slot(%s) for work(%s) failed: work is not working", usage, workID)
		return err
	}

	work.Basic().EnterTask()
	defer work.Basic().LeaveTask()

	if !work.Local().LockSlots(usage, weight) {
		blog.Errorf("mgr: lock local slot(%s) for work(%s) failed: %v",
			usage, workID, types.ErrSlotsLockFailed.Error())
		return types.ErrSlotsLockFailed
	}
	blog.Debugf("mgr: success to lock a local slot(%s) for work(%s)", usage, workID)
	return nil
}

// UnlockLocalSlots unlock local slots for work
func (m *mgr) UnlockLocalSlots(workID string, usage dcSDK.JobUsage, weight int32) error {
	blog.Debugf("mgr: try to unlock a local slot(%s) weight(%d) for work(%s)", usage, weight, workID)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return err
	}

	if !work.Basic().Info().IsWorking() {
		blog.Errorf("mgr: unlock local slot(%s) for work(%s) failed: work is not working", usage, workID)
		return err
	}

	work.Local().UnlockSlots(usage, weight)
	blog.Debugf("mgr: success to unlock a local slot(%s) for work(%s)", usage, workID)
	return nil
}

// ExecuteLocalTask do task for work
func (m *mgr) ExecuteLocalTask(
	workID string, req *types.LocalTaskExecuteRequest) (*types.LocalTaskExecuteResult, error) {
	blog.Infof("mgr: try to execute local task for work(%s) from pid(%d) with environment: %v, %v",
		workID, req.Pid, req.Environments, req.Commands)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return nil, err
	}

	if !work.Basic().Info().IsWorking() {
		blog.Errorf("mgr: execute local task for work(%s) from pid(%d) failed: %v",
			workID, req.Pid, types.ErrWorkIsNotWorking)
		return nil, types.ErrWorkIsNotWorking
	}

	var globalWork *types.Work
	if work.Basic().Settings().GlobalSlots {
		globalWork = m.globalWork
	}

	work.Basic().EnterTask()
	defer work.Basic().LeaveTask()
	// 记录job进入和离开的时间
	defer work.Basic().UpdateJobStats(req.Stats)
	dcSDK.StatsTimeNow(&req.Stats.EnterTime)
	defer dcSDK.StatsTimeNow(&req.Stats.LeaveTime)
	work.Basic().UpdateJobStats(req.Stats)
	withlocalresource := m.checkRunWithLocalResource(work)
	if withlocalresource {
		defer m.decLocalResourceTask()
	}
	result, err := work.Local().ExecuteTask(req, globalWork, withlocalresource)
	if err != nil {
		if result == nil {
			result = &types.LocalTaskExecuteResult{Result: &dcSDK.LocalTaskResult{
				ExitCode: -1,
				Message:  "unknown reason",
			}}
		}
		blog.Errorf("mgr: execute local task for work(%s) from pid(%d) failed: %v", workID, req.Pid, err)
		return result, nil
	}

	blog.Infof("mgr: success to execute local task for work(%s) from pid(%d) and get exit code(%d)",
		workID, req.Pid, result.Result.ExitCode)
	return result, nil
}

// ExecuteRemoteTask do remote task for work directly
func (m *mgr) ExecuteRemoteTask(
	workID string, req *types.RemoteTaskExecuteRequest) (*types.RemoteTaskExecuteResult, error) {
	blog.Infof("mgr: try to execute remote task for work(%s) from pid(%d)", workID, req.Pid)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return nil, err
	}

	if !work.Basic().Info().IsWorking() {
		blog.Errorf("mgr: execute remote task for work(%s) from pid(%d) failed: work is not working",
			workID, req.Pid)
		return nil, err
	}

	work.Basic().EnterTask()
	defer work.Basic().LeaveTask()
	result, err := work.Remote().ExecuteTask(req)
	if err != nil {
		blog.Errorf("mgr: execute remote task for work(%s) from pid(%d) failed: %v", workID, req.Pid, err)
		return nil, err
	}

	blog.Infof("mgr: success to execute remote task for work(%s) from pid(%d)", workID, req.Pid)
	return result, nil
}

// SendRemoteFile send remote files for work
func (m *mgr) SendRemoteFile(workID string, req *types.RemoteTaskSendFileRequest) error {
	blog.Infof("mgr: try to send remote file for work(%s) from pid(%d)", workID, req.Pid)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return err
	}

	if !work.Basic().Info().IsWorking() {
		blog.Errorf("mgr: send remote file for work(%s) from pid(%d) failed: work is not working",
			workID, req.Pid)
		return err
	}

	work.Basic().EnterTask()
	defer work.Basic().LeaveTask()
	_, err = work.Remote().SendFiles(req)
	if err != nil {
		blog.Errorf("mgr: send remote file for work(%s) from pid(%d) failed: %v", workID, req.Pid, err)
		return err
	}

	blog.Infof("mgr: success to send remote file for work(%s) from pid(%d)", workID, req.Pid)
	return nil
}

// UpdateJobStats update job stats for work
func (m *mgr) UpdateJobStats(workID string, stats *dcSDK.ControllerJobStats) error {
	blog.Debugf("mgr: try to update job stats for work(%s)", workID)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return err
	}

	work.Basic().UpdateJobStats(stats)

	blog.Debugf("mgr: success to update job stats for work(%s)", workID)
	return nil
}

// UpdateWorkStats update work stats for work
func (m *mgr) UpdateWorkStats(workID string, stats *types.WorkStats) error {
	blog.Debugf("mgr: try to update work stats for work(%s)", workID)

	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return err
	}

	work.Lock()
	defer work.Unlock()
	work.Basic().UpdateWorkStats(stats)

	blog.Debugf("mgr: success to update work stats for work(%s)", workID)
	return nil
}

// GetWorkDetailList return all works in pool
func (m *mgr) GetWorkDetailList() types.WorkStatsDetailList {
	r := make(types.WorkStatsDetailList, 0, 100)
	for _, work := range m.worksPool.all() {
		work.RLock()
		// work list不能包含任何jobs信息, 否则数据量会太大, 因此用index=-1
		r = append(r, work.Basic().GetDetails(-1))
		work.RUnlock()
	}
	sort.Sort(r)
	return r
}

// GetWorkDetail return work details for specific workID
func (m *mgr) GetWorkDetail(workID string, index int) (*types.WorkStatsDetail, error) {
	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return nil, err
	}

	work.RLock()
	defer work.RUnlock()
	return work.Basic().GetDetails(index), nil
}

// GetPumpCache return the file-cache and root-cache in pump mode for workID
func (m *mgr) GetPumpCache(workID string) (*analyser.FileCache, *analyser.RootCache, error) {
	work, err := m.worksPool.getWork(workID)
	if err != nil {
		blog.Errorf("mgr: get work(%s) failed: %v", workID, err)
		return nil, nil, err
	}

	f, r := work.Local().GetPumpCache()
	return f, r, nil
}

func (m *mgr) doCheckWork() {
	blog.Infof("controller: start do check work with tick: %s", workCheckIntervalTime.String())
	workCheckTick := time.NewTicker(workCheckIntervalTime)
	defer workCheckTick.Stop()
	var startseconds int64 = time.Now().Unix()

	for {
		select {
		case <-workCheckTick.C:
			m.checkWork(startseconds)
		}
	}
}

func (m *mgr) doCheckNet() {
	blog.Infof("controller: start do check net with tick: %s", netCheckIntervalTime.String())
	netCheckTick := time.NewTicker(netCheckIntervalTime)
	defer netCheckTick.Stop()

	for {
		select {
		case <-netCheckTick.C:
			m.checkNet()
		}
	}
}

func (m *mgr) checkWork(startseconds int64) {
	// 清理过期数据
	m.worksPool.cleanRemovableWork()

	// 释放心跳超时的work
	m.worksPool.cleanHeartbeatTimeout()

	// 屏蔽掉该逻辑，有两个理由：
	// 		1. 重复Init导致内存泄漏
	//		2. 该协程执行Init时，有可能globalWork在其它协程中正在被使用，导致未知错误（比如内存越界）
	//		后续如果有脏数据的情况，需要定位跟进
	// 每当work pool为空时, 重置一下globalWork, 避免一些脏数据残留
	// if m.worksPool.empty() {
	// 	m.globalWork.Local().Init()
	// }

	// 如果works pool空闲超过一定时间, 主动退出controller进程
	if m.conf.RemainTime >= 0 && m.worksPool.emptyTimeout(time.Duration(m.conf.RemainTime)*time.Second) {
		// to avoid quit immediately after started
		nowseconds := time.Now().Unix()
		if nowseconds-startseconds < 30 && !m.hasWorkUnregisted {
			return
		}

		blog.Warnf("mgr: works pool empty timeout, controller will exit")
		blog.CloseLogs()
		os.Exit(0)
	}

	// if m.conf.NoWait && m.hasWorkUnregisted {
	// 	if m.worksPool.emptyTimeout(time.Duration(0) * time.Second) {
	// 		blog.Warnf("mgr: works pool empty and do not need wait, controller will exit")
	// 		blog.CloseLogs()
	// 		os.Exit(0)
	// 	}
	// }
}

func (m *mgr) checkNet() {
	result, err := net.IOCounters(true)
	if err != nil {
		blog.Warnf("mgr: check net try to get io counters failed: %v", err)
		return
	}

	message := fmt.Sprintf("\n%-20s %-20s %-20s", "iface", "recv", "sent")
	for _, item := range result {
		if old, ok := m.netCounters[item.Name]; ok {
			recv := float64(item.BytesRecv-old.BytesRecv) / 1024 / 1024 / netCheckIntervalTime.Seconds()
			sent := float64(item.BytesSent-old.BytesSent) / 1024 / 1024 / netCheckIntervalTime.Seconds()

			message += fmt.Sprintf("\n%-20s %-20.6fMB/s %-20.6fMB/s", item.Name, recv, sent)
		}

		m.netCounters[item.Name] = item
	}

	blog.Debugf("mgr: check net stats: %s", message)
}

func (m *mgr) setCommonConfig(config *types.CommonConfig) error {
	blog.Infof("mgr: try to set common config")

	if err := m.saveCommonConfig(config); err != nil {
		blog.Infof("mgr: failed to save common config with error: %v", err)
		return err
	}
	blog.Infof("mgr: finished save common config")

	// update workers with this config
	if err := m.setWorkerConfig(config); err != nil {
		blog.Infof("mgr: failed to set worker config with error: %v", err)
		return err
	}

	return nil
}

func (m *mgr) saveCommonConfig(config *types.CommonConfig) error {
	blog.Debugf("mgr: try to save common config: %+v", *config)

	m.commonConfigMutex.Lock()
	defer m.commonConfigMutex.Unlock()

	if !m.coverRealType(config) {
		return fmt.Errorf("failed to check real type with: %+v", *config)
	}

	if len(m.commonconfigs) == 0 {
		m.commonconfigs = append(m.commonconfigs, config)
		return nil
	}

	// replace with newly
	replaced := false
	for i, v := range m.commonconfigs {
		if v.KeyEqual(config) {
			if m.needReplace(v, config) {
				m.commonconfigs[i] = config
				replaced = true
				break
			}
		}
	}

	if !replaced {
		m.commonconfigs = append(m.commonconfigs, config)
	}

	return nil
}

func (m *mgr) needReplace(oldconfig, newconfig *types.CommonConfig) bool {
	switch oldconfig.Configkey {
	case dcSDK.CommonConfigKeyToolChain:
		oldtoolchain, ok := oldconfig.Config.(dcSDK.OneToolChain)
		if !ok {
			blog.Warnf("mgr: failed to translate to tool chain for old config")
			return false
		}
		newtoolchain, ok := newconfig.Config.(dcSDK.OneToolChain)
		if !ok {
			blog.Warnf("mgr: failed to translate to tool chain for new config")
			return false
		}
		if oldtoolchain.ToolKey == newtoolchain.ToolKey {
			return true
		}
		return false
	default:
		blog.Warnf("mgr: unknown config key: %+v", oldconfig.Configkey)
		return false
	}
}

func (m *mgr) coverRealType(config *types.CommonConfig) bool {
	switch config.Configkey {
	case dcSDK.CommonConfigKeyToolChain:
		var realtype dcSDK.OneToolChain
		if err := codec.DecJSON(config.Data, &realtype); err != nil {
			blog.Errorf("mgr: deocde to OneToolChain failed: %v", err)
			return false
		}
		config.Config = realtype
		return true
	default:
		blog.Warnf("mgr: unknown config key: %+v", config.Configkey)
		return false
	}
}

func (m *mgr) getCommonSetting(projectID, scene string, batchMode bool) []*types.CommonConfig {
	blog.Debugf("mgr: get work common setting with projectid:%s scene:%s batchmode:%v",
		projectID, scene, batchMode)

	m.commonConfigMutex.Lock()
	defer m.commonConfigMutex.Unlock()

	commonconfigs4worker := make([]*types.CommonConfig, 0)
	if len(m.commonconfigs) > 0 {
		workerkey := &types.WorkerKeyConfig{
			BatchMode: batchMode,
			ProjectID: projectID,
			Scene:     scene,
		}
		for _, v := range m.commonconfigs {
			if (workerkey).Equal(&v.WorkerKey) {
				commonconfigs4worker = append(commonconfigs4worker, v)
			}
		}
	}

	blog.Debugf("mgr: get %d work common setting with projectid:%s scene:%s batchmode:%v",
		len(commonconfigs4worker), projectID, scene, batchMode)
	return commonconfigs4worker
}

func (m *mgr) setWorkerConfig(config *types.CommonConfig) error {
	blog.Infof("mgr: ready set worker config")

	works := []*types.Work{}
	for _, work := range m.worksPool.all() {
		info := work.Basic().Info()
		if info.ProjectID() == config.WorkerKey.ProjectID &&
			info.Scene() == config.WorkerKey.Scene &&
			info.IsBatchMode() == config.WorkerKey.BatchMode {
			works = append(works, work)
			blog.Infof("mgr: ready set config to worker:%s", work.ID())
		}
	}
	blog.Infof("mgr: got total %d workers", len(works))

	if config.Configkey == dcSDK.CommonConfigKeyToolChain {
		sdkToolChain, ok := config.Config.(dcSDK.OneToolChain)
		if ok {
			blog.Infof("mgr: got tool chain:%+v", sdkToolChain)
			toolchain := sdkToolChain2Types(&sdkToolChain)
			for _, work := range works {
				work.Lock()
				blog.Infof("mgr: ready set tool chain(%s) to worker(%s)", toolchain.ToolKey, work.ID())
				_ = work.Basic().SetToolChain(toolchain)
				work.Unlock()
			}
		} else {
			blog.Warnf("mgr: failed cast to toolchain with config:%+v", *config)
			return fmt.Errorf("failed cast to toolchain with config:%+v", *config)
		}
	} else {
		blog.Warnf("mgr: got unknown common config key:%s", config.Configkey)
		return fmt.Errorf("unknown common config key:%s", config.Configkey)
	}

	return nil
}

func sdkToolChain2Types(sdkToolChain *dcSDK.OneToolChain) *types.ToolChain {
	if sdkToolChain == nil {
		return nil
	}

	var TypeFiles []dcSDK.ToolFile
	if len(sdkToolChain.Files) > 0 {
		for _, v := range sdkToolChain.Files {
			TypeFiles = append(TypeFiles, v)
		}
	}

	return &types.ToolChain{
		ToolKey:                sdkToolChain.ToolKey,
		ToolName:               sdkToolChain.ToolName,
		ToolLocalFullPath:      sdkToolChain.ToolLocalFullPath,
		ToolRemoteRelativePath: sdkToolChain.ToolRemoteRelativePath,
		Files:                  TypeFiles,
		Timestamp:              time.Now().Local().UnixNano(),
	}
}

func (m *mgr) checkRunWithLocalResource(work *types.Work) bool {
	if m.conf.UseLocalCPUPercent <= 0 || m.conf.UseLocalCPUPercent > 100 {
		return false
	}

	m.localResourceTaskMutex.Lock()
	defer m.localResourceTaskMutex.Unlock()

	// check current running local resource tasks
	maxidlenum := runtime.NumCPU() - 2
	allowidlenum := maxidlenum * m.conf.UseLocalCPUPercent / 100
	runninglocalresourcetask := atomic.LoadInt32(&m.localResourceTaskNum)
	if runninglocalresourcetask >= int32(allowidlenum) {
		blog.Infof("mgr localresource check: running local resource task: %d,"+
			"max allow idle num:%d for work: %s",
			runninglocalresourcetask, allowidlenum, work.Basic().Info().WorkID())
		return false
	}

	// check prepared remote tasks
	prepared := work.Basic().Info().GetPrepared()
	remotetotal := work.Remote().TotalSlots()
	blog.Infof("mgr localresource check: prepared task: %d,total remote slots:%d for work: %s",
		prepared, remotetotal, work.Basic().Info().WorkID())
	if prepared < int32(remotetotal) {
		return false
	}

	// check local idle cpu
	reservedidlenum := maxidlenum - allowidlenum
	occupied := 0
	for _, work := range m.worksPool.all() {
		_, occupy := work.Local().Slots()
		if occupy > 0 {
			occupied += occupy
		}
	}
	if m.globalWork != nil {
		_, occupy := m.globalWork.Local().Slots()
		if occupy > 0 {
			occupied += occupy
		}
	}
	curidlenum := maxidlenum - occupied
	blog.Infof("mgr localresource check: local cpu percent %d,reserved cpu num %d,"+
		"current idle cpu num %d",
		m.conf.UseLocalCPUPercent, reservedidlenum, curidlenum)
	if curidlenum <= reservedidlenum {
		return false
	}

	blog.Infof("mgr localresource check: execute with local resource, "+
		"local resource task:%d,prepared task:%d,total remote slots:%d,"+
		"local cpu:%d,reserved cpu num:%d,current idle cpu num:%d for work(%s)",
		runninglocalresourcetask, prepared, remotetotal,
		m.conf.UseLocalCPUPercent, reservedidlenum, curidlenum,
		work.Basic().Info().WorkID())

	atomic.AddInt32(&m.localResourceTaskNum, 1)
	return true
}

func (m *mgr) decLocalResourceTask() {
	atomic.AddInt32(&m.localResourceTaskNum, -1)
}

// Get first workid
func (m *mgr) GetFirstWorkID() (string, error) {
	work, err := m.worksPool.getFirstWork()
	if err == nil {
		return work.ID(), nil
	} else {
		return "", err
	}
}
