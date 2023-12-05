package job

import (
	"fmt"
	"io/fs"
	"os"
	"strings"
	"time"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/config"
)

const (
	errorMsgFileSuffix           = "build_msg.log"
	prepareStartScriptFilePrefix = "devops_agent_prepare_start"
	prepareStartScriptFileSuffix = ".sh"
	startScriptFilePrefix        = "devops_agent_start"
	startScriptFileSuffix        = ".sh"
)

// checkWorkerCount 检查当前运行的最大任务数
func checkWorkerCount() bool {
	instanceCount := GBuildManager.GetBuildCount()
	if config.Config.MaxWorkerCount != 0 && instanceCount >= config.Config.MaxWorkerCount {
		logs.Infof("worker count exceed, wait worker done, MaxWorkerCount: %d, instance count: %d", config.Config.MaxWorkerCount, instanceCount)
		return false
	}

	return true
}

// getWorkerErrorMsgFile 获取worker执行错误信息的日志文件
func getWorkerErrorMsgFile(buildId, vmSeqId string) string {
	return fmt.Sprintf("%s/build_tmp/%s_%s_%s", config.Config.WorkDir, buildId, vmSeqId, errorMsgFileSuffix)
}

// getUnixWorkerPrepareStartScriptFile 获取unix系统，主要是darwin和linux的prepare start script文件
func getUnixWorkerPrepareStartScriptFile(projectId, buildId, vmSeqId string) string {
	return fmt.Sprintf("%s/%s_%s_%s_%s%s", config.Config.WorkDir, prepareStartScriptFilePrefix, projectId, buildId, vmSeqId, prepareStartScriptFileSuffix)
}

// getUnixWorkerStartScriptFile 获取unix系统，主要是darwin和linux的prepare start script文件
func getUnixWorkerStartScriptFile(projectId, buildId, vmSeqId string) string {
	return fmt.Sprintf("%s/%s_%s_%s_%s%s", config.Config.WorkDir, startScriptFilePrefix, projectId, buildId, vmSeqId, startScriptFileSuffix)
}

func workerBuildFinish(buildInfo *api.PersistenceBuildWithStatus, toDelFiles []string) {
	if buildInfo == nil {
		logs.Warn("buildInfo not exist")
		return
	}

	// 清理构建过程生成的文件
	if len(toDelFiles) > 0 {
		for _, filePath := range toDelFiles {
			e := os.Remove(filePath)
			logs.Info(fmt.Sprintf("build[%s] finish, delete:%s, err:%s", buildInfo.BuildId, filePath, e))
		}
	}

	// Agent build_tmp目录清理
	go checkAndDeleteBuildTmpFile()

	if buildInfo.Success {
		time.Sleep(8 * time.Second)
	}
	result, err := api.WorkerBuildFinish(buildInfo)
	if err != nil {
		logs.Error("send worker build finish failed: ", err.Error())
		return
	}
	if !result {
		logs.Error("worker build finish false")
		return
	}
	logs.Info("workerBuildFinish done")
}

// mkBuildTmpDir 创建构建提供的临时目录
// 对于指定构建帐号与当前agent运行帐号不同时，常用是用root安装运行agent，但配置文件中devops.slave.user指定其他普通帐号
// 需要设置最大权限，以便任何runUser能够使用, 不考虑用chown切换目录属主，会导致之前的运行中所产生的子目录/文件的清理权限问题。
func mkBuildTmpDir() (string, error) {
	tmpDir := fmt.Sprintf("%s/build_tmp", config.Config.WorkDir)
	err := os.MkdirAll(tmpDir, os.ModePerm)
	err2 := os.Chmod(tmpDir, os.ModePerm)
	if err == nil && err2 != nil {
		err = err2
	}
	return tmpDir, err
}

// checkAndDeleteBuildTmpFile 删除可能因为进程中断导致的没有被删除的构建过程临时文件
// Job最长运行时间为7天，所以这里通过检查超过7天最后修改时间的文件
func checkAndDeleteBuildTmpFile() {
	// win只用检查build_tmp目录
	workDir := config.Config.WorkDir
	dir := workDir + "/build_tmp"
	fss, err := os.ReadDir(dir)
	if err != nil {
		logs.Error("checkAndDeleteBuildTmpFile|read build_tmp dir error ", err)
		return
	}
	for _, f := range fss {
		if f.IsDir() {
			continue
		}
		// build_tmp 目录下的文件超过7天都清除掉
		removeFileThan7Days(dir, f)
	}

	// 还有prepare 和start文件
	fss, err = os.ReadDir(workDir)
	if err != nil {
		logs.Error("checkAndDeleteBuildTmpFile|read worker dir error ", err)
		return
	}
	for _, f := range fss {
		if f.IsDir() {
			continue
		}
		if !(strings.HasPrefix(f.Name(), startScriptFilePrefix) && strings.HasSuffix(f.Name(), startScriptFileSuffix)) &&
			!(strings.HasPrefix(f.Name(), prepareStartScriptFilePrefix) && strings.HasSuffix(f.Name(), prepareStartScriptFileSuffix)) {
			continue
		}
		removeFileThan7Days(workDir, f)
	}
}

func removeFileThan7Days(dir string, f fs.DirEntry) {
	info, err := f.Info()
	if err != nil {
		logs.Error("removeFileThan7Days|read file info error ", "file: ", f.Name(), " error: ", err)
		return
	}
	if (time.Since(info.ModTime())) > 7*24*time.Hour {
		err = os.Remove(dir + "/" + f.Name())
		if err != nil {
			logs.Error("removeFileThan7Days|remove file error ", "file: ", f.Name(), " error: ", err)
		}
	}
}
