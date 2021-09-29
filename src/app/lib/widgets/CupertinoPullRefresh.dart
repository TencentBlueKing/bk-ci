import 'dart:math';
import 'package:bkci_app/widgets/Empty.dart';
import 'package:bkci_app/widgets/CupertinoPullRequestIndicator.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class CupertinoPullRefresh<T> extends StatelessWidget {
  final Function onRefresh;
  final bool hasNext;
  final int page;
  final List list;
  final bool reverse;
  final ScrollController controller;
  final Widget Function(BuildContext context, T item, int index) itemBuilder;
  final Widget Function(BuildContext context) footerBuilder;
  final Widget Function(BuildContext context, int index, T item, T nextItem)
      separatorBuilder;
  final Widget emptyWidget;

  CupertinoPullRefresh({
    Key key,
    this.onRefresh,
    this.list,
    this.hasNext,
    this.page,
    this.itemBuilder,
    this.controller,
    this.footerBuilder,
    this.separatorBuilder,
    this.emptyWidget,
    this.reverse = false,
  }) : super(key: key);

  Widget _defaultSeparatorBuilder() => Divider(indent: 160.px);

  Widget _emptyBuilder(BuildContext context) {
    return SliverFillRemaining(
      child: emptyWidget ?? Empty(),
    );
  }

  @override
  Widget build(BuildContext context) {
    final List<Widget> slivers = [
      CupertinoSliverRefreshControl(
        onRefresh: onRefresh,
        builder: (
          context,
          refreshState,
          pulledExtent,
          refreshTriggerPullDistance,
          refreshIndicatorExtent,
        ) =>
            CupertinoPullRequestIndicator(
          context: context,
          refreshState: refreshState,
          pulledExtent: pulledExtent,
          refreshTriggerPullDistance: refreshTriggerPullDistance,
          refreshIndicatorExtent: refreshIndicatorExtent,
        ),
        // refreshTriggerPullDistance: 320,
      ),
      list.isNotEmpty
          ? SliverList(
              delegate: SliverChildBuilderDelegate(
                (context, index) {
                  final int itemIndex = index ~/ 2;
                  final item = list[itemIndex];

                  if (index.isEven) {
                    return itemBuilder(context, item, index);
                  }
                  final nextItem = index + 1 < (list.length * 2 - 1)
                      ? list[(index + 1) ~/ 2]
                      : null;

                  return separatorBuilder != null
                      ? separatorBuilder(
                          context,
                          index,
                          item,
                          nextItem,
                        )
                      : _defaultSeparatorBuilder();
                },
                childCount: max(0, list.length * 2 - 1),
              ),
            )
          : _emptyBuilder(context),
    ];

    if (footerBuilder != null) {
      slivers.add(
        SliverToBoxAdapter(
          child: footerBuilder((context)),
        ),
      );
    }

    return CustomScrollView(
      controller: controller,
      keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
      reverse: reverse,
      physics: const BouncingScrollPhysics(
        parent: AlwaysScrollableScrollPhysics(),
      ),
      slivers: slivers,
    );
  }
}
