import 'dart:collection';
import 'dart:io' show Platform;

import 'package:bkci_app/models/Artifactory.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/AppIcon.dart';
import 'package:bkci_app/widgets/BkAppBar.dart';
import 'package:bkci_app/widgets/BkErrorWidget.dart';
import 'package:bkci_app/widgets/ExecuteDetail/ArtifactDownloadIcon.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/ProgressBar.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter_downloader/flutter_downloader.dart';

class ArtifactoryDetailArgs {
  final Artifactory artifactory;
  final String artifactoryType;
  final String artifactoryPath;
  final String projectId;

  ArtifactoryDetailArgs({
    this.artifactory,
    @required this.projectId,
    @required this.artifactoryPath,
    @required this.artifactoryType,
  });
}

class ArtifactoryDetail extends StatelessWidget {
  static const String routePath = '/artifactoryDetail';
  final ArtifactoryDetailArgs args;

  ArtifactoryDetail({
    this.args,
  });

  final List<List<String>> keyGroups = [
    ['artifactName'],
    [
      'size',
      'artifactDate',
      'platformText',
    ],
    ['pipeline', 'buildNum', 'trigger'],
    ['path'],
  ];

  Future _getArtifactory() async {
    final response = await ajax.get(
      '$ARTIFACTORY_API_PREFIX/${args.projectId}/${args.artifactoryType}/detail',
      queryParameters: {
        'path': args.artifactoryPath,
      },
    );

    final HashMap items = HashMap();
    print(response.data);
    items.addAll({
      'artifactName': response.data['name'],
      'platform': response.data['platform'],
      'platformText': platformMap[response.data['platform']],
      'size': (response.data['size'] as int).mb,
      'artifactDate': ((response.data['createdTime'] as int) * 1000).yMdhm,
      'buildNum': response.data['buildNum'] is int
          ? '#${response.data['buildNum']}'
          : '--',
      'pipeline': response.data['pipelineName'],
      'trigger': response.data['creator'],
      'downloadable': platformMatch(response.data['name']),
      'logoUrl': response.data['logoUrl'],
      'projectName': response.data['projectName'],
      'path': hyphenator(response.data['path'])
    });

    Artifactory artifactory = Artifactory.fromJson(response.data);

    return [items, artifactory];
  }

  Widget _groupBuilder(BuildContext context, List<String> group, values) {
    return Container(
      margin: EdgeInsets.only(bottom: 16.px),
      child: Column(
        children: [
          for (final key in group) _itemBuilder(context, key, values[key]),
        ],
      ),
    );
  }

  Widget _itemBuilder(BuildContext context, String key, String value) {
    return SizedBox(
      child: Column(
        children: [
          Container(
            color: Colors.white,
            padding: EdgeInsets.fromLTRB(32.px, 24.px, 32.px, 24.px),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                PFText(
                  BkDevopsAppi18n.of(context).$t(key),
                  style: TextStyle(
                    color: Colors.black,
                    fontSize: 28.px,
                    height: 40.px / 28.px,
                  ),
                ),
                Expanded(
                  child: Container(
                    margin: EdgeInsets.only(left: 124.px),
                    alignment: Alignment.centerRight,
                    child: PFText(
                      value ?? '--',
                      maxLines: ['artifactName', 'path'].contains(key) ? 5 : 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 28.px,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          Divider(
            indent: 32.px,
            height: 1.px,
          ),
        ],
      ),
    );
  }

  Widget _downloadButtonBuilder(BuildContext context, Artifactory artifactory) {
    return ButtonTheme(
      minWidth: SizeFit.deviceWidth,
      height: 88.px,
      child: ArtifactDownloadIcon(
        artifactory: artifactory,
        projectId: args.projectId,
        entryBuilder: (
          BuildContext context,
          data,
          handler,
          ArtifactoryInfo value,
        ) {
          return Container(
            height: 86.px,
            margin: EdgeInsets.all(32.px),
            child: Platform.isAndroid &&
                    [
                      DownloadTaskStatus.running,
                      DownloadTaskStatus.paused,
                      DownloadTaskStatus.enqueued,
                    ].contains(value.status)
                ? buildProgress(context, value, handler)
                : ElevatedButton(
                    onPressed: handler,
                    style: ElevatedButton.styleFrom(
                      minimumSize: Size(double.infinity, 88.px),
                    ),
                    child: PFText(
                      BkDevopsAppi18n.of(context).$t(data[1]),
                      style: TextStyle(
                        fontSize: 30.px,
                        color: Colors.white,
                      ),
                    ),
                  ),
          );
        },
      ),
    );
  }

  Widget buildProgress(
    BuildContext context,
    ArtifactoryInfo value,
    Function handler,
  ) {
    return GestureDetector(
      onTap: handler,
      child: ClipPath(
        child: Container(
          decoration: ShapeDecoration(
            color: Colors.white,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(6.px),
              side: BorderSide(
                width: 1.px,
                color: Theme.of(context).primaryColor,
                style: BorderStyle.solid,
              ),
            ),
          ),
          child: ProgressBar(
            value: value.progress,
            isPause: value.status == DownloadTaskStatus.paused,
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: BkAppBar(
        shadowColor: Colors.transparent,
        title: BkDevopsAppi18n.of(context).$t('artifactoyDetailTitle'),
      ),
      body: Container(
        decoration: BoxDecoration(
          border: Border(
            top: BorderSide(
              color: Theme.of(context).dividerColor,
              width: 1.px,
            ),
          ),
        ),
        child: FutureBuilder(
          key: ValueKey(args.projectId),
          future: _getArtifactory(),
          builder: (context, snapshot) {
            if (snapshot.hasError) {
              return BkErrorWidget(
                withAppBar: false,
                flutterErrorDetails: snapshot.error,
                authTitle:
                    BkDevopsAppi18n.of(context).$t('noAccessArtifactoryTips'),
                authDesc:
                    BkDevopsAppi18n.of(context).$t('applyArtifactoryTips'),
              );
            }
            if (snapshot.hasData) {
              final detail = snapshot.data[0];
              return SingleChildScrollView(
                child: Column(
                  children: [
                    Container(
                      margin: EdgeInsets.only(
                        top: 36.px,
                        bottom: 23.px,
                      ),
                      child: AppIcon(url: detail['logoUrl']),
                    ),
                    Container(
                      margin: EdgeInsets.only(bottom: 42.px),
                      child: PFMediumText(
                        detail['projectName'],
                        style: TextStyle(
                          fontSize: 36.px,
                          color: Colors.black,
                        ),
                      ),
                    ),
                    Column(
                      children: [
                        for (final group in keyGroups)
                          _groupBuilder(context, group, detail),
                      ],
                    ),
                    if (detail['downloadable'])
                      _downloadButtonBuilder(context, snapshot.data[1]),
                  ],
                ),
              );
            }
            return SizedBox(
              height: 666.px,
              child: Center(
                child: CircularProgressIndicator(
                  backgroundColor: Theme.of(context).primaryColor,
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}
