package disttask

import (
	"build-booster/gateway/pkg/api"
	"build-booster/server/pkg/engine/disttask"
)

const (
	queryWorkerVersionKey = "worker_version"
	querySceneKey         = "scene"
	queryWorkIDKey        = "work_id"
	querySuccessKey       = "success"
)

var (
	defaultMySQL disttask.MySQL

	// implements the keys that can be filtered with "In" during task list
	listTaskInKey = api.WithBasicGroup(api.GroupListTaskInKey, map[string]bool{})

	// implements the keys that can be filtered with "Gt" during task list
	listTaskGtKey = api.WithBasicGroup(api.GroupListTaskGtKey, map[string]bool{})

	// implements the keys that can be filtered with "Lt" during task list
	listTaskLtKey = api.WithBasicGroup(api.GroupListTaskLtKey, map[string]bool{})

	// implements the keys that can be filtered with "In" during project list
	listProjectInKey = api.WithBasicGroup(api.GroupListProjectInKey, map[string]bool{
		querySceneKey: true,
	})

	// implements the keys that can be filtered with "In" during whitelist list
	listWhitelistInKey = api.WithBasicGroup(api.GroupListWhitelistInKey, map[string]bool{})

	// implements the keys that can be filtered with "In" during work stats list
	listWorkStatsInKey = map[string]bool{
		api.QueryTaskIDKey:    true,
		api.QueryProjectIDKey: true,
		querySceneKey:         true,
		queryWorkIDKey:        true,
		querySuccessKey:       true,
	}

	// implements the int keys
	intKey = api.WithBasicGroup(api.GroupIntKey, map[string]bool{
	})

	// implements the int64 keys
	int64Key = api.WithBasicGroup(api.GroupInt64Key, map[string]bool{})

	// implements the bool keys
	boolKey = api.WithBasicGroup(api.GroupBoolKey, map[string]bool{
		querySuccessKey: true,
	})

	// implements the float64 keys
	float64Key = api.WithBasicGroup(api.GroupBoolKey, map[string]bool{
	})
)

// InitStorage After server init, the instances of manager, store ... etc. should be given into api handler.
func InitStorage() (err error) {
	defaultMySQL = api.GetDistTaskServerAPIResource().MySQL
	return nil
}

func init() {
	api.RegisterInitFunc(InitStorage)
}
