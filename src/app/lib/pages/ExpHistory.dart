import 'package:bkci_app/widgets/BkAppBar.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/models/pageResponseBody.dart';
import 'package:bkci_app/widgets/InfinityList.dart';
import 'package:bkci_app/widgets/detailPage/HistoryListItem.dart';

class ExpHistoryArgument {
  final String expId;
  final String bundleIdentifier;

  ExpHistoryArgument({
    this.expId,
    this.bundleIdentifier,
  });
}

class ExpHistory extends StatefulWidget {
  static const String routePath = '/expHistory';
  final ExpHistoryArgument expArgs;

  ExpHistory({
    this.expArgs,
  });

  _ExpHistoryState createState() => _ExpHistoryState();
}

class _ExpHistoryState extends State<ExpHistory> {
  Future loadData(int page, int pageSize) async {
    final pageResult = await ajax.get(
        '/experience/api/app/experiences/${widget.expArgs.expId}/changeLog',
        queryParameters: {'page': page, 'pageSize': pageSize});
    final result = PageResponseBody.fromJson(pageResult.data);
    final List expList = [];
    final groupExpList = Map();

    List.from(result.records).forEach((e) {
      final Experience exp = Experience.fromJson(e);

      if (groupExpList[exp.getCreateMd] != null) {
        groupExpList[exp.getCreateMd].add(exp);
      } else {
        groupExpList[exp.getCreateMd] = [exp];
      }
    });

    groupExpList.forEach((key, value) {
      expList.add(key);
      expList.addAll(value);
    });

    return [
      expList,
      result.hasNext ?? false,
    ];
  }

  Future onRefresh(int pageSize) async {
    return loadData(1, pageSize);
  }

  Widget itemBuilder(item) {
    if (item is Experience) {
      return HistoryListItem(
        title: item.versionTitle,
        exp: item,
      );
    } else {
      return Container(
        color: Theme.of(context).backgroundColor,
        alignment: Alignment.centerLeft,
        padding: EdgeInsets.only(left: 32.px),
        height: 64.px,
        child: PFText(
          item,
          style: TextStyle(
            color: Colors.black,
            fontSize: 24.px,
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // backgroundColor: Colors.white,
      appBar: BkAppBar(
        title: BkDevopsAppi18n.of(context).$t('expHistory'),
      ),
      body: SafeArea(
        child: InfinityList(
          onFetchData: loadData,
          onRefresh: onRefresh,
          itemBuilder: itemBuilder,
          dividerBuilder: (
            BuildContext context,
            int index,
            item,
            nextItem,
          ) {
            return (item is Experience && nextItem is! String)
                ? Divider(
                    indent: 32.px,
                    height: 1.px,
                  )
                : SizedBox();
          },
        ),
      ),
    );
  }
}
