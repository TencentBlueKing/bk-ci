// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'userInfo.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UserInfo _$UserInfoFromJson(Map<String, dynamic> json) {
  return UserInfo(
    englishName: json['englishName'] as String,
    avatars: json['avatars'] as String,
    email: json['email'] as String,
  );
}

Map<String, dynamic> _$UserInfoToJson(UserInfo instance) => <String, dynamic>{
      'englishName': instance.englishName,
      'avatars': instance.avatars,
      'email': instance.email,
    };
