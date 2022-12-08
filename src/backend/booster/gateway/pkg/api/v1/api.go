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
	"sync"

	"github.com/Tencent/bk-ci/src/booster/gateway/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/gateway/pkg/api/v1/apisjob"
	"github.com/Tencent/bk-ci/src/booster/gateway/pkg/api/v1/distcc"
	"github.com/Tencent/bk-ci/src/booster/gateway/pkg/api/v1/disttask"
	"github.com/Tencent/bk-ci/src/booster/gateway/pkg/api/v1/fastbuild"

	"github.com/emicklei/go-restful"
)

var one sync.Once

// After server init, the instances of manager, store ... etc. should be given into api handler.
func InitStorage() (err error) {
	one.Do(func() {
		api.RegisterV1Action(api.Action{
			Verb:    "GET",
			Path:    "/health",
			Params:  nil,
			Handler: api.NoLimit(health),
		})

		initDCCActions()
		initFBActions()
		initAPISActions()
		initDistTaskActions()
		initAutoDistTaskActions()
	})
	return nil
}

// health return ok to caller
func health(_ *restful.Request, resp *restful.Response) {
	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func initDCCActions() {
	if api.GetDistCCServerAPIResource().MySQL == nil {
		return
	}

	// distcc task
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/distcc/resource/task",
		Params:  nil,
		Handler: api.NoLimit(distcc.ListTask),
	})
	// distcc worker images
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/distcc/resource/images",
		Params:  nil,
		Handler: api.NoLimit(distcc.ListWorkerImages),
	})
	// distcc project
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/distcc/resource/project",
		Params:  nil,
		Handler: api.NoLimit(distcc.ListProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    "/distcc/resource/project/{project_id}",
		Params:  nil,
		Handler: api.NoLimit(distcc.UpdateProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    "/distcc/resource/project/{project_id}",
		Params:  nil,
		Handler: api.NoLimit(distcc.DeleteProject),
	})

	// distcc whitelist
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/distcc/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(distcc.ListWhitelist),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    "/distcc/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(distcc.UpdateWhitelist),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    "/distcc/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(distcc.DeleteWhitelist),
	})

	// distcc gcc
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/distcc/resource/gcc",
		Params:  nil,
		Handler: api.NoLimit(distcc.ListGcc),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    "/distcc/resource/gcc/{gcc_version}",
		Params:  nil,
		Handler: api.NoLimit(distcc.UpdateGcc),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    "/distcc/resource/gcc/{gcc_version}",
		Params:  nil,
		Handler: api.NoLimit(distcc.DeleteGcc),
	})
}

func initFBActions() {
	if api.GetFBServerAPIResource().MySQL == nil {
		return
	}

	// fb task
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/fb/resource/task",
		Params:  nil,
		Handler: api.NoLimit(fastbuild.ListTask),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/fb/resource/subtask",
		Params:  nil,
		Handler: api.NoLimit(fastbuild.ListSubTask),
	})

	// fb project
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/fb/resource/project",
		Params:  nil,
		Handler: api.NoLimit(fastbuild.ListProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    "/fb/resource/project/{project_id}",
		Params:  nil,
		Handler: api.NoLimit(fastbuild.UpdateProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    "/fb/resource/project/{project_id}",
		Params:  nil,
		Handler: api.NoLimit(fastbuild.DeleteProject),
	})

	// fb whitelist
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/fb/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(fastbuild.ListWhitelist),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    "/fb/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(fastbuild.UpdateWhitelist),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    "/fb/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(fastbuild.DeleteWhitelist),
	})
}

func initAPISActions() {
	if api.GetXNAPISServerAPIResource().MySQL == nil {
		return
	}

	// apis task
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/apisjob/resource/task",
		Params:  nil,
		Handler: api.NoLimit(apisjob.ListTask),
	})

	// apis project
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/apisjob/resource/project",
		Params:  nil,
		Handler: api.NoLimit(apisjob.ListProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    "/apisjob/resource/project/{project_id}",
		Params:  nil,
		Handler: api.NoLimit(apisjob.UpdateProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    "/apisjob/resource/project/{project_id}",
		Params:  nil,
		Handler: api.NoLimit(apisjob.DeleteProject),
	})

	// apis whitelist
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/apisjob/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(apisjob.ListWhitelist),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    "/apisjob/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(apisjob.UpdateWhitelist),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    "/apisjob/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(apisjob.DeleteWhitelist),
	})
}

func initDistTaskActions() {
	if api.GetDistTaskServerAPIResource().MySQL == nil {
		return
	}
	// disttask client version
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/disttask/resource/version",
		Params:  nil,
		Handler: api.NoLimit(disttask.ListClientVersion),
	})

	// disttask worker images
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/disttask/resource/images",
		Params:  nil,
		Handler: api.NoLimit(disttask.ListWorkerImages),
	})

	// disttask task
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/disttask/resource/task",
		Params:  nil,
		Handler: api.NoLimit(disttask.ListTask),
	})

	// disttask work stats
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/disttask/resource/stats",
		Params:  nil,
		Handler: api.NoLimit(disttask.ListWorkStats),
	})

	// disttask task
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/disttask/resource/project",
		Params:  nil,
		Handler: api.NoLimit(disttask.ListProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    "/disttask/resource/project/{project_id}",
		Params:  nil,
		Handler: api.NoLimit(disttask.UpdateProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    "/disttask/resource/project/{project_id}/scene/{scene}",
		Params:  nil,
		Handler: api.NoLimit(disttask.UpdateProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    "/disttask/resource/project/{project_id}",
		Params:  nil,
		Handler: api.NoLimit(disttask.DeleteProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    "/disttask/resource/project/{project_id}/scene/{scene}",
		Params:  nil,
		Handler: api.NoLimit(disttask.DeleteProject),
	})

	// disttask whitelist
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/disttask/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(disttask.ListWhitelist),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    "/disttask/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(disttask.UpdateWhitelist),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    "/disttask/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(disttask.DeleteWhitelist),
	})

	// disttask worker
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/disttask/resource/worker",
		Params:  nil,
		Handler: api.NoLimit(disttask.ListWorker),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    "/disttask/resource/worker/{worker_version}/scene/{scene}",
		Params:  nil,
		Handler: api.NoLimit(disttask.UpdateWorker),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    "/disttask/resource/worker/{worker_version}/scene/{scene}",
		Params:  nil,
		Handler: api.NoLimit(disttask.DeleteWorker),
	})
}

func initAutoDistTaskActions() {
	if api.GetDistTaskServerAPIResource().MySQL == nil {
		return
	}

	autoKey := "/{auto_scene:disttask-[0-9A-Za-z_]+}"

	// auto disttask task
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    autoKey + "/resource/task",
		Params:  nil,
		Handler: api.NoLimit(disttask.AutoListTask),
	})

	// auto disttask work stats
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    autoKey + "/resource/stats",
		Params:  nil,
		Handler: api.NoLimit(disttask.AutoListWorkStats),
	})

	// auto disttask task
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    autoKey + "/resource/project",
		Params:  nil,
		Handler: api.NoLimit(disttask.AutoListProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    autoKey + "/resource/project/{project_id}",
		Params:  nil,
		Handler: api.NoLimit(disttask.AutoUpdateProject),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    autoKey + "/resource/project/{project_id}",
		Params:  nil,
		Handler: api.NoLimit(disttask.AutoDeleteProject),
	})

	// auto disttask whitelist
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    autoKey + "/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(disttask.AutoListWhitelist),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    autoKey + "/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(disttask.AutoUpdateWhitelist),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    autoKey + "/resource/whitelist",
		Params:  nil,
		Handler: api.NoLimit(disttask.AutoDeleteWhitelist),
	})

	// auto disttask worker
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    autoKey + "/resource/worker",
		Params:  nil,
		Handler: api.NoLimit(disttask.AutoListWorker),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "PUT",
		Path:    autoKey + "/resource/worker/{worker_version}",
		Params:  nil,
		Handler: api.NoLimit(disttask.AutoUpdateWorker),
	})
	api.RegisterV1Action(api.Action{
		Verb:    "DELETE",
		Path:    autoKey + "/resource/worker/{worker_version}",
		Params:  nil,
		Handler: api.NoLimit(disttask.AutoDeleteWorker),
	})
}

func init() {
	api.RegisterInitFunc(InitStorage)
}
