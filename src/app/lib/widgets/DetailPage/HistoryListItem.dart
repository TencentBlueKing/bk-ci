import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/providers/BkGlobalStateProvider.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/DownloadButton.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/pages/DetailScreen.dart';
import 'package:provider/provider.dart';

class HistoryListItem extends StatelessWidget {
  final String title;
  final Experience exp;

  final textStyle = TextStyle(
    fontSize: 24.px,
    color: '#979BA5'.color,
  );

  HistoryListItem({
    this.title,
    this.exp,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.only(
        left: 32.px,
        top: 4.px,
        bottom: 13.px,
        right: 27.px,
      ),
      tileColor: Colors.white,
      title: PFMediumText(
        title ?? '',
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontSize: 28.px,
          color: Colors.black,
        ),
      ),
      subtitle: Padding(
        padding: EdgeInsets.symmetric(vertical: 8.px),
        child: PFText(
          'v${exp.version}      ${exp.formatSize}',
          style: textStyle,
        ),
      ),
      trailing: Container(
        padding: EdgeInsets.all(0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            DownloadButton(
              logoUrl: exp.logoUrl,
              bundleIdentifier: exp.bundleIdentifier,
              createTime: exp.createTime,
              size: exp.size,
              expId: exp.experienceHashId,
              name: title,
              expired: exp.expired,
              lastDownloadHashId: exp.lastDownloadHashId,
              appScheme: exp.appScheme,
            ),
            Selector<BkGlobalStateProvider, bool>(
              selector: (context, globalStateProvider) {
                return globalStateProvider.isLastDownloadExpId(
                  exp.bundleIdentifier,
                  exp.experienceHashId,
                  exp.lastDownloadHashId,
                );
              },
              builder: (BuildContext context, bool isLastDownload,
                      Widget child) =>
                  isLastDownload
                      ? Container(
                          padding: EdgeInsets.only(top: 4.px),
                          child: PFText(
                            BkDevopsAppi18n.of(context).$t('lastDownloadFlag'),
                            style: TextStyle(
                              fontSize: 22.px,
                              color: '#979BA5'.color,
                            ),
                          ),
                        )
                      : SizedBox(),
            ),
          ],
        ),
      ),
      onTap: () {
        Navigator.of(context).pushNamed(
          DetailScreen.routePath,
          arguments: DetailScreenArgument(
            expId: exp.experienceHashId,
            fromHistory: true,
          ),
        );
      },
    );
  }
}
