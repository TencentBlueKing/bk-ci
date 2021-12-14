/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package remote

import (
	"container/list"
	"context"
	"sync"

	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
)

type lockWorkerMessage struct {
	jobUsage dcSDK.JobUsage
	toward   *dcProtocol.Host
	result   chan *dcProtocol.Host
}
type lockWorkerChan chan lockWorkerMessage

type usageWorkerSet struct {
	limit    int
	occupied int
}

func newResource(hl []*dcProtocol.Host, usageLimit map[dcSDK.JobUsage]int) *resource {
	wl := make([]*worker, 0, len(hl))
	total := 0
	for _, h := range hl {
		if h.Jobs <= 0 {
			continue
		}

		wl = append(wl, &worker{
			host:          h,
			totalSlots:    h.Jobs,
			occupiedSlots: 0,
		})
		total += h.Jobs
	}
	if total == 0 {
		wl = append(wl, &worker{
			host:          &dcProtocol.Host{},
			totalSlots:    1,
			occupiedSlots: 0,
		})
		total = 1
	}

	usageMap := make(map[dcSDK.JobUsage]*usageWorkerSet, 10)
	for k, v := range usageLimit {
		if v <= 0 {
			v = total
		}
		usageMap[k] = &usageWorkerSet{
			limit:    v,
			occupied: 0,
		}
	}
	usageMap[dcSDK.JobUsageDefault] = &usageWorkerSet{
		limit:    total,
		occupied: 0,
	}

	return &resource{
		totalSlots:    total,
		occupiedSlots: 0,
		usageMap:      usageMap,
		lockChan:      make(lockWorkerChan, 1000),
		unlockChan:    make(lockWorkerChan, 1000),
		worker:        wl,

		waitingList: list.New(),
	}
}

type resource struct {
	ctx context.Context

	workerLock sync.RWMutex
	worker     []*worker

	totalSlots    int
	occupiedSlots int

	usageMap map[dcSDK.JobUsage]*usageWorkerSet

	lockChan   lockWorkerChan
	unlockChan lockWorkerChan

	handling bool

	// to save waiting requests
	waitingList *list.List
}

// brings handler up and begin to handle requests
func (wr *resource) Handle(ctx context.Context) {
	if wr.handling {
		return
	}

	wr.handling = true

	go wr.handleLock(ctx)
}

// Lock get an usage lock, success with true, failed with false
func (wr *resource) Lock(usage dcSDK.JobUsage) *dcProtocol.Host {
	if !wr.handling {
		return nil
	}

	msg := lockWorkerMessage{
		jobUsage: usage,
		toward:   nil,
		result:   make(chan *dcProtocol.Host, 1),
	}

	// send a new lock request
	wr.lockChan <- msg

	select {
	case <-wr.ctx.Done():
		return &dcProtocol.Host{}

	// wait result
	case h := <-msg.result:
		return h
	}
}

// Unlock release an usage lock
func (wr *resource) Unlock(usage dcSDK.JobUsage, host *dcProtocol.Host) {
	if !wr.handling {
		return
	}

	wr.unlockChan <- lockWorkerMessage{
		jobUsage: usage,
		toward:   host,
		result:   nil,
	}
}

func (wr *resource) disableWorker(host *dcProtocol.Host) {
	wr.workerLock.Lock()
	defer wr.workerLock.Unlock()

	for _, w := range wr.worker {
		if !host.Equal(w.host) {
			continue
		}

		w.disabled = true
		return
	}
}

func (wr *resource) getWorkerWithMostFreeSlots() *worker {
	var w *worker
	max := 0
	for _, worker := range wr.worker {
		if worker.disabled {
			continue
		}

		free := worker.totalSlots - worker.occupiedSlots
		if free >= max {
			max = free
			w = worker
		}
	}
	if w == nil {
		w = wr.worker[0]
	}

	return w
}

func (wr *resource) occupyWorkerSlots() *dcProtocol.Host {
	wr.workerLock.Lock()
	defer wr.workerLock.Unlock()

	worker := wr.getWorkerWithMostFreeSlots()
	_ = worker.occupySlot()

	return worker.host
}

func (wr *resource) freeWorkerSlots(host *dcProtocol.Host) {
	wr.workerLock.Lock()
	defer wr.workerLock.Unlock()

	for _, w := range wr.worker {
		if !host.Equal(w.host) {
			continue
		}

		_ = w.freeSlot()
		return
	}
}

func (wr *resource) handleLock(ctx context.Context) {
	wr.ctx = ctx

	for {
		select {
		case <-ctx.Done():
			return
		case msg := <-wr.unlockChan:
			wr.putSlot(msg)
		case msg := <-wr.lockChan:
			wr.getSlot(msg)
		}
	}
}

func (wr *resource) getUsageSet(usage dcSDK.JobUsage) *usageWorkerSet {
	set, ok := wr.usageMap[usage]
	if !ok {
		// unknown usage, get default usage
		set = wr.usageMap[dcSDK.JobUsageDefault]
	}

	return set
}

func (wr *resource) isIdle(set *usageWorkerSet) bool {
	if set == nil {
		return false
	}

	if set.occupied < set.limit {
		return true
	}

	return false
}

func (wr *resource) getSlot(msg lockWorkerMessage) {
	satisfied := false
	usage := msg.jobUsage
	if wr.occupiedSlots < wr.totalSlots {
		set := wr.getUsageSet(usage)
		if wr.isIdle(set) {
			set.occupied++
			wr.occupiedSlots++
			msg.result <- wr.occupyWorkerSlots()
			satisfied = true
		}
	}

	if !satisfied {
		wr.waitingList.PushBack(usage)
		wr.waitingList.PushBack(msg.result)
	}
}

func (wr *resource) putSlot(msg lockWorkerMessage) {
	wr.freeWorkerSlots(msg.toward)
	wr.occupiedSlots--
	usage := msg.jobUsage
	set := wr.getUsageSet(usage)
	set.occupied--

	// check whether other waiting is satisfied now
	if wr.waitingList.Len() > 0 {
		index := 0
		for e := wr.waitingList.Front(); e != nil; e = e.Next() {
			if index%2 == 0 {
				usage := e.Value.(dcSDK.JobUsage)
				set := wr.getUsageSet(usage)
				if wr.isIdle(set) {
					set.occupied++
					wr.occupiedSlots++

					chanElement := e.Next()
					chanElement.Value.(chan *dcProtocol.Host) <- wr.occupyWorkerSlots()

					// delete this element
					wr.waitingList.Remove(e)
					wr.waitingList.Remove(chanElement)

					break
				}
			}
			index++
		}
	}
}

// worker describe the worker information includes the host details and the slots status
// and it is the caller's responsibility to ensure the lock.
type worker struct {
	disabled      bool
	host          *dcProtocol.Host
	totalSlots    int
	occupiedSlots int
}

func (wr *worker) occupySlot() error {
	wr.occupiedSlots++
	return nil
}

func (wr *worker) freeSlot() error {
	wr.occupiedSlots--
	return nil
}
