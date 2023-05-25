/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package client

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/common/blog"

	"github.com/gogo/protobuf/proto"
)

// define const vars
var (
	bkdistcmdversion = protocol.Bkdistcmdversion
	bkdistcmdmagic   = protocol.Bkdistcmdmagic
)

func bkformatTokenInt(token string, val int) ([]byte, error) {
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
func bkreadTokenInt(data []byte, token string) (int, int, error) {
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

func receiveCommonHead(client *TCPClient) (*protocol.PBHead, error) {
	blog.Debugf("receive bk-common-disptach head now")

	// receive head token
	data, datalen, err := client.ReadData(protocol.TOKENBUFLEN)
	if err != nil {
		blog.Warnf("failed to receive head token")
		return nil, err
	}
	blog.Debugf("succeed to recieve head token %s", string(data))

	// resolve head token
	headlen, _, err := bkreadTokenInt(data[0:datalen], protocol.TOEKNHEADFLAG)
	if err != nil {
		blog.Warnf("failed to get head token with error:%s", err)
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

func encodeSynctimeReq() ([]protocol.Message, error) {
	blog.Debugf("encode sync time request to message now")

	// encode head
	var bodylen int32
	var filebuflen int64
	cmdtype := protocol.PBCmdType_SYNCTIMEREQ
	pbhead := protocol.PBHead{
		Version: &bkdistcmdversion,
		Magic:   &bkdistcmdmagic,
		Bodylen: &bodylen,
		Buflen:  &filebuflen,
		Cmdtype: &cmdtype,
	}
	headdata, err := proto.Marshal(&pbhead)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbhead")
		return nil, err
	}
	blog.Debugf("encode head[%s] to size %d", pbhead.String(), pbhead.XXX_Size())

	headtokendata, err := bkformatTokenInt(protocol.TOEKNHEADFLAG, pbhead.XXX_Size())
	if err != nil {
		blog.Warnf("failed to format head token")
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

func receiveSynctimeRsp(client *TCPClient) (*protocol.PBBodySyncTimeRsp, error) {
	blog.Debugf("receive bk-common-disptach response now")

	// receive head
	head, err := receiveCommonHead(client)
	if err != nil {
		return nil, err
	}

	if head.GetCmdtype() != protocol.PBCmdType_SYNCTIMERSP {
		err := fmt.Errorf("unknown cmd type %v", head.GetCmdtype())
		blog.Warnf("%v", err)
		return nil, err
	}

	bodylen := head.GetBodylen()
	if bodylen <= 0 {
		err := fmt.Errorf("get invalid body length %d", bodylen)
		blog.Warnf("%v", err)
		return nil, err
	}
	blog.Debugf("got bodylen %d", bodylen)

	// receive body
	data, datalen, err := client.ReadData(int(bodylen))
	if err != nil {
		blog.Warnf("failed to receive pbbody")
		return nil, err
	}

	body := protocol.PBBodySyncTimeRsp{}
	err = proto.Unmarshal(data[0:datalen], &body)

	if err != nil {
		blog.Warnf("failed to decode pbbody error: %v", err)
	} else {
		blog.Debugf("succeed to decode pbbody ")
	}

	return &body, nil
}

func fillOneFile(
	filefullpath string,
	buffer []byte,
	compresstype protocol.CompressType,
	fileMode uint32) (protocol.Message, int64, error) {
	message := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         nil,
		Compresstype: protocol.CompressNone,
	}

	var data []byte
	var err error

	// if buffer is not nil, means it is the content read from file, do not read again
	if buffer != nil {
		data = buffer
	} else {
		// if file is a symlink, then create it in remote
		if os.FileMode(fileMode)&os.ModeSymlink != 0 || os.FileMode(fileMode).IsDir() {
			data = []byte("EMPTY")
		} else {
			data, err = ioutil.ReadFile(filefullpath)
			if err != nil {
				blog.Warnf("failed to read file[%s]", filefullpath)
				return message, 0, err
			}
		}
	}

	datalen := len(data)
	if datalen == 0 {
		blog.Debugf("file %s is empty", filefullpath)
		return message, 0, nil
	}

	var outdata []byte
	var outlen int
	// if compresstype == protocol.CompressLZO {
	// 	// compress with lzox1 firstly
	// 	outdata = golzo.Compress1X(data)
	// 	outlen = len(outdata)
	// 	blog.Debugf("file %s compressed with lzo1x, from [%d] to [%d]", filefullpath, datalen, outlen)
	// } else
	if compresstype == protocol.CompressLZ4 {
		// compress with lz4 firstly
		outdata, _ = dcUtil.Lz4Compress(data)
		outlen = len(outdata)
		blog.Debugf("file %s compressed with lz4, from [%d] to [%d]", filefullpath, datalen, outlen)
	} else if compresstype == protocol.CompressNone {
		outdata = data
		outlen = datalen
		blog.Debugf("file %s not compressed and length [%d]", filefullpath, outlen)
	} else {
		return message, 0, fmt.Errorf("not support compress type %s now", compresstype.String())
	}

	message.Data = outdata
	return message, int64(outlen), nil
}

func encodeCommonDispatchReq(req *dcSDK.BKDistCommand) ([]protocol.Message, error) {
	blog.Debugf("encode bk-common-disptach request to message now")

	// save files
	filemessages := make([]protocol.Message, 0)
	var filebuflen int64

	checkMd5 := false
	if env.GetEnv(env.KeyCommonCheckMd5) == "true" {
		checkMd5 = true
	}

	// encode body and file to message
	pbbody := protocol.PBBodyDispatchTaskReq{}
	for _, v := range req.Commands {
		envs := [][]byte{}
		for _, v := range v.Env {
			envs = append(envs, []byte(v))
		}

		pbcommand := protocol.PBCommand{
			Workdir:     &v.WorkDir,
			Exepath:     &v.ExePath,
			Exename:     &v.ExeName,
			Params:      v.Params,
			Resultfiles: v.ResultFiles,
			Env:         envs,
		}
		if len(v.Inputfiles) > 0 {
			for _, f := range v.Inputfiles {
				comprsstype := protocol.PBCompressType_LZ4
				if f.Compresstype == protocol.CompressLZO {
					comprsstype = protocol.PBCompressType_LZO
				} else if f.Compresstype == protocol.CompressNone {
					comprsstype = protocol.PBCompressType_NONE
				}

				fullpath := f.FilePath
				size := f.FileSize
				md5 := f.Md5
				targetrelativepath := f.Targetrelativepath
				filemode := f.Filemode
				linkTarget := f.LinkTarget
				modifytime := f.Lastmodifytime

				if size < 0 {
					pbcommand.Inputfiles = append(pbcommand.Inputfiles, &protocol.PBFileDesc{
						Fullpath:           &fullpath,
						Size:               &size,
						Md5:                &md5,
						Compresstype:       &comprsstype,
						Compressedsize:     &size,
						Targetrelativepath: &targetrelativepath,
						Filemode:           &filemode,
						Linktarget:         []byte(linkTarget),
						Modifytime:         &modifytime,
					})
					continue
				}

				if md5 == "" && checkMd5 {
					md5, _ = dcFile.Stat(fullpath).Md5()
				}

				m, compressedsize, err := fillOneFile(f.FilePath, f.Buffer, f.Compresstype, filemode)
				if err != nil {
					blog.Warnf("failed to encode message for file", f.FilePath)
					continue
				}

				pbcommand.Inputfiles = append(pbcommand.Inputfiles, &protocol.PBFileDesc{
					Fullpath:           &fullpath,
					Size:               &size,
					Md5:                &md5,
					Compresstype:       &comprsstype,
					Compressedsize:     &compressedsize,
					Targetrelativepath: &targetrelativepath,
					Filemode:           &filemode,
					Linktarget:         []byte(linkTarget),
					Modifytime:         &modifytime,
				})

				filemessages = append(filemessages, m)
				filebuflen += compressedsize
			}
		}

		pbbody.Cmds = append(pbbody.Cmds, &pbcommand)
	}

	bodydata, err := proto.Marshal(&pbbody)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbbody")
		return nil, err
	}
	bodymessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         bodydata,
		Compresstype: protocol.CompressNone,
	}
	bodylen := int32(pbbody.XXX_Size())
	blog.Debugf("encode body[%s] to size %d", pbbody.String(), bodylen)

	// encode head
	cmdtype := protocol.PBCmdType_DISPATCHTASKREQ
	pbhead := protocol.PBHead{
		Version: &bkdistcmdversion,
		Magic:   &bkdistcmdmagic,
		Bodylen: &bodylen,
		Buflen:  &filebuflen,
		Cmdtype: &cmdtype,
	}
	headdata, err := proto.Marshal(&pbhead)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbhead")
		return nil, err
	}
	blog.Debugf("encode head[%s] to size %d", pbhead.String(), pbhead.XXX_Size())

	headtokendata, err := bkformatTokenInt(protocol.TOEKNHEADFLAG, pbhead.XXX_Size())
	if err != nil {
		blog.Warnf("failed to format head token")
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

func receiveCommonDispatchRsp(client *TCPClient, sandbox *syscall.Sandbox) (*protocol.PBBodyDispatchTaskRsp, error) {
	blog.Debugf("receive bk-common-disptach response now")

	// receive head
	head, err := receiveCommonHead(client)
	if err != nil {
		return nil, err
	}

	if head.GetCmdtype() != protocol.PBCmdType_DISPATCHTASKRSP {
		err := fmt.Errorf("unknown cmd type %v", head.GetCmdtype())
		blog.Warnf("%v", err)
		return nil, err
	}

	bodylen := head.GetBodylen()
	buflen := head.GetBuflen()
	if bodylen <= 0 || buflen < 0 {
		err := fmt.Errorf("get invalid body length %d, buf len %d", bodylen, buflen)
		blog.Warnf("%v", err)
		return nil, err
	}
	blog.Debugf("got bodylen %d buflen %d", bodylen, buflen)

	// receive body
	data, datalen, err := client.ReadData(int(bodylen))
	if err != nil {
		blog.Warnf("failed to receive pbbody")
		return nil, err
	}

	// TODO : should by cmd type here
	body := protocol.PBBodyDispatchTaskRsp{}
	err = proto.Unmarshal(data[0:datalen], &body)

	if err != nil {
		blog.Warnf("failed to decode pbbody error: %v", err)
	} else {
		blog.Debugf("succeed to decode pbbody ")
	}

	// receive buf and save to files
	if buflen > 0 {
		if err := receiveCommonDispatchRspBuf(client, &body, buflen, sandbox); err != nil {
			return nil, err
		}
	}

	return &body, nil
}

func receiveCommonDispatchRspBuf(
	client *TCPClient,
	body *protocol.PBBodyDispatchTaskRsp,
	buflen int64,
	sandbox *syscall.Sandbox) error {
	blog.Debugf("receive bk-common-disptach response buf now")

	// receive buf and save to files
	if buflen > 0 {
		data, datalen, err := client.ReadData(int(buflen))
		if err != nil {
			blog.Warnf("failed to receive pb buf")
			return err
		}

		checkMd5 := false
		if env.GetEnv(env.KeyCommonCheckMd5) == "true" {
			checkMd5 = true
		}

		var bufstartoffset int64
		var bufendoffset int64
		for _, r := range body.GetResults() {
			for _, rf := range r.GetResultfiles() {
				compressedsize := rf.GetCompressedsize()
				if compressedsize > int64(datalen)-bufendoffset {
					blog.Warnf("not enought buf data for file [%s]", rf.String())
					return err
				}
				bufstartoffset = bufendoffset
				bufendoffset += compressedsize
				if err = saveResultFile(rf, data[bufstartoffset:bufendoffset], sandbox); err != nil {
					blog.Warnf("failed to save file [%s], err:%v", rf.String(), err)
					return err
				}

				srcMd5 := rf.GetMd5()
				if checkMd5 && srcMd5 != "" {
					curMd5, _ := dcFile.Stat(rf.GetFullpath()).Md5()
					if srcMd5 != curMd5 {
						return fmt.Errorf("file:%s, src md5:%s, received md5:%s",
							rf.GetFullpath(), srcMd5, curMd5)
					}
				}
			}
		}
	}

	return nil
}

func saveResultFile(rf *protocol.PBFileDesc, data []byte, sandbox *syscall.Sandbox) error {
	filePath := rf.GetFullpath()
	blog.Debugf("ready save file [%s]", filePath)
	if filePath == "" {
		blog.Warnf("file [%s] path is empty!", filePath)
		return fmt.Errorf("file path is empty")
	}

	// TODO: more clean code
	if !filepath.IsAbs(filePath) {
		filePath = filepath.Join(sandbox.Dir, filePath)
	}

	if strings.HasSuffix(filePath, ".gcno") && rf.GetSize() == 0 {
		blog.Debugf("empty gcno file:[%s] , not save", rf.GetFullpath())
		return nil
	}

	_ = os.MkdirAll(filepath.Dir(filePath), os.ModePerm)

	creatTime1 := time.Now().Local().UnixNano()
	f, err := os.Create(filePath)
	if err != nil {
		blog.Errorf("create file %s error: [%s]", filePath, err.Error())
		return err
	}
	creatTime2 := time.Now().Local().UnixNano()

	startTime := time.Now().Local().UnixNano()
	var allocTime int64
	var compressTime int64
	defer func() {

		endTime := time.Now().Local().UnixNano()
		blog.Debugf("[iotest] file [%s] srcsize [%d] compresssize [%d] createTime [%d] allocTime [%d] "+
			"uncpmpresstime [%d] savetime [%d] millionseconds",
			filePath,
			rf.GetSize(),
			rf.GetCompressedsize(),
			(creatTime2-creatTime1)/1000/1000,
			(allocTime-startTime)/1000/1000,
			(compressTime-allocTime)/1000/1000,
			(endTime-compressTime)/1000/1000)

		_ = f.Close()
	}()

	if rf.GetCompressedsize() > 0 {
		switch rf.GetCompresstype() {
		case protocol.PBCompressType_NONE:
			allocTime = time.Now().Local().UnixNano()
			compressTime = allocTime
			_, err := f.Write(data)
			if err != nil {
				blog.Errorf("save file [%s] error: [%s]", filePath, err.Error())
				return err
			}
			break
		// case protocol.PBCompressType_LZO:
		// 	// decompress with lzox1 firstly
		// 	outdata, err := golzo.Decompress1X(bytes.NewReader(data), int(rf.GetCompressedsize()), 0)
		// 	if err != nil {
		// 		blog.Errorf("decompress file %s error: [%s]", filePath, err.Error())
		// 		return err
		// 	}
		// 	outlen := len(string(outdata))
		// 	blog.Debugf("decompressed file %s with lzo1x, from [%d] to [%d]",
		// 		filePath, rf.GetCompressedsize(), outlen)
		// 	if outlen != int(rf.GetSize()) {
		// 		err := fmt.Errorf("decompressed size %d, expected size %d", outlen, rf.GetSize())
		// 		blog.Errorf("decompress error: [%v]", err)
		// 		return err
		// 	}

		// 	_, err = f.Write(outdata)
		// 	if err != nil {
		// 		blog.Errorf("save file [%s] error: [%v]", filePath, err)
		// 		return err
		// 	}
		// 	break
		case protocol.PBCompressType_LZ4:
			// decompress with lz4 firstly
			dst := make([]byte, rf.GetSize())
			if dst == nil {
				err := fmt.Errorf("failed to alloc [%d] size buffer", rf.GetSize())
				blog.Errorf("%v", err)
				return err
			}

			allocTime = time.Now().Local().UnixNano()
			outdata, err := dcUtil.Lz4Uncompress(data, dst)
			if err != nil {
				blog.Errorf("decompress [%s] error: [%s], data len:[%d], buffer len:[%d], filesize:[%d]",
					filePath, err.Error(), len(data), len(dst), rf.GetSize())
				return err
			}
			compressTime = time.Now().Local().UnixNano()
			// outlen := len(string(outdata))
			outlen := len(outdata)
			blog.Debugf("decompressed file %s with lz4, from [%d] to [%d]",
				filePath, rf.GetCompressedsize(), outlen)
			if outlen != int(rf.GetSize()) {
				err := fmt.Errorf("decompressed size %d, expected size %d", outlen, rf.GetSize())
				blog.Errorf("decompress error: [%v]", err)
				return err
			}

			_, err = f.Write(outdata)
			if err != nil {
				blog.Errorf("save file [%s] error: [%v]", filePath, err)
				return err
			}
			break
		default:
			return fmt.Errorf("unknown compress type [%s]", rf.GetCompresstype())
		}
	}

	blog.Debugf("succeed to save file [%s]", filePath)
	return nil
}

func getCompresstype(t protocol.PBCompressType) protocol.CompressType {
	switch t {
	case protocol.PBCompressType_NONE:
		return protocol.CompressNone
	case protocol.PBCompressType_LZO:
		return protocol.CompressLZO
	case protocol.PBCompressType_LZ4:
		return protocol.CompressLZ4
	default:
		return protocol.CompressUnknown
	}
}

func decodeCommonDispatchRsp(data *protocol.PBBodyDispatchTaskRsp) (*dcSDK.BKDistResult, error) {
	blog.Debugf("decode bk-common-disptach response now")

	results := dcSDK.BKDistResult{}
	for _, r := range data.GetResults() {
		resultFiles := make([]dcSDK.FileDesc, 0)
		for _, rf := range r.GetResultfiles() {
			resultFiles = append(resultFiles, dcSDK.FileDesc{
				FilePath:           rf.GetFullpath(),
				FileSize:           rf.GetSize(),
				Md5:                rf.GetMd5(),
				Compresstype:       getCompresstype(rf.GetCompresstype()),
				Buffer:             rf.GetBuffer(),
				CompressedSize:     rf.GetCompressedsize(),
				Targetrelativepath: rf.GetTargetrelativepath(),
			})
		}
		result := dcSDK.Result{
			RetCode:       r.GetRetcode(),
			OutputMessage: []byte(r.GetOutputmessage()),
			ErrorMessage:  []byte(r.GetErrormessage()),
			ResultFiles:   resultFiles,
		}
		results.Results = append(results.Results, result)
	}

	return &results, nil
}

func receiveCommonDispatchRspWithoutSaveFile(client *TCPClient) (*protocol.PBBodyDispatchTaskRsp, error) {
	blog.Debugf("receive bk-common-disptach response without save file")

	// receive head
	head, err := receiveCommonHead(client)
	if err != nil {
		return nil, err
	}

	if head.GetCmdtype() != protocol.PBCmdType_DISPATCHTASKRSP {
		err := fmt.Errorf("unknown cmd type %v", head.GetCmdtype())
		blog.Warnf("%v", err)
		return nil, err
	}

	bodylen := head.GetBodylen()
	buflen := head.GetBuflen()
	if bodylen <= 0 || buflen < 0 {
		err := fmt.Errorf("get invalid body length %d, buf len %d", bodylen, buflen)
		blog.Warnf("%v", err)
		return nil, err
	}
	blog.Debugf("got bodylen %d buflen %d", bodylen, buflen)

	// receive body
	data, datalen, err := client.ReadData(int(bodylen))
	if err != nil {
		blog.Warnf("failed to receive pbbody")
		return nil, err
	}

	// TODO : should by cmd type here
	body := protocol.PBBodyDispatchTaskRsp{}
	err = proto.Unmarshal(data[0:datalen], &body)

	if err != nil {
		blog.Warnf("failed to decode pbbody error: %v", err)
	} else {
		blog.Debugf("succeed to decode pbbody ")
	}

	// receive buf and save to files
	if buflen > 0 {
		if err := receiveCommonDispatchRspBufWithoutSaveFile(client, &body, buflen); err != nil {
			return nil, err
		}
	}

	return &body, nil
}

func receiveCommonDispatchRspBufWithoutSaveFile(
	client *TCPClient,
	body *protocol.PBBodyDispatchTaskRsp,
	buflen int64) error {
	blog.Debugf("receive bk-common-disptach response buf without save file")

	// receive buf and save to files
	if buflen > 0 {
		data, datalen, err := client.ReadData(int(buflen))
		if err != nil {
			blog.Warnf("failed to receive pb buf")
			return err
		}

		// checkMd5 := false
		// if env.GetEnv(env.KeyCommonCheckMd5) == "true" {
		// 	checkMd5 = true
		// }

		var bufstartoffset int64
		var bufendoffset int64
		for _, r := range body.GetResults() {
			for _, rf := range r.GetResultfiles() {
				compressedsize := rf.GetCompressedsize()
				if compressedsize > int64(datalen)-bufendoffset {
					err := fmt.Errorf("received buf is not complete, expected[%d], left[%d]",
						compressedsize, int64(datalen)-bufendoffset)
					blog.Warnf("%v", err)
					return err
				}
				bufstartoffset = bufendoffset
				bufendoffset += compressedsize
				// if err = saveResultFile(rf, data[bufstartoffset:bufendoffset]); err != nil {
				// 	blog.Warnf("failed to save file [%s], err:%v", rf.String(), err)
				// 	return err
				// }

				// srcMd5 := rf.GetMd5()
				// if checkMd5 && srcMd5 != "" {
				// 	curMd5, _ := dcUtil.FileMd5(rf.GetFullpath())
				// 	if srcMd5 != curMd5 {
				// 		return fmt.Errorf("file:%s, src md5:%s, received md5:%s", rf.GetFullpath(), srcMd5, curMd5)
				// 	}
				// }
				rf.Buffer = data[bufstartoffset:bufendoffset]
			}
		}
	}

	return nil
}

func encodeSendFileReq(req *dcSDK.BKDistFileSender, sandbox *syscall.Sandbox) ([]protocol.Message, error) {
	blog.Debugf("encode send files request to message now")

	// save files
	filemessages := make([]protocol.Message, 0)
	var filebuflen int64

	checkMd5 := false
	if env.GetEnv(env.KeyCommonCheckMd5) == "true" {
		checkMd5 = true
	}

	// encode body and file to message
	pbbody := protocol.PBBodySendFileReq{
		Inputfiles: []*protocol.PBFileDesc{},
	}
	for _, f := range req.Files {
		comprsstype := protocol.PBCompressType_LZ4
		if f.Compresstype == protocol.CompressLZO {
			comprsstype = protocol.PBCompressType_LZO
		} else if f.Compresstype == protocol.CompressNone {
			comprsstype = protocol.PBCompressType_NONE
		}

		fullpath := sandbox.GetAbsPath(f.FilePath)
		size := f.FileSize
		md5 := f.Md5
		targetrelativepath := f.Targetrelativepath
		filemode := f.Filemode
		linkTarget := f.LinkTarget
		modifytime := f.Lastmodifytime

		if size <= 0 {
			pbbody.Inputfiles = append(pbbody.Inputfiles, &protocol.PBFileDesc{
				Fullpath:           &fullpath,
				Size:               &size,
				Md5:                &md5,
				Compresstype:       &comprsstype,
				Compressedsize:     &size,
				Targetrelativepath: &targetrelativepath,
				Filemode:           &filemode,
				Linktarget:         []byte(linkTarget),
				Modifytime:         &modifytime,
			})
			continue
		}

		m, compressedsize, err := fillOneFile(fullpath, f.Buffer, f.Compresstype, filemode)
		if err != nil {
			blog.Warnf("failed to encode message for file", f.FilePath)
			continue
		}

		if md5 == "" && checkMd5 {
			md5, _ = dcFile.Stat(fullpath).Md5()
		}

		pbbody.Inputfiles = append(pbbody.Inputfiles, &protocol.PBFileDesc{
			Fullpath:           &fullpath,
			Size:               &size,
			Md5:                &md5,
			Compresstype:       &comprsstype,
			Compressedsize:     &compressedsize,
			Targetrelativepath: &targetrelativepath,
			Filemode:           &filemode,
			Linktarget:         []byte(linkTarget),
			Modifytime:         &modifytime,
		})

		// blog.Infof("encode send files add file(%s) modify time(%d)", fullpath, modifytime)

		filemessages = append(filemessages, m)
		filebuflen += compressedsize
	}

	bodydata, err := proto.Marshal(&pbbody)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbbody")
		return nil, err
	}
	bodymessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         bodydata,
		Compresstype: protocol.CompressNone,
	}
	bodylen := int32(pbbody.XXX_Size())
	blog.Debugf("encode body[%s] to size %d", pbbody.String(), bodylen)

	// encode head
	cmdtype := protocol.PBCmdType_SENDFILEREQ
	pbhead := protocol.PBHead{
		Version: &bkdistcmdversion,
		Magic:   &bkdistcmdmagic,
		Bodylen: &bodylen,
		Buflen:  &filebuflen,
		Cmdtype: &cmdtype,
	}
	headdata, err := proto.Marshal(&pbhead)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbhead")
		return nil, err
	}
	blog.Debugf("encode head[%s] to size %d", pbhead.String(), pbhead.XXX_Size())

	headtokendata, err := bkformatTokenInt(protocol.TOEKNHEADFLAG, pbhead.XXX_Size())
	if err != nil {
		blog.Warnf("failed to format head token")
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

func receiveSendFileRsp(client *TCPClient) (*protocol.PBBodySendFileRsp, error) {
	blog.Debugf("receive send file response now")

	// receive head
	head, err := receiveCommonHead(client)
	if err != nil {
		return nil, err
	}

	if head.GetCmdtype() != protocol.PBCmdType_SENDFILERSP {
		err := fmt.Errorf("unknown cmd type %v", head.GetCmdtype())
		blog.Warnf("%v", err)
		return nil, err
	}

	bodylen := head.GetBodylen()
	buflen := head.GetBuflen()
	if bodylen <= 0 || buflen < 0 {
		err := fmt.Errorf("get invalid body length %d, buf len %d", bodylen, buflen)
		blog.Warnf("%v", err)
		return nil, err
	}
	blog.Debugf("got bodylen %d buflen %d", bodylen, buflen)

	// receive body
	data, datalen, err := client.ReadData(int(bodylen))
	if err != nil {
		blog.Warnf("failed to receive pbbody")
		return nil, err
	}

	// TODO : should by cmd type here
	body := protocol.PBBodySendFileRsp{}
	err = proto.Unmarshal(data[0:datalen], &body)

	if err != nil {
		blog.Warnf("failed to decode pbbody error: %v", err)
		return nil, err
	}

	return &body, nil
}

func decodeSendFileRsp(data *protocol.PBBodySendFileRsp) (*dcSDK.BKSendFileResult, error) {
	blog.Debugf("decode send file response now")

	results := dcSDK.BKSendFileResult{}
	for _, r := range data.GetResults() {
		result := dcSDK.FileResult{
			FilePath:           r.GetFullpath(),
			RetCode:            r.GetRetcode(),
			Targetrelativepath: r.GetTargetrelativepath(),
		}
		results.Results = append(results.Results, result)
	}

	return &results, nil
}

func encodeCheckCacheReq(req *dcSDK.BKDistFileSender, sandbox *syscall.Sandbox) ([]protocol.Message, error) {
	blog.Debugf("encode sync time request to message now")

	// encode body and file to message
	pbbody := protocol.PBBodyCheckCacheReq{}

	for _, f := range req.Files {
		fullpath := sandbox.GetAbsPath(f.FilePath)
		md5, _ := dcFile.Stat(fullpath).Md5()

		if f.Targetrelativepath != "" {
			fullpath = filepath.Join(sandbox.GetAbsPath(f.Targetrelativepath), filepath.Base(f.FilePath))
		}

		pbbody.Params = append(pbbody.Params, &protocol.PBCacheParam{
			Name:       []byte(filepath.Base(f.FilePath)),
			Md5:        []byte(md5),
			Target:     []byte(fullpath),
			Filemode:   &f.Filemode,
			Linktarget: []byte(f.LinkTarget),
			Modifytime: &f.Lastmodifytime,
		})
	}

	bodydata, err := proto.Marshal(&pbbody)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbbody")
		return nil, err
	}
	bodymessage := protocol.Message{
		Messagetype:  protocol.MessageString,
		Data:         bodydata,
		Compresstype: protocol.CompressNone,
	}
	bodylen := int32(pbbody.XXX_Size())
	blog.Debugf("encode body[%s] to size %d", pbbody.String(), bodylen)

	// encode head
	var filebuflen int64
	cmdtype := protocol.PBCmdType_CHECKCACHEREQ
	pbhead := protocol.PBHead{
		Version: &bkdistcmdversion,
		Magic:   &bkdistcmdmagic,
		Bodylen: &bodylen,
		Buflen:  &filebuflen,
		Cmdtype: &cmdtype,
	}
	headdata, err := proto.Marshal(&pbhead)
	if err != nil {
		blog.Warnf("failed to proto.Marshal pbhead")
		return nil, err
	}
	blog.Debugf("encode head[%s] to size %d", pbhead.String(), pbhead.XXX_Size())

	headtokendata, err := bkformatTokenInt(protocol.TOEKNHEADFLAG, pbhead.XXX_Size())
	if err != nil {
		blog.Warnf("failed to format head token")
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

func receiveCheckCacheRsp(client *TCPClient) (*protocol.PBBodyCheckCacheRsp, error) {
	blog.Debugf("receive check cache response now")

	// receive head
	head, err := receiveCommonHead(client)
	if err != nil {
		return nil, err
	}

	if head.GetCmdtype() != protocol.PBCmdType_CHECKCACHERSP {
		err := fmt.Errorf("unknown cmd type %v", head.GetCmdtype())
		blog.Warnf("%v", err)
		return nil, err
	}

	bodylen := head.GetBodylen()
	buflen := head.GetBuflen()
	if bodylen <= 0 || buflen < 0 {
		err := fmt.Errorf("get invalid body length %d, buf len %d", bodylen, buflen)
		blog.Warnf("%v", err)
		return nil, err
	}
	blog.Debugf("got bodylen %d buflen %d", bodylen, buflen)

	// receive body
	data, datalen, err := client.ReadData(int(bodylen))
	if err != nil {
		blog.Warnf("failed to receive pbbody")
		return nil, err
	}

	// TODO : should by cmd type here
	body := protocol.PBBodyCheckCacheRsp{}
	err = proto.Unmarshal(data[0:datalen], &body)

	if err != nil {
		blog.Warnf("failed to decode pbbody error: %v", err)
		return nil, err
	}

	return &body, nil
}

func decodeCheckCacheRsp(data *protocol.PBBodyCheckCacheRsp) ([]bool, error) {
	blog.Debugf("decode send file response now")

	results := make([]bool, 0, len(data.GetResults()))
	for _, r := range data.GetResults() {
		results = append(results, r.GetStatus() == protocol.PBCacheStatus_SUCCESS)
	}

	return results, nil
}
