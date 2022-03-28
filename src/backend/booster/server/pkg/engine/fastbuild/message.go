/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package fastbuild

import (
	"errors"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
)

// MessageType difine message type
type MessageType int

// enum values for message type
const (
	_ MessageType = iota
	MessageTypeSubTaskDone
)

// define errors for message
var (
	ErrorInvalidMessageType = errors.New("invalid message type")
)

// String return the string of MessageType
func (rst MessageType) String() string {
	if s, ok := commandStatusTypeMap[rst]; ok {
		return s
	}

	return "unknown message type"
}

var commandStatusTypeMap = map[MessageType]string{
	MessageTypeSubTaskDone: "subtask done",
}

// Message define message data
type Message struct {
	Type MessageType
	Data []byte
}

// MessageSubTaskDone define struct for sub task done
type MessageSubTaskDone struct {
	TaskID string `json:"task_id"`

	Params      string `json:"params"`
	FullCmd     string `json:"full_cmd"`
	Env         string `json:"env"`
	RunDir      string `json:"run_dir"`
	CommandType string `json:"command_type"`
	Command     string `json:"command"`
	User        string `json:"user"`

	Status    string `json:"status"`
	StartTime int64  `json:"start_time"`
	EndTime   int64  `json:"end_time"`

	// fb summary
	CompileResult string `json:"compile_result"`
	FbSummary     FbSummary
}

// MessageSubTaskDone2table format TableSubTask with MessageSubTaskDone
func MessageSubTaskDone2table(msg *MessageSubTaskDone) *TableSubTask {
	return &TableSubTask{
		TaskID:        msg.TaskID,
		Params:        msg.Params,
		FullCmd:       msg.FullCmd,
		Env:           msg.Env,
		RunDir:        msg.RunDir,
		CommandType:   msg.CommandType,
		Command:       msg.Command,
		User:          msg.User,
		Status:        string(msg.Status),
		StartTime:     msg.StartTime,
		EndTime:       msg.EndTime,
		CompileResult: msg.CompileResult,
		FbSummary:     msg.FbSummary,
	}
}

// DecodeSubTaskDone to decode message data to MessageSubTaskDone
func DecodeSubTaskDone(data []byte) (*MessageSubTaskDone, error) {
	var msg MessageSubTaskDone
	if err := codec.DecJSON(data, &msg); err != nil {
		blog.Errorf("failed to DecodeSubTaskDone for [%v] with data[%s]", err, string(data))
		return nil, err
	}

	return &msg, nil
}
