/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

import (
	"fmt"
	"os"

	"github.com/Tencent/bk-ci/src/booster/common/version"

	commandCli "github.com/urfave/cli"
)

const (
	FlagProjectID     = "project_id"
	FlagBuildID       = "build_id"
	FlagArgs          = "args"
	FlagGccVersion    = "gcc_version"
	FlagCCacheEnabled = "ccache_enabled"
	FlagClang         = "clang"
	FlagDebug         = "debug"
	FlagTest          = "test"
	FlagBazelRcPath   = "bazelrc_path"
	FlagWorkspacePath = "workspace_path"
	FlagCommandPath   = "command_path"
	FlagLimit         = "limit"
	FlagNoLocal       = "no_local"
	FlagSaveCode      = "save_code"
	FlagMaxJobs       = "max_jobs"
	FlagMaxLocalJobs  = "max_local_jobs"
	FlagLocalLimit    = "local_limit"
	FlagLocalLimitCpp = "local_limit_cpp"
	FlagHook          = "hook"

	DistCCHostEnvKey = "DISTCC_HOSTS"
)

var (
	LocalIP  string
	Debug    bool
	NoLocal  bool
	SaveCode bool

	MaxJobs       int
	MaxLocalJobs  int
	SlotsLimit    = 0
	RunDir        = ""
	LocalLimit    = 0
	LocalLimitCpp = 0

	ErrNoLocal = fmt.Errorf("task degraded to local compiling and exit according to the --no_local flag set")
	ErrCompile = fmt.Errorf("compile error, build exit")
)

// Run run command
func Run(ct ClientType) {
	if err := run(ct); err != nil {
		_, _ = fmt.Fprintln(os.Stderr, err)
		switch err {
		case ErrCompile:
			os.Exit(3)
		case ErrNoLocal:
			os.Exit(4)
		default:
			os.Exit(1)
		}
	}
}

func run(ct ClientType) error {
	return GetApp(ct).Run(os.Args)
}

// GetApp return the command handler
func GetApp(ct ClientType) *commandCli.App {
	client := commandCli.NewApp()
	client.Name = ct.Name()
	client.Usage = ct.Usage()
	client.Version = fmt.Sprintf("Version:   %s\n\t Tag:       %s\n\t BuildTime: %s\n\t GitHash:   %s",
		version.Version, version.Tag, version.BuildTime, version.GitHash)

	client.Flags = []commandCli.Flag{
		commandCli.BoolFlag{
			Name:  "debug",
			Usage: "debug mode will print some information",
		},
		commandCli.StringFlag{
			Name:  "project_id, p",
			Usage: "project ID of this build, it is necessary",
		},
		commandCli.BoolFlag{
			Name:  "clang, C",
			Usage: "local compiler use clang instead of gcc",
		},
		commandCli.StringFlag{
			Name:  "args, a",
			Usage: "flags and args that will be pass-through",
		},
		commandCli.BoolFlag{
			Name:  "test, t",
			Usage: "connect to test environment",
		},
		commandCli.IntFlag{
			Name:  "limit, l",
			Usage: "limit the jobs per remote instance",
		},
		commandCli.BoolFlag{
			Name:  "no_local",
			Usage: "never degraded to local compiling, or exit with code 4",
		},
		commandCli.BoolFlag{
			Name:  "save_code, s",
			Usage: "save the original command exit code in file bk_origin_exit_code",
		},
		commandCli.IntFlag{
			Name: "max_jobs",
			Usage: "max parallel jobs for compiling, if the jobs given by server is larger, " +
				"then use the max_jobs instead",
		},
		commandCli.IntFlag{
			Name: "max_local_jobs",
			Usage: "max parallel jobs for compiling when degraded to local, if local cpu number is larger, " +
				"then use the max_local_jobs instead",
		},
		commandCli.IntFlag{
			Name:  "local_limit",
			Usage: "set localhost slots for distcc client, matters the max fallback compiling concurrency in client",
		},
		commandCli.IntFlag{
			Name:  "local_limit_cpp",
			Usage: "set localhost slots cpp for distcc client, matters the max pre-process concurrency in client",
		},
		commandCli.BoolFlag{
			Name:  "hook",
			Usage: "call distcc by hook if true",
		},
	}

	switch ct {
	case ClientMake:
		client.Flags = append(client.Flags, []commandCli.Flag{
			commandCli.StringFlag{
				Name:  "build_id, b",
				Usage: "ID of this build",
			},
			commandCli.StringFlag{
				Name:  "gcc_version, g",
				Usage: "gcc/g++ version of this build, if it is no specified, will be set the one in project settings",
			},
			commandCli.StringFlag{
				Name: "ccache_enabled, c",
				Usage: "true/false, decide whether the build use ccache in localhost. if it is no specified, " +
					"will be set the one in project settings",
			},
		}...)

		client.Action = MakeProcess
	case ClientCMake:
		client.Flags = append(client.Flags, []commandCli.Flag{}...)

		client.Action = CMakeProcess
	case ClientBazel:
		client.Flags = append(client.Flags, []commandCli.Flag{
			commandCli.StringFlag{
				Name:  "bazelrc_path, rp",
				Usage: "path of --bazelrc file, it will be ./bk_bazelrc by default",
			},
			commandCli.StringFlag{
				Name:  "workspace_path, wp",
				Usage: "path of workspace, it will be ./ by default",
			},
			commandCli.StringFlag{
				Name:  "command_path, cp",
				Usage: "path of command, such as path for make/bazel/blade/cmake",
			},
			commandCli.StringFlag{
				Name:  "build_id, b",
				Usage: "ID of this build",
			},
			commandCli.StringFlag{
				Name:  "gcc_version, g",
				Usage: "gcc/g++ version of this build, if it is no specified, will be set the one in project settings",
			},
			commandCli.StringFlag{
				Name: "ccache_enabled, c",
				Usage: "true/false, decide whether the build use ccache in localhost. if it is no specified, " +
					"will be set the one in project settings",
			},
		}...)

		client.Action = BazelProcess
	case ClientBlade:
		client.Flags = append(client.Flags, []commandCli.Flag{
			commandCli.StringFlag{
				Name:  "command_path, cp",
				Usage: "path of command, such as path for make/bazel/blade/cmake",
			},
			commandCli.StringFlag{
				Name:  "build_id, b",
				Usage: "ID of this build",
			},
			commandCli.StringFlag{
				Name:  "gcc_version, g",
				Usage: "gcc/g++ version of this build, if it is no specified, will be set the one in project settings",
			},
			commandCli.StringFlag{
				Name: "ccache_enabled, c",
				Usage: "true/false, decide whether the build use ccache in localhost. if it is no specified, " +
					"will be set the one in project settings",
			},
		}...)

		client.Action = BladeProcess
	case ClientNinja:
		client.Flags = append(client.Flags, []commandCli.Flag{
			commandCli.StringFlag{
				Name:  "build_id, b",
				Usage: "ID of this build",
			},
			commandCli.StringFlag{
				Name:  "gcc_version, g",
				Usage: "gcc/g++ version of this build, if it is no specified, will be set the one in project settings",
			},
			commandCli.StringFlag{
				Name: "ccache_enabled, c",
				Usage: "true/false, decide whether the build use ccache in localhost. if it is no specified, " +
					"will be set the one in project settings",
			},
		}...)

		client.Action = NinjaProcess
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

// DebugPrintf print logs when DEBUG mode
func DebugPrintf(format string, a ...interface{}) {
	if Debug {
		fmt.Printf(format, a...)
	}
}

// SetDebug update debug mode settings
func SetDebug(debug bool) {
	Debug = debug
}

func initProcess(c *commandCli.Context) {
	DistCCServerDomain = ProdDistCCServerDomain
	DistCCServerHost = ProdDistCCServerHost

	if c.Bool(FlagTest) {
		DistCCServerDomain = TestDistCCServerDomain
		DistCCServerHost = TestDistCCServerHost
	}
	if c.Bool(FlagClang) {
		Compiler = CompilerClang
	}

	NoLocal = c.Bool(FlagNoLocal)
	SaveCode = c.Bool(FlagSaveCode)
	MaxJobs = c.Int(FlagMaxJobs)
	MaxLocalJobs = c.Int(FlagMaxLocalJobs)
	SlotsLimit = c.Int(FlagLimit)
	LocalLimit = c.Int(FlagLocalLimit)
	LocalLimitCpp = c.Int(FlagLocalLimitCpp)

	var err error
	RunDir, err = os.Getwd()
	if err != nil {
		fmt.Printf("get working dir failed: %v\n", err)
	}
	SetDebug(c.Bool(FlagDebug))
}

func init() {
	var err error
	LocalIP, err = getLocalIP()
	if err != nil {
		_, _ = fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
