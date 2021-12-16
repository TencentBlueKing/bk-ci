/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package command

import (
	"fmt"
)

// ClientType define client type
type ClientType string

// define vars
var (
	ClientBKBooster ClientType = "bk-booster"

	ProdBuildBoosterServerDomain = ""
	ProdBuildBoosterServerPort   = ""
	ProdBuildBoosterServerHost   = fmt.Sprintf("http://%s:%s/api",
		ProdBuildBoosterServerDomain, ProdBuildBoosterServerPort)

	TestBuildBoosterServerDomain = ""
	TestBuildBoosterServerPort   = ""
	TestBuildBoosterServerHost   = fmt.Sprintf("http://%s:%s/api",
		TestBuildBoosterServerDomain, TestBuildBoosterServerPort)

	ControllerScheme = "http"
	ControllerIP     = "127.0.0.1"
	ControllerPort   = 30117
)

// const vars
const (
	ClientBKBoosterUsage = "BlueKing Booster Client"
)

// Name return client name
func (ct ClientType) Name() string {
	switch ct {
	case ClientBKBooster:
		return string(ct)
	}
	return "unknown"
}

// Usage return client usage
func (ct ClientType) Usage() string {
	switch ct {
	case ClientBKBooster:
		return ClientBKBoosterUsage
	}
	return "unknown"
}
