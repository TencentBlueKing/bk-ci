import 'package:bkci_app/widgets/CupertinoPullRequestIndicator.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class CupertinoPullRefreshContainer extends StatelessWidget {
  final Function onRefresh;
  final Widget child;
  CupertinoPullRefreshContainer({
    this.onRefresh,
    this.child,
  });

  @override
  Widget build(BuildContext context) {
    return CustomScrollView(
      physics: const BouncingScrollPhysics(
        parent: AlwaysScrollableScrollPhysics(),
      ),
      slivers: [
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
        SliverToBoxAdapter(
          child: child,
        )
      ],
    );
  }
}
