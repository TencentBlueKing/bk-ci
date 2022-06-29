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
	"github.com/Tencent/bk-ci/src/booster/common/blog"
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

	usageMap := make(map[dcSDK.JobUsage]*usageWorkerSet, 10)
	// do not use usageLimit, we only need JobUsageRemoteExe, and it is always 0 by now
	usageMap[dcSDK.JobUsageRemoteExe] = &usageWorkerSet{
		limit:    total,
		occupied: 0,
	}
	usageMap[dcSDK.JobUsageDefault] = &usageWorkerSet{
		limit:    total,
		occupied: 0,
	}

	for _, v := range usageMap {
		blog.Infof("remote slot: usage map:%v after new resource", *v)
	}
	blog.Infof("remote slot: total slots:%d after new resource", total)

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

// reset with []*dcProtocol.Host
// add new hosts and disable released hosts
func (wr *resource) Reset(hl []*dcProtocol.Host) ([]*dcProtocol.Host, error) {
	blog.Infof("remote slot: ready reset with %d host", len(hl))

	wr.workerLock.Lock()
	defer wr.workerLock.Unlock()

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

	usageMap := make(map[dcSDK.JobUsage]*usageWorkerSet, 10)
	// do not use usageLimit, we only need JobUsageRemoteExe, and it is always 0 by now
	usageMap[dcSDK.JobUsageRemoteExe] = &usageWorkerSet{
		limit:    total,
		occupied: 0,
	}
	usageMap[dcSDK.JobUsageDefault] = &usageWorkerSet{
		limit:    total,
		occupied: 0,
	}

	for _, v := range usageMap {
		blog.Infof("remote slot: usage map:%v after reset with new resource", *v)
	}
	blog.Infof("remote slot: total slots:%d after reset with new resource", total)

	wr.totalSlots = total
	wr.occupiedSlots = 0
	wr.usageMap = usageMap
	wr.worker = wl

	return hl, nil
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

func (wr *resource) TotalSlots() int {
	return wr.totalSlots
}

func (wr *resource) disableWorker(host *dcProtocol.Host) {
	if host == nil {
		return
	}

	wr.workerLock.Lock()
	defer wr.workerLock.Unlock()

	invalidjobs := 0
	for _, w := range wr.worker {
		if !host.Equal(w.host) {
			continue
		}

		if w.disabled {
			blog.Infof("remote slot: host:%v disabled before,do nothing now", *host)
			break
		}

		w.disabled = true
		invalidjobs = w.totalSlots
		break
	}

	// !!! wr.totalSlots and v.limit may be <= 0 !!!
	if invalidjobs > 0 {
		wr.totalSlots -= invalidjobs
		for _, v := range wr.usageMap {
			v.limit = wr.totalSlots
			blog.Infof("remote slot: usage map:%v after disable host:%v", *v, *host)
		}
	}

	blog.Infof("remote slot: total slot:%d after disable host:%v", wr.totalSlots, *host)
	return
}

func (wr *resource) disableAllWorker() {
	blog.Infof("remote slot: ready disable all host")

	wr.workerLock.Lock()
	defer wr.workerLock.Unlock()

	for _, w := range wr.worker {
		if w.disabled {
			continue
		}

		w.disabled = true
	}

	wr.totalSlots = 0
	for _, v := range wr.usageMap {
		v.limit = 0
		blog.Infof("remote slot: usage map:%v after disable all host", *v)
	}

	blog.Infof("remote slot: total slot:%d after disable all host", wr.totalSlots)
	return
}

func (wr *resource) addWorker(host *dcProtocol.Host) {
	if host == nil || host.Jobs <= 0 {
		return
	}

	wr.workerLock.Lock()
	defer wr.workerLock.Unlock()

	for _, w := range wr.worker {
		if host.Equal(w.host) {
			blog.Infof("remote slot: host(%s) existed when add", w.host.Server)
			return
		}
	}

	wr.worker = append(wr.worker, &worker{
		host:          host,
		totalSlots:    host.Jobs,
		occupiedSlots: 0,
	})
	wr.totalSlots += host.Jobs

	for _, v := range wr.usageMap {
		v.limit = wr.totalSlots
		blog.Infof("remote slot: usage map:%v after add host:%v", *v, *host)
	}

	blog.Infof("remote slot: total slot:%d after add host:%v", wr.totalSlots, *host)
	return
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

	if set.occupied < set.limit || set.limit <= 0 {
		return true
	}

	return false
}

func (wr *resource) getSlot(msg lockWorkerMessage) {
	satisfied := false
	usage := msg.jobUsage
	if wr.occupiedSlots < wr.totalSlots || wr.totalSlots <= 0 {
		set := wr.getUsageSet(usage)
		if wr.isIdle(set) {
			set.occupied++
			wr.occupiedSlots++
			blog.Infof("remote slot: total slots:%d occupied slots:%d, remote slot available",
				wr.totalSlots, wr.occupiedSlots)
			msg.result <- wr.occupyWorkerSlots()
			satisfied = true
		}
	}

	if !satisfied {
		blog.Infof("remote slot: total slots:%d occupied slots:%d, remote slot not available",
			wr.totalSlots, wr.occupiedSlots)
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
