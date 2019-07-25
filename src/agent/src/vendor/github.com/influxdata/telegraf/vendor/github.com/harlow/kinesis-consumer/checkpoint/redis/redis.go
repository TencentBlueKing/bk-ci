package redis

import (
	"fmt"
	"os"

	redis "gopkg.in/redis.v5"
)

const localhost = "127.0.0.1:6379"

// New returns a checkpoint that uses Redis for underlying storage
func New(appName string) (*Checkpoint, error) {
	addr := os.Getenv("REDIS_URL")
	if addr == "" {
		addr = localhost
	}

	client := redis.NewClient(&redis.Options{Addr: addr})

	// verify we can ping server
	_, err := client.Ping().Result()
	if err != nil {
		return nil, err
	}

	return &Checkpoint{
		appName: appName,
		client:  client,
	}, nil
}

// Checkpoint stores and retreives the last evaluated key from a DDB scan
type Checkpoint struct {
	appName string
	client  *redis.Client
}

// Get fetches the checkpoint for a particular Shard.
func (c *Checkpoint) Get(streamName, shardID string) (string, error) {
	val, _ := c.client.Get(c.key(streamName, shardID)).Result()
	return val, nil
}

// Set stores a checkpoint for a shard (e.g. sequence number of last record processed by application).
// Upon failover, record processing is resumed from this point.
func (c *Checkpoint) Set(streamName, shardID, sequenceNumber string) error {
	if sequenceNumber == "" {
		return fmt.Errorf("sequence number should not be empty")
	}
	err := c.client.Set(c.key(streamName, shardID), sequenceNumber, 0).Err()
	if err != nil {
		return err
	}
	return nil
}

// key generates a unique Redis key for storage of Checkpoint.
func (c *Checkpoint) key(streamName, shardID string) string {
	return fmt.Sprintf("%v:checkpoint:%v:%v", c.appName, streamName, shardID)
}
