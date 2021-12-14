/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package cache

import (
	"testing"
)

func TestIsEncrypt(t *testing.T) {
	key := "hello123"
	if isEncrypt(key) {
		t.Errorf("is encrypt error: %s is not a md5", key)
	}

	key = "62e6bd422bdf63989fe014b650cfdd30"
	if !isEncrypt(key) {
		t.Errorf("is encrypt error: %s is a md5", key)
	}

	key = "62e6bd42ABCf63989fe014b650cfdd30"
	if isEncrypt(key) {
		t.Errorf("is encrypt error: %s is not a md5", key)
	}

	key = "_sdf12323$_#ssssssssssssssssssss"
	if isEncrypt(key) {
		t.Errorf("is encrypt error: %s is not a md5", key)
	}

	key = ""
	if isEncrypt(key) {
		t.Errorf("is encrypt error: %s is not a md5", key)
	}

	key = "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh"
	if !isEncrypt(key) {
		t.Errorf("is encrypt error: %s is a md5", key)
	}
}
