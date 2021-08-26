import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/AppStoreListItem.dart';
import 'package:bkci_app/widgets/Empty.dart';
import 'package:bkci_app/widgets/ExperienceListItem.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/SearchTabs/SearchTop.dart';
import 'package:bkci_app/widgets/SearchTabs/RecommendList.dart';
import 'package:bkci_app/widgets/SearchTabs/SearchRecord.dart';
import 'package:bkci_app/pages/DetailScreen.dart';

import 'package:bkci_app/widgets/InfinityList.dart';
import 'package:bkci_app/widgets/AppListItem.dart';

class SearchScreenArgs {
  final bool experiencePublic;

  SearchScreenArgs({
    this.experiencePublic,
  });
}

class SearchScreen extends StatefulWidget {
  static const String routePath = '/search';
  final bool experiencePublic;

  SearchScreen({
    this.experiencePublic = true,
  });

  @override
  _SearchScreenState createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final int recordLimitCount = 10;
  bool isSearching = true;
  String selectValue = '';
  List<String> records = [];
  List<String> commendList = [];
  ScrollController mController = new ScrollController();

  @override
  void initState() {
    super.initState();
    records = Storage.getList<List<String>>('bkciSearchRecordList') ?? [];
    getRecommendList();

    mController.addListener(() {
      FocusScope.of(context).requestFocus(FocusNode());
    });
  }

  Future getRecommendList() async {
    final result =
        await ajax.get('/experience/api/app/experiences/search/recommends');

    setState(() {
      result.data.forEach((e) {
        commendList.add(e['content']);
      });
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
      Storage.setList('bkciSearchRecordList', records);
    });
  }

  void handleSelect(value) {
    setState(() {
      isSearching = false;
      selectValue = value;
      _handleSearchRecords(value);
      Storage.setList('bkciSearchRecordList', records);
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
          Storage.setList('bkciSearchRecordList', records);
        }
      }
    });
  }

  Future loadData(int page, int pageSize) async {
    final result = await ajax.get(
        '/experience/api/app/experiences/search/$selectValue?experiencePublic=${widget.experiencePublic}');

    final List<Experience> expList = [];
    result.data.forEach((e) {
      final Experience exp = Experience.fromJson(e);
      expList.add(exp);
    });
    if (widget.experiencePublic) {
      return [expList, false];
    }

    final sectionResult = [];
    new Map.fromIterable(
      expList,
      key: (key) => key.date,
      value: (value) {
        return expList.where((item) => item.date == value.date).toList();
      },
    ).forEach((key, value) {
      sectionResult.add(key);
      sectionResult.addAll(value);
    });

    return [sectionResult, false];
  }

  Future onRefresh(int pageSize) async {
    return loadData(1, pageSize);
  }

  void goDetail(BuildContext context, String expId) {
    DetailScreenArgument args = DetailScreenArgument(expId: expId);
    Navigator.of(context).pushNamed(DetailScreen.routePath, arguments: args);
  }

  Widget buildSectionHeader(String title) {
    return Container(
      color: Theme.of(context).backgroundColor,
      padding: EdgeInsets.fromLTRB(32.px, 24.px, 0, 16.px),
      child: PFMediumText(
        title,
        style: TextStyle(
          fontSize: 24.px,
        ),
      ),
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
                placeholder: BkDevopsAppi18n.of(context).$t('searchExp'),
              ),
              Divider(height: 1.px),
              isSearching
                  ? Expanded(
                      child: SingleChildScrollView(
                        controller: mController,
                        physics: BouncingScrollPhysics(
                          parent: AlwaysScrollableScrollPhysics(),
                        ),
                        padding: EdgeInsets.only(top: 0.px),
                        child: Column(
                          children: [
                            records.length > 0
                                ? SearchRecord(
                                    list: records,
                                    onSelected: handleSelect,
                                    onClear: handleClear)
                                : null,
                            widget.experiencePublic
                                ? RecommendList(
                                    list: commendList,
                                    onSelected: handleSelect,
                                  )
                                : null
                          ].where((e) => e != null).toList(),
                        ),
                      ),
                    )
                  : Expanded(
                      child: InfinityList(
                        key: ValueKey(selectValue),
                        pageSize: 300,
                        onFetchData: loadData,
                        onRefresh: onRefresh,
                        emptyWidget: Empty(
                          pic: EmptyPic.SEARCH,
                        ),
                        dividerBuilder: (
                          BuildContext context,
                          int index,
                          item,
                          nextItem,
                        ) {
                          return (item is Experience && nextItem is! String)
                              ? Divider(
                                  indent: 160.px,
                                  height: 1.px,
                                )
                              : SizedBox();
                        },
                        itemBuilder: (item) {
                          if (widget.experiencePublic) {
                            if (item.isAppStore) {
                              return AppStoreListItem(
                                id: item.experienceHashId,
                                bundleIdentifier: item.bundleIdentifier,
                                title: item.experienceName,
                                leadingUrl: item.logoUrl,
                                subTitle: BkDevopsAppi18n.of(context)
                                    .$t('goAppStoreTips'),
                                createTime: item.createTime,
                                externalUrl: item.externalUrl,
                              );
                            }
                            return AppListItem(
                              onTap: (String expId) {
                                goDetail(context, expId);
                              },
                              bundleIdentifier: item.bundleIdentifier,
                              id: item.experienceHashId,
                              title: item.experienceName,
                              subTitle: item.subTitle,
                              leadingUrl: item.logoUrl,
                              createTime: item.createTime,
                              size: item.size,
                              expired: item.expired,
                              appScheme: item.appScheme,
                              lastDownloadHashId: item.lastDownloadHashId,
                            );
                          }
                          return item is Experience
                              ? ExperienceListItem(
                                  item: item,
                                  onTap: goDetail,
                                )
                              : buildSectionHeader(item);
                        },
                      ),
                    ),
            ],
          ),
        ),
      ),
    );
  }
}
