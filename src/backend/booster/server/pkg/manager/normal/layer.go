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
	"fmt"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	selfMetric "github.com/Tencent/bk-ci/src/booster/server/pkg/metric"
)

// TaskBasicLayer or TaskCache maintains the unterminated task snapshot cache from all available engines
type TaskBasicLayer interface {
	// recover unterminated task from databases
	Recover() error

	// get engine instance by name
	GetEngineByTypeName(tn engine.TypeName) (engine.Engine, error)

	// get engine instance list
	GetEngineList() []engine.Engine

	// global lock for task by taskID
	LockTask(taskID string)
	UnLockTask(taskID string)

	// global lock for project by projectID
	LockProject(projectID string)
	UnLockProject(projectID string)

	// get task basic from cache, return a new pointer
	GetTaskBasic(taskID string) (*engine.TaskBasic, error)

	// list task basic from cache, return a new pointer
	ListTaskBasic(released bool, statusList ...engine.TaskStatusType) ([]*engine.TaskBasic, error)

	// init task basic, create task basic table in database
	InitTaskBasic(tb *engine.TaskBasic) error

	// update task basic, both database and cache, just update the field implements in task basic
	UpdateTaskBasic(tb *engine.TaskBasic) error

	// update heartbeat to task basic, both database and cache
	UpdateHeartbeat(taskID string) error

	// get current unterminated task number of the specific projectID
	GetConcurrency(projectID string) int

	// get the specific engine's task queue group
	GetTaskQueueGroup(engineName engine.TypeName) (*engine.TaskQueueGroup, error)
}

// NewDefaultTaskBasicLayer get a new default basic layer with engine maps.
func NewDefaultTaskBasicLayer(engines map[engine.TypeName]engine.Engine) TaskBasicLayer {
	if engines == nil {
		engines = make(map[engine.TypeName]engine.Engine, 10)
	}

	l := &taskBasicLayer{
		engines:              engines,
		taskLockMap:          make(map[string]*lock, 50000),
		projectInCreationMap: make(map[string]*sync.Mutex, 10000),
		tbm:                  make(map[string]*engine.TaskBasic, 2000),
		qgm:                  make(map[engine.TypeName]*engine.TaskQueueGroup, 100),
	}
	go l.runLockCleaner()
	go l.runReleasedTaskCleaner()

	return l
}

type taskBasicLayer struct {
	// task lock map holds global locks for task
	taskLockMap       map[string]*lock
	taskLockMapRWLock sync.RWMutex

	// project lock map holds global locks for project when creating task
	projectInCreationMap    map[string]*sync.Mutex
	projectInCreationRWLock sync.RWMutex

	// task basic map
	tbm     map[string]*engine.TaskBasic
	tbmLock sync.RWMutex

	// queue group map
	qgm     map[engine.TypeName]*engine.TaskQueueGroup
	qgmLock sync.RWMutex

	engines map[engine.TypeName]engine.Engine
}

// Recover recover cache data from databases.
func (tc *taskBasicLayer) Recover() error {
	return tc.recover()
}

// GetEngineByTypeName get engine instance from layer with name.
func (tc *taskBasicLayer) GetEngineByTypeName(tn engine.TypeName) (engine.Engine, error) {
	egn, ok := tc.engines[tn]
	if !ok {
		return nil, engine.ErrorUnknownEngineType
	}

	return egn, nil
}

// GetEngineList get all supported engine list from layer.
func (tc *taskBasicLayer) GetEngineList() []engine.Engine {
	el := make([]engine.Engine, 0, 10)
	for _, egn := range el {
		el = append(el, egn)
	}
	return el
}

// LockTask get a Write-Lock with taskID.
func (tc *taskBasicLayer) LockTask(taskID string) {
	defer blog.V(5).Infof("layer: lock task(%s)", taskID)

	tc.taskLockMapRWLock.RLock()
	mutex, ok := tc.taskLockMap[taskID]
	tc.taskLockMapRWLock.RUnlock()
	if ok {
		mutex.Lock()
		mutex.lastHold = time.Now().Local()
		return
	}

	tc.taskLockMapRWLock.Lock()
	mutex, ok = tc.taskLockMap[taskID]
	if !ok {
		blog.Info("layer: create task lock(%s), current lock num(%d)", taskID, len(tc.taskLockMap))
		mutex = &lock{
			createAt: time.Now().Local(),
		}
		tc.taskLockMap[taskID] = mutex
	}
	tc.taskLockMapRWLock.Unlock()

	mutex.Lock()
	mutex.lastHold = time.Now().Local()
}

// UnLockTask unlock task lock.
func (tc *taskBasicLayer) UnLockTask(taskID string) {
	tc.taskLockMapRWLock.RLock()
	mutex, ok := tc.taskLockMap[taskID]
	tc.taskLockMapRWLock.RUnlock()
	if !ok {
		blog.Errorf("layer: try to unlock a no exist task(%s)", taskID)
		return
	}

	// log a warning when the lock is hold for too long.
	now := time.Now().Local()
	if mutex.lastHold.Add(1 * time.Second).Before(now) {
		blog.Warnf("layer: task(%s) lock hold for too long: %s", taskID, now.Sub(mutex.lastHold).String())
	}
	blog.V(5).Infof("layer: unlock task(%s)", taskID)
	mutex.Unlock()
}

// LockProject get a Write-Lock with projectID.
func (tc *taskBasicLayer) LockProject(projectID string) {
	tc.projectInCreationRWLock.RLock()
	mutex, ok := tc.projectInCreationMap[projectID]
	tc.projectInCreationRWLock.RUnlock()
	if ok {
		mutex.Lock()
		return
	}

	tc.projectInCreationRWLock.Lock()
	mutex, ok = tc.projectInCreationMap[projectID]
	if !ok {
		blog.Info("layer: create project in creation lock(%s), current lock num(%d)",
			projectID, len(tc.projectInCreationMap))
		mutex = new(sync.Mutex)
		tc.projectInCreationMap[projectID] = mutex
	}
	tc.projectInCreationRWLock.Unlock()

	mutex.Lock()
	blog.Infof("layer: success to get project lock(%s)", projectID)
}

// UnLockProject unlock project lock.
func (tc *taskBasicLayer) UnLockProject(projectID string) {
	tc.projectInCreationRWLock.RLock()
	mutex, ok := tc.projectInCreationMap[projectID]
	tc.projectInCreationRWLock.RUnlock()
	if !ok {
		blog.Errorf("layer: try to unlock a no exist stats project")
		return
	}
	mutex.Unlock()
	blog.Infof("layer: success to release project lock(%s)", projectID)
}

// GetTaskBasic get task basic with taskID from layer cache.
func (tc *taskBasicLayer) GetTaskBasic(taskID string) (*engine.TaskBasic, error) {
	return tc.getTaskBasic(taskID)
}

// ListTaskBasic list task basic with status type list from layer cache.
func (tc *taskBasicLayer) ListTaskBasic(
	released bool, statusList ...engine.TaskStatusType) ([]*engine.TaskBasic, error) {
	tc.tbmLock.RLock()
	defer tc.tbmLock.RUnlock()

	rl := make([]*engine.TaskBasic, 0, 2000)
	for _, tb := range tc.tbm {
		for _, s := range statusList {
			if tb.Status.Status == s && tb.Status.Released == released {
				rl = append(rl, engine.CopyTaskBasic(tb))
				break
			}
		}
	}

	return rl, nil
}

// InitTaskBasic init a task basic into layer cache and databases.
func (tc *taskBasicLayer) InitTaskBasic(tb *engine.TaskBasic) error {
	return tc.updateTaskBasic(tb, true)
}

// UpdateTaskBasic update a existing task basic into layer cache and databases.
func (tc *taskBasicLayer) UpdateTaskBasic(tb *engine.TaskBasic) error {
	return tc.updateTaskBasic(tb, false)
}

// UpdateHeartbeat update a new heartbeat to a task basic with given taskID.
// Heartbeat info will be updated into layer cache and databases.
func (tc *taskBasicLayer) UpdateHeartbeat(taskID string) error {
	tc.LockTask(taskID)
	defer tc.UnLockTask(taskID)

	tb, err := tc.getTaskBasic(taskID)
	if err != nil {
		blog.Warnf("layer: try updating heartbeat, get task basic(%s) failed: %v", taskID, err)
		return err
	}

	// already terminated, do not accept heartbeat
	if tb.Status.Status.Terminated() {
		err = fmt.Errorf("task(%s) is already terminated", taskID)
		blog.Warnf("layer: try updating heartbeat failed: %v", err)
		return err
	}

	tb.Status.Beats()
	return tc.updateTaskBasic(tb, false)
}

// GetConcurrency return the current no-terminated number of task basic in layer cache under given projectID.
func (tc *taskBasicLayer) GetConcurrency(projectID string) int {
	tc.tbmLock.RLock()
	defer tc.tbmLock.RUnlock()

	ccy := 0
	for _, tb := range tc.tbm {
		if tb.Client.ProjectID == projectID && !tb.Status.Status.Terminated() {
			ccy++
		}
	}
	return ccy
}

// GetTaskQueueGroup return the task queue group belongs to the given engine name.
func (tc *taskBasicLayer) GetTaskQueueGroup(engineName engine.TypeName) (*engine.TaskQueueGroup, error) {
	qg, err := tc.getQueueGroup(engineName)
	if err != nil {
		blog.Errorf("layer: get task queue group from engine(%s) failed: %v", engineName, err)
		return nil, err
	}
	return qg, nil
}

func (tc *taskBasicLayer) getTaskBasic(taskID string) (*engine.TaskBasic, error) {
	tc.tbmLock.RLock()
	defer tc.tbmLock.RUnlock()

	tb, ok := tc.tbm[taskID]
	if !ok {
		blog.Warnf("layer: task(%s) not in layer, no exist or already released", taskID)
		return nil, engine.ErrorUnterminatedTaskNoFound
	}

	return engine.CopyTaskBasic(tb), nil
}

func (tc *taskBasicLayer) updateTaskBasic(tbRaw *engine.TaskBasic, new bool) error {
	blog.Debugf("layer: try to update task basic(%s) in status(%s) with engine(%s) and queue(%s)",
		tbRaw.ID, tbRaw.Status.Status, tbRaw.Client.EngineName, tbRaw.Client.QueueName)

	tb := engine.CopyTaskBasic(tbRaw)
	egn, err := tc.GetEngineByTypeName(tb.Client.EngineName)
	if err != nil {
		blog.Errorf("layer: try updating task basic(%s), get engine(%s) failed: %v", tb.ID, tb.Client.EngineName, err)
		return err
	}

	if new {
		err = engine.CreateTaskBasic(egn, tb)
	} else {
		err = engine.UpdateTaskBasic(egn, tb)
	}

	if err != nil {
		blog.Errorf("layer: update task basic(%s) via engine(%s) failed: %v", tb.ID, tb.Client.EngineName, err)
		return err
	}

	// task is un-released, should be in layer cache, waiting for handling
	tc.putTB(tb)

	blog.Infof("layer: success to update task basic(%s) in status(%s) with engine(%s) and queue(%s)",
		tb.ID, tb.Status.Status, tb.Client.EngineName, tb.Client.QueueName)
	return nil
}

func (tc *taskBasicLayer) putTB(tb *engine.TaskBasic) {
	tc.tbmLock.Lock()
	defer tc.tbmLock.Unlock()

	blog.Debugf("layer: get lock and going to putTB(%s) to cache and queue", tb.ID)

	// update metric data of task num
	// decrease last status num and add current status num, if the status is same as last one, then do nothing
	// if there is no last status, then just add the un-terminated status
	if oldTask, ok := tc.tbm[tb.ID]; ok {
		if oldTask.Status.Status != tb.Status.Status {
			selfMetric.TaskNumController.Dec(
				tb.Client.EngineName.String(), tb.Client.QueueName, string(oldTask.Status.Status), "")
			statusReason := ""
			if tb.Status.Status.Terminated() {
				statusReason = tb.Status.StatusCode.String()
			}
			selfMetric.TaskNumController.Inc(
				tb.Client.EngineName.String(), tb.Client.QueueName, string(tb.Status.Status), statusReason)
		}
	} else if tb.Status.Status != engine.TaskStatusFinish && tb.Status.Status != engine.TaskStatusFailed {
		selfMetric.TaskNumController.Inc(
			tb.Client.EngineName.String(), tb.Client.QueueName, string(tb.Status.Status), "")
	}

	qg, err := tc.getQueueGroup(tb.Client.EngineName)
	if err != nil {
		blog.Errorf("layer: put task basic(ID: %s, engine: %s) to cache failed: %v",
			tb.ID, tb.Client.EngineName, err)
		return
	}
	queue := qg.GetQueue(tb.Client.QueueName)

	// task in staging should be added into queue
	if tb.Status.Status == engine.TaskStatusStaging {

		// If the task is new-added, it should be added into queue,
		// or it exists already but its priority has been changed,
		// then should call delete & add to Queue for adjusting the task rank.
		if tc.tbm[tb.ID] == nil ||
			tc.tbm[tb.ID].Status.Status != engine.TaskStatusStaging ||
			tb.Client.Priority != tc.tbm[tb.ID].Client.Priority {

			tc.deleteTBFromQueue(tb)
			queue.Add(tb)
			blog.Infof("layer: task basic(%s) update into queue(%s) of engine(%s)",
				tb.ID, tb.Client.QueueName, tb.Client.EngineName)
		}

		tc.tbm[tb.ID] = tb
		return
	}

	// If the task status has changed from staging to others, then it should be kick out of queue
	if tc.deleteTBFromQueue(tb) {
		blog.Infof("layer: task basic(%s) with status(%s) quit from queue(%s) of engine(%s)",
			tb.ID, tb.Status.Status, tb.Client.QueueName, tb.Client.EngineName)
	}
	tc.tbm[tb.ID] = tb
}

// delete task basic from layer, both cache and queue
func (tc *taskBasicLayer) deleteTB(tb *engine.TaskBasic) {
	tc.tbmLock.Lock()
	defer tc.tbmLock.Unlock()

	tc.deleteTBFromQueue(tb)
	if _, ok := tc.tbm[tb.ID]; ok {
		delete(tc.tbm, tb.ID)
	}
}

func (tc *taskBasicLayer) deleteTBFromQueue(tb *engine.TaskBasic) bool {
	qg, err := tc.getQueueGroup(tb.Client.EngineName)
	if err != nil {
		return false
	}

	return qg.DeleteTask(tb.ID)
}

func (tc *taskBasicLayer) getQueueGroup(engineName engine.TypeName) (*engine.TaskQueueGroup, error) {
	tc.qgmLock.RLock()
	defer tc.qgmLock.RUnlock()

	qg, ok := tc.qgm[engineName]
	if !ok {
		return nil, engine.ErrorUnknownEngineType
	}

	return qg, nil
}

func (tc *taskBasicLayer) recover() error {
	// clean metric data every time doing recover
	selfMetric.TaskNumController.Clean()
	selfMetric.CheckFailController.Clean()

	tc.refreshQueueGroup()
	return tc.recoverTaskBasics()
}

func (tc *taskBasicLayer) refreshQueueGroup() {
	tc.qgmLock.Lock()
	defer tc.qgmLock.Unlock()

	// clean queue group map
	tc.qgm = make(map[engine.TypeName]*engine.TaskQueueGroup, 100)
	enl := make([]string, 0, 10)
	for engineName := range tc.engines {
		tc.qgm[engineName] = engine.NewTaskQueueGroup()
		enl = append(enl, engineName.String())
	}
	blog.Infof("layer: do refresh queue group: %v", enl)
}

func (tc *taskBasicLayer) recoverTaskBasics() error {
	tc.tbmLock.Lock()
	defer tc.tbmLock.Unlock()
	blog.Infof("do recover task basics")

	// clean task basic map
	tc.tbm = make(map[string]*engine.TaskBasic, 1000)
	for _, egn := range tc.engines {
		qg, err := tc.getQueueGroup(egn.Name())
		if err != nil {
			blog.Errorf("layer: try recovering engine(%s), get queue group failed: %v, skip this engine",
				egn.Name(), err)
			continue
		}

		tbl, err := tc.listUnterminatedTaskBasicFromEngines(egn)
		if err != nil {
			blog.Errorf("layer: try recovering engine(%s), list task basic failed: %v, skip this engine",
				egn.Name(), err)
			continue
		}

		nl := make([]string, 0, 500)
		for _, tb := range tbl {
			// must check engine name, for some engines use the same databases.
			if tb.Client.EngineName != egn.Name() {
				continue
			}

			tc.tbm[tb.ID] = tb
			nl = append(nl, tb.ID)

			selfMetric.TaskNumController.Inc(
				tb.Client.EngineName.String(), tb.Client.QueueName, string(tb.Status.Status), "")

			if tb.Status.Status == engine.TaskStatusStaging {
				qg.GetQueue(tb.Client.QueueName).Add(tb)
				blog.Infof("layer: recover staging task basic(%s) into queue(%s) of engine(%s)",
					tb.ID, tb.Client.QueueName, egn.Name().String())
			}
		}
		blog.Infof("layer: success to recover %d task basics from engine(%s): %+v", len(nl), egn.Name(), nl)
	}
	return nil
}

func (tc *taskBasicLayer) listUnterminatedTaskBasicFromEngines(egn engine.Engine) ([]*engine.TaskBasic, error) {
	released := false
	tl, err := engine.ListTaskBasic(egn, engine.NewTaskListOptions(&released))
	if err != nil {
		blog.Errorf("layer: list task from engine(%s) failed: %v", egn.Name(), err)
		return nil, err
	}

	return tl, nil
}

type lock struct {
	sync.Mutex
	createAt time.Time
	lastHold time.Time
}

func (tc *taskBasicLayer) runLockCleaner() {
	blog.Infof("layer: begin to run lock cleaner")
	ticker := time.NewTicker(layerCleanLockTimeGap)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			tc.taskLockMapRWLock.Lock()
			num := 0
			for key, lock := range tc.taskLockMap {
				if lock.createAt.Add(24 * time.Hour).Before(time.Now().Local()) {
					num++
					delete(tc.taskLockMap, key)
				}
			}
			blog.Infof("layer: clean %d lock", num)
			tc.taskLockMapRWLock.Unlock()
		}
	}
}

func (tc *taskBasicLayer) runReleasedTaskCleaner() {
	blog.Infof("layer: begin to run released task cleaner")
	ticker := time.NewTicker(layerCleanReleasedTaskGap)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			blog.Infof("layer: start to clean released tasks")
			tl, err := tc.ListTaskBasic(true, engine.TaskStatusFinish, engine.TaskStatusFailed)
			if err != nil {
				blog.Errorf("layer: clean released task, list task basic failed: %v", err)
				continue
			}

			num := 0
			for _, tb := range tl {
				// task is already in terminated status and released and out of time
				// just remove it out of layer
				if tb.Status.ShutDownTime.Add(layerCleanTimeAfterReleased).Before(time.Now().Local()) {
					tc.LockTask(tb.ID)
					tc.deleteTB(tb)
					tc.UnLockTask(tb.ID)
					num++
					blog.Infof("layer: success to remove released task(%s) from layer, "+
						"because of timeout(%s) after released(%s)",
						tb.ID, layerCleanTimeAfterReleased.String(), tb.Status.ShutDownTime.String())
				}
			}

			tc.tbmLock.RLock()
			total := len(tc.tbm)
			tc.tbmLock.RUnlock()

			blog.Infof("layer: clean %d released tasks, current %d tasks in layer", num, total)
		}
	}
}
