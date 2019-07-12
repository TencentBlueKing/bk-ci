// +build go1.10

package mssql

import (
	"context"
	"database/sql"
	"strings"
	"testing"
	"time"

	"cloud.google.com/go/civil"
)

func TestSessionInitSQL(t *testing.T) {
	checkConnStr(t)
	SetLogger(testLogger{t})

	d := &Driver{}
	connector, err := d.OpenConnector(makeConnStr(t).String())
	if err != nil {
		t.Fatal("unable to open connector", err)
	}

	// Do not use these settings in your application
	// unless you know what they do.
	// Thes are for this unit test only.
	//
	// Sessions will be reset even if SessionInitSQL is not set.
	connector.SessionInitSQL = `
SET XACT_ABORT ON; -- 16384
SET ANSI_NULLS ON; -- 32
SET ARITHIGNORE ON; -- 128
`

	pool := sql.OpenDB(connector)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	var opt int32
	err = pool.QueryRowContext(ctx, `
select Options = @@OPTIONS;
`).Scan(&opt)
	if err != nil {
		t.Fatal("failed to run query", err)
	}
	mask := int32(16384 | 128 | 32)

	if opt&mask != mask {
		t.Fatal("incorrect session settings", opt)
	}
}

func TestParameterTypes(t *testing.T) {
	checkConnStr(t)
	pool, err := sql.Open("sqlserver", makeConnStr(t).String())
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()

	tin, err := time.Parse(time.RFC3339, "2006-01-02T22:04:05-07:00")
	if err != nil {
		t.Fatal(err)
	}

	var nv, v, nvcm, vcm, dt1, dt2, tm, d, dto string
	row := pool.QueryRow(`
select
	nv = SQL_VARIANT_PROPERTY(@nv,'BaseType'),
	v = SQL_VARIANT_PROPERTY(@v,'BaseType'),
	@nvcm,
    @vcm,
	dt1 = SQL_VARIANT_PROPERTY(@dt1,'BaseType'),
	dt2 = SQL_VARIANT_PROPERTY(@dt2,'BaseType'),
	d = SQL_VARIANT_PROPERTY(@d,'BaseType'),
	tm = SQL_VARIANT_PROPERTY(@tm,'BaseType'),
	dto = SQL_VARIANT_PROPERTY(@dto,'BaseType')
;
	`,
		sql.Named("nv", "base type nvarchar"),
		sql.Named("v", VarChar("base type varchar")),
		sql.Named("nvcm", NVarCharMax(strings.Repeat("x", 5000))),
		sql.Named("vcm", VarCharMax(strings.Repeat("x", 5000))),
		sql.Named("dt1", DateTime1(tin)),
		sql.Named("dt2", civil.DateTimeOf(tin)),
		sql.Named("d", civil.DateOf(tin)),
		sql.Named("tm", civil.TimeOf(tin)),
		sql.Named("dto", DateTimeOffset(tin)),
	)
	err = row.Scan(&nv, &v, &nvcm, &vcm, &dt1, &dt2, &d, &tm, &dto)
	if err != nil {
		t.Fatal(err)
	}

	if nv != "nvarchar" {
		t.Errorf(`want "nvarchar" got %q`, nv)
	}
	if v != "varchar" {
		t.Errorf(`want "varchar" got %q`, v)
	}
	if nvcm != strings.Repeat("x", 5000) {
		t.Errorf(`incorrect value returned for nvarchar(max): %q`, nvcm)
	}
	if vcm != strings.Repeat("x", 5000) {
		t.Errorf(`incorrect value returned for varchar(max): %q`, vcm)
	}
	if dt1 != "datetime" {
		t.Errorf(`want "datetime" got %q`, dt1)
	}
	if dt2 != "datetime2" {
		t.Errorf(`want "datetime2" got %q`, dt2)
	}
	if d != "date" {
		t.Errorf(`want "date" got %q`, d)
	}
	if tm != "time" {
		t.Errorf(`want "time" got %q`, tm)
	}
	if dto != "datetimeoffset" {
		t.Errorf(`want "datetimeoffset" got %q`, dto)
	}
}

func TestParameterValues(t *testing.T) {
	checkConnStr(t)
	pool, err := sql.Open("sqlserver", makeConnStr(t).String())
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()

	sin := "high five"
	tin, err := time.Parse(time.RFC3339, "2006-01-02T22:04:05-07:00")
	if err != nil {
		t.Fatal(err)
	}

	var nv, v, tgo, dt1, dt2, tm, d, dto string
	err = pool.QueryRow(`
select
	nv = @nv,
	v = @v,
	tgo = @tgo,
	dt1 = convert(nvarchar(200), @dt1, 121),
	dt2 = convert(nvarchar(200), @dt2, 121),
	d = convert(nvarchar(200), @d, 121),
	tm = convert(nvarchar(200), @tm, 121),
	dto = convert(nvarchar(200), @dto, 121)
;
	`,
		sql.Named("nv", sin),
		sql.Named("v", sin),
		sql.Named("tgo", tin),
		sql.Named("dt1", DateTime1(tin)),
		sql.Named("dt2", civil.DateTimeOf(tin)),
		sql.Named("d", civil.DateOf(tin)),
		sql.Named("tm", civil.TimeOf(tin)),
		sql.Named("dto", DateTimeOffset(tin)),
	).Scan(&nv, &v, &tgo, &dt1, &dt2, &d, &tm, &dto)
	if err != nil {
		t.Fatal(err)
	}
	if want := sin; nv != want {
		t.Errorf(`want %q got %q`, want, nv)
	}
	if want := sin; v != want {
		t.Errorf(`want %q got %q`, want, v)
	}
	if want := "2006-01-02T22:04:05-07:00"; tgo != want {
		t.Errorf(`want %q got %q`, want, tgo)
	}
	if want := "2006-01-02 22:04:05.000"; dt1 != want {
		t.Errorf(`want %q got %q`, want, dt1)
	}
	if want := "2006-01-02 22:04:05.0000000"; dt2 != want {
		t.Errorf(`want %q got %q`, want, dt2)
	}
	if want := "2006-01-02"; d != want {
		t.Errorf(`want %q got %q`, want, d)
	}
	if want := "22:04:05.0000000"; tm != want {
		t.Errorf(`want %q got %q`, want, tm)
	}
	if want := "2006-01-02 22:04:05.0000000 -07:00"; dto != want {
		t.Errorf(`want %q got %q`, want, dto)
	}
}
