import 'package:bkci_app/models/experience.dart';
import 'package:json_annotation/json_annotation.dart';

part 'BkBanner.g.dart';

@JsonSerializable()
class BkBanner {
  final int type;
  final String experienceHashId;
  final String bannerUrl;
  final String externalUrl;

  BkBanner({
    this.type,
    this.experienceHashId,
    this.bannerUrl,
    this.externalUrl,
  });

  bool get isAppStore {
    return this.type == ExpType.APP_STORE.index;
  }

  factory BkBanner.fromJson(Map<String, dynamic> json) =>
      _$BkBannerFromJson(json);
  Map<String, dynamic> toJson() => _$BkBannerToJson(this);
}
