/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at http://opensource.org/licenses/MIT
 *
 */

package api

import (
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/config"
	rd "github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/register-discover"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/store"
)

// DistCCControllerAPIResource describe the distcc controller api resource
type DistCCControllerAPIResource struct {
	ActionsV1 []*httpserver.Action
	Rd        rd.RegisterDiscover
	Ops       store.Ops
	Conf      *config.DistCCControllerConfig
}

var api = DistCCControllerAPIResource{}

// GetAPIResource return the singleton api resource
func GetAPIResource() *DistCCControllerAPIResource {
	return &api
}

// InitActions init the http actions
func (a *DistCCControllerAPIResource) InitActions() {
	a.ActionsV1 = append(a.ActionsV1, GetAPIV1Action()...)
}
