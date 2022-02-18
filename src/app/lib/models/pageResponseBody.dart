import 'package:json_annotation/json_annotation.dart';

part 'pageResponseBody.g.dart';

@JsonSerializable()
class PageResponseBody {
  final bool hasNext;
  final List records;
  final int count;
  final int page;
  final int pageSize;
  final int totalPages;

  PageResponseBody({
    this.hasNext,
    this.records,
    this.count,
    this.page,
    this.pageSize,
    this.totalPages,
  });

  factory PageResponseBody.fromJson(Map<String, dynamic> json) =>
      _$PageResponseBodyFromJson(json);
  Map<String, dynamic> toJson() => _$PageResponseBodyToJson(this);
}
