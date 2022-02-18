import 'package:ansi_up/ansi_up.dart';
import 'package:bkci_app/models/LogModel.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/rendering.dart';

const MAX_LENGTH = 2000;

class BuildLog extends StatefulWidget {
  final String projectId;
  final String pipelineId;
  final String buildId;

  BuildLog({
    this.projectId,
    this.pipelineId,
    this.buildId,
  });

  @override
  _BuildLogState createState() => _BuildLogState();
}

class _BuildLogState extends State<BuildLog> {
  final textColor = Colors.white;
  final lineNoColor = '#A3ADBB'.color;
  final activeTimeColor = '#3883DA'.color;

  final ScrollController _scrollController = new ScrollController();
  final pageSize = 200;
  String logUrlPrefix;
  List list = [];
  int page = 1;
  bool showTime = false;
  bool _loading = false;
  bool isLoadingMore = false;
  bool hasMore = false;
  bool finished = true;
  bool showHeader = false;
  String _emptyLogI18nLabel;

  @override
  void initState() {
    super.initState();
    logUrlPrefix =
        '/log/api/app/logs/${widget.projectId}/${widget.pipelineId}/${widget.buildId}';
    _loading = true;
    _scrollController.addListener(scrollToTop);
    Future.delayed(Duration.zero, getLogs);
  }

  scrollToTop() {
    FocusScope.of(context).requestFocus(FocusNode());
    final bool isReachTop = _scrollController.position.pixels >
        _scrollController.position.maxScrollExtent * 0.98;
    if (isReachTop != showHeader) {
      setWidgetState(() {
        showHeader = isReachTop ? list.length >= MAX_LENGTH : false;
      });
    }

    if (_scrollController.position.pixels ==
            (_scrollController.position.maxScrollExtent) &&
        hasMore &&
        list.length < MAX_LENGTH) {
      loadMore();
    }
  }

  List<TextSpan> ansiParse(String str) {
    AnsiUp ansi = AnsiUp();
    Iterable<StyledText> result = decodeAnsiColorEscapeCodes(str, ansi);

    List<TextSpan> spans = [];
    for (final entry in result) {
      Color fgColor;
      Color bgColor;
      if (entry.fgColor != null) {
        fgColor = Color.fromRGBO(
            entry.fgColor[0], entry.fgColor[1], entry.fgColor[2], 1);
      }

      if (entry.bgColor != null) {
        bgColor = Color.fromRGBO(
            entry.bgColor[0], entry.bgColor[1], entry.bgColor[2], 1);
      }

      TextStyle spanStyle = TextStyle(
        color: fgColor ?? textColor,
        backgroundColor: bgColor,
        fontWeight: entry.bold ? FontWeight.bold : FontWeight.normal,
      );

      spans.add(
        TextSpan(
          text: entry.text,
          style: spanStyle,
        ),
      );
    }
    return spans;
  }

  Future<List> getLogs() async {
    final res = await ajax.get('$logUrlPrefix/bottom?size=$pageSize');
    if (res.data == null) return [];

    final LogModel logs = LogModel.fromJson(res.data);

    List result = [];

    logs.logs.forEach((e) {
      result.add({
        'children': ansiParse(e.message),
        'timestamp': e.timestamp.yMdhm,
        'lineNo': e.lineNo + 1,
      });
    });
    setWidgetState(() {
      _emptyLogI18nLabel = logs.statusLabel;
      list = result.reversed.toList();
      hasMore = logs.hasMore ?? false;
      finished = logs.finished ?? true;
      _loading = false;
    });

    return result;
  }

  Future<List> loadMore() async {
    setWidgetState(() {
      isLoadingMore = true;
    });

    final int end = list.last['lineNo'] - 1;
    final res = await ajax.get('$logUrlPrefix/before?end=$end&size=$pageSize');
    if (res.data == null) return [];
    final LogModel logs = LogModel.fromJson(res.data);

    List result = [];

    logs.logs.forEach((e) {
      result.add({
        'children': ansiParse(e.message),
        'timestamp': e.timestamp.yMdhm,
        'lineNo': e.lineNo + 1,
      });
    });

    setWidgetState(() {
      list = [
        ...list,
        ...(result.reversed),
      ].toList();
      hasMore = logs.hasMore ?? false;
      isLoadingMore = false;
    });

    return result;
  }

  setWidgetState(cb) {
    if (mounted) {
      setState(cb);
    }
  }

  TextSpan _buildTimeSpan(log) {
    return TextSpan(
      text: '【${log["timestamp"]}】',
      style: TextStyle(
        color: activeTimeColor,
      ),
    );
  }

  Widget _buildLogLine(context, logLine, index) {
    List<TextSpan> children = logLine['children'];

    if (showTime) {
      children = [];
      children.addAll(logLine['children']);
      children.insert(0, _buildTimeSpan(logLine));
    }

    return Text.rich(
      TextSpan(
        text: logLine['lineNo'].toString().padRight(8),
        style: TextStyle(
          color: lineNoColor,
          fontFamily: 'PingFang',
        ),
        children: children,
      ),
    );
  }

  _toggleTime() {
    setState(() {
      showTime = !showTime;
    });
  }

  Widget emptyWidget(BuildContext context) {
    return Center(
      child: PFText(
        BkDevopsAppi18n.of(context).$t(_emptyLogI18nLabel),
        style: TextStyle(
          color: textColor,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final timeColor = showTime ? activeTimeColor : Colors.white;

    return Container(
      color: '#222222'.color,
      child: SafeArea(
        top: false,
        child: _loading
            ? Center(
                child: CircularProgressIndicator(),
              )
            : Column(
                children: [
                  if (list.length > 0)
                    Container(
                      height: 88.px,
                      child: Row(
                        children: [
                          TextButton(
                            onPressed: _toggleTime,
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.start,
                              children: [
                                Icon(
                                  BkIcons.eye,
                                  color: timeColor,
                                  size: 36.px,
                                ),
                                Container(
                                  margin: EdgeInsets.only(
                                    left: 10.px,
                                    right: 30.px,
                                  ),
                                  child: PFMediumText(
                                    BkDevopsAppi18n.of(context).$t('time'),
                                    style: TextStyle(
                                      color: timeColor,
                                      fontSize: 30.px,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                          if (showHeader)
                            Expanded(
                              child: PFMediumText(
                                BkDevopsAppi18n.of(context).$t('moreLogTips'),
                                textAlign: TextAlign.center,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  color: Colors.white,
                                  fontSize: 30.px,
                                ),
                              ),
                            ),
                        ],
                      ),
                    ),
                  list.length > 0
                      ? Flexible(
                          child: ListView.builder(
                            padding: EdgeInsets.all(0),
                            reverse: true,
                            physics: const BouncingScrollPhysics(
                              parent: AlwaysScrollableScrollPhysics(),
                            ),
                            itemCount: list.length,
                            controller: _scrollController,
                            itemBuilder: (context, index) =>
                                _buildLogLine(context, list[index], index),
                          ),
                        )
                      : Expanded(child: emptyWidget(context)),
                ],
              ),
      ),
    );
  }
}
