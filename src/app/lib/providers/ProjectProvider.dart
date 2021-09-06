import 'package:bkci_app/models/project.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/models/pageResponseBody.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'dart:convert';

class ProjectInfoProvider with ChangeNotifier, DiagnosticableTreeMixin {
  Project _currentProject;
  Map _currentView;
  bool _hasProject = false;
  bool _loading = true;

  ProjectInfoProvider() {
    fetchInitProject();
  }

  Project get currentProject => _currentProject;
  Map get currentView => _currentView;
  bool get hasProject => _hasProject;
  bool get loading => _loading;

  updateCurrentProject(Project newProject) {
    modifyCurrentProject(newProject);
    notifyListeners();
  }

  modifyCurrentProject(Project newProject) {
    _currentProject = newProject;
    _currentView = _getProjectCurrentView(newProject.projectCode);
    options.headers['X-DEVOPS-PROJECT-ID'] = newProject?.projectCode ?? '';
  }

  updateCurrentView(Map view) {
    _currentView = view;
    notifyListeners();
  }

  // 先取storage第一个，如果没有则取接口返回的第一个
  Future fetchInitProject() async {
    _loading = true;
    List localList = [];
    Map<String, dynamic> tmpProject = {};
    try {
      final String recentStr = Storage.getString('recentList') ?? '';

      localList = json.decode(recentStr);
    } catch (_) {
      localList = [];
    }
    if (localList.length > 0) {
      tmpProject = localList.first;
    } else {
      final projectResult =
          await ajax.get('/project/api/app/projects?page=1&pageSize=1');

      final result = PageResponseBody.fromJson(projectResult.data);
      if (result.records.length > 0) {
        tmpProject = result.records[0];
        localList.add(result.records[0]);
        Storage.setString('recentList', json.encode(localList));
      }
    }

    _hasProject = tmpProject.isNotEmpty;

    if (_hasProject) {
      modifyCurrentProject(Project.fromJson(tmpProject));
    }
    _loading = false;
    notifyListeners();
  }

  _getProjectCurrentView(projectCode) {
    Map viewMap = {};
    Map tmpCurrentView = {};
    Map defaultView = {'id': 'myPipeline', 'name': 'myPipeline'};
    try {
      final String currentViewStr = Storage.getString('currentViewMap') ?? '';
      viewMap = json.decode(currentViewStr);
      tmpCurrentView = viewMap[projectCode] ?? defaultView;
    } catch (_) {
      tmpCurrentView = defaultView;
    }
    return tmpCurrentView;
  }
}
