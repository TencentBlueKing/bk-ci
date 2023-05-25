/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package command

import (
	"fmt"
	"os"

	"github.com/Tencent/bk-ci/src/booster/common/version"

	commandCli "github.com/urfave/cli"
)

// define const vars
const (
	FlagLog               = "log"
	FlagLogDir            = "log_dir"
	FlagActionJSONFile    = "actions_json_file"
	FlagToolChainJSONFile = "tool_chain_json_file"
	FlagMostDependFirst   = "most_depend_first"
)

// Run main entrance
func Run(ct ClientType) {
	if err := run(ct); err != nil {
		_, _ = fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}

func run(ct ClientType) error {
	return GetApp(ct).Run(os.Args)
}

// GetApp get app by type
func GetApp(ct ClientType) *commandCli.App {
	client := commandCli.NewApp()
	client.Name = ct.Name()
	client.Usage = ct.Usage()
	client.Version = fmt.Sprintf("Version:   %s\n\t Tag:       %s\n\t BuildTime: %s\n\t GitHash:   %s",
		version.Version, version.Tag, version.BuildTime, version.GitHash)

	client.Flags = []commandCli.Flag{
		commandCli.StringFlag{
			Name:  "log",
			Usage: "log level to print some information",
		},
		commandCli.StringFlag{
			Name:  "log_dir",
			Usage: "log dir to save log files",
		},
		commandCli.StringFlag{
			Name:  "actions_json_file",
			Usage: "json file to describe ue4 actions to execute",
		},
		commandCli.StringFlag{
			Name:  "tool_chain_json_file",
			Usage: "json file to describe tool chain",
		},
		commandCli.BoolFlag{
			Name:  "most_depend_first",
			Usage: "firstly execute actions which depend mostly",
		},
	}

	switch ct {
	case ClientBKUBTTool:
		client.Flags = append(client.Flags, []commandCli.Flag{}...)

		client.Action = mainProcess

	default:
		client.Action = func(c *commandCli.Context) error {
			return fmt.Errorf("unknown client")
		}
	}

	// override the version printer
	commandCli.VersionPrinter = func(c *commandCli.Context) {
		fmt.Printf("Version:   %s\nTag:       %s\nBuildTime: %s\nGitHash:   %s\n",
			version.Version, version.Tag, version.BuildTime, version.GitHash)
	}

	return client
}
