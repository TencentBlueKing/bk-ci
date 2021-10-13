// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'pageResponseBody.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PageResponseBody _$PageResponseBodyFromJson(Map<String, dynamic> json) {
  return PageResponseBody(
    hasNext: json['hasNext'] as bool,
    records: json['records'] as List,
    count: json['count'] as int,
    page: json['page'] as int,
    pageSize: json['pageSize'] as int,
    totalPages: json['totalPages'] as int,
  );
}

Map<String, dynamic> _$PageResponseBodyToJson(PageResponseBody instance) =>
    <String, dynamic>{
      'hasNext': instance.hasNext,
      'records': instance.records,
      'count': instance.count,
      'page': instance.page,
      'pageSize': instance.pageSize,
      'totalPages': instance.totalPages,
    };
