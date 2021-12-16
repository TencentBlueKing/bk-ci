/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

import (
	"fmt"
)

// define errors
var (
	ErrorNoActionsToRun           = fmt.Errorf("not found any action to execute")
	ErrorOverMaxTime              = fmt.Errorf("execute over max wait seconds")
	ErrorInvalidWorkID            = fmt.Errorf("not found valid work id")
	ErrorInvalidJSON              = fmt.Errorf("json invalid")
	ErrorFailedApplyResource      = fmt.Errorf("failed to apply resource after wait seconds")
	ErrorApplyCmdExitUnexpedted   = fmt.Errorf("apply resource cmd exit unexpected")
	ErrorFailedLaunchController   = fmt.Errorf("failed to launch controller")
	ErrorSignalInt                = fmt.Errorf("signal of SIGINT")
	ErrorSignalTerm               = fmt.Errorf("signal of SIGTERM")
	ErrorSignalOthers             = fmt.Errorf("signal of others")
	ErrorUnknown                  = fmt.Errorf("unknown error")
	ErrorProjectSettingNotExisted = fmt.Errorf("not found project setting file")
	ErrorContorllerNil            = fmt.Errorf("controller sdk is nil")
	ErrorBoosterNil               = fmt.Errorf("booster is nil")
	ErrContextCanceled            = fmt.Errorf("context canceled")
	ErrInitHTTPHandle             = fmt.Errorf("failed to init http handle")
)

var errorCodeMap map[error]int

func init() {
	errorCodeMap = map[error]int{
		ErrorSignalOthers:             1,
		ErrorApplyCmdExitUnexpedted:   2,
		ErrorFailedApplyResource:      3,
		ErrorFailedLaunchController:   4,
		ErrorInvalidJSON:              5,
		ErrorProjectSettingNotExisted: 6,
		ErrorSignalInt:                130,
		ErrorSignalTerm:               143,
		ErrorUnknown:                  99,
	}
}

// GetErrorCode return error code by error
func GetErrorCode(err error) int {
	if v, ok := errorCodeMap[err]; ok {
		return v
	}

	// default error code
	return 99
}
