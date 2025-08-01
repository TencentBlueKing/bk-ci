package main

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/config"
)

func main() {
	if len(os.Args) == 2 {
		switch os.Args[1] {
		case "version":
			fmt.Println(config.AgentVersion)
			os.Exit(0)
		case "fullVersion":
			fmt.Println(config.AgentVersion)
			fmt.Println(config.GitCommit)
			fmt.Println(config.BuildTime)
			os.Exit(0)
		}
	}

	// 初始化配置
	if err := config.InitConfig(); err != nil {
		fmt.Printf("Init config error %s\n", err.Error())
		os.Exit(1)
	}

	// 初始化日志
	var logStd bool
	if config.Config.IsDebug {
		logStd = true
	}
	if err := logs.Init(filepath.Join(config.Config.LogDir, "devopsAgent.log"), config.Config.IsDebug, logStd); err != nil {
		fmt.Printf("init agent log error %s\n", err.Error())
		os.Exit(1)
	}

	// 以agent安装目录为工作目录
	if err := os.Chdir(config.Config.WorkDir); err != nil {
		logs.WithError(err).Error("change work dir failed")
		os.Exit(1)
	}

	logs.Debug("agent start")
	logs.Debug("pid: ", os.Getpid())
	logs.Debug("agent version: ", config.AgentVersion)
	logs.Debug("git commit: ", config.GitCommit)
	logs.Debug("build time: ", config.BuildTime)
	logs.Debug("current user userName: ", config.Config.StartUser)
	logs.Debug("work dir: ", config.Config.WorkDir)

	pkg.Run()
}
