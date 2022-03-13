/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package handlermap

import (
	"fmt"

	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/cc"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/custom"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/echo"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/find"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/tc"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/ue4"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/winclangcl"
)

var handleMap map[dcType.BoosterType]func() (handler.Handler, error)

func init() {
	handleMap = map[dcType.BoosterType]func() (handler.Handler, error){
		dcType.BoosterCC:     cc.NewTaskCC,
		dcType.BoosterFind:   find.NewFinder,
		dcType.BoosterTC:     tc.NewTextureCompressor,
		dcType.BoosterUE4:    ue4.NewUE4,
		dcType.BoosterClangCl : winclangcl.NewWinClangCl,
		dcType.BoosterEcho:   echo.NewEcho,
		dcType.BoosterCustom: custom.NewCustom,
	}
}

// GetHandler return handle by type
func GetHandler(key dcType.BoosterType) (handler.Handler, error) {
	if v, ok := handleMap[key]; ok {
		return v()
	}

	// default handler
	return nil, fmt.Errorf("unknown handler %s", key)
}
