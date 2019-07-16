package redis

import (
	"testing"
)

func Test_CheckpointLifecycle(t *testing.T) {
	// new
	c, err := New("app")
	if err != nil {
		t.Fatalf("new checkpoint error: %v", err)
	}

	// set
	c.Set("streamName", "shardID", "testSeqNum")

	// get
	val, err := c.Get("streamName", "shardID")
	if err != nil {
		t.Fatalf("get checkpoint error: %v", err)
	}
	if val != "testSeqNum" {
		t.Fatalf("checkpoint exists expected %s, got %s", "testSeqNum", val)
	}
}

func Test_SetEmptySeqNum(t *testing.T) {
	c, err := New("app")
	if err != nil {
		t.Fatalf("new checkpoint error: %v", err)
	}

	err = c.Set("streamName", "shardID", "")
	if err == nil {
		t.Fatalf("should not allow empty sequence number")
	}
}

func Test_key(t *testing.T) {
	c, err := New("app")
	if err != nil {
		t.Fatalf("new checkpoint error: %v", err)
	}

	want := "app:checkpoint:stream:shard"

	if got := c.key("stream", "shard"); got != want {
		t.Fatalf("checkpoint key, want %s, got %s", want, got)
	}
}
