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
	return &Mgr{
		work:          work,
		ctx:           ctx,
		client:        cli,
		heartbeatTick: 5 * time.Second,
		infoTick:      100 * time.Millisecond,
	}
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

	client        *httpclient.HTTPClient
	serverHost    string
	heartbeatTick time.Duration
	infoTick      time.Duration

	launched        bool
	released        bool
	heartbeatCancel context.CancelFunc

	taskInfo *v2.RespTaskInfo
}

// HasAvailableWorkers check if there are available ready workers
func (m *Mgr) HasAvailableWorkers() bool {
	return m.launched && len(m.taskInfo.HostList) > 0
}

// SetServerHost set the tbs-server target
func (m *Mgr) SetServerHost(serverHost string) {
	m.serverHost = serverHost
}

// GetStatus return the resource task status
func (m *Mgr) GetStatus() *v2.RespTaskInfo {
	return m.taskInfo
}

// SetSpecificHosts set the specific worker list instead of applying
func (m *Mgr) SetSpecificHosts(hostList []string) {
	if m.launched {
		return
	}

	m.taskInfo = &v2.RespTaskInfo{
		Status:   engine.TaskStatusRunning,
		HostList: hostList,
	}

	m.launched = true

	info := m.work.Basic().Info()
	if info.CanBeResourceApplied() {
		info.ResourceApplied()
	}
}

// GetHosts return the worker list
func (m *Mgr) GetHosts() []*dcProtocol.Host {
	if m.taskInfo == nil {
		return nil
	}

	hosts := make([]*dcProtocol.Host, 0)
	for _, v := range m.taskInfo.HostList {
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

	return hosts
}

// Apply do the resource apply to tbs-server
func (m *Mgr) Apply(req *v2.ParamApply) (*v2.RespTaskInfo, error) {
	var data []byte
	_ = codec.EncJSON(req, &data)
	blog.Infof("resource: try to apply dist-resource for work(%s) info: %s", m.work.ID(), string(data))

	resp, _, err := m.request("POST", m.serverHost, applyDistributeResourcesURI, data)
	if err != nil {
		blog.Errorf("resource: apply dist-resource failed for work(%s): %v", m.work.ID(), err)
		return nil, err
	}

	var server v2.RespTaskInfo
	if err = codec.DecJSON(resp, &server); err != nil {
		return nil, err
	}

	// 启动心跳维持协程, 直到work被取消, 或心跳被取消
	ctx, cancel := context.WithCancel(m.ctx)
	m.heartbeatCancel = cancel
	m.launched = true
	go m.heartbeat(ctx, server.TaskID)
	go m.inspectInfo(ctx, server.TaskID)

	blog.Infof("resource: success to apply dist-resource for work(%s) with taskID(%s)",
		m.work.ID(), server.TaskID)
	return &server, nil
}

// Release do the resource release to tbs-server
func (m *Mgr) Release(req *v2.ParamRelease) error {
	blog.Infof("resource: try to release dist-resource task(%s) for work(%s)", req.TaskID, m.work.ID())

	if !m.launched || m.taskInfo == nil {
		blog.Errorf("resource: release dist-resource task(%s) for work(%s) failed: task no launched",
			req.TaskID, m.work.ID())
		return types.ErrTaskCannotBeReleased
	}
	req.TaskID = m.taskInfo.TaskID

	if m.released {
		blog.Errorf("resource: release dist-resource task(%s) for work(%s) failed: task already released",
			req.TaskID, m.work.ID())
		return types.ErrTaskCannotBeReleased
	}

	defer func() {
		m.released = true
	}()

	var data []byte
	_ = codec.EncJSON(req, &data)
	blog.Debugf("resource: release dist-resource task(%s) for work(%s) info: %s",
		req.TaskID, m.work.ID(), string(data))

	if m.heartbeatCancel != nil {
		m.heartbeatCancel()
	}

	_, _, err := m.request("POST", m.serverHost, releaseDistributeResourcesURI, data)
	if err != nil {
		blog.Warnf("resource: release dist-resource task(%s) for work(%s) failed: %v",
			req.TaskID, m.work.ID(), err)
		return err
	}

	blog.Infof("resource: success to release dist-resource task(%s) for work(%s)",
		req.TaskID, m.work.ID())
	return nil
}

// SendStats do then stats sending to tbs-server
func (m *Mgr) SendStats(brief bool) error {
	info := m.work.Basic().Info()
	cs := info.CommonStatus()
	as := m.work.Basic().AnalysisStatus()
	jobs := disttask.EmptyJobs

	if !brief {
		jobs = compress.ToBase64String(as.DumpJobs())
	}

	messageExtra := &disttask.Message{
		Type: disttask.MessageTypeTaskStats,
		MessageTaskStats: disttask.MessageTaskStats{
			WorkID:           info.WorkID(),
			TaskID:           info.TaskID(),
			Scene:            info.Scene(),
			Success:          info.Success(),
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

	var tmp []byte
	_ = codec.EncJSON(messageExtra, &tmp)

	message := &v2.ParamMessage{
		Type:      v2.MessageProject,
		ProjectID: info.ProjectID(),
		Scene:     info.Scene(),
		Extra:     string(tmp),
	}

	var data []byte
	_ = codec.EncJSON(message, &data)

	if _, _, err := m.request("POST", m.serverHost, messageURI, data); err != nil {
		blog.Errorf("resource: send stats to server for task(%s) work(%s) failed: %v",
			info.TaskID(), info.WorkID(), err)
		return err
	}

	blog.Infof("resource: success to send stats to server for task(%s) work(%s)",
		info.TaskID(), info.WorkID())
	return nil
}

func (m *Mgr) heartbeat(ctx context.Context, taskID string) {
	blog.Infof("resource: run heartbeat tick: %s", taskID)
	hbTicker := time.NewTicker(m.heartbeatTick)
	defer hbTicker.Stop()

	var heartbeatInfo []byte
	_ = codec.EncJSON(v2.ParamHeartbeat{
		TaskID: taskID,
		Type:   v2.HeartBeatPing.String(),
	}, &heartbeatInfo)

	for {
		select {
		case <-ctx.Done():
			blog.Infof("resource: run heartbeat for task(%s) canceled by context", taskID)
			return

		case <-hbTicker.C:
			go func() {
				if _, _, err := m.request("POST", m.serverHost, heartbeatURI, heartbeatInfo); err != nil {
					blog.Errorf("resource: heartbeat to task(%s) failed: %v", taskID, err)
				}
			}()
		}
	}
}

// inspectInfo 会调用work.Lock, 因此这个函数不应该被直接被提供给remote外的用户调用
func (m *Mgr) inspectInfo(ctx context.Context, taskID string) {
	blog.Infof("resource: run info inspect tick: %s", taskID)
	infoTicker := time.NewTicker(m.infoTick)
	defer infoTicker.Stop()

	for {
		select {
		case <-ctx.Done():
			blog.Infof("resource: run info inspect tick for task(%s) canceled by context", taskID)
			return

		case <-infoTicker.C:
			data, _, err := m.request("GET", m.serverHost,
				fmt.Sprintf(inspectDistributeTaskURI, taskID), nil)
			if err != nil {
				blog.Errorf("resource: heartbeat to get task(%s) info failed: %v", taskID, err)
				continue
			}

			var info v2.RespTaskInfo
			if err = codec.DecJSON(data, &info); err != nil {
				blog.Errorf("resource: heartbeat to get task(%s) info decode failed: %v", taskID, err)
				continue
			}

			m.taskInfo = &info

			if m.taskInfo == nil {
				continue
			}

			// Once task is running or terminated, no need keep inspecting info.
			switch m.taskInfo.Status {
			case engine.TaskStatusRunning:
				// 更新work info状态
				m.work.Lock()
				info := m.work.Basic().Info()
				if info.CanBeResourceApplied() {
					info.ResourceApplied()
				}
				m.work.Unlock()
				blog.Infof("resource: success to apply resources and get host(%d): %v",
					len(m.taskInfo.HostList), m.taskInfo.HostList)

				return

			case engine.TaskStatusFinish, engine.TaskStatusFailed:
				// 更新work info状态
				m.work.Lock()
				info := m.work.Basic().Info()
				if info.CanBeResourceApplyFailed() {
					info.ResourceApplyFailed()
				}
				m.work.Unlock()

				blog.Infof("resource: get task terminated in %s, %s", m.taskInfo.Status, m.taskInfo.Message)
				return
			}
		}
	}
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
