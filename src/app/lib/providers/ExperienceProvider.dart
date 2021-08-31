import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/models/pageResponseBody.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

// Mix-in [DiagnosticableTreeMixin] to have access to [debugFillProperties] for the devtool
class ExperienceProvider with ChangeNotifier, DiagnosticableTreeMixin {
  List<Experience> _myExperience = [];
  Map<String, List<Experience>> _section;
  bool _loading = false;
  bool _hasNext = false;
  int _page = 1;
  int _pageSize = 24;

  get myExperience => _myExperience;
  get section {
    final result = [];
    if (_section != null) {
      _section.forEach((key, value) {
        result.add(key);
        result.addAll(value);
      });
    }
    return result;
  }

  get hasNext => _hasNext;

  get page => _page;
  get pageSize => _pageSize;
  get loading => _loading;

  ExperienceProvider() {
    _loading = true;
    fetchMyExperience(_page, _pageSize);
  }

  loadMore() {
    if (_hasNext) {
      return fetchMyExperience(_page + 1, _pageSize);
    }
  }

  refresh() {
    return fetchMyExperience(1, _pageSize);
  }

  Future fetchMyExperience(int page, int pageSize) async {
    final List<Experience> result = [];

    try {
      final response =
          await ajax.get('$EXPERIENCE_API_PREFIX/v2/list', queryParameters: {
        'page': page,
        'pageSize': pageSize,
      });

      final PageResponseBody resBody = PageResponseBody.fromJson(response.data);

      resBody.records.forEach((ele) {
        result.add(Experience.fromJson(ele));
      });

      if (page == 1) {
        _myExperience = result;
      } else {
        _myExperience.addAll(result);
      }

      _section = new Map.fromIterable(
        _myExperience,
        key: (key) => key.getCreateDate,
        value: (value) {
          return _myExperience
              .where((item) => item.getCreateDate == value.getCreateDate)
              .toList();
        },
      );

      _hasNext = resBody.hasNext;
      _page = page;
      _pageSize = pageSize;
      _loading = false;

      notifyListeners();
    } catch (e) {
      _loading = false;
    }
  }
}
