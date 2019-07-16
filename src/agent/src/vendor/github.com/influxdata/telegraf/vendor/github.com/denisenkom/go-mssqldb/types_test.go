package mssql

import (
	"reflect"
	"testing"
	"time"
)

func TestMakeGoLangScanType(t *testing.T) {
	if (reflect.TypeOf(int64(0)) != makeGoLangScanType(typeInfo{TypeId: typeInt8})) {
		t.Errorf("invalid type returned for typeDateTime")
	}
	if (reflect.TypeOf(float64(0)) != makeGoLangScanType(typeInfo{TypeId: typeFlt4})) {
		t.Errorf("invalid type returned for typeDateTime")
	}
	if (reflect.TypeOf(float64(0)) != makeGoLangScanType(typeInfo{TypeId: typeFlt8})) {
		t.Errorf("invalid type returned for typeDateTime")
	}
	if (reflect.TypeOf("") != makeGoLangScanType(typeInfo{TypeId: typeVarChar})) {
		t.Errorf("invalid type returned for typeDateTime")
	}
	if (reflect.TypeOf(time.Time{}) != makeGoLangScanType(typeInfo{TypeId: typeDateTime})) {
		t.Errorf("invalid type returned for typeDateTime")
	}
	if (reflect.TypeOf(time.Time{}) != makeGoLangScanType(typeInfo{TypeId: typeDateTim4})) {
		t.Errorf("invalid type returned for typeDateTim4")
	}
	if (reflect.TypeOf(int64(0)) != makeGoLangScanType(typeInfo{TypeId: typeInt1})) {
		t.Errorf("invalid type returned for typeInt1")
	}
	if (reflect.TypeOf(int64(0)) != makeGoLangScanType(typeInfo{TypeId: typeInt2})) {
		t.Errorf("invalid type returned for typeInt2")
	}
	if (reflect.TypeOf(int64(0)) != makeGoLangScanType(typeInfo{TypeId: typeInt4})) {
		t.Errorf("invalid type returned for typeInt4")
	}
	if (reflect.TypeOf(int64(0)) != makeGoLangScanType(typeInfo{TypeId: typeIntN, Size: 4})) {
		t.Errorf("invalid type returned for typeIntN")
	}
	if (reflect.TypeOf([]byte{}) != makeGoLangScanType(typeInfo{TypeId: typeMoney, Size: 8})) {
		t.Errorf("invalid type returned for typeIntN")
	}
}

func TestMakeGoLangTypeName(t *testing.T) {
	defer handlePanic(t)

	tests := []struct {
		typeName   string
		typeString string
		typeID     uint8
	}{
		{"typeDateTime", "DATETIME", typeDateTime},
		{"typeDateTim4", "SMALLDATETIME", typeDateTim4},
		{"typeBigBinary", "BINARY", typeBigBinary},
		//TODO: Add other supported types
	}

	for _, tt := range tests {
		if makeGoLangTypeName(typeInfo{TypeId: tt.typeID}) != tt.typeString {
			t.Errorf("invalid type name returned for %s", tt.typeName)
		}
	}
}

func TestMakeGoLangTypeLength(t *testing.T) {
	defer handlePanic(t)

	tests := []struct {
		typeName   string
		typeVarLen bool
		typeLen    int64
		typeID     uint8
	}{
		{"typeDateTime", false, 0, typeDateTime},
		{"typeDateTim4", false, 0, typeDateTim4},
		{"typeBigBinary", false, 0, typeBigBinary},
		//TODO: Add other supported types
	}

	for _, tt := range tests {
		n, v := makeGoLangTypeLength(typeInfo{TypeId: tt.typeID})
		if v != tt.typeVarLen {
			t.Errorf("invalid type length variability returned for %s", tt.typeName)
		}
		if n != tt.typeLen {
			t.Errorf("invalid type length returned for %s", tt.typeName)
		}
	}
}

func TestMakeGoLangTypePrecisionScale(t *testing.T) {
	defer handlePanic(t)

	tests := []struct {
		typeName   string
		typeID     uint8
		typeVarLen bool
		typePrec   int64
		typeScale  int64
	}{
		{"typeDateTime", typeDateTime, false, 0, 0},
		{"typeDateTim4", typeDateTim4, false, 0, 0},
		{"typeBigBinary", typeBigBinary, false, 0, 0},
		//TODO: Add other supported types
	}

	for _, tt := range tests {
		prec, scale, varLen := makeGoLangTypePrecisionScale(typeInfo{TypeId: tt.typeID})
		if varLen != tt.typeVarLen {
			t.Errorf("invalid type length variability returned for %s", tt.typeName)
		}
		if prec != tt.typePrec || scale != tt.typeScale {
			t.Errorf("invalid type precision and/or scale returned for %s", tt.typeName)
		}
	}
}

func handlePanic(t *testing.T) {
	if r := recover(); r != nil {
		t.Errorf("recovered panic")
	}
}
