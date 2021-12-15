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
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
)

// Manager describe the build-booster server exported methods
// beyond this we can write a http-server to provide build service.
type Manager interface {
	// Run brings up the manager processor
	Run()

	// CreateTask receive TaskCreateParam and create a new task from setting.
	// since task was created, its lifetime begins with procedure, until it has been released.
	CreateTask(param *TaskCreateParam) (*engine.TaskBasic, error)

	// ReleaseTask receive TaskReleaseParam and release a task into terminated status.
	ReleaseTask(param *TaskReleaseParam) error

	// SendMessage receive the raw data and send it into engine, then get the message back
	SendProjectMessage(projectID string, data []byte) ([]byte, error)

	// SendMessage receive the raw data and send it into engine, then get the message back
	SendTaskMessage(taskID string, data []byte) ([]byte, error)

	// GetTaskRank return the rank of the specified task in waiting queue.
	GetTaskRank(taskID string) (int, error)

	// GetTaskExtension return the task extension with given taskID .
	GetTaskExtension(taskID string) (engine.TaskExtension, error)

	// GetTaskBasic return the unterminated task basic from cache.
	// if task basic does not exist or has already in terminated status, it return error ErrorUnterminatedTaskNoFound
	GetTaskBasic(taskID string) (*engine.TaskBasic, error)

	// GetTask return both task extension and task basic with given taskID .
	GetTask(taskID string) (*engine.TaskBasic, engine.TaskExtension, error)

	// UpdateHeartbeat support update heartbeat for unterminated task
	UpdateHeartbeat(taskID string) error
}

// TaskCreateParam describe the param struct when creating a new task
type TaskCreateParam struct {
	// specify which projectID the task belongs to
	// project settings will be pulled and set into task settings.
	ProjectID string

	// specify which buildID the task belongs to
	// does not matter in build-booster server, just a manual index for task
	BuildID string

	// client version for requesting for the creation action
	ClientVersion string

	// client local cpu num from the requesting machine
	ClientCPU int

	// IP v4 of the requesting machine
	ClientIP string

	// Client message for this task
	Message string

	// Extra contains the raw string for different engines' settings
	// it is the engine's responsibility to decode from it.
	Extra string
}

// TaskReleaseParam describe the param struct when releasing a unterminated task
type TaskReleaseParam struct {
	// specify which task will be released
	TaskID string

	// Client message for this task
	Message string

	// if the task success or not, deciding the status of the task in terminated, success or failed.
	Success bool

	// Extra contains the raw string for different engines' settings
	// it is the engine's responsibility to decode from it.
	Extra string
}
