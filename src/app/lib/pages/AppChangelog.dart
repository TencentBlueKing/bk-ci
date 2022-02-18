import 'package:bkci_app/models/Appversion.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/BkAppBar.dart';
import 'package:bkci_app/widgets/InfinityList.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class AppChangelog extends StatelessWidget {
  static const String routePath = '/changelog';
  Widget _itemBuilder(item) {
    final int releaseTime = item.releaseDate;
    return Container(
      margin: EdgeInsets.only(
        left: 32.px,
        right: 32.px,
      ),
      padding: EdgeInsets.only(
        top: 32.px,
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              PFMediumText(
                item.versionId,
                style: TextStyle(
                  color: '#333C48'.color,
                  fontSize: 28.px,
                ),
              ),
              PFText(
                releaseTime.yyMMdd,
                style: TextStyle(
                  color: '#A3ADBB'.color,
                  fontSize: 28.px,
                ),
              ),
            ],
          ),
          Container(
            alignment: Alignment.centerLeft,
            margin: EdgeInsets.only(top: 14.px),
            child: PFText(
              item.releaseContent,
              style: TextStyle(
                color: '#333C48'.color,
              ),
            ),
          )
        ],
      ),
    );
  }

  Future getAppChangelog() async {
    final response = await ajax.get(
      '/support/api/app/app/version',
      queryParameters: {
        'channelType': Storage.platfrom,
      },
    );
    final List<Appversion> result = [];

    response.data.forEach((ele) {
      final Appversion version = Appversion.fromJson(ele);
      result.add(version);
    });

    return [
      result,
      false,
    ];
  }

  Future fetchChangelog(int page, int pageSize) => getAppChangelog();

  Future refreshChangelog(int page) => getAppChangelog();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: BkAppBar(
        title: BkDevopsAppi18n.of(context).$t('about'),
      ),
      body: Container(
        color: Colors.white,
        child: InfinityList(
          onFetchData: fetchChangelog,
          onRefresh: refreshChangelog,
          itemBuilder: _itemBuilder,
          dividerBuilder: (
            BuildContext context,
            int index,
            item,
            nextItem,
          ) =>
              Divider(
            indent: 32.px,
            endIndent: 32.px,
          ),
        ),
      ),
    );
  }
}
