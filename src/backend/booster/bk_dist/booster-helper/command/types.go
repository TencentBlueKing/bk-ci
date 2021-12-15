/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at http://opensource.org/licenses/MIT
 *
 */

package command

import (
	"build-booster/server/pkg/engine/disttask"
	"fmt"
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
	TestBuildBoosterGatewayHost    = fmt.Sprintf("http://%s:%s/gateway/api",
		TestdBuildBoosterGatewayDomain, TestBuildBoosterGatewayPort)
)

// const vars
const (
	BKBoosterBoosterHelperUsage = "BlueKing Booster Helper"

	GetProjectSettingURI = "/v1/disttask/resource/project?project_id="
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
)

type ProjectInfo struct {
	Setting []disttask.TableProjectSetting `json:"data"`
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
