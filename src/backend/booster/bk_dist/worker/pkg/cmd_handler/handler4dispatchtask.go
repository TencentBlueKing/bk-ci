/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pbcmd

import (
	"fmt"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"

	dcConfig "github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
	dcEnv "github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/protocol"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// Handle4DispatchReq handler for dispatch task request
type Handle4DispatchReq struct {
}

// NewHandle4DispatchReq return Handle4DispatchReq
func NewHandle4DispatchReq() *Handle4DispatchReq {
	return &Handle4DispatchReq{}
}

// FilepathMapping return relativepath, abspath, error
func FilepathMapping(inputfile string, basedir string, relativedir string) (string, string, error) {
	outputfile := inputfile
	outputrelativepath := relativedir

	// deal with gch file to support client of old version
	if strings.HasSuffix(inputfile, ".gch") && filepath.IsAbs(inputfile) && relativedir == "" {
		abspath, err := filepath.Abs(inputfile)
		return outputrelativepath, abspath, err
	}

	if relativedir != "" {
		outputfile = filepath.Join(relativedir, filepath.Base(inputfile))
	} else {
		outputfile = filepath.Join(basedir, filepath.Base(inputfile))
		outputrelativepath = basedir
	}

	// return outputfile, nil
	abspath, err := filepath.Abs(outputfile)
	blog.Debugf("FilepathMapping inputfile:%s,basedir:%s,relativedir:%s to abspath:%s",
		inputfile, basedir, relativedir, abspath)

	return outputrelativepath, abspath, err
}

// ReceiveBody receive body for this cmd
func (h *Handle4DispatchReq) ReceiveBody(client *protocol.TCPClient,
	head *dcProtocol.PBHead,
	basedir string,
	c chan<- string) (interface{}, error) {
	// recieve body
	req, err := protocol.ReceiveBKCommonDispatchReq(client, head, basedir, FilepathMapping, c)
	if err != nil {
		blog.Errorf("failed to receive dispatch req body error:%v", err)
		return nil, err
	}

	blog.Infof("succeed to receive dispatch req body")
	return req, nil
}

// Handle to handle this cmd
func (h *Handle4DispatchReq) Handle(client *protocol.TCPClient,
	head *dcProtocol.PBHead,
	body interface{},
	receivedtime time.Time,
	basedir string,
	cmdreplacerules []dcConfig.CmdReplaceRule) error {
	blog.Infof("handle with base dir:%s", basedir)
	defer func() {
		blog.Infof("handle out for base dir:%s", basedir)
	}()

	// convert to req
	req, ok := body.(*dcProtocol.PBBodyDispatchTaskReq)
	if !ok {
		err := fmt.Errorf("failed to get body from interface")
		blog.Errorf("%v", err)
		return err
	}

	// exuecute cmd
	results, err := h.dealDispatchReq(req, receivedtime, basedir, cmdreplacerules)
	if err != nil {
		blog.Errorf("failed to deal dispatch req error:%v", err)
		return err
	}

	// encode response to messages
	messages, err := protocol.EncodeBKCommonDispatchRsp(results)
	if err != nil {
		blog.Errorf("failed to encode rsp to messages for error:%v", err)
	}
	blog.Infof("succeed to encode dispatch response to messages")

	// send response
	err = protocol.SendMessages(client, &messages)
	if err != nil {
		blog.Errorf("failed to send messages for error:%v", err)
	}
	blog.Infof("succeed to send messages")

	return nil
}

// func sendMessages(client *protocol.TCPClient, messages *[]dcProtocol.Message) error {
// 	blog.Infof("ready send messages")

// 	if messages == nil || len(*messages) == 0 {
// 		return fmt.Errorf("data to send is empty")
// 	}

// 	for _, v := range *messages {
// 		if v.Data == nil {
// 			blog.Warnf("found nil data when ready send bk-common dist request")
// 			continue
// 		}

// 		switch v.Messagetype {
// 		case dcProtocol.MessageString:
// 			if err := client.WriteData(v.Data); err != nil {
// 				return err
// 			}
// 		case dcProtocol.MessageFile:
// 			if err := client.SendFile(string(v.Data), v.Compresstype); err != nil {
// 				return err
// 			}
// 		default:
// 			return fmt.Errorf("unknown message type %s", v.Messagetype.String())
// 		}
// 	}

// 	return nil
// }

func (h *Handle4DispatchReq) dealDispatchReq(req *dcProtocol.PBBodyDispatchTaskReq,
	receivedtime time.Time,
	basedir string,
	cmdreplacerules []dcConfig.CmdReplaceRule) ([]*dcProtocol.PBResult, error) {
	results := make([]*dcProtocol.PBResult, 0)
	for _, v := range req.GetCmds() {
		result, err := h.dealOneCommand(v, receivedtime, basedir, cmdreplacerules)
		if err != nil {
			blog.Errorf("failed to execute command for error: %v", err)
			return nil, err
		}

		results = append(results, result)
	}

	return results, nil
}

func needCheckMd5(onecmd *dcProtocol.PBCommand) bool {
	if onecmd == nil {
		return false
	}

	for _, rf := range onecmd.GetInputfiles() {
		if rf.GetMd5() != "" {
			return true
		}
	}

	return false
}

func (h *Handle4DispatchReq) dealOneCommand(onecmd *dcProtocol.PBCommand,
	receivedtime time.Time,
	basedir string,
	cmdreplacerules []dcConfig.CmdReplaceRule) (*dcProtocol.PBResult, error) {

	startkey := dcProtocol.BKStatKeyStartTime
	start64 := time.Now().UnixNano()

	// adjust params with local real path
	params, _ := adjustParams(onecmd, basedir, cmdreplacerules)
	exepath, exename, _ := adjustExe(onecmd)

	// get environments
	environments := make([]string, 0)
	overwrite := true
	appendEnvKey := dcEnv.GetEnvKey(dcEnv.KeyRemoteEnvAppend)
	for _, item := range onecmd.GetEnv() {
		environments = append(environments, string(item))
		if strings.HasPrefix(string(item), appendEnvKey) {
			overwrite = false
		}
	}

	sandbox := dcSyscall.Sandbox{}
	sandbox.Dir = basedir
	if len(environments) > 0 {
		// overwrite env
		if overwrite {
			sandbox.Env = env.NewSandbox(environments)
		} else {
			// append env
			envs := os.Environ()
			envs = append(envs, environments...)
			sandbox.Env = env.NewSandbox(envs)
		}
	}
	if workDir := onecmd.GetWorkdir(); workDir != "" {
		sandbox.Dir = workDir
		_ = os.MkdirAll(sandbox.Dir, os.ModePerm)
	}

	exefullpath := path.Join(exepath, exename)
	blog.Infof("ready execute command from dir(%s): %s %s",
		sandbox.Dir, exefullpath, strings.Join(params, " "))
	retcode, stdout, stderr, err := sandbox.ExecCommandWithMessage(exefullpath, params...)
	outputMsg := string([]byte(string(stdout)))
	// ensure is utf8
	if !utf8.ValidString(outputMsg) {
		outputMsg = "include invalid utf8 for output message, return empty now"
	}

	errorMsg := string([]byte(string(stderr)))
	if !utf8.ValidString(errorMsg) {
		errorMsg = "include invalid utf8 for error message, return empty now"
	}

	endkey := dcProtocol.BKStatKeyEndTime
	end64 := time.Now().UnixNano()

	if err != nil {
		blog.Errorf("failed to execute command[%s %s] after %d seconds for error: %v,output:[%s],errmsg:[%s]",
			exefullpath,
			strings.Join(params, " "),
			(end64-start64)/1000/1000/1000,
			err,
			outputMsg,
			errorMsg)
	} else {
		blog.Infof("succeed to execute command[%s %s] after %d seconds for error: %v,output:[%s],errmsg:[%s]",
			exefullpath,
			strings.Join(params, " "),
			(end64-start64)/1000/1000/1000,
			err,
			outputMsg,
			errorMsg)
	}

	retcode32 := int32(retcode)
	r := dcProtocol.PBResult{
		Cmd:           onecmd,
		Retcode:       &retcode32,
		Outputmessage: &outputMsg,
		Errormessage:  &errorMsg,
	}

	// check and save result files
	compresstype := dcProtocol.PBCompressType_LZ4
	for _, v := range onecmd.GetResultfiles() {
		var realpath string
		if filepath.IsAbs(v) {
			realpath = v
		} else {
			_, realpath, _ = FilepathMapping(v, basedir, "")
		}

		f := dcFile.Stat(realpath)
		existed, filesize := f.Exist(), f.Size()
		if !existed {
			err := fmt.Errorf("result file %s not existed,output:%s,errmsg:%s,retcode:%d",
				realpath, outputMsg, errorMsg, retcode32)
			blog.Errorf("%v", err)
			// in this condition, we should return detail error message
			// return nil, err
			filesize = 0
		}

		md5 := ""
		if existed && needCheckMd5(onecmd) {
			md5, err = dcFile.Stat(realpath).Md5()
			if err != nil {
				blog.Errorf("failed to md5 for file:%s, err:%v", realpath, err)
			}
		}

		composedfilepath := v + types.FileConnectFlag + realpath
		r.Resultfiles = append(r.Resultfiles, &dcProtocol.PBFileDesc{
			Fullpath:       &composedfilepath,
			Size:           &filesize,
			Md5:            &md5, // not implement now
			Compresstype:   &compresstype,
			Compressedsize: nil, // fill this when ready to send
		})
	}

	// add stat info
	receivedkey := dcProtocol.BKStatKeyReceivedTime
	receivedtime64 := receivedtime.UnixNano()
	r.Stats = append(r.Stats, &dcProtocol.PBStatEntry{
		Key:  &receivedkey,
		Time: &receivedtime64,
	})

	r.Stats = append(r.Stats, &dcProtocol.PBStatEntry{
		Key:  &startkey,
		Time: &start64,
	})

	r.Stats = append(r.Stats, &dcProtocol.PBStatEntry{
		Key:  &endkey,
		Time: &end64,
	})

	return &r, nil
}

// func (h *Handle4DispatchReq) getWorkDir(basedir, cmdworkdir string) string {
// 	if cmdworkdir != "" && path.IsAbs(cmdworkdir) {
// 		return cmdworkdir
// 	}

// 	return path.Join(basedir, cmdworkdir)
// }

// func (h *Handle4DispatchReq) getRealPath(inputfile, workdir string) string {
// 	realpath := inputfile
// 	if !filepath.IsAbs(inputfile) {
// 		realpath = path.Join(workdir, inputfile)
// 	}

// 	return realpath
// }

// func (h *Handle4DispatchReq) ensureResultfileDir(files []string, workdir string) {
// 	for _, v := range files {
// 		realpath := h.getRealPath(v, workdir)
// 		os.MkdirAll(filepath.Dir(realpath), os.ModePerm)
// 	}
// }

// checkParamFuncList provide a type for check function list
type checkParamFuncList []func(param string) bool

type checkParamType int

const (
	checkParamOneTrue checkParamType = iota
	checkParamOneFalse
	checkParamAllTrue
	checkParamAllFalse
)

func (c checkParamFuncList) check(checkType checkParamType, param string) bool {
	for _, f := range c {
		if f(param) {
			switch checkType {
			case checkParamOneTrue:
				return true
			case checkParamAllFalse:
				return false
			}
			continue
		}

		switch checkType {
		case checkParamOneFalse:
			return true
		case checkParamAllTrue:
			return false
		}
	}

	switch checkType {
	case checkParamOneTrue, checkParamOneFalse:
		return false
	case checkParamAllTrue, checkParamAllFalse:
		return true
	}

	return false
}

// excluded params from replacing
var excludedFunc = checkParamFuncList{
	func(param string) bool {
		return strings.HasPrefix(param, "-frandom-seed=")
	},
}

// change both input and result files to relative paths
func adjustParams(
	onecmd *dcProtocol.PBCommand,
	basedir string,
	cmdreplacerules []dcConfig.CmdReplaceRule) ([]string, error) {
	params := onecmd.GetParams()

	// adjust input
	for _, v := range onecmd.GetInputfiles() {
		inputfile := v.GetFullpath()
		_, realpath, _ := FilepathMapping(inputfile, basedir, v.GetTargetrelativepath())
		if inputfile != realpath {
			for i := range params {
				// check param with excluded functions, if so then skip replacing
				if excludedFunc.check(checkParamOneTrue, params[i]) {
					continue
				}

				params[i] = strings.Replace(params[i], inputfile, realpath, -1)
			}
		}
	}

	// adjust result
	for _, v := range onecmd.GetResultfiles() {
		blog.Debugf("adjust path for %s", v)

		var realpath string
		if filepath.IsAbs(v) {
			realpath = v
		} else {
			_, realpath, _ = FilepathMapping(v, basedir, "")
			if v != realpath {
				for i := range params {
					// check param with excluded functions, if so then skip replacing
					if excludedFunc.check(checkParamOneTrue, params[i]) {
						continue
					}

					params[i] = strings.Replace(params[i], v, realpath, -1)
				}
			}
		}

		// ensure result dir exist
		// TODO : support abs path of realpath
		if filepath.IsAbs(realpath) {
			blog.Debugf("mkdir for %s", filepath.Dir(realpath))
			os.MkdirAll(filepath.Dir(realpath), os.ModePerm)
		} else {
			blog.Debugf("mkdir for %s", filepath.Join(basedir, filepath.Dir(realpath)))
			os.MkdirAll(filepath.Join(basedir, filepath.Dir(realpath)), os.ModePerm)
		}
	}

	// adjust with replace rules
	if cmdreplacerules != nil && len(cmdreplacerules) > 0 {
		for _, v := range cmdreplacerules {
			v.Replace(onecmd.GetExename(), params)
		}
	}

	return params, nil
}

func adjustExe(onecmd *dcProtocol.PBCommand) (string, string, error) {

	exepath := onecmd.GetExepath()
	exeName := onecmd.GetExename()
	if exepath != "" {
		// if exepach specifiled by user, do nothing
		return exepath, exeName, nil
	}

	// adjust with input
	for _, v := range onecmd.GetInputfiles() {
		inputfile := v.GetFullpath()
		if inputfile == exeName && v.GetTargetrelativepath() != "" {
			_, realpath, _ := FilepathMapping(inputfile, "", v.GetTargetrelativepath())
			return "", realpath, nil
		}
	}

	// get abs path of exe
	if abs, err := exec.LookPath(exeName); err == nil {
		return "", abs, nil
	}

	return exepath, exeName, nil
}
