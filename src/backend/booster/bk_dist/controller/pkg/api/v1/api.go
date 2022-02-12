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
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
)

var (
	pathParamWorkID             = "work_id"
	queryParamJobIndex          = "job_index"
	queryParamJobLeastLeaveTime = "job_least_leave_time"

	defaultManager types.Mgr
)

// InitStorage After server init, the instances of manager, store ... etc. should be given into api handler.
func InitStorage() (err error) {
	defaultManager = api.GetAPIResource().Manager
	api.RegisterV1Action(api.Action{
		Verb: "GET", Path: "/dist/available", Params: nil, Handler: api.FuncWrapper(available),
	})
	api.RegisterV1Action(api.Action{
		Verb: "GET", Path: "/dist/work/list", Params: nil, Handler: api.FuncWrapper(getWorkList),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/commonconfig", Params: nil, Handler: api.FuncWrapper(setCommonConfig),
	})

	api.RegisterV1Action(api.Action{
		Verb: "GET", Path: "/dist/work/{work_id}/detail", Params: nil, Handler: api.FuncWrapper(getWorkDetail),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/register", Params: nil, Handler: api.FuncWrapper(registerWork),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/message", Params: nil, Handler: api.FuncWrapper(recordMessage),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/heartbeat", Params: nil, Handler: api.FuncWrapper(heartbeatWork),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/unregister", Params: nil, Handler: api.FuncWrapper(unregisterWork),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/slots/local/occupy", Params: nil,
		Handler: api.FuncWrapper(occupyLocalSlots),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/slots/local/free", Params: nil,
		Handler: api.FuncWrapper(freeLocalSlots),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/start", Params: nil, Handler: api.FuncWrapper(startWork),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/end", Params: nil, Handler: api.FuncWrapper(endWork),
	})
	api.RegisterV1Action(api.Action{
		Verb: "GET", Path: "/dist/work/{work_id}/status", Params: nil, Handler: api.FuncWrapper(getWorkStatus),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/settings", Params: nil, Handler: api.FuncWrapper(setWorkSettings),
	})
	api.RegisterV1Action(api.Action{
		Verb: "GET", Path: "/dist/work/{work_id}/settings", Params: nil, Handler: api.FuncWrapper(getWorkSettings),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/job/stats", Params: nil, Handler: api.FuncWrapper(updateJobStats),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/stats", Params: nil, Handler: api.FuncWrapper(recordWorkStats),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/remote/execute", Params: nil,
		Handler: api.FuncWrapper(executeRemoteTask),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/remote/send", Params: nil, Handler: api.FuncWrapper(sendRemoteFile),
	})
	api.RegisterV1Action(api.Action{
		Verb: "POST", Path: "/dist/work/{work_id}/local/execute", Params: nil,
		Handler: api.FuncWrapper(executeLocalTask),
	})

	return nil
}

func init() {
	api.RegisterInitFunc(InitStorage)
}
