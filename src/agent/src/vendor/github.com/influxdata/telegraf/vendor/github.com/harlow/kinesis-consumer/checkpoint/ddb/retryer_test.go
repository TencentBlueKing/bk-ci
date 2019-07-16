package ddb

import (
	"errors"
	"testing"

	"github.com/aws/aws-sdk-go/aws/awserr"
	"github.com/aws/aws-sdk-go/service/dynamodb"
)

func TestDefaultRetyer(t *testing.T) {
	retryableError := awserr.New(dynamodb.ErrCodeProvisionedThroughputExceededException, "error is retryable", errors.New("don't care what is here"))
	// retryer is not nil and should returns according to what error is passed in.
	q := &DefaultRetryer{}
	if q.ShouldRetry(retryableError) != true {
		t.Errorf("expected ShouldRetry returns %v. got %v", false, q.ShouldRetry(retryableError))
	}

	nonRetryableError := awserr.New(dynamodb.ErrCodeBackupInUseException, "error is not retryable", errors.New("don't care what is here"))
	shouldRetry := q.ShouldRetry(nonRetryableError)
	if shouldRetry != false {
		t.Errorf("expected ShouldRetry returns %v. got %v", true, shouldRetry)
	}
}
