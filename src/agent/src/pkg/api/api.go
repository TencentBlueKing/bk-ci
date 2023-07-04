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

package api

import (
	"fmt"
	"runtime"
	"strconv"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/httputil"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

func buildUrl(url string) string {
	return config.GetGateWay() + url
}

func Heartbeat(
	buildInfos []ThirdPartyBuildInfo,
	jdkVersion []string,
	dockerTaskList []ThirdPartyDockerTaskInfo,
	dockerInitFileMd5 DockerInitFileInfo,
) (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/environment/api/buildAgent/agent/thirdPartyAgent/agents/newHeartbeat")

	var taskList []ThirdPartyTaskInfo
	for _, info := range buildInfos {
		taskList = append(taskList, ThirdPartyTaskInfo{
			ProjectId: info.ProjectId,
			BuildId:   info.BuildId,
			VmSeqId:   info.VmSeqId,
			Workspace: info.Workspace,
		})
	}
	agentHeartbeatInfo := &AgentHeartbeatInfo{
		MasterVersion:     config.AgentVersion,
		SlaveVersion:      config.GAgentEnv.SlaveVersion,
		HostName:          config.GAgentEnv.HostName,
		AgentIp:           config.GAgentEnv.AgentIp,
		ParallelTaskCount: config.GAgentConfig.ParallelTaskCount,
		AgentInstallPath:  systemutil.GetExecutableDir(),
		StartedUser:       systemutil.GetCurrentUser().Username,
		TaskList:          taskList,
		Props: AgentPropsInfo{
			Arch:              runtime.GOARCH,
			JdkVersion:        jdkVersion,
			DockerInitFileMd5: dockerInitFileMd5,
		},
		DockerParallelTaskCount: config.GAgentConfig.DockerParallelTaskCount,
		DockerTaskList:          dockerTaskList,
	}

	return httputil.NewHttpClient().Post(url).Body(agentHeartbeatInfo).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoDevopsResult()
}

func CheckUpgrade(jdkVersion []string, dockerInitFileMd5 DockerInitFileInfo) (*httputil.AgentResult, error) {
	url := buildUrl("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/upgradeNew")

	info := &UpgradeInfo{
		WorkerVersion:      config.GAgentEnv.SlaveVersion,
		GoAgentVersion:     config.AgentVersion,
		JdkVersion:         jdkVersion,
		DockerInitFileInfo: dockerInitFileMd5,
	}

	return httputil.NewHttpClient().Post(url).Body(info).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoAgentResult()
}

func FinishUpgrade(success bool) (*httputil.AgentResult, error) {
	url := buildUrl("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/upgrade?success=" + strconv.FormatBool(success))
	return httputil.NewHttpClient().Delete(url).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoAgentResult()
}

func DownloadUpgradeFile(serverFile string, saveFile string) (fileMd5 string, err error) {
	url := buildUrl("/ms/environment/api/buildAgent/agent/thirdPartyAgent/upgrade/files/download?file=" + serverFile)
	return httputil.DownloadUpgradeFile(url, config.GAgentConfig.GetAuthHeaderMap(), saveFile)
}

func DownloadAgentInstallScript(saveFile string) error {
	url := buildUrl(fmt.Sprintf("/external/agents/%s/install", config.GAgentConfig.AgentId))
	return httputil.DownloadAgentInstallScript(url, config.GAgentConfig.GetAuthHeaderMap(), saveFile)
}

func AgentStartup() (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/environment/api/buildAgent/agent/thirdPartyAgent/startup")

	startInfo := &ThirdPartyAgentStartInfo{
		HostName:      config.GAgentEnv.HostName,
		HostIp:        config.GAgentEnv.AgentIp,
		DetectOs:      config.GAgentEnv.OsName,
		MasterVersion: config.AgentVersion,
		SlaveVersion:  config.GAgentEnv.SlaveVersion,
	}

	return httputil.NewHttpClient().Post(url).Body(startInfo).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoDevopsResult()
}

func GetAgentStatus() (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/environment/api/buildAgent/agent/thirdPartyAgent/status")
	return httputil.NewHttpClient().Get(url).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoDevopsResult()
}

func GetBuild(buildType BuildJobType) (*httputil.AgentResult, error) {
	url := buildUrl(fmt.Sprintf("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/startup?buildType=%s", buildType))
	return httputil.NewHttpClient().Get(url).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoAgentResult()
}

func WorkerBuildFinish(buildInfo *ThirdPartyBuildWithStatus) (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/workerBuildFinish")
	return httputil.NewHttpClient().Post(url).Body(buildInfo).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoDevopsResult()
}

func GetAgentPipeline() (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/environment/api/buildAgent/agent/thirdPartyAgent/agents/pipelines")
	return httputil.NewHttpClient().Get(url).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoDevopsResult()
}

func UpdatePipelineStatus(response *PipelineResponse) (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/environment/api/buildAgent/agent/thirdPartyAgent/agents/pipelines")
	return httputil.NewHttpClient().Put(url).Body(response).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoDevopsResult()
}

func DownloadAgentInstallBatchZip(saveFile string) error {
	url := buildUrl(fmt.Sprintf("/ms/environment/api/external/thirdPartyAgent/%s/batch_zip",
		config.GAgentConfig.BatchInstallKey))
	return httputil.DownloadAgentInstallScript(url, config.GAgentConfig.GetAuthHeaderMap(), saveFile)
}

func PullDockerDebugTask() (*httputil.AgentResult, error) {
	url := buildUrl("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/docker/startupDebug")
	return httputil.NewHttpClient().Get(url).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoAgentResult()
}

func FinishDockerDebug(imageDebug *ImageDebug, success bool, debugUrl string, error *Error) (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/docker/startupDebug")
	body := &ImageDebugFinish{
		ProjectId:  imageDebug.ProjectId,
		DebugId:    imageDebug.DebugId,
		PipelineId: imageDebug.PipelineId,
		DebugUrl:   debugUrl,
		Success:    success,
		Error:      error,
	}
	return httputil.NewHttpClient().Post(url).Body(body).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoDevopsResult()
}

func FetchDockerDebugStatus(debugId int64) (*httputil.DevopsResult, error) {
	url := buildUrl(fmt.Sprintf("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/docker/debug/status?debugId=%d", debugId))
	return httputil.NewHttpClient().Get(url).SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute().IntoDevopsResult()
}

// AuthHeaderDevopsBuildId log需要的buildId的header
const (
	AuthHeaderDevopsBuildId = "X-DEVOPS-BUILD-ID"
	AuthHeaderDevopsVmSeqId = "X-DEVOPS-VM-SID"
)

func AddLogLine(buildId string, message *LogMessage, vmSeqId string) (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/log/api/build/logs")
	headers := config.GAgentConfig.GetAuthHeaderMap()
	headers[AuthHeaderDevopsBuildId] = buildId
	headers[AuthHeaderDevopsVmSeqId] = vmSeqId
	return httputil.NewHttpClient().
		Post(url).Body(message).SetHeaders(headers).Execute().
		IntoDevopsResult()
}

func AddLogRedLine(buildId string, message *LogMessage, vmSeqId string) (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/log/api/build/logs/red")
	headers := config.GAgentConfig.GetAuthHeaderMap()
	headers[AuthHeaderDevopsBuildId] = buildId
	headers[AuthHeaderDevopsVmSeqId] = vmSeqId
	return httputil.NewHttpClient().
		Post(url).Body(message).SetHeaders(headers).Execute().
		IntoDevopsResult()
}
