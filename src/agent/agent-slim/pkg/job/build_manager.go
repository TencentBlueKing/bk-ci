package job

import (
	"encoding/json"
	"os"
	"sync"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/i18n"
)

// buildManager 二进制构建对象管理
type buildManager struct {
	lock      *sync.RWMutex
	instances map[int]*buildData
}

type buildData struct {
	info       *api.PersistenceBuildInfo
	toDelFiles []string
}

var GBuildManager *buildManager

func init() {
	GBuildManager = &buildManager{
		lock:      &sync.RWMutex{},
		instances: make(map[int]*buildData),
	}
}

func (b *buildManager) GetBuildCount() int {
	b.lock.RLock()
	defer b.lock.RUnlock()
	return len(b.instances)
}

func (b *buildManager) AddBuild(processId int, buildInfo *api.PersistenceBuildInfo, toDelFiles []string) {
	b.lock.Lock()
	defer b.lock.Unlock()
	bytes, _ := json.Marshal(buildInfo)
	logs.Infof("add build: processId: %d , buildInfo: %s", processId, string(bytes))
	b.instances[processId] = &buildData{
		info:       buildInfo,
		toDelFiles: toDelFiles,
	}

	// #5806 预先录入异常信息，在构建进程正常结束时清理掉。如果没清理掉，则说明进程非正常退出，可能被OS或人为杀死
	errorMsgFile := getWorkerErrorMsgFile(buildInfo.BuildId, buildInfo.VmSeqId)
	_ = fileutil.WriteString(errorMsgFile, i18n.Localize("BuilderProcessWasKilled", nil))
	_ = os.Chmod(errorMsgFile, os.ModePerm)
	go b.waitProcessDone(processId)
}

func (b *buildManager) GetBuild(processId int) *buildData {
	b.lock.RLock()
	defer b.lock.RUnlock()
	return b.instances[processId]
}

func (b *buildManager) RemoveBuild(processId int) {
	b.lock.Lock()
	defer b.lock.Unlock()
	delete(b.instances, processId)
}

func (b *buildManager) waitProcessDone(processId int) {
	process, err := os.FindProcess(processId)
	data := b.GetBuild(processId)
	info := data.info
	if err != nil {
		errMsg := i18n.Localize("BuildProcessErr", map[string]interface{}{"pid": processId, "err": err.Error()})
		logs.Warn(errMsg)
		b.RemoveBuild(processId)
		workerBuildFinish(info.ToFinish(false, errMsg, api.BuildProcessRunErrorEnum), data.toDelFiles)
		return
	}

	state, err := process.Wait()
	// #5806 从b-xxxx_build_msg.log 读取错误信息，此信息可由worker-agent.jar写入，用于当异常时能够将信息上报给服务器
	msgFile := getWorkerErrorMsgFile(info.BuildId, info.VmSeqId)
	msg, _ := fileutil.GetString(msgFile)
	logs.Infof("build[%s] pid[%d] finish, state=%v err=%v, msg=%s", info.BuildId, processId, state, err, msg)

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
	b.RemoveBuild(processId)
	if success {
		workerBuildFinish(buildInfo.ToFinish(success, msg, api.NoErrorEnum), data.toDelFiles)
	} else {
		workerBuildFinish(buildInfo.ToFinish(success, msg, api.BuildProcessRunErrorEnum), data.toDelFiles)
	}
}
