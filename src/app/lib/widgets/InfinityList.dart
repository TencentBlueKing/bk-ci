import 'dart:async';

import 'package:bkci_app/providers/BkGlobalStateProvider.dart';
import 'package:bkci_app/widgets/CupertinoPullRefresh.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:provider/provider.dart';

class InfinityList<T> extends StatefulWidget {
  final bool intervalSync;
  final Duration interval;
  final int pageSize;
  final Future Function(int page, int pageSize) onFetchData;

  final Future Function(int pageSize) onRefresh;
  final Widget Function(T item) itemBuilder;
  final Widget Function(
    BuildContext context,
    int index,
    T item,
    T nextItem,
  ) dividerBuilder;
  final Widget emptyWidget;

  InfinityList({
    Key key,
    this.onFetchData,
    this.pageSize,
    this.intervalSync = false,
    this.interval,
    this.onRefresh,
    this.itemBuilder,
    this.dividerBuilder,
    this.emptyWidget,
  }) : super(key: key);

  @override
  _InfinityListState<T> createState() => _InfinityListState<T>();
}

class _InfinityListState<T> extends State<InfinityList> {
  bool showFooter = false;
  bool isLoadingMore = false;
  bool isinit = false;
  bool hasNext = false;
  bool isRefresh = false;
  Timer timer;
  List<T> list = [];
  int page = 1;
  int pageSize;

  setWidgetState(setFunc) {
    if (mounted) {
      setState(setFunc);
    }
  }

  ScrollController _scrollController = new ScrollController();
  @override
  void initState() {
    super.initState();
    pageSize = widget.pageSize ?? 24;
    _scrollController.addListener(scrollToEnd);
    Future.delayed(Duration.zero, () {
      initFetch();
    });
  }

  scrollToEnd() {
    final bool isReachBottom = _scrollController.position.pixels >=
        (_scrollController.position.maxScrollExtent * 0.98);
    if (isReachBottom != showFooter) {
      setWidgetState(() {
        showFooter = isReachBottom;
      });
    }
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent) {
      _loadMore();
    }
  }

  Future _loadMore() async {
    try {
      if (hasNext && !isLoadingMore && !isRefresh) {
        setWidgetState(() {
          isLoadingMore = true;
        });
        final nextPage = page + 1;
        final response = await widget.onFetchData(nextPage, pageSize);
        setWidgetState(() {
          isLoadingMore = false;
          list.addAll(response[0]);
          hasNext = response[1];
          page = nextPage;
          isinit = false;
        });
      }
    } catch (e) {
      // toast(e.message);
      print(e);
      setWidgetState(() {
        isLoadingMore = false;
      });
    }
  }

  Future initFetch() async {
    bool _hasNext = false;
    List result = [];
    try {
      if (!isLoadingMore && !isRefresh && !isinit) {
        setWidgetState(() {
          isinit = true;
        });

        final response = await widget.onFetchData(page, pageSize);
        result = response[0];
        _hasNext = response[1];

        if (widget.intervalSync) {
          Future.delayed(Duration(seconds: 2), () {
            _clearTimer();
            _syncList();
          });
        }
      }
    } catch (e) {
      print(e);
    } finally {
      setWidgetState(() {
        list = result;
        hasNext = _hasNext;
        isinit = false;
      });
    }
  }

  Future _onRefresh() async {
    try {
      setWidgetState(() {
        isRefresh = true;
      });

      final response = await widget.onRefresh(pageSize);
      setWidgetState(() {
        isRefresh = false;
        page = 1;
        list = response[0];
        hasNext = response[1];
      });
    } catch (e) {
      setWidgetState(() {
        isRefresh = false;
      });
    }
  }

  _syncList() {
    if (mounted && timer == null) {
      timer = Timer.periodic(widget.interval, (_) async {
        // TODO: ugly;
        final bool isAppForeground = Provider.of<BkGlobalStateProvider>(
          context,
          listen: false,
        ).isAppForeground;
        if (!isLoadingMore && !isRefresh && isAppForeground) {
          final response = await widget.onFetchData(1, list.length);
          if (list.length <= response[0].length) {
            setWidgetState(() {
              list = response[0];
            });
          }
        }
      });
    }
  }

  @override
  void didUpdateWidget(InfinityList oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (widget.intervalSync) {
      _syncList();
    } else {
      _clearTimer();
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    if (widget.intervalSync) {
      _syncList();
    } else {
      _clearTimer();
    }
  }

  _clearTimer() {
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
  }

  @override
  void deactivate() {
    super.deactivate();
    _clearTimer();
  }

  @override
  void dispose() {
    _scrollController.removeListener(scrollToEnd);
    _clearTimer();
    super.dispose();
  }

  Widget _loadMoreFooter(BuildContext context) {
    if (list.isEmpty || (!hasNext && page == 1)) return null;
    String label = 'pullUpLoadMore';
    final TextStyle textStyle = TextStyle(
      color: '#979BA5'.color,
      fontSize: 28.px,
    );
    List<Widget> children = [];
    if (hasNext && isLoadingMore) {
      label = 'loadingMore';
      children.add(Padding(
        padding: EdgeInsets.only(right: 16.px),
        child: CupertinoActivityIndicator(),
      ));
    } else if (!hasNext && page > 1) {
      label = 'noMoreTip';
    }

    children.add(Text(
      BkDevopsAppi18n.of(context).$t(label),
      style: textStyle,
    ));

    return Container(
      height: 60.px,
      alignment: Alignment.center,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: children,
      ),
    );
  }

  Widget _itemBuilder(BuildContext context, T item, index) {
    return widget.itemBuilder(item);
  }

  @override
  Widget build(BuildContext context) {
    if (isinit) {
      return Center(
        child: CircularProgressIndicator(
          backgroundColor: Theme.of(context).primaryColor,
        ),
      );
    }
    return CupertinoPullRefresh<T>(
      controller: _scrollController,
      onRefresh: _onRefresh,
      list: list,
      itemBuilder: _itemBuilder,
      emptyWidget: widget.emptyWidget,
      footerBuilder: showFooter ? _loadMoreFooter : null,
      separatorBuilder: widget.dividerBuilder,
    );
  }
}
