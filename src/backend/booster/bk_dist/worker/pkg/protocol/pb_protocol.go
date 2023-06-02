/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package protocol

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/cache"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"

	"github.com/gogo/protobuf/proto"
)

// define const vars
var (
	bkdistcmdversion = protocol.Bkdistcmdversion
	bkdistcmdmagic   = protocol.Bkdistcmdmagic
)

// return relativepath, abspath, error
type PathMapping func(inputfile string, basedir string, relativedir string) (string, string, error)

func formatTokenInt(token string, val int) ([]byte, error) {
	if len(token) != protocol.TOKENLEN {
		err := fmt.Errorf("token[%s] is invalid", token)
		blog.Debugf("write token int error: [%s]", err.Error())
		return nil, err
	}

	if val < 0 {
		err := fmt.Errorf("val[%d] is invalid", val)
		blog.Debugf("write token int error: [%s]", err.Error())
		return nil, err
	}

	data := []byte(fmt.Sprintf("%4s%08x", token, val))
	return data, nil
}

// return token int value, data offset and error
func readTokenInt(data []byte, token string) (int, int, error) {
	if len(data) < protocol.TOKENBUFLEN {
		return 0, 0, fmt.Errorf("data length is invalid")
	}

	if !bytes.HasPrefix(data, []byte(token)) {
		return 0, 0, fmt.Errorf("data has not start with %s", token)
	}

	// check int value
	val, err := strconv.ParseInt(string(data[protocol.TOKENLEN:protocol.TOKENBUFLEN]), 16, 64)
	if err != nil {
		err := fmt.Errorf("not found valid int val")
		blog.Errorf("read token int error: [%s]", err.Error())
		return 0, 0, err
	}

	return int(val), protocol.TOKENBUFLEN, nil
}

func fillOneFile(filefullpath string, compresstype protocol.PBCompressType) (protocol.Message, int64, error) {
	message := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         nil,
		Compresstype: protocol.CompressNone,
	}
	data, err := ioutil.ReadFile(filefullpath)
	if err != nil {
		blog.Warnf("failed to read file[%s] with err:%v", filefullpath, err)
		return message, 0, err
	}

	datalen := len(data)
	if datalen == 0 {
		blog.Debugf("file %s is empty", filefullpath)
		return message, 0, nil
	}

	var outdata []byte
	var outlen int
	// if compresstype == protocol.PBCompressType_LZO {
	// 	// compress with lzox1 firstly
	// 	outdata = golzo.Compress1X(data)
	// 	outlen = len(outdata)
	// 	blog.Infof("file %s compressed with lzo1x, from [%d] to [%d]", filefullpath, datalen, outlen)
	// } else
	if compresstype == protocol.PBCompressType_LZ4 {
		// compress with lz4 firstly
		outdata, _ = dcUtil.Lz4Compress(data)
		outlen = len(outdata)
		blog.Infof("file %s compressed with lz4, from [%d] to [%d]", filefullpath, datalen, outlen)
	} else if compresstype == protocol.PBCompressType_NONE {
		outdata = data
		outlen = datalen
		blog.Infof("file %s not compressed and length [%d]", filefullpath, outlen)
	} else {
		return message, 0, fmt.Errorf("not support compress type %s now", compresstype.String())
	}

	message.Data = outdata
	return message, int64(outlen), nil
}

// EncodeBKCommonDispatchRsp encode results to Messages
func EncodeBKCommonDispatchRsp(results []*protocol.PBResult) ([]protocol.Message, error) {
	blog.Debugf("encode bk-common-disptach request to message now")

	// save files
	filemessages := make([]protocol.Message, 0, 0)
	var filebuflen int64

	// encode body and file to message
	pbbody := protocol.PBBodyDispatchTaskRsp{
		Results: results,
	}
	for _, v := range results {
		for i, f := range v.Resultfiles {
			//
			fields := strings.Split(f.GetFullpath(), types.FileConnectFlag)
			realfile := f.GetFullpath()
			if len(fields) == 2 {
				realfile = fields[1]
			}
			m, compressedsize, err := fillOneFile(realfile, f.GetCompresstype())
			if err != nil {
				blog.Warnf("failed to encode message for file %s with err:%v", f.GetFullpath(), err)
				// for this condition, we will set data to 0
				// continue
				compressedsize = 0
			}

			v.Resultfiles[i].Compressedsize = &compressedsize
			v.Resultfiles[i].Fullpath = &fields[0]

			filemessages = append(filemessages, m)
			filebuflen += compressedsize
		}
	}

	bodydata, err := proto.Marshal(&pbbody)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbbody for error: %v", err)
		return nil, err
	}
	bodymessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         bodydata,
		Compresstype: protocol.CompressNone,
	}
	bodylen := int32(pbbody.XXX_Size())
	blog.Infof("encode body to size %d", bodylen)

	// encode head
	cmdtype := protocol.PBCmdType_DISPATCHTASKRSP
	pbhead := protocol.PBHead{
		Version: &bkdistcmdversion,
		Magic:   &bkdistcmdmagic,
		Bodylen: &bodylen,
		Buflen:  &filebuflen,
		Cmdtype: &cmdtype,
	}
	headdata, err := proto.Marshal(&pbhead)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbhead with err:%v", err)
		return nil, err
	}
	blog.Infof("encode head to size %d", pbhead.XXX_Size())

	headtokendata, err := formatTokenInt(protocol.TOEKNHEADFLAG, pbhead.XXX_Size())
	if err != nil {
		blog.Warnf("failed to format head token with err:%v", err)
		return nil, err
	}
	headtokenmessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         headtokendata,
		Compresstype: protocol.CompressNone,
	}

	headmessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         headdata,
		Compresstype: protocol.CompressNone,
	}

	// all messages
	messages := []protocol.Message{
		headtokenmessage,
		headmessage,
		bodymessage,
	}

	if len(filemessages) > 0 {
		messages = append(messages, filemessages...)
	}

	return messages, nil
}

// ReceiveBKCommonHead to receive pb command head
func ReceiveBKCommonHead(client *TCPClient) (*protocol.PBHead, error) {
	blog.Debugf("receive bk-common-disptach head now")

	// receive head token
	data, datalen, err := client.ReadData(protocol.TOKENBUFLEN)
	if err != nil {
		blog.Warnf("failed to receive head token with err:%v", err)
		return nil, err
	}
	blog.Debugf("succeed to recieve head token %s", string(data))

	// resolve head token
	headlen, _, err := readTokenInt(data[0:datalen], protocol.TOEKNHEADFLAG)
	if err != nil {
		blog.Warnf("got invalid head token error:%s", err)
		return nil, err
	}
	if err != nil || headlen <= 0 {
		err := fmt.Errorf("headlen %d is invalid", headlen)
		blog.Warnf("got invalid head token len %d", headlen)
		return nil, err
	}

	// receive head
	data, datalen, err = client.ReadData(int(headlen))
	if err != nil {
		blog.Warnf("failed to receive pbhead error: %v", err)
		return nil, err
	}

	head := protocol.PBHead{}
	err = proto.Unmarshal(data[0:datalen], &head)
	if err != nil {
		blog.Warnf("failed to decode pbhead error: %v", err)
	} else {
		blog.Debugf("succeed to decode pbhead %s", head.String())
		if err := checkHead(&head); err != nil {
			blog.Warnf("failed to check head for error: %v", err)
			return nil, err
		}
	}

	return &head, nil
}

func checkHead(head *protocol.PBHead) error {
	if head.GetMagic() != bkdistcmdmagic {
		return fmt.Errorf("cmd magic [%s] is invalid", head.GetMagic())
	}
	return nil
}

// ReceiveBKCommonDispatchReq to receive pb command body for protocol.PBCmdType_DISPATCHTASKREQ
func ReceiveBKCommonDispatchReq(client *TCPClient,
	head *protocol.PBHead,
	basedir string,
	callback PathMapping,
	c chan<- string) (*protocol.PBBodyDispatchTaskReq, error) {
	blog.Debugf("receive bk-common-disptach body now")

	bodylen := head.GetBodylen()
	buflen := head.GetBuflen()
	if bodylen <= 0 || buflen < 0 {
		err := fmt.Errorf("get invalid body length %d, buf len %d", bodylen, buflen)
		blog.Warnf("%v", err)
		return nil, err
	}

	// receive body
	data, datalen, err := client.ReadData(int(bodylen))
	if err != nil {
		blog.Warnf("failed to receive pbbody with err:%v", err)
		return nil, err
	}

	// TODO : should by cmd type here
	body := protocol.PBBodyDispatchTaskReq{}
	err = proto.Unmarshal(data[0:datalen], &body)

	if err != nil {
		blog.Warnf("failed to decode pbbody error: %v", err)
	} else {
		blog.Debugf("succeed to decode pbbody [%s]", body.String())
	}

	// receive buf and save to files
	if buflen >= 0 {
		if err := receiveBKCommonDispatchReqBuf(client, &body, buflen, basedir, callback, c); err != nil {
			return nil, err
		}
	}

	return &body, nil
}

func receiveBKCommonDispatchReqBuf(client *TCPClient,
	body *protocol.PBBodyDispatchTaskReq,
	buflen int64,
	basedir string,
	callback PathMapping,
	c chan<- string) error {
	blog.Debugf("receive bk-common-disptach request buf now")

	// receive buf and save to files
	if buflen >= 0 {
		data, datalen, err := client.ReadData(int(buflen))
		if err != nil {
			blog.Warnf("failed to receive pb buf with err:%v", err)
			return err
		}

		var bufstartoffset int64
		var bufendoffset int64
		for _, r := range body.GetCmds() {
			for _, rf := range r.GetInputfiles() {
				// do not need data buf for empty file
				if rf.GetCompressedsize() <= 0 {
					_, _ = saveFile(rf, nil, basedir, callback, c)
					continue
				}

				compressedsize := rf.GetCompressedsize()
				if compressedsize > int64(datalen)-bufendoffset {
					blog.Warnf("not enought buf data for file [%s]", rf.String())
					return err
				}
				bufstartoffset = bufendoffset
				bufendoffset += compressedsize
				realfilepath := ""
				if realfilepath, err = saveFile(
					rf, data[bufstartoffset:bufendoffset], basedir, callback, c); err != nil {
					blog.Warnf("failed to save file [%s], err:%v", rf.String(), err)
					return err
				}

				blog.Debugf("succeed to save file:%s with client:%s", rf.GetFullpath(), client.RemoteAddr())

				// check md5
				srcMd5 := rf.GetMd5()
				if srcMd5 != "" {
					curMd5, _ := dcFile.Stat(realfilepath).Md5()
					blog.Infof("md5:%s for file:%s", curMd5, realfilepath)
					if srcMd5 != curMd5 {
						blog.Warnf("failed to save file [%s], srcMd5:%s,curmd5:%s", rf.String(), srcMd5, curMd5)
						return err
					}
				}
			}
		}
	}

	return nil
}

// for size < 0, only get file path mapping
// for size == 0, save one empty file
func saveFile(
	rf *protocol.PBFileDesc,
	data []byte,
	basedir string,
	callback PathMapping,
	c chan<- string) (inputfile string, err error) {
	blog.Debugf("ready save file: %s, relative path: %s, link path: %s",
		rf.GetFullpath(), rf.GetTargetrelativepath(), rf.GetLinktarget())

	inputfile = rf.GetFullpath()
	relativepath := rf.GetTargetrelativepath()
	linkTarget := string(rf.GetLinktarget())
	if inputfile == "" {
		blog.Warnf("file [%s] path is empty!", rf.String())
		return "", fmt.Errorf("file path is empty")
	}

	if callback != nil {
		relativepath, inputfile, _ = callback(inputfile, basedir, rf.GetTargetrelativepath())
		if linkTarget != "" && !filepath.IsAbs(linkTarget) {
			_, linkTarget, _ = callback(linkTarget, basedir, rf.GetTargetrelativepath())
		}
		rf.Targetrelativepath = &relativepath
	}

	// compressed size < 0 means do not save, or may will overwrite existing files.
	if size := rf.GetCompressedsize(); size < 0 {
		blog.Debugf("get compressed size for [%s] < 0: %d", inputfile, size)
		return inputfile, nil
	}

	// symlink should just make a link by force
	if linkTarget != "" {
		inputDir := filepath.Dir(inputfile)
		if err = os.MkdirAll(inputDir, os.ModePerm); err != nil && !os.IsExist(err) {
			blog.Warnf("create dir %s before create input symlink %s failed: %v", inputDir, inputfile, err)
			return "", err
		}
		if err = os.Symlink(linkTarget, inputfile); err != nil && !os.IsExist(err) {
			blog.Errorf("create input symlink %s -> %s error: [%s]", inputfile, linkTarget, err.Error())
			return "", err
		}

		// if the link is a dir, then we should ensure the target exist.
		if os.FileMode(rf.GetFilemode()).IsDir() {
			_ = os.MkdirAll(linkTarget, os.ModePerm)
		}
		blog.Debugf("succeed to save symlink file %s -> %s", inputfile, linkTarget)
		c <- inputfile

		return inputfile, nil
	}

	// dir should mkdir instead of create file
	if os.FileMode(rf.GetFilemode()).IsDir() {
		err = os.MkdirAll(inputfile, os.ModePerm)
		if err != nil && !os.IsExist(err) {
			blog.Errorf("create input dir %s error: [%s]", inputfile, err.Error())
			return "", err
		}
		blog.Debugf("succeed to save dir file %s", inputfile)
		c <- inputfile

		return inputfile, nil
	}

	dir := filepath.Dir(inputfile)
	err = os.MkdirAll(dir, os.ModePerm)
	if err != nil && !os.IsExist(err) {
		blog.Errorf("create dir %s error: [%s]", dir, err.Error())
		return "", err
	}

	blog.Debugf("succeed to create dir %s", dir)

	// we can't overwrite if file is readonly or running
	// save as temp file if existed, then change name after saved
	existed := dcFile.Stat(inputfile).Exist()
	targetname := inputfile
	targetbakname := fmt.Sprintf("%s_%d", targetname, time.Now().UnixNano())
	tempname := fmt.Sprintf("%s_temp", targetbakname)
	if existed {
		inputfile = tempname
		blog.Infof("ready save file %s", inputfile)
	}
	newfilesaved := false

	f, err := os.Create(inputfile)
	if err != nil {
		blog.Errorf("create file %s error: [%s]", inputfile, err.Error())
		return "", err
	}
	defer func() {
		_ = f.Close()

		if existed && newfilesaved {
			blog.Infof("rename existed file %s to %s", targetname, targetbakname)
			err = os.Rename(targetname, targetbakname)
			if err != nil {
				blog.Infof("failed to rename existed file %s to %s with error:%v",
					targetname, targetbakname, err)
			}

			// !!! ensure rename tempname after f.Close(), to avoid other error
			blog.Infof("rename new file %s to %s", tempname, targetname)
			err = os.Rename(tempname, targetname)
			if err != nil {
				blog.Infof("failed to rename new file %s to %s with error:%v", tempname, targetname, err)
			}

			inputfile = targetname
		}

		// !! set file attribute after rename
		// TODO : change filemode and modifytime if new file created here
		if newfilesaved {
			filemode := rf.GetFilemode()
			if filemode > 0 {
				blog.Infof("get file[%s] filemode[%d]", inputfile, filemode)
				if err = os.Chmod(inputfile, os.FileMode(filemode)); err != nil {
					blog.Warnf("chmod file %s to file-mode %s failed: %v", inputfile, os.FileMode(filemode), err)
				}
			}

			modifytime := rf.GetModifytime()
			if modifytime > 0 {
				blog.Infof("get file[%s] modify time [%d]", inputfile, modifytime)
				if err = os.Chtimes(inputfile, time.Now(), time.Unix(0, modifytime)); err != nil {
					blog.Warnf("Chtimes file %s to time %s failed: %v", inputfile, time.Unix(0, modifytime), err)
				}
			}
		}
	}()

	if rf.GetCompressedsize() > 0 {
		switch rf.GetCompresstype() {
		case protocol.PBCompressType_NONE:
			_, err := f.Write(data)
			if err != nil {
				blog.Errorf("save file [%s] error: [%s]", inputfile, err.Error())
				return "", err
			}

			// filemode := rf.GetFilemode()
			// if filemode > 0 {
			// 	blog.Infof("get file[%s] filemode[%d]", inputfile, filemode)
			// 	_ = os.Chmod(inputfile, os.FileMode(filemode))
			// }
			break
		// case protocol.PBCompressType_LZO:
		// 	// decompress with lzox1 firstly
		// 	outdata, err := golzo.Decompress1X(bytes.NewReader(data), int(rf.GetCompressedsize()), 0)
		// 	if err != nil {
		// 		blog.Errorf("decompress error: [%s]", err.Error())
		// 		return "", err
		// 	}
		// 	outlen := len(string(outdata))
		// 	blog.Debugf("decompressed with lzo1x, from [%d] to [%d]", rf.GetCompressedsize(), outlen)
		// 	if outlen != int(rf.GetSize()) {
		// 		err := fmt.Errorf("decompressed size %d, expected size %d", outlen, rf.GetSize())
		// 		blog.Errorf("decompress error: [%v]", err)
		// 		return "", err
		// 	}

		// 	_, err = f.Write(outdata)
		// 	if err != nil {
		// 		blog.Errorf("save file [%s] error: [%v]", inputfile, err)
		// 		return "", err
		// 	}

		// 	filemode := rf.GetFilemode()
		// 	if filemode > 0 {
		// 		blog.Infof("get file[%s] filemode[%d]", inputfile, filemode)
		// 		_ = os.Chmod(inputfile, os.FileMode(filemode))
		// 	}
		// 	break
		case protocol.PBCompressType_LZ4:
			// decompress with lz4 firstly
			dst := make([]byte, rf.GetSize())
			if dst == nil {
				err = fmt.Errorf("failed to alloc [%d] size buffer", rf.GetSize())
				blog.Errorf("%v", err)
				return "", err
			}
			outdata, err := dcUtil.Lz4Uncompress(data, dst)
			if err != nil {
				blog.Errorf("decompress [%s] error: [%s], data len:[%d], buffer len:[%d], filesize:[%d]",
					rf.GetFullpath(), err.Error(), len(data), len(dst), rf.GetSize())
				return "", err
			}

			// !!! here len will return int, not int64, so it will be wrong with file greate than 4G(max int)
			// outlen := len(string(outdata))
			outlen := len(outdata)
			blog.Debugf("decompressed with lz4, from [%d] to [%d]", rf.GetCompressedsize(), outlen)
			if int64(outlen) != rf.GetSize() {
				err := fmt.Errorf("decompressed size %d, expected size %d", outlen, rf.GetSize())
				blog.Errorf("decompress error: [%v]", err)
				return "", err
			}

			_, err = f.Write(outdata)
			if err != nil {
				blog.Errorf("save file [%s] error: [%v]", inputfile, err)
				return "", err
			}

			// filemode := rf.GetFilemode()
			// if filemode > 0 {
			// 	blog.Infof("get file[%s] filemode[%d]", inputfile, filemode)
			// 	if err = os.Chmod(inputfile, os.FileMode(filemode)); err != nil {
			// 		blog.Warnf("chmod file %s to file-mode %s failed: %v", inputfile, os.FileMode(filemode), err)
			// 	}
			// }
			break
		default:
			return "", fmt.Errorf("unknown compress type [%s]", rf.GetCompresstype())
		}
	}

	blog.Debugf("succeed to save file %s", inputfile)
	newfilesaved = true
	c <- inputfile

	return inputfile, nil
}

// EncodeBKSyncTimeRsp encode time to Messages
func EncodeBKSyncTimeRsp(receivedtime time.Time) ([]protocol.Message, error) {
	blog.Debugf("encode synctime request to message now")

	// encode body and file to message
	receivedtime64 := receivedtime.UnixNano()
	pbbody := protocol.PBBodySyncTimeRsp{
		Timenanosecond: &receivedtime64,
	}

	bodydata, err := proto.Marshal(&pbbody)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbbody for error: %v", err)
		return nil, err
	}
	bodymessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         bodydata,
		Compresstype: protocol.CompressNone,
	}
	bodylen := int32(pbbody.XXX_Size())
	blog.Infof("encode body to size %d", bodylen)

	// encode head
	var filebuflen int64
	cmdtype := protocol.PBCmdType_SYNCTIMERSP
	pbhead := protocol.PBHead{
		Version: &bkdistcmdversion,
		Magic:   &bkdistcmdmagic,
		Bodylen: &bodylen,
		Buflen:  &filebuflen,
		Cmdtype: &cmdtype,
	}
	headdata, err := proto.Marshal(&pbhead)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbhead with err:%v", err)
		return nil, err
	}
	blog.Infof("encode head to size %d", pbhead.XXX_Size())

	headtokendata, err := formatTokenInt(protocol.TOEKNHEADFLAG, pbhead.XXX_Size())
	if err != nil {
		blog.Warnf("failed to format head token with err:%v", err)
		return nil, err
	}
	headtokenmessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         headtokendata,
		Compresstype: protocol.CompressNone,
	}

	headmessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         headdata,
		Compresstype: protocol.CompressNone,
	}

	// all messages
	messages := []protocol.Message{
		headtokenmessage,
		headmessage,
		bodymessage,
	}

	return messages, nil
}

// EncodeBKCheckCacheRsp encode result to Messages
func EncodeBKCheckCacheRsp(result []*protocol.PBCacheResult) ([]protocol.Message, error) {
	blog.Debugf("encode synctime request to message now")

	// encode result to message
	pbbody := protocol.PBBodyCheckCacheRsp{
		Results: result,
	}

	bodydata, err := proto.Marshal(&pbbody)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbbody for error: %v", err)
		return nil, err
	}
	bodymessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         bodydata,
		Compresstype: protocol.CompressNone,
	}
	bodylen := int32(pbbody.XXX_Size())
	blog.Infof("encode body to size %d", bodylen)

	// encode head
	var filebuflen int64
	cmdtype := protocol.PBCmdType_CHECKCACHERSP
	pbhead := protocol.PBHead{
		Version: &bkdistcmdversion,
		Magic:   &bkdistcmdmagic,
		Bodylen: &bodylen,
		Buflen:  &filebuflen,
		Cmdtype: &cmdtype,
	}
	headdata, err := proto.Marshal(&pbhead)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbhead with err:%v", err)
		return nil, err
	}
	blog.Infof("encode head to size %d", pbhead.XXX_Size())

	headtokendata, err := formatTokenInt(protocol.TOEKNHEADFLAG, pbhead.XXX_Size())
	if err != nil {
		blog.Warnf("failed to format head token with err:%v", err)
		return nil, err
	}
	headtokenmessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         headtokendata,
		Compresstype: protocol.CompressNone,
	}

	headmessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         headdata,
		Compresstype: protocol.CompressNone,
	}

	// all messages
	messages := []protocol.Message{
		headtokenmessage,
		headmessage,
		bodymessage,
	}

	return messages, nil
}

// EncodeBKUnknownRsp encode time to Messages
func EncodeBKUnknownRsp(_ time.Time) ([]protocol.Message, error) {
	blog.Debugf("encode unknown request to message now")

	// encode head
	var bodylen int32
	var filebuflen int64
	cmdtype := protocol.PBCmdType_UNKNOWN
	pbhead := protocol.PBHead{
		Version: &bkdistcmdversion,
		Magic:   &bkdistcmdmagic,
		Bodylen: &bodylen,
		Buflen:  &filebuflen,
		Cmdtype: &cmdtype,
	}
	headdata, err := proto.Marshal(&pbhead)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbhead with err:%v", err)
		return nil, err
	}
	blog.Infof("encode head to size %d", pbhead.XXX_Size())

	headtokendata, err := formatTokenInt(protocol.TOEKNHEADFLAG, pbhead.XXX_Size())
	if err != nil {
		blog.Warnf("failed to format head token with err:%v", err)
		return nil, err
	}
	headtokenmessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         headtokendata,
		Compresstype: protocol.CompressNone,
	}

	headmessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         headdata,
		Compresstype: protocol.CompressNone,
	}

	// all messages
	messages := []protocol.Message{
		headtokenmessage,
		headmessage,
	}

	return messages, nil
}

// SendMessages send messages back to client
func SendMessages(client *TCPClient, messages *[]protocol.Message) error {
	blog.Infof("ready send messages with remote %s", client.RemoteAddr())

	if messages == nil || len(*messages) == 0 {
		return fmt.Errorf("data to send is empty")
	}

	for _, v := range *messages {
		if v.Data == nil {
			blog.Warnf("found nil data when ready send bk-common dist request")
			continue
		}

		switch v.Messagetype {
		case protocol.MessageString:
			if err := client.WriteData(v.Data); err != nil {
				return err
			}
		case protocol.MessageFile:
			if err := client.SendFile(string(v.Data), v.Compresstype); err != nil {
				return err
			}
		default:
			return fmt.Errorf("unknown message type %s", v.Messagetype.String())
		}
	}

	return nil
}

// ReceiveBKSendFile to receive pb command body for send file
func ReceiveBKSendFile(client *TCPClient,
	head *protocol.PBHead,
	basedir string,
	callback PathMapping,
	c chan<- string,
	cm cache.Manager) (*protocol.PBBodySendFileReq, error) {
	blog.Debugf("receive send file body now")

	bodylen := head.GetBodylen()
	buflen := head.GetBuflen()
	if bodylen <= 0 || buflen < 0 {
		err := fmt.Errorf("get invalid body length %d, buf len %d", bodylen, buflen)
		blog.Warnf("%v", err)
		return nil, err
	}

	// receive body
	data, datalen, err := client.ReadData(int(bodylen))
	if err != nil {
		blog.Warnf("failed to receive pbbody with err:%v", err)
		return nil, err
	}

	// TODO : should by cmd type here
	body := protocol.PBBodySendFileReq{}
	err = proto.Unmarshal(data[0:datalen], &body)

	if err != nil {
		blog.Warnf("failed to decode pbbody error: %v", err)
	} else {
		blog.Debugf("succeed to decode pbbody")
	}

	// receive buf and save to files
	if buflen > 0 {
		if err := receiveBKSendFileBuf(client, &body, buflen, basedir, callback, c, cm); err != nil {
			return nil, err
		}
	}

	return &body, nil
}

func receiveBKSendFileBuf(client *TCPClient,
	body *protocol.PBBodySendFileReq,
	buflen int64,
	basedir string,
	callback PathMapping,
	c chan<- string,
	cm cache.Manager) error {
	blog.Debugf("receive send file request buf now")

	// receive buf and save to files
	if buflen > 0 {
		data, datalen, err := client.ReadData(int(buflen))
		if err != nil {
			blog.Warnf("failed to receive pb buf with err:%v", err)
			return err
		}

		var bufstartoffset int64
		var bufendoffset int64

		for _, rf := range body.GetInputfiles() {
			// do not need data buf for empty file
			if rf.GetCompressedsize() <= 0 {
				_, _ = saveFile(rf, nil, basedir, callback, c)
				continue
			}

			compressedsize := rf.GetCompressedsize()
			if compressedsize > int64(datalen)-bufendoffset {
				blog.Warnf("not enought buf data for file [%s]", rf.String())
				return err
			}
			bufstartoffset = bufendoffset
			bufendoffset += compressedsize
			realfilepath := ""
			if realfilepath, err = saveFile(rf, data[bufstartoffset:bufendoffset], basedir, callback, c); err != nil {
				blog.Warnf("failed to save file [%s], err:%v", rf.String(), err)
				return err
			}

			blog.Debugf("succeed to save file:%s with client:%s", rf.GetFullpath(), client.RemoteAddr())

			// check md5
			srcMd5 := rf.GetMd5()
			if srcMd5 != "" {
				curMd5, _ := dcFile.Stat(realfilepath).Md5()
				blog.Infof("md5:%s for file:%s", curMd5, realfilepath)
				if srcMd5 != curMd5 {
					blog.Warnf("failed to save file [%s], srcMd5:%s,curmd5:%s", rf.String(), srcMd5, curMd5)
					return err
				}
			}

			// try store file to cache
			go storeFile2Cache(cm, realfilepath)
		}
	}

	return nil
}

func storeFile2Cache(cm cache.Manager, realFilePath string) {
	if cm == nil {
		return
	}

	f, err := cache.NewFile(realFilePath)
	if err != nil {
		blog.Warnf("failed to get new file [%s] for cache manager: %v", realFilePath, err)
		return
	}

	err = cm.Store(f)
	if err == cache.ErrFileNoNeedStore {
		blog.Infof("file [%s] no need to be cached", realFilePath)
		return
	}

	if err != nil {
		blog.Warnf("failed to store new file [%s] failed: %v", realFilePath, err)
		return
	}

	blog.Infof("success to store new file [%s] to cache", realFilePath)
}

// EncodeBKSendFileRsp encode results to Messages
func EncodeBKSendFileRsp(req *protocol.PBBodySendFileReq) ([]protocol.Message, error) {
	blog.Debugf("encode send file response to message now")

	var filebuflen int64

	// encode body and file to message
	pbbody := protocol.PBBodySendFileRsp{}
	for _, v := range req.GetInputfiles() {
		//
		fields := strings.Split(v.GetFullpath(), types.FileConnectFlag)
		realfile := v.GetFullpath()
		if len(fields) == 2 {
			realfile = fields[1]
		}

		var retcode int32
		targetrelativepath := v.GetTargetrelativepath()
		pbbody.Results = append(pbbody.Results, &protocol.PBFileResult{
			Fullpath:           &realfile,
			Retcode:            &retcode,
			Targetrelativepath: &targetrelativepath,
		})
	}

	bodydata, err := proto.Marshal(&pbbody)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbbody for error: %v", err)
		return nil, err
	}
	bodymessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         bodydata,
		Compresstype: protocol.CompressNone,
	}
	bodylen := int32(pbbody.XXX_Size())
	blog.Infof("encode body to size %d", bodylen)

	// encode head
	cmdtype := protocol.PBCmdType_SENDFILERSP
	pbhead := protocol.PBHead{
		Version: &bkdistcmdversion,
		Magic:   &bkdistcmdmagic,
		Bodylen: &bodylen,
		Buflen:  &filebuflen,
		Cmdtype: &cmdtype,
	}
	headdata, err := proto.Marshal(&pbhead)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbhead with err:%v", err)
		return nil, err
	}
	blog.Infof("encode head to size %d", pbhead.XXX_Size())

	headtokendata, err := formatTokenInt(protocol.TOEKNHEADFLAG, pbhead.XXX_Size())
	if err != nil {
		blog.Warnf("failed to format head token with err:%v", err)
		return nil, err
	}
	headtokenmessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         headtokendata,
		Compresstype: protocol.CompressNone,
	}

	headmessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         headdata,
		Compresstype: protocol.CompressNone,
	}

	// all messages
	messages := []protocol.Message{
		headtokenmessage,
		headmessage,
		bodymessage,
	}

	return messages, nil
}

// ReceiveBKCheckCache receive check cache request and generate the body
func ReceiveBKCheckCache(client *TCPClient,
	head *protocol.PBHead,
	_ string,
	_ PathMapping,
	_ chan<- string) (*protocol.PBBodyCheckCacheReq, error) {
	blog.Debugf("receive check cache body now")

	bodylen := head.GetBodylen()
	buflen := head.GetBuflen()
	if bodylen <= 0 || buflen < 0 {
		err := fmt.Errorf("get invalid body length %d, buf len %d", bodylen, buflen)
		blog.Warnf("%v", err)
		return nil, err
	}

	// receive body
	data, datalen, err := client.ReadData(int(bodylen))
	if err != nil {
		blog.Warnf("failed to receive pbbody with err:%v", err)
		return nil, err
	}

	// TODO : should by cmd type here
	body := protocol.PBBodyCheckCacheReq{}
	err = proto.Unmarshal(data[0:datalen], &body)

	if err != nil {
		blog.Warnf("failed to decode pbbody error: %v", err)
	} else {
		blog.Debugf("succeed to decode pbbody")
	}

	return &body, nil
}

// ReceiveUnknown to receive pb command body for unknown cmd
func ReceiveUnknown(client *TCPClient,
	head *protocol.PBHead,
	_ string,
	_ PathMapping) error {
	blog.Debugf("receive body for unknow cmd now")

	bodylen := head.GetBodylen()
	buflen := head.GetBuflen()

	// receive body
	if bodylen > 0 {
		_, _, err := client.ReadData(int(bodylen))
		if err != nil {
			return err
		}
	}

	// receive buf
	if buflen > 0 {
		_, _, err := client.ReadData(int(buflen))
		if err != nil {
			return err
		}
	}

	return nil
}
