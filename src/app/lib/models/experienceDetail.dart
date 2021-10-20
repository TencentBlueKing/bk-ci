import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:json_annotation/json_annotation.dart';
import 'package:bkci_app/utils/util.dart';

part 'experienceDetail.g.dart';

@JsonSerializable()
class ExperienceDetail {
  final String name;
  final String experienceHashId;
  final String experienceName;
  final String logoUrl;
  final String shareUrl;
  final String platform;
  final String version;
  final String versionTitle;
  final String remark;
  final String packageName;
  final bool expired;
  final bool canExperience;
  final bool online;
  final bool publicExperience;
  final String bundleIdentifier;
  final int appStatus;
  final String lastDownloadHashId;
  final String appScheme;
  final int experienceCondition;

  final List<String> productOwner;

  final int size;
  final int categoryId;
  final int createDate;
  final int endDate;

  ExperienceDetail(
      {this.name,
      this.experienceHashId,
      this.experienceName,
      this.logoUrl,
      this.shareUrl,
      this.platform,
      this.version,
      this.versionTitle,
      this.remark,
      this.packageName,
      this.expired,
      this.canExperience,
      this.online,
      this.publicExperience,
      this.productOwner = const [],
      this.size = 0,
      this.createDate,
      this.endDate,
      this.categoryId,
      this.bundleIdentifier,
      this.lastDownloadHashId,
      this.appScheme,
      this.appStatus,
      this.experienceCondition});

  String get formatSize {
    return size.mb;
  }

  String get formatCreateDate {
    return createDate.yMdhm;
  }

  String get formatEndDate {
    return endDate.yMdhm;
  }

  String get platformText {
    return platformMap[platform] ?? '';
  }

  String get getPublicType {
    return expTypeMap[experienceCondition] ?? '';
  }

  bool get showExpHistory {
    return experienceCondition == 2 || experienceCondition == 3;
  }

  Map<String, dynamic> get getInfoMap {
    return {
      "platform": platformText,
      "size": formatSize,
      "category": BkDevopsAppi18n.translate(CATEGORY_MAP[categoryId]),
      "createDate": formatCreateDate,
      "endDate": formatEndDate,
      "productOwner": productOwner
    };
  }

  factory ExperienceDetail.fromJson(Map<String, dynamic> json) =>
      _$ExperienceDetailFromJson(json);
  Map<String, dynamic> toJson() => _$ExperienceDetailToJson(this);
}
