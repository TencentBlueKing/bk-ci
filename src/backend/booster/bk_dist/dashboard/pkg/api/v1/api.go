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
	"github.com/Tencent/bk-ci/src/booster/bk_dist/dashboard/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"
)

const (
	pathParamKeyTaskID = "task_id"

	pathQueryKeyTaskID    = "task_id"
	pathQueryKeyWorkID    = "work_id"
	pathQueryKeyProjectID = "project_id"
	pathQueryKeyUser      = "user"
	pathQueryKeySourceIP  = "source_ip"
	pathQueryKeyOffset    = "offset"
	pathQueryKeyLimit     = "limit"
	pathQueryKeyDay       = "day"
	pathQueryKeyDecodeJob = "decode_job"
)

var (
	defaultMySQL disttask.MySQL
)

// InitStorage After server init, the instances of manager, store ... etc. should be given into api handler.
func InitStorage() (err error) {
	defaultMySQL = api.GetAPIResource().DistTaskMySQL

	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/disttask/task/{task_id}",
		Params:  nil,
		Handler: api.FuncWrapper(GetTask),
	})

	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/disttask/task",
		Params:  nil,
		Handler: api.FuncWrapper(ListTask),
	})

	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/disttask/work_stats",
		Params:  nil,
		Handler: api.FuncWrapper(GetWorkStats),
	})

	api.RegisterV1Action(api.Action{
		Verb:    "GET",
		Path:    "/disttask/work",
		Params:  nil,
		Handler: api.FuncWrapper(ListWorks),
	})

	return nil
}

func init() {
	api.RegisterInitFunc(InitStorage)
}
