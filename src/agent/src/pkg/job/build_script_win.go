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
	"io/ioutil"
	"strings"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/api"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/config"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/systemutil"
	"github.com/astaxie/beego/logs"
)


func writeStartBuildAgentScriptBat(buildInfo *api.ThirdPartyBuildInfo) (string, error) {
	scriptFile := fmt.Sprintf(
		"%s/devops_agent_start_%s_%s_%s.bat",
		systemutil.GetWorkDir(),
		buildInfo.ProjectId,
		buildInfo.BuildId,
		buildInfo.VmSeqId)

	logs.Info("write start agent script: ", scriptFile)
	agentLogPrefix := fmt.Sprintf("%s_%s_agent", buildInfo.BuildId, buildInfo.VmSeqId)
	lines := []string{
		fmt.Sprintf("%s -Ddevops.slave.agent.start.file=%s -Dbuild.type=AGENT -DAGENT_LOG_PREFIX=%s -jar %s %s",
			config.GetJava(), scriptFile, agentLogPrefix, config.BuildAgentJarPath(), getEncodedBuildInfo(buildInfo)),
	}

	scriptContent := strings.Join(lines, "\n")

	err := ioutil.WriteFile(scriptFile, []byte(scriptContent), 0777)
	if err != nil {
		return "", err
	} else {
		return scriptFile, nil
	}
}
