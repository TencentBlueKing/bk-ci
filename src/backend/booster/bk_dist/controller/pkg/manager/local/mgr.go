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
	"fmt"
	"strings"
	"time"

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
		ctx:               ctx,
		work:              work,
		resource:          newResource(0, nil),
		pumpFileCache:     analyser.NewFileCache(),
		pumpRootCache:     analyser.NewRootCache(),
		checkApplyTick:    1 * time.Second,
		checkApplyTimeout: 20 * time.Second,
	}
}

// Mgr describe the local manager
// provides the local actions handler for work
type Mgr struct {
	ctx context.Context

	work     *types.Work
	resource *resource
	// initCancel context.CancelFunc

	pumpFileCache *analyser.FileCache
	pumpRootCache *analyser.RootCache

	recorder *recorder.Recorder

	checkApplyTick    time.Duration
	checkApplyTimeout time.Duration
}

// Init do the initialization for local manager
func (m *Mgr) Init() {
	blog.Infof("local: init for work:%s", m.work.ID())
}

// Start start resource slots for local manager
func (m *Mgr) Start() {
	blog.Infof("local: start for work:%s", m.work.ID())

	settings := m.work.Basic().Settings()
	m.resource = newResource(settings.LocalTotalLimit, settings.UsageLimit)

	m.recorder, _ = m.work.GetRecorder(types.GlobalRecorderKey)

	// if m.initCancel != nil {
	// 	m.initCancel()
	// }
	ctx, _ := context.WithCancel(m.ctx)
	// m.initCancel = cancel

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

	// 该work被置为degraded || 该executor被置为degraded, 则直接走本地执行
	if m.work.Basic().Settings().Degraded || e.degrade() {
		blog.Warnf("local: execute task for work(%s) from pid(%d) degrade to local with degraded",
			m.work.ID(), req.Pid)
		return e.executeLocalTask(), nil
	}

	// 历史记录显示该任务多次远程失败，则直接走本地执行
	if e.retryAndSuccessTooManyAndDegradeDirectly() {
		blog.Warnf("local: execute task for work(%s) from pid(%d) degrade to local for too many failed",
			m.work.ID(), req.Pid)
		return e.executeLocalTask(), nil
	}

	// 该任务已确定用本地资源运行，则直接走本地执行
	if withlocalresource {
		blog.Infof("local: execute task for work(%s) from pid(%d) degrade to local for with local resource",
			m.work.ID(), req.Pid)
		return e.executeLocalTask(), nil
	}

	// 没有申请到资源(或资源已释放) || 申请到资源但都失效了
	// if !m.work.Resource().HasAvailableWorkers() ||
	// 	m.work.Remote().TotalSlots() <= 0 {
	// !! 去掉申请到资源但失效的情况，因为该情况很可能是网络原因，再加资源也没有意义
	if !m.work.Resource().HasAvailableWorkers() {
		// check whether this task need remote worker,
		// apply resource when need, if not in appling, apply then
		if e.needRemoteResource() {
			_, err := m.work.Resource().Apply(nil, false)
			if err != nil {
				blog.Warnf("local: execute task for work(%s) from pid(%d) failed to apply resource with err:%v",
					m.work.ID(), req.Pid, err)
			}
		}

		blog.Infof("local: execute task for work(%s) from pid(%d) degrade to local for no remote workers",
			m.work.ID(), req.Pid)
		return e.executeLocalTask(), nil
	}

	// TODO : check whether need more resource

	// !! remember dec after finished remote execute !!
	m.work.Basic().Info().IncPrepared()
	m.work.Remote().IncRemoteJobs()

	c, err := e.executePreTask()
	if err != nil {
		m.work.Basic().Info().DecPrepared()
		m.work.Remote().DecRemoteJobs()
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
	m.work.Remote().DecRemoteJobs()
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

func (m *Mgr) waitApplyFinish() error {
	ctx, _ := context.WithCancel(m.ctx)
	blog.Infof("local: run wait apply finish tick for work(%s)", m.work.ID())
	ticker := time.NewTicker(m.checkApplyTick)
	defer ticker.Stop()
	timer := time.NewTimer(m.checkApplyTimeout)
	defer timer.Stop()

	for {
		select {
		case <-ctx.Done():
			blog.Infof("local: run wait apply finish tick  for work(%s) canceled by context", m.work.ID())
			return fmt.Errorf("canceld by context")

		case <-ticker.C:
			// get apply status
			if m.work.Resource().IsApplyFinished() {
				return nil
			}

		case <-timer.C:
			// check timeout
			blog.Infof("local: wait apply status timeout for work(%s)", m.work.ID())
			return fmt.Errorf("wait apply status timeout")
		}
	}
}
