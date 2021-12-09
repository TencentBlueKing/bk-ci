/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package common

import (
	"build-booster/bk_dist/controller/pkg/api/v1"
	"build-booster/bk_dist/controller/pkg/types"
)

// GetV1Manager 获取controller manager的单例
func GetV1Manager() types.Mgr {
	return v1.GetManager()
}
