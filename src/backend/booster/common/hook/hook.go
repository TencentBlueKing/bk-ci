/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package hook

import (
	"fmt"
	"os"
	"os/exec"
	"strconv"
	"strings"
	"syscall"
)

// define env keys for hook
const (
	EnvKeyLDPreload  = "LD_PRELOAD"
	EnvKeyHookConfig = "BK_HOOK_CONFIG_PATH"
	EnvKeyDistccHost = "DISTCC_HOSTS"
)

// define keys for bazel
const (
	BazelCmdKey         = "bazel "
	BazelBuildKey       = " build "
	BazelBuildSuffixKey = " build"
	BazelActionEnvKey   = "--action_env"
)

// ErrorCode define hook error code
type ErrorCode int

// define error codes
const (
	ErrorOk ErrorCode = iota
	ErrorFileNotExist
	ErrorFailedSetEnv
	ErrorInvalidBazelCmd
	ErrorNewBazelCmd
)

// RunProcess to hook and start process
func RunProcess(ldPreloadLibPath string, hookConfigPath string, envs map[string]string, cmd string) (int, error) {
	fmt.Printf("RunProcess with ldPreloadLibPath[%s],hookConfigPath[%s],cmd[%s]\n",
		ldPreloadLibPath, hookConfigPath, cmd)
	if err := checkParams(ldPreloadLibPath, hookConfigPath); err != nil {
		fmt.Printf("failed to RunProcess[%s] for error[%s]\n", cmd, err.Error())
		return int(ErrorFileNotExist), err
	}

	err := os.Setenv(EnvKeyLDPreload, ldPreloadLibPath)
	if err != nil {
		fmt.Printf("failed to RunProcess[%s] for error[%s]\n", cmd, err.Error())
		return int(ErrorFailedSetEnv), err
	}

	err = os.Setenv(EnvKeyHookConfig, hookConfigPath)
	if err != nil {
		fmt.Printf("failed to RunProcess[%s] for error[%s]\n", cmd, err.Error())
		return int(ErrorFailedSetEnv), err
	}

	for k, v := range envs {
		err = os.Setenv(k, v)
		if err != nil {
			fmt.Printf("failed to RunProcess[%s] for error[%s]\n", cmd, err.Error())
			return int(ErrorFailedSetEnv), err
		}
	}

	// run cmd now
	return runCmd(cmd)
}

// RunBazelProcess to hook and start bazel build process
func RunBazelProcess(
	ldPreloadLibPath string, hookConfigPath string, envs map[string]string, cmd string, jobs string) (int, error) {
	fmt.Printf("RunBazelProcess with ldPreloadLibPath[%s],hookConfigPath[%s],cmd[%s]\n",
		ldPreloadLibPath, hookConfigPath, cmd)
	if err := isValidBazelCmd(cmd); err != nil {
		fmt.Printf("failed to RunBazelProcess[%s] for error[%s]\n", cmd, err.Error())
		return int(ErrorInvalidBazelCmd), err
	}

	// adjust bazel cmd here
	newBazelCmd, err := getNewBazelCmd(ldPreloadLibPath, hookConfigPath, envs, cmd, jobs)
	if err != nil {
		fmt.Printf("failed to RunBazelProcess[%s] for error[%s]\n", cmd, err.Error())
		return int(ErrorNewBazelCmd), err
	}

	return RunProcess(ldPreloadLibPath, hookConfigPath, envs, newBazelCmd)
}

func isValidBazelCmd(cmd string) error {
	if !strings.Contains(cmd, BazelCmdKey) ||
		(!strings.Contains(cmd, BazelBuildKey) && !strings.HasSuffix(cmd, BazelBuildSuffixKey)) {
		return fmt.Errorf("not found [%s] or [%s] for bazel cmd", BazelCmdKey, BazelBuildKey)
	}

	return nil
}

func getNewBazelCmd(
	ldPreloadLibPath string, hookConfigPath string, envs map[string]string, cmd string, jobs string) (string, error) {
	actionEnvHome := fmt.Sprintf("%s=HOME", BazelActionEnvKey)
	actionEnvPreload := fmt.Sprintf("%s=%s=%s", BazelActionEnvKey, EnvKeyLDPreload, ldPreloadLibPath)
	actionEnvConfig := fmt.Sprintf("%s=%s=%s", BazelActionEnvKey, EnvKeyHookConfig, hookConfigPath)
	actionEnvDistcc := ""
	for k, v := range envs {
		if k == EnvKeyDistccHost {
			actionEnvDistcc = fmt.Sprintf("%s=%s=\"%s\"", BazelActionEnvKey, EnvKeyDistccHost, v)
			break
		}
	}
	// if actionEnvDistcc == "" {
	// 	return "", fmt.Errorf("not found [%s] in envs when getNewBazelCmd", EnvKeyDistccHost)
	// }
	bazelJobs := ""
	intjobs, err := strconv.Atoi(jobs)
	if err == nil && intjobs > 0 {
		bazelJobs = fmt.Sprintf("--jobs=%d", intjobs)
	}

	replaceStr := fmt.Sprintf(" %s %s %s %s %s %s ",
		BazelBuildKey, actionEnvHome, actionEnvPreload, actionEnvConfig, actionEnvDistcc, bazelJobs)
	if strings.HasSuffix(cmd, BazelBuildSuffixKey) {
		return strings.Replace(cmd, BazelBuildSuffixKey, replaceStr, -1), nil
	} else {
		return strings.Replace(cmd, BazelBuildKey, replaceStr, -1), nil
	}
}

func runCmd(fullcmd string) (int, error) {
	fmt.Printf("runCmd [%s]\n", fullcmd)
	cmd := exec.Command("/bin/bash", "-c", fullcmd)
	dir, _ := os.Getwd()
	cmd.Dir = dir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	err := cmd.Run()
	if err != nil {
		return getExitCode(err), err
	}
	return int(ErrorOk), nil
}

func getExitCode(err error) int {
	var code int
	if err != nil {
		exitErr, ok := err.(*exec.ExitError)
		if !ok {
			return code
		}

		status, ok := exitErr.Sys().(syscall.WaitStatus)
		if !ok {
			return code
		}

		code = status.ExitStatus()
	}
	return code
}

func checkParams(ldPreloadLibPath, hookConfigPath string) error {
	flag, err := fileExists(ldPreloadLibPath)
	if !flag {
		return err
	}

	flag, err = fileExists(hookConfigPath)
	if !flag {
		return err
	}

	return nil
}

func fileExists(filename string) (bool, error) {
	if _, err := os.Stat(filename); os.IsNotExist(err) {
		return false, fmt.Errorf("%s is not existed", filename)
	} else if err != nil {
		return false, err
	}
	return true, nil
}
