package main

import (
	"devopsRemoting/common/logs"
	"devopsRemoting/src/pkg/cli/check"
	"fmt"
	"os"

	"github.com/spf13/cobra"
)

var (
	Service = "remoting"
	Version = ""
)

func main() {
	logs.Init(Service, Version, true, false)

	if err := newDevopsRemotingCommand(Version).Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}

func newDevopsRemotingCommand(version string) *cobra.Command {
	cmds := &cobra.Command{
		Use:   "devopsRemoting",
		Short: "devopsRemoting 是用来管理并启动远程登录IDE进程的服务",
		Args:  check.MinimumNArgs(1),
	}

	cmds.AddCommand(
		newCommandVersion(version),
		newCommandInit(),
		newCommandRun(),
	)

	return cmds
}

func newCommandVersion(version string) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "version",
		Short: "devopsRemotring 版本",
		Run: func(_ *cobra.Command, _ []string) {
			fmt.Println(version)
		},
	}

	return cmd
}
