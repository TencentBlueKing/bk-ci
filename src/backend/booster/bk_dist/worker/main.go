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

	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

func main() {
	c := config.NewConfig()
	if c == nil {
		fmt.Fprintln(os.Stderr, fmt.Errorf("failed to get config file"))
		os.Exit(1)
	}

	c.Parse()
	blog.InitLogs(c.LogConfig)
	defer blog.CloseLogs()

	if err := pkg.Run(c); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
