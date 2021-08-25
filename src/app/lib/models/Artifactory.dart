import 'package:bkci_app/utils/util.dart';
import 'package:json_annotation/json_annotation.dart';
import 'package:bkci_app/utils/constants.dart';

part 'Artifactory.g.dart';

@JsonSerializable()
class Artifactory {
  final String name;
  final String fullName;
  final String path;
  final String fullPath;
  final int size;
  final bool folder;
  final int modifiedTime;
  final String artifactoryType;
  final bool show;
  final bool canDownload;
  final String logoUrl;
  final String bundleIdentifier;
  final String md5;
  final String platform;

  Artifactory({
    this.name,
    this.fullName,
    this.path,
    this.fullPath,
    this.size,
    this.folder,
    this.modifiedTime,
    this.artifactoryType,
    this.show,
    this.canDownload,
    this.logoUrl,
    this.bundleIdentifier,
    this.md5,
    this.platform,
  });

  String get identify {
    return '${fullPath.replaceAll('.apk', '')}_$artifactoryType';
  }

  String get uniqueId {
    return '${fullPath.replaceAll('.apk', '')}_${artifactoryType}_$md5';
  }

  String get platformText {
    return platformMap[platform] ?? '--';
  }

  bool get downloadable {
    return platformMatch(name);
  }

  bool get isPkg {
    return bundleIdentifier != null;
  }

  factory Artifactory.fromJson(Map<String, dynamic> json) =>
      _$ArtifactoryFromJson(json);
  Map<String, dynamic> toJson() => _$ArtifactoryToJson(this);
}
