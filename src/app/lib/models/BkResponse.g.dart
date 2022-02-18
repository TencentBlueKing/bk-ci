// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'BkResponse.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

BkResponse _$BkResponseFromJson(Map<String, dynamic> json) {
  return BkResponse(
    status: json['status'] as int,
    data: json['data'],
    message: json['message'] as String,
  );
}

Map<String, dynamic> _$BkResponseToJson(BkResponse instance) =>
    <String, dynamic>{
      'status': instance.status,
      'data': instance.data,
      'message': instance.message,
    };
