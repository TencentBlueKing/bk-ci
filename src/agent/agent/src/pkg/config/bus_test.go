package config

import (
	"bytes"
	"strconv"
	"sync"
	"testing"
	"time"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
)

func TestEventBus(t *testing.T) {
	logs.UNTestDebugInit()
	wg := sync.WaitGroup{}
	wg.Add(1)
	go func() {
		time.Sleep(3 * time.Second)
		ipChan := EBus.Subscribe(IpEvent, "go1", 1)

		defer func() {
			if err := recover(); err != nil {
				logs.Error("agent collect panic: ", err)
			}
			EBus.Unsubscribe(IpEvent, "go1")
			wg.Done()
		}()

		data := <-ipChan.DChan
		if data.Data != "127.0.0.1" {
			t.Errorf("Subscribe error data is %s", data.Data)
		}
		time.Sleep(3 * time.Second)

		var b *bytes.Buffer
		b.Bytes()
	}()

	time.Sleep(1 * time.Second)
	EBus.Publish(IpEvent, "127.0.0.2")
	time.Sleep(3 * time.Second)
	EBus.Publish(IpEvent, "127.0.0.1")
	for i := 3; i <= 8; i++ {
		time.Sleep(1 * time.Second)
		EBus.Publish(IpEvent, "127.0.0."+strconv.Itoa(i))
	}
	wg.Wait()
	if len(EBus.subscribers[IpEvent]) > 0 {
		t.Error("unsubscribe error")
	}
}
