import 'package:bkci_app/utils/i18n.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class CupertinoPullRequestIndicator extends StatelessWidget {
  CupertinoPullRequestIndicator({
    Key key,
    @required this.context,
    @required this.refreshState,
    @required this.pulledExtent,
    @required this.refreshTriggerPullDistance,
    @required this.refreshIndicatorExtent,
  }) : super(key: key);

  final TextStyle textStyle = TextStyle(
    color: '#979BA5'.color,
    fontSize: 28.px,
  );
  final BuildContext context;
  final RefreshIndicatorMode refreshState;
  final double pulledExtent;
  final double refreshTriggerPullDistance;
  final double refreshIndicatorExtent;

  @override
  Widget build(BuildContext context) {
    String label;
    Widget activityIndicator;
    var icon;
    final double percentageComplete = pulledExtent / refreshTriggerPullDistance;

    switch (refreshState) {
      case RefreshIndicatorMode.armed:
        label = 'releaseToRefresh';
        icon = Icons.arrow_downward_rounded;
        break;
      case RefreshIndicatorMode.done:
        label = 'refreshSuccess';
        icon = Icons.check_rounded;
        break;
      case RefreshIndicatorMode.refresh:
        label = 'refreshing';
        activityIndicator = CupertinoActivityIndicator();
        break;
      case RefreshIndicatorMode.drag:
        const Curve opacityCurve = Interval(0.0, 0.35, curve: Curves.easeInOut);
        return Opacity(
          opacity: opacityCurve.transform(percentageComplete),
          child: CupertinoActivityIndicator.partiallyRevealed(
            progress: percentageComplete,
          ),
        );
      default:
        return SizedBox();
    }

    return Center(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Padding(
            padding: EdgeInsets.only(right: 10.px),
            child: icon != null ? Icon(icon, size: 32.px) : activityIndicator,
          ),
          Text(
            BkDevopsAppi18n.of(context).$t(label),
            style: textStyle,
          ),
        ],
      ),
    );
  }
}
