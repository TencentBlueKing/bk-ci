package main

import (
	"disaptch-k8s-manager/pkg/apiserver"
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/cron"
	"disaptch-k8s-manager/pkg/db/mysql"
	"disaptch-k8s-manager/pkg/db/redis"
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/task"
	_ "disaptch-k8s-manager/swagger/apiserver"
	"fmt"
	"math/rand"
	"os"
	"path/filepath"
	"time"
)

// 在编译时传入，是否是debug环境，是会开启接口文档等特性
var debug string

// 在编译时传入，保存日志和配置文件的基础路径
var configDir string
var outDir string

func main() {
	rand.Seed(time.Now().UnixNano())

	if configDir == "" {
		configDir = filepath.Join(".", "resources")
	}
	initConfig(configDir)

	if outDir == "" {
		outDir = filepath.Join(".", "out")
	}
	logs.Init(filepath.Join(outDir, "logs", config.ManagerLog))

	if err := mysql.InitMysql(); err != nil {
		fmt.Printf("init mysql error %v\n", err)
		os.Exit(1)
	}
	defer mysql.Mysql.Close()

	redis.InitRedis()
	defer redis.Rdb.Close()

	informerStopper := make(chan struct{})
	defer close(informerStopper)
	if err := kubeclient.InitKubeClient(filepath.Join(configDir, "kubeConfig.yaml"), informerStopper); err != nil {
		fmt.Printf("init kubenetes client error %v\n", err)
		os.Exit(1)
	}

	task.InitTask()

	if err := cron.InitCronJob(); err != nil {
		fmt.Printf("init corn job error %v\n", err)
		os.Exit(1)
	}

	if err := apiserver.InitApiServer(filepath.Join(outDir, "logs", config.AccessLog)); err != nil {
		fmt.Printf("init api server error %v\n", err)
		os.Exit(1)
	}

	<-informerStopper
}

func initConfig(configDir string) {
	if debug == "true" {
		config.Envs.IsDebug = true
	} else {
		config.Envs.IsDebug = false
	}

	if err := config.InitConfig(configDir, "config"); err != nil {
		fmt.Printf("init config error %v\n", err)
		os.Exit(1)
	}
}
