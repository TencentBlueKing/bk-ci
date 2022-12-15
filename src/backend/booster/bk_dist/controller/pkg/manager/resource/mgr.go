/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package resource

import (
	"context"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"time"

	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/compress"
	commonHTTP "github.com/Tencent/bk-ci/src/booster/common/http"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
	v2 "github.com/Tencent/bk-ci/src/booster/server/pkg/api/v2"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"
)

// NewMgr get a new ResourceMgr
func NewMgr(pCtx context.Context, work *types.Work) types.ResourceMgr {
	cli := httpclient.NewHTTPClient()
	cli.SetHeader("Content-Type", "application/json")
	cli.SetHeader("Accept", "application/json")

	ctx, _ := context.WithCancel(pCtx)
	mgr := Mgr{
		work:          work,
		ctx:           ctx,
		client:        cli,
		heartbeatTick: 5 * time.Second,
		infoTick:      100 * time.Millisecond,
		resources:     make([]*Res, 0),
		releaseTick:   30 * time.Second,
		callbacks:     make([]types.CB4ResChanged, 0),
		applystatus:   ResourceInit,
		// maybe should adjust cool time
		applyCoolSeconds: 10,
		newlyTaskID:      "",
	}

	mgr.RegisterCallback(mgr.callback4ResChanged)

	return &mgr
}

const (
	applyDistributeResourcesURI   = "v2/build/apply"
	releaseDistributeResourcesURI = "v2/build/release"
	inspectDistributeTaskURI      = "v2/build/task?task_id=%s"
	heartbeatURI                  = "v2/build/heartbeat"
	messageURI                    = "v2/build/message"
)

// Mgr describe the resource manager
// provides the actions to server and resource
type Mgr struct {
	work *types.Work

	ctx context.Context

	client         *httpclient.HTTPClient
	serverHost     string
	lastapplyparam *v2.ParamApply

	releaseTick time.Duration

	infoTick time.Duration

	heartbeatlaunched bool
	heartbeatTick     time.Duration
	// heartbeatCancel   context.CancelFunc

	reslock   sync.RWMutex
	resources []*Res

	hasAvailableWorkers bool

	callbacklock sync.RWMutex
	callbacks    []types.CB4ResChanged

	applylock         sync.RWMutex
	applystatus       Status
	applyCoolSeconds  int64
	applyLastFailTime int64

	newlyTaskID string
}

// IsApplyFinished check whether apply finished
func (m *Mgr) IsApplyFinished() bool {
	m.applylock.RLock()
	defer m.applylock.RUnlock()

	return m.applystatus != ResourceApplying
}

// HasAvailableWorkers check if there are available ready workers
func (m *Mgr) HasAvailableWorkers() bool {
	return m.hasAvailableWorkers
}

// call this when resource changed
func (m *Mgr) updateAvailableStatus() error {
	for _, r := range m.resources {
		if (r.status == ResourceApplySucceed || r.status == ResourceSpecified) &&
			len(r.taskInfo.HostList) > 0 {
			m.hasAvailableWorkers = true
			return nil
		}
	}

	m.hasAvailableWorkers = false
	return nil
}

// SetServerHost set the tbs-server target
func (m *Mgr) SetServerHost(serverHost string) {
	m.serverHost = serverHost
}

// GetStatus return the resource task status
func (m *Mgr) GetStatus() *v2.RespTaskInfo {
	m.reslock.Lock()
	defer m.reslock.Unlock()

	// return the latest task
	if len(m.resources) > 0 {
		return m.resources[len(m.resources)-1].taskInfo
	}

	return nil
}

// GetNewlyTaskID get newly task id
func (m *Mgr) GetNewlyTaskID() string {
	return m.newlyTaskID
}

// SetSpecificHosts set the specific worker list instead of applying
func (m *Mgr) SetSpecificHosts(hostList []string) {
	m.reslock.Lock()
	defer func() {
		m.reslock.Unlock()
		m.onResChanged()
	}()
	m.resources = append(m.resources, &Res{
		taskid: "",
		status: ResourceSpecified,
		taskInfo: &v2.RespTaskInfo{
			Status:   engine.TaskStatusRunning,
			HostList: hostList,
		},
	})

	info := m.work.Basic().Info()
	if info.CanBeResourceApplied() {
		info.ResourceApplied()
	}

	// m.onResChanged()
}

// GetHosts return the worker list
func (m *Mgr) GetHosts() []*dcProtocol.Host {
	if m.resources == nil {
		return nil
	}

	m.reslock.RLock()
	defer m.reslock.RUnlock()

	hosts := make([]*dcProtocol.Host, 0)
	for _, r := range m.resources {
		if (r.status == ResourceApplySucceed || r.status == ResourceSpecified) &&
			len(r.taskInfo.HostList) > 0 {
			for _, v := range r.taskInfo.HostList {
				hostField := strings.Split(v, "/")

				if len(hostField) < 2 {
					blog.Warnf("resource: got invalid host %s", v)
					continue
				}

				jobs, err := strconv.Atoi(hostField[1])
				if err != nil {
					blog.Warnf("resource: got invalid jobs for host %s", v)
					continue
				}

				host := &dcProtocol.Host{
					Server:       hostField[0],
					TokenString:  hostField[0],
					Hosttype:     dcProtocol.HostRemote,
					Jobs:         jobs,
					Compresstype: dcProtocol.CompressLZ4,
					Protocol:     "tcp",
				}

				hosts = append(hosts, host)
			}
		}
	}

	return hosts
}

func (m *Mgr) checkAndUpdateApplyStatus() bool {
	m.applylock.Lock()
	defer m.applylock.Unlock()

	// check whether appling
	if m.applystatus == ResourceApplying {
		return false
	}

	// check whether in cool time
	if m.applyLastFailTime > 0 && time.Now().Unix()-m.applyLastFailTime < m.applyCoolSeconds {
		return false
	}

	m.applystatus = ResourceApplying
	return true
}

func (m *Mgr) updateApplyEndStatus(succeed bool) {
	m.applylock.Lock()
	defer m.applylock.Unlock()

	if succeed {
		m.applystatus = ResourceApplySucceed
		m.applyLastFailTime = 0
	} else {
		m.applystatus = ResourceApplyFailed
		m.applyLastFailTime = time.Now().Unix()
	}
}

// Apply do the resource apply to tbs-server
func (m *Mgr) Apply(req *v2.ParamApply, force bool) (*v2.RespTaskInfo, error) {
	blog.Infof("resource: try to apply dist-resource for work(%s)", m.work.ID())

	// support req with nil
	if req != nil {
		m.lastapplyparam = req
	} else {
		req = m.lastapplyparam
	}

	if req == nil {
		err := fmt.Errorf("param is nil")
		blog.Errorf("resource: apply dist-resource failed for work(%s) error:%v", m.work.ID(), err)
		return nil, err
	}

	// 确保只有一个apply生效中，避免多个apply同时运行
	// 另外还需要设置一个失败后的冷却期，比如30秒后才能下次申请，避免频繁触发导致server端的压力
	if !force && !m.checkAndUpdateApplyStatus() {
		if m.applystatus == ResourceApplying {
			blog.Infof("resource: appling by others now for work(%s), do nothing now", m.work.ID())
			return nil, nil
		}
		err := fmt.Errorf("in apply failed cool time")
		blog.Infof("resource: apply dist-resource failed for work(%s) error:%v", m.work.ID(), err)
		return nil, nil
	}

	var data []byte
	_ = codec.EncJSON(req, &data)
	blog.Infof("resource: try to apply dist-resource for work(%s) info: %s", m.work.ID(), string(data))

	resp, _, err := m.request("POST", m.serverHost, applyDistributeResourcesURI, data)
	if err != nil {
		blog.Errorf("resource: apply dist-resource failed for work(%s): %v", m.work.ID(), err)
		m.updateApplyEndStatus(false)
		return nil, err
	}

	var server v2.RespTaskInfo
	if err = codec.DecJSON(resp, &server); err != nil {
		m.updateApplyEndStatus(false)
		return nil, err
	}

	m.reslock.Lock()
	m.resources = append(m.resources, &Res{
		taskid:        server.TaskID,
		status:        ResourceApplying,
		taskInfo:      nil,
		heartbeatInfo: nil,
		applyTime:     time.Now(),
	})
	m.reslock.Unlock()

	// 启动心跳维持协程, 直到work被取消, 或心跳被取消
	// ctx, cancel := context.WithCancel(m.ctx)
	if !m.heartbeatlaunched {
		m.heartbeatlaunched = true
		// m.heartbeatCancel = cancel
		go m.heartbeat()
	}
	go m.inspectInfo(server.TaskID)

	blog.Infof("resource: success to apply dist-resource for work(%s) with taskID(%s)",
		m.work.ID(), server.TaskID)
	return &server, nil
}

// Release release all resource by this function
func (m *Mgr) Release(req *v2.ParamRelease) error {
	blog.Infof("resource: try to release all dist-resource for work(%s)", m.work.ID())

	if req == nil {
		req = &v2.ParamRelease{
			Success: true,
			Message: "",
			Extra:   "",
		}
	}

	m.reslock.Lock()
	resourcechanged := false
	defer func() {
		m.reslock.Unlock()
		if resourcechanged {
			m.onResChanged()
		}
	}()

	for _, r := range m.resources {
		if r.canRelease() {
			resourcechanged = true
			r.status = ResourceReleasing
			err := m.sendReleaseReq(req, r)
			if err != nil {
				r.status = ResourceReleaseFailed
				go m.releaseTimer(req, r)
			} else {
				r.status = ResourceReleaseSucceed
			}
			// should remove from array ???
		}
	}

	// if m.heartbeatCancel != nil {
	// 	m.heartbeatCancel()
	// }

	// not return error here, for it will try again auto
	return nil
}

// releaseOne release one resouce
func (m *Mgr) releaseOne(req *v2.ParamRelease, r *Res) error {
	blog.Infof("resource: try to release one dist-resource task(%s) status(%s) for work(%s)",
		r.taskid, r.status.String(), m.work.ID())

	if !r.canRelease() {
		return nil
	}

	if req == nil {
		req = &v2.ParamRelease{
			Success: true,
			Message: "",
			Extra:   "",
		}
	}

	r.status = ResourceReleasing
	err := m.sendReleaseReq(req, r)
	if err != nil {
		r.status = ResourceReleaseFailed
		go m.releaseTimer(req, r)

		return err
	}

	r.status = ResourceReleaseSucceed
	// should remove from array ???

	return nil
}

func (m *Mgr) sendReleaseReq(req *v2.ParamRelease, r *Res) error {
	blog.Infof("resource: try to send release request for task(%s) for work(%s)", r.taskid, m.work.ID())

	req.TaskID = r.taskid
	var data []byte
	_ = codec.EncJSON(req, &data)
	blog.Debugf("resource: release dist-resource task(%s) for work(%s) info: %s",
		req.TaskID, m.work.ID(), string(data))

	_, _, err := m.request("POST", m.serverHost, releaseDistributeResourcesURI, data)
	if err != nil {
		blog.Warnf("resource: release dist-resource task(%s) for work(%s) failed: %v",
			req.TaskID, m.work.ID(), err)
		return err
	}

	blog.Infof("resource: success to send release request for task(%s) for work(%s)",
		req.TaskID, m.work.ID())

	return nil
}

func (m *Mgr) releaseTimer(req *v2.ParamRelease, r *Res) {
	ctx, _ := context.WithCancel(m.ctx)
	blog.Infof("resource: run release tick for work: %s taskid:%s", m.work.ID(), r.taskid)
	ticker := time.NewTicker(m.releaseTick)
	defer ticker.Stop()

	index := 0
	for {
		select {
		case <-ctx.Done():
			blog.Infof("resource: run release timer for work(%s) canceled by context", m.work.ID())
			return

		case <-ticker.C:
			if r.canRelease() {
				index++
				blog.Infof("resource: try to send release request for task(%s) for work(%s) for %d times",
					r.taskid, m.work.ID(), index)
				err := m.sendReleaseReq(req, r)
				if err != nil {
					r.status = ResourceReleaseFailed
				} else {
					r.status = ResourceReleaseSucceed
					return
				}
			}

		}
	}
}

// SendStats do then stats sending to tbs-server
func (m *Mgr) SendStats(brief bool) error {
	// TODO : should lock work here, but locked before call this
	info := m.work.Basic().Info()
	cs := info.CommonStatus()
	as := m.work.Basic().AnalysisStatus()
	jobs := disttask.EmptyJobs

	jobbytes := []byte{}
	if !brief {
		jobbytes = as.DumpJobs()
	}

	scene := info.Scene()
	success := info.Success()
	projectID := info.ProjectID()
	// taskID := info.TaskID()
	taskID := m.newlyTaskID
	workID := info.WorkID()

	messageExtra := &disttask.Message{
		Type: disttask.MessageTypeTaskStats,
		MessageTaskStats: disttask.MessageTaskStats{
			WorkID:           workID,
			TaskID:           taskID,
			Scene:            scene,
			Success:          success,
			StartTime:        cs.StartTime.Local().UnixNano(),
			EndTime:          cs.EndTime.Local().UnixNano(),
			RegisteredTime:   cs.RegisteredTime.Local().UnixNano(),
			UnregisteredTime: cs.UnregisteredTime.Local().UnixNano(),
			Jobs:             jobs,
		},
	}

	messageExtra.MessageTaskStats.JobRemoteOK,
		messageExtra.MessageTaskStats.JobRemoteError,
		messageExtra.MessageTaskStats.JobLocalOK,
		messageExtra.MessageTaskStats.JobLocalError = as.BasicCount()

	if !brief {
		jobs = compress.ToBase64String(jobbytes)
		messageExtra.MessageTaskStats.Jobs = jobs
	}

	var tmp []byte
	_ = codec.EncJSON(messageExtra, &tmp)

	message := &v2.ParamMessage{
		Type:      v2.MessageProject,
		ProjectID: projectID,
		Scene:     scene,
		Extra:     string(tmp),
	}

	var data []byte
	_ = codec.EncJSON(message, &data)

	if _, _, err := m.request("POST", m.serverHost, messageURI, data); err != nil {
		blog.Errorf("resource: send stats(detail %v) to server for task(%s) work(%s) failed: %v",
			brief, taskID, workID, err)
		return err
	}

	blog.Infof("resource: success to send stats detail %v to server for task(%s) work(%s)",
		brief, taskID, workID)
	return nil
}

// send stats and reset after sent, if brief true, then will not send the job stats
// !! this will call m.work.Lock() , to avoid dead lock
func (m *Mgr) SendAndResetStats(brief bool, resapplytimes []int64) error {

	for _, t := range resapplytimes {
		data, _ := m.getSendStatsData(brief, t)
		go m.sendStatsData(data)

		// reset stat
		m.work.Lock()
		m.work.Basic().ResetStat()
		m.work.Unlock()
	}

	return nil
}

// get stat data which ready to send
func (m *Mgr) getSendStatsData(brief bool, t int64) (*[]byte, error) {
	m.work.Lock()
	info := m.work.Basic().Info()
	cs := info.CommonStatus()
	as := m.work.Basic().AnalysisStatus()
	jobs := disttask.EmptyJobs

	jobbytes := []byte{}
	if !brief {
		jobbytes = as.DumpJobs()
	}

	scene := info.Scene()
	success := info.Success()
	projectID := info.ProjectID()
	// taskID := info.TaskID()
	taskID := m.newlyTaskID
	workID := info.WorkID()

	endTime := cs.EndTime.Local().UnixNano()
	if endTime <= 0 {
		endTime = time.Now().UnixNano()
	}
	registeredTime := cs.RegisteredTime.Local().UnixNano()
	if registeredTime <= 0 {
		registeredTime = t
	}
	unregisteredTime := cs.UnregisteredTime.Local().UnixNano()
	if unregisteredTime <= 0 {
		unregisteredTime = time.Now().UnixNano()
	}

	messageExtra := &disttask.Message{
		Type: disttask.MessageTypeTaskStats,
		MessageTaskStats: disttask.MessageTaskStats{
			WorkID:           workID,
			TaskID:           taskID,
			Scene:            scene,
			Success:          success,
			StartTime:        cs.StartTime.Local().UnixNano(),
			EndTime:          endTime,
			RegisteredTime:   registeredTime,
			UnregisteredTime: unregisteredTime,
			Jobs:             jobs,
		},
	}

	messageExtra.MessageTaskStats.JobRemoteOK,
		messageExtra.MessageTaskStats.JobRemoteError,
		messageExtra.MessageTaskStats.JobLocalOK,
		messageExtra.MessageTaskStats.JobLocalError = as.BasicCount()

	m.work.Unlock()

	if !brief {
		jobs = compress.ToBase64String(jobbytes)
		messageExtra.MessageTaskStats.Jobs = jobs
	}

	var tmp []byte
	_ = codec.EncJSON(messageExtra, &tmp)

	message := &v2.ParamMessage{
		Type:      v2.MessageProject,
		ProjectID: projectID,
		Scene:     scene,
		Extra:     string(tmp),
	}

	var data []byte
	_ = codec.EncJSON(message, &data)

	return &data, nil
}

func (m *Mgr) sendStatsData(data *[]byte) error {
	if _, _, err := m.request("POST", m.serverHost, messageURI, *data); err != nil {
		blog.Errorf("resource: send stats server failed: %v", err)
		return err
	}

	blog.Infof("resource: success to send stat data")
	return nil
}

func (m *Mgr) heartbeat() {
	blog.Infof("resource: run heartbeat tick for work: %s", m.work.ID())
	ctx, _ := context.WithCancel(m.ctx)
	hbTicker := time.NewTicker(m.heartbeatTick)
	defer hbTicker.Stop()

	for {
		select {
		case <-ctx.Done():
			blog.Infof("resource: run heartbeat for work(%s) canceled by context", m.work.ID())
			m.heartbeatlaunched = false
			return

		case <-hbTicker.C:
			m.reslock.RLock()
			for _, r := range m.resources {
				if r.needHeartBeat() {
					go func(data []byte) {
						if _, _, err := m.request("POST", m.serverHost, heartbeatURI, data); err != nil {
							blog.Errorf("resource: heartbeat to task(%s) work(%s) failed with error: %v", r.taskid, m.work.ID(), err)
						}
					}(r.heartbeatData())
				}
			}
			m.reslock.RUnlock()
		}
	}
}

// inspectInfo 会调用work.Lock, 因此这个函数不应该被直接被提供给remote外的用户调用
func (m *Mgr) inspectInfo(taskID string) {
	blog.Infof("resource: run info inspect tick: %s", taskID)
	ctx, _ := context.WithCancel(m.ctx)
	infoTicker := time.NewTicker(m.infoTick)
	defer infoTicker.Stop()

	for {
		select {
		case <-ctx.Done():
			blog.Infof("resource: run info inspect tick for task(%s) canceled by context", taskID)
			m.updateApplyEndStatus(false)
			return

		case <-infoTicker.C:
			data, _, err := m.request("GET", m.serverHost,
				fmt.Sprintf(inspectDistributeTaskURI, taskID), nil)
			if err != nil {
				blog.Errorf("resource: inspect to get task(%s) info failed: %v", taskID, err)
				continue
			}

			var info v2.RespTaskInfo
			if err = codec.DecJSON(data, &info); err != nil {
				blog.Errorf("resource: inspect to get task(%s) info decode failed: %v", taskID, err)
				continue
			}

			// blog.Infof("resource: inspectInfo get task info: %+v", info)

			// Once task is running or terminated, no need keep inspecting info.
			switch info.Status {
			case engine.TaskStatusRunning:
				// we should set ResourceApplyFailed if len(info.HostList) == 0
				s := ResourceApplySucceed
				if len(info.HostList) == 0 {
					s = ResourceApplyFailed
				}

				resapplytimes, err := m.clearOldInvalidRes(&info)
				if err == nil {
					m.SendAndResetStats(false, resapplytimes)
				}
				m.addRes(&info, s)

				m.updateApplyEndStatus(s == ResourceApplySucceed)
				blog.Infof("resource: success to apply resources and get host(%d): %v",
					len(info.HostList), info.HostList)
				return

			case engine.TaskStatusFinish, engine.TaskStatusFailed:
				resapplytimes, err := m.clearOldInvalidRes(&info)
				if err == nil {
					m.SendAndResetStats(false, resapplytimes)
				}
				m.addRes(&info, ResourceApplyFailed)

				m.updateApplyEndStatus(false)
				blog.Infof("resource: get task terminated in %s, %s", info.Status, info.Message)
				return
			}
		}
	}
}

// clean old resources which host list is empty, and notify server to terminate these resources
func (m *Mgr) clearOldInvalidRes(info *v2.RespTaskInfo) ([]int64, error) {
	blog.Infof("resource: ready check and clean old invalid resource")

	m.reslock.Lock()
	defer m.reslock.Unlock()

	if len(m.resources) == 0 {
		return nil, nil
	}

	needrelease := false
	for _, r := range m.resources {
		if r.status == ResourceInit || r.status == ResourceApplying {
			continue
		}

		if r.taskInfo != nil && len(r.taskInfo.HostList) == 0 && r.taskid != info.TaskID {
			needrelease = true
		}
	}

	if !needrelease {
		return nil, nil
	}

	resapplytimes := []int64{}
	newres := []*Res{}
	for _, r := range m.resources {
		// do nothing with current task info, it mabye need by others
		if r.taskid == info.TaskID {
			newres = append(newres, r)
			continue
		}

		if len(r.taskInfo.HostList) == 0 {
			m.releaseOne(nil, r)
			// 确保 m.reslock.Lock() 和 m.work.Lock() 不要相互包含，避免死锁
			// TODO : send detail stat data and reset stat data
			// m.SendAndResetStats(false, r.applyTime.UnixNano())
			resapplytimes = append(resapplytimes, r.applyTime.UnixNano())

		} else {
			newres = append(newres, r)
		}
	}
	m.resources = newres

	return resapplytimes, nil
}

func (m *Mgr) addRes(info *v2.RespTaskInfo, status Status) error {
	if info == nil {
		return nil
	}

	// TODO : reset stat data to new status
	// 更新work info状态
	m.work.Lock()
	workinfo := m.work.Basic().Info()
	if status == ResourceApplySucceed {
		if workinfo.CanBeResourceApplied() {
			workinfo.ResourceApplied()
		}
	} else {
		if workinfo.CanBeResourceApplyFailed() {
			workinfo.ResourceApplyFailed()
		}
	}
	// update start time
	workinfo.StartTime(time.Now())
	for _, r := range m.resources {
		if r.taskid == info.TaskID {
			workinfo.RegisterTime(r.applyTime)
			break
		}
	}
	m.work.Unlock()

	changed := false
	m.reslock.Lock()
	defer func() {
		m.reslock.Unlock()
		if changed {
			m.onResChanged()
		}
	}()

	// send stat data with the newly taskid(resource id)
	m.newlyTaskID = info.TaskID

	for _, r := range m.resources {
		if r.taskid == info.TaskID {
			// if resource in release status, do not change it
			if r.isReleaseStatus() {
				blog.Infof("resource: not add task:%s for status(%s) work:%s", r.taskid, r.status.String(), m.work.ID())
				return nil
			}

			if status == ResourceApplySucceed {
				// check whether host list changed
				if r.taskInfo == nil {
					changed = true
				} else if len(info.HostList) != len(r.taskInfo.HostList) {
					changed = true
				} else {
					for _, v1 := range r.taskInfo.HostList {
						found := false
						for _, v2 := range info.HostList {
							if v1 == v2 {
								found = true
								break
							}
						}
						if !found {
							changed = true
							break
						}
					}
				}
			}

			r.status = status
			r.taskInfo = info

			blog.Infof("resource: add task:%s status(%s) work:%s", r.taskid, status.String(), m.work.ID())
			return nil
		}
	}

	blog.Warnf("resource: add task:%s status(%s) work:%s but not found taskid in resources?", info.TaskID, status.String(), m.work.ID())
	m.resources = append(m.resources, &Res{
		taskid:   info.TaskID,
		status:   status,
		taskInfo: info,
	})

	if status == ResourceApplySucceed {
		changed = true
	}

	return nil
}

func (m *Mgr) onResChanged() error {
	blog.Infof("resource: resource changed for work:%s", m.work.ID())

	m.callbacklock.RLock()
	defer m.callbacklock.RUnlock()

	for _, f := range m.callbacks {
		err := f()
		if err != nil {
			blog.Warnf("resource: callback function failed with error:%v for work:%s", err, m.work.ID())
		}
	}

	return nil
}

func (m *Mgr) callback4ResChanged() error {
	blog.Infof("resource: resource changed call back for work:%s", m.work.ID())

	err := m.updateAvailableStatus()
	if err != nil {
		blog.Warnf("resource: callback function failed with error:%v for work:%s", err, m.work.ID())
	}

	return nil
}

// RegisterCallback regist call back function
func (m *Mgr) RegisterCallback(f types.CB4ResChanged) error {

	m.callbacklock.Lock()
	defer m.callbacklock.Unlock()

	m.callbacks = append(m.callbacks, f)

	return nil
}

// request send request to server and get return data.
// if requests failed, http code is not 200, return flag false.
// else once the http code is 200, return flat true.
// Only both flag is true and error is nil, guarantees that the data is not nil.
func (m *Mgr) request(method, server, uri string, data []byte) ([]byte, bool, error) {
	uri = fmt.Sprintf("%s/%s", server, uri)
	blog.Debugf("booster: method(%s), server(%s) uri(%s), data: %s", method, server, uri, string(data))

	var resp *httpclient.HttpResponse
	var err error

	switch method {
	case "GET":
		resp, err = m.client.Get(uri, nil, data)

	case "POST":
		resp, err = m.client.Post(uri, nil, data)

	case "DELETE":
		resp, err = m.client.Delete(uri, nil, data)

	case "PUT":
		resp, err = m.client.Put(uri, nil, data)

	default:
		err = fmt.Errorf("uri %s method %s is invalid", uri, method)
	}

	if err != nil {
		return nil, false, err
	}

	if resp.StatusCode != http.StatusOK {
		return nil, false, fmt.Errorf("%s", string(resp.Reply))
	}

	var apiResp *commonHTTP.APIResponse
	if err = codec.DecJSON(resp.Reply, &apiResp); err != nil {
		return nil, true, fmt.Errorf("decode request %s response %s error %s",
			uri, string(resp.Reply), err.Error())
	}

	if apiResp.Code != common.RestSuccess {
		return nil, true, fmt.Errorf(apiResp.Message)
	}

	var by []byte
	if err = codec.EncJSON(apiResp.Data, &by); err != nil {
		return nil, true, fmt.Errorf("encode apiResp.Data error %s", err.Error())
	}

	return by, true, nil
}
