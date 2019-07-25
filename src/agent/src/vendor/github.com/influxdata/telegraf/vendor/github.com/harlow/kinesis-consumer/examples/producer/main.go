package main

import (
	"bufio"
	"flag"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/kinesis"
)

func main() {
	var streamName = flag.String("stream", "", "Stream name")
	flag.Parse()

	// open dummy user data
	f, err := os.Open("users.txt")
	if err != nil {
		log.Fatal("Cannot open users.txt file")
	}
	defer f.Close()

	var records []*kinesis.PutRecordsRequestEntry
	var client = kinesis.New(session.New())

	// loop over file data
	b := bufio.NewScanner(f)
	for b.Scan() {
		records = append(records, &kinesis.PutRecordsRequestEntry{
			Data:         b.Bytes(),
			PartitionKey: aws.String(time.Now().Format(time.RFC3339Nano)),
		})

		if len(records) > 250 {
			putRecords(client, streamName, records)
			records = nil
		}
	}

	if len(records) > 0 {
		putRecords(client, streamName, records)
	}
}

func putRecords(client *kinesis.Kinesis, streamName *string, records []*kinesis.PutRecordsRequestEntry) {
	_, err := client.PutRecords(&kinesis.PutRecordsInput{
		StreamName: streamName,
		Records:    records,
	})
	if err != nil {
		log.Fatalf("error putting records: %v", err)
	}
	fmt.Print(".")
}
