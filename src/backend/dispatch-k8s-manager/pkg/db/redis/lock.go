package redis

import (
	"disaptch-k8s-manager/pkg/logs"
	"time"
)

func Lock(key string, expiration time.Duration) (bool, error) {
	ok, err := Rdb.SetNX(key, 1, expiration).Result()
	if err != nil {
		return false, err
	}
	return ok, nil
}

func UnLock(key string) {
	nums, err := Rdb.Del(key).Result()
	if err == nil && nums > 0 {
		return
	}

	logs.Error("redis unlock ", key, " error", err)
}
