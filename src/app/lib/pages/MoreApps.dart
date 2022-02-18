import 'package:bkci_app/main.dart';
import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/models/pageResponseBody.dart';
import 'package:bkci_app/pages/DetailScreen.dart';
import 'package:bkci_app/pages/SearchScreen.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/AppStoreListItem.dart';
import 'package:bkci_app/widgets/BkAppBar.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/InfinityList.dart';
import 'package:bkci_app/widgets/AppListItem.dart';

class MoreAppsArgument {
  final String title;
  final String type;

  MoreAppsArgument({
    this.title,
    this.type,
  });
}

class MoreApps extends StatefulWidget {
  static const String routePath = '/moreapps';
  final String title;
  final String type;

  MoreApps({Key key, this.title, this.type}) : super(key: key);
  @override
  _MoreAppsState createState() => _MoreAppsState();
}

class _MoreAppsState extends State<MoreApps> {
  void goSearch() {
    DevopsApp.navigatorKey.currentState.pushNamed(SearchScreen.routePath);
  }

  Future loadData(int page, int pageSize) async {
    final pageResult = await ajax.get(
        '/experience/api/app/experiences/index/${widget.type}?page=$page&pageSize=$pageSize');
    final result = PageResponseBody.fromJson(pageResult.data);
    final List<Experience> expList = [];
    List.from(result.records).forEach((e) {
      final Experience exp = Experience.fromJson(e);
      expList.add(exp);
    });
    return [
      expList,
      result.hasNext ?? false,
    ];
  }

  Future onRefresh(int pageSize) async {
    return loadData(1, pageSize);
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
    } else {
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
        appScheme: item.appScheme,
        expired: item.expired,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: BkAppBar(
        title: BkDevopsAppi18n.of(context).$t(widget.title),
        actions: [
          IconButton(
            icon: Icon(BkIcons.search),
            onPressed: goSearch,
          ),
        ],
      ),
      body: Padding(
        padding: EdgeInsets.only(top: 24.px),
        child: InfinityList<Experience>(
          onFetchData: loadData,
          onRefresh: onRefresh,
          itemBuilder: itemBuilder,
        ),
      ),
    );
  }
}
