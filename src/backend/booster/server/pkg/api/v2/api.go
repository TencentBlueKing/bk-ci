/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package v2

import (
	"github.com/Tencent/bk-ci/src/booster/server/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/manager"
)

var (
	defaultManager manager.Manager
)

// InitStorage After server init, the instances of manager, store ... etc. should be given into api handler.
func InitStorage() (err error) {
	defaultManager = api.GetAPIResource().Manager
	api.RegisterV2Action(api.Action{
		Verb: "GET", Path: "/health", Params: nil, Handler: api.NoLimit(Health),
	})
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
