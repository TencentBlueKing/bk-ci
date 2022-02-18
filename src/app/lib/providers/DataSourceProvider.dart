import 'package:bkci_app/models/build.dart';
import 'package:flutter/material.dart';

class DataSource<T> extends ChangeNotifier {
  List<T> list = [];

  void setDataSource(List<T> data) {
    this.list = data;
  }

  void notify() {
    notifyListeners();
  }

  bool hasData() {
    return this.list != null && this.list.length > 0;
  }
}

class DataListSource extends DataSource<Build> {
  Map<String, int> oldBuildIdMap = Map();

  DataListSource() {
    this.list.asMap().forEach((int index, Build e) {
      oldBuildIdMap[e.id] = index;
    });
  }

  void removeItemByIndex(int index) {
    this.list.removeAt(index);
    notify();
  }

  void removeItem(Build buildInfo) {
    var indexOf = oldBuildIdMap[buildInfo.id] ?? -1;
    if (indexOf >= 0) {
      removeItemByIndex(indexOf);
    }
  }

  void mergePartialData(List<Build> newData) {
    bool hasUpdate = false;
    final newBuildIdMap = Map<String, int>();

    newData.asMap().forEach((int index, Build buildInfo) {
      newBuildIdMap[buildInfo.id] = index;
      var existing = oldBuildIdMap[buildInfo.id] ?? -1;
      if (existing >= 0) {
        // my example compares modified dates but this is simplery
        list[existing] = buildInfo;
        hasUpdate = true;
      } else if (existing == -1) {
        list.add(buildInfo);
        hasUpdate = true;
      }
    });

    oldBuildIdMap.forEach((String id, int index) {
      if (newBuildIdMap[id] == null) {
        list.removeAt(index);
        hasUpdate = true;
      }
    });

    list.asMap().forEach((int index, Build e) {
      oldBuildIdMap[e.id] = index;
    });

    if (hasUpdate) {
      notify();
    }
  }
}
