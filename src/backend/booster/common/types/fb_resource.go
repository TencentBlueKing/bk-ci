/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

// FBClusterResource describe the fastbuild cluster resource
type FBClusterResource struct {
	DiskTotal float64              `json:"disktotal"`
	MemTotal  float64              `json:"memtotal"`
	CPUTotal  float64              `json:"cputotal"`
	DiskUsed  float64              `json:"diskused"`
	MemUsed   float64              `json:"memused"`
	CPUUsed   float64              `json:"cpuused"`
	Agents    []FBClusterAgentInfo `json:"agents"`
}

// FBClusterAgentInfo describe the fastbuild single node resource
type FBClusterAgentInfo struct {
	HostName  string  `json:"hostname"`
	IP        string  `json:"ip"`
	DiskTotal float64 `json:"disktotal"`
	MemTotal  float64 `json:"memtotal"`
	CPUTotal  float64 `json:"cputotal"`
	DiskUsed  float64 `json:"diskused"`
	MemUsed   float64 `json:"memused"`
	CPUUsed   float64 `json:"cpuused"`

	Disabled   bool               `json:"disabled"`
	Attributes []FBAgentAttribute `json:"attributes"`
}

// FBAgentAttribute describe the fastbuild single node labels
type FBAgentAttribute struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}
