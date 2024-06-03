package redis

import (
	"disaptch-k8s-manager/pkg/config"
	"github.com/go-redis/redis"
)

var Rdb *redis.Client

func InitRedis() {
	Rdb = redis.NewClient(&redis.Options{
		Addr:     config.Config.Redis.Addr,
		Password: config.Config.Redis.Password,
		DB:       config.Config.Redis.Db,
	})
}
