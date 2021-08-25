import 'package:bkci_app/models/pageResponseBody.dart';
import 'package:bkci_app/models/build.dart';
import 'package:bkci_app/routes/routes.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:flutter/material.dart';
import '../InfinityList.dart';
import 'package:bkci_app/widgets/pipelineListTab/BuildListItem.dart';
import 'package:bkci_app/widgets/Empty.dart';
import 'package:bkci_app/utils/i18n.dart';

class BuildList extends StatefulWidget {
  final String url;
  final String projectId;
  final String pipelineId;
  final String pipelineName;
  final String queryStr;

  BuildList({
    Key key,
    this.url,
    this.projectId,
    this.pipelineId,
    this.queryStr,
    this.pipelineName,
  }) : super(key: key);

  @override
  _BuildListState createState() => _BuildListState();
}

class _BuildListState extends State<BuildList> with RouteAware {
  bool _intervalSync = false;

  @override
  void initState() {
    super.initState();

    _intervalSync = true;
  }

  Future _loadData(int page, int pageSize) async {
    final responses = await ajax
        .get('${widget.url}?page=$page&pageSize=$pageSize${widget.queryStr}');

    final item = PageResponseBody.fromJson(responses.data);
    final List<Build> result = item.records
        .map(
          (e) => Build.fromJson(e),
        )
        .toList();
    return [result, (item.totalPages > page)];
  }

  Future _reflashData(int pageSize) async {
    return _loadData(1, pageSize);
  }

  Widget itemBuilder(item) {
    return BuildListItem(
      buildArgs: BuildItemArgs(
        buildItem: item,
        pipelineName: widget.pipelineName,
        projectId: widget.projectId,
        pipelineId: widget.pipelineId,
      ),
    );
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    routeObserver.subscribe(this, ModalRoute.of(context));
  }

  @override
  void dispose() {
    super.dispose();
    routeObserver.unsubscribe(this);
  }

  @override
  void didPushNext() {
    super.didPushNext();
    setSyncFlag(false);
  }

  @override
  void didPopNext() {
    super.didPopNext();
    setSyncFlag(true);
  }

  void setSyncFlag(bool isSync) {
    if (mounted) {
      setState(() {
        _intervalSync = isSync;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final i18n = BkDevopsAppi18n.of(context).$t;
    return InfinityList(
      key: ValueKey(widget.url),
      intervalSync: _intervalSync,
      interval: Duration(seconds: 6),
      emptyWidget: Empty(
        title: widget.queryStr == ''
            ? i18n('historyEmptyTitle')
            : i18n('historyFilterEmptyTitle'),
        subTitle: widget.queryStr == ''
            ? i18n('historyEmptySubTitle')
            : i18n('historyFilterEmptySubTitle'),
      ),
      onFetchData: _loadData,
      onRefresh: _reflashData,
      itemBuilder: itemBuilder,
      dividerBuilder: (
        BuildContext context,
        int index,
        item,
        nextItem,
      ) {
        return Divider(
          height: 0,
        );
      },
    );
  }
}
