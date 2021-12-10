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
	"os"

	"build-booster/common/blog"
	"build-booster/server/pkg/resource/direct/agent/config"
	"build-booster/server/pkg/resource/direct/agent/pkg"
)

func main() {
	c := config.NewConfig()
	c.Parse()
	blog.InitLogs(c.LogConfig)
	defer blog.CloseLogs()

	//fmt.Printf("server mode: %s, no-master: %v", config.Mode, c.ServerNoMasterMode)
	if err := pkg.Run(c); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
