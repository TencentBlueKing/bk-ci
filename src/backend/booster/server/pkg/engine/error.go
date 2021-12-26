/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package engine

import "fmt"

var (
	ErrorUnknownEngineType       = fmt.Errorf("unknown engine type")
	ErrorNoTaskInQueue           = fmt.Errorf("there is no task in the queue")
	ErrorNoEnoughResources       = fmt.Errorf("no enough resources")
	ErrorTaskNoFound             = fmt.Errorf("task no found")
	ErrorUnterminatedTaskNoFound = fmt.Errorf("unterminated task no found")
	ErrorProjectNoFound          = fmt.Errorf("project no found")
	ErrorWhitelistNoFound        = fmt.Errorf("whitelist no found")
	ErrorInnerEngineError        = fmt.Errorf("inner engine error")
	ErrorNoSupportDegrade        = fmt.Errorf("no support degrade")
	ErrorNoQueueNameSpecified    = fmt.Errorf("no queue name specified")
	ErrorUnknownMessageType      = fmt.Errorf("unknown message type")
	ErrorIPNotSpecified          = fmt.Errorf("ip not specified")
)
