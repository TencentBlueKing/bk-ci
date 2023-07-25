package logs

import (
	"github.com/sirupsen/logrus"
)

func Info(args ...interface{}) {
	Logs.Info(args...)
}

func Infof(format string, args ...interface{}) {
	Logs.Infof(format, args...)
}

func Warn(args ...interface{}) {
	Logs.Warn(args...)
}

func Warnf(format string, args ...interface{}) {
	Logs.Warnf(format, args...)
}

func Error(args ...interface{}) {
	Logs.Error(args...)
}

func Errorf(format string, args ...interface{}) {
	Logs.Errorf(format, args...)
}

func Fatal(args ...interface{}) {
	Logs.Fatal(args...)
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
