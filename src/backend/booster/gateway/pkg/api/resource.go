package api

import (
	"build-booster/common/http/httpserver"
	"build-booster/gateway/config"
	"build-booster/gateway/pkg/register-discover"
	"build-booster/server/pkg/engine/apisjob"
	"build-booster/server/pkg/engine/distcc"
	"build-booster/server/pkg/engine/disttask"
	"build-booster/server/pkg/engine/fastbuild"
)

var Rd register_discover.RegisterDiscover

// DistCCServerAPIResource describe all the server api resources
type DistCCServerAPIResource struct {
	ActionsV1 []*httpserver.Action
	MySQL     distcc.MySQL
	Conf      *config.GatewayConfig
}

var distCCServerAPI = DistCCServerAPIResource{}

func GetDistCCServerAPIResource() *DistCCServerAPIResource {
	return &distCCServerAPI
}

func (a *DistCCServerAPIResource) InitActions() {
	a.ActionsV1 = append(a.ActionsV1, GetAPIV1Action()...)
}

type FBServerAPIResource struct {
	ActionsV1 []*httpserver.Action
	MySQL     fastbuild.MySQL
	Conf      *config.GatewayConfig
}

var fbServerAPI = FBServerAPIResource{}

func GetFBServerAPIResource() *FBServerAPIResource {
	return &fbServerAPI
}

func (a *FBServerAPIResource) InitActions() {
	a.ActionsV1 = append(a.ActionsV1, GetAPIV1Action()...)
}

type XNAPISServerAPIResource struct {
	ActionsV1 []*httpserver.Action
	MySQL     apisjob.MySQL
	Conf      *config.GatewayConfig
}

var xnAPISServerAPI = XNAPISServerAPIResource{}

func GetXNAPISServerAPIResource() *XNAPISServerAPIResource {
	return &xnAPISServerAPI
}

func (a *XNAPISServerAPIResource) InitActions() {
	a.ActionsV1 = append(a.ActionsV1, GetAPIV1Action()...)
}

type DistTaskServerAPIResource struct {
	ActionsV1 []*httpserver.Action
	MySQL     disttask.MySQL
	Conf      *config.GatewayConfig
}

var distTaskServerAPI = DistTaskServerAPIResource{}

func GetDistTaskServerAPIResource() *DistTaskServerAPIResource {
	return &distTaskServerAPI
}

func (a *DistTaskServerAPIResource) InitActions() {
	a.ActionsV1 = append(a.ActionsV1, GetAPIV1Action()...)
}
