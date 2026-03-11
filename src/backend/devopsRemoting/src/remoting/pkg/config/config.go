package config

import (
	"encoding/json"
	"os"
	"path/filepath"

	"github.com/Netflix/go-env"
	"github.com/pkg/errors"
)

const remotingConfigFile = "devopsRemoting-config.json"

var RemotingCfg *Config

// 获取remoting所需的所有配置
func GetConfig() (*Config, error) {
	if RemotingCfg != nil {
		return RemotingCfg, nil
	}

	remotingConfig, err := loadRemotingConfigFromFile()
	if err != nil {
		return nil, err
	}

	var ide *IDEConfig
	if remotingConfig.IDEConfigLocation != "" {
		if _, err := os.Stat(remotingConfig.IDEConfigLocation); !os.IsNotExist((err)) {
			ide, err = loadIDEConfigFromFile(remotingConfig.IDEConfigLocation)
			if err != nil {
				return nil, err
			}
		}
	}

	desktopIde, err := loadIDEConfigFromFile(remotingConfig.DesktopIDEConfigLocation)
	if err != nil {
		return nil, err
	}

	workspace, err := loadWorkspaceConfigFromEnv()
	if err != nil {
		return nil, err
	}

	RemotingCfg = &Config{
		Config:     *remotingConfig,
		IDE:        ide,
		DesktopIDE: *desktopIde,
		WorkSpace:  *workspace,
	}

	return RemotingCfg, nil
}

// loadRemotingConfigFromFile 加载DevopsRemoting的配置文件 devopsRemoting-config.json，与DevopsRemoting可执行文件放在一起。
func loadRemotingConfigFromFile() (*RemotingConfig, error) {
	path, err := os.Executable()
	if err != nil {
		return nil, errors.Wrap(err, "cannot get executable path error")
	}

	path = filepath.Join(filepath.Dir(path), remotingConfigFile)
	content, err := os.ReadFile(path)
	if err != nil {
		return nil, errors.Wrapf(err, "cannot read remoting config file %s error", path)
	}

	var res RemotingConfig
	err = json.Unmarshal(content, &res)
	if err != nil {
		return nil, errors.Wrapf(err, "cannot unmarshal devopsRemoting config file %s error", path)
	}

	return &res, nil
}

// loadIDEConfigFromFile 加载IDE相关配置
func loadIDEConfigFromFile(file string) (*IDEConfig, error) {
	f, err := os.Open(file)
	if err != nil {
		return nil, errors.Wrapf(err, "cannot load IDE config %s error", file)
	}
	defer f.Close()

	var res IDEConfig
	err = json.NewDecoder(f).Decode(&res)
	if err != nil {
		return nil, errors.Wrapf(err, "cannot unmarshal IDE config %s error", file)
	}

	return &res, nil
}

// loadWorkspaceConfigFromEnv 从环境变量中加载工作空间相关配置
func loadWorkspaceConfigFromEnv() (*WorkspaceConfig, error) {
	var res WorkspaceConfig
	_, err := env.UnmarshalFromEnviron(&res)
	if err != nil {
		return nil, errors.Errorf("cannot load workspace config: %s", err.Error())
	}

	return &res, nil
}

const WorkspaceLogRateLimitMin = 50

func (c Config) IDELogRateLimit(ideConfig *IDEConfig) int {
	if c.WorkSpace.WorkspaceLogRateLimit == 0 {
		return WorkspaceLogRateLimitMin
	}
	return c.WorkSpace.WorkspaceLogRateLimit
}
