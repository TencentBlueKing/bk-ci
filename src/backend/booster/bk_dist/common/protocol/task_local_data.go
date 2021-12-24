/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package protocol

// TaskKey key for task
type TaskKey struct {
	TaskID    string `json:"task_id"` // if not obtained from server, should generated one
	ProjectID string `json:"project_id"`
	BuildID   string `json:"build_id"`
	ProcessID string `json:"process_id"`
}

// RegTaskHostsReq json struct to register hosts for one task
type RegTaskHostsReq struct {
	Taskinfo TaskKey
	Hosts    []Host `json:"hosts"`
	HostsRaw string `json:"hosts_raw"`
}

// RegTaskHostsRsp response data for reg-hosts
type RegTaskHostsRsp struct {
	Retcode int32  `json:"retcode"`
	Message string `json:"message"`
}

// UnregTaskHostsReq json struct to unregister hosts for one task
type UnregTaskHostsReq struct {
	Taskinfo TaskKey
	Hosts    []Host `json:"hosts"`
	HostsRaw string `json:"hosts_raw"`
}

// UnregTaskHostsRsp response data for unreg-hosts
type UnregTaskHostsRsp struct {
	Retcode int32  `json:"retcode"`
	Message string `json:"message"`
}

// HostSlot to desc slot of host
type HostSlot struct {
	Host         Host  `json:"host"`
	SlotSequence int32 `json:"slot_sequence"`
}

// GetHostReq json struct to get host
type GetHostReq struct {
	TaskID  string `json:"task_id"`
	SlotNum int32  `json:"slot_num"`
}

// GetHostRsp response data for get host slot
type GetHostRsp struct {
	Retcode int32      `json:"retcode"`
	Message string     `json:"message"`
	TaskID  string     `json:"task_id"`
	Slots   []HostSlot `json:"host_slots"`
}

// ReturnHostReq request data to return host slot
type ReturnHostReq struct {
	TaskID string     `json:"task_id"`
	Slots  []HostSlot `json:"host_slots"`
}

// ReturnHostRsp response data for return host slot
type ReturnHostRsp struct {
	Retcode int32  `json:"retcode"`
	Message string `json:"message"`
}
