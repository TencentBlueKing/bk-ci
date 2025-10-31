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

package api

import (
	"fmt"
	"reflect"
	"strconv"

	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/httputil"
)

func buildUrl(url string) string {
	return config.GetGateWay() + url
}

func FinishUpgrade(success bool) (*httputil.AgentResult, error) {
	url := buildUrl("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/upgrade?success=" + strconv.FormatBool(success))
	return httputil.NewHttpClient().Delete(url).
		SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(nil).IntoAgentResult()
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
		HostIp:        config.GAgentEnv.GetAgentIp(),
		DetectOs:      config.GAgentEnv.OsName,
		MasterVersion: config.AgentVersion,
		SlaveVersion:  third_components.Worker.GetVersion(),
	}

	return httputil.NewHttpClient().Post(url).Body(startInfo, false).
		SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(nil).IntoDevopsResult()
}

func WorkerBuildFinish(buildInfo *ThirdPartyBuildWithStatus) (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/workerBuildFinish")
	return httputil.NewHttpClient().Post(url).Body(buildInfo, false).
		SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(nil).IntoDevopsResult()
}

func UpdatePipelineStatus(response *PipelineResponse) (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/environment/api/buildAgent/agent/thirdPartyAgent/agents/pipelines")
	return httputil.NewHttpClient().Put(url).Body(response, false).
		SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(nil).IntoDevopsResult()
}

func DownloadAgentInstallBatchZip(saveFile string) error {
	url := buildUrl(fmt.Sprintf("/ms/environment/api/external/thirdPartyAgent/%s/batch_zip",
		config.GAgentConfig.BatchInstallKey))
	return httputil.DownloadAgentInstallScript(url, config.GAgentConfig.GetAuthHeaderMap(), saveFile)
}

func FinishDockerDebug(
	imageDebug *ImageDebug,
	success bool,
	debugUrl string,
	error *Error,
) (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/docker/startupDebug")
	body := &ImageDebugFinish{
		ProjectId:  imageDebug.ProjectId,
		DebugId:    imageDebug.DebugId,
		PipelineId: imageDebug.PipelineId,
		DebugUrl:   debugUrl,
		Success:    success,
		Error:      error,
	}
	return httputil.NewHttpClient().Post(url).Body(body, false).
		SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(nil).IntoDevopsResult()
}

func FetchDockerDebugStatus(debugId int64) (*httputil.DevopsResult, error) {
	url := buildUrl(
		fmt.Sprintf("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/docker/debug/status?debugId=%d", debugId),
	)
	return httputil.NewHttpClient().Get(url).
		SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(nil).IntoDevopsResult()
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
	return httputil.NewHttpClient().Post(url).Body(message, false).SetHeaders(headers).Execute(nil).IntoDevopsResult()
}

func AddLogRedLine(buildId string, message *LogMessage, vmSeqId string) (*httputil.DevopsResult, error) {
	url := buildUrl("/ms/log/api/build/logs/red")
	headers := config.GAgentConfig.GetAuthHeaderMap()
	headers[AuthHeaderDevopsBuildId] = buildId
	headers[AuthHeaderDevopsVmSeqId] = vmSeqId
	return httputil.NewHttpClient().Post(url).Body(message, false).SetHeaders(headers).Execute(nil).IntoDevopsResult()
}

// 针对Ask请求做日志特殊处理，优化打印重复日志过多影响排查的问题

var askRequest = struct {
	Body *AskInfo
	Resp *httputil.IgnoreDupLogResp
}{
	&AskInfo{},
	&httputil.IgnoreDupLogResp{},
}

func Ask(info *AskInfo) (*httputil.AgentResult, error) {
	url := buildUrl("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/ask")

	bodyEq := reflect.DeepEqual(info, askRequest.Body)

	resp := httputil.NewHttpClient().Post(url).Body(info, bodyEq).
		SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(askRequest.Resp)

	if !bodyEq {
		askRequest.Body = info
	}
	if !resp.IgnoreDupLog {
		askRequest.Resp.Status = resp.Status
		askRequest.Resp.Resp = string(resp.Body)
	}

	return resp.IntoAgentResult()
}
