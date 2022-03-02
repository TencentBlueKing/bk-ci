/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

/*
 *
 * 测试相对路径的命令调用
 */

package syscall

import (
	"testing"
)

func TestRelativePathCall(t *testing.T) {
	sandbox := &Sandbox{
		Dir : "/tmp",
	}
	_, err := sandbox.ExecCommand("./bk-dist-executor")
	if (err != nil) {
		t.Errorf("exec command error %v", err)
	}
	
}


