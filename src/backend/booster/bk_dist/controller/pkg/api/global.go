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
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
)

// ServerAPIResource describe all the server api resources
type ServerAPIResource struct {
	ActionsV1 []*httpserver.Action
	ActionsV2 []*httpserver.Action
	Manager   types.Mgr
	Conf      *config.ServerConfig
}

var api = ServerAPIResource{}

// GetAPIResource get the standalone api resource instance
func GetAPIResource() *ServerAPIResource {
	return &api
}

func (a *ServerAPIResource) initActions() {
	a.ActionsV1 = append(a.ActionsV1, GetAPIV1Action()...)
}

// RegisterWebServer register all actions in api-resource into the given http server.
func (a *ServerAPIResource) RegisterWebServer(svr *httpserver.HTTPServer) error {
	a.initActions()

	if err := svr.RegisterWebServer(PathV1, nil, a.ActionsV1); err != nil {
		return err
	}

	return nil
}
