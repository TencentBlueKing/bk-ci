package logs

import (
	"bytes"
	"fmt"
	"io"
	"os"

	"github.com/sirupsen/logrus"
	"gopkg.in/natefinch/lumberjack.v2"
)

const (
	ErrorNoStackKey = "error_no_stack_key"
)

var Logs *logrus.Entry

func Init(filepath string, isDebug bool, logStd bool) error {
	logInfo := logrus.WithFields(logrus.Fields{})

	lumLog := &lumberjack.Logger{
		Filename:   filepath,
		MaxAge:     7,
		MaxBackups: 7,
		LocalTime:  true,
	}

	// 同时写入到 std
	if logStd {
		logInfo.Logger.Out = io.MultiWriter(lumLog, os.Stdout)
	} else {
		logInfo.Logger.Out = lumLog
	}

	logInfo.Logger.SetFormatter(&MyFormatter{})

	go DoDailySplitLog(filepath, lumLog)

	if isDebug {
		logInfo.Logger.SetLevel(logrus.DebugLevel)
	}

	Logs = logInfo

	return nil
}

// UNTestDebugInit DebugInit 初始化为debug模式下的log，将日志输出到标准输出流，只是为了单元测试使用
func UNTestDebugInit() {
	logInfo := logrus.WithFields(logrus.Fields{})
	logInfo.Logger.SetFormatter(&MyFormatter{})
	logInfo.Logger.SetOutput(os.Stdout)
	logInfo.Logger.SetLevel(logrus.DebugLevel)
	Logs = logInfo
}

type MyFormatter struct{}

func (m *MyFormatter) Format(entry *logrus.Entry) ([]byte, error) {
	var b *bytes.Buffer
	if entry.Buffer != nil {
		b = entry.Buffer
	} else {
		b = &bytes.Buffer{}
	}

	timestamp := entry.Time.Format("2006-01-02 15:04:05.000")

	newLog := fmt.Sprintf("%s|%s|%s", timestamp, entry.Level, entry.Message)
	b.WriteString(newLog)

	for k, v := range entry.Data {
		if k == ErrorNoStackKey {
			b.WriteString(fmt.Sprintf("|error: %v", v))
			continue
		}
		switch v := v.(type) {
		case error:
			// Otherwise errors are ignored by `encoding/json`
			// https://github.com/sirupsen/logrus/issues/137
			//
			// Print errors verbosely to get stack traces where available
			b.WriteString(fmt.Sprintf("|%s: %+v", k, v))
		default:
			b.WriteString(fmt.Sprintf("|%s: %v", k, v))
		}
	}

	b.WriteString("\n")

	return b.Bytes(), nil
}
