package job

import (
	"fmt"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

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
