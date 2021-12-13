/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package clfilter

import (
	"fmt"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// ensure compiler exist in args.
func ensureCompiler(args []string) (string, []string, error) {
	dependFile := ""
	if len(args) == 0 {
		blog.Errorf("cf: ensure compiler got empty arg")
		return dependFile, nil, ErrorMissingOption
	}

	if !strings.HasSuffix(args[0], defaultCompiler) {
		return dependFile, nil, fmt.Errorf("not supported cmd %s", args[0])
	}

	clargs := make([]string, 0, 0)
	for i, v := range args {
		if strings.HasPrefix(v, "-dependencies=") {
			dependFile = v[14:]

		} else if v == "--" {
			if i < len(args)-1 {
				for _, v1 := range args[i+1:] {
					clargs = append(clargs, strings.Trim(v1, "\""))
				}
			}
		}
	}

	return dependFile, clargs, nil
}
