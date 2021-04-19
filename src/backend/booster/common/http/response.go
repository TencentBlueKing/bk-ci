/*
Copyright 2016 The GSE Authors All rights reserved
*/

package http

import (
	"fmt"
	"os"
	"path/filepath"

	"build-booster/common/codec"
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
	if err = codec.EncJSON(resp, &r); err != nil {
		return
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
