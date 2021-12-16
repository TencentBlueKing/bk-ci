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

// define exported error messages
var (
	ErrNoTaskInQueue      = fmt.Errorf("there is no task in the queue")
	ErrDoesNotExist       = fmt.Errorf("the resource is not exist")
	ErrAppNotFound        = fmt.Errorf("application no found")
	ErrNilObject          = fmt.Errorf("object is nil")
	ErrResourceDiff       = fmt.Errorf("resource objects are not same ip or city")
	ErrResourceNotEnought = fmt.Errorf("resource objects is not enought")
	ErrResourceReported   = fmt.Errorf("resource reported is not valid")
	ErrInitHTTPHandle     = fmt.Errorf("failed to init http handle")
	ErrNotFoundServer     = fmt.Errorf("failed to get server info")
)
