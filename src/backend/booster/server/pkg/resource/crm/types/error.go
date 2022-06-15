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
	ErrorResourceAlreadyInit        = fmt.Errorf("resource already init")
	ErrorResourceNotRunning         = fmt.Errorf("resource not running")
	ErrorResourceNoExist            = fmt.Errorf("resource no exist")
	ErrorResourceAlreadyReleased    = fmt.Errorf("resource already released")
	ErrorApplicationNoFound         = fmt.Errorf("application no found")
	ErrorApplicationAlreadyLaunched = fmt.Errorf("application already launched")
	ErrorManagerNotRunning          = fmt.Errorf("manager not running")
	ErrorBrokerParamNotFit          = fmt.Errorf("broker parameters not fit")
	ErrorBrokerNotEnoughResources   = fmt.Errorf("broker not enough resources")
	ErrorBrokeringUnderCoolingTime  = fmt.Errorf("broker under cooling time")
)
