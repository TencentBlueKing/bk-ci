package config

import (
	"os"
	"os/user"
	"path/filepath"

	"github.com/Netflix/go-env"
	"github.com/pkg/errors"
)

var Config *ConfigType

type ConfigType struct {
	// WorkDir 程序运行目录，不用多次拿取
	WorkDir string
	// StartUser 启动用户
	StartUser string
	EnvConfig
}

func InitConfig() error {
	envConfig, err := loadConfigFromEnv()
	if err != nil {
		return err
	}

	workDir, err := os.Executable()
	if err != nil {
		return err
	}

	startUser, err := user.Current()
	if err != nil {
		return err
	}

	Config = &ConfigType{
		EnvConfig: envConfig,
		WorkDir:   filepath.Dir(workDir),
		StartUser: startUser.Username,
	}

	// 补全一些默认值
	if Config.WorkerUser == "" {
		Config.WorkerUser = startUser.Username
	}

	// 最大运行数不能为 0
	if Config.MaxWorkerCount == 0 {
		Config.MaxWorkerCount = 1
	}

	if Config.LogDir == "" {
		Config.LogDir = workDir
	}

	// 做一些初始化
	err = os.Mkdir(Config.LogDir, os.ModePerm)
	if err != nil {
		return err
	}

	return nil
}

type EnvConfig struct {
	// DEBUG 模式
	IsDebug bool `env:"DEVOPS_AGENTSLIM_ISDEBUG"`
	// 日志路径
	LogDir string `env:"DEVOPS_AGENTSLIM_LOGPATH"`
	// worker启动用户
	WorkerUser string `env:"DEVOPS_AGENTSLIM_WORKER_USER"`
	// 国际化语言
	Language string `env:"DEVOPS_AGENTSLIM_LANUAGE"`
	// 最大可执行 worker 数量
	MaxWorkerCount int `env:"DEVOPS_AGENTSLIM_MAX_WORKER_COUNT"`
	// 后台网关
	GateWay string `env:"DEVOPS_AGENTSLIM_GATEWAY"`
	// 后台仓库网关
	FileGateWay   string `env:"DEVOPS_AGENTSLIM_FILEGATEWAY"`
	ProjectId     string `env:"DEVOPS_AGENTSLIM_PROJECT_ID"`
	ContainerName string `env:"DEVOPS_AGENTSLIM_CONTAINER_NAME"`
	// workerjar 路径
	WorkerPath string `env:"DEVOPS_AGENTSLIM_WORKER_PATH"`
	// java 路径
	JavaPath string `env:"DEVOPS_AGENTSLIM_JAVA_PATH"`
	// 是否根据系统切换 shell，false使用 /bin/bash
	DetectShell bool `env:"DEVOPS_AGENTSLIM_WORKER_DETECTSHELL"`
}

// loadConfigFromEnv 从环境变量中加载相关配置
func loadConfigFromEnv() (EnvConfig, error) {
	var res EnvConfig
	_, err := env.UnmarshalFromEnviron(&res)
	if err != nil {
		return res, errors.Errorf("cannot load workspace config: %s", err.Error())
	}

	return res, nil
}
