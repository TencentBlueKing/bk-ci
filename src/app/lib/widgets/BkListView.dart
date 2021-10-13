import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/providers/ExperienceProvider.dart';
import 'package:bkci_app/widgets/CupertinoPullRefresh.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:provider/provider.dart';

class BkListView extends StatefulWidget {
  final Widget Function(dynamic item) itemBuilder;
  final Widget Function(
          BuildContext context, int index, dynamic item, dynamic nextItem)
      dividerBuilder;

  BkListView({
    Key key,
    this.itemBuilder,
    this.dividerBuilder,
  }) : super(key: key);

  @override
  _BkListViewState createState() => _BkListViewState();
}

class _BkListViewState<T> extends State<BkListView> {
  bool showFooter = false;
  bool isLoadingMore = false;
  bool isRefresh = false;
  List<Experience> list = [];
  int page = 1;
  int pageSize = 24;

  ScrollController _scrollController = new ScrollController();
  @override
  void initState() {
    super.initState();

    _scrollController.addListener(scrollToEnd);
  }

  setWidgetState(setFunc) {
    if (mounted) {
      setState(setFunc);
    }
  }

  scrollToEnd() {
    final bool isReachBottom = _scrollController.position.pixels >=
        (_scrollController.position.maxScrollExtent * 0.98);
    if (isReachBottom != showFooter) {
      setWidgetState(() {
        showFooter = isReachBottom;
      });
    }
    if (_scrollController.position.pixels ==
        (_scrollController.position.maxScrollExtent)) {
      _loadMore();
    }
  }

  Future _loadMore() async {
    if (!isLoadingMore && !isRefresh) {
      setWidgetState(() {
        isLoadingMore = true;
      });

      await Provider.of<ExperienceProvider>(context, listen: false).loadMore();
      setWidgetState(() {
        isLoadingMore = false;
      });
    }
  }

  Future _onRefresh() async {
    setWidgetState(() {
      isRefresh = true;
    });

    await Provider.of<ExperienceProvider>(context, listen: false).refresh();

    setWidgetState(() {
      isRefresh = false;
    });
  }

  @override
  void dispose() {
    _scrollController.removeListener(scrollToEnd);
    super.dispose();
  }

  Widget _loadMoreFooter(
      BuildContext context, List list, bool hasNext, int page) {
    if (list.isEmpty || (!hasNext && page == 1)) return null;
    print('$hasNext, $page, $isLoadingMore');
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

  Widget _itemBuilder(BuildContext context, dynamic item, index) {
    return widget.itemBuilder(item);
  }

  @override
  Widget build(BuildContext context) {
    return Selector<ExperienceProvider, List>(
      selector: (context, provider) {
        return [
          provider.loading,
          provider.section,
          provider.hasNext,
          provider.page
        ];
      },
      builder: (context, value, child) {
        if (value[0]) {
          return Center(
            child: CircularProgressIndicator(
              backgroundColor: Theme.of(context).primaryColor,
            ),
          );
        }
        return CupertinoPullRefresh(
          controller: _scrollController,
          onRefresh: _onRefresh,
          list: value[1],
          itemBuilder: _itemBuilder,
          footerBuilder: showFooter
              ? (context) {
                  return _loadMoreFooter(context, value[1], value[2], value[3]);
                }
              : null,
          separatorBuilder: widget.dividerBuilder,
        );
      },
    );
  }
}
