import 'package:bkci_app/models/pageResponseBody.dart';
import 'package:bkci_app/models/project.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/Empty.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/pipelineListTab/ProjectItem.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import '../InfinityList.dart';
import 'dart:convert';

import 'package:bkci_app/providers/ProjectProvider.dart';
import 'package:provider/provider.dart';

import 'package:bkci_app/widgets/SearchInput.dart';

class SelectProject extends StatefulWidget {
  SelectProject();

  @override
  _SelectProjectState createState() => _SelectProjectState();
}

class _SelectProjectState extends State<SelectProject> {
  bool showSearch = false;
  String _tempSearchValue = '';
  String searchValue = '';

  List projectList = [];

  void handleInputChange(String value, bool isSubmit) {
    value = value.trim();
    setState(() {
      _tempSearchValue = value;
      if (isSubmit) {
        showSearch = value.isNotEmpty;
        searchValue = value;
      }
    });
  }

  void _handleRecentList(project) {
    final String recentStr = Storage.getString('recentList') ?? '';
    List localList = [];
    if (recentStr != '') {
      localList = json.decode(recentStr);
    }
    localList
        .removeWhere((item) => item['projectCode'] == project['projectCode']);
    localList.insert(0, project);
    if (localList.length > 5) {
      localList.removeRange(5, localList.length);
    }
    Storage.setString('recentList', jsonEncode(localList));
  }

  void _onTap(item) {
    _handleRecentList(item);
    Provider.of<ProjectInfoProvider>(context, listen: false)
        .updateCurrentProject(Project.fromJson(item));
    Navigator.of(context).pop();
  }

  // 最近访问列表
  List<Widget> recentListWidget() {
    List<Widget> widgets = [];
    try {
      final String recentStr = Storage.getString('recentList') ?? '';
      this.projectList = json.decode(recentStr);
    } catch (_) {
      this.projectList = [];
    }
    if (this.projectList.isEmpty) {
      return [
        Center(
          child: PFText(
            BkDevopsAppi18n.of(context).$t('noRecentProjectListTips'),
          ),
        ),
      ].toList();
    }
    Map firstProject = this.projectList[0];
    this.projectList.forEach((item) {
      Project projectItem = Project.fromJson(item);
      widgets.add(ProjectItem(
        projectItem: projectItem,
        onTap: _onTap,
        selected: firstProject['projectCode'] == projectItem.projectCode,
      ));
      widgets.add(Divider(height: 1.px, indent: 80.px));
    });
    return widgets;
  }

  // 搜索结果和全部项目列表
  Widget projectListWidget() {
    return Padding(
      padding: EdgeInsets.only(top: showSearch ? 32.px : 0),
      child: InfinityList(
        key: ValueKey(searchValue),
        pageSize: 100,
        onFetchData: loadData,
        onRefresh: onRefresh,
        itemBuilder: (item) => ProjectItem(
          projectItem: item,
          onTap: _onTap,
        ),
        emptyWidget: Empty(
          pic: showSearch ? EmptyPic.SEARCH : EmptyPic.NORMAL,
        ),
        dividerBuilder: (
          BuildContext context,
          int index,
          item,
          nextItem,
        ) {
          return Divider(
            indent: 80.px,
            height: 1.px,
          );
        },
      ),
    );
  }

  Future loadData(int page, int pageSize) async {
    String url = '/project/api/app/projects?page=$page&pageSize=$pageSize';
    if (showSearch) {
      url += '&searchName=$searchValue';
    }
    final projectResult = await ajax.get(url);
    final result = PageResponseBody.fromJson(projectResult.data);
    final List<Project> list = [];
    List.from(result.records).forEach((e) {
      final Project item = Project.fromJson(e);
      list.add(item);
    });
    return [
      list,
      result.hasNext ?? false,
    ];
  }

  Future onRefresh(int pageSize) async {
    return loadData(1, pageSize);
  }

  Column _projectColumn(BuildContext context) {
    return Column(
      children: [
        Container(
          alignment: Alignment.topLeft,
          padding: EdgeInsets.only(top: 40.px),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              PFMediumText(
                BkDevopsAppi18n.of(context).$t('recentVisit'),
                style: TextStyle(
                  color: '#979BA5'.color,
                  fontSize: 28.px,
                ),
              ),
              SingleChildScrollView(
                keyboardDismissBehavior:
                    ScrollViewKeyboardDismissBehavior.onDrag,
                physics: BouncingScrollPhysics(
                  parent: const AlwaysScrollableScrollPhysics(),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: recentListWidget(),
                ),
              )
            ],
          ),
        ),
        Container(
          alignment: Alignment.topLeft,
          padding: EdgeInsets.only(top: 16.px),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              PFMediumText(
                BkDevopsAppi18n.of(context).$t('allProject'),
                style: TextStyle(
                  color: '#979BA5'.color,
                  fontSize: 28.px,
                ),
              ),
              IconButton(
                onPressed: () {
                  showDialog(
                    context: context,
                    builder: (BuildContext context) {
                      return AlertDialog(
                        title: PFMediumText(
                          BkDevopsAppi18n.of(context).$t('stream'),
                        ),
                        content: PFText(
                          BkDevopsAppi18n.of(context).$t('streamProjectTips'),
                        ),
                        actions: [
                          TextButton(
                            onPressed: () {
                              Navigator.of(context).pop();
                            },
                            child: PFText(
                              BkDevopsAppi18n.of(context).$t('cancel'),
                            ),
                          ),
                        ],
                      );
                    },
                  );
                },
                iconSize: 36.px,
                color: '#979BA5'.color,
                padding: EdgeInsets.zero,
                icon: Icon(BkIcons.info),
              ),
            ],
          ),
        ),
        Expanded(
          child: projectListWidget(),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Container(
        height: SizeFit.deviceHeight * 0.85,
        padding: EdgeInsets.fromLTRB(32.px, 8.px, 32.px, 0),
        child: Column(
          children: [
            Center(
              child: Container(
                width: 72.px,
                height: 8.px,
                margin: EdgeInsets.only(top: 10.px),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(4.px),
                  color: Theme.of(context).hintColor,
                ),
              ),
            ),
            Container(
              padding: EdgeInsets.only(top: 30.px),
              child: SearchInput(
                searchValue: _tempSearchValue,
                handleChange: handleInputChange,
                autofocus: false,
                placeholder: BkDevopsAppi18n.of(context).$t('searchProject'),
              ),
            ),
            Expanded(
              child: showSearch ? projectListWidget() : _projectColumn(context),
            )
          ],
        ),
      ),
    );
  }
}
