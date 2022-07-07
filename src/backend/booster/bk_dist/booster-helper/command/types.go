/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at http://opensource.org/licenses/MIT
 *
 */

package command

import (
	"fmt"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"
)

// ClientType define client type
type ClientType string

// define vars
var (
	ClientBKBoosterHelper ClientType = "bk-booster-helper"

	ProdBuildBoosterGatewayDomain = ""
	ProdBuildBoosterGatewayPort   = ""
	ProdBuildBoosterGatewayHost   = fmt.Sprintf("http://%s:%s/api",
		ProdBuildBoosterGatewayDomain, ProdBuildBoosterGatewayPort)

	TestdBuildBoosterGatewayDomain = ""
	TestBuildBoosterGatewayPort    = ""
	TestBuildBoosterGatewayHost    = fmt.Sprintf("http://%s:%s/api",
		TestdBuildBoosterGatewayDomain, TestBuildBoosterGatewayPort)

	LocalTestDomain = "http://bktbs.com:8081/gateway/api"
	gatewayHost     = ""

	projectNeedIncreaseCpu []CpuStats
	projectNeedDecreaseCpu []CpuStats
	projectCpuReasonable   []CpuStats
)

// const vars
const (
	BKBoosterBoosterHelperUsage = "BlueKing Booster Helper"

	GetProjectSettingURI     = "/v1/disttask/resource/project?project_id="
	GetProjectListURI        = "/v1/disttask/resource/project"
	GetProjectWorkerStatsURI = "/v1/disttask/resource/stats?task_id="
	GetTaskInfoURI           = "/v1/disttask/resource/task?project_id="

	ProjectSelector = "?selector=project_id"
	TaskSelector    = "&selector=task_id,request_cpu,cpu_total,start_time,status"
)

// error
var (
	ErrProjectidMissed   = fmt.Errorf("missing project_id")
	ErrBoosterTypeMissed = fmt.Errorf("missing booster_type")
	ErrBoosterTypeWrong  = fmt.Errorf("wrong booster type")
	ErrServerNotFound    = fmt.Errorf("server not found")
	ErrProjectNotFound   = fmt.Errorf("project not found")
	ErrDecode            = fmt.Errorf("decode error")
	ErrGetFailed         = fmt.Errorf("get project info failed")
	ErrGetServerfailed   = fmt.Errorf("get server failed")
	ErrDayFormatWrong    = fmt.Errorf("flag day need int value")
	ErrNoWork            = fmt.Errorf("no work in this task")
	ErrScopeFormatWrong  = fmt.Errorf("scope invalid,use default scope ")
	ErrNotInStats        = fmt.Errorf("not in stats")
)

type ProjectInfo struct {
	Setting []disttask.TableProjectSetting `json:"data"`
}

type TaskList struct {
	Tasks []disttask.TableTask `json:"data"`
}

type WorkStats struct {
	Works []disttask.TableWorkStats `json:"data"`
}

type JobStats struct {
	Jobs []sdk.ControllerJobStats
}

type JobTime struct {
	startTime int64
	endTime   int64
}

type TaskInfo struct {
	taskID         string
	maxConcurrency float64
}

type CpuStats struct {
	projectID      string
	requestCpu     float64
	taskSum        int
	validTaskSum   int
	taskDistribute [][]TaskInfo
}

func (ct ClientType) Name() string {
	switch ct {
	case ClientBKBoosterHelper:
		return string(ct)
	}
	return "unknown"
}

// Usage return client usage
func (ct ClientType) Usage() string {
	switch ct {
	case ClientBKBoosterHelper:
		return BKBoosterBoosterHelperUsage
	}
	return "unknown"
}
