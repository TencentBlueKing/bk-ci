import 'package:bkci_app/models/ExecuteModel.dart';
import 'package:bkci_app/pages/ExecuteDetailPage.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/statusIcon.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/models/build.dart';

class BuildItemArgs {
  final Build buildItem;
  final String projectId;
  final String pipelineId;
  final String pipelineName;

  BuildItemArgs({
    this.buildItem,
    this.projectId,
    this.pipelineId,
    this.pipelineName,
  });
}

class BuildListItem extends StatefulWidget {
  final BuildItemArgs buildArgs;

  BuildListItem({
    this.buildArgs,
  });

  _BuildListItemState createState() => _BuildListItemState();
}

class _BuildListItemState extends State<BuildListItem> {
  void showAppVersion(versions) {
    if (versions.length < 1) return null;
    showDialog(
      context: context,
      builder: (BuildContext context) {
        var child = Column(
          children: <Widget>[
            ListTile(
              title: Text(
                "App Versions",
              ),
            ),
            Expanded(
              child: ListView.separated(
                itemCount: versions.length,
                separatorBuilder: (BuildContext context, int index) {
                  return Divider(
                    height: 1.px,
                  );
                },
                itemBuilder: (BuildContext context, int index) {
                  return ListTile(
                    title: Text(
                      versions[index],
                      style: TextStyle(fontSize: 28.px),
                    ),
                  );
                },
              ),
            ),
          ],
        );
        return Dialog(
          child: Container(
            height: 600.px,
            child: child,
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final item = widget.buildArgs.buildItem;
    return GestureDetector(
      onTap: () {
        ExecuteDetailPageArgs args = ExecuteDetailPageArgs(
          projectId: widget.buildArgs.projectId,
          pipelineId: widget.buildArgs.pipelineId,
          buildId: item.id,
          initialIndex: ['RUNNING', 'QUEUE'].indexOf(item.status) >= 0 ? 2 : 0,
          initModel: ExecuteModel(
            buildId: item.id,
            buildMsg: item.buildMsg,
            userId: item.userId,
            trigger: item.trigger,
            status: item.status,
            packageVersion: item.getAppVersions.join('；'),
            pipelineVersion: item.pipelineVersion,
            pipelineId: widget.buildArgs.pipelineId,
            projectId: widget.buildArgs.projectId,
            startTime: item.startTime,
            endTime: item.endTime,
            buildNum: item.buildNum,
            material: item.material,
            executeTime: item.executeTime,
            pipelineName: widget.buildArgs.pipelineName,
          ),
        );
        Navigator.of(context).pushNamed(
          ExecuteDetailPage.routePath,
          arguments: args,
        );
      },
      child: Container(
        margin: EdgeInsets.only(bottom: 20.px),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16.px),
        ),
        // height: 232.px,
        padding: EdgeInsets.fromLTRB(32.px, 24.px, 32.px, 32.px),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            PFMediumText(
              item.buildMsg ?? '',
              overflow: TextOverflow.ellipsis,
              maxLines: 2,
              style: TextStyle(
                fontSize: 28.px,
              ),
            ),
            Padding(
              padding: EdgeInsets.fromLTRB(0, 8.px, 0, 16.px),
              child: PFText(
                '#${item.buildNum}',
                style: TextStyle(
                  color: Colors.black,
                  fontSize: 24.px,
                ),
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    StatusIcon(
                      icon: item.icon,
                      iconColor: item.statusColor,
                      isLoading: item.isLoading,
                    ),
                    InkWell(
                      onTap: () {
                        showAppVersion(item.getAppVersions);
                      },
                      child: Container(
                        margin: EdgeInsets.only(left: 26.px),
                        padding: EdgeInsets.only(left: 20.px, right: 20.px),
                        constraints: BoxConstraints(
                          maxWidth: 200.px,
                        ),
                        height: 38.px,
                        decoration: BoxDecoration(
                          color: '#F5F6FA'.color,
                          borderRadius: BorderRadius.circular(19.px),
                        ),
                        child: Text(
                          item.getAppVersions.length > 0
                              ? item.getAppVersions.first
                              : '--',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 24.px,
                            color: '#979BA5'.color,
                          ),
                        ),
                      ),
                    ),
                  ].where((e) => e != null).toList(),
                ),
                Row(
                  children: [
                    PFText(
                      item.getTimeUser,
                      style: TextStyle(fontSize: 24.px, color: '#979BA5'.color),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
