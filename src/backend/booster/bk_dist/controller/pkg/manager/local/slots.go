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
	"container/list"
	"context"

	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

func newResource(maxSlots int, usageLimit map[dcSDK.JobUsage]int) *resource {
	if maxSlots <= 0 {
		maxSlots = 1
	}

	usageMap := make(map[dcSDK.JobUsage]*usageSet, 10)
	waitingList := make(map[dcSDK.JobUsage]*list.List, 10)
	for k, v := range usageLimit {
		if v <= 0 {
			v = maxSlots
		}
		usageMap[k] = &usageSet{
			limit:    v,
			occupied: 0,
		}

		waitingList[k] = list.New()
	}
	usageMap[dcSDK.JobUsageDefault] = &usageSet{
		limit:    maxSlots,
		occupied: 0,
	}
	waitingList[dcSDK.JobUsageDefault] = list.New()

	usages := make([]dcSDK.JobUsage, 0, 0)
	for k := range waitingList {
		usages = append(usages, k)
	}

	for _, v := range usageMap {
		blog.Infof("local slot: usage map:%v after new resource", *v)
	}
	blog.Infof("local slot: total slots:%d after new resource", maxSlots)

	return &resource{
		totalSlots:    maxSlots,
		occupiedSlots: 0,
		usageMap:      usageMap,
		lockChan:      make(chanChanPair, 1000),
		unlockChan:    make(chanChanPair, 1000),

		waitingList: waitingList,

		usages:   usages,
		cursor:   0,
		totallen: len(usages),
	}
}

type chanResult chan struct{}

type chanPair struct {
	jobUsage dcSDK.JobUsage
	result   chanResult
	weight   int32
}

type chanChanPair chan chanPair

type usageSet struct {
	limit    int
	occupied int
}

type resource struct {
	ctx context.Context

	totalSlots    int
	occupiedSlots int

	usageMap map[dcSDK.JobUsage]*usageSet

	lockChan   chanChanPair
	unlockChan chanChanPair

	handling bool

	// change from one list to multi-list for each job usage
	waitingList map[dcSDK.JobUsage]*list.List

	usages   []dcSDK.JobUsage
	cursor   int
	totallen int
}

// brings handler up and begin to handle requests
func (lr *resource) Handle(ctx context.Context) {
	if lr.handling {
		return
	}

	lr.handling = true
	blog.Infof("local slot: is handling now")

	go lr.handleLock(ctx)
}

// GetStatus get current slots status
func (lr *resource) GetStatus() (int, int) {
	return lr.totalSlots, lr.occupiedSlots
}

// Lock get an usage lock, success with true, failed with false
func (lr *resource) Lock(usage dcSDK.JobUsage, weight int32) bool {
	if !lr.handling {
		blog.Infof("local: failed to lock for resource not handling")
		return false
	}

	var realweight int32 = 1
	if weight > 1 {
		realweight = weight
	}
	msg := chanPair{jobUsage: usage, weight: realweight, result: make(chanResult, 1)}
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
func (lr *resource) Unlock(usage dcSDK.JobUsage, weight int32) {
	if !lr.handling {
		return
	}

	var realweight int32 = 1
	if weight > 1 {
		realweight = weight
	}
	msg := chanPair{jobUsage: usage, weight: realweight, result: nil}
	lr.unlockChan <- msg
}

func (lr *resource) handleLock(ctx context.Context) {
	lr.ctx = ctx

	for {
		select {
		case <-ctx.Done():
			lr.handling = false
			blog.Infof("local slot: stop handling now for ctx.Done")
			return
		case pairChan := <-lr.unlockChan:
			lr.putSlot(pairChan)
		case pairChan := <-lr.lockChan:
			lr.getSlot(pairChan)
		}
	}
}

func (lr *resource) getusageSet(usage dcSDK.JobUsage) *usageSet {
	set, ok := lr.usageMap[usage]
	if !ok {
		// unknown usage, get default usage
		set = lr.usageMap[dcSDK.JobUsageDefault]
	}

	return set
}

func (lr *resource) isIdle(set *usageSet) bool {
	if set == nil {
		return false
	}

	if set.occupied < set.limit {
		return true
	}

	return false
}

func (lr *resource) getList(usage dcSDK.JobUsage) *list.List {
	l, ok := lr.waitingList[usage]
	if !ok {
		// unknown usage, get default usage
		l = lr.waitingList[dcSDK.JobUsageDefault]
	}

	return l
}

// 轮询查找合适的等待队列（该队列有排队的任务，并且该队列对应的场景还有空闲的锁）
func (lr *resource) getListByPoll() (*list.List, error) {
	for start := 0; start < lr.totallen; start++ {
		lr.cursor = lr.cursor % lr.totallen
		usage := lr.usages[lr.cursor]
		lr.cursor++
		set := lr.getusageSet(usage)
		if !lr.isIdle(set) {
			continue
		}
		l := lr.getList(usage)
		if l.Len() > 0 {
			return l, nil
		}
	}

	return nil, types.ErrNoWaitingTask
}

func (lr *resource) getSlot(pairChan chanPair) {
	satisfied := false
	usage := pairChan.jobUsage
	if lr.occupiedSlots < lr.totalSlots {
		set := lr.getusageSet(usage)
		if lr.isIdle(set) {
			set.occupied += int(pairChan.weight)
			lr.occupiedSlots += int(pairChan.weight)
			pairChan.result <- struct{}{}
			satisfied = true
		}
	}

	if !satisfied {
		l := lr.getList(usage)
		l.PushBack(pairChan)
	}
}

func (lr *resource) putSlot(pairChan chanPair) {
	lr.occupiedSlots -= int(pairChan.weight)
	if lr.occupiedSlots < 0 {
		lr.occupiedSlots = 0
	}
	usage := pairChan.jobUsage
	set := lr.getusageSet(usage)
	set.occupied -= int(pairChan.weight)
	if set.occupied < 0 {
		set.occupied = 0
	}

	// 在锁增加了权重后，一次可能会释放多个锁，这些锁需要均衡分配给各个等待队列
	for lr.occupiedSlots < lr.totalSlots {
		selectedlist, err := lr.getListByPoll()
		if err != nil || selectedlist == nil {
			return
		}

		// 唤醒选中队列的第一个任务
		if selectedlist.Len() > 0 {
			e := selectedlist.Front()
			if e != nil {
				tempchan := e.Value.(chanPair)
				usage := tempchan.jobUsage
				set := lr.getusageSet(usage)
				if lr.isIdle(set) {
					set.occupied += int(tempchan.weight)
					lr.occupiedSlots += int(tempchan.weight)

					// awake this task
					tempchan.result <- struct{}{}

					// delete this element
					selectedlist.Remove(e)
				}
			}
		}
	}
}
