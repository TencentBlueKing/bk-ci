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
	"context"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/i18n"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/job_docker"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/upgrade/download"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/mount"
	"github.com/docker/docker/api/types/network"
	"github.com/docker/docker/client"
	"github.com/pkg/errors"
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
	if !systemutil.IsLinux() {
		GBuildDockerManager.RemoveBuild(buildInfo.BuildId)
		dockerBuildFinish(buildInfo.ToFinish(false, i18n.Localize("DockerOnlySupportLinux", nil), api.DockerOsErrorEnum))
		return
	}
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

	go doDockerJob(buildInfo)
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
	cli, err := client.NewClientWithOpts(client.FromEnv, client.WithAPIVersionNegotiation())
	if err != nil {
		logs.Error("DOCKER_JOB|create docker client error ", err)
		dockerBuildFinish(buildInfo.ToFinish(false, i18n.Localize("LinkDockerError", map[string]interface{}{"err": err}), api.DockerClientCreateErrorEnum))
		return
	}

	imageName := strings.TrimSpace(dockerBuildInfo.Image)

	// 判断本地是否已经有镜像了
	images, err := cli.ImageList(ctx, types.ImageListOptions{})
	if err != nil {
		logs.Error("DOCKER_JOB|list docker images error ", err)
		dockerBuildFinish(buildInfo.ToFinish(false, i18n.Localize("GetDockerImagesError", map[string]interface{}{"err": err}), api.DockerImagesFetchErrorEnum))
		return
	}
	localExist := false
	imageStr := strings.TrimPrefix(strings.TrimPrefix(imageName, "http://"), "https://")
	for _, image := range images {
		for _, tagName := range image.RepoTags {
			if tagName == imageStr {
				localExist = true
			}
		}
	}

	imageStrSub := strings.Split(imageStr, ":")
	isLatest := false
	// mirrors.tencent.com/ruotiantang/image-test:latest
	// 长度为2说明第二个就是tag
	if len(imageStrSub) == 2 && imageStrSub[1] == "latest" {
		isLatest = true
	} else if len(imageStr) == 1 {
		// 等于1说明没填tag按照docker的规则默认会去拉取最新的为 latest
		isLatest = true
	}

	if job_docker.IfPullImage(localExist, isLatest, dockerBuildInfo.ImagePullPolicy) {
		if isLatest {
			postLog(false, i18n.Localize("PullLatest", nil), buildInfo, api.LogtypeLog)
		}
		postLog(false, i18n.Localize("StartPullImage", map[string]interface{}{"name": imageName}), buildInfo, api.LogtypeLog)
		postLog(false, i18n.Localize("FirstPullTips", nil), buildInfo, api.LogtypeLog)

		auth, err := job_docker.GenerateDockerAuth(dockerBuildInfo.Credential.User, dockerBuildInfo.Credential.Password)
		if err != nil {
			logs.WithError(err).Errorf("DOCKER_JOB|pull new image generateDockerAuth %s error ", imageName)
			dockerBuildFinish(buildInfo.ToFinish(false, i18n.Localize("PullImageError", map[string]interface{}{"name": imageName, "err": err.Error()}), api.DockerImagePullErrorEnum))
			return
		}
		reader, err := cli.ImagePull(ctx, imageName, types.ImagePullOptions{
			RegistryAuth: auth,
		})
		if err != nil {
			logs.Error(fmt.Sprintf("DOCKER_JOB|pull new image %s error ", imageName), err)
			dockerBuildFinish(buildInfo.ToFinish(false, i18n.Localize("PullImageError", map[string]interface{}{"name": imageName, "err": err.Error()}), api.DockerImagePullErrorEnum))
			return
		}
		defer reader.Close()
		buf := new(strings.Builder)
		_, err = io.Copy(buf, reader)
		if err != nil {
			logs.Error("DOCKER_JOB|write image message error ", err)
			postLog(true, i18n.Localize("GetPullImageLogError", map[string]interface{}{"err": err.Error()}), buildInfo, api.LogtypeLog)
		} else {
			// 异步打印，防止过大卡住主流程
			go postLog(false, buf.String(), buildInfo, api.LogtypeLog)
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
	dockerConfig, err := job_docker.ParseDockeroptions(cli, dockerBuildInfo.Options)
	if err != nil {
		logs.Error("DOCKER_JOB|" + err.Error())
		dockerBuildFinish(buildInfo.ToFinish(false, err.Error(), api.DockerDockerOptionsErrorEnum))
		return
	}

	// 创建容器
	containerName := fmt.Sprintf("dispatch-%s-%s-%s", buildInfo.BuildId, buildInfo.VmSeqId, util.RandStringRunes(8))
	mounts, err := parseContainerMounts(buildInfo)
	if err != nil {
		errMsg := i18n.Localize("ReadDockerMountsError", map[string]interface{}{"err": err.Error()})
		logs.Error("DOCKER_JOB| ", err)
		dockerBuildFinish(buildInfo.ToFinish(false, errMsg, api.DockerMountCreateErrorEnum))
		return
	}

	var confg *container.Config
	var hostConfig *container.HostConfig
	var netConfig *network.NetworkingConfig
	if dockerConfig != nil {
		confg = dockerConfig.Config
		confg.Image = imageStr
		confg.Cmd = []string{}
		confg.Entrypoint = []string{"/bin/sh", "-c", entryPointCmd}
		confg.Env = parseContainerEnv(dockerBuildInfo)

		hostConfig = dockerConfig.HostConfig
		hostConfig.CapAdd = append(hostConfig.CapAdd, "SYS_PTRACE")
		hostConfig.Mounts = append(hostConfig.Mounts, mounts...)
		hostConfig.NetworkMode = container.NetworkMode("bridge")

		netConfig = dockerConfig.NetworkingConfig
	} else {
		confg = &container.Config{
			Image:      imageStr,
			Cmd:        []string{},
			Entrypoint: []string{"/bin/sh", "-c", entryPointCmd},
			Env:        parseContainerEnv(dockerBuildInfo),
		}

		hostConfig = &container.HostConfig{
			CapAdd:      []string{"SYS_PTRACE"},
			Mounts:      mounts,
			NetworkMode: container.NetworkMode("bridge"),
		}

		netConfig = nil
	}

	creatResp, err := cli.ContainerCreate(ctx, confg, hostConfig, netConfig, nil, containerName)
	if err != nil {
		logs.Error(fmt.Sprintf("DOCKER_JOB|create container %s error ", containerName), err)
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
		if err = cli.ContainerRemove(ctx, creatResp.ID, types.ContainerRemoveOptions{Force: true}); err != nil {
			logs.Error(fmt.Sprintf("DOCKER_JOB|remove container %s error ", creatResp.ID), err)
		}
	}()

	// 启动容器
	if err := cli.ContainerStart(ctx, creatResp.ID, types.ContainerStartOptions{}); err != nil {
		logs.Error(fmt.Sprintf("DOCKER_JOB|start container %s error ", creatResp.ID), err)
		dockerBuildFinish(buildInfo.ToFinish(
			false,
			i18n.Localize("StartContainerError", map[string]interface{}{"name": containerName, "err": err.Error()}),
			api.DockerContainerStartErrorEnum,
		))
		return
	}

	// 等待容器结束，处理错误信息并上报
	statusCh, errCh := cli.ContainerWait(ctx, creatResp.ID, container.WaitConditionNotRunning)
	select {
	case err := <-errCh:
		if err != nil {
			logs.Error(fmt.Sprintf("DOCKER_JOB|wait container %s over error ", creatResp.ID), err)
			dockerBuildFinish(buildInfo.ToFinish(
				false,
				i18n.Localize("WaitContainerError", map[string]interface{}{"name": containerName, "err": err.Error()}),
				api.DockerContainerRunErrorEnum,
			))
			return
		}
	case status := <-statusCh:
		if status.Error != nil {
			logs.Error(fmt.Sprintf("DOCKER_JOB|wait container %s over error ", creatResp.ID), status.Error)
			dockerBuildFinish(buildInfo.ToFinish(
				false,
				i18n.Localize("WaitContainerError", map[string]interface{}{"name": containerName, "err": status.Error.Message}),
				api.DockerContainerRunErrorEnum,
			))
			return
		} else {
			if status.StatusCode != 0 {
				logs.Warn(fmt.Sprintf("DOCKER_JOB|wait container %s over status not 0, exit code %d", creatResp.ID, status.StatusCode))
				msg := ""
				// 如果docker状态不为零，将日志上报
				logFile := func(tag string) (string, error) {
					logsDir := fmt.Sprintf("%s/%s/logs/%s/%s", workDir, job_docker.LocalDockerWorkSpaceDirName, buildInfo.BuildId, buildInfo.VmSeqId)
					logFile := filepath.Join(logsDir, "docker.log")
					content, err := os.ReadFile(logFile)
					if err != nil && os.IsNotExist(err) {
						return "", nil
					} else if err != nil {
						return "", err
					}
					// 超过字数说明肯定不是错误，不打印
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

				// 这里可能就是docker最开始执行时报错，拿一下docker log，如果原本有docker.log 则容器日志上传为debug日志，否则上传为结束日志
				containerLogB, err := cli.ContainerLogs(ctx, creatResp.ID, types.ContainerLogsOptions{
					ShowStdout: true,
					ShowStderr: true,
				})
				var containerLog = ""
				if err == nil {
					buf := new(strings.Builder)
					_, err := io.Copy(buf, containerLogB)
					if err != nil {
						logs.Error("copy container error", err)
						containerLog = ""
					} else {
						containerLog = buf.String()
					}
				} else {
					logs.Error("get container error", err)
				}

				if msg == "" {
					msg = containerLog
				} else {
					go postLog(false, i18n.Localize("DockerContainerLog", nil)+containerLog, buildInfo, api.LogtypeDebug)
				}

				if msg == longLogTag {
					msg = ""
				}

				dockerBuildFinish(
					buildInfo.ToFinish(
						false,
						i18n.Localize("ContainerExitCodeNotZero", map[string]interface{}{"name": containerName, "code": status.StatusCode, "msg": msg}),
						api.DockerContainerRunErrorEnum,
					))
				return
			}
		}
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

// parseContainerMounts 解析生成容器挂载内容
func parseContainerMounts(buildInfo *api.ThirdPartyBuildInfo) ([]mount.Mount, error) {
	var mounts []mount.Mount

	// 默认绑定本机的java用来执行worker，因为仅支持linux容器所以仅限linux构建机绑定
	if systemutil.IsLinux() {
		javaDir := config.GAgentConfig.JdkDirPath
		mounts = append(mounts, mount.Mount{
			Type:     mount.TypeBind,
			Source:   javaDir,
			Target:   "/usr/local/jre",
			ReadOnly: true,
		})
	}

	// 挂载docker构建机初始化脚本
	workDir := systemutil.GetWorkDir()
	mounts = append(mounts, mount.Mount{
		Type:     mount.TypeBind,
		Source:   config.GetDockerInitFilePath(),
		Target:   entryPointCmd,
		ReadOnly: true,
	})

	// 创建并挂载data和log
	// data目录优先选择用户自定的工作空间
	dataDir := ""
	if buildInfo.Workspace == "" {
		dataDir = fmt.Sprintf("%s/%s/data/%s/%s", workDir, job_docker.LocalDockerWorkSpaceDirName, buildInfo.PipelineId, buildInfo.VmSeqId)
	} else {
		dataDir = buildInfo.Workspace
	}
	err := systemutil.MkDir(dataDir)
	if err != nil && !os.IsExist(err) {
		return nil, errors.Wrapf(err, "create local data dir %s error", dataDir)
	}
	mounts = append(mounts, mount.Mount{
		Type:     mount.TypeBind,
		Source:   dataDir,
		Target:   job_docker.DockerDataDir,
		ReadOnly: false,
	})

	logsDir := fmt.Sprintf("%s/%s/logs/%s/%s", workDir, job_docker.LocalDockerWorkSpaceDirName, buildInfo.BuildId, buildInfo.VmSeqId)
	err = systemutil.MkDir(logsDir)
	if err != nil && !os.IsExist(err) {
		return nil, errors.Wrapf(err, "create local logs dir %s error", logsDir)
	}
	mounts = append(mounts, mount.Mount{
		Type:     mount.TypeBind,
		Source:   logsDir,
		Target:   job_docker.DockerLogDir,
		ReadOnly: false,
	})

	return mounts, nil
}

// parseContainerEnv 解析生成容器环境变量
func parseContainerEnv(dockerBuildInfo *api.ThirdPartyDockerBuildInfo) []string {
	var envs []string

	// 默认传入环境变量用来构建
	envs = append(envs, "devops_project_id="+config.GAgentConfig.ProjectId)
	envs = append(envs, "devops_agent_id="+dockerBuildInfo.AgentId)
	envs = append(envs, "devops_agent_secret_key="+dockerBuildInfo.SecretKey)
	envs = append(envs, "devops_gateway="+config.GetGateWay())
	// 通过环境变量区分agent docker
	envs = append(envs, "agent_build_env=DOCKER")

	return envs
}
