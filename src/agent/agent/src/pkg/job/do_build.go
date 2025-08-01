//go:build linux || darwin
// +build linux darwin

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package job

import (
	"fmt"
	"os"
	"os/exec"
	"runtime"
	"strings"
	"syscall"

	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"
	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	exitcode "github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/i18n"
	ucommand "github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"
)

func doBuild(
	buildInfo *api.ThirdPartyBuildInfo,
	tmpDir string,
	workDir string,
	goEnv map[string]string,
	runUser string,
) error {
	startScriptFile, err := writeStartBuildAgentScript(buildInfo, tmpDir)
	if err != nil {
		errMsg := i18n.Localize("CreateStartScriptFailed", map[string]interface{}{"err": err.Error()})
		logs.Error(errMsg)
		workerBuildFinish(buildInfo.ToFinish(false, errMsg, api.PrepareScriptCreateErrorEnum))
		return err
	}

	enableExitGroup := config.FetchEnvAndCheck(constant.DevopsAgentEnableExitGroup, "true") ||
		(systemutil.IsMacos() && runtime.GOARCH == "arm64")
	if enableExitGroup {
		logs.Infof("%s enable exit group", buildInfo.BuildId)
	}
	cmd, err := StartProcessCmd(startScriptFile, []string{}, workDir, goEnv, runUser, enableExitGroup)
	if err != nil {
		errMsg := i18n.Localize("StartWorkerProcessFailed", map[string]interface{}{"err": err.Error()})
		logs.Error(errMsg)
		workerBuildFinish(buildInfo.ToFinish(false, errMsg, api.BuildProcessStartErrorEnum))
		return err
	}

	pid := cmd.Process.Pid
	GBuildManager.AddBuild(pid, buildInfo)
	logs.Info(fmt.Sprintf("[%s]|Job#_%s|Build started, pid:%d ", buildInfo.BuildId, buildInfo.VmSeqId, pid))

	// #5806 预先录入异常信息，在构建进程正常结束时清理掉。如果没清理掉，则说明进程非正常退出，可能被OS或人为杀死
	errorMsgFile := getWorkerErrorMsgFile(buildInfo.BuildId, buildInfo.VmSeqId)
	_ = fileutil.WriteString(errorMsgFile, i18n.Localize("BuilderProcessWasKilled", nil))
	_ = systemutil.Chmod(errorMsgFile, os.ModePerm)

	if enableExitGroup {
		pgId, errPg := syscall.Getpgid(pid)
		if errPg != nil {
			logs.Errorf("%s %d get pgid error %s", buildInfo.BuildId, pid, errPg.Error())
		}
		err = cmd.Wait()
		if errPg == nil {
			go func() {
				logs.Infof("%s do kill %d process group %d", buildInfo.BuildId, pid, pgId)
				// 杀死进程组
				errPg = syscall.Kill(-pgId, syscall.SIGKILL)
				if errPg != nil {
					logs.Errorf("%s failed to kill %d process group %d : %s", buildInfo.BuildId, pid, pgId, errPg.Error())
					return
				}
			}()
		}
	} else {
		err = cmd.Wait()
	}
	// #5806 从b-xxxx_build_msg.log 读取错误信息，此信息可由worker-agent.jar写入，用于当异常时能够将信息上报给服务器
	msgFile := getWorkerErrorMsgFile(buildInfo.BuildId, buildInfo.VmSeqId)
	msg, _ := fileutil.GetString(msgFile)
	if err != nil {
		logs.Errorf("build[%s] pid[%d] finish, state=%v err=%v, msg=%s", buildInfo.BuildId, pid, cmd.ProcessState, err, msg)
	} else {
		logs.Infof("build[%s] pid[%d] finish, state=%v err=%v, msg=%s", buildInfo.BuildId, pid, cmd.ProcessState, err, msg)
	}

	// #10362 Worker杀掉当前进程父进程导致Agent误报
	// agent 改动后可能会导致业务执行完成但是进程被杀掉导致流水线错误，所以将错误只是作为额外信息添加
	cmdErrMsg := ""
	if err != nil {
		cmdErrMsg = "|" + err.Error()
	}

	success := true
	if len(msg) == 0 {
		msg = i18n.Localize("WorkerExit", map[string]interface{}{"pid": pid}) + cmdErrMsg
	} else {
		msg += cmdErrMsg
		success = false
	}

	GBuildManager.DeleteBuild(pid)
	if success {
		workerBuildFinish(buildInfo.ToFinish(success, msg, api.NoErrorEnum))
	} else {
		workerBuildFinish(buildInfo.ToFinish(success, msg, api.BuildProcessRunErrorEnum))
	}

	return nil
}

func writeStartBuildAgentScript(buildInfo *api.ThirdPartyBuildInfo, tmpDir string) (string, error) {
	logs.Info("write start build agent script to file")
	// 套娃，多加一层脚本，使用exec新起进程，这样才会读取 .bash_profile
	prepareScriptFile := fmt.Sprintf(
		"%s/devops_agent_prepare_start_%s_%s_%s.sh",
		systemutil.GetWorkDir(), buildInfo.ProjectId, buildInfo.BuildId, buildInfo.VmSeqId)
	scriptFile := fmt.Sprintf(
		"%s/devops_agent_start_%s_%s_%s.sh",
		systemutil.GetWorkDir(), buildInfo.ProjectId, buildInfo.BuildId, buildInfo.VmSeqId)

	errorMsgFile := getWorkerErrorMsgFile(buildInfo.BuildId, buildInfo.VmSeqId)
	buildInfo.ToDelTmpFiles = []string{
		scriptFile, prepareScriptFile, errorMsgFile,
	}

	logs.Info("start agent script: ", scriptFile)
	agentLogPrefix := fmt.Sprintf("%s_%s_agent", buildInfo.BuildId, buildInfo.VmSeqId)
	lines := []string{
		"#!" + getCurrentShell(),
		fmt.Sprintf("cd %s", systemutil.GetWorkDir()),
		fmt.Sprintf("%s -Ddevops.slave.agent.start.file=%s -Ddevops.slave.agent.prepare.start.file=%s "+
			"-Ddevops.agent.error.file=%s "+
			"-Dbuild.type=AGENT -DAGENT_LOG_PREFIX=%s -Xmx2g -Djava.io.tmpdir=%s -jar %s %s",
			third_components.GetJavaLatest(), scriptFile, prepareScriptFile,
			errorMsgFile,
			agentLogPrefix, tmpDir, config.BuildAgentJarPath(), getEncodedBuildInfo(buildInfo)),
	}
	scriptContent := strings.Join(lines, "\n")

	err := exitcode.WriteFileWithCheck(scriptFile, []byte(scriptContent), os.ModePerm)
	defer func() {
		_ = systemutil.Chmod(scriptFile, os.ModePerm)
		_ = systemutil.Chmod(prepareScriptFile, os.ModePerm)
	}()
	if err != nil {
		return "", err
	} else {
		prepareScriptContent := strings.Join(getShellLines(scriptFile), "\n")
		err := exitcode.WriteFileWithCheck(prepareScriptFile, []byte(prepareScriptContent), os.ModePerm)
		if err != nil {
			return "", err
		} else {
			return prepareScriptFile, nil
		}
	}
}

// getShellLines 根据不同的shell的参数要求，这里可能需要不同的参数或者参数顺序
func getShellLines(scriptFile string) (newLines []string) {
	shell := getCurrentShell()
	switch shell {
	case "/bin/tcsh":
		newLines = []string{
			"#!" + shell,
			"exec " + shell + " " + scriptFile + " -l",
		}
	default:
		newLines = []string{
			"#!" + shell,
			"exec " + shell + " -l " + scriptFile,
		}
	}
	return newLines
}

func getCurrentShell() (shell string) {
	if config.GAgentConfig.DetectShell {
		shell = os.Getenv("SHELL")
		if strings.TrimSpace(shell) == "" {
			shell = "/bin/bash"
		}
	} else {
		shell = "/bin/bash"
	}
	logs.Info("current shell: ", shell)
	return
}

func StartProcessCmd(
	command string,
	args []string,
	workDir string,
	envMap map[string]string,
	runUser string,
	enableExitGroup bool,
) (*exec.Cmd, error) {
	cmd := exec.Command(command)

	// arm64机器目前无法通过worker杀进程
	if enableExitGroup {
		cmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true}
	}

	if len(args) > 0 {
		cmd.Args = append(cmd.Args, args...)
	}

	if workDir != "" {
		cmd.Dir = workDir
	}

	cmd.Env = os.Environ()
	if envMap != nil {
		for k, v := range envMap {
			cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", k, v))
		}
	}

	err := ucommand.SetUser(cmd, runUser)
	if err != nil {
		logs.Error("set user failed: ", err.Error())
		return nil, errors.Wrap(err, "Please check [devops.slave.user] in the {agent_dir}/.agent.properties")
	}

	logs.Info("cmd.Path: ", cmd.Path)
	logs.Info("cmd.Args: ", cmd.Args)
	logs.Info("cmd.workDir: ", cmd.Dir)
	logs.Info("runUser: ", runUser)

	err = cmd.Start()
	if err != nil {
		return nil, err
	}

	return cmd, nil
}
