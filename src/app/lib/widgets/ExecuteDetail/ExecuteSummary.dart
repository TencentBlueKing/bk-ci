import 'package:bkci_app/models/ExecuteModel.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/ExpandableText.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/StatusTag.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class ExecuteSummary extends StatelessWidget {
  final ExecuteModel execDetail;

  ExecuteSummary({
    this.execDetail,
  });

  Map<String, String> get summary {
    return {
      'trigger': execDetail.userId,
      'startTime': execDetail.startTime.yMdhm,
      'executeTime': execDetail.totalExecuteTime,
      'material': material(),
      'appVersion':
          execDetail.packageVersion == null || execDetail.packageVersion == ''
              ? '--'
              : execDetail.packageVersion,
      'remark': execDetail.remark ?? '--'
    };
  }

  Widget _itemBuilder(BuildContext context, String key, String value) {
    return Container(
      alignment: Alignment.centerLeft,
      margin: EdgeInsets.only(
        bottom: 20.px,
      ),
      child: Stack(
        children: [
          Positioned(
            left: 0.px,
            child: PFText(
              BkDevopsAppi18n.of(context).$t(key),
              style: TextStyle(
                color: Colors.black,
                fontSize: 24.px,
                height: 34.px / 24.px,
              ),
            ),
          ),
          Padding(
            padding: EdgeInsets.only(
              left: 184.px,
            ),
            child: ExpandableText(
              value,
              maxLines: 1,
              showCollapse: false,
              expanded: false,
              style: TextStyle(
                color: Theme.of(context).secondaryHeaderColor,
                fontSize: 24.px,
                height: 34.px / 24.px,
              ),
            ),
          ),
        ],
      ),
    );
  }

  String material() {
    return execDetail.material != null
        ? execDetail.material
            .map((e) => "${e['aliasName']}@${e['branchName']}")
            .join('\n')
        : '--';
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      alignment: Alignment.centerLeft,
      padding: EdgeInsets.fromLTRB(32.px, 23.px, 32.px, 12.px),
      margin: EdgeInsets.only(bottom: 17.px),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border(
          top: BorderSide(
            color: Theme.of(context).dividerColor,
            width: 1.px,
          ),
          bottom: BorderSide(
            color: Theme.of(context).dividerColor,
            width: 1.px,
          ),
        ),
      ),
      child: Column(
        children: [
          Container(
            alignment: Alignment.centerLeft,
            margin: EdgeInsets.only(bottom: 29.px),
            child: ExpandableText(
              '【#${execDetail.buildNum}】${execDetail.buildMsg}',
              style: TextStyle(
                fontFamily: 'PingFang-Medium',
                fontSize: 28.px,
                color: '#313238'.color,
                height: 40.px / 28.px,
              ),
              customExpandWidget: TextSpan(
                children: [
                  WidgetSpan(
                    alignment: PlaceholderAlignment.bottom,
                    child: StatusTag(
                      status: execDetail.status,
                      icon: execDetail.icon,
                      background: execDetail.statusColor,
                      isLoading: execDetail.isLoading,
                    ),
                  ),
                ],
              ),
              expandWidgetSize: Size(222.px, 36.px),
            ),
          ),
          for (final key in summary.keys)
            _itemBuilder(context, key, summary[key]),
        ],
      ),
    );
  }
}
