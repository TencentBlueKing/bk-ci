/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

import (
	"fmt"
)

var (
	ErrorManagerNotRunning     = fmt.Errorf("manager is not running")
	ErrorInvalidIPV4           = fmt.Errorf("invalid ip v4")
	ErrorIPNotAllowed          = fmt.Errorf("ip not allowed")
	ErrorConcurrencyLimit      = fmt.Errorf("the task concurrency reaches the limits")
	ErrorGenerateTaskIDFailed  = fmt.Errorf("generate task id failed")
	ErrorTaskAlreadyTerminated = fmt.Errorf("task is already in terminated status")
	ErrorLeaderNoFound         = fmt.Errorf("leader no found")
	ErrorDataPathNoFound       = fmt.Errorf("data path no found")
)
