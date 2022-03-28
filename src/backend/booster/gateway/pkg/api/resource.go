/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package api

import (
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
	"github.com/Tencent/bk-ci/src/booster/gateway/config"
	rd "github.com/Tencent/bk-ci/src/booster/gateway/pkg/register-discover"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/apisjob"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/fastbuild"
)

var Rd rd.RegisterDiscover

// DistCCServerAPIResource describe all the distcc api resources
type DistCCServerAPIResource struct {
	ActionsV1 []*httpserver.Action
	MySQL     distcc.MySQL
	Conf      *config.GatewayConfig
}

var distCCServerAPI = DistCCServerAPIResource{}

// GetDistCCServerAPIResource return the singleton distcc api resource
func GetDistCCServerAPIResource() *DistCCServerAPIResource {
	return &distCCServerAPI
}

// InitActions add actions
func (a *DistCCServerAPIResource) InitActions() {
	a.ActionsV1 = append(a.ActionsV1, GetAPIV1Action()...)
}

// FBServerAPIResource describe all the fastbuild api resources
type FBServerAPIResource struct {
	ActionsV1 []*httpserver.Action
	MySQL     fastbuild.MySQL
	Conf      *config.GatewayConfig
}

var fbServerAPI = FBServerAPIResource{}

// GetDistCCServerAPIResource return the singleton fastbuild api resource
func GetFBServerAPIResource() *FBServerAPIResource {
	return &fbServerAPI
}

// InitActions add actions
func (a *FBServerAPIResource) InitActions() {
	a.ActionsV1 = append(a.ActionsV1, GetAPIV1Action()...)
}

// XNAPISServerAPIResource describe all the apis api resources
type XNAPISServerAPIResource struct {
	ActionsV1 []*httpserver.Action
	MySQL     apisjob.MySQL
	Conf      *config.GatewayConfig
}

var xnAPISServerAPI = XNAPISServerAPIResource{}

// GetDistCCServerAPIResource return the singleton apis api resource
func GetXNAPISServerAPIResource() *XNAPISServerAPIResource {
	return &xnAPISServerAPI
}

// InitActions add actions
func (a *XNAPISServerAPIResource) InitActions() {
	a.ActionsV1 = append(a.ActionsV1, GetAPIV1Action()...)
}

// DistTaskServerAPIResource describe all the disttask api resources
type DistTaskServerAPIResource struct {
	ActionsV1 []*httpserver.Action
	MySQL     disttask.MySQL
	Conf      *config.GatewayConfig
}

var distTaskServerAPI = DistTaskServerAPIResource{}

// GetDistCCServerAPIResource return the singleton disttask api resource
func GetDistTaskServerAPIResource() *DistTaskServerAPIResource {
	return &distTaskServerAPI
}

// InitActions add actions
func (a *DistTaskServerAPIResource) InitActions() {
	a.ActionsV1 = append(a.ActionsV1, GetAPIV1Action()...)
}
