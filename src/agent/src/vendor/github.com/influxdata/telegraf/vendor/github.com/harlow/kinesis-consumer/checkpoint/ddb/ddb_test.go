package ddb

import (
	"testing"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/dynamodb"
)

type fakeRetryer struct {
	Name string
}

func (r *fakeRetryer) ShouldRetry(err error) bool {
	r.Name = "fakeRetryer"
	return false
}

func TestNewCheckpoint(t *testing.T) {
	c, err := New("", "")
	if c == nil {
		t.Errorf("expected checkpoint client instance. got %v", c)
	}
	if err != nil {
		t.Errorf("new checkpoint error expected nil. got %v", err)
	}
}

func TestCheckpointSetting(t *testing.T) {
	var ck Checkpoint
	ckPtr := &ck

	// Test WithMaxInterval
	setInterval := WithMaxInterval(time.Duration(2 * time.Minute))
	setInterval(ckPtr)

	// Test WithRetryer
	var r fakeRetryer
	setRetryer := WithRetryer(&r)
	setRetryer(ckPtr)

	// Test WithDyanmoDBClient
	var fakeDbClient = dynamodb.New(
		session.New(aws.NewConfig()), &aws.Config{
			Region: aws.String("us-west-2"),
		},
	)
	setDDBClient := WithDynamoClient(fakeDbClient)
	setDDBClient(ckPtr)

	if ckPtr.maxInterval != time.Duration(2*time.Minute) {
		t.Errorf("new checkpoint maxInterval expected 2 minute. got %v", ckPtr.maxInterval)
	}
	if ckPtr.retryer.ShouldRetry(nil) != false {
		t.Errorf("new checkpoint retryer ShouldRetry always returns %v . got %v", false, ckPtr.retryer.ShouldRetry(nil))
	}
	if ckPtr.client != fakeDbClient {
		t.Errorf("new checkpoint dynamodb client reference should be  %p. got %v", &fakeDbClient, ckPtr.client)
	}
}

func TestNewCheckpointWithOptions(t *testing.T) {
	// Test WithMaxInterval
	setInterval := WithMaxInterval(time.Duration(2 * time.Minute))

	// Test WithRetryer
	var r fakeRetryer
	setRetryer := WithRetryer(&r)

	// Test WithDyanmoDBClient
	var fakeDbClient = dynamodb.New(
		session.New(aws.NewConfig()), &aws.Config{
			Region: aws.String("us-west-2"),
		},
	)
	setDDBClient := WithDynamoClient(fakeDbClient)

	ckPtr, err := New("testapp", "testtable", setInterval, setRetryer, setDDBClient)
	if ckPtr == nil {
		t.Errorf("expected checkpoint client instance. got %v", ckPtr)
	}
	if err != nil {
		t.Errorf("new checkpoint error expected nil. got %v", err)
	}
	if ckPtr.appName != "testapp" {
		t.Errorf("new checkpoint app name expected %v. got %v", "testapp", ckPtr.appName)
	}
	if ckPtr.tableName != "testtable" {
		t.Errorf("new checkpoint table expected %v. got %v", "testtable", ckPtr.maxInterval)
	}

	if ckPtr.maxInterval != time.Duration(2*time.Minute) {
		t.Errorf("new checkpoint maxInterval expected 2 minute. got %v", ckPtr.maxInterval)
	}
	if ckPtr.retryer.ShouldRetry(nil) != false {
		t.Errorf("new checkpoint retryer ShouldRetry always returns %v . got %v", false, ckPtr.retryer.ShouldRetry(nil))
	}
	if ckPtr.client != fakeDbClient {
		t.Errorf("new checkpoint dynamodb client reference should be  %p. got %v", &fakeDbClient, ckPtr.client)
	}

}
