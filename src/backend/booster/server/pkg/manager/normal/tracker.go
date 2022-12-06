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
	selfMetric "github.com/Tencent/bk-ci/src/booster/server/pkg/metric"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/metric/controllers"
)

// Tracker will track the task in status starting, and since the server is ready for serving, make the task
// to status running and generate all the server information to task.
// Task will not be updated since it go to status starting.
type Tracker interface {
	Run(ctx context.Context) error
	OnTaskStatus(tb *engine.TaskBasic, curstatus engine.TaskStatusType) error
}

// NewTracker get a new tracker with given layer.
func NewTracker(layer TaskBasicLayer, mgr *manager) Tracker {
	return &tracker{
		layer: layer,
		mgr:   mgr,
	}
}

type tracker struct {
	ctx          context.Context
	startingLock sync.RWMutex
	startingMap  map[string]*engine.TaskBasic

	layer TaskBasicLayer
	mgr   *manager
	c     chan bool
}

// Run the tracker handler with context.
func (t *tracker) Run(ctx context.Context) error {
	t.startingMap = make(map[string]*engine.TaskBasic, 100)
	t.ctx = ctx
	t.c = make(chan bool, 100)
	go t.start(t.c)
	return nil
}

func (t *tracker) start(c chan bool) {
	blog.Infof("tracker start")
	timeTicker := time.NewTicker(trackerCheckGapTime)
	defer timeTicker.Stop()

	for {
		select {
		case <-t.ctx.Done():
			blog.Warnf("tracker shutdown")
			return
		case <-timeTicker.C:
			t.check()
		case <-c:
			blog.Infof("tracker: received notify")
			t.check()
		}
	}
}

func (t *tracker) OnTaskStatus(tb *engine.TaskBasic, curstatus engine.TaskStatusType) error {
	blog.Infof("tracker: ready notify to task(%s) engine(%s) queue(%s)", tb.ID, tb.Client.EngineName, tb.Client.QueueName)
	t.c <- true
	return nil
}

func (t *tracker) check() {
	startingTaskList, err := t.layer.ListTaskBasic(false, engine.TaskStatusStarting)
	if err != nil {
		blog.Errorf("tracker: doing check, list starting task failed: %v", err)
		return
	}

	for _, tb := range startingTaskList {
		t.startingLock.RLock()
		_, ok := t.startingMap[tb.ID]
		t.startingLock.RUnlock()

		// this task is already under tracking
		if ok {
			continue
		}

		t.startingLock.Lock()
		t.startingMap[tb.ID] = tb
		t.startingLock.Unlock()

		egn, err := t.layer.GetEngineByTypeName(tb.Client.EngineName)
		if err != nil {
			blog.Errorf("tracker: try get task(%s) engine failed, stop tracking: %v", tb.ID, err)
			continue
		}

		// start tracking this task, until it get rid of status "starting"
		go t.track(tb.ID, egn)
	}
}

func (t *tracker) track(taskID string, egn engine.Engine) {
	blog.Infof("tracker: start tracking task(%s)", taskID)

	// execute before tick, to avoid wait
	if t.isFinishStarting(taskID, egn) {
		blog.Infof("tracker: task(%s) finish status starting, stop tracking", taskID)
		t.startingLock.Lock()
		delete(t.startingMap, taskID)
		t.startingLock.Unlock()
		return
	}

	timeTicker := time.NewTicker(trackerTrackGapTime)
	defer timeTicker.Stop()

	for {
		select {
		case <-t.ctx.Done():
			blog.Warnf("tracker: context done, stop tracking task(%s)", taskID)
			return
		case <-timeTicker.C:
			if !t.isFinishStarting(taskID, egn) {
				continue
			}

			blog.Infof("tracker: task(%s) finish status starting, stop tracking", taskID)
			t.startingLock.Lock()
			delete(t.startingMap, taskID)
			t.startingLock.Unlock()
			return
		}
	}
}

func (t *tracker) isFinishStarting(taskID string, egn engine.Engine) bool {
	t.layer.LockTask(taskID)
	defer t.layer.UnLockTask(taskID)

	blog.V(5).Infof("tracker: try to check if task(%s) is finish starting", taskID)
	tb, err := t.layer.GetTaskBasic(taskID)
	if err == engine.ErrorUnterminatedTaskNoFound {
		blog.Infof("tracker: task(%s) is not in terminated status, just finish starting", taskID)
		return true
	}
	if err != nil {
		blog.Errorf("tracker: try checking if task finish starting, get task(%s) failed: %v", taskID, err)
		return false
	}

	if tb.Status.Status != engine.TaskStatusStarting {
		blog.Infof("tracker: task(%s) is not starting but in status(%s), finish starting",
			taskID, tb.Status.Status)
		return true
	}

	ok, err := egn.LaunchDone(taskID)
	if err != nil {
		blog.Errorf("tracker: check task(%s) launch done failed: %v", taskID, err)
		return false
	}

	// task still under launching
	if !ok {
		blog.V(5).Infof("tracker: task(%s) is still starting", taskID)
		return false
	}

	task, err := egn.GetTaskExtension(taskID)
	if err != nil {
		blog.Error("tracker: get task extension(%s) from engine(%s) failed: %v", taskID, egn.Name(), err)
		return false
	}

	// if task is done but available resource is less than expect, make task failed
	if !task.EnoughAvailableResource() {
		blog.Warnf("tracker: check task(%s) launch is done but no enough resource", taskID)
		tb.Status.FailWithServerDown()
		tb.Status.Message = messageNoEnoughAvailableWorkers
		if err = t.layer.UpdateTaskBasic(tb); err != nil {
			blog.Errorf("tracker: update basic task failed: %v", err)
			return false
		}
		selfMetric.CheckFailController.Inc(
			tb.Client.EngineName.String(), tb.Client.QueueName, controllers.CheckFailNotEnoughAvailableWorkers)
		return true
	}

	tb.Status.Ready()
	tb.Status.Start()

	blog.Info("tracker: task(%s) launch deployment at %s, launch success at %s, time consume %d(s), request pod num %d, real pod num %d",
		taskID, tb.Status.LaunchTime.String(), tb.Status.StartTime.String(),
		tb.Status.StartTime.Unix()-tb.Status.LaunchTime.Unix(), task.GetRequestInstance(), task.GetWorkerCount())

	tb.Status.Message = messageTaskRunning
	blog.Infof("tracker: task(%s) is ready at start_time(%s)", taskID, tb.Status.StartTime.String())
	if err = t.layer.UpdateTaskBasic(tb); err != nil {
		blog.Errorf("tracker: set task(%s) running and update basic task failed: %v", taskID, err)
		return false
	}
	blog.Infof("tracker: task(%s) is running successfully", taskID)
	return true
}
