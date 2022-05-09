/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package protocol

// const vars
const (
	BKDistDir     = ".bk_dist"
	BKDistLockDir = "lock"

	Bkdistcmdversion = "1.0.0.0"
	Bkdistcmdmagic   = "b&*(%$#@*(@k"

	BKStatKeyReceivedTime = "recieved_time"
	BKStatKeyStartTime    = "start_time"
	BKStatKeyEndTime      = "end_time"
	BKStatKeySendTime     = "send_time"

	TOKENLEN      = 4
	TOKENBUFLEN   = 12
	TOEKNHEADFLAG = "HEAD"
)

// MessageType define message type
type MessageType int32

// define message types
const (
	MessageString MessageType = iota
	MessageFile
)

var (
	messageType2StringMap = map[MessageType]string{
		MessageString: "string",
		MessageFile:   "file",
	}
)

// String return the string of MessageType
func (r MessageType) String() string {
	if v, ok := messageType2StringMap[r]; ok {
		return v
	}

	return UnknownMessage
}

// Message define message to send
type Message struct {
	Messagetype MessageType
	// for message string, it's message content, or it's file path for message file
	Data []byte
	// to compress file content if need
	Compresstype CompressType
}
