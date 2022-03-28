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

var (
	ErrNoLocal = fmt.Errorf("task degraded to local compiling and " +
		"exit according to the --no_local flag set")
	ErrCompile                 = fmt.Errorf("compile error, build exit")
	ErrBoosterNoRegistered     = fmt.Errorf("booster no registered")
	ErrTaskPreparingTimeout    = fmt.Errorf("task prepareing timeout")
	ErrContextCanceled         = fmt.Errorf("context canceled")
	ErrWorkAlreadyRegistered   = fmt.Errorf("work already registered")
	ErrWorkAlreadyUnregistered = fmt.Errorf("work already unregistered")
	ErrTaskApplyingFailed      = fmt.Errorf("task applying failed")
)
