package logs

import (
	"bytes"
	"fmt"
	"os"

	"github.com/sirupsen/logrus"
	log "github.com/sirupsen/logrus"
	"gopkg.in/natefinch/lumberjack.v2"
)

var Logs = log.WithFields(log.Fields{})

func Init(filepath string, isDebug bool) error {
	logInfo := log.WithFields(log.Fields{})

	lumLog := &lumberjack.Logger{
		Filename:   filepath,
		MaxAge:     7,
		MaxBackups: 7,
		LocalTime:  true,
	}

	logInfo.Logger.Out = lumLog

	logInfo.Logger.SetFormatter(&MyFormatter{})

	go DoDailySplitLog(filepath, lumLog)

	if isDebug {
		logInfo.Logger.SetLevel(logrus.DebugLevel)
	}

	Logs = logInfo

	return nil
}

// DebugInit 初始化为debug模式下的log，将日志输出到标准输出流，只是为了单元测试使用
func UNTestDebugInit() {
	logInfo := log.WithFields(log.Fields{})
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
