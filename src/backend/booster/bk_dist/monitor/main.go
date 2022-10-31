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
	"fmt"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/monitor/command"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/monitor/pkg"
)

func main() {
	if !pkg.Lock() {
		fmt.Printf("exit for other instance is already started")
		return
	}
	defer pkg.Unlock()

	command.Run(command.ClientBKDistMonitor)
}
