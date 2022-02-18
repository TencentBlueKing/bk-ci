import 'package:json_annotation/json_annotation.dart';
import 'package:version/version.dart';
import 'package:bkci_app/utils/util.dart';

part 'Appversion.g.dart';

@JsonSerializable()
class Appversion {
  final int id;
  final String versionId;
  final int releaseDate;
  final String releaseContent;
  final int channelType;
  final int updateType;

  Appversion({
    this.id,
    this.versionId,
    this.releaseDate,
    this.releaseContent,
    this.channelType,
    this.updateType,
  });

  Version get semver {
    return Version.parse(versionId);
  }

  String get releaseTime {
    return releaseDate.yyMMdd;
  }

  bool get isForceUpgrade {
    return updateType == 1;
  }

  factory Appversion.fromJson(Map<String, dynamic> json) =>
      _$AppversionFromJson(json);
  Map<String, dynamic> toJson() => _$AppversionToJson(this);
}
