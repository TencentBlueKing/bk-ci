import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/pages/DetailScreen.dart';
import 'package:bkci_app/pages/SearchScreen.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/BkListView.dart';
import 'package:bkci_app/widgets/ExperienceListItem.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class ExperienceScreen extends StatelessWidget {
  static const routePath = '/experience';

  void goSearch(BuildContext context) {
    final SearchScreenArgs args = SearchScreenArgs(
      experiencePublic: false,
    );
    Navigator.of(context).pushNamed(SearchScreen.routePath, arguments: args);
  }

  void goDetail(BuildContext context, String expId) {
    DetailScreenArgument args = DetailScreenArgument(expId: expId);
    Navigator.of(context).pushNamed(DetailScreen.routePath, arguments: args);
  }

  Widget buildSectionHeader(context, title) {
    return Container(
      color: Theme.of(context).backgroundColor,
      padding: EdgeInsets.fromLTRB(32.px, 24.px, 0, 16.px),
      child: PFMediumText(
        title,
        style: TextStyle(
          fontSize: 24.px,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final i18n = BkDevopsAppi18n.of(context).$t;
    return Scaffold(
      appBar: AppBar(
        brightness: Brightness.light,
        shadowColor: Colors.transparent,
        title: PFMediumText(
          i18n('myExperience'),
          style: TextStyle(
            color: Colors.black,
            fontSize: 36.px,
          ),
        ),
        actions: [
          Padding(
            padding: EdgeInsets.only(
              right: 32.px,
            ),
            child: IconButton(
              icon: Icon(
                BkIcons.search,
                size: 44.px,
              ),
              onPressed: () {
                goSearch(context);
              },
            ),
          ),
        ],
      ),
      body: Container(
        color: Colors.white,
        child: BkListView(
          dividerBuilder: (
            BuildContext context,
            int index,
            item,
            nextItem,
          ) =>
              (item is Experience && nextItem is! String)
                  ? Divider(
                      height: 1.px,
                      indent: 160.px,
                    )
                  : SizedBox(),
          itemBuilder: (dynamic item) {
            return item is Experience
                ? ExperienceListItem(
                    item: item,
                    onTap: goDetail,
                  )
                : buildSectionHeader(context, item);
          },
        ),
      ),
    );
  }
}
