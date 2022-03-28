package pkg

import (
	"fmt"
	"os"

	"github.com/Tencent/bk-ci/src/booster/common/version"

	commandCli "github.com/urfave/cli"
)

// define const keys for fast build main
const (
	FlagURL  = "url"
	FlagFile = "file"
	FlagDir  = "dir"

	CommandDownload = "download"
	CommandMd5sum   = "md5sum"
	CommandUnzip    = "unzip"
)

// Name : return client name
func Name() string {
	return "bk-help-tool"
}

// Run : start main thread
func Run() {
	if err := run(); err != nil {
		_, _ = fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}

func run() error {
	client := commandCli.NewApp()
	client.Name = Name()
	client.Version = fmt.Sprintf("Version:   %s\n\t Tag:       %s\n\t BuildTime: %s\n\t GitHash:   %s",
		version.Version, version.Tag, version.BuildTime, version.GitHash)

	client.Commands = []commandCli.Command{
		{
			Name:    CommandDownload,
			Aliases: []string{"dl"},
			Usage:   "download file by http url",
			Action:  Action,
			Flags: []commandCli.Flag{
				commandCli.StringFlag{
					Name:  "url",
					Usage: "http url ready to download",
				},
				commandCli.StringFlag{
					Name:  "file, f",
					Usage: "specified local file to save",
				},
			},
		},
		{
			Name:    CommandMd5sum,
			Aliases: []string{"md5sum"},
			Usage:   "md5sum specified file",
			Action:  Action,
			Flags: []commandCli.Flag{
				commandCli.StringFlag{
					Name:  "file, f",
					Usage: "file to md5sum",
				},
			},
		},
		{
			Name:    CommandUnzip,
			Aliases: []string{"unzip"},
			Usage:   "unzip file to specified dir",
			Action:  Action,
			Flags: []commandCli.Flag{
				commandCli.StringFlag{
					Name:  "file, f",
					Usage: "local zip file",
				},
				commandCli.StringFlag{
					Name:  "dir",
					Usage: "directory to save unziped files",
				},
			},
		},
	}

	client.Action = Action

	// override the version printer
	commandCli.VersionPrinter = func(c *commandCli.Context) {
		fmt.Printf("Version:   %s\nTag:       %s\nBuildTime: %s\nGitHash:   %s\n",
			version.Version, version.Tag, version.BuildTime, version.GitHash)
	}

	return client.Run(os.Args)
}
