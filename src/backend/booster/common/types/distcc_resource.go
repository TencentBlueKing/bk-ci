/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

// BcsClusterResource describe the bcs cluster resource report status.
type BcsClusterResource struct {
	DiskTotal float64               `json:"disktotal"`
	MemTotal  float64               `json:"memtotal"`
	CPUTotal  float64               `json:"cputotal"`
	DiskUsed  float64               `json:"diskused"`
	MemUsed   float64               `json:"memused"`
	CPUUsed   float64               `json:"cpuused"`
	Agents    []BcsClusterAgentInfo `json:"agents"`
}

// BcsClusterAgentInfo describe the bcs single agent resource report status.
type BcsClusterAgentInfo struct {
	HostName  string  `json:"hostname"`
	IP        string  `json:"ip"`
	DiskTotal float64 `json:"disktotal"`
	MemTotal  float64 `json:"memtotal"`
	CPUTotal  float64 `json:"cputotal"`
	DiskUsed  float64 `json:"diskused"`
	MemUsed   float64 `json:"memused"`
	CPUUsed   float64 `json:"cpuused"`

	Disabled       bool                 `json:"disabled"`
	HostAttributes []*BcsAgentAttribute `json:"host_attributes"`
	Attributes     []*BcsAgentAttribute `json:"attributes"`

	RegisteredTime   int64 `json:"registered_time"`
	ReRegisteredTime int64 `json:"reregistered_time"`
}

// MesosValueScalar scalar value type.
type MesosValueScalar struct {
	Value float64 `json:"value"`
}

// MesosValueRanges range value type.
type MesosValueRanges struct {
	Begin uint64 `json:"begin"`
	End   uint64 `json:"end"`
}

// MesosValueText text value type.
type MesosValueText struct {
	Value string `json:"value"`
}

// MesosValueSet set value type.
type MesosValueSet struct {
	Item []string `json:"item"`
}

type MesosValueType int32

// BcsAgentAttribute define the bcs single agent's attributes.
type BcsAgentAttribute struct {
	Name   string              `json:"name,omitempty"`
	Type   MesosValueType      `json:"type,omitempty"`
	Scalar *MesosValueScalar   `json:"scalar,omitempty"`
	Ranges []*MesosValueRanges `json:"ranges,omitempty"`
	Set    *MesosValueSet      `json:"set,omitempty"`
	Text   *MesosValueText     `json:"text,omitempty"`
}
