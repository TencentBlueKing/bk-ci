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
	"encoding/json"
	"fmt"
	"os"
	"sync"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/i18n"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

// buildManager 二进制构建对象管理
type buildManager struct {
	// preInstance 接取的构建任务但还没开始进行构建 [string]bool
	preInstances sync.Map
	// instances 正在执行中的构建对象 [int]*api.ThirdPartyBuildInfo
	instances sync.Map
}

var GBuildManager *buildManager

func init() {
	GBuildManager = new(buildManager)
}

func (b *buildManager) GetInstanceCount() int {
	var i = 0
	b.instances.Range(func(_, _ interface{}) bool {
		i++
		return true
	})
	return i
}

func (b *buildManager) GetInstances() []api.ThirdPartyBuildInfo {
	result := make([]api.ThirdPartyBuildInfo, 0)
	b.instances.Range(func(_, value interface{}) bool {
		result = append(result, *value.(*api.ThirdPartyBuildInfo))
		return true
	})
	return result
}

func (b *buildManager) AddBuild(processId int, buildInfo *api.ThirdPartyBuildInfo) {
	bytes, _ := json.Marshal(buildInfo)
	logs.Info("add build: processId: ", processId, ", buildInfo: ", string(bytes))
	b.instances.Store(processId, buildInfo)
	// 启动构建了就删除preInstance
	b.DeletePreInstance(buildInfo.BuildId)

	// #5806 预先录入异常信息，在构建进程正常结束时清理掉。如果没清理掉，则说明进程非正常退出，可能被OS或人为杀死
	errorMsgFile := getWorkerErrorMsgFile(buildInfo.BuildId, buildInfo.VmSeqId)
	_ = fileutil.WriteString(errorMsgFile, i18n.Localize("BuilderProcessWasKilled", nil))
	_ = systemutil.Chmod(errorMsgFile, os.ModePerm)
	go b.waitProcessDone(processId)
}

func (b *buildManager) waitProcessDone(processId int) {
	process, err := os.FindProcess(processId)
	inf, ok := b.instances.Load(processId)
	var info *api.ThirdPartyBuildInfo
	if ok {
		info = inf.(*api.ThirdPartyBuildInfo)
	}
	if err != nil {
		errMsg := i18n.Localize("BuildProcessErr", map[string]interface{}{"pid": processId, "err": err.Error()})
		logs.Warn(errMsg)
		b.instances.Delete(processId)
		workerBuildFinish(info.ToFinish(false, errMsg, api.BuildProcessRunErrorEnum))
		return
	}

	state, err := process.Wait()
	// #5806 从b-xxxx_build_msg.log 读取错误信息，此信息可由worker-agent.jar写入，用于当异常时能够将信息上报给服务器
	msgFile := getWorkerErrorMsgFile(info.BuildId, info.VmSeqId)
	msg, _ := fileutil.GetString(msgFile)
	logs.Info(fmt.Sprintf("build[%s] pid[%d] finish, state=%v err=%v, msg=%s", info.BuildId, processId, state, err, msg))

	if err != nil {
		if len(msg) == 0 {
			msg = err.Error()
		}
	}
	success := true
	if len(msg) == 0 {
		msg = i18n.Localize("WorkerExit", map[string]interface{}{"pid": processId})
	} else {
		success = false
	}

	buildInfo := info
	b.instances.Delete(processId)
	if success {
		workerBuildFinish(buildInfo.ToFinish(success, msg, api.NoErrorEnum))
	} else {
		workerBuildFinish(buildInfo.ToFinish(success, msg, api.BuildProcessRunErrorEnum))
	}
}

func (b *buildManager) GetPreInstancesCount() int {
	var i = 0
	b.preInstances.Range(func(_, _ interface{}) bool {
		i++
		return true
	})
	return i
}

func (b *buildManager) AddPreInstance(buildId string) {
	b.preInstances.Store(buildId, true)
}

func (b *buildManager) DeletePreInstance(buildId string) {
	b.preInstances.Delete(buildId)
}
