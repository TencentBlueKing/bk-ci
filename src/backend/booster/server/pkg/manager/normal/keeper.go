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
	"github.com/Tencent/bk-ci/src/booster/common/conf"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	selfMetric "github.com/Tencent/bk-ci/src/booster/server/pkg/metric"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/metric/controllers"
)

// Keeper monitor the tasks since creating, until terminated status(failed or finish). Include following checks:
// taskHealthCheck:
//    - if the client heartbeat is timeout, the task will be deemed to be lost, then release it as failed.
//    - if the task is in status starting for a long time much than status-changed timeout, then release it as failed.
// serverHealthCheck:
//    - if the server running taskGroups are less than the least instance of this task, the server will be deemed to be
//      unhealthy, and the task will be released as failed.
type Keeper interface {
	Run(pCtx context.Context) error
}

// NewKeeper get a new keeper with given layer. If debug mode set, it will skip all checks during keeper process.
func NewKeeper(layer TaskBasicLayer, debugMode bool, config conf.CommonEngineConfig) Keeper {
	return &keeper{
		layer:     layer,
		debugMode: debugMode,
		conf: commonEngineConfig{
			KeepStartingTimeout: time.Duration(config.KeeperStartingTimeout) * time.Second,
		},
	}
}

type commonEngineConfig struct {
	KeepStartingTimeout time.Duration
}

type keeper struct {
	ctx       context.Context
	layer     TaskBasicLayer
	conf      commonEngineConfig
	debugMode bool
}

// Run the keeper handler with context.
func (k *keeper) Run(ctx context.Context) error {
	k.ctx = ctx
	go k.start()
	return nil
}

func (k *keeper) start() {
	blog.Infof("keeper start and sleep for status recover in grace time(%s)", keeperFirstStartGraceTime.String())
	time.Sleep(keeperFirstStartGraceTime)
	blog.Infof("keeper start working")

	timeTicker := time.NewTicker(keeperHealthCheckGapTime)
	defer timeTicker.Stop()

	for {
		select {
		case <-k.ctx.Done():
			blog.Warnf("keeper shutdown")
			return
		case <-timeTicker.C:
			if k.debugMode {
				continue
			}

			k.check()
		}
	}
}

// Including heartbeat checking and starting timeout checking.
// The terminated status(finish or failed) will not be check.
func (k *keeper) check() {
	blog.Debugf("keeper: do check for checking task health")
	taskList, err := k.layer.ListTaskBasic(false,
		engine.TaskStatusInit,
		engine.TaskStatusStaging,
		engine.TaskStatusStarting,
		engine.TaskStatusRunning,
	)
	if err != nil {
		blog.Errorf("keeper: doing check, list task failed: %v", err)
		return
	}

	var wg sync.WaitGroup

	for _, tb := range taskList {
		wg.Add(1)
		go k.checkTaskBasic(tb.ID, &wg)
	}
	wg.Wait()
}

func (k *keeper) checkTaskBasic(taskID string, wg *sync.WaitGroup) {
	defer wg.Done()

	k.layer.LockTask(taskID)
	defer k.layer.UnLockTask(taskID)

	tb, err := k.layer.GetTaskBasic(taskID)
	if err != nil {
		blog.Errorf("keeper: try checking task basic health, get task basic(%s) failed: %v", taskID, err)
		return
	}

	// ensure that the status is unterminated and is not in init
	switch tb.Status.Status {
	case engine.TaskStatusInit, engine.TaskStatusStaging, engine.TaskStatusStarting, engine.TaskStatusRunning:
	default:
		blog.Warnf("keeper: try checking task basic health, but task basic(%s) is in status(%s), skip",
			taskID, tb.Status.Status)
		return
	}

	nowTime := time.Now().Local()
	egn, err := k.layer.GetEngineByTypeName(tb.Client.EngineName)
	if err != nil {
		blog.Errorf("keeper: try get task(%s) engine failed: %v", tb.ID, err)
		return
	}

	// timeout after last received heartbeat
	heartBeatTimeout := time.Duration(egn.GetPreferences().HeartbeatTimeoutTickTimes) * clientHeartBeatTickTime
	if tb.Status.LastHeartBeatTime.Add(heartBeatTimeout).Before(nowTime) {
		blog.Errorf("keeper: check and find task(%s) heartbeat timeout(over %s) "+
			"since last beat(%s) from ip(%s), will be canceled",
			tb.ID, heartBeatTimeout.String(), tb.Status.LastHeartBeatTime.String(), tb.Client.ClientIP)

		if tb.Status.Status == engine.TaskStatusRunning {
			tb.Status.End()
		}
		tb.Status.FailWithClientLost()
		tb.Status.Message = messageHeartBeatTimeoutAndLost
		k.updateTaskBasic(tb)
		selfMetric.CheckFailController.Inc(
			tb.Client.EngineName.String(), tb.Client.QueueName, controllers.CheckFailHeartbeatTimeout)
		return
	}

	switch tb.Status.Status {
	case engine.TaskStatusStaging:
		// staging timeout
		if tb.Status.CreateTime.Add(time.Duration(tb.Client.StageTimeout) * time.Second).Before(nowTime) {
			blog.Errorf("keeper: check and find task(%s) staging timeout(%ds) since(%s), will degrade task",
				tb.ID, tb.Client.StageTimeout, tb.Status.CreateTime.String())

			if err = egn.DegradeTask(tb.ID); err != nil {
				blog.Errorf("keeper: degrade task(%s) failed: %v", tb.ID, err)
				tb.Status.FailWithServerDown()
				tb.Status.Message = messageStagingTimeout
				k.updateTaskBasic(tb)
				return
			}

			tb.Status.Ready()
			tb.Status.Start()
			tb.Status.Message = messageDegradeTask
			k.updateTaskBasic(tb)
			selfMetric.CheckFailController.Inc(
				tb.Client.EngineName.String(), tb.Client.QueueName, controllers.CheckFailStagingTimeout)
			return
		}

	case engine.TaskStatusStarting:
		if tb.Status.StatusChangeTime.Add(k.conf.KeepStartingTimeout).Before(nowTime) {
			task, err := egn.GetTaskExtension(taskID)
			if err != nil {
				blog.Errorf("keeper: get task extension failed: (%v)", err)
				return
			}

			// 拉起资源超时，此时若有足够的资源，则先启动任务
			blog.Errorf("keeper: check and find task(%s) starting timeout(%s) since(%s), check if it can be running",
				tb.ID, k.conf.KeepStartingTimeout.String(), tb.Status.LaunchTime.String())
			if task.EnoughAvailableResource() {
				tb.Status.Ready()
				tb.Status.Start()
				tb.Status.Message = messageTaskRunning
				k.updateTaskBasic(tb)
				blog.Infof("keeper: task(%s) starting timeout, will start with current workers:(%s)", taskID, task.WorkerList())
				return
			}

			blog.Errorf("keeper: check and find task(%s) starting timeout(%s) since(%s), will be canceled",
				tb.ID, k.conf.KeepStartingTimeout.String(), tb.Status.LaunchTime.String())

			tb.Status.FailWithServerDown()
			tb.Status.Message = messageTaskStartingTimeout
			k.updateTaskBasic(tb)
			selfMetric.CheckFailController.Inc(
				tb.Client.EngineName.String(), tb.Client.QueueName, controllers.CheckFailStartingTimeout)
			return
		}

	case engine.TaskStatusInit:
		if tb.Status.StatusChangeTime.Add(keeperInitTimeout).Before(nowTime) {
			blog.Errorf("keeper: check and find task(%s) starting timeout(%s) since(%s), will be canceled",
				tb.ID, keeperInitTimeout.String(), tb.Status.LaunchTime.String())

			tb.Status.FailWithServerDown()
			tb.Status.Message = messageTaskInitTimeout
			k.updateTaskBasic(tb)
			selfMetric.CheckFailController.Inc(
				tb.Client.EngineName.String(), tb.Client.QueueName, controllers.CheckFailInitTimeout)
			return
		}
	}

	// let engine judge task status
	if err = egn.CheckTask(tb); err != nil {
		blog.Errorf("keeper: check task(%s) failed: %v", tb.ID, err)
		if tb.Status.Status == engine.TaskStatusRunning {
			tb.Status.End()
		}
		tb.Status.FailWithServerDown()
		tb.Status.Message = err.Error()
		k.updateTaskBasic(tb)
		selfMetric.CheckFailController.Inc(
			tb.Client.EngineName.String(), tb.Client.QueueName, controllers.CheckFailEngineCheckFail)
		return
	}
}

func (k *keeper) updateTaskBasic(tb *engine.TaskBasic) {
	if err := k.layer.UpdateTaskBasic(tb); err != nil {
		blog.Errorf("keeper: update basic task(%s) failed: %v", tb.ID, err)
	}
}
