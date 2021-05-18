package encrypt

import (
	"bytes"
	"crypto/cipher"
	"crypto/des"
	"encoding/base64"

	"build-booster/common/static"
)

var (
	//key for encryption
	priKey = static.EncryptionKey
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
func DesEncryptToBase(src []byte) ([]byte, error) {
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
func DesDecryptFromBase(src []byte) ([]byte, error) {
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
