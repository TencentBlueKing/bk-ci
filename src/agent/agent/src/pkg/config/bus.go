package config

import (
	"sync"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

type DataEvent struct {
	Data  any
	Topic DataEventType
}

type DataEventType string

const (
	IpEvent DataEventType = "IP"
)

// DataChannel 是一个能接收 DataEvent 的 channel
type DataChannel struct {
	Id    string
	DChan chan DataEvent
}

// EventBus 存储有关订阅者感兴趣的特定主题的信息
type EventBus struct {
	subscribers map[DataEventType][]DataChannel
	rwLock      sync.RWMutex
}

func (eb *EventBus) Publish(topic DataEventType, data any) {
	go func() {
		eb.rwLock.RLock()
		logs.Infof("EventBus Publish %s %v", topic, data)
		defer eb.rwLock.RUnlock()

		if chs, found := eb.subscribers[topic]; found {
			event := DataEvent{Data: data, Topic: topic}
			for _, ch := range chs {
				// 先尝试非阻塞发送
				select {
				case ch.DChan <- event:
					logs.Debugf("EventBus Publish send %s %v", topic, data)
				default:
					// channel 满了，丢弃最旧的一条再重试
					select {
					case <-ch.DChan:
					default:
					}
					select {
					case ch.DChan <- event:
						logs.Debugf("EventBus Publish send (after discard) %s %v", topic, data)
					default:
						logs.Warnf("EventBus Publish drop %s %v", topic, data)
					}
				}
			}
		}
		logs.Debugf("EventBus Publish send over %s %v", topic, data)
	}()
}

func (eb *EventBus) Subscribe(topic DataEventType, id string, chanBuffer int) DataChannel {
	ch := DataChannel{
		Id:    id,
		DChan: make(chan DataEvent, chanBuffer),
	}
	eb.rwLock.Lock()
	logs.Infof("EventBus Subscribe %s %s", topic, ch.Id)
	defer eb.rwLock.Unlock()
	if prev, found := eb.subscribers[topic]; found {
		eb.subscribers[topic] = append(prev, ch)
	} else {
		eb.subscribers[topic] = append([]DataChannel{}, ch)
	}

	return ch
}

func (eb *EventBus) Unsubscribe(topic DataEventType, id string) {
	eb.rwLock.Lock()
	logs.Infof("EventBus Unsubscribe %s %s", topic, id)
	defer eb.rwLock.Unlock()
	slice := eb.subscribers[topic]
	for index, ch := range slice {
		if ch.Id == id {
			logs.Debugf("EventBus Unsubscribe close %s %s", topic, ch.Id)
			close(ch.DChan)
			eb.subscribers[topic] = append(slice[:index], slice[index+1:]...)
			break
		}
	}
}

var EBus *EventBus

func init() {
	EBus = &EventBus{
		subscribers: map[DataEventType][]DataChannel{},
	}
}
