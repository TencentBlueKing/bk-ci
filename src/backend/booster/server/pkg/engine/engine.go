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
	"github.com/jinzhu/gorm"
)

// Engine describe how different distribute service work under the manager
type Engine interface {

	// Name() is to get this engine name, this name must match the project.EngineName
	Name() TypeName

	// SelectFirstTaskBasic receive a task queue group(which contains many queues) and the target queue name
	// let the engine decide which task basic should be get in priority
	// basically, queueGroup.GetQueue(queueName).First() is just fine
	SelectFirstTaskBasic(queueGroup *TaskQueueGroup, queueName string) (*TaskBasic, error)

	// CreateTaskExtension provide the task basic data and the extra data,
	// extra data should be defined by different engines and decode inside engines
	// engine should generate the concerned data and save it during this function
	CreateTaskExtension(tb *TaskBasic, extra []byte) error

	// Get TaskExtension from engine
	// Task provide many public functions so that manager don't have to ask engine everything between progresses
	GetTaskExtension(taskID string) (TaskExtension, error)

	// LaunchTask tell engine to launch the distribute service
	// it should be asynchronous and return immediately after exec the launch job
	// if there is no enough resources for task launching, just return engine.ErrorNoEnoughResources
	LaunchTask(tb *TaskBasic, queueName string) error

	// LaunchDone is used to check if the launch job is done
	LaunchDone(taskID string) (bool, error)

	// CheckTask let engine check this task status
	CheckTask(tb *TaskBasic) error

	// DegradeTask will be called when manager decide to degrade this task, such as staging timeout
	// engine can just return an error if no support degrade mode.
	DegradeTask(taskID string) error

	// SendProjectMessage provide the message to engine,
	// it should be decode inside the engines
	// then get data back
	SendProjectMessage(projectID string, message []byte) ([]byte, error)

	// SendTaskMessage provide the message to engine,
	// it should be decode inside the engines
	// then get data back
	SendTaskMessage(taskID string, message []byte) ([]byte, error)

	// CollectTaskData tell engine that this distribute service will not be used any more,
	// fell free to collect data or some after-work jobs
	CollectTaskData(tb *TaskBasic) error

	// ReleaseTask tell engine to shutdown the service and release the resource
	ReleaseTask(taskID string) error

	// GetPreferences return the engine preferences settings
	GetPreferences() Preferences

	// GetTaskBasicTable return the DB instance where TableTaskBasic is
	GetTaskBasicTable() *gorm.DB

	// GetProjectBasicTable return the DB instance where TableProjectBasic is
	GetProjectBasicTable() *gorm.DB

	// GetProjectInfoBasicTable return the DB instance where TableProjectInfoBasic is
	GetProjectInfoBasicTable() *gorm.DB

	// GetWhitelistBasicTable return the DB instance where TableWhitelistBasic is
	GetWhitelistBasicTable() *gorm.DB
}

// Engine type name
type TypeName string

// String return the engine type name
func (t TypeName) String() string {
	return string(t)
}

// Preferences describe the custom settings by different engines.
// these settings matters when manager handle some public works such as heartbeat.
type Preferences struct {
	// heartbeat timeout = heartbeat tick gap * tick times
	HeartbeatTimeoutTickTimes int
}
