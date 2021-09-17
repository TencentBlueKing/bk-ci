import 'package:bkci_app/main.dart';
import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/models/pageResponseBody.dart';
import 'package:bkci_app/pages/DetailScreen.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/AppListItem.dart';
import 'package:bkci_app/widgets/AppStoreListItem.dart';
import 'package:bkci_app/widgets/ToggleTab.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import '../InfinityList.dart';

class CatagoryTab extends StatefulWidget {
  final int catagory;

  CatagoryTab({
    @required this.catagory,
  });

  @override
  _CatagoryTabState createState() => _CatagoryTabState();
}

class _CatagoryTabState extends State<CatagoryTab> {
  int activeIndex = 0;
  String activeTabId = 'hot';

  static List<TabItem> tabs = [
    TabItem(
      id: 'hot',
      label: '热门APP',
    ),
    TabItem(
      id: 'new',
      label: '最近更新',
    ),
  ];

  void handleTab(TabItem tab, int index) {
    setState(() {
      activeIndex = index;
      activeTabId = tab.id;
    });
  }

  Future _fetchCatagory(int page, int pageSize) async {
    final responses = await ajax.get(
        '$EXPERIENCE_API_PREFIX/index/category/${widget.catagory}/$activeTabId?page=$page&pageSize=$pageSize');
    final item = PageResponseBody.fromJson(responses.data);
    final List<Experience> result =
        item.records.map((e) => Experience.fromJson(e)).toList();
    return [
      result,
      item.hasNext,
    ];
  }

  Future _refreshCatagory(int pageSize) async {
    return _fetchCatagory(1, pageSize);
  }

  void goDetail(String expId) {
    DetailScreenArgument args = DetailScreenArgument(expId: expId);
    DevopsApp.navigatorKey.currentState
        .pushNamed(DetailScreen.routePath, arguments: args);
  }

  Widget itemBuilder(item) {
    if (item.isAppStore) {
      return AppStoreListItem(
        id: item.experienceHashId,
        bundleIdentifier: item.bundleIdentifier,
        title: item.experienceName,
        leadingUrl: item.logoUrl,
        subTitle: BkDevopsAppi18n.of(context).$t('goAppStoreTips'),
        createTime: item.createTime,
        externalUrl: item.externalUrl,
      );
    }
    return AppListItem(
      onTap: goDetail,
      bundleIdentifier: item.bundleIdentifier,
      id: item.experienceHashId,
      title: item.experienceName,
      subTitle: item.subTitle,
      leadingUrl: item.logoUrl,
      createTime: item.createTime,
      size: item.size,
      lastDownloadHashId: item.lastDownloadHashId,
      expired: item.expired,
      appScheme: item.appScheme,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: EdgeInsets.only(top: 40.px),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32.px)),
        color: Colors.white,
      ),
      child: Column(
        children: [
          Container(
            margin: EdgeInsets.fromLTRB(32.px, 32.px, 32.px, 32.px),
            child: ToggleTab(
              value: activeIndex == 0,
              handleTap: handleTab,
              tabs: tabs,
            ),
          ),
          Expanded(
            child: InfinityList<Experience>(
              key: ValueKey(activeTabId),
              onFetchData: _fetchCatagory,
              onRefresh: _refreshCatagory,
              itemBuilder: itemBuilder,
            ),
          ),
        ],
      ),
    );
  }
}
