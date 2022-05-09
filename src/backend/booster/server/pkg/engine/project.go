/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package engine

// ProjectBasic describe the basic settings of project,
type ProjectBasic struct {

	// PRIMARY key, which should be unique in global(among all engines).
	ProjectID string

	// detail name for this project
	ProjectName string

	// detail descriptions for this project
	Message string

	// Priority defines the priority of tasks launched by this project
	// which matters the task position in waiting queue
	Priority TaskPriority

	// QueueName defines which queue will manage the tasks launched by this project.
	QueueName string

	// EngineName decide which engine will be used to launch the tasks from this project
	EngineName TypeName

	// StageTimeout defines the timeout(seconds) when tasks in staging status.
	// After timeout, the task will be regarded as in the situation "no enough resources",
	// then the task will be do the regraded progress
	StageTimeout int

	// Concurrency defines the max concurrency unterminated task under this projects
	// a new task which reach the limits will be rejected.
	Concurrency int
}

// WhitelistBasic describe the basic whitelist information
// One whitelist record means:
//  A request from the IP, ask to applying a new task under the Project, will be approved.
type WhitelistBasic struct {
	ProjectID string
	IP        string
	Message   string
}
