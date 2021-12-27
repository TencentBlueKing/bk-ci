/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package metric

import (
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/metric/controllers"
	selfController "github.com/Tencent/bk-ci/src/booster/server/pkg/metric/controllers"
)

type HttpRequestLabel = controllers.HttpRequestLabel

var (
	HttpRequestController    *controllers.HttpRequestController
	ElectionStatusController *controllers.ElectionStatusController
	MySQLOperationController *controllers.MySQLOperationController
	ResourceStatusController *controllers.ResourceStatusController

	CheckFailController       *selfController.CheckFailController
	TaskNumController         *selfController.TaskNumController
	TaskTimeController        *selfController.TaskTimeController
	TaskRunningTimeController *selfController.TaskRunningTimeController
)

// TimeMetricRecord as a decorator for functions, records the function execution time.
//
// func f() {
//     TimeMetricRecord("operation")()
//
//     // Do function work
// }
func TimeMetricRecord(operation string) func() {
	startTime := time.Now()
	return func() {
		if MySQLOperationController != nil {
			MySQLOperationController.Observe(operation, float64(time.Since(startTime).Nanoseconds()/1e6))
		}
	}
}
