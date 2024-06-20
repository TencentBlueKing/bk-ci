package exitcode

import (
	"errors"
	"fmt"
	"os"
	"strings"
	"sync/atomic"
	"syscall"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
)

type ExitErrorType struct {
	ErrorEnum ExitErrorEnum `json:"errorEnum"`
	Message   string        `json:"message"`
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

func GetAndResetExitError() *ExitErrorType {
	exit := exitError
	exitError = nil
	return exit
}

func Exit(exitError *ExitErrorType) {
	if exitError != nil {
		logs.Errorf("ExitError|%s|%s", exitError.ErrorEnum, exitError.Message)
	}
	os.Exit(constant.DaemonExitCode)
}

func WriteFileWithCheck(name string, data []byte, perm os.FileMode) error {
	err := os.WriteFile(name, data, perm)

	CheckOsIoError(name, err)

	return err
}

func CheckOsIoError(path string, err error) {
	if err == nil {
		return
	}

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

// 避免不必要的错杀，信号错误最少连续持续 10 次以上才能杀掉
var jdkSignFlag atomic.Int32 = atomic.Int32{}

func CheckSignalJdkError(err error) {
	if err == nil {
		if jdkSignFlag.Load() > 0 {
			jdkSignFlag.Add(-1)
			logs.Warn("signjdk err nil add -1")
		}
		return
	}

	// 检查是不是需要杀掉的信号
	if strings.TrimSpace(err.Error()) != "signal: killed" {
		if jdkSignFlag.Load() > 0 {
			jdkSignFlag.Add(-1)
			logs.Warn(fmt.Sprintf("signjdk err %s unkunow add -1", err.Error()))
		}
		return
	}

	jdkSignFlag.Add(1)
	logs.Warn(fmt.Sprintf("signjdk err %s add 1", err.Error()))
	if jdkSignFlag.Load() >= 10 {
		msg := fmt.Sprintf("signjdk err %s time 10, will exit", err.Error())
		logs.Error(msg)
		AddExitError(ExitJdkError, msg)
		return
	}
}

var workerSignFlag atomic.Int32 = atomic.Int32{}

func CheckSignalWorkerError(err error) {
	if err == nil {
		if workerSignFlag.Load() > 0 {
			workerSignFlag.Add(-1)
			logs.Warn("signworker err nil add -1")
		}
		return
	}

	// 检查是不是需要杀掉的信号
	if strings.TrimSpace(err.Error()) != "signal: killed" {
		if workerSignFlag.Load() > 0 {
			workerSignFlag.Add(-1)
			logs.Warn(fmt.Sprintf("signworker err %s unkunow add -1", err.Error()))
		}
		return
	}

	workerSignFlag.Add(1)
	logs.Warn(fmt.Sprintf("signworker err %s add 1", err.Error()))
	if workerSignFlag.Load() >= 10 {
		msg := fmt.Sprintf("signworker err %s time 10, will exit", err.Error())
		logs.Error(msg)
		AddExitError(ExitWorkerError, msg)
		return
	}
}
