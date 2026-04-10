package logs

import (
	"fmt"
	"os"

	"github.com/sirupsen/logrus"
)

// ready reports whether the logger has been initialised via Init().
func ready() bool {
	return Logs != nil
}

func Info(args ...interface{}) {
	if !ready() {
		fmt.Fprintln(os.Stderr, args...)
		return
	}
	Logs.Info(args...)
}

func Infof(format string, args ...interface{}) {
	if !ready() {
		fmt.Fprintf(os.Stderr, format+"\n", args...)
		return
	}
	Logs.Infof(format, args...)
}

func Warn(args ...interface{}) {
	if !ready() {
		fmt.Fprint(os.Stderr, "[WARN] ")
		fmt.Fprintln(os.Stderr, args...)
		return
	}
	Logs.Warn(args...)
}

func Warnf(format string, args ...interface{}) {
	if !ready() {
		fmt.Fprintf(os.Stderr, "[WARN] "+format+"\n", args...)
		return
	}
	Logs.Warnf(format, args...)
}

func Error(args ...interface{}) {
	if !ready() {
		fmt.Fprint(os.Stderr, "[ERROR] ")
		fmt.Fprintln(os.Stderr, args...)
		return
	}
	Logs.Error(args...)
}

func Errorf(format string, args ...interface{}) {
	if !ready() {
		fmt.Fprintf(os.Stderr, "[ERROR] "+format+"\n", args...)
		return
	}
	Logs.Errorf(format, args...)
}

func Fatal(args ...interface{}) {
	if !ready() {
		fmt.Fprint(os.Stderr, "[FATAL] ")
		fmt.Fprintln(os.Stderr, args...)
		os.Exit(1)
		return
	}
	Logs.Fatal(args...)
}

func Fatalf(format string, args ...interface{}) {
	if !ready() {
		fmt.Fprintf(os.Stderr, "[FATAL] "+format+"\n", args...)
		os.Exit(1)
		return
	}
	Logs.Fatalf(format, args...)
}

func Debug(args ...interface{}) {
	if !ready() {
		return // debug messages are silently dropped before init
	}
	Logs.Debug(args...)
}

func Debugf(format string, args ...interface{}) {
	if !ready() {
		return // debug messages are silently dropped before init
	}
	Logs.Debugf(format, args...)
}

func WithField(key string, value interface{}) *logrus.Entry {
	if !ready() {
		// return a stderr-only entry so chained calls don't panic
		e := logrus.NewEntry(logrus.StandardLogger())
		e.Logger.SetOutput(os.Stderr)
		return e.WithField(key, value)
	}
	return Logs.WithField(key, value)
}

func WithError(err error) *logrus.Entry {
	if !ready() {
		e := logrus.NewEntry(logrus.StandardLogger())
		e.Logger.SetOutput(os.Stderr)
		return e.WithError(err)
	}
	return Logs.WithError(err)
}

func WithErrorNoStack(err error) *logrus.Entry {
	if !ready() {
		e := logrus.NewEntry(logrus.StandardLogger())
		e.Logger.SetOutput(os.Stderr)
		return e.WithField(ErrorNoStackKey, err)
	}
	return Logs.WithField(ErrorNoStackKey, err)
}
