//go:build windows
// +build windows

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
	"syscall"

	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"
	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/i18n"
	ucommand "github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/process"
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
	// windows特有环境变量
	goEnv["DEVOPS_AGENT_WIN_SERVICE"] = config.GAgentEnv.WinTask
	var err error
	var exitGroup process.ProcessExitGroup
	enableExitGroup := config.FetchEnvAndCheck(constant.DevopsAgentEnableExitGroup, "true")
	if enableExitGroup {
		logs.Info("DEVOPS_AGENT_ENABLE_EXIT_GROUP enable")
		exitGroup, err = process.NewProcessExitGroup()
		if err != nil {
			errMsg := i18n.Localize("StartWorkerProcessFailed", map[string]interface{}{"err": err.Error()})
			logs.Error(errMsg)
			workerBuildFinish(buildInfo.ToFinish(false, errMsg, api.BuildProcessStartErrorEnum))
			return err
		}

		defer func() {
			logs.Infof("%s exit group dispose", buildInfo.BuildId)
			exitGroup.Dispose()
		}()
	}

	startCmd := third_components.GetJavaLatest()
	agentLogPrefix := fmt.Sprintf("%s_%s_agent", buildInfo.BuildId, buildInfo.VmSeqId)
	errorMsgFile := getWorkerErrorMsgFile(buildInfo.BuildId, buildInfo.VmSeqId)
	args := []string{
		"-Djava.io.tmpdir=" + tmpDir,
		"-Ddevops.agent.error.file=" + errorMsgFile,
		"-Dbuild.type=AGENT",
		"-DAGENT_LOG_PREFIX=" + agentLogPrefix,
		"-Xmx2g", // #5806 兼容性问题，必须独立一行
		"-jar",
		config.BuildAgentJarPath(),
		getEncodedBuildInfo(buildInfo)}
	cmd, err := StartProcessCmd(startCmd, args, workDir, goEnv, runUser)
	if err != nil {
		errMsg := i18n.Localize("StartWorkerProcessFailed", map[string]interface{}{"err": err.Error()})
		logs.Error(errMsg)
		workerBuildFinish(buildInfo.ToFinish(false, errMsg, api.BuildProcessStartErrorEnum))
		return err
	}
	pid := cmd.Process.Pid

	if enableExitGroup {
		logs.Infof("%s process %d add exit group ", buildInfo.BuildId, pid)
		if err := exitGroup.AddProcess(cmd.Process); err != nil {
			logs.Errorf("%s add process  to %d exit group error %s", buildInfo.BuildId, pid, err.Error())
		}
	}

	// 添加需要构建结束后删除的文件
	buildInfo.ToDelTmpFiles = []string{errorMsgFile}

	GBuildManager.AddBuild(pid, buildInfo)
	logs.Info(fmt.Sprintf("[%s]|Job#_%s|Build started, pid:%d ", buildInfo.BuildId, buildInfo.VmSeqId, pid))

	// #5806 预先录入异常信息，在构建进程正常结束时清理掉。如果没清理掉，则说明进程非正常退出，可能被OS或人为杀死
	_ = fileutil.WriteString(errorMsgFile, i18n.Localize("BuilderProcessWasKilled", nil))
	_ = systemutil.Chmod(errorMsgFile, os.ModePerm)

	err = cmd.Wait()
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

func StartProcessCmd(command string, args []string, workDir string, envMap map[string]string, runUser string) (*exec.Cmd, error) {
	cmd := exec.Command(command)

	if config.FetchEnvAndCheck(constant.DevopsAgentEnableNewConsole, "true") {
		cmd.SysProcAttr = &syscall.SysProcAttr{
			CreationFlags:    constant.WinCommandNewConsole,
			NoInheritHandles: true,
		}
		logs.Info("DEVOPS_AGENT_ENABLE_NEW_CONSOLE enabled")
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
