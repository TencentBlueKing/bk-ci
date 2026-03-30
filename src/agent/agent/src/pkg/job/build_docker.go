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
	"context"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/i18n"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/job_docker"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/upgrade/download"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
)

// buildDockerManager docker构建机构建对象管理
type buildDockerManager struct {
	// instances 正在执行中的构建对象 [string]*api.ThirdPartyDockerTaskInfo
	instances sync.Map
}

func (b *buildDockerManager) GetInstanceCount() int {
	var i = 0
	b.instances.Range(func(_, _ interface{}) bool {
		i++
		return true
	})
	return i
}

func (b *buildDockerManager) GetInstanceStr() string {
	var sb strings.Builder
	b.instances.Range(func(_, value interface{}) bool {
		v := *value.(*api.ThirdPartyDockerTaskInfo)
		sb.WriteString(fmt.Sprintf("%s[%s],", v.BuildId, v.VmSeqId))
		return true
	})
	return sb.String()
}

func (b *buildDockerManager) GetInstances() []api.ThirdPartyDockerTaskInfo {
	result := make([]api.ThirdPartyDockerTaskInfo, 0)
	b.instances.Range(func(_, value interface{}) bool {
		result = append(result, *value.(*api.ThirdPartyDockerTaskInfo))
		return true
	})
	return result
}

func (b *buildDockerManager) AddBuild(buildId string, info *api.ThirdPartyDockerTaskInfo) {
	b.instances.Store(buildId, info)
}

func (b *buildDockerManager) RemoveBuild(buildId string) {
	b.instances.Delete(buildId)
}

var GBuildDockerManager *buildDockerManager

func init() {
	GBuildDockerManager = new(buildDockerManager)
}

const (
	entryPointCmd = "/data/init.sh"
)

func runDockerBuild(buildInfo *api.ThirdPartyBuildInfo) {
	// 第一次使用docker构建机后，则直接开启docker构建机相关逻辑
	if !config.GAgentConfig.EnableDockerBuild {
		config.GAgentConfig.EnableDockerBuild = true
		go config.GAgentConfig.SaveConfig()
	}

	// 兼容旧数据，对于没有docker文件的需要重新下载，防止重复下载所以放到主流程做
	if _, err := os.Stat(config.GetDockerInitFilePath()); err != nil {
		if os.IsNotExist(err) {
			_, err = download.DownloadDockerInitFile(systemutil.GetWorkDir())
			if err != nil {
				GBuildDockerManager.RemoveBuild(buildInfo.BuildId)
				dockerBuildFinish(buildInfo.ToFinish(false, i18n.Localize("DownloadDockerInitScriptError", map[string]interface{}{"err": err}), api.DockerRunShInitErrorEnum))
				return
			}
		} else {
			GBuildDockerManager.RemoveBuild(buildInfo.BuildId)
			dockerBuildFinish(buildInfo.ToFinish(false, i18n.Localize("StatDockerInitScriptError", map[string]interface{}{"err": err}), api.DockerRunShStatErrorEnum))
			return
		}
	}

	// 每次执行前都校验并修改一次dockerfile权限，防止用户修改或者升级丢失权限
	if err := systemutil.Chmod(config.GetDockerInitFilePath(), os.ModePerm); err != nil {
		GBuildDockerManager.RemoveBuild(buildInfo.BuildId)
		dockerBuildFinish(buildInfo.ToFinish(false, i18n.Localize("CheckDockerInitScriptAuthError", map[string]interface{}{"err": err}), api.DockerChmodInitshErrorEnum))
		return
	}

	doDockerJob(buildInfo)
}

const longLogTag = "toolong"

// doDockerJob 使用docker启动构建
func doDockerJob(buildInfo *api.ThirdPartyBuildInfo) {
	defer func() {
		// 各种情况退出时减运行任务数量
		GBuildDockerManager.RemoveBuild(buildInfo.BuildId)
	}()

	workDir := systemutil.GetWorkDir()

	dockerBuildInfo := buildInfo.DockerBuildInfo
	if dockerBuildInfo.Credential.ErrMsg != "" {
		logs.Error("DOCKER_JOB|get docker cred error ", dockerBuildInfo.Credential.ErrMsg)
		dockerBuildFinish(buildInfo.ToFinish(false, i18n.Localize("GetDockerCertError", map[string]interface{}{"err": dockerBuildInfo.Credential.ErrMsg}), api.DockerCredGetErrorEnum))
		return
	}
	ctx := context.Background()
	runner := newBuildDockerRunner(buildInfo)

	imageName := strings.TrimSpace(dockerBuildInfo.Image)
	imageStr := strings.TrimPrefix(strings.TrimPrefix(imageName, "http://"), "https://")
	var err error

	imageStrSub := strings.Split(imageStr, ":")
	localExist := false
	isLatest := false
	// mirrors.tencent.com/ruotiantang/image-test:latest
	// 长度为2说明第二个就是tag
	if len(imageStrSub) == 2 && imageStrSub[1] == "latest" {
		isLatest = true
	} else if len(imageStr) == 1 {
		// 等于1说明没填tag按照docker的规则默认会去拉取最新的为 latest
		isLatest = true
	}

	if job_docker.NeedLocalImageInspect(isLatest, dockerBuildInfo.ImagePullPolicy) {
		localExist, err = runner.ImageExists(ctx, imageName)
		if err != nil {
			logs.WithError(err).Error("DOCKER_JOB|inspect docker image error")
			dockerBuildFinish(buildInfo.ToFinish(false, i18n.Localize("GetDockerImagesError", map[string]interface{}{"err": err}), api.DockerImagesFetchErrorEnum))
			return
		}
	}

	if job_docker.IfPullImage(localExist, isLatest, dockerBuildInfo.ImagePullPolicy) {
		if isLatest {
			postLog(false, i18n.Localize("PullLatest", nil), buildInfo, api.LogtypeLog)
		}
		postLog(false, i18n.Localize("StartPullImage", map[string]interface{}{"name": imageName}), buildInfo, api.LogtypeLog)
		postLog(false, i18n.Localize("FirstPullTips", nil), buildInfo, api.LogtypeLog)
		pullOut, err := runner.PullImage(ctx, imageName, dockerBuildInfo.Credential.User, dockerBuildInfo.Credential.Password)
		if err != nil {
			logs.WithError(err).Errorf("DOCKER_JOB|pull new image %s error", imageName)
			dockerBuildFinish(buildInfo.ToFinish(false, i18n.Localize("PullImageError", map[string]interface{}{"name": imageName, "err": err.Error()}), api.DockerImagePullErrorEnum))
			return
		}
		if strings.TrimSpace(pullOut) != "" {
			go postLog(false, pullOut, buildInfo, api.LogtypeLog)
		}
	} else {
		postLog(false, i18n.Localize("LocalExistImage", nil)+imageName, buildInfo, api.LogtypeLog)
	}

	// 创建docker构建机运行准备空间，拉取docker构建机初始化文件
	tmpDir := fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), job_docker.LocalDockerBuildTmpDirName)
	err = systemutil.MkDir(tmpDir)
	if err != nil {
		errMsg := i18n.Localize("CreateDockerTmpDirError", map[string]interface{}{"err": err.Error()})
		logs.Error("DOCKER_JOB|" + errMsg)
		dockerBuildFinish(buildInfo.ToFinish(false, errMsg, api.DockerMakeTmpDirErrorEnum))
		return
	}

	// 解析docker options
	createArgs, err := buildDockerCreateArgs(
		fmt.Sprintf("dispatch-%s-%s-%s", buildInfo.BuildId, buildInfo.VmSeqId, util.RandStringRunes(8)),
		imageStr,
		buildInfo,
	)
	if err != nil {
		logs.WithError(err).Error("DOCKER_JOB|")
		dockerBuildFinish(buildInfo.ToFinish(false, err.Error(), api.DockerDockerOptionsErrorEnum))
		return
	}

	containerName := createArgs[1]
	creatID, err := runner.CreateContainer(ctx, createArgs)
	if err != nil {
		logs.WithError(err).Errorf("DOCKER_JOB|create container %s error", containerName)
		dockerBuildFinish(buildInfo.ToFinish(
			false,
			i18n.Localize("CreateContainerError", map[string]interface{}{"name": containerName, "err": err.Error()}),
			api.DockerContainerCreateErrorEnum,
		))
		return
	}

	defer func() {
		if config.IsDebug {
			logs.Debug("debug no remove container")
			return
		}
		if err = runner.RemoveContainer(context.Background(), creatID); err != nil {
			logs.WithError(err).Errorf("DOCKER_JOB|remove container %s error", creatID)
		}
	}()

	// 启动容器
	if err := runner.StartContainer(ctx, creatID); err != nil {
		logs.WithError(err).Errorf("DOCKER_JOB|start container %s error", creatID)
		dockerBuildFinish(buildInfo.ToFinish(
			false,
			i18n.Localize("StartContainerError", map[string]interface{}{"name": containerName, "err": err.Error()}),
			api.DockerContainerStartErrorEnum,
		))
		return
	}

	statusCode, err := runner.WaitContainer(ctx, creatID)
	if err != nil {
		logs.WithError(err).Errorf("DOCKER_JOB|wait container %s over error ", creatID)
		dockerBuildFinish(buildInfo.ToFinish(
			false,
			i18n.Localize("WaitContainerError", map[string]interface{}{"name": containerName, "err": err.Error()}),
			api.DockerContainerRunErrorEnum,
		))
		return
	}
	if statusCode != 0 {
		logs.Warn(fmt.Sprintf("DOCKER_JOB|wait container %s over status not 0, exit code %d", creatID, statusCode))
		msg := ""
		logFile := func(tag string) (string, error) {
			logsDir := fmt.Sprintf("%s/%s/logs/%s/%s", workDir, job_docker.LocalDockerWorkSpaceDirName, buildInfo.BuildId, buildInfo.VmSeqId)
			logFile := filepath.Join(logsDir, "docker.log")
			content, err := os.ReadFile(logFile)
			if err != nil && os.IsNotExist(err) {
				return "", nil
			} else if err != nil {
				return "", err
			}
			if len(content) > 1000 {
				return tag, nil
			}
			return string(content), nil
		}
		content, err := logFile(longLogTag)
		if err != nil {
			msg = fmt.Sprintf("read log file error %s", err.Error())
		} else {
			msg = content
		}
		containerLog, err := runner.ContainerLogs(ctx, creatID)
		if err != nil {
			logs.Error("get container log error", err)
		}
		if msg == "" {
			msg = containerLog
		} else if strings.TrimSpace(containerLog) != "" {
			go postLog(false, i18n.Localize("DockerContainerLog", nil)+containerLog, buildInfo, api.LogtypeDebug)
		}
		if msg == longLogTag {
			msg = ""
		}
		dockerBuildFinish(buildInfo.ToFinish(
			false,
			i18n.Localize("ContainerExitCodeNotZero", map[string]interface{}{"name": containerName, "code": statusCode, "msg": msg}),
			api.DockerContainerRunErrorEnum,
		))
		return
	}

	dockerBuildFinish(buildInfo.ToFinish(true, "", api.NoErrorEnum))
}

// dockerBuildFinish docker构建结束相关
func dockerBuildFinish(buildInfo *api.ThirdPartyBuildWithStatus) {
	if buildInfo == nil {
		logs.Warn("DOCKER_JOB|buildInfo not exist")
		return
	}

	if buildInfo.Success {
		time.Sleep(8 * time.Second)
	}
	result, err := api.WorkerBuildFinish(buildInfo)
	if err != nil {
		logs.Error("DOCKER_JOB|send worker build finish failed: ", err.Error())
	}
	if result.IsNotOk() {
		logs.Error("DOCKER_JOB|send worker build finish failed: ", result.Message)
	}
	logs.Info("DOCKER_JOB|workerBuildFinish done")
}

// postLog 向后台上报日志
func postLog(red bool, message string, buildInfo *api.ThirdPartyBuildInfo, logType api.LogType) {
	taskId := "startVM-" + buildInfo.VmSeqId

	logMessage := &api.LogMessage{
		Message:      message,
		Timestamp:    time.Now().UnixMilli(),
		Tag:          taskId,
		JobId:        buildInfo.ContainerHashId,
		LogType:      logType,
		ExecuteCount: buildInfo.ExecuteCount,
		SubTag:       nil,
	}

	var err error
	if red {
		_, err = api.AddLogRedLine(buildInfo.BuildId, logMessage, buildInfo.VmSeqId)
	} else {
		_, err = api.AddLogLine(buildInfo.BuildId, logMessage, buildInfo.VmSeqId)
	}
	if err != nil {
		logs.Error("DOCKER_JOB|api post log error", err)
	}
}
