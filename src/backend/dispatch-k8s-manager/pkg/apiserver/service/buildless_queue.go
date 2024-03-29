package service

import (
	"disaptch-k8s-manager/pkg/db/redis"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/types"
	"encoding/json"
)

const buildLessReadyKey string = "buildless:ready_task"

func leftPushBuildLessReadyTask(buildLessTask types.BuildLessTask) {
	jsonData, err := json.Marshal(buildLessTask)
	if err != nil {
		logs.Error("Error marshalling JSON:", err)
		return
	}
	nums, err := redis.Rdb.LPush(buildLessReadyKey, jsonData).Result()
	if err == nil && nums > 0 {
		return
	}

	logs.Error("Failed to leftPush BuildLessTask")
}

func rightPushBuildLessReadyTask(buildLessTask types.BuildLessTask) {
	jsonData, err := json.Marshal(buildLessTask)
	if err != nil {
		logs.Error("Error marshalling JSON:", err)
		return
	}
	nums, err := redis.Rdb.RPush(buildLessReadyKey, jsonData).Result()
	if err == nil && nums > 0 {
		return
	}

	logs.Error("Failed to rightPush BuildLessTask")
}

func popBuildLessReadyTask() (buildLessTask *types.BuildLessTask, err error) {
	buildLessTaskStr, popErr := redis.Rdb.RPop(buildLessReadyKey).Result()
	if popErr != nil {
		logs.Error("Failed to pop BuildLessTask", popErr)
		return buildLessTask, popErr
	}

	if len(buildLessTaskStr) == 0 {
		logs.Warn("Pop buildLessTask is empty")
		return buildLessTask, nil
	}

	jsonErr := json.Unmarshal([]byte(buildLessTaskStr), &buildLessTask)
	if err != nil {
		logs.Error("Error unmarshalling JSON:", jsonErr)
		return buildLessTask, jsonErr
	}

	return buildLessTask, nil
}
