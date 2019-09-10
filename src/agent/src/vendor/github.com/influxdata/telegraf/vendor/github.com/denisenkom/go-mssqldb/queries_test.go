package mssql

import (
	"bytes"
	"context"
	"database/sql"
	"database/sql/driver"
	"fmt"
	"log"
	"math"
	"net"
	"reflect"
	"strings"
	"sync"
	"testing"
	"time"
)

func driverWithProcess(t *testing.T) *Driver {
	return &Driver{
		log:              optionalLogger{testLogger{t}},
		processQueryText: true,
	}
}
func driverNoProcess(t *testing.T) *Driver {
	return &Driver{
		log:              optionalLogger{testLogger{t}},
		processQueryText: false,
	}
}

func TestSelect(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	t.Run("scan into interface{}", func(t *testing.T) {
		type testStruct struct {
			sql string
			val interface{}
		}

		longstr := strings.Repeat("x", 10000)

		values := []testStruct{
			{"1", int64(1)},
			{"-1", int64(-1)},
			{"cast(1 as int)", int64(1)},
			{"cast(-1 as int)", int64(-1)},
			{"cast(1 as tinyint)", int64(1)},
			{"cast(255 as tinyint)", int64(255)},
			{"cast(1 as smallint)", int64(1)},
			{"cast(-1 as smallint)", int64(-1)},
			{"cast(1 as bigint)", int64(1)},
			{"cast(-1 as bigint)", int64(-1)},
			{"cast(1 as bit)", true},
			{"cast(0 as bit)", false},
			{"'abc'", string("abc")},
			{"cast(0.5 as float)", float64(0.5)},
			{"cast(0.5 as real)", float64(0.5)},
			{"cast(1 as decimal)", []byte("1")},
			{"cast(1.2345 as money)", []byte("1.2345")},
			{"cast(-1.2345 as money)", []byte("-1.2345")},
			{"cast(1.2345 as smallmoney)", []byte("1.2345")},
			{"cast(-1.2345 as smallmoney)", []byte("-1.2345")},
			{"cast(0.5 as decimal(18,1))", []byte("0.5")},
			{"cast(-0.5 as decimal(18,1))", []byte("-0.5")},
			{"cast(-0.5 as numeric(18,1))", []byte("-0.5")},
			{"cast(4294967296 as numeric(20,0))", []byte("4294967296")},
			{"cast(-0.5 as numeric(18,2))", []byte("-0.50")},
			{"N'abc'", string("abc")},
			{"cast(null as nvarchar(3))", nil},
			{"NULL", nil},
			{"cast('1753-01-01' as datetime)", time.Date(1753, 1, 1, 0, 0, 0, 0, time.UTC)},
			{"cast('2000-01-01' as datetime)", time.Date(2000, 1, 1, 0, 0, 0, 0, time.UTC)},
			{"cast('2000-01-01T12:13:14.12' as datetime)",
				time.Date(2000, 1, 1, 12, 13, 14, 120000000, time.UTC)},
			{"cast('2014-06-26 11:08:09.673' as datetime)", time.Date(2014, 06, 26, 11, 8, 9, 673000000, time.UTC)},
			{"cast('9999-12-31T23:59:59.997' as datetime)", time.Date(9999, 12, 31, 23, 59, 59, 997000000, time.UTC)},
			{"cast(NULL as datetime)", nil},
			{"cast('1900-01-01T00:00:00' as smalldatetime)",
				time.Date(1900, 1, 1, 0, 0, 0, 0, time.UTC)},
			{"cast('2000-01-01T12:13:00' as smalldatetime)",
				time.Date(2000, 1, 1, 12, 13, 0, 0, time.UTC)},
			{"cast('2079-06-06T23:59:00' as smalldatetime)",
				time.Date(2079, 6, 6, 23, 59, 0, 0, time.UTC)},
			{"cast(NULL as smalldatetime)", nil},
			{"cast(0x6F9619FF8B86D011B42D00C04FC964FF as uniqueidentifier)",
				[]byte{0x6F, 0x96, 0x19, 0xFF, 0x8B, 0x86, 0xD0, 0x11, 0xB4, 0x2D, 0x00, 0xC0, 0x4F, 0xC9, 0x64, 0xFF}},
			{"cast(NULL as uniqueidentifier)", nil},
			{"cast(0x1234 as varbinary(2))", []byte{0x12, 0x34}},
			{"cast(N'abc' as nvarchar(max))", "abc"},
			{"cast(null as nvarchar(max))", nil},
			{"cast('<root/>' as xml)", "<root/>"},
			{"cast('abc' as text)", "abc"},
			{"cast(null as text)", nil},
			{"cast(N'abc' as ntext)", "abc"},
			{"cast(0x1234 as image)", []byte{0x12, 0x34}},
			{"cast('abc' as char(3))", "abc"},
			{"cast('abc' as varchar(3))", "abc"},
			{"cast(N'проверка' as nvarchar(max))", "проверка"},
			{"cast(N'Δοκιμή' as nvarchar(max))", "Δοκιμή"},
			{"cast(cast(N'สวัสดี' as nvarchar(max)) collate Thai_CI_AI as varchar(max))", "สวัสดี"},                // cp874
			{"cast(cast(N'你好' as nvarchar(max)) collate Chinese_PRC_CI_AI as varchar(max))", "你好"},                 // cp936
			{"cast(cast(N'こんにちは' as nvarchar(max)) collate Japanese_CI_AI as varchar(max))", "こんにちは"},              // cp939
			{"cast(cast(N'안녕하세요.' as nvarchar(max)) collate Korean_90_CI_AI as varchar(max))", "안녕하세요."},           // cp949
			{"cast(cast(N'你好' as nvarchar(max)) collate Chinese_Hong_Kong_Stroke_90_CI_AI as varchar(max))", "你好"}, // cp950
			{"cast(cast(N'cześć' as nvarchar(max)) collate Polish_CI_AI as varchar(max))", "cześć"},                // cp1250
			{"cast(cast(N'Алло' as nvarchar(max)) collate Cyrillic_General_CI_AI as varchar(max))", "Алло"},        // cp1251
			{"cast(cast(N'Bonjour' as nvarchar(max)) collate French_CI_AI as varchar(max))", "Bonjour"},            // cp1252
			{"cast(cast(N'Γεια σας' as nvarchar(max)) collate Greek_CI_AI as varchar(max))", "Γεια σας"},           // cp1253
			{"cast(cast(N'Merhaba' as nvarchar(max)) collate Turkish_CI_AI as varchar(max))", "Merhaba"},           // cp1254
			{"cast(cast(N'שלום' as nvarchar(max)) collate Hebrew_CI_AI as varchar(max))", "שלום"},                  // cp1255
			{"cast(cast(N'مرحبا' as nvarchar(max)) collate Arabic_CI_AI as varchar(max))", "مرحبا"},                // cp1256
			{"cast(cast(N'Sveiki' as nvarchar(max)) collate Lithuanian_CI_AI as varchar(max))", "Sveiki"},          // cp1257
			{"cast(cast(N'chào' as nvarchar(max)) collate Vietnamese_CI_AI as varchar(max))", "chào"},              // cp1258
			{fmt.Sprintf("cast(N'%s' as nvarchar(max))", longstr), longstr},
			{"cast(NULL as sql_variant)", nil},
			{"cast(cast(0x6F9619FF8B86D011B42D00C04FC964FF as uniqueidentifier) as sql_variant)",
				[]byte{0x6F, 0x96, 0x19, 0xFF, 0x8B, 0x86, 0xD0, 0x11, 0xB4, 0x2D, 0x00, 0xC0, 0x4F, 0xC9, 0x64, 0xFF}},
			{"cast(cast(1 as bit) as sql_variant)", true},
			{"cast(cast(10 as tinyint) as sql_variant)", int64(10)},
			{"cast(cast(-10 as smallint) as sql_variant)", int64(-10)},
			{"cast(cast(-20 as int) as sql_variant)", int64(-20)},
			{"cast(cast(-20 as bigint) as sql_variant)", int64(-20)},
			{"cast(cast('2000-01-01' as datetime) as sql_variant)", time.Date(2000, 1, 1, 0, 0, 0, 0, time.UTC)},
			{"cast(cast('2000-01-01T12:13:00' as smalldatetime) as sql_variant)",
				time.Date(2000, 1, 1, 12, 13, 0, 0, time.UTC)},
			{"cast(cast(0.125 as real) as sql_variant)", float64(0.125)},
			{"cast(cast(0.125 as float) as sql_variant)", float64(0.125)},
			{"cast(cast(1.2345 as smallmoney) as sql_variant)", []byte("1.2345")},
			{"cast(cast(1.2345 as money) as sql_variant)", []byte("1.2345")},
			{"cast(cast(0x1234 as varbinary(2)) as sql_variant)", []byte{0x12, 0x34}},
			{"cast(cast(0x1234 as binary(2)) as sql_variant)", []byte{0x12, 0x34}},
			{"cast(cast(-0.5 as decimal(18,1)) as sql_variant)", []byte("-0.5")},
			{"cast(cast(-0.5 as numeric(18,1)) as sql_variant)", []byte("-0.5")},
			{"cast(cast('abc' as varchar(3)) as sql_variant)", "abc"},
			{"cast(cast('abc' as char(3)) as sql_variant)", "abc"},
			{"cast(N'abc' as sql_variant)", "abc"},
		}

		for _, test := range values {
			t.Run(test.sql, func(t *testing.T) {
				stmt, err := conn.Prepare("select " + test.sql)
				if err != nil {
					t.Error("Prepare failed:", test.sql, err.Error())
					return
				}
				defer stmt.Close()

				row := stmt.QueryRow()
				var retval interface{}
				err = row.Scan(&retval)
				if err != nil {
					t.Error("Scan failed:", test.sql, err.Error())
					return
				}
				var same bool
				switch decodedval := retval.(type) {
				case []byte:
					switch decodedvaltest := test.val.(type) {
					case []byte:
						same = bytes.Equal(decodedval, decodedvaltest)
					default:
						same = false
					}
				default:
					same = retval == test.val
				}
				if !same {
					t.Errorf("Values don't match '%s' '%s' for test: %s", retval, test.val, test.sql)
					return
				}
			})
		}
	})
	t.Run("scan into *int64", func(t *testing.T) {
		t.Run("from integer", func(t *testing.T) {
			row := conn.QueryRow("select 11")
			var retval *int64
			err := row.Scan(&retval)
			if err != nil {
				t.Error("Scan failed", err.Error())
				return
			}
			if *retval != 11 {
				t.Errorf("Expected 11, got %v", retval)
			}
		})
		t.Run("from null", func(t *testing.T) {
			row := conn.QueryRow("select null")
			var retval *int64
			err := row.Scan(&retval)
			if err != nil {
				t.Error("Scan failed", err.Error())
				return
			}
			if retval != nil {
				t.Errorf("Expected nil, got %v", retval)
			}
		})
	})
}

func TestSelectDateTimeOffset(t *testing.T) {
	type testStruct struct {
		sql string
		val time.Time
	}
	values := []testStruct{
		{"cast('2010-11-15T11:56:45.123+14:00' as datetimeoffset(3))",
			time.Date(2010, 11, 15, 11, 56, 45, 123000000, time.FixedZone("", 14*60*60))},
		{"cast(cast('2010-11-15T11:56:45.123-14:00' as datetimeoffset(3)) as sql_variant)",
			time.Date(2010, 11, 15, 11, 56, 45, 123000000, time.FixedZone("", -14*60*60))},
		{"cast('0001-01-01T00:00:00.0000000+00:00' as datetimeoffset(7))",
			time.Date(1, 1, 1, 0, 0, 0, 0, time.FixedZone("", 0))},
		{"cast('9999-12-31T23:59:59.9999999+00:00' as datetimeoffset(7))",
			time.Date(9999, 12, 31, 23, 59, 59, 999999900, time.FixedZone("", 0))},
	}

	conn := open(t)
	defer conn.Close()
	for _, test := range values {
		row := conn.QueryRow("select " + test.sql)
		var retval interface{}
		err := row.Scan(&retval)
		if err != nil {
			t.Error("Scan failed:", test.sql, err.Error())
			continue
		}
		retvalDate := retval.(time.Time)
		if retvalDate.UTC() != test.val.UTC() {
			t.Errorf("UTC values don't match '%v' '%v' for test: %s", retvalDate, test.val, test.sql)
			continue
		}
		if retvalDate.String() != test.val.String() {
			t.Errorf("Locations don't match '%v' '%v' for test: %s", retvalDate.String(), test.val.String(), test.sql)
			continue
		}
	}
}

func TestSelectNewTypes(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	var ver string
	err := conn.QueryRow("select SERVERPROPERTY('productversion')").Scan(&ver)
	if err != nil {
		t.Fatalf("cannot select productversion: %s", err)
	}
	var n int
	_, err = fmt.Sscanf(ver, "%d", &n)
	if err != nil {
		t.Fatalf("cannot parse productversion: %s", err)
	}
	// 8 is SQL 2000, 9 is SQL 2005, 10 is SQL 2008, 11 is SQL 2012
	if n < 10 {
		return
	}
	// run tests for new data types available only in SQL Server 2008 and later
	type testStruct struct {
		sql string
		val interface{}
	}
	values := []testStruct{
		{"cast('0001-01-01' as date)",
			time.Date(1, 1, 1, 0, 0, 0, 0, time.UTC)},
		{"cast('2000-01-01' as date)",
			time.Date(2000, 1, 1, 0, 0, 0, 0, time.UTC)},
		{"cast('9999-12-31' as date)",
			time.Date(9999, 12, 31, 0, 0, 0, 0, time.UTC)},
		{"cast(NULL as date)", nil},
		{"cast('00:00:00.0000000' as time(7))",
			time.Date(1, 1, 1, 0, 0, 0, 0, time.UTC)},
		{"cast('00:00:45.123' as time(3))",
			time.Date(1, 1, 1, 00, 00, 45, 123000000, time.UTC)},
		{"cast('11:56:45.123' as time(3))",
			time.Date(1, 1, 1, 11, 56, 45, 123000000, time.UTC)},
		{"cast('11:56:45' as time(0))",
			time.Date(1, 1, 1, 11, 56, 45, 0, time.UTC)},
		{"cast('23:59:59.9999999' as time(7))",
			time.Date(1, 1, 1, 23, 59, 59, 999999900, time.UTC)},
		{"cast(null as time(0))", nil},
		{"cast('0001-01-01T00:00:00.0000000' as datetime2(7))",
			time.Date(1, 1, 1, 0, 0, 0, 0, time.UTC)},
		{"cast('2010-11-15T11:56:45.123' as datetime2(3))",
			time.Date(2010, 11, 15, 11, 56, 45, 123000000, time.UTC)},
		{"cast('2010-11-15T11:56:45' as datetime2(0))",
			time.Date(2010, 11, 15, 11, 56, 45, 0, time.UTC)},
		{"cast(cast('2000-01-01' as date) as sql_variant)",
			time.Date(2000, 1, 1, 0, 0, 0, 0, time.UTC)},
		{"cast(cast('00:00:45.123' as time(3)) as sql_variant)",
			time.Date(1, 1, 1, 00, 00, 45, 123000000, time.UTC)},
		{"cast(cast('2010-11-15T11:56:45.123' as datetime2(3)) as sql_variant)",
			time.Date(2010, 11, 15, 11, 56, 45, 123000000, time.UTC)},
		{"cast('9999-12-31T23:59:59.9999999' as datetime2(7))",
			time.Date(9999, 12, 31, 23, 59, 59, 999999900, time.UTC)},
		{"cast(null as datetime2(3))", nil},
	}
	for _, test := range values {
		stmt, err := conn.Prepare("select " + test.sql)
		if err != nil {
			t.Error("Prepare failed:", test.sql, err.Error())
			return
		}
		defer stmt.Close()

		row := stmt.QueryRow()
		var retval interface{}
		err = row.Scan(&retval)
		if err != nil {
			t.Error("Scan failed:", test.sql, err.Error())
			continue
		}
		if retval != test.val {
			t.Errorf("Values don't match '%s' '%s' for test: %s", retval, test.val, test.sql)
			continue
		}
	}
}

func TestTrans(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	var tx *sql.Tx
	var err error
	if tx, err = conn.Begin(); err != nil {
		t.Fatal("Begin failed", err.Error())
	}
	if err = tx.Commit(); err != nil {
		t.Fatal("Commit failed", err.Error())
	}

	if tx, err = conn.Begin(); err != nil {
		t.Fatal("Begin failed", err.Error())
	}
	if _, err = tx.Exec("create table #abc (fld int)"); err != nil {
		t.Fatal("Create table failed", err.Error())
	}
	if err = tx.Rollback(); err != nil {
		t.Fatal("Rollback failed", err.Error())
	}
}

func TestNull(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	t.Run("scan into interface{}", func(t *testing.T) {
		types := []string{
			"tinyint",
			"smallint",
			"int",
			"bigint",
			"real",
			"float",
			"smallmoney",
			"money",
			"decimal",
			//"varbinary(15)",
			//"binary(15)",
			"nvarchar(15)",
			"nchar(15)",
			"varchar(15)",
			"char(15)",
			"bit",
			"smalldatetime",
			"date",
			"time",
			"datetime",
			"datetime2",
			"datetimeoffset",
			"uniqueidentifier",
			"sql_variant",
		}
		for _, typ := range types {
			t.Run(typ, func(t *testing.T) {
				row := conn.QueryRow("declare @x "+typ+" = ?; select @x", nil)
				var retval interface{}
				err := row.Scan(&retval)
				if err != nil {
					t.Error("Scan failed for type "+typ, err.Error())
					return
				}
				if retval != nil {
					t.Error("Value should be nil, but it is ", retval)
					return
				}
			})
		}
	})

	t.Run("scan into NullInt64", func(t *testing.T) {
		types := []string{
			"tinyint",
			"smallint",
			"int",
			"bigint",
			"real",
			"float",
			"smallmoney",
			"money",
			"decimal",
			//"varbinary(15)",
			//"binary(15)",
			"nvarchar(15)",
			"nchar(15)",
			"varchar(15)",
			"char(15)",
			"bit",
			"smalldatetime",
			"date",
			"time",
			"datetime",
			"datetime2",
			"datetimeoffset",
			"uniqueidentifier",
			"sql_variant",
		}
		for _, typ := range types {
			row := conn.QueryRow("declare @x "+typ+" = ?; select @x", nil)
			var retval sql.NullInt64
			err := row.Scan(&retval)
			if err != nil {
				t.Error("Scan failed for type "+typ, err.Error())
				return
			}
			if retval.Valid {
				t.Error("Value should be nil, but it is ", retval)
				return
			}
		}
	})

	t.Run("scan into *int", func(t *testing.T) {
		types := []string{
			"tinyint",
			"smallint",
			"int",
			"bigint",
			"real",
			"float",
			"smallmoney",
			"money",
			"decimal",
			//"varbinary(15)",
			//"binary(15)",
			"nvarchar(15)",
			"nchar(15)",
			"varchar(15)",
			"char(15)",
			"bit",
			"smalldatetime",
			"date",
			"time",
			"datetime",
			"datetime2",
			"datetimeoffset",
			"uniqueidentifier",
			"sql_variant",
		}
		for _, typ := range types {
			row := conn.QueryRow("declare @x "+typ+" = ?; select @x", nil)
			var retval *int
			err := row.Scan(&retval)
			if err != nil {
				t.Error("Scan failed for type "+typ, err.Error())
				return
			}
			if retval != nil {
				t.Error("Value should be nil, but it is ", retval)
				return
			}
		}
	})
}

func TestParams(t *testing.T) {
	longstr := strings.Repeat("x", 10000)
	longbytes := make([]byte, 10000)
	testdate, err := time.Parse(time.RFC3339, "2010-01-01T00:00:00-00:00")
	if err != nil {
		t.Fatal(err)
	}
	values := []interface{}{
		int64(5),
		"hello",
		"",
		[]byte{1, 2, 3},
		[]byte{},
		float64(1.12313554),
		true,
		false,
		nil,
		longstr,
		longbytes,
		testdate.UTC(),
	}

	conn := open(t)
	defer conn.Close()

	for _, val := range values {
		t.Run(fmt.Sprintf("%T:%#v", val, val), func(t *testing.T) {
			row := conn.QueryRow("select ?", val)
			var retval interface{}
			err := row.Scan(&retval)
			if err != nil {
				t.Error("Scan failed", err.Error())
				return
			}
			var same bool
			switch decodedval := retval.(type) {
			case []byte:
				switch decodedvaltest := val.(type) {
				case []byte:
					same = bytes.Equal(decodedval, decodedvaltest)
				default:
					same = false
				}
			case time.Time:
				same = decodedval.UTC() == val
			default:
				same = retval == val
			}
			if !same {
				t.Error("Value don't match", retval, val)
				return
			}
		})
	}
}

func TestExec(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	res, err := conn.Exec("create table #abc (fld int)")
	if err != nil {
		t.Fatal("Exec failed", err.Error())
	}
	_ = res
}

func TestShortTimeout(t *testing.T) {
	if testing.Short() {
		t.Skip("short")
	}
	checkConnStr(t)
	SetLogger(testLogger{t})
	dsn := makeConnStr(t)
	dsnParams := dsn.Query()
	dsnParams.Set("Connection Timeout", "2")
	dsn.RawQuery = dsnParams.Encode()
	conn, err := sql.Open("mssql", dsn.String())
	if err != nil {
		t.Fatal("Open connection failed:", err.Error())
	}
	defer conn.Close()

	_, err = conn.Exec("waitfor delay '00:00:15'")
	if err == nil {
		t.Fatal("Exec should fail with timeout, but no failure occurred")
	}
	if neterr, ok := err.(net.Error); !ok || !neterr.Timeout() {
		t.Fatal("failure not a timeout, failed with", err)
	}

	// connection should be usable after timeout
	row := conn.QueryRow("select 1")
	var val int64
	err = row.Scan(&val)
	if err != nil {
		t.Fatal("Scan failed with", err)
	}
}

func TestTwoQueries(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	rows, err := conn.Query("select 1")
	if err != nil {
		t.Fatal("First exec failed", err)
	}
	if !rows.Next() {
		t.Fatal("First query didn't return row")
	}
	var i int
	if err = rows.Scan(&i); err != nil {
		t.Fatal("Scan failed", err)
	}
	if i != 1 {
		t.Fatalf("Wrong value returned %d, should be 1", i)
	}

	if rows, err = conn.Query("select 2"); err != nil {
		t.Fatal("Second query failed", err)
	}
	if !rows.Next() {
		t.Fatal("Second query didn't return row")
	}
	if err = rows.Scan(&i); err != nil {
		t.Fatal("Scan failed", err)
	}
	if i != 2 {
		t.Fatalf("Wrong value returned %d, should be 2", i)
	}
}

func TestError(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	_, err := conn.Query("exec bad")
	if err == nil {
		t.Fatal("Query should fail")
	}

	if sqlerr, ok := err.(Error); !ok {
		t.Fatalf("Should be sql error, actually %T, %v", err, err)
	} else {
		if sqlerr.Number != 2812 { // Could not find stored procedure 'bad'
			t.Fatalf("Should be specific error code 2812, actually %d %s", sqlerr.Number, sqlerr)
		}
	}
}

func TestQueryNoRows(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	var rows *sql.Rows
	var err error
	if rows, err = conn.Query("create table #abc (fld int)"); err != nil {
		t.Fatal("Query failed", err)
	}
	if rows.Next() {
		t.Fatal("Query shoulnd't return any rows")
	}
}

func TestQueryManyNullsRow(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	var row *sql.Row
	var err error
	if row = conn.QueryRow("select null, null, null, null, null, null, null, null"); err != nil {
		t.Fatal("Query failed", err)
	}
	var v [8]sql.NullInt64
	if err = row.Scan(&v[0], &v[1], &v[2], &v[3], &v[4], &v[5], &v[6], &v[7]); err != nil {
		t.Fatal("Scan failed", err)
	}
}

func TestOrderBy(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	tx, err := conn.Begin()
	if err != nil {
		t.Fatal("Begin tran failed", err)
	}
	defer tx.Rollback()

	_, err = tx.Exec("if (exists(select * from INFORMATION_SCHEMA.TABLES where TABLE_NAME='tbl')) drop table tbl")
	if err != nil {
		t.Fatal("Drop table failed", err)
	}

	_, err = tx.Exec("create table tbl (fld1 int primary key, fld2 int)")
	if err != nil {
		t.Fatal("Create table failed", err)
	}
	_, err = tx.Exec("insert into tbl (fld1, fld2) values (1, 2)")
	if err != nil {
		t.Fatal("Insert failed", err)
	}
	_, err = tx.Exec("insert into tbl (fld1, fld2) values (2, 1)")
	if err != nil {
		t.Fatal("Insert failed", err)
	}

	rows, err := tx.Query("select * from tbl order by fld1")
	if err != nil {
		t.Fatal("Query failed", err)
	}

	for rows.Next() {
		var fld1 int32
		var fld2 int32
		err = rows.Scan(&fld1, &fld2)
		if err != nil {
			t.Fatal("Scan failed", err)
		}
	}

	err = rows.Err()
	if err != nil {
		t.Fatal("Rows have errors", err)
	}
}

func TestScanDecimal(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	var f float64
	err := conn.QueryRow("select cast(0.5 as numeric(25,1))").Scan(&f)
	if err != nil {
		t.Error("query row / scan failed:", err.Error())
		return
	}
	if math.Abs(f-0.5) > 0.000001 {
		t.Error("Value is not 0.5:", f)
		return
	}

	var s string
	err = conn.QueryRow("select cast(-0.05 as numeric(25,2))").Scan(&s)
	if err != nil {
		t.Error("query row / scan failed:", err.Error())
		return
	}
	if s != "-0.05" {
		t.Error("Value is not -0.05:", s)
		return
	}
}

func TestAffectedRows(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	tx, err := conn.Begin()
	if err != nil {
		t.Fatal("Begin tran failed", err)
	}
	defer tx.Rollback()

	res, err := tx.Exec("create table #foo (bar int)")
	if err != nil {
		t.Fatal("create table failed")
	}
	n, err := res.RowsAffected()
	if err != nil {
		t.Fatal("rows affected failed")
	}
	if n != 0 {
		t.Error("Expected 0 rows affected, got ", n)
	}

	res, err = tx.Exec("insert into #foo (bar) values (1)")
	if err != nil {
		t.Fatal("insert failed")
	}
	n, err = res.RowsAffected()
	if err != nil {
		t.Fatal("rows affected failed")
	}
	if n != 1 {
		t.Error("Expected 1 row affected, got ", n)
	}

	res, err = tx.Exec("insert into #foo (bar) values (?)", 2)
	if err != nil {
		t.Fatal("insert failed", err)
	}
	n, err = res.RowsAffected()
	if err != nil {
		t.Fatal("rows affected failed")
	}
	if n != 1 {
		t.Error("Expected 1 row affected, got ", n)
	}
}

func TestIdentity(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	tx, err := conn.Begin()
	if err != nil {
		t.Fatal("Begin tran failed", err)
	}
	defer tx.Rollback()

	res, err := tx.Exec("create table #foo (bar int identity, baz int unique)")
	if err != nil {
		t.Fatal("create table failed")
	}

	res, err = tx.Exec("insert into #foo (baz) values (1)")
	if err != nil {
		t.Fatal("insert failed")
	}
	n, err := res.LastInsertId()
	if err != nil {
		t.Fatal("last insert id failed")
	}
	if n != 1 {
		t.Error("Expected 1 for identity, got ", n)
	}

	res, err = tx.Exec("insert into #foo (baz) values (20)")
	if err != nil {
		t.Fatal("insert failed")
	}
	n, err = res.LastInsertId()
	if err != nil {
		t.Fatal("last insert id failed")
	}
	if n != 2 {
		t.Error("Expected 2 for identity, got ", n)
	}

	res, err = tx.Exec("insert into #foo (baz) values (1)")
	if err == nil {
		t.Fatal("insert should fail")
	}

	res, err = tx.Exec("insert into #foo (baz) values (?)", 1)
	if err == nil {
		t.Fatal("insert should fail")
	}
}

func queryParamRoundTrip(db *sql.DB, param interface{}, dest interface{}) {
	err := db.QueryRow("select ?", param).Scan(dest)
	if err != nil {
		log.Panicf("select / scan failed: %v", err.Error())
	}
}

func TestDateTimeParam(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	type testStruct struct {
		t time.Time
	}
	var emptydate time.Time
	mindate := time.Date(1, 1, 1, 0, 0, 0, 0, time.UTC)
	maxdate := time.Date(9999, 12, 31, 23, 59, 59, 999999900, time.UTC)
	values := []testStruct{
		{time.Date(2015, time.October, 12, 10, 22, 0, 0, time.FixedZone("PST", -8*60*60))}, // back to the future day
		{time.Date(1961, time.April, 12, 9, 7, 0, 0, time.FixedZone("MSK", 3*60*60))},      // First man in space
		{time.Date(1969, time.July, 20, 20, 18, 0, 0, time.UTC)},                           // First man on the Moon
		{time.Date(1970, 1, 1, 0, 0, 0, 0, time.UTC)},                                      // UNIX date
		{time.Date(1982, 1, 3, 12, 13, 14, 300, time.FixedZone("YAKT", 9*60*60))},          // some random date
		{time.Date(4, 6, 3, 12, 13, 14, 150000000, time.UTC)},                              // some random date
		{mindate}, // minimal value
		{maxdate}, // maximum value
		{time.Date(10000, 1, 1, 0, 0, 0, 0, time.UTC)}, // just over limit
		{emptydate},
	}
	for _, test := range values {
		t.Run(fmt.Sprintf("Test for %v", test.t), func(t *testing.T) {
			var t2 time.Time
			queryParamRoundTrip(conn, test.t, &t2)
			expected := test.t
			// clip value
			if test.t.Before(mindate) {
				expected = mindate
			}
			if test.t.After(maxdate) {
				expected = maxdate
			}

			if expected.Sub(t2) != 0 {
				t.Errorf("expected: '%s', got: '%s' delta: %d", expected, t2, expected.Sub(t2))
			}
		})
	}

}

func TestUniqueIdentifierParam(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	type testStruct struct {
		name string
		uuid interface{}
	}

	expected := UniqueIdentifier{0x01, 0x23, 0x45, 0x67,
		0x89, 0xAB,
		0xCD, 0xEF,
		0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF,
	}

	values := []testStruct{
		{
			"[]byte",
			[]byte{0x67, 0x45, 0x23, 0x01,
				0xAB, 0x89,
				0xEF, 0xCD,
				0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF}},
		{
			"string",
			"01234567-89ab-cdef-0123-456789abcdef"},
	}

	for _, test := range values {
		t.Run(test.name, func(t *testing.T) {
			var uuid2 UniqueIdentifier
			err := conn.QueryRow("select ?", test.uuid).Scan(&uuid2)
			if err != nil {
				t.Fatal("select / scan failed", err.Error())
			}

			if expected != uuid2 {
				t.Errorf("uniqueidentifier does not match: '%s' '%s'", expected, uuid2)
			}
		})
	}
}

func TestBigQuery(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	rows, err := conn.Query(`WITH n(n) AS
		(
		    SELECT 1
		    UNION ALL
		    SELECT n+1 FROM n WHERE n < 10000
		)
		SELECT n, @@version FROM n ORDER BY n
		OPTION (MAXRECURSION 10000);`)
	if err != nil {
		t.Fatal("cannot exec query", err)
	}
	rows.Next()
	rows.Close()
	var res int
	err = conn.QueryRow("select 0").Scan(&res)
	if err != nil {
		t.Fatal("cannot scan value", err)
	}
	if res != 0 {
		t.Fatal("expected 0, got ", res)
	}
}

func TestBug32(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	tx, err := conn.Begin()
	if err != nil {
		t.Fatal("Begin tran failed", err)
	}
	defer tx.Rollback()

	_, err = tx.Exec("if (exists(select * from INFORMATION_SCHEMA.TABLES where TABLE_NAME='tbl')) drop table tbl")
	if err != nil {
		t.Fatal("Drop table failed", err)
	}

	_, err = tx.Exec("create table tbl(a int primary key,fld bit null)")
	if err != nil {
		t.Fatal("Create table failed", err)
	}

	_, err = tx.Exec("insert into tbl (a,fld) values (1,nullif(?, ''))", "")
	if err != nil {
		t.Fatal("Insert failed", err)
	}
}

func TestIgnoreEmptyResults(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	rows, err := conn.Query("set nocount on; select 2")
	if err != nil {
		t.Fatal("Query failed", err.Error())
	}
	if !rows.Next() {
		t.Fatal("Query didn't return row")
	}
	var fld1 int32
	err = rows.Scan(&fld1)
	if err != nil {
		t.Fatal("Scan failed", err)
	}
	if fld1 != 2 {
		t.Fatal("Returned value doesn't match")
	}
}

func TestStmt_SetQueryNotification(t *testing.T) {
	checkConnStr(t)
	mssqldriver := driverWithProcess(t)
	cn, err := mssqldriver.Open(makeConnStr(t).String())
	if err != nil {
		t.Fatalf("failed to open connection: %v", err)
	}
	stmt, err := cn.Prepare("SELECT 1")
	if err != nil {
		t.Error("Connection failed", err)
	}

	sqlstmt := stmt.(*Stmt)
	sqlstmt.SetQueryNotification("ABC", "service=WebCacheNotifications", time.Hour)

	rows, err := sqlstmt.Query(nil)
	if err == nil {
		rows.Close()
	}
	// notifications are sent to Service Broker
	// see for more info: https://github.com/denisenkom/go-mssqldb/pull/90
}

func TestErrorInfo(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	_, err := conn.Exec("select bad")
	if sqlError, ok := err.(Error); ok {
		if sqlError.SQLErrorNumber() != 207 /*invalid column name*/ {
			t.Errorf("Query failed with unexpected error number %d %s", sqlError.SQLErrorNumber(), sqlError.SQLErrorMessage())
		}
		if sqlError.SQLErrorLineNo() != 1 {
			t.Errorf("Unexpected line number returned %v, expected %v", sqlError.SQLErrorLineNo(), 1)
		}
	} else {
		t.Error("Failed to convert error to SQLErorr", err)
	}
	_, err = conn.Exec("RAISERROR('test message', 18, 111)")
	if sqlError, ok := err.(Error); ok {
		if sqlError.SQLErrorNumber() != 50000 {
			t.Errorf("Query failed with unexpected error number %d %s", sqlError.SQLErrorNumber(), sqlError.SQLErrorMessage())
		}
		if sqlError.SQLErrorMessage() != "test message" {
			t.Fail()
		}
		if sqlError.SQLErrorClass() != 18 {
			t.Fail()
		}
		if sqlError.SQLErrorState() != 111 {
			t.Fail()
		}
		if sqlError.SQLErrorLineNo() != 1 {
			t.Errorf("Unexpected line number returned %v, expected %v", sqlError.SQLErrorLineNo(), 1)
		}
		// just call those methods to make sure we have some coverage for them
		sqlError.SQLErrorServerName()
		sqlError.SQLErrorProcName()
	} else {
		t.Error("Failed to convert error to SQLErorr", err)
	}
}

func TestSetLanguage(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	_, err := conn.Exec("set language russian")
	if err != nil {
		t.Errorf("Query failed with unexpected error %s", err)
	}

	row := conn.QueryRow("select cast(getdate() as varchar(50))")
	var val interface{}
	err = row.Scan(&val)
	if err != nil {
		t.Errorf("Query failed with unexpected error %s", err)
	}
	t.Log("Returned value", val)
}

func TestConnectionClosing(t *testing.T) {
	pool := open(t)
	defer pool.Close()
	for i := 1; i <= 100; i++ {
		if pool.Stats().OpenConnections > 1 {
			t.Errorf("Open connections is expected to stay <= 1, but it is %d", pool.Stats().OpenConnections)
			return
		}

		stmt, err := pool.Query("select 1")
		if err != nil {
			t.Fatalf("Query failed with unexpected error %s", err)
		}
		for stmt.Next() {
			var val interface{}
			err := stmt.Scan(&val)
			if err != nil {
				t.Fatalf("Query failed with unexpected error %s", err)
			}
		}
	}
}

func TestBeginTranError(t *testing.T) {
	checkConnStr(t)
	drv := driverWithProcess(t)
	conn, err := drv.open(context.Background(), makeConnStr(t).String())
	if err != nil {
		t.Fatalf("Open failed with error %v", err)
	}

	defer conn.Close()
	// close actual connection to make begin transaction to fail during sending of a packet
	conn.sess.buf.transport.Close()

	ctx := context.Background()
	_, err = conn.begin(ctx, isolationSnapshot)
	if err == nil || conn.connectionGood == true {
		t.Errorf("begin should fail as a bad connection, err=%v", err)
	}

	// reopen connection
	conn, err = drv.open(context.Background(), makeConnStr(t).String())
	if err != nil {
		t.Fatalf("Open failed with error %v", err)
	}
	err = conn.sendBeginRequest(ctx, isolationSerializable)
	if err != nil {
		t.Fatalf("sendBeginRequest failed with error %v", err)
	}

	// close connection to cause processBeginResponse to fail
	conn.sess.buf.transport.Close()
	_, err = conn.processBeginResponse(ctx)
	switch err {
	case nil:
		t.Error("processBeginResponse should fail but it succeeded")
	case driver.ErrBadConn:
		t.Error("processBeginResponse should fail with error different from ErrBadConn but it did")
	}

	if conn.connectionGood {
		t.Fatal("Connection should be in a bad state")
	}
}

func TestCommitTranError(t *testing.T) {
	checkConnStr(t)
	drv := driverWithProcess(t)
	conn, err := drv.open(context.Background(), makeConnStr(t).String())
	if err != nil {
		t.Fatalf("Open failed with error %v", err)
	}

	defer conn.Close()
	// close actual connection to make commit transaction to fail during sending of a packet
	conn.sess.buf.transport.Close()

	ctx := context.Background()
	err = conn.Commit()
	if err == nil || conn.connectionGood {
		t.Errorf("begin should fail and set the connection to bad, but it returned %v", err)
	}

	// reopen connection
	conn, err = drv.open(context.Background(), makeConnStr(t).String())
	if err != nil {
		t.Fatalf("Open failed with error %v", err)
	}
	err = conn.sendCommitRequest()
	if err != nil {
		t.Fatalf("sendCommitRequest failed with error %v", err)
	}

	// close connection to cause processBeginResponse to fail
	conn.sess.buf.transport.Close()
	err = conn.simpleProcessResp(ctx)
	switch err {
	case nil:
		t.Error("simpleProcessResp should fail but it succeeded")
	case driver.ErrBadConn:
		t.Error("simpleProcessResp should fail with error different from ErrBadConn but it did")
	}

	if conn.connectionGood {
		t.Fatal("Connection should be in a bad state")
	}

	// reopen connection
	conn, err = drv.open(context.Background(), makeConnStr(t).String())
	defer conn.Close()
	if err != nil {
		t.Fatalf("Open failed with error %v", err)
	}
	// should fail because there is no transaction
	err = conn.Commit()
	switch err {
	case nil:
		t.Error("Commit should fail but it succeeded")
	case driver.ErrBadConn:
		t.Error("Commit should fail with error different from ErrBadConn but it did")
	}
}

func TestRollbackTranError(t *testing.T) {
	checkConnStr(t)
	drv := driverWithProcess(t)
	conn, err := drv.open(context.Background(), makeConnStr(t).String())
	if err != nil {
		t.Fatalf("Open failed with error %v", err)
	}

	defer conn.Close()
	// close actual connection to make commit transaction to fail during sending of a packet
	conn.sess.buf.transport.Close()

	ctx := context.Background()
	err = conn.Rollback()
	if err == nil || conn.connectionGood {
		t.Errorf("Rollback should fail and set connection to bad but it returned %v", err)
	}

	// reopen connection
	conn, err = drv.open(context.Background(), makeConnStr(t).String())
	if err != nil {
		t.Fatalf("Open failed with error %v", err)
	}
	err = conn.sendRollbackRequest()
	if err != nil {
		t.Fatalf("sendCommitRequest failed with error %v", err)
	}

	// close connection to cause processBeginResponse to fail
	conn.sess.buf.transport.Close()
	err = conn.simpleProcessResp(ctx)
	switch err {
	case nil:
		t.Error("simpleProcessResp should fail but it succeeded")
	case driver.ErrBadConn:
		t.Error("simpleProcessResp should fail with error different from ErrBadConn but it did")
	}

	if conn.connectionGood {
		t.Fatal("Connection should be in a bad state")
	}

	// reopen connection
	conn, err = drv.open(context.Background(), makeConnStr(t).String())
	defer conn.Close()
	if err != nil {
		t.Fatalf("Open failed with error %v", err)
	}
	// should fail because there is no transaction
	err = conn.Rollback()
	switch err {
	case nil:
		t.Error("Commit should fail but it succeeded")
	case driver.ErrBadConn:
		t.Error("Commit should fail with error different from ErrBadConn but it did")
	}
}

func TestSendQueryErrors(t *testing.T) {
	checkConnStr(t)
	drv := driverWithProcess(t)
	conn, err := drv.open(context.Background(), makeConnStr(t).String())
	if err != nil {
		t.FailNow()
	}

	defer conn.Close()
	stmt, err := conn.prepareContext(context.Background(), "select 1")
	if err != nil {
		t.FailNow()
	}

	// should fail because parameter is invalid
	_, err = stmt.Query([]driver.Value{conn})
	if err == nil {
		t.Fail()
	}

	// close actual connection to make commit transaction to fail during sending of a packet
	conn.sess.buf.transport.Close()

	// should fail because connection is closed
	_, err = stmt.Query([]driver.Value{})
	if err == nil || stmt.c.connectionGood {
		t.Fail()
	}

	stmt, err = conn.prepareContext(context.Background(), "select ?")
	if err != nil {
		t.FailNow()
	}
	// should fail because connection is closed
	_, err = stmt.Query([]driver.Value{int64(1)})
	if err == nil || stmt.c.connectionGood {
		t.Fail()
	}
}

func TestProcessQueryErrors(t *testing.T) {
	checkConnStr(t)
	drv := driverWithProcess(t)
	conn, err := drv.open(context.Background(), makeConnStr(t).String())
	if err != nil {
		t.Fatal("open expected to succeed, but it failed with", err)
	}
	stmt, err := conn.prepareContext(context.Background(), "select 1")
	if err != nil {
		t.Fatal("prepareContext expected to succeed, but it failed with", err)
	}
	err = stmt.sendQuery([]namedValue{})
	if err != nil {
		t.Fatal("sendQuery expected to succeed, but it failed with", err)
	}
	// close actual connection to make reading response to fail
	conn.sess.buf.transport.Close()
	_, err = stmt.processQueryResponse(context.Background())
	if err == nil {
		t.Error("processQueryResponse expected to fail but it succeeded")
	}
	// should not fail with ErrBadConn because query was successfully sent to server
	if err == driver.ErrBadConn {
		t.Error("processQueryResponse expected to fail with error other than ErrBadConn but it failed with it")
	}

	if conn.connectionGood {
		t.Fatal("Connection should be in a bad state")
	}
}

func TestSendExecErrors(t *testing.T) {
	checkConnStr(t)
	drv := driverWithProcess(t)
	conn, err := drv.open(context.Background(), makeConnStr(t).String())
	if err != nil {
		t.FailNow()
	}

	defer conn.Close()
	stmt, err := conn.prepareContext(context.Background(), "select 1")
	if err != nil {
		t.FailNow()
	}

	// should fail because parameter is invalid
	_, err = stmt.Exec([]driver.Value{conn})
	if err == nil {
		t.Fail()
	}

	// close actual connection to make commit transaction to fail during sending of a packet
	conn.sess.buf.transport.Close()

	// should fail because connection is closed
	_, err = stmt.Exec([]driver.Value{})
	if err == nil || stmt.c.connectionGood {
		t.Fail()
	}

	stmt, err = conn.prepareContext(context.Background(), "select ?")
	if err != nil {
		t.FailNow()
	}
	// should fail because connection is closed
	_, err = stmt.Exec([]driver.Value{int64(1)})
	if err == nil || stmt.c.connectionGood {
		t.Fail()
	}
}

func TestLongConnection(t *testing.T) {
	checkConnStr(t)

	list := []struct {
		connTimeout  string
		queryTimeout string
		ctxTimeout   time.Duration
		wantFail     bool
	}{
		{"1", "00:00:02", 6 * time.Second, true},
		{"2", "00:00:01", 6 * time.Second, false},

		// Check no connection timeout.
		{"0", "00:00:01", 2 * time.Second, false},
		// {"0", "00:00:45", 60 * time.Second, false}, // Skip for normal testing to limit time.
	}

	for i, item := range list {
		t.Run(fmt.Sprintf("item-index-%d,want-fail=%t", i, item.wantFail), func(t *testing.T) {
			dsn := makeConnStr(t)
			dsnParams := dsn.Query()
			dsnParams.Set("connection timeout", item.connTimeout)
			dsn.RawQuery = dsnParams.Encode()

			db, err := sql.Open("sqlserver", dsn.String())
			if err != nil {
				t.Fatalf("failed to open driver sqlserver")
			}
			defer db.Close()

			ctx, cancel := context.WithTimeout(context.Background(), item.ctxTimeout)
			defer cancel()

			_, err = db.ExecContext(ctx, "WAITFOR DELAY '"+item.queryTimeout+"';")
			if item.wantFail && err == nil {
				t.Fatal("exec no error")
			}
			if !item.wantFail && err != nil {
				t.Fatal("exec error", err)
			}
		})
	}
}

func TestNextResultSet(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	rows, err := conn.Query("select 1; select 2")
	if err != nil {
		t.Fatal("Query failed", err.Error())
	}
	defer func() {
		err := rows.Err()
		if err != nil {
			t.Error("unexpected error:", err)
		}
	}()

	defer rows.Close()

	if !rows.Next() {
		t.Fatal("Query didn't return row")
	}
	var fld1, fld2 int32
	err = rows.Scan(&fld1)
	if err != nil {
		t.Fatal("Scan failed", err)
	}
	if fld1 != 1 {
		t.Fatal("Returned value doesn't match")
	}
	if rows.Next() {
		t.Fatal("Query returned unexpected second row.")
	}
	// calling next again should still return false
	if rows.Next() {
		t.Fatal("Query returned unexpected second row.")
	}
	if !rows.NextResultSet() {
		t.Fatal("NextResultSet should return true but returned false")
	}
	if !rows.Next() {
		t.Fatal("Query didn't return row")
	}
	err = rows.Scan(&fld2)
	if err != nil {
		t.Fatal("Scan failed", err)
	}
	if fld2 != 2 {
		t.Fatal("Returned value doesn't match")
	}
	if rows.NextResultSet() {
		t.Fatal("NextResultSet should return false but returned true")
	}
}

func TestColumnTypeIntrospection(t *testing.T) {
	type tst struct {
		expr         string
		typeName     string
		reflType     reflect.Type
		hasSize      bool
		size         int64
		hasPrecScale bool
		precision    int64
		scale        int64
	}
	tests := []tst{
		{"cast(1 as bit)", "BIT", reflect.TypeOf(true), false, 0, false, 0, 0},
		{"cast(1 as tinyint)", "TINYINT", reflect.TypeOf(int64(0)), false, 0, false, 0, 0},
		{"cast(1 as smallint)", "SMALLINT", reflect.TypeOf(int64(0)), false, 0, false, 0, 0},
		{"1", "INT", reflect.TypeOf(int64(0)), false, 0, false, 0, 0},
		{"cast(1 as bigint)", "BIGINT", reflect.TypeOf(int64(0)), false, 0, false, 0, 0},
		{"cast(1 as real)", "REAL", reflect.TypeOf(0.0), false, 0, false, 0, 0},
		{"cast(1 as float)", "FLOAT", reflect.TypeOf(0.0), false, 0, false, 0, 0},
		{"cast('abc' as varbinary(3))", "VARBINARY", reflect.TypeOf([]byte{}), true, 3, false, 0, 0},
		{"cast('abc' as varbinary(max))", "VARBINARY", reflect.TypeOf([]byte{}), true, 2147483645, false, 0, 0},
		{"cast(1 as datetime)", "DATETIME", reflect.TypeOf(time.Time{}), false, 0, false, 0, 0},
		{"cast(1 as smalldatetime)", "SMALLDATETIME", reflect.TypeOf(time.Time{}), false, 0, false, 0, 0},
		{"cast(getdate() as datetime2(7))", "DATETIME2", reflect.TypeOf(time.Time{}), false, 0, false, 0, 0},
		{"cast(getdate() as datetimeoffset(7))", "DATETIMEOFFSET", reflect.TypeOf(time.Time{}), false, 0, false, 0, 0},
		{"cast(getdate() as date)", "DATE", reflect.TypeOf(time.Time{}), false, 0, false, 0, 0},
		{"cast(getdate() as time)", "TIME", reflect.TypeOf(time.Time{}), false, 0, false, 0, 0},
		{"'abc'", "VARCHAR", reflect.TypeOf(""), true, 3, false, 0, 0},
		{"cast('abc' as varchar(max))", "VARCHAR", reflect.TypeOf(""), true, 2147483645, false, 0, 0},
		{"N'abc'", "NVARCHAR", reflect.TypeOf(""), true, 3, false, 0, 0},
		{"cast(N'abc' as NVARCHAR(MAX))", "NVARCHAR", reflect.TypeOf(""), true, 1073741822, false, 0, 0},
		{"cast(1 as decimal)", "DECIMAL", reflect.TypeOf([]byte{}), false, 0, true, 18, 0},
		{"cast(1 as decimal(5, 2))", "DECIMAL", reflect.TypeOf([]byte{}), false, 0, true, 5, 2},
		{"cast(1 as numeric(10, 4))", "DECIMAL", reflect.TypeOf([]byte{}), false, 0, true, 10, 4},
		{"cast(1 as money)", "MONEY", reflect.TypeOf([]byte{}), false, 0, false, 0, 0},
		{"cast(1 as smallmoney)", "SMALLMONEY", reflect.TypeOf([]byte{}), false, 0, false, 0, 0},
		{"cast(0x6F9619FF8B86D011B42D00C04FC964FF as uniqueidentifier)", "UNIQUEIDENTIFIER", reflect.TypeOf([]byte{}), false, 0, false, 0, 0},
		{"cast('<root/>' as xml)", "XML", reflect.TypeOf(""), true, 1073741822, false, 0, 0},
		{"cast('abc' as text)", "TEXT", reflect.TypeOf(""), true, 2147483647, false, 0, 0},
		{"cast(N'abc' as ntext)", "NTEXT", reflect.TypeOf(""), true, 1073741823, false, 0, 0},
		{"cast('abc' as image)", "IMAGE", reflect.TypeOf([]byte{}), true, 2147483647, false, 0, 0},
		{"cast('abc' as char(3))", "CHAR", reflect.TypeOf(""), true, 3, false, 0, 0},
		{"cast(N'abc' as nchar(3))", "NCHAR", reflect.TypeOf(""), true, 3, false, 0, 0},
		{"cast(1 as sql_variant)", "SQL_VARIANT", reflect.TypeOf(nil), false, 0, false, 0, 0},
	}
	conn := open(t)
	defer conn.Close()
	for _, tt := range tests {
		rows, err := conn.Query("select " + tt.expr)
		if err != nil {
			t.Fatalf("Query failed with unexpected error %s", err)
		}
		ct, err := rows.ColumnTypes()
		if err != nil {
			t.Fatalf("Query failed with unexpected error %s", err)
		}
		if ct[0].DatabaseTypeName() != tt.typeName {
			t.Errorf("Expected type %s but returned %s", tt.typeName, ct[0].DatabaseTypeName())
		}
		size, ok := ct[0].Length()
		if ok != tt.hasSize {
			t.Errorf("Expected has size %v but returned %v for %s", tt.hasSize, ok, tt.expr)
		} else {
			if ok && size != tt.size {
				t.Errorf("Expected size %d but returned %d for %s", tt.size, size, tt.expr)
			}
		}

		prec, scale, ok := ct[0].DecimalSize()
		if ok != tt.hasPrecScale {
			t.Errorf("Expected has prec/scale %v but returned %v for %s", tt.hasPrecScale, ok, tt.expr)
		} else {
			if ok && prec != tt.precision {
				t.Errorf("Expected precision %d but returned %d for %s", tt.precision, prec, tt.expr)
			}
			if ok && scale != tt.scale {
				t.Errorf("Expected scale %d but returned %d for %s", tt.scale, scale, tt.expr)
			}
		}

		if ct[0].ScanType() != tt.reflType {
			t.Errorf("Expected ScanType %v but got %v for %s", tt.reflType, ct[0].ScanType(), tt.expr)
		}
	}
}

func TestColumnIntrospection(t *testing.T) {
	type tst struct {
		expr         string
		fieldName    string
		typeName     string
		nullable     bool
		hasSize      bool
		size         int64
		hasPrecScale bool
		precision    int64
		scale        int64
	}
	tests := []tst{
		{"f1 int null", "f1", "INT", true, false, 0, false, 0, 0},
		{"f2 varchar(15) not null", "f2", "VARCHAR", false, true, 15, false, 0, 0},
		{"f3 decimal(5, 2) null", "f3", "DECIMAL", true, false, 0, true, 5, 2},
	}
	conn := open(t)
	defer conn.Close()

	// making table variable with specified fields and making a select from it
	exprs := make([]string, len(tests))
	for i, test := range tests {
		exprs[i] = test.expr
	}
	exprJoined := strings.Join(exprs, ",")
	rows, err := conn.Query(fmt.Sprintf("declare @tbl table(%s); select * from @tbl", exprJoined))
	if err != nil {
		t.Fatalf("Query failed with unexpected error %s", err)
	}

	ct, err := rows.ColumnTypes()
	if err != nil {
		t.Fatalf("ColumnTypes failed with unexpected error %s", err)
	}
	for i, test := range tests {
		if ct[i].Name() != test.fieldName {
			t.Errorf("Field expected have name %s but it has name %s", test.fieldName, ct[i].Name())
		}

		if ct[i].DatabaseTypeName() != test.typeName {
			t.Errorf("Invalid type name returned %s expected %s", ct[i].DatabaseTypeName(), test.typeName)
		}

		nullable, ok := ct[i].Nullable()
		if ok {
			if nullable != test.nullable {
				t.Errorf("Invalid nullable value returned %v", nullable)
			}
		} else {
			t.Error("Nullable was expected to support Nullable but it didn't")
		}

		size, ok := ct[i].Length()
		if ok != test.hasSize {
			t.Errorf("Expected has size %v but returned %v for %s", test.hasSize, ok, test.expr)
		} else {
			if ok && size != test.size {
				t.Errorf("Expected size %d but returned %d for %s", test.size, size, test.expr)
			}
		}

		prec, scale, ok := ct[i].DecimalSize()
		if ok != test.hasPrecScale {
			t.Errorf("Expected has prec/scale %v but returned %v for %s", test.hasPrecScale, ok, test.expr)
		} else {
			if ok && prec != test.precision {
				t.Errorf("Expected precision %d but returned %d for %s", test.precision, prec, test.expr)
			}
			if ok && scale != test.scale {
				t.Errorf("Expected scale %d but returned %d for %s", test.scale, scale, test.expr)
			}
		}
	}
}

func TestContext(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	opts := &sql.TxOptions{
		Isolation: sql.LevelSerializable,
	}
	ctx := context.Background()
	tx, err := conn.BeginTx(ctx, opts)
	if err != nil {
		t.Errorf("BeginTx failed with unexpected error %s", err)
		return
	}
	rows, err := tx.QueryContext(ctx, "DBCC USEROPTIONS")
	properties := make(map[string]string)
	for rows.Next() {
		var name, value string
		if err = rows.Scan(&name, &value); err != nil {
			t.Errorf("Scan failed with unexpected error %s", err)
		}
		properties[name] = value
	}

	if properties["isolation level"] != "serializable" {
		t.Errorf("Expected isolation level to be serializable but it is %s", properties["isolation level"])
	}

	row := tx.QueryRowContext(ctx, "select 1")
	var val int64
	if err = row.Scan(&val); err != nil {
		t.Errorf("QueryRowContext failed with unexpected error %s", err)
	}
	if val != 1 {
		t.Error("Incorrect value returned from query")
	}

	_, err = tx.ExecContext(ctx, "select 1")
	if err != nil {
		t.Errorf("ExecContext failed with unexpected error %s", err)
		return
	}

	_, err = tx.PrepareContext(ctx, "select 1")
	if err != nil {
		t.Errorf("PrepareContext failed with unexpected error %s", err)
		return
	}
}

func TestBeginTxtReadOnlyNotSupported(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	opts := &sql.TxOptions{ReadOnly: true}
	_, err := conn.BeginTx(context.Background(), opts)
	if err == nil {
		t.Error("BeginTx expected to fail for read only transaction because MSSQL doesn't support it, but it succeeded")
	}
}

func TestConn_BeginTx(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	_, err := conn.Exec("create table test (f int)")
	defer conn.Exec("drop table test")
	if err != nil {
		t.Fatal("create table failed with error", err)
	}

	tx1, err := conn.BeginTx(context.Background(), nil)
	if err != nil {
		t.Fatal("BeginTx failed with error", err)
	}
	tx2, err := conn.BeginTx(context.Background(), nil)
	if err != nil {
		t.Fatal("BeginTx failed with error", err)
	}
	_, err = tx1.Exec("insert into test (f) values (1)")
	if err != nil {
		t.Fatal("insert failed with error", err)
	}
	_, err = tx2.Exec("insert into test (f) values (2)")
	if err != nil {
		t.Fatal("insert failed with error", err)
	}
	tx1.Rollback()
	tx2.Commit()

	rows, err := conn.Query("select f from test")
	if err != nil {
		t.Fatal("select failed with error", err)
	}
	values := []int64{}
	for rows.Next() {
		var val int64
		err = rows.Scan(&val)
		if err != nil {
			t.Fatal("scan failed with error", err)
		}
		values = append(values, val)
	}
	if !reflect.DeepEqual(values, []int64{2}) {
		t.Errorf("Values is expected to be [1] but it is %v", values)
	}
}

func TestNamedParameters(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	row := conn.QueryRow(
		"select :param2, :param1, :param2",
		sql.Named("param1", 1),
		sql.Named("param2", 2))
	var col1, col2, col3 int64
	err := row.Scan(&col1, &col2, &col3)
	if err != nil {
		t.Errorf("Scan failed with unexpected error %s", err)
		return
	}
	if col1 != 2 || col2 != 1 || col3 != 2 {
		t.Errorf("Unexpected values returned col1=%d, col2=%d, col3=%d", col1, col2, col3)
	}
}

func TestBadNamedParameters(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	row := conn.QueryRow(
		"select :param2, :param1, :param2",
		sql.Named("badparam1", 1),
		sql.Named("param2", 2))
	var col1, col2, col3 int64
	err := row.Scan(&col1, &col2, &col3)
	if err == nil {
		t.Error("Scan succeeded unexpectedly")
		return
	}
	t.Logf("Scan failed as expected with error %s", err)
}

func TestMixedParameters(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	row := conn.QueryRow(
		"select :2, :param1, :param2",
		5, // this parameter will be unused
		6,
		sql.Named("param1", 1),
		sql.Named("param2", 2))
	var col1, col2, col3 int64
	err := row.Scan(&col1, &col2, &col3)
	if err != nil {
		t.Errorf("Scan failed with unexpected error %s", err)
		return
	}
	if col1 != 6 || col2 != 1 || col3 != 2 {
		t.Errorf("Unexpected values returned col1=%d, col2=%d, col3=%d", col1, col2, col3)
	}
}

/*
func TestMixedParametersExample(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	row := conn.QueryRow(
		"select :id, ?",
		sql.Named("id", 1),
		2,
		)
	var col1, col2 int64
	err := row.Scan(&col1, &col2)
	if err != nil {
		t.Errorf("Scan failed with unexpected error %s", err)
		return
	}
	if col1 != 1 || col2 != 2 {
		t.Errorf("Unexpected values returned col1=%d, col2=%d", col1, col2)
	}
}
*/

func TestPinger(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	err := conn.Ping()
	if err != nil {
		t.Errorf("Failed to hit database")
	}
}

func TestQueryCancelLowLevel(t *testing.T) {
	checkConnStr(t)
	drv := driverWithProcess(t)
	conn, err := drv.open(context.Background(), makeConnStr(t).String())
	if err != nil {
		t.Fatalf("Open failed with error %v", err)
	}

	defer conn.Close()
	ctx, cancel := context.WithCancel(context.Background())
	stmt, err := conn.prepareContext(ctx, "waitfor delay '00:00:03'")
	if err != nil {
		t.Fatalf("Prepare failed with error %v", err)
	}
	err = stmt.sendQuery([]namedValue{})
	if err != nil {
		t.Fatalf("sendQuery failed with error %v", err)
	}

	cancel()

	_, err = stmt.processExec(ctx)
	if err != context.Canceled {
		t.Errorf("Expected error to be Cancelled but got %v", err)
	}

	// same connection should be usable again after it was cancelled
	stmt, err = conn.prepareContext(context.Background(), "select 1")
	if err != nil {
		t.Fatalf("Prepare failed with error %v", err)
	}
	rows, err := stmt.Query([]driver.Value{})
	if err != nil {
		t.Fatalf("Query failed with error %v", err)
	}

	values := []driver.Value{nil}
	err = rows.Next(values)
	if err != nil {
		t.Fatalf("Next failed with error %v", err)
	}
}

func TestQueryCancelHighLevel(t *testing.T) {
	conn := open(t)
	defer conn.Close()

	ctx, cancel := context.WithCancel(context.Background())
	go func() {
		time.Sleep(200 * time.Millisecond)
		cancel()
	}()
	_, err := conn.ExecContext(ctx, "waitfor delay '00:00:03'")
	if err != context.Canceled {
		t.Errorf("ExecContext expected to fail with Cancelled but it returned %v", err)
	}

	// connection should be usable after timeout
	row := conn.QueryRow("select 1")
	var val int64
	err = row.Scan(&val)
	if err != nil {
		t.Fatal("Scan failed with", err)
	}
}

func TestQueryTimeout(t *testing.T) {
	conn := open(t)
	defer conn.Close()
	ctx, cancel := context.WithTimeout(context.Background(), 200*time.Millisecond)
	defer cancel()
	_, err := conn.ExecContext(ctx, "waitfor delay '00:00:03'")
	if err != context.DeadlineExceeded {
		t.Errorf("ExecContext expected to fail with DeadlineExceeded but it returned %v", err)
	}

	// connection should be usable after timeout
	row := conn.QueryRow("select 1")
	var val int64
	err = row.Scan(&val)
	if err != nil {
		t.Fatal("Scan failed with", err)
	}
}

func TestDriverParams(t *testing.T) {
	checkConnStr(t)
	SetLogger(testLogger{t})
	type sqlCmd struct {
		Name   string
		Driver string
		Query  string
		Param  []interface{}
		Expect []interface{}
	}

	list := []sqlCmd{
		{
			Name:   "preprocess-ordinal",
			Driver: "mssql",
			Query:  `select V1=:1`,
			Param:  []interface{}{"abc"},
			Expect: []interface{}{"abc"},
		},
		{
			Name:   "preprocess-name",
			Driver: "mssql",
			Query:  `select V1=:First`,
			Param:  []interface{}{sql.Named("First", "abc")},
			Expect: []interface{}{"abc"},
		},
		{
			Name:   "raw-ordinal",
			Driver: "sqlserver",
			Query:  `select V1=@p1`,
			Param:  []interface{}{"abc"},
			Expect: []interface{}{"abc"},
		},
		{
			Name:   "raw-name",
			Driver: "sqlserver",
			Query:  `select V1=@First`,
			Param:  []interface{}{sql.Named("First", "abc")},
			Expect: []interface{}{"abc"},
		},
	}

	for cmdIndex, cmd := range list {
		t.Run(cmd.Name, func(t *testing.T) {
			db, err := sql.Open(cmd.Driver, makeConnStr(t).String())
			if err != nil {
				t.Fatalf("failed to open driver %q", cmd.Driver)
			}
			defer db.Close()

			rows, err := db.Query(cmd.Query, cmd.Param...)
			if err != nil {
				t.Fatalf("failed to run query %q %v", cmd.Query, err)
			}
			defer rows.Close()

			columns, err := rows.Columns()
			if err != nil {
				t.Fatalf("failed to get column schema %v", err)
			}
			clen := len(columns)

			if clen != len(cmd.Expect) {
				t.Fatalf("query column has %d, expect %d columns", clen, len(cmd.Expect))
			}

			values := make([]interface{}, clen)
			into := make([]interface{}, clen)
			for i := 0; i < clen; i++ {
				into[i] = &values[i]
			}
			for rows.Next() {
				err = rows.Scan(into...)
				if err != nil {
					t.Fatalf("failed to scan into row for %d %q", cmdIndex, cmd.Driver)
				}
				for i := range cmd.Expect {
					if values[i] != cmd.Expect[i] {
						t.Fatalf("expected value in index %d %v != actual value %v", i, cmd.Expect[i], values[i])
					}
				}
			}
		})
	}
}

type connInterrupt struct {
	net.Conn

	mu           sync.Mutex
	disruptRead  bool
	disruptWrite bool
}

func (c *connInterrupt) Interrupt(write bool) {
	c.mu.Lock()
	if write {
		c.disruptWrite = true
	} else {
		c.disruptRead = true
	}
	c.mu.Unlock()
}

func (c *connInterrupt) Read(b []byte) (n int, err error) {
	c.mu.Lock()
	dis := c.disruptRead
	c.mu.Unlock()
	if dis {
		return 0, disconnectError{}
	}
	return c.Conn.Read(b)
}

func (c *connInterrupt) Write(b []byte) (n int, err error) {
	c.mu.Lock()
	dis := c.disruptWrite
	c.mu.Unlock()
	if dis {
		return 0, disconnectError{}
	}
	return c.Conn.Write(b)
}

type dialerInterrupt struct {
	nd tcpDialer

	mu   sync.Mutex
	list []*connInterrupt
}

func (d *dialerInterrupt) Dial(ctx context.Context, addr string) (net.Conn, error) {
	conn, err := d.nd.Dial(ctx, addr)
	if err != nil {
		return nil, err
	}
	ci := &connInterrupt{Conn: conn}
	d.mu.Lock()
	d.list = append(d.list, ci)
	d.mu.Unlock()
	return ci, err
}

func (d *dialerInterrupt) Interrupt(write bool) {
	d.mu.Lock()
	defer d.mu.Unlock()

	for _, ci := range d.list {
		ci.Interrupt(write)
	}
}

var _ net.Error = disconnectError{}

type disconnectError struct{}

func (disconnectError) Error() string {
	return "disconnect"
}

func (disconnectError) Timeout() bool {
	return true
}

func (disconnectError) Temporary() bool {
	return true
}

// TestDisconnect1 ensures errors and states are handled correctly if
// the server is disconnected mid-query.
func TestDisconnect1(t *testing.T) {
	if testing.Short() {
		t.Skip("short")
	}
	checkConnStr(t)
	SetLogger(testLogger{t})

	// Revert to the normal dialer after the test is done.
	normalCreateDialer := createDialer
	defer func() {
		createDialer = normalCreateDialer
	}()

	waitDisrupt := make(chan struct{})
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*2)
	defer cancel()

	createDialer = func(p *connectParams) dialer {
		nd := tcpDialer{&net.Dialer{Timeout: p.dial_timeout, KeepAlive: p.keepAlive}}
		di := &dialerInterrupt{nd: nd}
		go func() {
			<-waitDisrupt
			di.Interrupt(true)
			di.Interrupt(false)
		}()
		return di
	}
	db, err := sql.Open("sqlserver", makeConnStr(t).String())
	if err != nil {
		t.Fatal(err)
	}

	if err := db.PingContext(ctx); err != nil {
		t.Fatal(err)
	}
	defer db.Close()

	_, err = db.ExecContext(ctx, `SET LOCK_TIMEOUT 1800;`)
	if err != nil {
		t.Fatal(err)
	}

	go func() {
		time.Sleep(time.Second * 1)
		close(waitDisrupt)
	}()
	t.Log("prepare for query")
	_, err = db.ExecContext(ctx, `waitfor delay '00:00:3';`)
	if err != nil {
		t.Log("expected error after disconnect", err)
		return
	}
	t.Fatal("wanted error after Exec")
}

// TestDisconnect2 tests a read error so the query is started
// but results cannot be read.
func TestDisconnect2(t *testing.T) {
	if testing.Short() {
		t.Skip("short")
	}
	checkConnStr(t)
	SetLogger(testLogger{t})

	// Revert to the normal dialer after the test is done.
	normalCreateDialer := createDialer
	defer func() {
		createDialer = normalCreateDialer
	}()

	end := make(chan error)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	go func() {
		waitDisrupt := make(chan struct{})
		ctx, cancel = context.WithTimeout(ctx, time.Second*2)
		defer cancel()

		createDialer = func(p *connectParams) dialer {
			nd := tcpDialer{&net.Dialer{Timeout: p.dial_timeout, KeepAlive: p.keepAlive}}
			di := &dialerInterrupt{nd: nd}
			go func() {
				<-waitDisrupt
				di.Interrupt(false)
			}()
			return di
		}
		db, err := sql.Open("sqlserver", makeConnStr(t).String())
		if err != nil {
			t.Fatal(err)
		}

		if err := db.PingContext(ctx); err != nil {
			t.Fatal(err)
		}
		defer db.Close()

		_, err = db.ExecContext(ctx, `SET LOCK_TIMEOUT 1800;`)
		if err != nil {
			t.Fatal(err)
		}
		close(waitDisrupt)

		_, err = db.ExecContext(ctx, `waitfor delay '00:00:3';`)
		end <- err
	}()

	timeout := time.After(10 * time.Second)
	select {
	case err := <-end:
		if err == nil {
			t.Fatal("test err")
		}
	case <-timeout:
		t.Fatal("timeout")
	}
}
