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
	"time"

	dcConfig "github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/protocol"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

var ()

// Handle4SendFile handler for send file
type Handle4SendFile struct {
}

// NewHandle4SendFile return Handle4DispatchReq
func NewHandle4SendFile() *Handle4SendFile {
	return &Handle4SendFile{}
}

// ReceiveBody receive body for this cmd
func (h *Handle4SendFile) ReceiveBody(client *protocol.TCPClient,
	head *dcProtocol.PBHead,
	basedir string,
	c chan<- string) (interface{}, error) {
	// recieve body
	req, err := protocol.ReceiveBKSendFile(client, head, basedir, FilepathMapping, c, defaultCM)
	if err != nil {
		blog.Errorf("failed to receive dispatch req body error:%v", err)
		return nil, err
	}

	blog.Infof("succeed to receive dispatch req body")
	return req, nil
}

// Handle to handle this cmd
func (h *Handle4SendFile) Handle(client *protocol.TCPClient,
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
	req, ok := body.(*dcProtocol.PBBodySendFileReq)
	if !ok {
		err := fmt.Errorf("failed to get body from interface")
		blog.Errorf("%v", err)
		return err
	}

	// encode response to messages
	messages, err := protocol.EncodeBKSendFileRsp(req)
	if err != nil {
		blog.Errorf("failed to encode rsp to messages for error:%v", err)
	}
	blog.Infof("succeed to encode send file response to messages")

	// send response
	err = protocol.SendMessages(client, &messages)
	if err != nil {
		blog.Errorf("failed to send messages for error:%v", err)
	}
	blog.Infof("succeed to send messages")

	return nil
}
