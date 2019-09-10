package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"

	consumer "github.com/harlow/kinesis-consumer"
	checkpoint "github.com/harlow/kinesis-consumer/checkpoint/redis"
)

func main() {
	var (
		app    = flag.String("app", "", "App name")
		stream = flag.String("stream", "", "Stream name")
	)
	flag.Parse()

	// redis checkpoint
	ck, err := checkpoint.New(*app)
	if err != nil {
		log.Fatalf("checkpoint error: %v", err)
	}

	// consumer
	c, err := consumer.New(
		*stream, consumer.WithCheckpoint(ck),
	)
	if err != nil {
		log.Fatalf("consumer error: %v", err)
	}

	// use cancel func to signal shutdown
	ctx, cancel := context.WithCancel(context.Background())

	// trap SIGINT, wait to trigger shutdown
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, os.Interrupt)

	go func() {
		<-signals
		cancel()
	}()

	// scan stream
	err = c.Scan(ctx, func(r *consumer.Record) consumer.ScanStatus {
		fmt.Println(string(r.Data))

		// continue scanning
		return consumer.ScanStatus{}
	})
	if err != nil {
		log.Fatalf("scan error: %v", err)
	}
}
