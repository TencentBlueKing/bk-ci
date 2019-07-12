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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package job

import (
	"encoding/json"
	"github.com/astaxie/beego/logs"
	"os"
	"pkg/api"
)

type buildManager struct {
	instances map[int]*api.ThirdPartyBuildInfo
}

var GBuildManager *buildManager

func init() {
	GBuildManager = new(buildManager)
	GBuildManager.instances = make(map[int]*api.ThirdPartyBuildInfo)
}

func (b *buildManager) GetInstanceCount() int {
	return len(b.instances)
}

func (b *buildManager) GetInstances() []api.ThirdPartyBuildInfo {
	result := make([]api.ThirdPartyBuildInfo, 0)
	for _, value := range b.instances {
		result = append(result, *value)
	}
	return result
}

func (b *buildManager) AddBuild(processId int, buildInfo *api.ThirdPartyBuildInfo) {
	bytes, _ := json.Marshal(buildInfo)
	logs.Info("add build: processId: ", processId, ", buildInfo: ", string(bytes))
	b.instances[processId] = buildInfo
	go b.waitProcessDone(processId)
}

func (b *buildManager) waitProcessDone(processId int) {
	process, err := os.FindProcess(processId)
	if err != nil {
		logs.Warn("build process err, pid: ", processId, ", err: ", err.Error())
		delete(b.instances, processId)
		return
	}
	process.Wait()
	logs.Info("build process finish: pid: ", processId)
	delete(b.instances, processId)
}
