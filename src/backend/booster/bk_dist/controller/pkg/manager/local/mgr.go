/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package local

import (
	"context"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager/recorder"

	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager/analyser"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// NewMgr get a new LocalMgr
func NewMgr(pCtx context.Context, work *types.Work) types.LocalMgr {
	ctx, _ := context.WithCancel(pCtx)

	return &Mgr{
		ctx:           ctx,
		work:          work,
		resource:      newResource(0, nil),
		pumpFileCache: analyser.NewFileCache(),
		pumpRootCache: analyser.NewRootCache(),
	}
}

// Mgr describe the local manager
// provides the local actions handler for work
type Mgr struct {
	ctx context.Context

	work       *types.Work
	resource   *resource
	initCancel context.CancelFunc

	pumpFileCache *analyser.FileCache
	pumpRootCache *analyser.RootCache

	recorder *recorder.Recorder
}

// Init do the initialization for local manager
func (m *Mgr) Init() {
	settings := m.work.Basic().Settings()
	m.resource = newResource(settings.LocalTotalLimit, settings.UsageLimit)

	m.recorder, _ = m.work.GetRecorder(types.GlobalRecorderKey)

	if m.initCancel != nil {
		m.initCancel()
	}
	ctx, cancel := context.WithCancel(m.ctx)
	m.initCancel = cancel

	m.resource.Handle(ctx)
}

// LockSlots lock a local slot
func (m *Mgr) LockSlots(usage dcSDK.JobUsage, weight int32) bool {
	return m.resource.Lock(usage, weight)
}

// UnlockSlots unlock a local slot
func (m *Mgr) UnlockSlots(usage dcSDK.JobUsage, weight int32) {
	m.resource.Unlock(usage, weight)
}

// GetPumpCache get pump cache in work
func (m *Mgr) GetPumpCache() (*analyser.FileCache, *analyser.RootCache) {
	return m.pumpFileCache, m.pumpRootCache
}

// ExecuteTask 若是task command本身运行失败, 不作为execute失败, 将结果放在result中返回即可
// 只有筹备执行的过程中失败, 才作为execute失败
func (m *Mgr) ExecuteTask(
	req *types.LocalTaskExecuteRequest,
	globalWork *types.Work,
	withlocalresource bool) (*types.LocalTaskExecuteResult, error) {
	blog.Infof("local: try to execute task(%s) for work(%s) from pid(%d) in env(%v) dir(%s)",
		strings.Join(req.Commands, " "), m.work.ID(), req.Pid, req.Environments, req.Dir)

	e, err := newExecutor(m, req, globalWork)
	if err != nil {
		blog.Errorf("local: try to execute task for work(%s) from pid(%d) get executor failed: %v",
			m.work.ID(), req.Pid, err)
		return nil, err
	}

	defer e.executeFinalTask()
	defer e.handleRecord()

	// 若
	// 1. 该work被置为degraded
	// 2. 该executor被置为degraded
	// 3. 远程无可用资源
	// 则直接走本地执行
	if m.work.Basic().Settings().Degraded ||
		e.degrade() ||
		!m.work.Resource().HasAvailableWorkers() ||
		e.retryAndSuccessTooManyAndDegradeDirectly() ||
		withlocalresource {
		blog.Warnf("local: execute pre-task for work(%s) from pid(%d) degrade to local", m.work.ID(), req.Pid)
		return e.executeLocalTask(), nil
	}

	m.work.Basic().Info().IncPrepared()
	c, err := e.executePreTask()
	if err != nil {
		m.work.Basic().Info().DecPrepared()
		blog.Warnf("local: execute pre-task for work(%s) from pid(%d) : %v", m.work.ID(), req.Pid, err)
		return e.executeLocalTask(), nil
	}

	var r *types.RemoteTaskExecuteResult
	for i := 0; i < e.remoteTryTimes(); i++ {
		req.Stats.RemoteTryTimes = i + 1
		r, err = m.work.Remote().ExecuteTask(&types.RemoteTaskExecuteRequest{
			Pid:       req.Pid,
			Req:       c,
			Stats:     req.Stats,
			Sandbox:   e.sandbox,
			IOTimeout: e.ioTimeout,
		})
		if err != nil {
			blog.Warnf("local: execute remote-task for work(%s) from pid(%d) (%d)try failed: %v", m.work.ID(), req.Pid, i, err)
			req.Stats.RemoteErrorMessage = err.Error()
			// do not retry if remote timeout
			if req.Stats.RemoteWorkTimeout {
				blog.Warnf("local: execute remote-task for work(%s) from pid(%d) (%d)try failed with remote timeout, error: %v",
					m.work.ID(), req.Pid, i, err)
				break
			}
		} else {
			break
		}
	}
	m.work.Basic().Info().DecPrepared()
	if err != nil {
		return e.executeLocalTask(), nil
	}

	err = e.executePostTask(r.Result)
	if err != nil {
		blog.Warnf("local: execute post-task for work(%s) from pid(%d) failed: %v", m.work.ID(), req.Pid, err)
		req.Stats.RemoteErrorMessage = err.Error()

		if !e.skipLocalRetry() {
			return e.executeLocalTask(), nil
		}

		blog.Warnf("local: executor skip local retry for work(%s) from pid(%d) "+
			"and return remote err directly: %v", m.work.ID(), req.Pid, err)
		return &types.LocalTaskExecuteResult{
			Result: &dcSDK.LocalTaskResult{
				ExitCode: 1,
				Stdout:   []byte(err.Error()),
				Stderr:   []byte(err.Error()),
				Message:  "executor skip local retry",
			},
		}, nil
	}

	req.Stats.Success = true
	m.work.Basic().UpdateJobStats(req.Stats)
	blog.Infof("local: success to execute task for work(%s) from pid(%d) in env(%v) dir(%s)",
		m.work.ID(), req.Pid, req.Environments, req.Dir)
	return &types.LocalTaskExecuteResult{
		Result: &dcSDK.LocalTaskResult{
			ExitCode: 0,
			Stdout:   e.Stdout(),
			Stderr:   e.Stderr(),
			Message:  "success to process all steps",
		},
	}, nil
}

// Slots get current total and occupied slots
func (m *Mgr) Slots() (int, int) {
	return m.resource.GetStatus()
}
