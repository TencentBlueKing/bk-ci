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
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager/recorder"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager/basic"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager/local"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager/remote"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager/resource"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/util"
	v2 "github.com/Tencent/bk-ci/src/booster/server/pkg/api/v2"
)

var (
	mgrSet = types.MgrSet{
		Basic:    basic.NewMgr,
		Local:    local.NewMgr,
		Remote:   remote.NewMgr,
		Resource: resource.NewMgr,
	}
)

func newWorksPool(conf *config.ServerConfig) *worksPool {
	return &worksPool{
		conf:          conf,
		works:         make(map[string]*types.Work),
		lastEmptyTime: time.Now().Local(),
		recordersPool: recorder.NewRecordersPool(),
	}
}

type worksPool struct {
	sync.RWMutex
	conf          *config.ServerConfig
	works         map[string]*types.Work
	lastEmptyTime time.Time

	recordersPool *recorder.RecordersPool
}

func (wp *worksPool) all() []*types.Work {
	wp.RLock()
	defer wp.RUnlock()

	r := make([]*types.Work, 0, 100)
	for _, work := range wp.works {
		r = append(r, work)
	}

	return r
}

func (wp *worksPool) getWork(workID string) (*types.Work, error) {
	wp.RLock()
	defer wp.RUnlock()

	work, ok := wp.works[workID]
	if !ok {
		return nil, types.ErrWorkNoFound
	}

	return work, nil
}

func (wp *worksPool) getFirstWork() (*types.Work, error) {
	wp.RLock()
	defer wp.RUnlock()

	for _, work := range wp.works {
		return work, nil
	}

	return nil, types.ErrNoWork
}

func (wp *worksPool) find(projectID, scene string, batchMode bool) *types.Work {
	wp.RLock()
	defer wp.RUnlock()

	for _, work := range wp.works {
		info := work.Basic().Info()
		if info.ProjectID() == projectID && info.Scene() == scene && info.IsBatchMode() == batchMode {
			return work
		}
	}

	return nil
}

func (wp *worksPool) initWork(commonconfigs4worker []*types.CommonConfig) (*types.Work, error) {
	wp.Lock()
	defer wp.Unlock()

	var workID string
	for i := 0; i < 3; i++ {
		workID = fmt.Sprintf("%s_%d", util.RandomString(types.WorkIDLength), time.Now().Local().Unix())
		if _, ok := wp.works[workID]; ok {
			workID = ""
			blog.Warnf("worksPool: generate an existing workID: %s, will try again", workID)
			continue
		}
		break
	}

	if workID == "" {
		return nil, types.ErrWorkIDGenerateFailed
	}

	worker := types.NewWork(workID, wp.conf, mgrSet, wp.recordersPool)

	// set common config
	if commonconfigs4worker != nil && len(commonconfigs4worker) > 0 {
		for _, v := range commonconfigs4worker {
			switch v.Configkey {
			case dcSDK.CommonConfigKeyToolChain:
				sdkToolChain, ok := v.Config.(dcSDK.OneToolChain)
				if ok {
					toolchain := sdkToolChain2Types(&sdkToolChain)
					_ = worker.Basic().SetToolChain(toolchain)
				}
			default:
				blog.Warnf("worksPool: found unknown common config type:%v", v.Configkey)
			}
		}
	}

	wp.works[workID] = worker

	return wp.works[workID], nil
}

func (wp *worksPool) cleanRemovableWork() {
	wp.Lock()

	notEmpty := len(wp.works) != 0
	var wg sync.WaitGroup
	removed := false
	for workID, work := range wp.works {
		if work.Basic().Info().CanBeRemoved() {
			blog.Infof("worksPool: work(%s) is removable, deleted from cache", workID)
			delete(wp.works, workID)
			removed = true

			wg.Add(1)
			go func(w *types.Work) {
				defer wg.Done()

				// 停止一切work的活动
				w.Lock()
				w.Cancel()
				w.Unlock()
			}(work)
		}
	}

	if notEmpty && len(wp.works) == 0 {
		wp.lastEmptyTime = time.Now().Local()
	}

	wp.Unlock()

	if removed {
		blog.Infof("worksPool: some work has removed, and try save all recorders")
		go wp.recordersPool.SaveAll()
	}

	wg.Wait()
}

func (wp *worksPool) cleanHeartbeatTimeout() {
	var wg sync.WaitGroup
	for _, work := range wp.all() {
		wg.Add(1)
		go func(w *types.Work) {
			defer wg.Done()

			w.Lock()
			defer w.Unlock()

			info := w.Basic().Info()
			if w.Basic().Alive() > 0 || info.LastHeartbeat().Add(dcSDK.WorkHeartbeatTimeout).After(time.Now().Local()) {
				return
			}

			blog.Infof("mgr: work(%s) last heartbeat(%s) is already timeout(%s), will be unregistered",
				w.ID(), info.LastHeartbeat().String(), dcSDK.WorkHeartbeatTimeout.String())
			w.Basic().DecRegistered()
			if err := w.Basic().Unregister(&types.WorkUnregisterConfig{
				Release: &v2.ParamRelease{
					Success: w.Basic().Info().IsBatchMode(),
					Message: "work heartbeat timeout",
				},
				TimeoutBefore: dcSDK.WorkHeartbeatTimeout,
				Force:         true,
			}); err != nil {
				blog.Warnf("mgr: work(%s) unregister for heartbeat timeout failed: %v", w.ID(), err)
			}
		}(work)
	}

	wg.Wait()
}

func (wp *worksPool) empty() bool {
	wp.RLock()
	defer wp.RUnlock()

	return len(wp.works) == 0
}

func (wp *worksPool) emptyTimeout(timeout time.Duration) bool {
	wp.RLock()
	defer wp.RUnlock()

	if len(wp.works) == 0 && wp.lastEmptyTime.Add(timeout).Before(time.Now().Local()) {
		blog.Infof("worksPool: there is no active work since(%s), reaches remain time(%s), "+
			"works pool empty timeout", wp.lastEmptyTime.Local().String(), timeout.String())
		return true
	}

	return false
}

func newGlobalWork(conf *config.ServerConfig) *types.Work {
	work := types.NewWork("", conf, mgrSet, nil)
	work.Basic().SetSettings(&types.WorkSettings{
		UsageLimit: map[sdk.JobUsage]int{
			sdk.JobUsageLocalPre:  conf.LocalPreSlots,
			sdk.JobUsageLocalExe:  conf.LocalExeSlots,
			sdk.JobUsageLocalPost: conf.LocalPostSlots,
		},
		LocalTotalLimit: conf.LocalSlots,
	})
	work.Local().Init()
	return work
}
