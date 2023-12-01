package exitcode

import (
	"errors"
	"fmt"
	"os"
	"sync/atomic"
	"syscall"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
)

type ExitErrorType struct {
	ErrorEnum ExitErrorEnum
	Message   string
}

type ExitErrorEnum string

const (
	ExitNotWorker          ExitErrorEnum = "THIRD_AGENT_EXIT_NOT_WORKER"
	ExitLeftDevice                       = "THIRD_AGENT_EXIT_LEFT_DEVICE"
	ExitNoPermissionDenied               = "THIRD_AGENT_EXIT_PERMISSION_DENIED"
	ExitJdkError                         = "THIRD_AGENT_EXIT_JDK_ERROR"
	ExitWorkerError                      = "THIRD_AGENT_EXIT_WORKER_ERROR"
)

var exitError *ExitErrorType = nil

func AddExitError(enum ExitErrorEnum, msg string) {
	logs.Errorf("AddExitError|%s|%s", enum, msg)
	exitError = &ExitErrorType{
		ErrorEnum: enum,
		Message:   msg,
	}
}

func GetExitError() *ExitErrorType {
	exit := exitError
	exitError = nil
	return exit
}

func Exit() {
	if exitError != nil {
		logs.Errorf("ExitError|%s|%s", exitError.ErrorEnum, exitError.Message)
	}
	os.Exit(constant.DAEMON_EXIT_CODE)
}

func WriteFileWithCheck(name string, data []byte, perm os.FileMode) error {
	err := os.WriteFile(name, data, perm)

	CheckOsIoError(name, err)

	return err
}

func CheckOsIoError(path string, err error) {
	// 检查写入磁盘空间不足
	if errors.Is(err, syscall.ENOSPC) {
		AddExitError(ExitLeftDevice, fmt.Sprintf("WriteFile %s|%s", path, err.Error()))
		return
	}

	// 写入没有权限
	if errors.Is(err, syscall.EACCES) {
		AddExitError(ExitNoPermissionDenied, fmt.Sprintf("WriteFile %s|%s", path, err.Error()))
		return
	}

	// 目前未知的先打印后续依次添加
	logs.Errorf("WriteFileCheck %s|%s", path, err.Error())
}

var workerSignFlag atomic.Int32 = atomic.Int32{}
var jdkSignFlag atomic.Int32 = atomic.Int32{}

type ExitSignType string

const (
	ExitSignJdk    = "signjdk"
	ExitSignWorker = "signworker"
)

// 避免不必要的错杀，信号错误最少连续持续 10 次以上才能杀掉
func CheckSignError(err error, typ ExitSignType) {
	// 检查是不是 signkill
	var res int32 = -1
	if err.Error() == "signal: killed" {
		res = 1
	}

	switch typ {
	case ExitSignJdk:
		if jdkSignFlag.Load() == 0 && res == -1 {
			return
		}

		jdkSignFlag.Add(res)

		if jdkSignFlag.Load() == 10 {
			msg := fmt.Sprintf("%s %s time 10, exit", typ, err.Error())
			logs.Error(msg)
			AddExitError(ExitJdkError, msg)
			return
		}

	case ExitSignWorker:
		if workerSignFlag.Load() == 0 && res == -1 {
			return
		}

		workerSignFlag.Add(res)

		if workerSignFlag.Load() == 10 {
			msg := fmt.Sprintf("%s %s time 10, exit", typ, err.Error())
			logs.Error(msg)
			AddExitError(ExitWorkerError, msg)
			return
		}
	}

	if res == 1 {
		logs.Warn(fmt.Sprintf("%s %s add 1", typ, err.Error()))
	}
}
