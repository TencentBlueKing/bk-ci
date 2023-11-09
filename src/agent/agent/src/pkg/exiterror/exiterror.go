package exitcode

import (
	"errors"
	"fmt"
	"os"
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
	ExitNoPermissionDenied               = "THIRD_AGENT_EXIT_PERMISSION_DENID"
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
	return exitError
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
