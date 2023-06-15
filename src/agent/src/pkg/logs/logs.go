package logs

import (
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

	logInfo.Logger.SetFormatter(&jsonFormatter{
		log.JSONFormatter{
			TimestampFormat: "2006-01-02 15:04:05.000",
		},
	})

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
	logInfo.Logger.SetFormatter(&jsonFormatter{
		log.JSONFormatter{
			TimestampFormat: "2006-01-02 15:04:05.000",
		},
	})
	logInfo.Logger.SetOutput(os.Stdout)
	logInfo.Logger.SetLevel(logrus.DebugLevel)
	Logs = logInfo
}

type jsonFormatter struct {
	log.JSONFormatter
}

func (f *jsonFormatter) Format(entry *log.Entry) ([]byte, error) {
	for k, v := range entry.Data {
		switch v := v.(type) {
		case error:
			// Otherwise errors are ignored by `encoding/json`
			// https://github.com/sirupsen/logrus/issues/137
			//
			// Print errors verbosely to get stack traces where available
			entry.Data[k] = fmt.Sprintf("%+v", v)
		}
	}

	return f.JSONFormatter.Format(entry)
}
