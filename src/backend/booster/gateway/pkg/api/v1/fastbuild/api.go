/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package fastbuild

import (
	"github.com/Tencent/bk-ci/src/booster/gateway/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/fastbuild"
)

const (
	queryUserKey          = "user"
	queryCompilerCountKey = "compiler_count"
	queryRequestCPUKey    = "request_cpu"
	queryLeastCPUKey      = "least_cpu"
	queryBanAllBooster    = "ban_all_booster"
)

var (
	defaultMySQL fastbuild.MySQL

	// implements the keys that can be filtered with "In" during task list
	listTaskInKey = api.WithBasicGroup(api.GroupListTaskInKey, map[string]bool{
		queryCompilerCountKey: true,
		queryUserKey:          true,
		queryBanAllBooster:    true,
	})

	// implements the keys that can be filtered with "Gt" during task list
	listTaskGtKey = api.WithBasicGroup(api.GroupListTaskGtKey, map[string]bool{})

	// implements the keys that can be filtered with "Lt" during task list
	listTaskLtKey = api.WithBasicGroup(api.GroupListTaskLtKey, map[string]bool{})

	// implements the keys that can be filtered with "In" during project list
	listProjectInKey = api.WithBasicGroup(api.GroupListProjectInKey, map[string]bool{
		queryRequestCPUKey: true,
		queryLeastCPUKey:   true,
		queryBanAllBooster: true,
	})

	// implements the keys that can be filtered with "In" during whitelist list
	listWhitelistInKey = api.WithBasicGroup(api.GroupListWhitelistInKey, map[string]bool{})

	// implements the int keys
	intKey = api.WithBasicGroup(api.GroupIntKey, map[string]bool{
		queryCompilerCountKey: true,
	})

	// implements the int64 keys
	int64Key = api.WithBasicGroup(api.GroupInt64Key, map[string]bool{})

	// implements the bool keys
	boolKey = api.WithBasicGroup(api.GroupBoolKey, map[string]bool{
		queryBanAllBooster: true,
	})

	// implements the float64 keys
	float64Key = api.WithBasicGroup(api.GroupBoolKey, map[string]bool{})
)

// InitStorage After server init, the instances of manager, store ... etc. should be given into api handler.
func InitStorage() (err error) {
	defaultMySQL = api.GetFBServerAPIResource().MySQL
	return nil
}

func init() {
	api.RegisterInitFunc(InitStorage)
}
