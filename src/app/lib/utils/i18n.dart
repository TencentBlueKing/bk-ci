import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';

class BkDevopsAppi18n {
  BkDevopsAppi18n(this.locale);

  final Locale locale;

  static BkDevopsAppi18n of(BuildContext context) {
    return Localizations.of<BkDevopsAppi18n>(context, BkDevopsAppi18n);
  }

  static const LocalizationsDelegate<BkDevopsAppi18n> delegate =
      BkDevopsAppi18nDelegate();

  static Map<String, String> _bkDevopsAppi18nDictionary;

  Future<bool> load() async {
    String jsonStr =
        await rootBundle.loadString('assets/i18n/${locale.languageCode}.json');
    Map<String, dynamic> localeDictionary = json.decode(jsonStr);

    _bkDevopsAppi18nDictionary = localeDictionary.map((key, value) {
      return MapEntry(key, value.toString());
    });

    return true;
  }

  String get title {
    return _bkDevopsAppi18nDictionary['title'];
  }

  String $t(String text) {
    return _bkDevopsAppi18nDictionary[text] ?? text;
  }

  static String translate(String text) {
    return _bkDevopsAppi18nDictionary[text] ?? text;
  }
}

class BkDevopsAppi18nDelegate extends LocalizationsDelegate<BkDevopsAppi18n> {
  const BkDevopsAppi18nDelegate();

  @override
  bool isSupported(Locale locale) => ['en', 'zh'].contains(locale.languageCode);

  @override
  Future<BkDevopsAppi18n> load(Locale locale) async {
    // return SynchronousFuture<BkDevopsAppi18n>(BkDevopsAppi18n(locale));
    BkDevopsAppi18n localizations = new BkDevopsAppi18n(locale);
    Intl.defaultLocale = locale.languageCode;
    await localizations.load();
    return localizations;
  }

  @override
  bool shouldReload(BkDevopsAppi18nDelegate old) => false;
}
