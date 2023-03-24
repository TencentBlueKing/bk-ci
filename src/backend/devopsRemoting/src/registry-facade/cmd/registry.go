package cmd

import (
	"common/logs"
	"fmt"
	"os"

	"registry-facade/pkg/constant"

	"github.com/spf13/cobra"
)

var (
	ServiceName = "registry-facade"
	Version     = ""
)

var rootCmd = &cobra.Command{
	Use:   "registry-facade",
	Short: "当前服务作为workspace镜像的registry,用来优化workspace镜像分发",
	Args:  cobra.MinimumNArgs(1),
	PersistentPreRun: func(_ *cobra.Command, _ []string) {
		logs.Init(ServiceName, Version, true, os.Getenv(constant.DebugModEnvName) == "true")
	},
}

func Execute() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
