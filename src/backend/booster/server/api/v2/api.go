package v2

import (
	"build-booster/server/pkg/api"
	"build-booster/server/pkg/manager"
)

var (
	defaultManager manager.Manager
)

// InitStorage After server init, the instances of manager, store ... etc. should be given into api handler.
func InitStorage() (err error) {
	defaultManager = api.GetAPIResource().Manager
	api.RegisterV2Action(api.Action{
		Verb: "POST", Path: "/build/apply", Params: nil, Handler: api.MasterRequired(ApplyResource),
	})
	api.RegisterV2Action(api.Action{
		Verb: "POST", Path: "/build/release", Params: nil, Handler: api.MasterRequired(ReleaseResource),
	})
	api.RegisterV2Action(api.Action{
		Verb: "POST", Path: "/build/message", Params: nil, Handler: api.MasterRequired(SendMessage),
	})
	api.RegisterV2Action(api.Action{
		Verb: "GET", Path: "/build/task", Params: nil, Handler: api.MasterRequired(QueryTaskInfo),
	})
	api.RegisterV2Action(api.Action{
		Verb: "POST", Path: "/build/heartbeat", Params: nil, Handler: api.MasterRequired(UpdateHeartbeat),
	})

	return nil
}

func init() {
	api.RegisterInitFunc(InitStorage)
}
