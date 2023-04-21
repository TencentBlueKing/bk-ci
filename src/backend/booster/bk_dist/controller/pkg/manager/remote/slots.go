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
	"runtime"
	"sync"

	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/shirou/gopsutil/mem"
)

type lockWorkerMessage struct {
	jobUsage  dcSDK.JobUsage
	toward    *dcProtocol.Host
	result    chan *dcProtocol.Host
	largeFile string
}
type lockWorkerChan chan lockWorkerMessage

type usageWorkerSet struct {
	limit    int
	occupied int
}

// func newResource(hl []*dcProtocol.Host, usageLimit map[dcSDK.JobUsage]int) *resource {
func newResource(hl []*dcProtocol.Host) *resource {
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
func (wr *resource) Lock(usage dcSDK.JobUsage, f string) *dcProtocol.Host {
	if !wr.handling {
		return nil
	}

	msg := lockWorkerMessage{
		jobUsage:  usage,
		toward:    nil,
		result:    make(chan *dcProtocol.Host, 1),
		largeFile: f,
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

// 大文件优先
func (wr *resource) getWorkerLargeFileFirst(f string) *worker {
	var w *worker
	max := 0
	inlargequeue := false
	for _, worker := range wr.worker {
		if worker.disabled {
			continue
		}

		free := worker.totalSlots - worker.occupiedSlots

		// 在资源空闲时，大文件优先
		if free > worker.totalSlots/2 && worker.hasFile(f) {
			// if free > 0 && worker.hasFile(f) {
			if !inlargequeue { // first in large queue
				inlargequeue = true
				max = free
				w = worker
			} else {
				if free >= max {
					max = free
					w = worker
				}
			}
			continue
		}

		if free >= max && !inlargequeue {
			max = free
			w = worker
		}
	}
	if w == nil {
		w = wr.worker[0]
	}

	if f != "" && !w.hasFile(f) {
		w.largefiles = append(w.largefiles, f)
	}

	return w
}

func (wr *resource) occupyWorkerSlots(f string) *dcProtocol.Host {
	wr.workerLock.Lock()
	defer wr.workerLock.Unlock()

	var w *worker
	if f == "" {
		w = wr.getWorkerWithMostFreeSlots()
	} else {
		w = wr.getWorkerLargeFileFirst(f)
	}
	_ = w.occupySlot()

	return w.host
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
			msg.result <- wr.occupyWorkerSlots(msg.largeFile)
			satisfied = true
		}
	}

	if !satisfied {
		blog.Infof("remote slot: total slots:%d occupied slots:%d, remote slot not available",
			wr.totalSlots, wr.occupiedSlots)
		// wr.waitingList.PushBack(usage)
		// wr.waitingList.PushBack(msg.result)
		wr.waitingList.PushBack(&msg)
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
		// index := 0
		for e := wr.waitingList.Front(); e != nil; e = e.Next() {
			// if index%2 == 0 {
			msg := e.Value.(*lockWorkerMessage)
			// usage := e.Value.(dcSDK.JobUsage)
			// set := wr.getUsageSet(usage)
			set := wr.getUsageSet(msg.jobUsage)
			if wr.isIdle(set) {
				set.occupied++
				wr.occupiedSlots++

				msg.result <- wr.occupyWorkerSlots(msg.largeFile)

				// chanElement := e.Next()
				// chanElement.Value.(chan *dcProtocol.Host) <- wr.occupyWorkerSlots()

				// delete this element
				wr.waitingList.Remove(e)
				// wr.waitingList.Remove(chanElement)

				break
			}
			// }
			// index++
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
	// > 100MB
	largefiles         []string
	largefiletotalsize uint64
}

func (wr *worker) occupySlot() error {
	wr.occupiedSlots++
	return nil
}

func (wr *worker) freeSlot() error {
	wr.occupiedSlots--
	return nil
}

func (wr *worker) hasFile(f string) bool {
	if len(wr.largefiles) > 0 {
		for _, v := range wr.largefiles {
			if v == f {
				return true
			}
		}
	}

	return false
}

// by tming to limit local memory usage
func newMemorySlot(maxSlots int64) *memorySlot {
	minmemroy := int64(2 * 1024 * 1024 * 1024) // 2GB

	if maxSlots <= 0 {
		v, err := mem.VirtualMemory()
		if err != nil {
			blog.Infof("memory slot: failed to get virtaul memory with err:%v", err)
			maxSlots = int64((runtime.NumCPU() - 2)) * 1024 * 1024 * 1024
		} else {
			// maxSlots = int64(v.Total) - minmemroy
			maxSlots = int64(v.Total) / 2
		}
	}

	if maxSlots < minmemroy {
		maxSlots = minmemroy
	}

	blog.Infof("memory slot: set max local memory:%d", maxSlots)

	waitingList := list.New()

	return &memorySlot{
		totalSlots:    maxSlots,
		occupiedSlots: 0,

		lockChan:   make(chanChanPair, 1000),
		unlockChan: make(chanChanPair, 1000),

		waitingList: waitingList,
	}
}

type chanResult chan struct{}

type chanPair struct {
	result chanResult
	weight int64
}

type chanChanPair chan chanPair

type memorySlot struct {
	ctx context.Context

	totalSlots    int64
	occupiedSlots int64

	lockChan   chanChanPair
	unlockChan chanChanPair

	handling bool

	waitingList *list.List
}

// brings handler up and begin to handle requests
func (lr *memorySlot) Handle(ctx context.Context) {
	if lr.handling {
		return
	}

	lr.handling = true

	go lr.handleLock(ctx)
}

// GetStatus get current slots status
func (lr *memorySlot) GetStatus() (int64, int64) {
	return lr.totalSlots, lr.occupiedSlots
}

// Lock get an usage lock, success with true, failed with false
func (lr *memorySlot) Lock(weight int64) bool {
	if !lr.handling {
		return false
	}

	msg := chanPair{weight: weight, result: make(chanResult, 1)}
	lr.lockChan <- msg

	select {
	case <-lr.ctx.Done():
		return false

	// wait result
	case <-msg.result:
		return true
	}
}

// Unlock release an usage lock
func (lr *memorySlot) Unlock(weight int64) {
	if !lr.handling {
		return
	}

	msg := chanPair{weight: weight, result: nil}
	lr.unlockChan <- msg
}

func (lr *memorySlot) handleLock(ctx context.Context) {
	lr.ctx = ctx

	for {
		select {
		case <-ctx.Done():
			lr.handling = false
			return
		case pairChan := <-lr.unlockChan:
			lr.putSlot(pairChan)
		case pairChan := <-lr.lockChan:
			lr.getSlot(pairChan)
		}
	}
}

func (lr *memorySlot) getSlot(pairChan chanPair) {
	// blog.Infof("memory slot: before get slot occpy size:%d,total size:%d,wait length:%d", lr.occupiedSlots, lr.totalSlots, lr.waitingList.Len())

	if lr.occupiedSlots < lr.totalSlots {
		lr.occupiedSlots += pairChan.weight
		pairChan.result <- struct{}{}
	} else {
		lr.waitingList.PushBack(pairChan)
	}

	// blog.Infof("memory slot: after get slot occpy size:%d,total size:%d,wait length:%d", lr.occupiedSlots, lr.totalSlots, lr.waitingList.Len())
}

func (lr *memorySlot) putSlot(pairChan chanPair) {
	// blog.Infof("memory slot: before put slot occpy size:%d,total size:%d,wait length:%d", lr.occupiedSlots, lr.totalSlots, lr.waitingList.Len())

	lr.occupiedSlots -= pairChan.weight
	if lr.occupiedSlots < 0 {
		lr.occupiedSlots = 0
	}

	for lr.occupiedSlots < lr.totalSlots && lr.waitingList.Len() > 0 {
		e := lr.waitingList.Front()
		if e != nil {
			tempchan := e.Value.(chanPair)
			lr.occupiedSlots += tempchan.weight

			// awake this task
			tempchan.result <- struct{}{}

			// delete this element
			lr.waitingList.Remove(e)
		}
	}

	// blog.Infof("memory slot: after put slot occpy size:%d,total size:%d,wait length:%d", lr.occupiedSlots, lr.totalSlots, lr.waitingList.Len())
}
