package cmd

import (
	"fmt"
	"os"

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
}

func Execute() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
