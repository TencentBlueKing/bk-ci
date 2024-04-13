//go:build windows
// +build windows

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/i18n"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
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
	// TODO: #10179 根据环境变量判断是否使用进程组
	var exitGroup *command.ProcessExitGroup = nil
	if true {
		exitGroup, err := command.NewProcessExitGroup()
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

	startCmd := config.GetJava()
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
	cmd, err := command.StartProcessCmd(startCmd, args, workDir, goEnv, runUser)
	if err != nil {
		errMsg := i18n.Localize("StartWorkerProcessFailed", map[string]interface{}{"err": err.Error()})
		logs.Error(errMsg)
		workerBuildFinish(buildInfo.ToFinish(false, errMsg, api.BuildProcessStartErrorEnum))
		return err
	}
	pid := cmd.Process.Pid

	if exitGroup != nil {
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
	logs.Infof("build[%s] pid[%d] finish, state=%v err=%v, msg=%s", buildInfo.BuildId, pid, cmd.ProcessState, err, msg)

	if err != nil {
		if len(msg) == 0 {
			msg = err.Error()
		}
	}
	success := true
	if len(msg) == 0 {
		msg = i18n.Localize("WorkerExit", map[string]interface{}{"pid": pid})
	} else {
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
