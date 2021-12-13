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
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/types"

	commandCli "github.com/urfave/cli"
)

// MakeProcess do the make process:
// 1. apply resources.
// 2. keep heartbeat.
// 3. execute the make command when resources are ready.
// 4. release the resources when cancel or finish.
func MakeProcess(c *commandCli.Context) error {
	initProcess(c)
	return makeProcess(c)
}

func makeProcess(c *commandCli.Context) error {
	if c.Bool(FlagClang) {
		Compiler = CompilerClang
	}

	projectID := c.String(FlagProjectID)
	if projectID == "" {
		fmt.Printf("WARNING: %s must be specified\n", FlagProjectID)
	}

	buildID := c.String(FlagBuildID)
	if buildID == "" {
		fmt.Println("buildID is not specified, will be ignored")
	}

	var cCacheEnabled *bool
	if c.IsSet(FlagCCacheEnabled) {
		tmp := c.String(FlagCCacheEnabled) == "true"
		cCacheEnabled = &tmp

		hasSetCache = true
		cacheEnabled = &tmp
	}

	gccVersion := c.String(FlagGccVersion)
	args := c.String(FlagArgs)
	hookMode = c.Bool(FlagHook)

	task, ok, err := applyDistCCResources(types.DistccServerSets{
		CommandType:   types.CommandMake,
		ProjectId:     projectID,
		BuildId:       buildID,
		GccVersion:    gccVersion,
		CCacheEnabled: cCacheEnabled,
		Params:        args,
		ExtraVars: types.ExtraVars{
			MaxJobs: MaxJobs,
		},
	})
	// only server communicate ok and error then return error, if communicate failed then compile locally.
	if ok && err != nil {
		fmt.Printf("request compiler resource failed: %v, degraded to local compiling.\n", err)
		return localMakeCompiling(args)
	} else if !ok {
		fmt.Printf("failed to connect to server, degraded to local compiling.\n")
		return localMakeCompiling(args)
	}
	fmt.Printf("success to apply new task: %s\n", task.TaskID)
	fmt.Printf("status(%s): %s\n", task.Status, task.Message)

	var data []byte
	_ = codec.EncJSON(task, &data)
	DebugPrintf("request info: %s\n", string(data))

	go loopHeartBeat(task.TaskID)
	go handlerSysSignal(task.TaskID)

	var waitedSecs = 1
	var waitTimeout = false
	var lastStatus = task.Status
	for ; ; func() {
		time.Sleep(time.Duration(SleepSecsPerWait) * time.Second)
		waitedSecs += SleepSecsPerWait
	}() {
		info, err := inspectDistCCServers(task.TaskID)
		if err != nil {
			fmt.Printf("inspect distcc servers error: %v\n", err)
			if waitedSecs >= TotalWaitServerSecs {
				fmt.Printf("has waited server for [%d]seconds, quit to local compile\n", waitedSecs)
				waitTimeout = true
				break
			}
			continue
		}

		if info.Status != lastStatus || waitedSecs%PrintEveryTimes == 0 {
			fmt.Printf("status(%s): %s\n", info.Status, info.Message)
		}
		lastStatus = info.Status

		if info.Status == types.ServerStatusRunning || info.Status == types.ServerStatusFailed ||
			info.Status == types.ServerStatusFinish {
			task = info
			break
		}
	}

	if task.Status == types.ServerStatusFailed ||
		task.Status == types.ServerStatusFinish ||
		waitTimeout {
		fmt.Printf("init distcc server error: %s\n", task.Message)
		return localMakeCompiling(args)
	}

	_ = codec.EncJSON(task, &data)
	DebugPrintf("distcc server: %s\n", string(data))
	var clientInfo *types.DistccClientInfo

	// transform gcc version from some mask tag
	task.GccVersion = TransformGccVersion(task.GccVersion)

	if !strings.Contains(task.GccVersion, string(Compiler)) {
		err = fmt.Errorf("settings compiler version: %s, "+
			"seems like not fit for this tool which provide compiler: %s", task.GccVersion, Compiler)
		fmt.Printf("%v\n", err)
		clientInfo = &types.DistccClientInfo{
			TaskID:  task.TaskID,
			Status:  types.ClientStatusFailed,
			Message: err.Error(),
		}
		distCCDone(clientInfo)
		os.Exit(1)
	}

	// check local compiler version and remote settings
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

	if task.CCacheEnabled {
		cmd := exec.Command("/bin/bash", "-c", "ccache -z")
		err = cmd.Run()
		if err != nil {
			fmt.Printf("exec command ccache -z error: %v\n", err)
		}
	}

	if hookMode {
		fmt.Printf("exec command: %s\n", task.Cmds)
		err = hookCompiling(task.Cmds, task.Envs)
	} else {
		fmt.Printf("exec command: %s\n", task.Cmds)
		cmd := exec.Command("/bin/bash", "-c", task.Cmds)
		dir, _ := os.Getwd()
		cmd.Dir = dir
		cmd.Stdout = os.Stdout
		cmd.Stderr = os.Stderr
		err = cmd.Run()
	}

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

	fmt.Printf("\n* %s done *\n", ClientMake.Name())
	return err
}

func localMakeCompiling(param string) error {
	if NoLocal {
		return ErrNoLocal
	}

	if hookMode {
		return localMakeCompilingHook(param)
	}

	jobs := getLocalJobs()

	var command string
	switch Compiler {
	case CompilerGcc:
		command = fmt.Sprintf("make -j%d BK_CC='gcc' BK_CXX='g++' BK_JOBS=%d %s", jobs, jobs, param)
	case CompilerClang:
		command = fmt.Sprintf("make -j%d BK_CC='clang' BK_CXX='clang++' BK_JOBS=%d %s", jobs, jobs, param)
	}

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

func localMakeCompilingHook(param string) error {
	var command string
	command = fmt.Sprintf("make %s", param)
	fmt.Printf("exec command: %s\n", command)

	var commandCC string
	switch Compiler {
	case CompilerGcc:
		commandCC = "gcc"
	case CompilerClang:
		commandCC = "clang"
	}

	if hasSetCache && *cacheEnabled {
		commandCC = "ccache " + commandCC
	}
	envs := map[string]string{}
	err := hookCompilingWithCmdCompilers(command, commandCC, envs)
	saveExitCode(err)
	if err != nil {
		return ErrCompile
	}
	return nil
}
