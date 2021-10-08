import 'package:bkci_app/models/CommitRecord.dart';
import 'package:json_annotation/json_annotation.dart';

part 'MaterialItem.g.dart';

@JsonSerializable()
class MaterialItem {
  final String name;
  final String elementId;
  final List<CommitRecord> records;

  MaterialItem({
    this.name,
    this.elementId,
    this.records,
  });

  factory MaterialItem.fromJson(Map<String, dynamic> json) =>
      _$MaterialItemFromJson(json);
  Map<String, dynamic> toJson() => _$MaterialItemToJson(this);
}
