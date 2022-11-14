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
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io/fs"
	"io/ioutil"
	"os"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/agent/src/pkg/api"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/config"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/logs"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/command"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/httputil"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/systemutil"
)

const buildIntervalInSeconds = 5

// AgentStartup 上报构建机启动
func AgentStartup() (agentStatus string, err error) {
	result, err := api.AgentStartup()
	return parseAgentStatusResult(result, err)
}

// getAgentStatus 获取构建机状态
func getAgentStatus() (agentStatus string, err error) {
	result, err := api.GetAgentStatus()
	return parseAgentStatusResult(result, err)
}

// parseAgentStatusResult 解析状态信息
func parseAgentStatusResult(result *httputil.DevopsResult, resultErr error) (agentStatus string, err error) {
	if resultErr != nil {
		logs.Error("parse agent status error: ", resultErr.Error())
		return "", errors.New("parse agent status error")
	}
	if result.IsNotOk() {
		logs.Error("parse agent status failed: ", result.Message)
		return "", errors.New("parse agent status failed")
	}

	agentStatus, ok := result.Data.(string)
	if !ok || result.Data == "" {
		logs.Error("parse agent status error")
		return "", errors.New("parse agent status error")
	}
	return agentStatus, nil
}

// DoPollAndBuild 获取构建，如果达到最大并发或者是处于升级中，则不执行
func DoPollAndBuild() {
	for {
		time.Sleep(buildIntervalInSeconds * time.Second)
		agentStatus, err := getAgentStatus()
		if err != nil {
			logs.Warn("get agent status err: ", err.Error())
			continue
		}
		if agentStatus != config.AgentStatusImportOk {
			logs.Error("agent is not ready for build, agent status: " + agentStatus)
			continue
		}

		instanceCount := GBuildManager.GetInstanceCount()
		if config.GAgentConfig.ParallelTaskCount != 0 && instanceCount >= config.GAgentConfig.ParallelTaskCount {
			logs.Info(fmt.Sprintf("parallel task count exceed , wait job done, "+
				"ParallelTaskCount config: %d, instance count: %d",
				config.GAgentConfig.ParallelTaskCount, instanceCount))
			continue
		}

		// 在接取任务先获取锁，防止与其他任务产生干扰
		GBuildManager.Lock.Lock()

		buildInfo, err := getBuild()
		if err != nil {
			logs.Error("get build failed, retry, err", err.Error())
			GBuildManager.Lock.Unlock()
			continue
		}

		if buildInfo == nil {
			logs.Info("no build to run, skip")
			GBuildManager.Lock.Unlock()
			continue
		}

		// 接取任务之后解锁
		GBuildManager.AddPreInstance(buildInfo.BuildId)
		GBuildManager.Lock.Unlock()

		err = runBuild(buildInfo)
		if err != nil {
			logs.Error("start build failed: ", err.Error())
		}
	}
}

// getBuild 从服务器认领要构建的信息
func getBuild() (*api.ThirdPartyBuildInfo, error) {
	logs.Info("get build")
	result, err := api.GetBuild()
	if err != nil {
		return nil, err
	}

	if result.IsNotOk() {
		logs.Error("get build info failed, message", result.Message)
		return nil, errors.New("get build info failed")
	}

	if result.Data == nil {
		return nil, nil
	}

	buildInfo := new(api.ThirdPartyBuildInfo)
	err = util.ParseJsonToData(result.Data, buildInfo)
	if err != nil {
		return nil, err
	}

	return buildInfo, nil
}

// runBuild 启动构建
func runBuild(buildInfo *api.ThirdPartyBuildInfo) error {

	workDir := systemutil.GetWorkDir()
	agentJarPath := config.BuildAgentJarPath()
	if !fileutil.Exists(agentJarPath) {
		// #5806 尝试自愈
		upgradeWorkerFile := systemutil.GetUpgradeDir() + "/" + config.WorkAgentFile

		if fileutil.Exists(upgradeWorkerFile) {

			_, err := fileutil.CopyFile(upgradeWorkerFile, agentJarPath, true)
			upgradeWorkerFileVersion := config.DetectWorkerVersion()
			if err != nil || !strings.HasPrefix(upgradeWorkerFileVersion, "v") {
				// #5806 宽松判断合法的版本v开头
				errorMsg := fmt.Sprintf(
					"\n尝试恢复 [%s] 执行文件失败，请到 [%s] 目录下执行 install.sh 或解压 agent.zip 还原安装目录"+
						"\nRestore %s failed, `run install.sh` or `unzip agent.zip` in %s.",
					agentJarPath, workDir, agentJarPath, workDir)
				logs.Error(errorMsg)
				workerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: errorMsg})
			} else { // #5806 替换后修正版本号
				if config.GAgentEnv.SlaveVersion != upgradeWorkerFileVersion {
					config.GAgentEnv.SlaveVersion = upgradeWorkerFileVersion
				}
			}
		} else {
			errorMsg := fmt.Sprintf(
				"\n%s执行文件丢失，请到%s目录下执行 install.sh 或者重新解压 agent.zip 还原安装目录"+
					"\nMissing %s, `run install.sh` or `unzip agent.zip` in %s.",
				agentJarPath, workDir, agentJarPath, workDir)
			logs.Error(errorMsg)
			workerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: errorMsg})
		}
	}

	runUser := config.GAgentConfig.SlaveUser

	goEnv := map[string]string{
		"DEVOPS_AGENT_VERSION":  config.AgentVersion,
		"DEVOPS_WORKER_VERSION": config.GAgentEnv.SlaveVersion,
		"DEVOPS_PROJECT_ID":     buildInfo.ProjectId,
		"DEVOPS_BUILD_ID":       buildInfo.BuildId,
		"DEVOPS_VM_SEQ_ID":      buildInfo.VmSeqId,
		"DEVOPS_SLAVE_VERSION":  config.GAgentEnv.SlaveVersion, //deprecated
		"PROJECT_ID":            buildInfo.ProjectId,           //deprecated
		"BUILD_ID":              buildInfo.BuildId,             //deprecated
		"VM_SEQ_ID":             buildInfo.VmSeqId,             //deprecated

	}
	if config.GEnvVars != nil {
		for k, v := range config.GEnvVars {
			goEnv[k] = v
		}
	}
	// #5806 定义临时目录
	tmpDir, tmpMkErr := systemutil.MkBuildTmpDir()
	if tmpMkErr != nil {
		errMsg := fmt.Sprintf("创建临时目录失败(create tmp directory failed): %s", tmpMkErr.Error())
		logs.Error(errMsg)
		workerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: errMsg})
		return tmpMkErr
	}
	if systemutil.IsWindows() {
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
		pid, err := command.StartProcess(startCmd, args, workDir, goEnv, runUser)
		if err != nil {
			errMsg := "start worker process failed: " + err.Error()
			logs.Error(errMsg)
			workerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: errMsg})
			return err
		}
		// 添加需要构建结束后删除的文件
		buildInfo.ToDelTmpFiles = []string{errorMsgFile}

		GBuildManager.AddBuild(pid, buildInfo)
		logs.Info(fmt.Sprintf("[%s]|Job#_%s|Build started, pid:%d ", buildInfo.BuildId, buildInfo.VmSeqId, pid))
		return nil
	} else {
		startScriptFile, err := writeStartBuildAgentScript(buildInfo, tmpDir)
		if err != nil {
			errMsg := "准备构建脚本生成失败(create start script failed): " + err.Error()
			logs.Error(errMsg)
			workerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: errMsg})
			return err
		}
		pid, err := command.StartProcess(startScriptFile, []string{}, workDir, goEnv, runUser)
		if err != nil {
			errMsg := "启动构建进程失败(start worker process failed): " + err.Error()
			logs.Error(errMsg)
			workerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: errMsg})
			return err
		}
		GBuildManager.AddBuild(pid, buildInfo)
		logs.Info(fmt.Sprintf("[%s]|Job#_%s|Build started, pid:%d ", buildInfo.BuildId, buildInfo.VmSeqId, pid))
	}
	return nil
}

func getEncodedBuildInfo(buildInfo *api.ThirdPartyBuildInfo) string {
	strBuildInfo, _ := json.Marshal(buildInfo)
	logs.Info("buildInfo: ", string(strBuildInfo))
	codedBuildInfo := base64.StdEncoding.EncodeToString(strBuildInfo)
	logs.Info("base64: ", codedBuildInfo)
	return codedBuildInfo
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
			config.GetJava(), scriptFile, prepareScriptFile,
			errorMsgFile,
			agentLogPrefix, tmpDir, config.BuildAgentJarPath(), getEncodedBuildInfo(buildInfo)),
	}
	scriptContent := strings.Join(lines, "\n")

	err := ioutil.WriteFile(scriptFile, []byte(scriptContent), os.ModePerm)
	defer func() {
		_ = systemutil.Chmod(scriptFile, os.ModePerm)
		_ = systemutil.Chmod(prepareScriptFile, os.ModePerm)
	}()
	if err != nil {
		return "", err
	} else {
		prepareScriptContent := strings.Join(getShellLines(scriptFile), "\n")
		err := ioutil.WriteFile(prepareScriptFile, []byte(prepareScriptContent), os.ModePerm)
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

func workerBuildFinish(buildInfo *api.ThirdPartyBuildWithStatus) {
	if buildInfo == nil {
		logs.Warn("buildInfo not exist")
		return
	}

	// #5806 防止意外情况没有清理生成的脚本
	// 清理构建过程生成的文件
	if buildInfo.ToDelTmpFiles != nil {
		for _, filePath := range buildInfo.ToDelTmpFiles {
			e := fileutil.TryRemoveFile(filePath)
			logs.Info(fmt.Sprintf("build[%s] finish, delete:%s, err:%s", buildInfo.BuildId, filePath, e))
		}
	}

	// #7453 Agent build_tmp目录清理
	go checkAndDeleteBuildTmpFile()

	if buildInfo.Success {
		time.Sleep(8 * time.Second)
	}
	result, err := api.WorkerBuildFinish(buildInfo)
	if err != nil {
		logs.Error("send worker build finish failed: ", err.Error())
	}
	if result.IsNotOk() {
		logs.Error("send worker build finish failed: ", result.Message)
	}
	logs.Info("workerBuildFinish done")
}

// checkAndDeleteBuildTmpFile 删除可能因为进程中断导致的没有被删除的构建过程临时文件
// Job最长运行时间为7天，所以这里通过检查超过7天最后修改时间的文件
func checkAndDeleteBuildTmpFile() {
	// win只用检查build_tmp目录
	workDir := systemutil.GetWorkDir()
	dir := workDir + "/build_tmp"
	fss, err := os.ReadDir(dir)
	if err != nil {
		logs.Error("checkAndDeleteBuildTmpFile|read build_tmp dir error ", err)
		return
	}
	for _, f := range fss {
		if f.IsDir() {
			continue
		}
		// build_tmp 目录下的文件超过7天都清除掉
		removeFileThan7Days(dir, f)
	}

	// darwin和linux还有prepare 和start文件
	if !systemutil.IsWindows() {
		fss, err = os.ReadDir(workDir)
		if err != nil {
			logs.Error("checkAndDeleteBuildTmpFile|read worker dir error ", err)
			return
		}
		for _, f := range fss {
			if f.IsDir() {
				continue
			}
			if !(strings.HasPrefix(f.Name(), startScriptFilePrefix) && strings.HasSuffix(f.Name(), startScriptFileSuffix)) &&
				!(strings.HasPrefix(f.Name(), prepareStartScriptFilePrefix) && strings.HasSuffix(f.Name(), prepareStartScriptFileSuffix)) {
				continue
			}
			removeFileThan7Days(workDir, f)
		}
	}
}

func removeFileThan7Days(dir string, f fs.DirEntry) {
	info, err := f.Info()
	if err != nil {
		logs.Error("removeFileThan7Days|read file info error ", "file: ", f.Name(), " error: ", err)
	}
	if (time.Now().Sub(info.ModTime())) > 7*24*time.Hour {
		err = os.Remove(dir + "/" + f.Name())
		if err != nil {
			logs.Error("removeFileThan7Days|remove file error ", "file: ", f.Name(), " error: ", err)
		}
	}
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
