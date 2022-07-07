/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at http://opensource.org/licenses/MIT
 *
 */

package command

import (
	"fmt"
	"os"

	"github.com/Tencent/bk-ci/src/booster/common/version"

	commandCli "github.com/urfave/cli"
)

const (
	FlagProjectID      = "project_id"
	FlagBoosterType    = "booster_type"
	FlagAllInfo        = "all"
	FlagUseTestAdderss = "test"
	FlagDay            = "within_days"

	CommandGetConfig    = "get_config"
	CommandGetWorkStats = "get_stats"
)

func Run(ct ClientType) {
	if err := run(ct); err != nil {
		_, _ = fmt.Fprintln(os.Stderr, err)
	}
}

func run(ct ClientType) error {
	return GetApp(ct).Run(os.Args)
}

var (
	getconfigFlags = []commandCli.Flag{
		commandCli.StringFlag{
			Name:  "project_id, p",
			Usage: "project ID of this build, it is necessary",
		},
		commandCli.StringFlag{
			Name:  "booster_type, bt",
			Usage: "booster build type, it is necessary",
		},
		commandCli.BoolFlag{
			Name:  "all, a",
			Usage: "print all the info of project",
		},
		commandCli.BoolFlag{
			Name:  "test, t",
			Usage: "using test gateway address",
		},
	}
	getstatsFlags = []commandCli.Flag{
		commandCli.StringFlag{
			Name:  "project_id, p",
			Usage: "project ID of this build, it is necessary",
		},
		commandCli.StringFlag{
			Name:  "booster_type, bt",
			Usage: "booster build type, it is necessary",
		},
		commandCli.StringFlag{
			Name:  "within_days , d",
			Value: "1",
			Usage: "get stats in within_days ",
		},
		commandCli.BoolFlag{
			Name:  "all, a",
			Usage: "print all the task_id",
		},
	}
)

// GetApp get app by type
func GetApp(ct ClientType) *commandCli.App {
	client := commandCli.NewApp()
	client.Name = ct.Name()
	client.Usage = ct.Usage()
	client.Version = fmt.Sprintf("Version:   %s\n\t Tag:       %s\n\t BuildTime: %s\n\t GitHash:   %s",
		version.Version, version.Tag, version.BuildTime, version.GitHash)

	client.Commands = []commandCli.Command{
		{
			Name:    CommandGetConfig,
			Aliases: []string{"getconfig"},
			Usage:   "get project info by project id",
			Action:  Action,
			Flags:   getconfigFlags,
		},
		{
			Name:    CommandGetWorkStats,
			Aliases: []string{"getstats"},
			Usage:   "get work stats by project id",
			Action:  Action,
			Flags:   getstatsFlags,
		},
	}

	return client
}
