package exitcode

import (
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
)

type ExitErrorType struct {
	ErrorEnum ExitErrorEnum
	Message   string
}

type ExitErrorEnum string

const (
	ExitNotWorker ExitErrorEnum = "THIRD_AGENT_EXIT_NOT_WORKER"
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
	systemutil.ExitProcess(constant.DAEMON_EXIT_CODE)
}
