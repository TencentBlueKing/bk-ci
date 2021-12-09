package api

import (
	"build-booster/common/http/httpserver"
	"build-booster/server/pkg/engine/distcc/controller/config"
	"build-booster/server/pkg/engine/distcc/controller/pkg/register-discover"
	"build-booster/server/pkg/engine/distcc/controller/pkg/store"
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
