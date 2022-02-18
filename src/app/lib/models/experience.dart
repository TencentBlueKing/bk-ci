import 'package:json_annotation/json_annotation.dart';
import 'package:bkci_app/utils/util.dart';

part 'experience.g.dart';

enum ExpType {
  DEFAULT,
  ENTERTIANMENT,
  APP_STORE,
}

@JsonSerializable()
class Experience {
  final int type;
  final String experienceHashId;
  final String bundleIdentifier;
  final String experienceName;
  final String version;
  final String logoUrl;
  final int createTime;
  final int createDate;
  final int size;
  final String name;
  final String versionTitle;
  final int downloadTime;
  final String externalUrl;
  final int appStatus;
  final String lastDownloadHashId;
  final String appScheme;
  final bool expired;

  Experience({
    this.type,
    this.experienceHashId,
    this.bundleIdentifier,
    this.experienceName,
    this.version,
    this.size = 0,
    this.createTime,
    this.createDate,
    this.logoUrl,
    this.name,
    this.versionTitle,
    this.externalUrl,
    this.downloadTime,
    this.appStatus,
    this.lastDownloadHashId,
    this.appScheme,
    this.expired = false,
  });

  String get formatSize {
    return size.mb;
  }

  String get date {
    return createTime.yyMMdd;
  }

  String get getCreateDate {
    return createDate.yyMMdd;
  }

  String get getCreateMd {
    return createDate.mmd;
  }

  String get subTitle {
    return '${this.date}      ${this.formatSize}';
  }

  bool get isAppStore {
    return this.type == ExpType.APP_STORE.index;
  }

  String get downloadDate {
    return downloadTime is int ? downloadTime.yMdhm : '';
  }

  factory Experience.fromJson(Map<String, dynamic> json) =>
      _$ExperienceFromJson(json);
  Map<String, dynamic> toJson() => _$ExperienceToJson(this);
}
