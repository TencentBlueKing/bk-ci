package logs

import (
	"fmt"
	"strings"

	"github.com/sirupsen/logrus"
)

func Info(f interface{}, v ...interface{}) {
	Logs.Info(formatLog(f, v...))
}

func Infof(format string, args ...interface{}) {
	Logs.Infof(format, args...)
}

func Warn(f interface{}, v ...interface{}) {
	Logs.Warn(formatLog(f, v...))
}

func Warnf(format string, args ...interface{}) {
	Logs.Warnf(format, args...)
}

func Error(f interface{}, v ...interface{}) {
	Logs.Error(formatLog(f, v...))
}

func Errorf(format string, args ...interface{}) {
	Logs.Errorf(format, args...)
}

func Fatal(f interface{}, v ...interface{}) {
	Logs.Fatal(formatLog(f, v...))
}

func Fatalf(format string, args ...interface{}) {
	Logs.Fatalf(format, args...)
}

func Debug(args ...interface{}) {
	Logs.Debug(args...)
}

func Debugf(format string, args ...interface{}) {
	Logs.Debugf(format, args...)
}

func WithField(key string, value interface{}) *logrus.Entry {
	return Logs.WithField(key, value)
}

func WithError(err error) *logrus.Entry {
	return Logs.WithError(err)
}

// TODO: 删掉自己的format全部改用原生logrus的格式
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
