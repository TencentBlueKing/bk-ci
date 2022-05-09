/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package resource

import (
	"time"
)

const (
	PlatformLinux   = "linux"
	PlatformWindows = "windows"
	PlatformMacOS   = "darwin"
)

// Details describe the details from both crm and drm
type Details struct {
	Rsc []*RscDetails `json:"rsc"`
	App []*AppDetails `json:"app"`
}

// RscDetails describe the resource detail from both crm and drm
type RscDetails struct {
	Labels string `json:"labels"`

	CPUTotal float64 `json:"cpu_total"`
	CPUUsed  float64 `json:"cpu_used"`
	MemTotal float64 `json:"mem_total"`
	MemUsed  float64 `json:"mem_used"`

	CPUPerInstance    float64 `json:"cpu_per_instance"`
	AvailableInstance int     `json:"available_instance"`
	ReportInstance    int     `json:"report_instance"`
	NotReadyInstance  int     `json:"not_ready_instance"`
}

// AppDetails describe the application detail from both crm and drm
type AppDetails struct {
	ResourceID string `json:"resource_id"`
	BrokerID   string `json:"broker_id"`
	BrokerName string `json:"broker_name"`
	BrokerSold bool   `json:"broker_sold"`

	User       string    `json:"user"`
	Status     string    `json:"status"`
	Image      string    `json:"image"`
	CreateTime time.Time `json:"create_time"`

	RequestInstance  int    `json:"request_instance"`
	NotReadyInstance int    `json:"not_ready_instance"`
	Label            string `json:"label"`
}
