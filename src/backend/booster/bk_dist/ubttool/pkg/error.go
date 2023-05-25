/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

import "fmt"

var (
	ErrorNoActionsToRun           = fmt.Errorf("not found any action to execute")
	ErrorOverMaxTime              = fmt.Errorf("execute over max wait seconds")
	ErrorInvalidWorkID            = fmt.Errorf("not found valid work id")
	ErrorProjectSettingNotExisted = fmt.Errorf("not found project setting file")
	ErrorUnknown                  = fmt.Errorf("unknown error")
)

var errorCodeMap map[error]int

func init() {
	errorCodeMap = map[error]int{
		ErrorProjectSettingNotExisted: 6,
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
