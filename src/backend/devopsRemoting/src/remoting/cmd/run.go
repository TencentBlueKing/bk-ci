package main

import (
	"common/logs"
	"os"
	remoting "remoting/pkg"
	"remoting/pkg/constant"

	"github.com/spf13/cobra"
)

const runDesc = `
run 用来执行DevopsRemoting服务进程

如：devopsRemoting run
`

func newCommandRun() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "run",
		Short: "启动 DevopsRemoting",
		Long:  runDesc,
		Run: func(_ *cobra.Command, _ []string) {
			logs.Init(Service, Version, true, os.Getenv(constant.DebugModEnvName) == "true")
			remoting.Run()
		},
	}

	return cmd
}
