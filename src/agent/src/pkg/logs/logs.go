package logs

import (
	"bytes"
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/sirupsen/logrus"
	"gopkg.in/natefinch/lumberjack.v2"
)

var logs *logrus.Logger

func Init(filepath string, isDebug bool) error {
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

	if isDebug {
		logInfo.SetLevel(logrus.DebugLevel)
	}

	logs = logInfo

	return nil
}

// DebugInit 初始化为debug模式下的log，将日志输出到标准输出流，只是为了单元测试使用
func DebugInit() {
	logInfo := logrus.New()
	logInfo.SetOutput(os.Stdout)
	logs = logInfo
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
	switch strings.ToLower(l.String()) {
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

func Infof(format string, args ...interface{}) {
	logs.Infof(format, args...)
}

func Warn(f interface{}, v ...interface{}) {
	logs.Warn(formatLog(f, v...))
}

func Warnf(format string, args ...interface{}) {
	logs.Warnf(format, args...)
}

func Error(f interface{}, v ...interface{}) {
	logs.Error(formatLog(f, v...))
}

func Debug(args ...interface{}) {
	logs.Debug(args...)
}

func Debugf(format string, args ...interface{}) {
	logs.Debugf(format, args...)
}

func formatLog(f interface{}, v ...interface{}) string {
	var msg string
	switch f := f.(type) {
	case string:
		msg = f
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
