import 'package:json_annotation/json_annotation.dart';

part 'CommitRecord.g.dart';

@JsonSerializable()
class CommitRecord {
  int type;
  String pipelineId;
  String buildId;
  String commit;
  String committer;
  int commitTime;
  String comment;
  String repoId;
  String elementId;
  String url;

  CommitRecord({
    this.type,
    this.pipelineId,
    this.buildId,
    this.commit,
    this.committer,
    this.commitTime,
    this.comment,
    this.repoId,
    this.elementId,
    this.url,
  });

  factory CommitRecord.fromJson(Map<String, dynamic> json) =>
      _$CommitRecordFromJson(json);
  Map<String, dynamic> toJson() => _$CommitRecordToJson(this);
}
