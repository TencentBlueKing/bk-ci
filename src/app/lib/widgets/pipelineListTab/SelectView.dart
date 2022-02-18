import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/pipelineListTab/ViewItem.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/utils/i18n.dart';
import '../InfinityList.dart';
import 'dart:convert';

import 'package:bkci_app/providers/ProjectProvider.dart';
import 'package:provider/provider.dart';

class SelectView extends StatefulWidget {
  final String projectCode;
  final String currentView;

  SelectView({@required this.projectCode, @required this.currentView});

  @override
  _SelectViewState createState() => _SelectViewState();
}

class _SelectViewState extends State<SelectView> {
  List projectList = [];

  List<Widget> widgets = [];

  void updateStorageViews(item) {
    Map viewMap;
    try {
      final String currentViewStr = Storage.getString('currentViewMap') ?? '';
      viewMap = json.decode(currentViewStr);
    } catch (err) {
      viewMap = {};
    }
    viewMap[widget.projectCode] = item;
    Storage.setString('currentViewMap', json.encode(viewMap));
  }

  void _onTap(item) {
    this.updateStorageViews(item);
    Provider.of<ProjectInfoProvider>(context, listen: false)
        .updateCurrentView(item);
    Navigator.of(context).pop();
  }

  // 视图列表
  Widget viewListWidget() {
    return Padding(
      padding: EdgeInsets.only(top: 0),
      child: InfinityList(
        pageSize: 100,
        onFetchData: loadData,
        onRefresh: onRefresh,
        itemBuilder: (item) => ViewItem(
          viewItem: item,
          selected: item['id'] == widget.currentView,
          onTap: _onTap,
        ),
        dividerBuilder: (
          BuildContext context,
          int index,
          item,
          nextItem,
        ) {
          return Divider(
            height: .5,
          );
        },
      ),
    );
  }

  Future loadData(int page, int pageSize) async {
    String url =
        '/process/api/app/pipelineViews/projects/${widget.projectCode}/settings';

    final viewResult = await ajax.get(url);

    final List<dynamic> list = [];
    if (viewResult.data['viewClassifies'] != null) {
      List.from(viewResult.data['viewClassifies']).forEach((e) {
        list.addAll(e['viewList']);
      });
      // 我的流水线和全部流水线数据一样，去掉全部流水线
      list.removeWhere((item) => item['id'] == 'allPipeline');
    }
    return [
      list,
      false,
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
          padding: EdgeInsets.only(top: 32.px),
          child: PFMediumText(
            BkDevopsAppi18n.of(context).$t('viewList') ?? '',
            style: TextStyle(
              color: '#979BA5'.color,
              fontSize: 28.px,
            ),
          ),
        ),
        Expanded(
          child: viewListWidget(),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    widgets = [];

    return SafeArea(
      child: SingleChildScrollView(
        child: Container(
          height: 1200.px,
          padding: EdgeInsets.fromLTRB(40.px, 8.px, 40.px, 0),
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
              Expanded(
                child: _projectColumn(context),
              )
            ],
          ),
        ),
      ),
    );
  }
}
