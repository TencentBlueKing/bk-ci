package api

import (
	"build-booster/common/http/httpserver"
	"build-booster/server/config"
	"build-booster/server/pkg/manager"
	"build-booster/server/pkg/rd"
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
