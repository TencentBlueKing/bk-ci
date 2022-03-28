/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package http

import (
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
)

// APIResponse response for api request
type APIResponse struct {
	Result  bool        `json:"result"`
	Code    int         `json:"code"`
	Message string      `json:"message"`
	Data    interface{} `json:"data"`
}

// GetResponse adaptor
func GetResponse(code int, message string, data interface{}) ([]byte, error) {
	return createResponseEx(code, message, data, nil)
}

// GetResponseEx extension adaptor
func GetResponseEx(code int, message string, data interface{}, extra map[string]interface{}) ([]byte, error) {
	return createResponseEx(code, message, data, extra)
}

func createResponseEx(code int, message string, data interface{}, extra map[string]interface{}) (r []byte, err error) {
	result := code == 0
	if !result {
		appName := filepath.Base(os.Args[0])
		message = fmt.Sprintf("(%s):%s", appName, message)
	}

	resp := APIResponse{result, code, message, data}

	t1 := time.Now()
	if err = codec.EncJSON(resp, &r); err != nil {
		return
	}
	if delta := time.Now().Sub(t1); delta > 10*time.Millisecond {
		blog.Info("json cost: %s", delta)
	}

	return addExtraField(r, extra)
}

func addExtraField(s []byte, extra map[string]interface{}) (r []byte, err error) {
	if extra == nil {
		return s, nil
	}

	var jsn map[string]interface{}
	if err = codec.DecJSON(s, &jsn); err != nil {
		return
	}
	for k, v := range extra {
		if _, ok := jsn[k]; !ok {
			jsn[k] = v
		}
	}
	err = codec.EncJSON(jsn, &r)
	return
}
