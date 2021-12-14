/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package v2

import "github.com/Tencent/bk-ci/src/booster/server/pkg/engine"

const (
	queryTaskIDKey = "task_id"
)

// ParamApply describe the protocol of applying a piece of resources for distribute workers
// and there will be a TaskBasic to maintain the resource lifetime.
type ParamApply struct {
	// project_id describe which project this task belongs to
	// project is used to check whitelist and get static settings.
	ProjectID string `json:"project_id"`

	// scene describe the addition unique part of this project.
	// If it is empty, the project_id is the final project_id,
	// else, the project_id should be made of project_id + scene
	Scene string `json:"scene"`

	// build_id is not used during the whole lifetime, just a specific key for query
	BuildID string `json:"build_id"`

	// client_version
	ClientVersion string `json:"client_version"`

	Message   string `json:"message"`
	ClientCPU int    `json:"client_cpu"`

	// extra is a raw string contains the extra configurations for specific engine,
	// it will be parsed in engine handler.
	Extra string `json:"extra"`
}

// RespTaskInfo contains the return data of Apply and Query
type RespTaskInfo struct {
	TaskID      string                `json:"task_id"`
	Status      engine.TaskStatusType `json:"status"`
	HostList    []string              `json:"host_list"`
	QueueNumber int                   `json:"queue_number"`
	Message     string                `json:"message"`

	Extra string `json:"extra"`
}

// ParamHeartbeat contains the UpdateHeartbeat request data
type ParamHeartbeat struct {
	TaskID string `json:"task_id"`
	Type   string `json:"type"`
}

// RespHeartbeat contains the return data of UpdateHeartbeat
type RespHeartbeat struct {
	TaskID string `json:"task_id"`
	Type   string `json:"type"`
}

type HeartBeatType string

// get string data from heartbeat type
func (h HeartBeatType) String() string {
	return string(h)
}

const (
	HeartBeatPing HeartBeatType = "ping"
	HeartBeatPong HeartBeatType = "pong"
)

// ParamRelease contains the ReleaseTask request data
type ParamRelease struct {
	TaskID  string `json:"task_id"`
	Message string `json:"message"`
	Success bool   `json:"success"`

	// extra is a raw string contains the summary data from client for specific engine,
	// it will be parsed in engine handler.
	Extra string `json:"extra"`
}

// ParamMessage contains the SendMessage request data
type ParamMessage struct {
	Type      MessageType `json:"type"`
	TaskID    string      `json:"task_id"`
	ProjectID string      `json:"project_id"`
	Scene     string      `json:"scene"`
	Extra     string      `json:"extra"`
}

type MessageType string

const (
	MessageTask    MessageType = "task"
	MessageProject MessageType = "project"
)
