package logs

import (
	"bytes"
	"fmt"
	"github.com/sirupsen/logrus"
	"gopkg.in/natefinch/lumberjack.v2"
	"path/filepath"
	"strings"
)

var logs *logrus.Logger

func Init(filepath string) error {
	logInfo := logrus.New()

	lumLog := &lumberjack.Logger{
		Filename:   filepath,
		MaxAge:     7,
		MaxBackups: 7,
		LocalTime:  true,
	}

	logInfo.Out = lumLog

	logInfo.SetFormatter(&MyFormatter{})

	go DoDailySplitLog(filepath, lumLog)

	logs = logInfo

	return nil
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
	var newLog string

	//HasCaller()为true才会有调用信息
	if entry.HasCaller() {
		fName := filepath.Base(entry.Caller.File)
		newLog = fmt.Sprintf("%s [%s] [%s:%d %s] %s\n",
			timestamp, entry.Level, fName, entry.Caller.Line, entry.Caller.Function, entry.Message)
	} else {
		level, err := parseLevel(entry.Level)
		if err != nil {
			return nil, err
		}
		newLog = fmt.Sprintf("%s [%s]  %s\n", timestamp, level, entry.Message)
	}

	b.WriteString(newLog)
	return b.Bytes(), nil
}

func parseLevel(l logrus.Level) (string, error) {
	switch strings.ToLower(fmt.Sprintf("%s", l)) {
	case "panic":
		return "P", nil
	case "fatal":
		return "F", nil
	case "error":
		return "E", nil
	case "warn", "warning":
		return "W", nil
	case "info":
		return "I", nil
	case "debug":
		return "D", nil
	case "trace":
		return "T", nil
	}

	return "U", fmt.Errorf("not a valid logrus Level: %q", l)
}

func Info(f interface{}, v ...interface{}) {
	logs.Info(formatLog(f, v...))
}

func Warn(f interface{}, v ...interface{}) {
	logs.Warn(formatLog(f, v...))
}

func Error(f interface{}, v ...interface{}) {
	logs.Error(formatLog(f, v...))
}

func formatLog(f interface{}, v ...interface{}) string {
	var msg string
	switch f.(type) {
	case string:
		msg = f.(string)
		if len(v) == 0 {
			return msg
		}
		if strings.Contains(msg, "%") && !strings.Contains(msg, "%%") {
			//format string
		} else {
			//do not contain format char
			msg += strings.Repeat(" %v", len(v))
		}
	default:
		msg = fmt.Sprint(f)
		if len(v) == 0 {
			return msg
		}
		msg += strings.Repeat(" %v", len(v))
	}
	return fmt.Sprintf(msg, v...)
}
