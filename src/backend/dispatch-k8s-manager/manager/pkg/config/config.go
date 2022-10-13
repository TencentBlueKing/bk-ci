package config

import (
	"github.com/spf13/viper"
)

var Config = &ConfigYaml{}

func InitConfig(configDir string, configFile string) error {
	vip := viper.New()

	vip.AddConfigPath(configDir)
	vip.SetConfigName(configFile)

	if err := vip.ReadInConfig(); err != nil {
		return err
	}

	if err := vip.Unmarshal(Config); err != nil {
		return err
	}

	return nil
}

// UseRealResourceUsage 配置开关是否使用RealResourceUsage
// RealResourceUsage 通过prometheus计算容器真实的资源使用，来优化调度
func UseRealResourceUsage() bool {
	return Config.Dispatch.Builder.RealResource.PrometheusUrl != ""
}

var Envs = &envConfig{}

// EnvConfig 一些通过环境变量或者启动参数设置的配置
type envConfig struct {
	IsDebug bool
}
