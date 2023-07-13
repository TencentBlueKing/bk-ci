package service

import (
	"disaptch-k8s-manager/pkg/db/mysql"
	"disaptch-k8s-manager/pkg/kubeclient"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/types"
	"fmt"
	"strings"
)

func Executor(buildlessStartInfo *types.BuildLessStartInfo) (err error) {
	leftPushBuildLessReadyTask(types.BuildLessTask{
		ProjectId:      buildlessStartInfo.ProjectId,
		AgentId:        buildlessStartInfo.AgentId,
		BuildId:        buildlessStartInfo.BuildId,
		PipelineId:     buildlessStartInfo.PipelineId,
		VmSeqId:        buildlessStartInfo.VmSeqId,
		SecretKey:      buildlessStartInfo.SecretKey,
		ExecutionCount: buildlessStartInfo.ExecutionCount,
	})

	return nil
}

func StopBuildless(buildLessEndInfo types.BuildLessEndInfo) (err error) {
	kubeclient.DeletePod(buildLessEndInfo.PodName)
	return nil
}

func ClaimBuildLessTask(podId string) (buildLessTask *types.BuildLessTask, err error) {
	// 判断是否有认领任务记录
	buildLessTask, err = mysql.SelectBuildLessTask(podId)
	if err != nil {
		logs.Error(fmt.Sprintf("Check %s buildLess history error.", podId))
		return nil, err
	}

	if buildLessTask != nil {
		logs.Warn(fmt.Sprintf("Pod %s has claim task: %s, delete pod.", podId, buildLessTask))

		// 删除当前pod
		kubeclient.DeletePod(parsePodName(podId))

		return nil, nil
	}

	buildLessTask, err = popBuildLessReadyTask()
	if buildLessTask != nil {
		// db记录podName构建记录
		mysql.InsertBuildLessTask(podId, *buildLessTask)
	}

	return buildLessTask, err
}

func parsePodName(podId string) string {
	parts := strings.Split(podId, "-")
	parts = parts[:len(parts)-1]

	return strings.Join(parts, "-")
}
