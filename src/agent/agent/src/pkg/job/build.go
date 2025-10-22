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
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io/fs"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"
	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	exitcode "github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/i18n"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/httputil"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"
)

type BuildTotalManagerType struct {
	// Lock 多协程修改时的执行锁，这个锁主要用来判断当前是否还有任务，所以添加了任务就可以解锁了
	Lock sync.Mutex
}

var BuildTotalManager *BuildTotalManagerType

func init() {
	BuildTotalManager = new(BuildTotalManagerType)
}

// AgentStartup 上报构建机启动
func AgentStartup() (agentStatus string, err error) {
	result, err := api.AgentStartup()
	return parseAgentStatusResult(result, err)
}

// parseAgentStatusResult 解析状态信息
func parseAgentStatusResult(result *httputil.DevopsResult, resultErr error) (agentStatus string, err error) {
	if resultErr != nil {
		logs.WithErrorNoStack(resultErr).Error("parse agent status error")
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

// DoBuild 获取构建，如果达到最大并发或者是处于升级中，则不执行
func DoBuild(buildInfo *api.ThirdPartyBuildInfo) {
	if buildInfo == nil {
		logs.Warn("buildinfo is nil")
		return
	}

	// 在执行任务先获取锁，防止与其他操作产生干扰
	BuildTotalManager.Lock.Lock()

	// 拿到锁后再判断一次当前是否可以执行任务，防止出现并发问题
	dockerCanRun, normalCanRun := CheckParallelTaskCount()

	if buildInfo.DockerBuildInfo != nil && dockerCanRun {
		// 接取job任务之后才可以解除总任务锁解锁
		GBuildDockerManager.AddBuild(buildInfo.BuildId, &api.ThirdPartyDockerTaskInfo{
			ProjectId: buildInfo.ProjectId,
			BuildId:   buildInfo.BuildId,
			VmSeqId:   buildInfo.VmSeqId,
		})
		BuildTotalManager.Lock.Unlock()

		// 接取任务后判断国际化是否需要切换语言
		i18n.CheckLocalizer()

		runDockerBuild(buildInfo)
		return
	}

	if !normalCanRun {
		BuildTotalManager.Lock.Unlock()
		return
	}

	// 接取任务之后解锁
	GBuildManager.AddPreInstance(buildInfo.BuildId)
	BuildTotalManager.Lock.Unlock()

	// 接取任务后判断国际化是否需要切换语言
	i18n.CheckLocalizer()

	err := runBuild(buildInfo)
	if err != nil {
		logs.WithError(err).Error("start build failed")
	}
}

// CheckParallelTaskCount checkParallelTaskCount 检查当前运行的最大任务数
func CheckParallelTaskCount() (dockerCanRun bool, normalCanRun bool) {
	// 检查docker任务
	dockerInstanceCount := GBuildDockerManager.GetInstanceCount()
	if !systemutil.IsLinux() {
		dockerCanRun = false
	} else if config.GAgentConfig.DockerParallelTaskCount != 0 &&
		dockerInstanceCount >= config.GAgentConfig.DockerParallelTaskCount {
		dockerCanRun = false
	} else {
		dockerCanRun = true
	}

	// 检查普通任务
	instanceCount := GBuildManager.GetInstanceCount()
	if config.GAgentConfig.ParallelTaskCount != 0 && instanceCount >= config.GAgentConfig.ParallelTaskCount {
		normalCanRun = false
	} else {
		normalCanRun = true
	}

	return dockerCanRun, normalCanRun
}

// runBuild 启动构建
func runBuild(buildInfo *api.ThirdPartyBuildInfo) error {
	defer func() {
		// 防止因为某种场景无法进入构建时也要删除预构建任务，防止产生干扰
		GBuildManager.DeletePreInstance(buildInfo.BuildId)
	}()

	workDir := systemutil.GetWorkDir()
	agentJarPath := config.BuildAgentJarPath()
	if !fileutil.Exists(agentJarPath) {
		// #5806 尝试自愈
		upgradeWorkerFile := systemutil.GetUpgradeDir() + "/" + config.WorkAgentFile

		if fileutil.Exists(upgradeWorkerFile) {
			_, err := fileutil.CopyFile(upgradeWorkerFile, agentJarPath, true)
			upgradeWorkerFileVersion := third_components.Worker.DetectWorkerVersion()
			if err != nil || !strings.HasPrefix(upgradeWorkerFileVersion, "v") {
				// #5806 宽松判断合法的版本v开头
				errorMsg := i18n.Localize("AttemptToRestoreFailed", map[string]interface{}{"filename": agentJarPath, "dir": workDir})
				logs.Error(errorMsg)
				workerBuildFinish(buildInfo.ToFinish(false, errorMsg, api.RecoverRunFileErrorEnum))
			} else { // #5806 替换后修正版本号
				if third_components.Worker.GetVersion() != upgradeWorkerFileVersion {
					third_components.Worker.SetVersion(upgradeWorkerFileVersion)
				}
			}
		} else {
			errorMsg := i18n.Localize("ExecutableFileMissing", map[string]interface{}{"filename": agentJarPath, "dir": workDir})
			logs.Error(errorMsg)
			workerBuildFinish(buildInfo.ToFinish(false, errorMsg, api.LoseRunFileErrorEnum))

			// 丢失 worker 添加退出码
			exitcode.AddExitError(exitcode.ExitNotWorker, errorMsg)
		}
	}

	runUser := config.GAgentConfig.SlaveUser

	goEnv := map[string]string{
		"DEVOPS_AGENT_VERSION":     config.AgentVersion,
		"DEVOPS_WORKER_VERSION":    third_components.Worker.GetVersion(),
		"DEVOPS_PROJECT_ID":        buildInfo.ProjectId,
		"DEVOPS_BUILD_ID":          buildInfo.BuildId,
		"DEVOPS_VM_SEQ_ID":         buildInfo.VmSeqId,
		"DEVOPS_SLAVE_VERSION":     third_components.Worker.GetVersion(), //deprecated
		"PROJECT_ID":               buildInfo.ProjectId,                  //deprecated
		"BUILD_ID":                 buildInfo.BuildId,                    //deprecated
		"VM_SEQ_ID":                buildInfo.VmSeqId,                    //deprecated
		"DEVOPS_FILE_GATEWAY":      config.GAgentConfig.FileGateway,
		"DEVOPS_GATEWAY":           config.GetGateWay(),
		"BK_CI_LOCALE_LANGUAGE":    config.GAgentConfig.Language,
		"DEVOPS_AGENT_JDK_8_PATH":  third_components.Jdk.Jdk8.GetJavaOrNull(),
		"DEVOPS_AGENT_JDK_17_PATH": third_components.Jdk.Jdk17.GetJavaOrNull(),
	}
	if config.GApiEnvVars != nil {
		config.GApiEnvVars.RangeDo(func(k, v string) bool {
			goEnv[k] = v
			return true
		})
	}
	// #5806 定义临时目录
	tmpDir, tmpMkErr := systemutil.MkBuildTmpDir()
	if tmpMkErr != nil {
		errMsg := i18n.Localize("CreateTmpDirectoryFailed", map[string]interface{}{"err": tmpMkErr.Error()})
		logs.Error(errMsg)
		workerBuildFinish(buildInfo.ToFinish(false, errMsg, api.MakeTmpDirErrorEnum))
		return tmpMkErr
	}

	if err := doBuild(buildInfo, tmpDir, workDir, goEnv, runUser); err != nil {
		return err
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
		logs.WithErrorNoStack(err).Error("send worker build finish failed")
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
		return
	}
	if (time.Since(info.ModTime())) > 7*24*time.Hour {
		err = os.Remove(dir + "/" + f.Name())
		if err != nil {
			logs.Error("removeFileThan7Days|remove file error ", "file: ", f.Name(), " error: ", err)
		}
	}
}

const (
	errorMsgFileSuffix           = "build_msg.log"
	prepareStartScriptFilePrefix = "devops_agent_prepare_start"
	prepareStartScriptFileSuffix = ".sh"
	startScriptFilePrefix        = "devops_agent_start"
	startScriptFileSuffix        = ".sh"
)

// getWorkerErrorMsgFile 获取worker执行错误信息的日志文件
func getWorkerErrorMsgFile(buildId, vmSeqId string) string {
	return fmt.Sprintf("%s/build_tmp/%s_%s_%s",
		systemutil.GetWorkDir(), buildId, vmSeqId, errorMsgFileSuffix)
}

// getUnixWorkerPrepareStartScriptFile 获取unix系统，主要是darwin和linux的prepare start script文件
func getUnixWorkerPrepareStartScriptFile(projectId, buildId, vmSeqId string) string {
	return fmt.Sprintf("%s/%s_%s_%s_%s%s",
		systemutil.GetWorkDir(), prepareStartScriptFilePrefix, projectId, buildId, vmSeqId, prepareStartScriptFileSuffix)
}

// getUnixWorkerStartScriptFile 获取unix系统，主要是darwin和linux的prepare start script文件
func getUnixWorkerStartScriptFile(projectId, buildId, vmSeqId string) string {
	return fmt.Sprintf("%s/%s_%s_%s_%s%s",
		systemutil.GetWorkDir(), startScriptFilePrefix, projectId, buildId, vmSeqId, startScriptFileSuffix)
}

// CheckRunningJob 校验当前是否有正在跑的任务
func CheckRunningJob() bool {
	if GBuildManager.GetPreInstancesCount() > 0 ||
		GBuildManager.GetInstanceCount() > 0 ||
		GBuildDockerManager.GetInstanceCount() > 0 {
		return true
	}
	return false
}
