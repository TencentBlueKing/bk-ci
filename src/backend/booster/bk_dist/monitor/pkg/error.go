/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

import "fmt"

var (
	ErrorRuleSettingNotExisted = fmt.Errorf("not found rules file")
	ErrFileLock                = fmt.Errorf("lock file failed")
)
