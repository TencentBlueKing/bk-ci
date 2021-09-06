import 'package:bkci_app/models/pipeline.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/widgets/Empty.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/SearchTabs/SearchTop.dart';
import 'package:bkci_app/widgets/SearchTabs/SearchRecord.dart';
import 'package:bkci_app/widgets/pipelineListTab/ListItem.dart';
import 'package:bkci_app/models/pageResponseBody.dart';
import 'package:bkci_app/widgets/InfinityList.dart';

class SearchPipelineArgument {
  final String projectCode;

  SearchPipelineArgument({this.projectCode});
}

class SearchPipeline extends StatefulWidget {
  static const String routePath = '/searchPipeline';
  final String projectCode;

  SearchPipeline({this.projectCode});
  @override
  _SearchPipelineState createState() => _SearchPipelineState();
}

class _SearchPipelineState extends State<SearchPipeline> {
  final int recordLimitCount = 10;
  bool isSearching = true;
  String selectValue = '';
  List<String> records = [];
  List<String> commendList = [];
  ScrollController mController = new ScrollController();

  @override
  void initState() {
    super.initState();
    records = Storage.getList<List<String>>('bkciSearchPipelineList') ?? [];
    mController.addListener(() {
      FocusScope.of(context).requestFocus(FocusNode());
    });
  }

  List<String> _handleSearchRecords(String value) {
    if (value.isNotEmpty) {
      records.removeWhere((item) => item == value);
      records.insert(0, value);
      if (records.length > recordLimitCount) {
        records.removeRange(recordLimitCount, records.length);
      }
    }
    return records;
  }

  void handleClear() {
    setState(() {
      records = [];
      Storage.setList('bkciSearchPipelineList', records);
    });
  }

  void handleSelect(value) {
    setState(() {
      isSearching = false;
      selectValue = value;
      _handleSearchRecords(value);
      Storage.setList('bkciSearchPipelineList', records);
    });
  }

  void handleInput(value, isSubmit) {
    setState(() {
      selectValue = value;
      if (value == '') {
        isSearching = true;
      } else {
        if (isSubmit) {
          isSearching = false;
          _handleSearchRecords(value);
          Storage.setList('bkciSearchPipelineList', records);
        }
      }
    });
  }

  Future _loadData(int page, int pageSize) async {
    final responses = await ajax.get(
      '/process/api/app/pipelineViews/projects/${widget.projectCode}/listViewPipelines/v2?viewId=myPipeline&page=$page&pageSize=$pageSize&filterByPipelineName=$selectValue',
    );

    final item = PageResponseBody.fromJson(responses.data);
    final List<Pipeline> result =
        item.records.map((e) => Pipeline.fromJson(e)).toList();
    return [
      result,
      item.hasNext ?? false,
    ];
  }

  Future _reflashData(int pageSize) async {
    return _loadData(1, pageSize);
  }

  Widget itemBuilder(item) {
    return ListItem(
      title: item.pipelineName,
      subTitle: item.subTitle,
      status: item.getStatus,
      statusColor: item.statusColor,
      statusIcon: item.icon,
      projectId: item.projectId,
      pipelineId: item.pipelineId,
      hasCollect: item.hasCollect,
      canManualStartup: item.canManualStartup,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: GestureDetector(
          onTap: () {
            FocusScope.of(context).requestFocus(FocusNode());
          },
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SearchTop(
                searchValue: selectValue,
                onChanged: handleInput,
                placeholder: BkDevopsAppi18n.of(context).$t('searchPipeline'),
              ),
              Divider(height: 1.px),
              !isSearching
                  ? Expanded(
                      child: InfinityList(
                        key: ValueKey(selectValue),
                        pageSize: 300,
                        onFetchData: _loadData,
                        onRefresh: _reflashData,
                        itemBuilder: itemBuilder,
                        emptyWidget: Empty(
                          pic: EmptyPic.SEARCH,
                        ),
                        dividerBuilder: (
                          BuildContext context,
                          int index,
                          item,
                          nextItem,
                        ) {
                          return Divider(
                            indent: 32.px,
                            height: 1.px,
                          );
                        },
                      ),
                    )
                  : records.length > 0
                      ? Expanded(
                          child: SingleChildScrollView(
                            controller: mController,
                            physics: BouncingScrollPhysics(
                              parent: AlwaysScrollableScrollPhysics(),
                            ),
                            padding: EdgeInsets.only(top: 0.px),
                            child: SearchRecord(
                              list: records,
                              onSelected: handleSelect,
                              onClear: handleClear,
                            ),
                          ),
                        )
                      : null,
            ].where((e) => e != null).toList(),
          ),
        ),
      ),
    );
  }
}
