package main

import (
	"context"
	"expvar"
	"flag"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"os/signal"

	alog "github.com/apex/log"
	"github.com/apex/log/handlers/text"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/awserr"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/dynamodb"
	"github.com/aws/aws-sdk-go/service/kinesis"
	consumer "github.com/harlow/kinesis-consumer"
	checkpoint "github.com/harlow/kinesis-consumer/checkpoint/ddb"
)

// kick off a server for exposing scan metrics
func init() {
	sock, err := net.Listen("tcp", "localhost:8080")
	if err != nil {
		log.Printf("net listen error: %v", err)
	}
	go func() {
		fmt.Println("Metrics available at http://localhost:8080/debug/vars")
		http.Serve(sock, nil)
	}()
}

// A myLogger provides a minimalistic logger satisfying the Logger interface.
type myLogger struct {
	logger alog.Logger
}

// Log logs the parameters to the stdlib logger. See log.Println.
func (l *myLogger) Log(args ...interface{}) {
	l.logger.Infof("producer: %v", args...)
}

func main() {
	// Wrap myLogger around  apex logger
	log := &myLogger{
		logger: alog.Logger{
			Handler: text.New(os.Stdout),
			Level:   alog.DebugLevel,
		},
	}

	var (
		app    = flag.String("app", "", "App name")
		stream = flag.String("stream", "", "Stream name")
		table  = flag.String("table", "", "Checkpoint table name")
	)
	flag.Parse()

	sess, err := session.NewSession(aws.NewConfig())
	if err != nil {
		log.Log("new session error: %v", err)
	}

	// New Kinesis and DynamoDB clients (if you need custom config)
	myKsis := kinesis.New(sess)
	myDdbClient := dynamodb.New(sess)

	// ddb checkpoint
	ck, err := checkpoint.New(*app, *table, checkpoint.WithDynamoClient(myDdbClient), checkpoint.WithRetryer(&MyRetryer{}))
	if err != nil {
		log.Log("checkpoint error: %v", err)
	}

	// expvar counter
	var counter = expvar.NewMap("counters")

	// consumer
	c, err := consumer.New(
		*stream,
		consumer.WithCheckpoint(ck),
		consumer.WithLogger(log),
		consumer.WithCounter(counter),
		consumer.WithClient(myKsis),
	)
	if err != nil {
		log.Log("consumer error: %v", err)
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
		log.Log("scan error: %v", err)
	}

	if err := ck.Shutdown(); err != nil {
		log.Log("checkpoint shutdown error: %v", err)
	}
}

// MyRetryer used for checkpointing
type MyRetryer struct {
	checkpoint.Retryer
}

// ShouldRetry implements custom logic for when a checkpont should retry
func (r *MyRetryer) ShouldRetry(err error) bool {
	if awsErr, ok := err.(awserr.Error); ok {
		switch awsErr.Code() {
		case dynamodb.ErrCodeProvisionedThroughputExceededException, dynamodb.ErrCodeLimitExceededException:
			return true
		default:
			return false
		}
	}
	return false
}
