import 'package:json_annotation/json_annotation.dart';

part 'PipelineModel.g.dart';

@JsonSerializable()
class PipelineModel {
  final String name;
  final String desc;
  final List stages;

  PipelineModel({
    this.name,
    this.desc,
    this.stages,
  });

  factory PipelineModel.fromJson(Map<String, dynamic> json) =>
      _$PipelineModelFromJson(json);
  Map<String, dynamic> toJson() => _$PipelineModelToJson(this);
}
