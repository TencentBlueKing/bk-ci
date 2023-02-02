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
	"context"
	"fmt"
	"net/url"
	"os"
	"os/signal"
	"os/user"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"syscall"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/booster/pkg"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/conf"

	"github.com/shirou/gopsutil/process"
	commandCli "github.com/urfave/cli"
)

// boosterProcess do the make process:
// 1. apply resources.
// 2. keep heartbeat.
// 3. execute task with remote resources.
// 4. release the resources when cancel or finish.
func boosterProcess(c *commandCli.Context) error {
	toconsole := true
	if c.String(FlagLogToConsole) == "false" {
		toconsole = false
	}
	initialLogDir(getLogDir(c.String(FlagLogDir)), toconsole)
	setLogLevel(c.String(FlagLog))
	defer blog.CloseLogs()

	// get the new Booster and give it to the standalone booster in commandline tool.
	booster, err := newBooster(c)
	if err != nil {
		blog.Errorf("booster-command: init booster failed: %v", err)
		return err
	}
	ctx, cancel := context.WithCancel(context.Background())

	// run a system signal watcher for breaking process
	go sysSignalHandler(cancel, booster)

	// run booster.
	code, err := booster.Run(ctx)

	// save commands exit code if need
	if c.Bool(FlagSaveCode) {
		saveExitCode(code)
	}

	blog.Infof("booster-command: run process done, taskID: %s", booster.GetTaskID())
	return err
}

func saveExitCode(code int) {
	_, err := os.Stat(exitCodeFile)
	if os.IsNotExist(err) {
		f, err := os.Create(exitCodeFile)
		if err != nil {
			blog.Errorf("booster-command: failed to create exit code file: %v\n", err)
			return
		}
		defer func() {
			_ = f.Close()
		}()
	}

	file, err := os.OpenFile(exitCodeFile, os.O_RDWR, 0644)
	if err != nil {
		blog.Errorf("booster-command: failed to open exit code file: %v\n", err)
		return
	}
	defer func() {
		_ = file.Close()
	}()

	_ = file.Truncate(0)
	_, err = file.WriteString(fmt.Sprintf("%d", code))
	if err != nil {
		blog.Errorf("booster-command: failed to write exit code file: %v\n", err)
		return
	}

	blog.Infof("booster-command: success to write exit code(%d) in %s\n", code, exitCodeFile)
}

func sysSignalHandler(cancel context.CancelFunc, booster *pkg.Booster) {
	interrupt := make(chan os.Signal)
	signal.Notify(interrupt, syscall.SIGINT, syscall.SIGTERM)

	select {
	case sig := <-interrupt:
		blog.Warnf("booster-command: get system signal %s, going to exit", sig.String())

		// cancel booster's context and make sure that task is released.
		cancel()

		booster.ExitCode = 1
		_ = booster.UnregisterWork()

		p, err := process.NewProcess(int32(os.Getpid()))
		if err == nil {
			blog.Debugf("booster: ready kill children when recieved sinal")
			// kill children
			pkg.KillChildren(p)
		}
		blog.CloseLogs()

		// catch control-C and should return code 130(128+0x2)
		if sig == syscall.SIGINT {
			os.Exit(130)
		}

		// catch kill and should return code 143(128+0xf)
		if sig == syscall.SIGTERM {
			os.Exit(143)
		}

		os.Exit(1)
	}
}

func newBooster(c *commandCli.Context) (*pkg.Booster, error) {
	// get current running dir.
	// if failed, it doesn't matter, just give a warning.
	runDir, err := os.Getwd()
	if err != nil {
		blog.Warnf("booster-command: get working dir failed: %v", err)
	}

	// get current user, use the login username.
	// if failed, it doesn't matter, just give a warning.
	usr, err := user.Current()
	if err != nil {
		blog.Warnf("booster-command: get current user failed: %v", err)
		return nil, err
	}

	// decide which server to connect to.
	ServerDomain, ServerHost, err := getSpecificDomainAndHost(c)
	if err != nil {
		blog.Errorf("booster-command: get specific server failed: %v", err)
		return nil, err
	}

	controllerIP := ControllerIP
	if c.Bool(FlagDashboard) {
		if ipList := dcUtil.GetIPAddress(); len(ipList) > 0 {
			controllerIP = ipList[0]
		}
	}

	projectID := c.String(FlagProjectID)
	if !c.IsSet(FlagProjectID) {
		projectID = os.Getenv(EnvProjectID)
	}

	bt := c.String(FlagBoosterType)
	if !c.IsSet(FlagBoosterType) {
		bt = os.Getenv(EnvBoosterType)
	}

	buildID := c.String(FlagBuildID)
	if !c.IsSet(FlagBuildID) {
		buildID = os.Getenv(EnvBuildIDOld)
		if buildID == "" {
			buildID = os.Getenv(EnvBuildID)
		}
	}

	blog.Debugf("booster-command: got project id[%s] from flags", projectID)
	if projectID == "" || projectID == "nothing" {
		projectID = dcUtil.SearchProjectID()
		blog.Debugf("booster-command: got project id[%s] from json file", projectID)
	}

	remaintime := 120
	if c.IsSet(FlagControllerRemainTime) {
		remaintime = c.Int(FlagControllerRemainTime)
	}

	waitResourceSeconds := 60
	if c.IsSet(FlagResourceTimeoutSecs) {
		waitResourceSeconds = c.Int(FlagResourceTimeoutSecs)
	}

	useLocalCPUPercent := 0
	if c.IsSet(FlagLocalIdleCPUPercent) {
		useLocalCPUPercent = c.Int(FlagLocalIdleCPUPercent)
	}

	resIdleSecsForFree := 120
	if c.IsSet(FlagResIdleSecsForFree) {
		resIdleSecsForFree = c.Int(FlagResIdleSecsForFree)
	}

	pumpCacheSizeMaxMB := int32(c.Int(FlagPumpCacheSizeMaxMB))
	if pumpCacheSizeMaxMB <= 0 {
		pumpCacheSizeMaxMB = 1024
	}

	pumpMinActionNum := 50
	if c.IsSet(FlagPumpMinActionNum) {
		pumpMinActionNum = c.Int(FlagPumpMinActionNum)
	}

	// generate a new booster.
	cmdConfig := dcType.BoosterConfig{
		Type:      dcType.GetBoosterType(bt),
		ProjectID: projectID,
		BuildID:   buildID,
		BatchMode: c.Bool(FlagBatchMode),
		Args:      c.String(FlagArgs),
		Cmd:       strings.Join(os.Args, " "),
		Works: dcType.BoosterWorks{
			Stdout:               os.Stdout,
			Stderr:               os.Stderr,
			RunDir:               runDir,
			User:                 usr.Username,
			LimitPerWorker:       c.Int(FlagLimit),
			Jobs:                 c.Int(FlagJobs),
			MaxJobs:              c.Int(FlagMaxJobs),
			MaxDegradedJobs:      c.Int(FlagMaxDegradedJobs),
			MaxLocalTotalJobs:    defaultCPULimit(c.Int(FlagMaxLocalTotalJobs)),
			MaxLocalPreJobs:      c.Int(FlagMaxLocalPreJobs),
			MaxLocalExeJobs:      c.Int(FlagMaxLocalExeJobs),
			MaxLocalPostJobs:     c.Int(FlagMaxLocalPostJobs),
			HookPreloadLibPath:   c.String(FlagHookPreloadLib),
			HookConfigPath:       c.String(FlagHookConfig),
			HookMode:             c.Bool(FlagHook),
			NoLocal:              c.Bool(FlagNoLocal),
			Local:                c.Bool(FlagLocal),
			WorkerSideCache:      c.Bool(FlagWorkerSideCache),
			LocalRecord:          c.Bool(FlagLocalRecord),
			Degraded:             c.Bool(FlagDegraded),
			ExecutorLogLevel:     c.String(FlagExecutorLog),
			Environments:         make(map[string]string),
			Bazel:                c.Bool(FlagBazel),
			BazelPlus:            c.Bool(FlagBazelPlus),
			Bazel4Plus:           c.Bool(FlagBazel4Plus),
			Launcher:             c.Bool(FlagLauncher) || c.Bool(FlagBazelPlus) || c.Bool(FlagBazel4Plus),
			AdditionFiles:        c.StringSlice(FlagAdditionFile),
			WorkerList:           c.StringSlice(FlagWorkerList),
			CheckMd5:             c.Bool(FlagCheckMd5),
			OutputEnvJSONFile:    c.StringSlice(FlagOutputEnvJSONFile),
			OutputEnvSourceFile:  c.StringSlice(FlagOutputEnvSourceFile),
			CommitSuicide:        c.Bool(FlagCommitSuicide),
			ToolChainJSONFile:    c.String(FlagToolChainJSONFile),
			SupportDirectives:    c.Bool(FlagDirectives),
			GlobalSlots:          c.Bool(FlagGlobalSlots),
			IOTimeoutSecs:        c.Int(FlagIOTimeoutSecs),
			Pump:                 c.Bool(FlagPump),
			PumpDisableMacro:     c.Bool(FlagPumpDisableMacro),
			PumpIncludeSysHeader: c.Bool(FlagPumpIncludeSysHeader),
			PumpCheck:            c.Bool(FlagPumpCheck),
			PumpCache:            c.Bool(FlagPumpCache),
			PumpCacheDir:         c.String(FlagPumpCacheDir),
			PumpCacheSizeMaxMB:   pumpCacheSizeMaxMB,
			PumpCacheRemoveAll:   c.Bool(FlagPumpCacheRemoveAll),
			PumpBlackList:        c.StringSlice(FlagPumpBlackList),
			PumpMinActionNum:     int32(pumpMinActionNum),
			ForceLocalList:       c.StringSlice(FlagForceLocalList),
			NoWork:               c.Bool(FlagNoWork),
			WriteMemroy:          c.Bool(FlagWriteMemroMemroy),
			IdleKeepSecs:         c.Int(FlagIdleKeepSecs),
		},

		Transport: dcType.BoosterTransport{
			ServerDomain:           ServerDomain,
			ServerHost:             ServerHost,
			Timeout:                5 * time.Second,
			HeartBeatTick:          5 * time.Second,
			InspectTaskTick:        100 * time.Millisecond,
			TaskPreparingTimeout:   time.Duration(waitResourceSeconds) * time.Second,
			PrintTaskInfoEveryTime: 5,
			CommitSuicideCheckTick: 5 * time.Second,
		},

		Controller: sdk.ControllerConfig{
			NoLocal: false,
			Scheme:  ControllerScheme,
			IP:      controllerIP,
			Port:    ControllerPort,
			Timeout: 5 * time.Second,
			LogDir:  getLogDir(c.String(FlagLogDir)),
			LogVerbosity: func() int {
				// debug模式下, --v=3
				if c.String(FlagLog) == dcUtil.PrintDebug.String() {
					return 3
				}
				return 0
			}(),
			TotalSlots:         c.Int(FlagMaxLocalTotalJobs),
			PreSlots:           c.Int(FlagMaxLocalPreJobs),
			ExeSlots:           c.Int(FlagMaxLocalExeJobs),
			PostSlots:          c.Int(FlagMaxLocalPostJobs),
			Sudo:               c.Bool(FlagSudoController),
			NoWait:             c.Bool(FlagControllerNoWait),
			RemainTime:         remaintime,
			UseLocalCPUPercent: useLocalCPUPercent,
			DisableFileLock:    c.Bool(FlagDisableFileLock),
			AutoResourceMgr:    c.Bool(FlagAutoResourceMgr),
			ResIdleSecsForFree: resIdleSecsForFree,
			SendCork:           c.Bool(FlagSendCork),
		},
	}

	// by tomtian 20210407
	dealWithUserDefinedEnv(c, &cmdConfig)
	blog.Infof("booster-command: config detail:%+v", cmdConfig)

	return pkg.NewBooster(cmdConfig)
}

// if set by user, set it to env, otherwise try get from env
func dealWithUserDefinedEnv(c *commandCli.Context, b *dcType.BoosterConfig) {
	envstrvalue := c.String(FlagLog)
	if envstrvalue != "" && envstrvalue != "nothing" {
		_ = env.SetEnv(env.KeyUserDefinedLogLevel, envstrvalue)
	}

	envstrvalue = c.String(FlagExecutorLog)
	if envstrvalue != "" && envstrvalue != "nothing" {
		_ = env.SetEnv(env.KeyUserDefinedExecutorLogLevel, envstrvalue)
	} else {
		envstrvalue = env.GetEnv(env.KeyUserDefinedExecutorLogLevel)
		if envstrvalue != "" {
			b.Works.ExecutorLogLevel = envstrvalue
		}
	}

	envintvalue := c.Int(FlagJobs)
	if envintvalue > 0 {
		_ = env.SetEnv(env.KeyUserDefinedJobs, strconv.Itoa(envintvalue))
	} else {
		envstrvalue = env.GetEnv(env.KeyUserDefinedJobs)
		if envstrvalue != "" {
			envintvalue, _ = strconv.Atoi(envstrvalue)
			b.Works.Jobs = envintvalue
		}
	}

	envintvalue = c.Int(FlagMaxJobs)
	if envintvalue > 0 {
		_ = env.SetEnv(env.KeyUserDefinedMaxJobs, strconv.Itoa(envintvalue))
	} else {
		envstrvalue = env.GetEnv(env.KeyUserDefinedMaxJobs)
		if envstrvalue != "" {
			envintvalue, _ = strconv.Atoi(envstrvalue)
			b.Works.MaxJobs = envintvalue
		}
	}

	envintvalue = c.Int(FlagMaxLocalTotalJobs)
	if envintvalue > 0 {
		_ = env.SetEnv(env.KeyUserDefinedMaxLocalJobs, strconv.Itoa(envintvalue))
	} else {
		envstrvalue = env.GetEnv(env.KeyUserDefinedMaxLocalJobs)
		if envstrvalue != "" {
			envintvalue, _ = strconv.Atoi(envstrvalue)
			b.Works.MaxLocalTotalJobs = envintvalue
		}
	}

	envintvalue = c.Int(FlagMaxLocalExeJobs)
	if envintvalue > 0 {
		_ = env.SetEnv(env.KeyUserDefinedMaxLocalExeJobs, strconv.Itoa(envintvalue))
	} else {
		envstrvalue = env.GetEnv(env.KeyUserDefinedMaxLocalExeJobs)
		if envstrvalue != "" {
			envintvalue, _ = strconv.Atoi(envstrvalue)
			b.Works.MaxLocalExeJobs = envintvalue
		}
	}

	envintvalue = c.Int(FlagMaxLocalPreJobs)
	if envintvalue > 0 {
		_ = env.SetEnv(env.KeyUserDefinedMaxLocalPreJobs, strconv.Itoa(envintvalue))
	} else {
		envstrvalue = env.GetEnv(env.KeyUserDefinedMaxLocalPreJobs)
		if envstrvalue != "" {
			envintvalue, _ = strconv.Atoi(envstrvalue)
			b.Works.MaxLocalPreJobs = envintvalue
		}
	}

	envintvalue = c.Int(FlagMaxLocalPostJobs)
	if envintvalue > 0 {
		_ = env.SetEnv(env.KeyUserDefinedMaxLocalPostJobs, strconv.Itoa(envintvalue))
	} else {
		envstrvalue = env.GetEnv(env.KeyUserDefinedMaxLocalPostJobs)
		if envstrvalue != "" {
			envintvalue, _ = strconv.Atoi(envstrvalue)
			b.Works.MaxLocalPostJobs = envintvalue
		}
	}

	envintvalue = c.Int(FlagIOTimeoutSecs)
	if envintvalue > 0 {
		_ = env.SetEnv(env.KeyUserDefinedIOTimeoutSecs, strconv.Itoa(envintvalue))
	} else {
		envstrvalue = env.GetEnv(env.KeyUserDefinedIOTimeoutSecs)
		if envstrvalue != "" {
			envintvalue, _ = strconv.Atoi(envstrvalue)
			b.Works.IOTimeoutSecs = envintvalue
		}
	}

	envstrvaluelist := c.StringSlice(FlagForceLocalList)
	if len(envstrvaluelist) > 0 {
		_ = env.SetEnv(env.KeyUserDefinedForceLocalList, strings.Join(envstrvaluelist, env.CommonBKEnvSepKey))
	} else {
		envstrvalue = env.GetEnv(env.KeyUserDefinedForceLocalList)
		if envstrvalue != "" {
			b.Works.ForceLocalList = strings.Split(envstrvalue, env.CommonBKEnvSepKey)
		}
	}
}

const (
	configFilename = "config.json"
)

// Config describe the configs in file
type Config struct {
	Server string `json:"server"`
}

func getSpecificDomainAndHost(c *commandCli.Context) (string, string, error) {
	// Server Necessary, Settings priority from high to low: --server ~/.bk_dist/config.json /etc/bk_dist/config.json
	server, err := getServerFromConfig(c)
	if err != nil {
		if ServerNecessary != "" {
			return "", "", err
		}

		// decide which server to connect to.
		ServerDomain := ProdBuildBoosterServerDomain
		ServerHost := ProdBuildBoosterServerHost
		if c.Bool(FlagTest) {
			ServerDomain = TestBuildBoosterServerDomain
			ServerHost = TestBuildBoosterServerHost
			blog.Infof("booster-command: -t specified and this task will connect to test server")
		}

		blog.Infof("booster-command: got server domain(%s), server host(%s)", ServerDomain, ServerHost)
		return ServerDomain, ServerHost, nil
	}

	uri, err := url.ParseRequestURI(server)
	if err != nil {
		return "", "", err
	}

	scheme := "http"
	if index := strings.Index(server, "://"); index > 0 {
		if scheme = server[:index]; scheme != "http" && scheme != "https" {
			err = fmt.Errorf("get error scheme, %s", scheme)
			return "", "", err
		}
		server = server[index:]
	}
	server = scheme + "://" + strings.TrimLeft(server, "/:")
	uri, err = url.ParseRequestURI(server)
	if err != nil {
		return "", "", err
	}

	ServerDomain := uri.Hostname()
	ServerHost := strings.TrimRight(uri.String(), "/") + "/api"

	blog.Infof("booster-command: got server domain(%s), server host(%s)", ServerDomain, ServerHost)
	return ServerDomain, ServerHost, nil
}

func getServerFromConfig(c *commandCli.Context) (string, error) {
	if c.IsSet(FlagServer) {
		s := c.String(FlagServer)
		blog.Infof("booster-command: use server from command line --server specified: %s", s)
		return s, nil
	}

	userF := filepath.Join(dcUtil.GetRuntimeDir(), configFilename)
	userC, userErr := getConfig(userF)
	if userErr == nil {
		blog.Infof("booster-command: use server from user home config file(%s): %s", userF, userC.Server)
		return userC.Server, nil
	}

	globalF := config.GetFile(configFilename)
	globalC, globalErr := getConfig(globalF)
	if globalErr == nil {
		blog.Infof("booster-command: use server from global config file(%s): %s", globalF, globalC.Server)
		return globalC.Server, nil
	}

	blog.Warnf("booster-command: no server specified, none of --server, user home config, or global config")
	return "", fmt.Errorf("no server specified")
}

func getConfig(path string) (*Config, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}

	var c Config
	if err = codec.DecJSONReader(f, &c); err != nil {
		blog.Warnf("booster-command: decode json settings from %s failed: %v", path, err)
		return nil, err
	}

	return &c, nil
}

func setLogLevel(level string) {
	if level == "" {
		level = env.GetEnv(env.KeyUserDefinedLogLevel)
	}

	switch level {
	case dcUtil.PrintDebug.String():
		blog.SetV(3)
		blog.SetStderrLevel(blog.StderrLevelInfo)
	case dcUtil.PrintInfo.String():
		blog.SetStderrLevel(blog.StderrLevelInfo)
	case dcUtil.PrintWarn.String():
		blog.SetStderrLevel(blog.StderrLevelWarning)
	case dcUtil.PrintError.String():
		blog.SetStderrLevel(blog.StderrLevelError)
	case dcUtil.PrintNothing.String():
		blog.SetStderrLevel(blog.StderrLevelNothing)
	default:
		// default to be error printer.
		blog.SetStderrLevel(blog.StderrLevelInfo)
	}
}

func defaultCPULimit(custom int) int {
	if custom > 0 {
		return custom
	}
	return runtime.NumCPU() - 2
}

func initialLogDir(dir string, toconsole bool) {
	blog.InitLogs(conf.LogConfig{
		LogDir:       dir,
		LogMaxNum:    10,
		LogMaxSize:   500,
		AlsoToStdErr: toconsole,
	})
}

func getLogDir(dir string) string {
	if dir == "" {
		return dcUtil.GetLogsDir()
	}

	return dir
}
