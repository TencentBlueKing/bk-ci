/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package shader

import (
	"bufio"
	"bytes"
	"fmt"
	"os"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/common/blog"

	"github.com/saintfish/chardet"
	"golang.org/x/text/encoding/unicode"
)

func checkCharset(rawBytes []byte) (string, error) {
	detector := chardet.NewTextDetector()
	charset, err := detector.DetectBest(rawBytes)
	if err != nil {
		return "", err
	}

	return charset.Charset, nil
}

func toBom(rawBytes []byte) (string, error) {
	r := bytes.NewReader(rawBytes)
	dec := unicode.UTF16(unicode.LittleEndian, unicode.UseBOM).NewDecoder()
	scn := bufio.NewScanner(dec.Reader(r))
	data := ""
	for scn.Scan() {
		data = data + scn.Text()
	}
	if err := scn.Err(); err != nil {
		return "", err
	}

	return data, nil
}

func toUTF8(rawBytes []byte) (string, error) {
	if rawBytes == nil {
		return "", fmt.Errorf("input data is nil when ready to utf8")
	}

	charset, err := checkCharset(rawBytes)
	if err != nil {
		return "", err
	}

	blog.Debugf("shader: got data charset[%s]", charset)
	data := ""
	if charset == "UTF-16LE" || charset == "ISO-8859-1" || charset == "windows-1252" {
		data, err = toBom(rawBytes)
	} else {
		data = string(rawBytes)
		err = nil
	}
	if err != nil {
		return "", err
	}

	if data == "" {
		return "", fmt.Errorf("result data is empty")
	}

	return data, nil
}

func checkResultFile(f string, size int64, rawBytes []byte) error {
	if size < 5*1024 {
		data, err := toUTF8(rawBytes)
		contentError := false
		if err == nil {
			blackKeyList := []string{"Assertion failed"}
			// blog.Debugf("shader: ready check shader file[%s] content[%s]", f, data)
			for _, v := range blackKeyList {
				if strings.Contains(data, v) {
					contentError = true
					break
				}
			}

			if contentError {
				err = fmt.Errorf("found error for shader file[%s] with content[%s]", f, data)
				blog.Warnf("shader: %v", err)
				return err
			}
		} else {
			blog.Warnf("shader: failed to check shader result data with error[%v]", err)
		}
	}

	// blog.Infof("shader: succeed to check shader file[%s]", f)
	return nil
}

func saveToFile(resultfilepath string, data []byte) error {
	blog.Debugf("shader: ready save file [%s]", resultfilepath)
	if resultfilepath == "" {
		blog.Warnf("shader: file [%s] path is empty!", resultfilepath)
		return fmt.Errorf("file path is empty")
	}

	f, err := os.Create(resultfilepath)
	if err != nil {
		blog.Errorf("shader: create file %s error: [%s]", resultfilepath, err.Error())
		return err
	}

	defer func() {
		_ = f.Close()
	}()

	_, err = f.Write(data)

	return err
}

func checkAndsaveResultFile(rf *dcSDK.FileDesc) error {
	data := rf.Buffer
	resultfilepath := rf.FilePath

	if rf.CompressedSize > 0 {
		switch rf.Compresstype {
		case protocol.CompressNone:
			err := checkResultFile(resultfilepath, rf.FileSize, data)
			if err != nil {
				blog.Errorf("check file [%s] error: [%s]", resultfilepath, err.Error())
				return err
			}

			err = saveToFile(resultfilepath, data)
			if err != nil {
				blog.Errorf("save file [%s] error: [%s]", resultfilepath, err.Error())
				return err
			}
			break
		// case protocol.CompressLZO:
		// 	// decompress with lzox1 firstly
		// 	outdata, err := golzo.Decompress1X(bytes.NewReader(data), int(rf.CompressedSize), 0)
		// 	if err != nil {
		// 		blog.Errorf("shader: decompress file %s error: [%s]", resultfilepath, err.Error())
		// 		return err
		// 	}
		// 	outlen := len(string(outdata))
		// 	blog.Debugf("shader: decompressed file %s with lzo1x, from [%d] to [%d]",
		// 		resultfilepath, rf.CompressedSize, outlen)
		// 	if outlen != int(rf.FileSize) {
		// 		err := fmt.Errorf("shader: decompressed size %d, expected size %d", outlen, rf.FileSize)
		// 		blog.Errorf("shader: decompress error: [%v]", err)
		// 		return err
		// 	}

		// 	err = checkResultFile(resultfilepath, int64(outlen), outdata)
		// 	if err != nil {
		// 		blog.Errorf("check file [%s] error: [%s]", resultfilepath, err.Error())
		// 		return err
		// 	}

		// 	err = saveToFile(resultfilepath, outdata)
		// 	if err != nil {
		// 		blog.Errorf("shader: save file [%s] error: [%v]", resultfilepath, err)
		// 		return err
		// 	}
		// 	break
		case protocol.CompressLZ4:
			// decompress with lz4 firstly
			dst := make([]byte, rf.FileSize)
			if dst == nil {
				err := fmt.Errorf("failed to alloc [%d] size buffer", rf.FileSize)
				blog.Errorf("%v", err)
				return err
			}

			outdata, err := dcUtil.Lz4Uncompress(data, dst)
			if err != nil {
				blog.Errorf("shader: decompress [%s] error: [%s], data len:[%d], buffer len:[%d], filesize:[%d]",
					resultfilepath, err.Error(), len(data), len(dst), rf.FileSize)
				return err
			}

			outlen := len(outdata)
			blog.Debugf("shader: decompressed file %s with lz4, from [%d] to [%d]",
				resultfilepath, rf.CompressedSize, outlen)
			if outlen != int(rf.FileSize) {
				err := fmt.Errorf("decompressed size %d, expected size %d", outlen, rf.FileSize)
				blog.Errorf("shader: decompress error: [%v]", err)
				return err
			}

			err = checkResultFile(resultfilepath, int64(outlen), outdata)
			if err != nil {
				blog.Errorf("check file [%s] error: [%s]", resultfilepath, err.Error())
				return err
			}

			err = saveToFile(resultfilepath, outdata)
			if err != nil {
				blog.Errorf("shader: save file [%s] error: [%v]", resultfilepath, err)
				return err
			}
			break
		default:
			return fmt.Errorf("shader: unknown compress type [%s]", rf.Compresstype)
		}
	} else {
		return fmt.Errorf("empty file")
	}

	blog.Debugf("shader: succeed to save file [%s]", resultfilepath)
	return nil
}
