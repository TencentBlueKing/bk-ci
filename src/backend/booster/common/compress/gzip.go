/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package compress

import (
	"bytes"
	"compress/gzip"
	"encoding/base64"
)

// compress source data with gzip, and then encode the result to base64
func ToBase64String(src []byte) string {
	var b bytes.Buffer
	gz := gzip.NewWriter(&b)
	_, _ = gz.Write(src)
	_ = gz.Close()

	return base64.StdEncoding.EncodeToString(b.Bytes())
}

// decode base64 and then decompress with gzip to source code
func ToSourceCode(dst string) []byte {
	d, _ := base64.StdEncoding.DecodeString(dst)

	b := bytes.NewBuffer(d)
	r, err := gzip.NewReader(b)
	if err != nil {
		return nil
	}

	var resB bytes.Buffer
	_, err = resB.ReadFrom(r)
	if err != nil {
		return nil
	}

	return resB.Bytes()
}
