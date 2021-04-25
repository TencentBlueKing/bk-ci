package v1

import (
	"build-booster/gateway/pkg/api"
	"build-booster/gateway/pkg/api/v1/apisjob"
	"build-booster/gateway/pkg/api/v1/distcc"
	"build-booster/gateway/pkg/api/v1/disttask"
	"build-booster/gateway/pkg/api/v1/fastbuild"
)

// After server init, the instances of manager, store ... etc. should be given into api handler.
func InitStorage() (err error) {
	initDCCActions()
	initFBActions()
	initAPISActions()
	initDistTaskActions()
	return nil
}

func initDCCActions() {
	// distcc task
	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/distcc/resource/task",
		Params:  nil,
		Handler: api.NoLimit(distcc.ListTask),
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

func init() {
	api.RegisterInitFunc(InitStorage)
}
