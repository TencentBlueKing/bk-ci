/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package engine

import (
	"container/list"
	"sync"
	"sync/atomic"
)

// StagingTaskQueue maintains a priority-driven queue for TaskBasic
type StagingTaskQueue interface {
	// get length of this queue
	Len() int64

	// list all TaskBasic from this queue, order by Priority and CreateTime(in same Priority)
	All() []*TaskBasic

	// get first instance from this queue
	First() (*TaskBasic, error)

	// check if task exist
	Exist(taskID string) bool

	// get the specific TaskBasic rank in this queue
	Rank(taskID string) (int, error)

	// add new TaskBasic into this queue
	// the ordering will be adjusted automatically
	Add(task *TaskBasic)

	// delete the specific TaskBasic from this queue
	Delete(taskID string) error

	// clear all elements from this queue
	Clear()
}

// NewStagingTaskQueue get a new, empty, initialized StagingTaskQueue
func NewStagingTaskQueue() StagingTaskQueue {
	return &stagingTaskQueue{
		length:         0,
		listGroup:      make(map[TaskPriority]*queueSubList, 100),
		taskElementMap: make(map[string]*list.Element, 100),
	}
}

type stagingTaskQueue struct {
	sync.Mutex
	length         int64
	listGroup      map[TaskPriority]*queueSubList
	taskElementMap map[string]*list.Element
}

// Len get length of queue
func (stq *stagingTaskQueue) Len() int64 {
	return atomic.LoadInt64(&stq.length)
}

// All list all items from queue
func (stq *stagingTaskQueue) All() []*TaskBasic {
	r := make([]*TaskBasic, 0, 100)
	stq.Lock()
	defer stq.Unlock()

	for priority := MaxTaskPriority; priority >= MinTaskPriority; priority-- {
		subList, ok := stq.listGroup[priority]
		if !ok || subList.Len() == 0 {
			continue
		}

		for e := subList.Front(); e != nil; e = e.Next() {
			r = append(r, e.Value.(*TaskBasic))
		}
	}
	return r
}

// First get first item from queue
func (stq *stagingTaskQueue) First() (*TaskBasic, error) {
	stq.Lock()
	defer stq.Unlock()

	for priority := MaxTaskPriority; priority >= MinTaskPriority; priority-- {
		subList, ok := stq.listGroup[priority]
		if !ok || subList.Len() == 0 {
			continue
		}
		task := subList.Front().Value.(*TaskBasic)
		return CopyTaskBasic(task), nil
	}
	return nil, ErrorNoTaskInQueue
}

// Exist check if the task is in queue
func (stq *stagingTaskQueue) Exist(taskID string) bool {
	stq.Lock()
	defer stq.Unlock()

	_, ok := stq.taskElementMap[taskID]
	return ok
}

// Rank get this task's rank in queue
func (stq *stagingTaskQueue) Rank(taskID string) (int, error) {
	stq.Lock()
	defer stq.Unlock()

	element, ok := stq.taskElementMap[taskID]
	if !ok {
		return -1, ErrorTaskNoFound
	}

	rank := 0
	task := element.Value.(*TaskBasic)
	for element = element.Prev(); element != nil; element = element.Prev() {
		rank++
	}

	for priority := MaxTaskPriority; priority > task.Client.Priority; priority-- {
		if _, ok := stq.listGroup[priority]; ok {
			rank += stq.listGroup[priority].Len()
		}
	}

	return rank, nil
}

// Add add a new item into queue
func (stq *stagingTaskQueue) Add(tbRaw *TaskBasic) {
	stq.Lock()
	defer stq.Unlock()

	tb := CopyTaskBasic(tbRaw)
	if _, ok := stq.taskElementMap[tb.ID]; ok {
		return
	}

	priority := tb.Client.Priority
	if _, ok := stq.listGroup[priority]; !ok {
		stq.listGroup[priority] = newQueueSubList()
	}
	subList := stq.listGroup[priority]
	stq.taskElementMap[tb.ID] = subList.insert(tb)
	atomic.AddInt64(&stq.length, 1)
}

// Delete delete a item from queue, if no exists, it return error ErrorTaskNoFound
func (stq *stagingTaskQueue) Delete(taskID string) error {
	stq.Lock()
	defer stq.Unlock()

	element, ok := stq.taskElementMap[taskID]
	if !ok {
		return ErrorTaskNoFound
	}

	for _, subList := range stq.listGroup {
		subList.Remove(element)
	}

	delete(stq.taskElementMap, taskID)
	atomic.AddInt64(&stq.length, -1)
	return nil
}

// Clear clean all data in queue
func (stq *stagingTaskQueue) Clear() {
	stq.Lock()
	defer stq.Unlock()

	atomic.StoreInt64(&stq.length, 0)
	stq.listGroup = make(map[TaskPriority]*queueSubList, 100)
	stq.taskElementMap = make(map[string]*list.Element, 100)
}

func newQueueSubList() *queueSubList {
	return &queueSubList{list.New()}
}

type queueSubList struct {
	*list.List
}

func (qsl *queueSubList) insert(task *TaskBasic) *list.Element {
	for t := qsl.Back(); t != nil; t = t.Prev() {
		if queueTask := t.Value.(*TaskBasic); queueTask.Status.CreateTime.Before(task.Status.CreateTime) {
			return qsl.InsertAfter(task, t)
		}
	}
	return qsl.PushFront(task)
}

// NewTaskQueueGroup get a new, empty, initialized TaskQueueGroup
func NewTaskQueueGroup() *TaskQueueGroup {
	return &TaskQueueGroup{
		queue: make(map[string]StagingTaskQueue, 100),
	}
}

// TaskQueueGroup handle a few StagingTaskQueues
type TaskQueueGroup struct {
	sync.Mutex
	queue map[string]StagingTaskQueue
}

// GetQueue get the specific queue from group
func (tqg *TaskQueueGroup) GetQueue(name string) StagingTaskQueue {
	tqg.Lock()
	defer tqg.Unlock()

	if _, ok := tqg.queue[name]; !ok {
		tqg.queue[name] = NewStagingTaskQueue()
	}

	return tqg.queue[name]
}

// DeleteTask delete a task from all queues of groups
func (tqg *TaskQueueGroup) DeleteTask(taskID string) bool {
	tqg.Lock()
	defer tqg.Unlock()

	ok := false
	for _, queue := range tqg.queue {
		if err := queue.Delete(taskID); err == nil {
			ok = true
		}
	}

	return ok
}

// Exist check if the task is in any queues of groups
func (tqg *TaskQueueGroup) Exist(taskID string) bool {
	tqg.Lock()
	defer tqg.Unlock()

	for _, queue := range tqg.queue {
		if ok := queue.Exist(taskID); ok {
			return true
		}
	}

	return false
}

// QueueBriefInfo describe a brief information of a queue
// it is a queue - engine pair
type QueueBriefInfo struct {
	QueueName  string
	EngineName TypeName
}

type QueueShareType int

const (
	QueueShareTypeAllAllowed QueueShareType = iota
	QueueShareTypeOnlyTakeFromPublic
	QueueShareTypeOnlyGiveToPublic
	QueueShareTypeNoneAllowed
)
