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
	"time"

	dcConfig "github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/protocol"
)

// Handler describe
type Handler interface {
	ReceiveBody(client *protocol.TCPClient,
		head *dcProtocol.PBHead,
		basedir string,
		c chan<- string) (interface{}, error)
	Handle(client *protocol.TCPClient,
		head *dcProtocol.PBHead,
		body interface{},
		receivedtime time.Time,
		basedir string,
		cmdreplacerules []dcConfig.CmdReplaceRule) error
}

var handlemap map[dcProtocol.PBCmdType]Handler

// InitHandlers init handlers
func InitHandlers() {
	handlemap = map[dcProtocol.PBCmdType]Handler{
		dcProtocol.PBCmdType_DISPATCHTASKREQ: NewHandle4DispatchReq(),
		dcProtocol.PBCmdType_SYNCTIMEREQ:     NewHandle4SyncTime(),
		dcProtocol.PBCmdType_SENDFILEREQ:     NewHandle4SendFile(),
		dcProtocol.PBCmdType_CHECKCACHEREQ:   NewHandle4FileCache(),
		dcProtocol.PBCmdType_UNKNOWN:         NewHandle4Unknown(),
	}
}

// GetHandler return handle by type
func GetHandler(key dcProtocol.PBCmdType) Handler {
	if v, ok := handlemap[key]; ok {
		return v
	}

	return nil

	// switch key {
	// case dcProtocol.PBCmdType_DISPATCHTASKREQ:
	// 	return NewHandle4DispatchReq()
	// case dcProtocol.PBCmdType_SYNCTIMEREQ:
	// 	return NewHandle4SyncTime()
	// default:
	// 	return nil
	// }

}
