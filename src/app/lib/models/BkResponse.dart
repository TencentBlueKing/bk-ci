import 'package:json_annotation/json_annotation.dart';

part 'BkResponse.g.dart';

@JsonSerializable()
class BkResponse {
  int status;
  dynamic data;
  String message;

  BkResponse({
    this.status,
    this.data,
    this.message,
  });

  factory BkResponse.fromJson(Map<String, dynamic> json) =>
      _$BkResponseFromJson(json);
  Map<String, dynamic> toJson() => _$BkResponseToJson(this);
}
