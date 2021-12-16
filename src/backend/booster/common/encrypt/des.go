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
	"bytes"
	"crypto/cipher"
	"crypto/des"
	"encoding/base64"
	"fmt"

	"github.com/Tencent/bk-ci/src/booster/common/static"
)

var (
	//key for encryption
	priKey = static.EncryptionKey

	Disabled = ""
)

// PKCS5Padding size padding
func PKCS5Padding(ciphertext []byte, blockSize int) []byte {
	padding := blockSize - len(ciphertext)%blockSize
	padtext := bytes.Repeat([]byte{byte(padding)}, padding)
	return append(ciphertext, padtext...)
}

// PKCS5UnPadding size unpadding
func PKCS5UnPadding(origData []byte) []byte {
	length := len(origData)
	unpadding := int(origData[length-1])
	return origData[:(length - unpadding)]
}

// DesEncryptToBase encrypt with priKey simply, out base64 string
func DesEncryptToBase(src []byte) (b []byte, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("encrypt code from %s error, not a valid param", string(src))
		}
	}()

	if Disabled != "" {
		return src, nil
	}

	block, err := des.NewTripleDESCipher([]byte(priKey))
	if err != nil {
		return nil, err
	}
	src = PKCS5Padding(src, block.BlockSize())
	blockMode := cipher.NewCBCEncrypter(block, []byte(priKey)[:block.BlockSize()])
	out := make([]byte, len(src))
	blockMode.CryptBlocks(out, src)
	strOut := base64.StdEncoding.EncodeToString(out)
	return []byte(strOut), nil
}

// DesDecryptFromBase base64 decoding, and decrypt with priKey
func DesDecryptFromBase(src []byte) (b []byte, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("decrypt code from %s error, not a valid param", string(src))
		}
	}()

	if Disabled != "" {
		return src, nil
	}

	ori, _ := base64.StdEncoding.DecodeString(string(src))
	block, err := des.NewTripleDESCipher([]byte(priKey))
	if err != nil {
		return nil, err
	}
	blockMode := cipher.NewCBCDecrypter(block, []byte(priKey)[:block.BlockSize()])
	out := make([]byte, len(ori))
	blockMode.CryptBlocks(out, ori)
	out = PKCS5UnPadding(out)
	return out, nil
}
