import 'package:bkci_app/utils/i18n.dart';
import 'package:flutter/material.dart';

class SettingItem {
  final String key;
  final String label;
  final List<Map> options;
  final defaultValue;

  SettingItem({
    this.key,
    this.label,
    this.options,
    this.defaultValue,
  });
}

class AppSetting {
  Locale _locale;
  int _initRoute;

  AppSetting(Locale locale, int initRoute) {
    this._locale = locale;
    this._initRoute = initRoute;
  }

  Locale get locale => _locale;
  int get initRoute => _initRoute;

  static List<SettingItem> getSettingItems() {
    return [
      // SettingItem(
      //   key: 'locale',
      //   label: 'language',
      //   options: localeList,
      //   defaultValue: 'zh',
      // ),
      SettingItem(
        key: 'initRoute',
        label: 'initRoute',
        options: [
          {
            'key': 0,
            'value': BkDevopsAppi18n.translate('home'),
          },
          {
            'key': 1,
            'value': BkDevopsAppi18n.translate('experience'),
          },
          {
            'key': 2,
            'value': BkDevopsAppi18n.translate('pipeline'),
          },
        ],
        defaultValue: 0,
      ),
    ];
  }

  static List<Map<String, String>> localeList = [
    {
      'key': 'zh',
      'countryCode': 'CN',
      'value': '简体中文',
    },
    {
      'key': 'en',
      'countryCode': '',
      'value': 'English',
    },
  ];

  static Map getDefaultSetting() {
    return {
      'locale': 'zh',
      'initRoute': 0,
    };
  }
}
