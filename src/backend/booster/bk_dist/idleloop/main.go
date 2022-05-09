/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package main

import (
	"github.com/Tencent/bk-ci/src/booster/bk_dist/idleloop/command"
)

func main() {
	// // make blog logs into stderr
	// blog.InitLogs(conf.LogConfig{
	// 	ToStdErr: true,
	// })

	command.Run(command.ClientBKIdleLoop)
}
