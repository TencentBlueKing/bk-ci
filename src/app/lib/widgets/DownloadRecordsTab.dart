import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/models/pageResponseBody.dart';
import 'package:bkci_app/pages/DetailScreen.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/AppListItem.dart';
import 'package:bkci_app/widgets/InfinityList.dart';

import 'package:flutter/material.dart';

class DownloadRecordsTab extends StatelessWidget {
  Future getDownloadRecords(int page, int pageSize) async {
    try {
      final res = await ajax.get(
          '$EXPERIENCE_API_PREFIX/download/records?page=$page&pageSize=$pageSize');
      final result = PageResponseBody.fromJson(res.data);

      final List<Experience> expList = [];
      List.from(result.records).forEach((e) {
        final Experience exp = Experience.fromJson(e);
        expList.add(exp);
      });
      return [
        expList,
        result.hasNext ?? false,
      ];
    } catch (e) {
      return [[], false];
    }
  }

  Future handleRefresh(int pageSize) {
    return getDownloadRecords(1, pageSize);
  }

  goDetail(BuildContext context, Experience exp) {
    DetailScreenArgument args =
        DetailScreenArgument(expId: exp.experienceHashId);
    Navigator.of(context).pushNamed(DetailScreen.routePath, arguments: args);
  }

  @override
  Widget build(BuildContext context) {
    return InfinityList(
      onFetchData: getDownloadRecords,
      onRefresh: handleRefresh,
      itemBuilder: (item) => AppListItem(
        onTap: (String id) => goDetail(context, item),
        bundleIdentifier: item.bundleIdentifier,
        id: item.experienceHashId,
        leadingUrl: item.logoUrl,
        title: item.experienceName,
        subTitle: '${item.downloadDate}',
        createTime: item.createTime,
        size: item.size,
        isInDownloadRecordsTab: true,
        lastDownloadHashId: item.lastDownloadHashId,
        expired: item.expired,
        appScheme: item.appScheme,
      ),
    );
  }
}
