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
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
)

// Cleaner checks all the terminated status(finish or failed), find out the no-released-yet tasks, collect the
// stats information and release the backend servers.
type Cleaner interface {
	Run(pCtx context.Context) error
}

// NewCleaner get a new cleaner with given layer.
func NewCleaner(layer TaskBasicLayer) Cleaner {
	return &cleaner{
		layer: layer,
	}
}

type cleaner struct {
	ctx   context.Context
	layer TaskBasicLayer
}

// Run the cleaner handler with context.
func (c *cleaner) Run(ctx context.Context) error {
	c.ctx = ctx
	go c.start()
	return nil
}

func (c *cleaner) start() {
	blog.Infof("cleaner start")
	timeTicker := time.NewTicker(cleanerReleaseCheckGapTime)
	defer timeTicker.Stop()

	for {
		select {
		case <-c.ctx.Done():
			blog.Warnf("cleaner shutdown")
			return
		case <-timeTicker.C:
			c.check()
		}
	}
}

func (c *cleaner) check() {
	terminatedTaskList, err := c.layer.ListTaskBasic(false, engine.TaskStatusFinish, engine.TaskStatusFailed)
	if err != nil {
		blog.Errorf("cleaner: doing check, list terminated task basic failed: %v", err)
		return
	}

	var wg sync.WaitGroup
	for _, tb := range terminatedTaskList {
		if tb.Status.Released {
			continue
		}
		blog.Infof("cleaner: check and find task(%s) is unreleased, prepare to collect data and release", tb.ID)

		egn, err := c.layer.GetEngineByTypeName(tb.Client.EngineName)
		if err != nil {
			blog.Errorf("cleaner: try get task(%s) engine failed: %v", tb.ID, err)
			continue
		}

		wg.Add(1)
		go c.clean(tb.ID, egn, &wg)
	}
	wg.Wait()
}

func (c *cleaner) clean(taskID string, egn engine.Engine, wg *sync.WaitGroup) {
	defer wg.Done()

	c.layer.LockTask(taskID)
	defer c.layer.UnLockTask(taskID)

	tb, err := c.layer.GetTaskBasic(taskID)
	if err != nil {
		blog.Errorf("cleaner: get task(%s) failed: %v", taskID, err)
		return
	}

	// StatusCode records from which status the task changed and if the server is probably alive, then collect
	// the stats info from servers.
	if tb.Status.StatusCode.ServerAlive() {
		// try collecting data via engine, no matter successful or not, this will not be retried
		blog.Infof("cleaner: try collecting task(%s) data before released", taskID)
		if err = egn.CollectTaskData(tb); err != nil {
			blog.Errorf("cleaner: try collecting task(%s) data failed: %v", taskID, err)
		}
	}

	projectInfoDelta := engine.DeltaProjectInfoBasic{}
	switch tb.Status.Status {
	case engine.TaskStatusFinish:
		projectInfoDelta.CompileFinishTimes = 1
	case engine.TaskStatusFailed:
		projectInfoDelta.CompileFailedTimes = 1
	}
	if err = engine.UpdateProjectInfoBasic(egn, tb.Client.ProjectID, projectInfoDelta); err != nil {
		blog.Errorf("cleaner: try update project(%s) info basic according task(%s) with delta(%+v) failed: %v",
			tb.Client.ProjectID, taskID, projectInfoDelta, err)
	}

	blog.Infof("cleaner: try releasing task(%s)", taskID)
	if err = egn.ReleaseTask(taskID); err != nil {
		blog.Errorf("cleaner: try releasing task(%s) failed: %v", taskID, err)
		return
	}

	blog.Infof("cleaner: success to release task(%s)", taskID)
	tb.Status.ShutDown()
	if err = c.layer.UpdateTaskBasic(tb); err != nil {
		blog.Errorf("cleaner: update task basic(%s) failed: %v", taskID, err)
		return
	}
	blog.Infof("cleaner: success to release and update task basic(%s)", taskID)
}
