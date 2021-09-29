import 'dart:io';
import 'dart:math';

import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/models/Artifactory.dart';
import 'package:bkci_app/models/ExecuteModel.dart';
import 'package:bkci_app/models/ShareArgs.dart';
import 'package:bkci_app/pages/ArtifactoryDetail.dart';
import 'package:bkci_app/providers/DownloadProvider.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/ExecuteDetail/ArtifactDownloadIcon.dart';

import 'package:bkci_app/widgets/InfinityList.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/ProgressBar.dart';
import 'package:bkci_app/widgets/SharePopup.dart';
import 'package:flutter/material.dart';

import 'package:bkci_app/pages/CreateExp.dart';
import 'package:flutter_downloader/flutter_downloader.dart';
import 'package:provider/provider.dart';
import 'package:bkci_app/widgets/Empty.dart';

class ArtifactoryTab extends StatelessWidget {
  final ExecuteModel args;

  ArtifactoryTab({
    this.args,
  });

  _shareArtifact(BuildContext context, Artifactory item) {
    ShareArgs shareArgs = ShareArgs(
      isArtifact: true,
      kind: 'webpage',
      title: '${args.buildMsg}(#${args.buildNum})',
      description: item.fullName,
      previewImageUrl: item.logoUrl,
      packageName: item.name,
      url:
          '$SHARE_URL_PREFIX/artifactoryDetail/?flag=artifactoryDetail&projectId=${args.projectId}&artifactoryType=${item.artifactoryType}&artifactoryPath=${encodePath(item.path)}',
    );
    SharePopup(shareArgs: shareArgs).show(context);
  }

  _goArtifactoryDetail(BuildContext context, Artifactory artifactory) {
    Navigator.of(context).pushNamed(
      ArtifactoryDetail.routePath,
      arguments: ArtifactoryDetailArgs(
        artifactory: artifactory,
        artifactoryPath: artifactory.path,
        artifactoryType: artifactory.artifactoryType,
        projectId: args.projectId,
      ),
    );
  }

  Future getArtifactoies(int page, int pageSize) async {
    final response = await ajax.get(
        '$ARTIFACTORY_API_PREFIX/${args.projectId}/${args.pipelineId}/${args.buildId}/fileList');
    final List<Artifactory> result = [];
    response.data.forEach((ele) {
      result.add(Artifactory.fromJson(ele));
    });

    return [
      result,
      false,
    ];
  }

  Future onRefresh(int pageSize) async {
    return getArtifactoies(1, pageSize);
  }

  Widget _itemBuilder(BuildContext context, Artifactory item) {
    final primaryColor = Theme.of(context).primaryColor;
    List actions = [];
    if (item.isPkg) {
      actions = [
        {
          'icon': BkIcons.conversion,
          'handler': () {
            CreateExpArgument expArgs = CreateExpArgument(
              projectId: args.projectId,
              artifact: item.toJson(),
            );
            Navigator.of(context)
                .pushNamed(CreateExp.routePath, arguments: expArgs);
          },
        },
        {
          'icon': BkIcons.share,
          'handler': () {
            _shareArtifact(context, item);
          }
        },
      ];
    }

    return GestureDetector(
      onTap: item.isPkg
          ? () {
              _goArtifactoryDetail(
                context,
                item,
              );
            }
          : null,
      child: Container(
        color: Colors.white,
        padding: EdgeInsets.fromLTRB(32.px, 24.px, 32.px, 0.px),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Container(
              alignment: Alignment.centerLeft,
              child: PFMediumText(
                item.name,
                overflow: TextOverflow.ellipsis,
                maxLines: 5,
                style: TextStyle(
                  color: Colors.black,
                  fontSize: 28.px,
                  height: 40.px / 28.px,
                ),
              ),
            ),
            Container(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Expanded(
                    child: _renderProgress(item),
                  ),
                  Container(
                    child: Row(
                      children: [
                        for (final action in actions)
                          IconButton(
                            onPressed: action['handler'],
                            icon: Icon(action['icon']),
                            iconSize: 44.px,
                            color: primaryColor,
                          ),
                        if (item.isPkg)
                          item.downloadable
                              ? ArtifactDownloadIcon(
                                  projectId: args.projectId,
                                  artifactory: item,
                                  iconSize: 44.px,
                                  iconColor: primaryColor,
                                )
                              : IconButton(
                                  onPressed: null,
                                  icon: Icon(BkIcons.download),
                                  iconSize: 44.px,
                                  color: '#979BA5'.color,
                                ),
                      ],
                    ),
                  )
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget sizeWidgetBuilder(int size) {
    return Container(
      alignment: Alignment.centerLeft,
      child: PFText(
        size.mb,
        style: TextStyle(
          color: '#979BA5'.color,
          fontSize: 24.px,
          height: 34.px / 24.px,
        ),
      ),
    );
  }

  Widget _renderProgress(Artifactory artifactory) {
    TextStyle style = TextStyle(
      fontSize: 24.px,
      color: '#979BA5'.color,
    );

    if (Platform.isIOS) {
      return sizeWidgetBuilder(artifactory.size);
    }
    return Selector<DownloadProvider, List>(
      selector: (context, provider) {
        int progress = 0;
        DownloadTaskStatus status;
        DownloadJob task;
        if (provider != null && provider.downloadJobs.isNotEmpty) {
          task = provider.downloadJobs.find<DownloadJob>(
            (DownloadJob task) {
              return (task.jobInfo != null &&
                  (task.jobInfo.expId == artifactory.identify ||
                      task.jobInfo.expId == artifactory.uniqueId));
            },
          );
        }
        if (task != null) {
          progress = task.progress > 0 ? min(task.progress, 100) : 0;
          status = task.status;
        }

        return [progress, status];
      },
      builder: (context, value, child) => Container(
        margin: EdgeInsets.only(right: 32.px),
        child: value[1] == null
            ? sizeWidgetBuilder(artifactory.size)
            : Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      PFText(
                        '${(artifactory.size * value[0] ~/ 100).mb}/${artifactory.size.mb}',
                        style: style,
                      ),
                      PFText(
                        BkDevopsAppi18n.of(context)
                            .$t(downloadStatusLabel[value[1].value]),
                        style: style,
                      ),
                    ],
                  ),
                  Container(
                    height: 4.px,
                    child: ProgressBar(
                      value: value[0],
                      bgColor: '#D8D8D8'.color,
                    ),
                  ),
                ],
              ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final i18n = BkDevopsAppi18n.of(context).$t;
    return Container(
      child: InfinityList(
        itemBuilder: (item) => _itemBuilder(context, item),
        onFetchData: getArtifactoies,
        onRefresh: onRefresh,
        emptyWidget: Empty(
          title: ['RUNNING', 'QUEUE'].indexOf(args.status) >= 0
              ? i18n('runningArtifactEmptyTitle')
              : i18n('artifactEmptyTitle'),
          subTitle: ['RUNNING', 'QUEUE'].indexOf(args.status) >= 0
              ? i18n('runningArtifactEmptySubTitle')
              : '',
        ),
        dividerBuilder: (
          BuildContext context,
          int index,
          item,
          nextItem,
        ) =>
            Divider(
          indent: 32.px,
          height: 1.px,
        ),
      ),
    );
  }
}
