package postgres

import (
	"database/sql"
	"fmt"
	"testing"
	"time"

	"github.com/pkg/errors"
	sqlmock "gopkg.in/DATA-DOG/go-sqlmock.v1"
)

func TestNew(t *testing.T) {
	appName := "streamConsumer"
	tableName := "checkpoint"
	connString := "UserID=root;Password=myPassword;Host=localhost;Port=5432;Database=myDataBase;"
	ck, err := New(appName, tableName, connString)

	if ck == nil {
		t.Errorf("expected checkpointer not equal nil, but got %v", ck)
	}
	if err != nil {
		t.Errorf("expected error equals nil, but got %v", err)
	}
	ck.Shutdown()
}

func TestNew_AppNameEmpty(t *testing.T) {
	appName := ""
	tableName := "checkpoint"
	connString := ""
	ck, err := New(appName, tableName, connString)

	if ck != nil {
		t.Errorf("expected checkpointer equal nil, but got %v", ck)
	}
	if err == nil {
		t.Errorf("expected error equals not nil, but got %v", err)
	}
}

func TestNew_TableNameEmpty(t *testing.T) {
	appName := "streamConsumer"
	tableName := ""
	connString := ""
	ck, err := New(appName, tableName, connString)

	if ck != nil {
		t.Errorf("expected checkpointer equal nil, but got %v", ck)
	}
	if err == nil {
		t.Errorf("expected error equals not nil, but got %v", err)
	}
}

func TestNew_WithMaxIntervalOption(t *testing.T) {
	appName := "streamConsumer"
	tableName := "checkpoint"
	connString := "UserID=root;Password=myPassword;Host=localhost;Port=5432;Database=myDataBase;"
	maxInterval := time.Second
	ck, err := New(appName, tableName, connString, WithMaxInterval(maxInterval))

	if ck == nil {
		t.Errorf("expected checkpointer not equal nil, but got %v", ck)
	}
	if ck.GetMaxInterval() != time.Second {
		t.Errorf("expected max interval equals %v, but got %v", maxInterval, ck.GetMaxInterval())
	}
	if err != nil {
		t.Errorf("expected error equals nil, but got %v", err)
	}
	ck.Shutdown()
}

func TestCheckpoint_Get(t *testing.T) {
	appName := "streamConsumer"
	tableName := "checkpoint"
	connString := "UserID=root;Password=myPassword;Host=localhost;Port=5432;Database=myDataBase;"
	streamName := "myStreamName"
	shardID := "shardId-00000000"
	expectedSequenceNumber := "49578481031144599192696750682534686652010819674221576194"
	maxInterval := time.Second
	connMock, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("error occurred during the sqlmock creation. cause: %v", err)
	}
	ck, err := New(appName, tableName, connString, WithMaxInterval(maxInterval))
	if err != nil {
		t.Fatalf("error occurred during the checkpoint creation. cause: %v", err)
	}
	ck.SetConn(connMock) // nolint: gotypex, the function available only in test

	rows := []string{"sequence_number"}
	namespace := fmt.Sprintf("%s-%s", appName, streamName)
	expectedRows := sqlmock.NewRows(rows)
	expectedRows.AddRow(expectedSequenceNumber)
	expectedSQLRegexString := fmt.Sprintf(`SELECT sequence_number FROM %s WHERE namespace=\$1 AND shard_id=\$2;`,
		tableName)
	mock.ExpectQuery(expectedSQLRegexString).WithArgs(namespace, shardID).WillReturnRows(expectedRows)

	gotSequenceNumber, err := ck.Get(streamName, shardID)

	if gotSequenceNumber != expectedSequenceNumber {
		t.Errorf("expected sequence number equals %v, but got %v", expectedSequenceNumber, gotSequenceNumber)
	}
	if err != nil {
		t.Errorf("expected error equals nil, but got %v", err)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
	ck.Shutdown()
}

func TestCheckpoint_Get_NoRows(t *testing.T) {
	appName := "streamConsumer"
	tableName := "checkpoint"
	connString := "UserID=root;Password=myPassword;Host=localhost;Port=5432;Database=myDataBase;"
	streamName := "myStreamName"
	shardID := "shardId-00000000"
	maxInterval := time.Second
	connMock, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("error occurred during the sqlmock creation. cause: %v", err)
	}
	ck, err := New(appName, tableName, connString, WithMaxInterval(maxInterval))
	if err != nil {
		t.Fatalf("error occurred during the checkpoint creation. cause: %v", err)
	}
	ck.SetConn(connMock) // nolint: gotypex, the function available only in test

	namespace := fmt.Sprintf("%s-%s", appName, streamName)
	expectedSQLRegexString := fmt.Sprintf(`SELECT sequence_number FROM %s WHERE namespace=\$1 AND shard_id=\$2;`,
		tableName)
	mock.ExpectQuery(expectedSQLRegexString).WithArgs(namespace, shardID).WillReturnError(sql.ErrNoRows)

	gotSequenceNumber, err := ck.Get(streamName, shardID)

	if gotSequenceNumber != "" {
		t.Errorf("expected sequence number equals empty, but got %v", gotSequenceNumber)
	}
	if err != nil {
		t.Errorf("expected error equals nil, but got %v", err)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
	ck.Shutdown()
}

func TestCheckpoint_Get_QueryError(t *testing.T) {
	appName := "streamConsumer"
	tableName := "checkpoint"
	connString := "UserID=root;Password=myPassword;Host=localhost;Port=5432;Database=myDataBase;"
	streamName := "myStreamName"
	shardID := "shardId-00000000"
	maxInterval := time.Second
	connMock, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("error occurred during the sqlmock creation. cause: %v", err)
	}
	ck, err := New(appName, tableName, connString, WithMaxInterval(maxInterval))
	if err != nil {
		t.Fatalf("error occurred during the checkpoint creation. cause: %v", err)
	}
	ck.SetConn(connMock) // nolint: gotypex, the function available only in test

	namespace := fmt.Sprintf("%s-%s", appName, streamName)
	expectedSQLRegexString := fmt.Sprintf(`SELECT sequence_number FROM %s WHERE namespace=\$1 AND shard_id=\$2;`,
		tableName)
	mock.ExpectQuery(expectedSQLRegexString).WithArgs(namespace, shardID).WillReturnError(errors.New("an error"))

	gotSequenceNumber, err := ck.Get(streamName, shardID)

	if gotSequenceNumber != "" {
		t.Errorf("expected sequence number equals empty, but got %v", gotSequenceNumber)
	}
	if err == nil {
		t.Errorf("expected error equals not nil, but got %v", err)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
	ck.Shutdown()
}

func TestCheckpoint_Set(t *testing.T) {
	appName := "streamConsumer"
	tableName := "checkpoint"
	connString := "UserID=root;Password=myPassword;Host=localhost;Port=5432;Database=myDataBase;"
	streamName := "myStreamName"
	shardID := "shardId-00000000"
	expectedSequenceNumber := "49578481031144599192696750682534686652010819674221576194"
	maxInterval := time.Second
	ck, err := New(appName, tableName, connString, WithMaxInterval(maxInterval))
	if err != nil {
		t.Fatalf("error occurred during the checkpoint creation. cause: %v", err)
	}

	err = ck.Set(streamName, shardID, expectedSequenceNumber)

	if err != nil {
		t.Errorf("expected error equals nil, but got %v", err)
	}
	ck.Shutdown()
}

func TestCheckpoint_Set_SequenceNumberEmpty(t *testing.T) {
	appName := "streamConsumer"
	tableName := "checkpoint"
	connString := "UserID=root;Password=myPassword;Host=localhost;Port=5432;Database=myDataBase;"
	streamName := "myStreamName"
	shardID := "shardId-00000000"
	expectedSequenceNumber := ""
	maxInterval := time.Second
	ck, err := New(appName, tableName, connString, WithMaxInterval(maxInterval))
	if err != nil {
		t.Fatalf("error occurred during the checkpoint creation. cause: %v", err)
	}

	err = ck.Set(streamName, shardID, expectedSequenceNumber)

	if err == nil {
		t.Errorf("expected error equals not nil, but got %v", err)
	}
	ck.Shutdown()
}

func TestCheckpoint_Shutdown(t *testing.T) {
	appName := "streamConsumer"
	tableName := "checkpoint"
	connString := "UserID=root;Password=myPassword;Host=localhost;Port=5432;Database=myDataBase;"
	streamName := "myStreamName"
	shardID := "shardId-00000000"
	expectedSequenceNumber := "49578481031144599192696750682534686652010819674221576194"
	maxInterval := time.Second
	connMock, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("error occurred during the sqlmock creation. cause: %v", err)
	}
	ck, err := New(appName, tableName, connString, WithMaxInterval(maxInterval))
	if err != nil {
		t.Fatalf("error occurred during the checkpoint creation. cause: %v", err)
	}
	ck.SetConn(connMock) // nolint: gotypex, the function available only in test

	namespace := fmt.Sprintf("%s-%s", appName, streamName)
	expectedSQLRegexString := fmt.Sprintf(`INSERT INTO %s \(namespace, shard_id, sequence_number\) VALUES\(\$1, \$2, \$3\) ON CONFLICT \(namespace, shard_id\) DO UPDATE SET sequence_number= \$3;`, tableName)
	result := sqlmock.NewResult(0, 1)
	mock.ExpectExec(expectedSQLRegexString).WithArgs(namespace, shardID, expectedSequenceNumber).WillReturnResult(result)

	err = ck.Set(streamName, shardID, expectedSequenceNumber)

	if err != nil {
		t.Fatalf("unable to set checkpoint for data initialization. cause: %v", err)
	}

	err = ck.Shutdown()

	if err != nil {
		t.Errorf("expected error equals not nil, but got %v", err)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
}

func TestCheckpoint_Shutdown_SaveError(t *testing.T) {
	appName := "streamConsumer"
	tableName := "checkpoint"
	connString := "UserID=root;Password=myPassword;Host=localhost;Port=5432;Database=myDataBase;"
	streamName := "myStreamName"
	shardID := "shardId-00000000"
	expectedSequenceNumber := "49578481031144599192696750682534686652010819674221576194"
	maxInterval := time.Second
	connMock, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("error occurred during the sqlmock creation. cause: %v", err)
	}
	ck, err := New(appName, tableName, connString, WithMaxInterval(maxInterval))
	if err != nil {
		t.Fatalf("error occurred during the checkpoint creation. cause: %v", err)
	}
	ck.SetConn(connMock) // nolint: gotypex, the function available only in test

	namespace := fmt.Sprintf("%s-%s", appName, streamName)
	expectedSQLRegexString := fmt.Sprintf(`INSERT INTO %s \(namespace, shard_id, sequence_number\) VALUES\(\$1, \$2, \$3\) ON CONFLICT \(namespace, shard_id\) DO UPDATE SET sequence_number= \$3;`, tableName)
	mock.ExpectExec(expectedSQLRegexString).WithArgs(namespace, shardID, expectedSequenceNumber).WillReturnError(errors.New("an error"))

	err = ck.Set(streamName, shardID, expectedSequenceNumber)

	if err != nil {
		t.Fatalf("unable to set checkpoint for data initialization. cause: %v", err)
	}

	err = ck.Shutdown()

	if err == nil {
		t.Errorf("expected error equals nil, but got %v", err)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
}
