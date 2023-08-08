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

	if Config.WorkerUser == "" {
		Config.WorkerUser = startUser.Username
	}

	return nil
}

type EnvConfig struct {
	IsDebug    bool   `env:"DEVOPS_AGENTSLIM_ISDEBUG"`
	LogPath    string `env:"DEVOPS_AGENTSLIM_LOGPATH"`
	WorkerUser string `env:"DEVOPS_AGENTSLIM_WORKER_USER"`
	Lanuage    string `env:"DEVOPS_AGENTSLIM_LANUAGE"`
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
