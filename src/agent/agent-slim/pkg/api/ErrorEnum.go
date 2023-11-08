package api

import "github.com/TencentBlueKing/bk-ci/agentslim/pkg/i18n"

type ErrorCode int

const (
	BuildProcessRunError ErrorCode = 2128040 + iota
	RecoverRunFileError
	LoseRunFileError
	MakeTmpDirError
	BuildProcessStartError
	PrepareScriptCreateError
)

type ErrorTypes string

const (
	System     ErrorTypes = "SYSTEM"
	User       ErrorTypes = "USER"
	ThirdParty ErrorTypes = "THIRD_PARTY"
	Plugin     ErrorTypes = "PLUGIN"
)

type ErrorEnum struct {
	Type ErrorTypes
	Code ErrorCode
	// 方便国际化
	MessageId string
}

var (
	NoErrorEnum                  = &ErrorEnum{Type: "", Code: 0, MessageId: ""}
	BuildProcessRunErrorEnum     = &ErrorEnum{Type: User, Code: BuildProcessRunError, MessageId: "EC_BuildProcessRunError"}
	LoseRunFileErrorEnum         = &ErrorEnum{Type: User, Code: LoseRunFileError, MessageId: "EC_LoseRunFileError"}
	MakeTmpDirErrorEnum          = &ErrorEnum{Type: User, Code: MakeTmpDirError, MessageId: "EC_MakeTmpDirError"}
	BuildProcessStartErrorEnum   = &ErrorEnum{Type: User, Code: BuildProcessStartError, MessageId: "EC_BuildProcessStartError"}
	PrepareScriptCreateErrorEnum = &ErrorEnum{Type: User, Code: PrepareScriptCreateError, MessageId: "EC_PrepareScriptCreateError"}
)

func (t *PersistenceBuildInfo) ToFinish(
	success bool,
	message string,
	errorEnum *ErrorEnum,
) *PersistenceBuildWithStatus {
	if success || errorEnum == NoErrorEnum {
		return &PersistenceBuildWithStatus{
			PersistenceBuildInfo: *t,
			Success:              success,
			Message:              message,
			Error:                nil,
		}
	}
	errMsg := ""
	if errorEnum.MessageId != "" {
		errMsg = i18n.Localize(errorEnum.MessageId, nil)
	}
	return &PersistenceBuildWithStatus{
		PersistenceBuildInfo: *t,
		Success:              success,
		Message:              message,
		Error: &Error{
			ErrorType:    errorEnum.Type,
			ErrorMessage: errMsg,
			ErrorCode:    errorEnum.Code,
		},
	}
}
