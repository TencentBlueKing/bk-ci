import 'package:bkci_app/models/pageResponseBody.dart';
import 'package:bkci_app/models/pipeline.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import '../InfinityList.dart';
import 'package:bkci_app/widgets/pipelineListTab/ListItem.dart';
import 'package:bkci_app/widgets/Empty.dart';
import 'package:bkci_app/utils/i18n.dart';

class PipelineList extends StatefulWidget {
  final String projectId;
  final String viewId;

  PipelineList({
    this.viewId,
    this.projectId,
  });

  @override
  _PipelineListState createState() => _PipelineListState();
}

class _PipelineListState extends State<PipelineList> {
  Future _loadData(int page, int pageSize) async {
    final responses = await ajax.get(
        '/process/api/app/pipelineViews/projects/${widget.projectId}/listViewPipelines/v2?viewId=${widget.viewId}&page=$page&pageSize=$pageSize&sortType=CREATE_TIME');

    final item = PageResponseBody.fromJson(responses.data);
    final List<Pipeline> result =
        item.records.map((e) => Pipeline.fromJson(e)).toList();

    return [
      result,
      item.hasNext ?? false,
    ];
  }

  Future _reflashData(int pageSize) async {
    return _loadData(1, pageSize);
  }

  Widget itemBuilder(item) {
    return ListItem(
      title: item.pipelineName,
      subTitle: item.subTitle,
      status: item.getStatus,
      statusColor: item.statusColor,
      statusIcon: item.icon,
      projectId: item.projectId,
      pipelineId: item.pipelineId,
      hasCollect: item.hasCollect,
      canManualStartup: item.canManualStartup,
    );
  }

  @override
  Widget build(BuildContext context) {
    final i18n = BkDevopsAppi18n.of(context).$t;
    return Container(
      child: (InfinityList(
        key: ValueKey('${widget.projectId}${widget.viewId}'),
        onFetchData: _loadData,
        onRefresh: _reflashData,
        emptyWidget: Empty(
          title: widget.viewId == 'myPipeline'
              ? i18n('pipelineEmptyTitle')
              : (widget.viewId == 'collect'
                  ? i18n('collectEmptyTitle')
                  : i18n('viewEmptyTitle')),
          subTitle: widget.viewId == 'myPipeline'
              ? i18n('pipelineEmptySubTitle')
              : (widget.viewId == 'collect'
                  ? i18n('collectEmptySubTitle')
                  : i18n('viewEmptySubTitle')),
        ),
        itemBuilder: itemBuilder,
        dividerBuilder: (
          BuildContext context,
          int index,
          item,
          nextItem,
        ) {
          return Divider(
            indent: 40.px,
            endIndent: 32.px,
            height: 1.px,
          );
        },
      )),
    );
  }
}
