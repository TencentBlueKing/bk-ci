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
	"github.com/Tencent/bk-ci/src/booster/server/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/manager"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/rd"
)

// ServerAPIResource describe all the server api resources
type ServerAPIResource struct {
	ActionsV1 []*httpserver.Action
	ActionsV2 []*httpserver.Action
	Rd        rd.RegisterDiscover
	Manager   manager.Manager
	Conf      *config.ServerConfig
}

var api = ServerAPIResource{}

// GetAPIResource get the standalone api resource instance
func GetAPIResource() *ServerAPIResource {
	return &api
}

func (a *ServerAPIResource) initActions() {
	a.ActionsV1 = append(a.ActionsV1, GetAPIV1Action()...)
	a.ActionsV2 = append(a.ActionsV2, GetAPIV2Action()...)
}

// RegisterWebServer register all actions in api-resource into the given http server.
func (a *ServerAPIResource) RegisterWebServer(svr *httpserver.HTTPServer) error {
	a.initActions()

	if err := svr.RegisterWebServer(PathV1, nil, a.ActionsV1); err != nil {
		return err
	}

	if err := svr.RegisterWebServer(PathV2, nil, a.ActionsV2); err != nil {
		return err
	}

	return nil
}
