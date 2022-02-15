/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package encrypt

import (
	"fmt"
	"testing"
)

func TestDeEncrytion(t *testing.T) {
	oriStr := "bcs@2017"
	fmt.Println("original: ", oriStr)
	b64Str, err := DesEncryptToBase([]byte(oriStr))
	if err != nil {
		t.Errorf("encrypt err: %s\n", err.Error())
	}
	fmt.Println("base64 out string: ", string(b64Str))

	original, derr := DesDecryptFromBase(b64Str)
	if derr != nil {
		t.Errorf("decrypt err: %s\n", derr.Error())
	}
	fmt.Println("decrypt: ", string(original))
	if string(original) != oriStr {
		t.Errorf("Decryption Error, old: %s, new: %s", oriStr, original)
	}
}
