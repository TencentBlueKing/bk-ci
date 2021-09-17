import 'package:json_annotation/json_annotation.dart';

part 'Pkg.g.dart';

@JsonSerializable()
class Pkg {
  final String experienceHashId;
  final int size;
  final String logoUrl;
  final String experienceName;
  final int createTime;
  final String bundleIdentifier;

  Pkg({
    this.experienceHashId,
    this.size,
    this.logoUrl,
    this.experienceName,
    this.createTime,
    this.bundleIdentifier,
  });

  factory Pkg.fromJson(Map<String, dynamic> json) => _$PkgFromJson(json);
  Map<String, dynamic> toJson() => _$PkgToJson(this);
}
