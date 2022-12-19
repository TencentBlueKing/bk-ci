/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package basic

import (
	"context"
	"fmt"
	"sync"
	"sync/atomic"
	"time"

	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/util"
)

const (
	toolchainTaskIDKey = "{{task_id}}"
)

type toolchainCache struct {
	toolchain *types.ToolChain
	files     *[]dcSDK.FileDesc
}

// NewMgr get a new BasicMgr
func NewMgr(pCtx context.Context, work *types.Work) types.BasicMgr {
	ctx, _ := context.WithCancel(pCtx)

	return &Mgr{
		ctx:               ctx,
		work:              work,
		settings:          &types.WorkSettings{},
		info:              types.NewInitWorkInfo(work.ID()),
		waitWorkReadyTick: 100 * time.Millisecond,
		analysisStatus:    types.NewWorkAnalysisStatus(),
		toolchainMap:      make(map[string]toolchainCache, 1),
	}
}

// Mgr describe the basic manager
// provide the basic methods of work
type Mgr struct {
	ctx  context.Context
	work *types.Work

	setS       bool
	settings   *types.WorkSettings
	info       *types.WorkInfo
	workStatus *types.WorkStats

	waitWorkReadyTick time.Duration

	analysisStatus *types.WorkAnalysisStatus

	toolchainLock sync.RWMutex
	toolchainMap  map[string]toolchainCache

	aliveTask int64

	registeredCounter int32
}

// Alive return the current alive task number
func (m *Mgr) Alive() int64 {
	return atomic.LoadInt64(&m.aliveTask)
}

// EnterTask record the task enter
func (m *Mgr) EnterTask() {
	atomic.AddInt64(&m.aliveTask, 1)
}

// LeaveTask record the task leave
func (m *Mgr) LeaveTask() {
	_ = m.Heartbeat()
	atomic.AddInt64(&m.aliveTask, -1)
}

func (m *Mgr) getRegisteredCounter() int32 {
	return atomic.LoadInt32(&m.registeredCounter)
}

// IncRegistered increase the number of current registered counter for one work(usually in batch mode)
func (m *Mgr) IncRegistered() {
	atomic.AddInt32(&m.registeredCounter, 1)
}

// DecRegistered reduce the number of current registered counter for one work(usually in batch mode)
func (m *Mgr) DecRegistered() {
	_ = m.Heartbeat()
	atomic.AddInt32(&m.registeredCounter, -1)
}

// SetSettings update work setting
func (m *Mgr) SetSettings(settings *types.WorkSettings) {
	// 如果是batch-mode, 只允许第一个settings被写入
	if m.info.IsBatchMode() && m.setS {
		return
	}

	m.setS = true
	m.settings = settings
	m.info.SetSettings(settings)
}

// Settings get work setting
func (m *Mgr) Settings() *types.WorkSettings {
	return m.settings
}

// Info get work info
func (m *Mgr) Info() *types.WorkInfo {
	return m.info
}

// UpdateJobStats 管理所有的job stats信息, 根据pid和进入executor的时间作为key来存储,
// 默认同一个key的caller需要保证stats是按照时间顺序来update的, 后来的会直接覆盖前者
func (m *Mgr) UpdateJobStats(stats *dcSDK.ControllerJobStats) {
	m.analysisStatus.Update(stats)
}

// UpdateWorkStats update work stats
func (m *Mgr) UpdateWorkStats(stats *types.WorkStats) {
	m.info.SetStats(stats)
}

// Heartbeat update work heartbeat
func (m *Mgr) Heartbeat() error {
	if !m.info.CanBeHeartbeat() {
		return fmt.Errorf("%s %s", m.info.Status(), types.ErrWorkCannotBeUpdatedHeartbeat)
	}

	m.info.UpdateHeartbeat(time.Now().Local())

	return nil
}

// Register do register work
func (m *Mgr) Register(config *types.WorkRegisterConfig) error {
	if !m.info.CanBeRegister() {
		return fmt.Errorf("%s %s", m.info.Status(), types.ErrWorkCannotBeRegistered)
	}

	// 先注册状态, 再处理资源
	m.info.Register(config.BatchMode)
	_ = m.Heartbeat()

	m.info.SetProjectID(config.Apply.ProjectID)
	m.info.SetScene(config.Apply.Scene)

	m.work.Resource().SetServerHost(config.ServerHost)
	if len(config.SpecificHostList) > 0 {
		m.work.Resource().SetSpecificHosts(config.SpecificHostList)
	} else {
		if config.NeedApply {
			m.applyResource(config)
		}
	}

	return nil
}

func (m *Mgr) ApplyResource(config *types.WorkRegisterConfig) error {
	m.applyResource(config)

	return nil
}

func (m *Mgr) applyResource(config *types.WorkRegisterConfig) {
	blog.Infof("basic: going to apply resource for work(%s) with project(%s) scene(%s)",
		m.work.ID(), config.Apply.ProjectID, config.Apply.Scene)
	if _, err := m.work.Resource().Apply(config.Apply, false); err != nil {
		blog.Errorf("basic: register work(%s) try apply resource with project(%s) scene(%s) failed: %v",
			m.work.ID(), config.Apply.ProjectID, config.Apply.Scene, err)

		if m.info.CanBeResourceApplyFailed() {
			m.info.ResourceApplyFailed()
			m.info.SetStatusMessage(err.Error() + fmt.Sprintf(" with project(%s) bt(%s)",
				config.Apply.ProjectID, config.Apply.Scene))
		}
		return
	}

	if m.info.CanBeResourceApplying() {
		m.info.ResourceApplying()
	}
	blog.Infof("basic: success to apply resource for work(%s) with project(%s) scene(%s)",
		m.work.ID(), config.Apply.ProjectID, config.Apply.Scene)
}

// Unregister do unregister work
func (m *Mgr) Unregister(config *types.WorkUnregisterConfig) error {
	blog.Infof("basic: going to unregister work(%s)", m.work.ID())
	if !m.info.CanBeUnregistered() {
		return fmt.Errorf("%s %s", m.info.Status(), types.ErrWorkCannotBeUnregistered.Error())
	}

	if m.getRegisteredCounter() > 0 {
		return fmt.Errorf("%s can't be unregistered now for someone remain registered", m.info.Status())
	}

	if m.info.IsWorking() {
		if err := m.End(config.TimeoutBefore); err != nil {
			blog.Warnf("basic: unregister work(%s) try to end work failed: %v", m.work.ID, err)
		}
	}

	m.info.Unregister()
	m.handleUnregisteredProcess(config)

	blog.Infof("basic: set work(%s) unregistered", m.work.ID())
	return nil
}

func (m *Mgr) handleUnregisteredProcess(config *types.WorkUnregisterConfig) {
	blog.Infof("basic: try to handle the unregistered process for work(%s) and set it into removable",
		m.info.WorkID())
	if !m.info.IsUnregistered() {
		blog.Errorf("basic: handle unregistered process for work(%s) got another status(%s)",
			m.info.WorkID(), m.info.Status())
		return
	}

	if !m.settings.Degraded {
		if err := m.work.Resource().SendStats(false); err != nil {
			blog.Errorf("basic: handle unregistered process for work(%s), send stats failed: %v",
				m.info.WorkID(), err)
		}

		if err := m.work.Resource().Release(config.Release); err != nil {
			blog.Errorf("basic: handle unregistered process for work(%s), try to release resource failed: %v",
				m.info.WorkID(), err)
		}
	}

	m.info.SetRemovable()
	blog.Infof("basic: success to handle the unregistered process for work(%s) and set it into removable",
		m.info.WorkID())
}

// Start do start work
func (m *Mgr) Start() error {
	if m.info.IsWorking() {
		blog.Infof("basic: workinfo(%s) is working now, do nothing now",
			m.info.WorkID())
		return nil
	}

	if !m.info.CanBeStart() {
		return fmt.Errorf("%s %s", m.info.Status(), types.ErrWorkCannotBeStart)
	}

	m.info.Start()

	// work正式启动后, 需要初始化一系列manager,
	// local:  启动本地的持锁队列
	// remote: 启动远程连接池
	// change from Init to Start
	// m.work.Local().Init()
	// m.work.Remote().Init()
	m.work.Local().Start()
	m.work.Remote().Start()

	go m.syncBriefStats()
	return nil
}

func (m *Mgr) syncBriefStats() {
	blog.Infof("basic: begin to sync brief stats")

	ticker := time.NewTicker(10 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-m.ctx.Done():
			return
		case <-ticker.C:
			r := m.work.Resource()
			if r == nil {
				continue
			}

			if err := r.SendStats(true); err != nil {
				blog.Warnf("basic: sync brief stats failed: %v", err)
			}
		}
	}
}

// End do end work
func (m *Mgr) End(timeoutBefore time.Duration) error {
	if !m.info.CanBeEnd() {
		return fmt.Errorf("%s %s", m.info.Status(), types.ErrWorkCannotBeEnd)
	}

	m.info.End(timeoutBefore)

	return nil
}

// WaitUntilWorking hang until the work is working then return true, if exception happened then return false
func (m *Mgr) WaitUntilWorking(ctx context.Context) bool {
	ticker := time.NewTicker(m.waitWorkReadyTick)

	for {
		select {
		case <-ctx.Done():
			return false
		case <-ticker.C:
			if m.info.IsWorking() {
				return true
			}

			if m.info.IsUnregistered() {
				return false
			}
		}
	}
}

// GetDetails return the work stats detail, only return the jobs whose index is greater then jobIndex
func (m *Mgr) GetDetails(jobIndex int) *types.WorkStatsDetail {
	cs := m.info.CommonStatus()
	as := m.analysisStatus

	remoteOK, remoteError, localOK, localError := as.BasicCount()

	return &types.WorkStatsDetail{
		CurrentTime:      time.Now().Local().UnixNano(),
		WorkID:           m.info.WorkID(),
		TaskID:           m.info.TaskID(),
		Scene:            m.info.Scene(),
		Status:           m.info.Status().String(),
		Success:          m.info.Success(),
		StartTime:        cs.StartTime.Local().UnixNano(),
		EndTime:          cs.EndTime.Local().UnixNano(),
		RegisteredTime:   cs.RegisteredTime.Local().UnixNano(),
		UnregisteredTime: cs.UnregisteredTime.Local().UnixNano(),
		JobRemoteOK:      remoteOK,
		JobRemoteError:   remoteError,
		JobLocalOK:       localOK,
		JobLocalError:    localError,
		Jobs:             as.GetJobsByIndex(jobIndex),
	}
}

// AnalysisStatus return the analysis status
func (m *Mgr) AnalysisStatus() *types.WorkAnalysisStatus {
	return m.analysisStatus
}

// ResetStat reset stat
func (m *Mgr) ResetStat() error {
	return m.analysisStatus.Reset()
}

// SetToolChain : save the ToolChain if local file list changed, do not care remote path change now
func (m *Mgr) SetToolChain(toolchain *types.ToolChain) error {
	m.toolchainLock.Lock()
	defer m.toolchainLock.Unlock()

	// replace taskid of remote path if necessary
	uniqid := m.settings.TaskID
	if uniqid == "" {
		uniqid = fmt.Sprintf("toolchain_%s_%d",
			util.RandomString(types.WorkIDLength), time.Now().Local().UnixNano())
	}
	_ = replaceTaskID(uniqid, toolchain)

	// get local file list of tool chain
	newfiles, err := getToolChainFiles(toolchain)
	if err != nil {
		blog.Errorf("basic: failed to found all new files for tool chain with key: %s "+
			"with error: %v, do not update", toolchain.ToolKey, err)
		return err
	}

	if newfiles == nil || len(newfiles) == 0 {
		blog.Errorf("basic: local file list is empty for tool chain with key:%s, do not update",
			toolchain.ToolKey)
		return err
	}
	blog.Infof("basic: tool chain with key:%s contains [%d] files", toolchain.ToolKey, len(newfiles))

	// uniq files
	newfiles, _ = uniqFiles(newfiles)

	// check whether the local file list changed
	cachedata, ok := m.toolchainMap[toolchain.ToolKey]
	if ok {
		blog.Infof("basic: existed tool chain with key:%s, ready to check and update it now", toolchain.ToolKey)

		same, diffdesc, _ := diffToolChainFiles(cachedata.files, &newfiles)
		if !same {
			blog.Infof("basic: file list of tool chain with key:%s changed, diff detail: [%s]",
				toolchain.ToolKey, diffdesc)
		} else {
			blog.Infof("basic: file list of tool chain with key:%s not changed, do nothing now",
				toolchain.ToolKey)
			return nil
		}
	}

	// update tool chain cache
	var totalsize int64
	for _, v := range newfiles {
		totalsize += v.FileSize
	}
	blog.Infof("basic: add tool chain with key:%s [%d] files total size[%d] to cache",
		toolchain.ToolKey, len(newfiles), totalsize)
	m.toolchainMap[toolchain.ToolKey] = toolchainCache{
		toolchain: toolchain,
		files:     &newfiles,
	}

	return nil
}

// GetToolChainFiles return the toolchain files
func (m *Mgr) GetToolChainFiles(key string) ([]dcSDK.FileDesc, int64, error) {
	m.toolchainLock.RLock()
	defer m.toolchainLock.RUnlock()

	v, ok := m.toolchainMap[key]
	if !ok {
		return nil, 0, fmt.Errorf("not found tool chain for key:%s", key)
	}

	return *(v.files), v.toolchain.Timestamp, nil
}

// GetToolChainRemotePath return the toolchain remote path according to provided key
func (m *Mgr) GetToolChainRemotePath(key string) (string, error) {
	m.toolchainLock.RLock()
	defer m.toolchainLock.RUnlock()

	v, ok := m.toolchainMap[key]
	if !ok {
		return "", fmt.Errorf("not found tool chain for key:%s", key)
	}

	return v.toolchain.ToolRemoteRelativePath, nil
}

// GetToolChainTimestamp return the toolchain timestamp according to provided key
func (m *Mgr) GetToolChainTimestamp(key string) (int64, error) {
	m.toolchainLock.RLock()
	defer m.toolchainLock.RUnlock()

	v, ok := m.toolchainMap[key]
	if !ok {
		return 0, fmt.Errorf("not found tool chain for key:%s", key)
	}

	return v.toolchain.Timestamp, nil
}
