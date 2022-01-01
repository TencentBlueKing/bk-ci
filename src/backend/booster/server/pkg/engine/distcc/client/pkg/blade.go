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
	"os/exec"
	"strconv"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/types"

	commandCli "github.com/urfave/cli"
)

var (
	bladeCommandPath         = "blade"
	bladeCCacheDisableEnvKey = "CCACHE_DISABLE"

	templateJobsKey = "@BK_JOBS"
)

// BladeProcess do the blade build process:
// 1. apply resources.
// 2. keep heartbeat.
// 3. execute the blade command when resources are ready.
// 4. release the resources when cancel or finish.
func BladeProcess(c *commandCli.Context) error {
	initProcess(c)
	return bladeProcess(c)
}

func bladeProcess(c *commandCli.Context) error {
	// get args
	if c.IsSet(FlagCommandPath) {
		bladeCommandPath = c.String(FlagCommandPath)
		fmt.Printf("Blade command path is set to [%s] by user\n", bladeCommandPath)
	}

	projectID := c.String(FlagProjectID)
	if projectID == "" {
		fmt.Printf("WARNING: %s must be specified\n", FlagProjectID)
	}

	buildID := c.String(FlagBuildID)
	if buildID == "" {
		fmt.Println("buildID is not specified, will be ignored")
	}

	if c.IsSet(FlagCCacheEnabled) {
		tmp := c.String(FlagCCacheEnabled) == "true"
		cacheEnabled = &tmp
		hasSetCache = true
	}

	gccVersion := c.String(FlagGccVersion)
	args := c.String(FlagArgs)

	task, ok, err := applyDistCCResources(types.DistccServerSets{
		CommandType:   types.CommandBlade,
		ProjectId:     projectID,
		BuildId:       buildID,
		GccVersion:    gccVersion,
		CCacheEnabled: cacheEnabled,
		Params:        args,
		ExtraVars: types.ExtraVars{
			MaxJobs: MaxJobs,
		},
	})
	// only server communicate ok and error then return error, if communicate failed then compile locally.
	if ok && err != nil {
		fmt.Printf("request compiler resource failed: %v, degraded to local compiling.\n", err)
		return bladeLocalCompiling(args)
	} else if !ok {
		fmt.Printf("failed to connect to server, degraded to local compiling.\n")
		return bladeLocalCompiling(args)
	}
	fmt.Printf("success to apply new task: %s\n", task.TaskID)
	fmt.Printf("status(%s): %s\n", task.Status, task.Message)

	var data []byte
	_ = codec.EncJSON(task, &data)
	DebugPrintf("request info: %s\n", string(data))

	go loopHeartBeat(task.TaskID)
	go handlerSysSignal(task.TaskID)

	var waitedsecs = 0
	var waittimeout = false
	for {
		time.Sleep(time.Duration(SleepSecsPerWait) * time.Second)
		waitedsecs += SleepSecsPerWait

		info, err := inspectDistCCServers(task.TaskID)
		if err != nil {
			fmt.Printf("inspect distcc servers error: %v\n", err)
			if waitedsecs >= TotalWaitServerSecs {
				fmt.Printf("has waited server for [%d]seconds,quit to local compile\n", waitedsecs)
				waittimeout = true
				break
			}
			continue
		}

		fmt.Printf("status(%s): %s\n", info.Status, info.Message)
		if info.Status == types.ServerStatusRunning || info.Status == types.ServerStatusFailed ||
			info.Status == types.ServerStatusFinish {
			task = info
			break
		}
	}

	if task.Status == types.ServerStatusFailed ||
		task.Status == types.ServerStatusFinish ||
		waittimeout {
		fmt.Printf("init distcc server error: %s\n", task.Message)
		return bladeLocalCompiling(args)
	}

	_ = codec.EncJSON(task, &data)
	DebugPrintf("distcc server: %s\n", string(data))
	var clientInfo *types.DistccClientInfo

	// transform gcc version from some mask tag
	task.GccVersion = TransformGccVersion(task.GccVersion)

	if !strings.Contains(task.GccVersion, string(Compiler)) {
		err = fmt.Errorf("settings BladeCompiler version: %s, "+
			"seems like not fit for this tool which provide BladeCompiler: %s", task.GccVersion, Compiler)
		fmt.Printf("%v\n", err)
		clientInfo = &types.DistccClientInfo{
			TaskID:  task.TaskID,
			Status:  types.ClientStatusFailed,
			Message: err.Error(),
		}
		distCCDone(clientInfo)
		os.Exit(1)
	}

	// check local BladeCompiler version and remote settings
	switch Compiler {
	case CompilerGcc:
		err = checkLocalGccVersion(strings.TrimPrefix(task.GccVersion, "gcc"))
	case CompilerClang:
		err = checkLocalClangVersion(strings.TrimPrefix(task.GccVersion, "clang"))
	}
	if err != nil {
		clientInfo = &types.DistccClientInfo{
			TaskID:  task.TaskID,
			Status:  types.ClientStatusFailed,
			Message: err.Error(),
		}
		distCCDone(clientInfo)
		os.Exit(1)
	}

	// exec the limit progress
	task.DistccHosts = setLimit(task.DistccHosts, SlotsLimit)
	task.DistccHosts = setLocalSlots(task.DistccHosts, LocalLimit, LocalLimitCpp)

	for k, v := range task.Envs {
		if k == DistCCHostEnvKey {
			v = task.DistccHosts
		}

		err := os.Setenv(k, v)
		if err != nil {
			fmt.Printf("set env %s=%s error: %v\n", k, v, err)
		}
	}

	for _, v := range task.UnsetEnvs {
		err := os.Unsetenv(v)
		if err != nil {
			fmt.Printf("UnsetEnvs %s error: %v\n", v, err)
		}
	}

	if task.CCacheEnabled {
		cmd := exec.Command("/bin/bash", "-c", "ccache -z")
		err = cmd.Run()
		if err != nil {
			fmt.Printf("exec command ccache -z error: %v\n", err)
		}
	}

	fmt.Printf("exec command: %s\n", task.Cmds)
	cmd := exec.Command("/bin/bash", "-c", task.Cmds)
	dir, _ := os.Getwd()
	cmd.Dir = dir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err = cmd.Run()

	saveExitCode(err)
	if err != nil {
		fmt.Printf("exec command: %s, failed: %v\n", task.Cmds, err)
		clientInfo = &types.DistccClientInfo{
			TaskID:  task.TaskID,
			Status:  types.ClientStatusFailed,
			Message: err.Error(),
		}
		err = ErrCompile
	} else {
		clientInfo = &types.DistccClientInfo{
			TaskID: task.TaskID,
			Status: types.ClientStatusSuccess,
		}
	}

	if task.CCacheEnabled {
		cCache, cacheErr := statisticsCCache()
		if cacheErr != nil {
			fmt.Printf("get ccache stats failed: %v\n", cacheErr)
		} else {
			clientInfo.Ccache = cCache
		}
	}

	distCCDone(clientInfo)

	fmt.Printf("\n* %s done *\n", ClientBlade.Name())
	return err
}

func bladeLocalCompiling(param string) error {
	if NoLocal {
		return ErrNoLocal
	}

	if hasSetCache {
		if *cacheEnabled {
			_ = os.Unsetenv(bladeCCacheDisableEnvKey)
		} else {
			_ = os.Setenv(bladeCCacheDisableEnvKey, "1")
		}
	}

	tempParamKey := strings.Replace(param, templateJobsKey, strconv.Itoa(getLocalJobs()), -1)
	var command string
	command = fmt.Sprintf("%s %s", bladeCommandPath, tempParamKey)
	fmt.Printf("exec command: %s\n", command)
	cmd := exec.Command("/bin/bash", "-c", command)
	dir, _ := os.Getwd()
	cmd.Dir = dir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	err := cmd.Run()
	saveExitCode(err)
	if err != nil {
		return ErrCompile
	}
	return nil
}
