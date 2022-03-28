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
	"strings"

	"github.com/shirou/gopsutil/process"
)

// KillChildren kill all process children of given process
func KillChildren(p *process.Process) {
	children, err := p.Children()
	if err == nil && len(children) > 0 {
		for _, v := range children {
			n, err := v.Name()
			// do not kill bk-dist-controller, for it may be used by other process
			if err == nil && strings.Contains(n, "bk-dist-controller") {
				continue
			}
			KillChildren(v)
			_ = v.Kill()
		}
	}
}
