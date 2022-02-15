/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package v1

import (
	"github.com/Tencent/bk-ci/src/booster/server/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/api/v1/dcc"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/api/v1/fb"
	v2 "github.com/Tencent/bk-ci/src/booster/server/pkg/api/v2"
)

// InitStorage associated the url and handle
func InitStorage() (err error) {
	// distcc v1 API
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/distcc/apply", Params: nil, Handler: api.MasterRequired(dcc.ApplyResource),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/distcc/release", Params: nil, Handler: api.MasterRequired(dcc.ReleaseResource),
	})
	api.RegisterV1Action(api.Action{
		Verb: "GET", Path: "/distcc/task", Params: nil, Handler: api.MasterRequired(dcc.QueryTaskInfo),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/distcc/heartbeat", Params: nil, Handler: api.MasterRequired(v2.UpdateHeartbeat),
	})
	api.RegisterV1Action(api.Action{
		Verb: "GET", Path: "/distcc/cmake", Params: nil, Handler: api.MasterRequired(dcc.GetCmakeArgs),
	})

	// fastbuild v1 API
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/fb/apply", Params: nil, Handler: api.MasterRequired(fb.ApplyResource),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/fb/release", Params: nil, Handler: api.MasterRequired(fb.ReleaseResource),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/fb/subtaskdone", Params: nil, Handler: api.MasterRequired(fb.SubTaskDone),
	})
	api.RegisterV1Action(api.Action{
		Verb: "GET", Path: "/fb/task", Params: nil, Handler: api.MasterRequired(fb.QueryTaskInfo),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/fb/heartbeat", Params: nil, Handler: api.MasterRequired(v2.UpdateHeartbeat),
	})

	return nil
}

func init() {
	api.RegisterInitFunc(InitStorage)
}
