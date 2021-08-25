import 'package:bkci_app/models/BkBanner.dart';
import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/models/pageResponseBody.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

class HomeData {
  final List<BkBanner> banners;
  final List<Experience> hots;
  final List<Experience> necessary;
  final List<Experience> newest;

  HomeData({
    this.banners,
    this.hots,
    this.necessary,
    this.newest,
  });
}

// Mix-in [DiagnosticableTreeMixin] to have access to [debugFillProperties] for the devtool
class HomeProvider with ChangeNotifier, DiagnosticableTreeMixin {
  HomeData _homeData = HomeData();
  bool _loading = false;

  get hots => _homeData.hots;
  get necessary => _homeData.necessary;
  get banners => _homeData.banners;
  get newest => _homeData.newest;

  get loading => _loading;

  HomeProvider() {
    _loading = true;
    fetchHome();
  }

  Future fetchHome() async {
    final apis = [
      'banners?pageSize=3',
      'hots?pageSize=9',
      'necessary?pageSize=5',
      'newest?pageSize=9',
    ];
    final List responses = await Future.wait(
      apis.map(
        (String e) => ajax.get(
            '$EXPERIENCE_API_PREFIX/index/$e&includeExternalUrl=true&page=1'),
      ),
    );

    final Map<String, dynamic> result = {};

    responses.forEach((e) {
      final item = PageResponseBody.fromJson(e.data);
      final int index = responses.indexOf(e);
      final String type = apis[index].split('?')[0];
      var list;
      if (type == 'banners') {
        list = item.records.map((e) => BkBanner.fromJson(e)).toList();
      } else {
        list = item.records.map((e) => Experience.fromJson(e)).toList();
      }
      result[type] = list;
    });

    _homeData = HomeData(
      banners: result['banners'],
      hots: result['hots'],
      necessary: result['necessary'],
      newest: result['newest'],
    );

    _loading = false;

    notifyListeners();
  }
}
