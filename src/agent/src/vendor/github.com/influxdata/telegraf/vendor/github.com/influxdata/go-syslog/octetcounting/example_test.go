package octetcounting

import (
	"github.com/influxdata/go-syslog"
	"io"
	"strings"
	"time"

	"github.com/davecgh/go-spew/spew"
)

func output(out interface{}) {
	spew.Config.DisableCapacities = true
	spew.Config.DisablePointerAddresses = true
	spew.Dump(out)
}

func Example() {
	results := []syslog.Result{}
	acc := func(res *syslog.Result) {
		results = append(results, *res)
	}
	r := strings.NewReader("48 <1>1 2003-10-11T22:14:15.003Z host.local - - - -25 <3>1 - host.local - - - -38 <2>1 - host.local su - - - κόσμε")
	NewParser(syslog.WithBestEffort(), syslog.WithListener(acc)).Parse(r)
	output(results)
	// Output:
	// ([]syslog.Result) (len=3) {
	//  (syslog.Result) {
	//   Message: (*rfc5424.SyslogMessage)({
	//    priority: (*uint8)(1),
	//    facility: (*uint8)(0),
	//    severity: (*uint8)(1),
	//    version: (uint16) 1,
	//    timestamp: (*time.Time)(2003-10-11 22:14:15.003 +0000 UTC),
	//    hostname: (*string)((len=10) "host.local"),
	//    appname: (*string)(<nil>),
	//    procID: (*string)(<nil>),
	//    msgID: (*string)(<nil>),
	//    structuredData: (*map[string]map[string]string)(<nil>),
	//    message: (*string)(<nil>)
	//   }),
	//   Error: (error) <nil>
	//  },
	//  (syslog.Result) {
	//   Message: (*rfc5424.SyslogMessage)({
	//    priority: (*uint8)(3),
	//    facility: (*uint8)(0),
	//    severity: (*uint8)(3),
	//    version: (uint16) 1,
	//    timestamp: (*time.Time)(<nil>),
	//    hostname: (*string)((len=10) "host.local"),
	//    appname: (*string)(<nil>),
	//    procID: (*string)(<nil>),
	//    msgID: (*string)(<nil>),
	//    structuredData: (*map[string]map[string]string)(<nil>),
	//    message: (*string)(<nil>)
	//   }),
	//   Error: (error) <nil>
	//  },
	//  (syslog.Result) {
	//   Message: (*rfc5424.SyslogMessage)({
	//    priority: (*uint8)(2),
	//    facility: (*uint8)(0),
	//    severity: (*uint8)(2),
	//    version: (uint16) 1,
	//    timestamp: (*time.Time)(<nil>),
	//    hostname: (*string)((len=10) "host.local"),
	//    appname: (*string)((len=2) "su"),
	//    procID: (*string)(<nil>),
	//    msgID: (*string)(<nil>),
	//    structuredData: (*map[string]map[string]string)(<nil>),
	//    message: (*string)((len=11) "κόσμε")
	//   }),
	//   Error: (error) <nil>
	//  }
	// }
}

func Example_channel() {
	messages := []string{
		"16 <1>1 - - - - - -",
		"17 <2>12 A B C D E -",
		"16 <1>1",
	}

	r, w := io.Pipe()

	go func() {
		defer w.Close()

		for _, m := range messages {
			w.Write([]byte(m))
			time.Sleep(time.Millisecond * 220)
		}
	}()

	c := make(chan syslog.Result)
	emit := func(res *syslog.Result) {
		c <- *res
	}

	parser := NewParser(syslog.WithBestEffort(), syslog.WithListener(emit))
	go func() {
		defer close(c)
		parser.Parse(r)
	}()

	for r := range c {
		output(r)
	}

	r.Close()

	// Output:
	// (syslog.Result) {
	//  Message: (*rfc5424.SyslogMessage)({
	//   priority: (*uint8)(1),
	//   facility: (*uint8)(0),
	//   severity: (*uint8)(1),
	//   version: (uint16) 1,
	//   timestamp: (*time.Time)(<nil>),
	//   hostname: (*string)(<nil>),
	//   appname: (*string)(<nil>),
	//   procID: (*string)(<nil>),
	//   msgID: (*string)(<nil>),
	//   structuredData: (*map[string]map[string]string)(<nil>),
	//   message: (*string)(<nil>)
	//  }),
	//  Error: (error) <nil>
	// }
	// (syslog.Result) {
	//  Message: (*rfc5424.SyslogMessage)({
	//   priority: (*uint8)(2),
	//   facility: (*uint8)(0),
	//   severity: (*uint8)(2),
	//   version: (uint16) 12,
	//   timestamp: (*time.Time)(<nil>),
	//   hostname: (*string)(<nil>),
	//   appname: (*string)(<nil>),
	//   procID: (*string)(<nil>),
	//   msgID: (*string)(<nil>),
	//   structuredData: (*map[string]map[string]string)(<nil>),
	//   message: (*string)(<nil>)
	//  }),
	//  Error: (*errors.errorString)(expecting a RFC3339MICRO timestamp or a nil value [col 6])
	// }
	// (syslog.Result) {
	//  Message: (*rfc5424.SyslogMessage)({
	//   priority: (*uint8)(1),
	//   facility: (*uint8)(0),
	//   severity: (*uint8)(1),
	//   version: (uint16) 1,
	//   timestamp: (*time.Time)(<nil>),
	//   hostname: (*string)(<nil>),
	//   appname: (*string)(<nil>),
	//   procID: (*string)(<nil>),
	//   msgID: (*string)(<nil>),
	//   structuredData: (*map[string]map[string]string)(<nil>),
	//   message: (*string)(<nil>)
	//  }),
	//  Error: (*errors.errorString)(parsing error [col 4])
	// }
}
