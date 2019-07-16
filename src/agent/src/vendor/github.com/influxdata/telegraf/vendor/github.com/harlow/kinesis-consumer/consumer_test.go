package consumer

import (
	"context"
	"fmt"
	"sync"
	"testing"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/kinesis"
	"github.com/aws/aws-sdk-go/service/kinesis/kinesisiface"
)

func TestNew(t *testing.T) {
	if _, err := New("myStreamName"); err != nil {
		t.Fatalf("new consumer error: %v", err)
	}
}

func TestConsumer_Scan(t *testing.T) {
	records := []*kinesis.Record{
		{
			Data:           []byte("firstData"),
			SequenceNumber: aws.String("firstSeqNum"),
		},
		{
			Data:           []byte("lastData"),
			SequenceNumber: aws.String("lastSeqNum"),
		},
	}
	client := &kinesisClientMock{
		getShardIteratorMock: func(input *kinesis.GetShardIteratorInput) (*kinesis.GetShardIteratorOutput, error) {
			return &kinesis.GetShardIteratorOutput{
				ShardIterator: aws.String("49578481031144599192696750682534686652010819674221576194"),
			}, nil
		},
		getRecordsMock: func(input *kinesis.GetRecordsInput) (*kinesis.GetRecordsOutput, error) {
			return &kinesis.GetRecordsOutput{
				NextShardIterator: nil,
				Records:           records,
			}, nil
		},
		describeStreamMock: func(input *kinesis.DescribeStreamInput) (*kinesis.DescribeStreamOutput, error) {
			return &kinesis.DescribeStreamOutput{
				StreamDescription: &kinesis.StreamDescription{
					Shards: []*kinesis.Shard{
						{ShardId: aws.String("myShard")},
					},
				},
			}, nil
		},
	}
	var (
		cp  = &fakeCheckpoint{cache: map[string]string{}}
		ctr = &fakeCounter{}
	)

	c, err := New("myStreamName",
		WithClient(client),
		WithCounter(ctr),
		WithCheckpoint(cp),
	)
	if err != nil {
		t.Fatalf("new consumer error: %v", err)
	}

	var resultData string
	var fnCallCounter int
	var fn = func(r *Record) ScanStatus {
		fnCallCounter++
		resultData += string(r.Data)
		return ScanStatus{}
	}

	if err := c.Scan(context.Background(), fn); err != nil {
		t.Errorf("scan shard error expected nil. got %v", err)
	}

	if resultData != "firstDatalastData" {
		t.Errorf("callback error expected %s, got %s", "firstDatalastData", resultData)
	}
	if fnCallCounter != 2 {
		t.Errorf("the callback function expects %v, got %v", 2, fnCallCounter)
	}
	if val := ctr.counter; val != 2 {
		t.Errorf("counter error expected %d, got %d", 2, val)
	}

	val, err := cp.Get("myStreamName", "myShard")
	if err != nil && val != "lastSeqNum" {
		t.Errorf("checkout error expected %s, got %s", "lastSeqNum", val)
	}
}

func TestConsumer_Scan_NoShardsAvailable(t *testing.T) {
	client := &kinesisClientMock{
		describeStreamMock: func(input *kinesis.DescribeStreamInput) (*kinesis.DescribeStreamOutput, error) {
			return &kinesis.DescribeStreamOutput{
				StreamDescription: &kinesis.StreamDescription{
					Shards: make([]*kinesis.Shard, 0),
				},
			}, nil
		},
	}
	var (
		cp  = &fakeCheckpoint{cache: map[string]string{}}
		ctr = &fakeCounter{}
	)

	c, err := New("myStreamName",
		WithClient(client),
		WithCounter(ctr),
		WithCheckpoint(cp),
	)
	if err != nil {
		t.Fatalf("new consumer error: %v", err)
	}

	var fnCallCounter int
	var fn = func(r *Record) ScanStatus {
		fnCallCounter++
		return ScanStatus{}
	}

	if err := c.Scan(context.Background(), fn); err == nil {
		t.Errorf("scan shard error expected not nil. got %v", err)
	}

	if fnCallCounter != 0 {
		t.Errorf("the callback function expects %v, got %v", 0, fnCallCounter)
	}
	if val := ctr.counter; val != 0 {
		t.Errorf("counter error expected %d, got %d", 0, val)
	}
	val, err := cp.Get("myStreamName", "myShard")
	if err != nil && val != "" {
		t.Errorf("checkout error expected %s, got %s", "", val)
	}
}

func TestScanShard(t *testing.T) {
	var records = []*kinesis.Record{
		{
			Data:           []byte("firstData"),
			SequenceNumber: aws.String("firstSeqNum"),
		},
		{
			Data:           []byte("lastData"),
			SequenceNumber: aws.String("lastSeqNum"),
		},
	}

	var client = &kinesisClientMock{
		getShardIteratorMock: func(input *kinesis.GetShardIteratorInput) (*kinesis.GetShardIteratorOutput, error) {
			return &kinesis.GetShardIteratorOutput{
				ShardIterator: aws.String("49578481031144599192696750682534686652010819674221576194"),
			}, nil
		},
		getRecordsMock: func(input *kinesis.GetRecordsInput) (*kinesis.GetRecordsOutput, error) {
			return &kinesis.GetRecordsOutput{
				NextShardIterator: nil,
				Records:           records,
			}, nil
		},
	}

	var (
		cp  = &fakeCheckpoint{cache: map[string]string{}}
		ctr = &fakeCounter{}
	)

	c, err := New("myStreamName",
		WithClient(client),
		WithCounter(ctr),
		WithCheckpoint(cp),
	)
	if err != nil {
		t.Fatalf("new consumer error: %v", err)
	}

	// callback fn appends record data
	var res string
	var fn = func(r *Record) ScanStatus {
		res += string(r.Data)
		return ScanStatus{}
	}

	// scan shard
	if err := c.ScanShard(context.Background(), "myShard", fn); err != nil {
		t.Fatalf("scan shard error: %v", err)
	}

	// runs callback func
	if res != "firstDatalastData" {
		t.Fatalf("callback error expected %s, got %s", "firstDatalastData", res)
	}

	// increments counter
	if val := ctr.counter; val != 2 {
		t.Fatalf("counter error expected %d, got %d", 2, val)
	}

	// sets checkpoint
	val, err := cp.Get("myStreamName", "myShard")
	if err != nil && val != "lastSeqNum" {
		t.Fatalf("checkout error expected %s, got %s", "lastSeqNum", val)
	}
}

func TestScanShard_StopScan(t *testing.T) {
	var records = []*kinesis.Record{
		{
			Data:           []byte("firstData"),
			SequenceNumber: aws.String("firstSeqNum"),
		},
		{
			Data:           []byte("lastData"),
			SequenceNumber: aws.String("lastSeqNum"),
		},
	}

	var client = &kinesisClientMock{
		getShardIteratorMock: func(input *kinesis.GetShardIteratorInput) (*kinesis.GetShardIteratorOutput, error) {
			return &kinesis.GetShardIteratorOutput{
				ShardIterator: aws.String("49578481031144599192696750682534686652010819674221576194"),
			}, nil
		},
		getRecordsMock: func(input *kinesis.GetRecordsInput) (*kinesis.GetRecordsOutput, error) {
			return &kinesis.GetRecordsOutput{
				NextShardIterator: nil,
				Records:           records,
			}, nil
		},
	}

	c, err := New("myStreamName", WithClient(client))
	if err != nil {
		t.Fatalf("new consumer error: %v", err)
	}

	// callback fn appends record data
	var res string
	var fn = func(r *Record) ScanStatus {
		res += string(r.Data)
		return ScanStatus{StopScan: true}
	}

	if err := c.ScanShard(context.Background(), "myShard", fn); err != nil {
		t.Fatalf("scan shard error: %v", err)
	}

	if res != "firstData" {
		t.Fatalf("callback error expected %s, got %s", "firstData", res)
	}
}

func TestScanShard_ShardIsClosed(t *testing.T) {
	var client = &kinesisClientMock{
		getShardIteratorMock: func(input *kinesis.GetShardIteratorInput) (*kinesis.GetShardIteratorOutput, error) {
			return &kinesis.GetShardIteratorOutput{
				ShardIterator: aws.String("49578481031144599192696750682534686652010819674221576194"),
			}, nil
		},
		getRecordsMock: func(input *kinesis.GetRecordsInput) (*kinesis.GetRecordsOutput, error) {
			return &kinesis.GetRecordsOutput{
				NextShardIterator: nil,
				Records:           make([]*Record, 0),
			}, nil
		},
	}

	c, err := New("myStreamName", WithClient(client))
	if err != nil {
		t.Fatalf("new consumer error: %v", err)
	}

	var fn = func(r *Record) ScanStatus {
		return ScanStatus{StopScan: true}
	}

	if err := c.ScanShard(context.Background(), "myShard", fn); err != nil {
		t.Fatalf("scan shard error: %v", err)
	}
}

type kinesisClientMock struct {
	kinesisiface.KinesisAPI
	getShardIteratorMock func(*kinesis.GetShardIteratorInput) (*kinesis.GetShardIteratorOutput, error)
	getRecordsMock       func(*kinesis.GetRecordsInput) (*kinesis.GetRecordsOutput, error)
	describeStreamMock   func(*kinesis.DescribeStreamInput) (*kinesis.DescribeStreamOutput, error)
}

func (c *kinesisClientMock) GetRecords(in *kinesis.GetRecordsInput) (*kinesis.GetRecordsOutput, error) {
	return c.getRecordsMock(in)
}

func (c *kinesisClientMock) GetShardIterator(in *kinesis.GetShardIteratorInput) (*kinesis.GetShardIteratorOutput, error) {
	return c.getShardIteratorMock(in)
}

func (c *kinesisClientMock) DescribeStream(in *kinesis.DescribeStreamInput) (*kinesis.DescribeStreamOutput, error) {
	return c.describeStreamMock(in)
}

// implementation of checkpoint
type fakeCheckpoint struct {
	cache map[string]string
	mu    sync.Mutex
}

func (fc *fakeCheckpoint) Set(streamName, shardID, sequenceNumber string) error {
	fc.mu.Lock()
	defer fc.mu.Unlock()

	key := fmt.Sprintf("%s-%s", streamName, shardID)
	fc.cache[key] = sequenceNumber
	return nil
}

func (fc *fakeCheckpoint) Get(streamName, shardID string) (string, error) {
	fc.mu.Lock()
	defer fc.mu.Unlock()

	key := fmt.Sprintf("%s-%s", streamName, shardID)
	return fc.cache[key], nil
}

// implementation of counter
type fakeCounter struct {
	counter int64
}

func (fc *fakeCounter) Add(streamName string, count int64) {
	fc.counter += count
}
