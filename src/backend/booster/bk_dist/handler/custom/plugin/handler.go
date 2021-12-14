/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package plugin

import (
	"fmt"
	"plugin"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
)

const (
	newerFuncName = "New"
)

// New get a new go-plugin handler
func New(path string) (handler.Handler, error) {
	p, err := plugin.Open(path)
	if err != nil {
		return nil, err
	}

	symbol, err := p.Lookup(newerFuncName)
	if err != nil {
		return nil, err
	}

	f, ok := symbol.(func() handler.Handler)
	if !ok {
		return nil, fmt.Errorf("get newer function 'New' from plugin %s failed, function not match", path)
	}

	return f(), nil
}
