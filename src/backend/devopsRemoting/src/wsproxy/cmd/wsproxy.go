package main

import (
	"common/cli/check"
	"common/logs"
	"fmt"
	"os"

	"github.com/spf13/cobra"
)

var (
	Service = "wsproxy"
	Version = ""
)

func main() {
	logs.Init(Service, Version, true, false)

	if err := newWsproxyCommand(Version).Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}

func newWsproxyCommand(version string) *cobra.Command {
	cmds := &cobra.Command{
		Use:   "wsproxy",
		Short: "wsproxy 这充当所有工作区绑定请求的反向代理",
		Args:  check.MinimumNArgs(1),
	}

	cmds.AddCommand(
		newCommandVersion(version),
		newCommandRun(),
	)

	return cmds
}

func newCommandVersion(version string) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "version",
		Short: "wsproxy 版本",
		Run: func(_ *cobra.Command, _ []string) {
			fmt.Println(version)
		},
	}

	return cmd
}
