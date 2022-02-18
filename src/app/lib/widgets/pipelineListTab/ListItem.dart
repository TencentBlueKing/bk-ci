import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/pages/BuildHistory.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/statusIcon.dart';

class ListItem extends StatefulWidget {
  final String title;
  final String subTitle;
  final String status;
  final Color statusColor;
  final IconData statusIcon;
  final String projectId;
  final String pipelineId;
  final bool hasCollect;
  final bool canManualStartup;

  ListItem({
    this.title,
    this.subTitle,
    this.status,
    this.statusColor,
    this.statusIcon,
    this.projectId,
    this.pipelineId,
    this.hasCollect,
    this.canManualStartup,
  });

  _ListItemState createState() => _ListItemState();
}

class _ListItemState extends State<ListItem> {
  bool isCollect;

  Future handleCollect() async {
    final result = await ajax.post(
        '/process/api/app/pipeline/${widget.projectId}/${widget.pipelineId}/collect?isCollect=${!isCollect}');
    if (result.data == true) {
      setState(() {
        isCollect = !isCollect;
      });
    }
  }

  @override
  void initState() {
    super.initState();
    isCollect = widget.hasCollect;
  }

  @override
  Widget build(BuildContext context) {
    final SlidableController slidableController = SlidableController();
    return Slidable(
      key: Key(widget.title),
      controller: slidableController,
      actionPane: SlidableStrechActionPane(),
      secondaryActions: <Widget>[
        IconSlideAction(
          caption: BkDevopsAppi18n.of(context)
              .$t(isCollect ? 'uncollect' : 'collect'),
          color: '#979BA5'.color,
          icon: isCollect ? BkIcons.unfavoriteFill : BkIcons.collectFill,
          onTap: handleCollect,
        ),
      ],
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        child: Container(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Container(
                width: 40.px,
                child: isCollect == true
                    ? Icon(
                        BkIcons.collectFill,
                        size: 28.px,
                        color: Colors.orange,
                      )
                    : null,
              ),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      padding: EdgeInsets.fromLTRB(0, 24.px, 10.px, 10.px),
                      constraints: BoxConstraints(maxWidth: 560.px),
                      child: Text(
                        widget.title ?? '',
                        overflow: TextOverflow.ellipsis,
                        maxLines: 2,
                        style: TextStyle(
                          fontSize: 28.px,
                          color: Colors.black,
                          fontFamily: 'PingFang-medium',
                        ),
                      ),
                    ),
                    Container(
                      padding: EdgeInsets.only(bottom: 27.px),
                      child: PFText(
                        widget.subTitle ?? '',
                        style: TextStyle(
                          fontSize: 24.px,
                          color: '#979BA5'.color,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                padding: EdgeInsets.only(right: 38.px),
                child: StatusIcon(
                  icon: widget.statusIcon,
                  iconColor: widget.statusColor,
                  isLoading: widget.status == 'RUNNING',
                ),
              ),
            ],
          ),
        ),
        onTap: () {
          Navigator.of(context).pushNamed(
            BuildHistory.routePath,
            arguments: BuildHistoryArgument(
              projectId: widget.projectId,
              pipelineId: widget.pipelineId,
              pipelineName: widget.title,
              canTrigger: widget.canManualStartup,
            ),
          );
        },
      ),
    );
  }
}
