import 'package:bkci_app/main.dart';
import 'package:bkci_app/models/BkBanner.dart';
import 'package:bkci_app/pages/DetailScreen.dart';
import 'package:bkci_app/pages/MoreApps.dart';
import 'package:bkci_app/providers/HomeProvider.dart';
import 'package:bkci_app/widgets/AuthImage.dart';
import 'package:bkci_app/widgets/TencentAppList.dart';
import 'package:bkci_app/widgets/ExpHorizontalList.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:flutter/material.dart';
import 'package:flutter_swiper/flutter_swiper.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

class RecommendTab extends StatelessWidget {
  void goMoreAppList(String title, String type) {
    final MoreAppsArgument moreArgs = MoreAppsArgument(
      title: title,
      type: type,
    );
    DevopsApp.navigatorKey.currentState.pushNamed(
      MoreApps.routePath,
      arguments: moreArgs,
    );
  }

  Future _handleRefresh(BuildContext context) async {
    await Provider.of<HomeProvider>(context, listen: false).fetchHome();
  }

  Future<void> goDetail(BkBanner exp) async {
    if (exp.isAppStore) {
      if (await canLaunch(exp.externalUrl)) {
        launch(exp.externalUrl, forceSafariVC: true);
      }
    } else {
      final DetailScreenArgument args =
          DetailScreenArgument(expId: exp.experienceHashId);
      DevopsApp.navigatorKey.currentState.pushNamed(
        DetailScreen.routePath,
        arguments: args,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      backgroundColor: Theme.of(context).primaryColor,
      onRefresh: () => _handleRefresh(context),
      child: SingleChildScrollView(
        physics: const BouncingScrollPhysics(
          parent: const AlwaysScrollableScrollPhysics(),
        ),
        child: Consumer<HomeProvider>(
          builder: (BuildContext context, HomeProvider value, Widget child) {
            if (value.loading) {
              return Container(
                margin: EdgeInsets.only(top: SizeFit.deviceHeight / 3),
                child: Center(
                  child: CircularProgressIndicator(
                    backgroundColor: Theme.of(context).primaryColor,
                  ),
                ),
              );
            }

            return Column(
              children: [
                Container(
                  width: 700.px,
                  height: 400.px,
                  padding: EdgeInsets.only(top: 32.px),
                  child: new Swiper(
                    itemHeight: 360.px,
                    itemBuilder: (BuildContext context, int index) {
                      final BkBanner item = value.banners[index];
                      return ClipRRect(
                        borderRadius: BorderRadius.circular(20.px),
                        child: AuthImage(
                          url: item.bannerUrl,
                        ),
                      );
                    },
                    onTap: (index) {
                      final BkBanner item = value.banners[index];
                      goDetail(item);
                    },
                    itemCount: value.banners.length,
                    pagination: SwiperPagination(),
                  ),
                ),
                Container(
                  color: Colors.white,
                  height: 8.px,
                ),
                Container(
                  color: Colors.white,
                  child: ExpHorizontalList(
                    title: BkDevopsAppi18n.of(context).$t('weekly'),
                    actionTap: () {
                      goMoreAppList('weekly', 'newest');
                    },
                    list: value.newest,
                  ),
                ),
                Container(
                  color: '#F0F1F5'.color,
                  height: 16.px,
                ),
                GestureDetector(
                  onTap: () {
                    goMoreAppList('tencentNeeded', 'necessary');
                  },
                  child: Container(
                    padding: EdgeInsets.fromLTRB(32.px, 40.px, 32.px, 46.px),
                    color: Colors.white,
                    child: TencentAppList(
                      onTap: () {
                        goMoreAppList('tencentNeeded', 'necessary');
                      },
                      list: value.necessary,
                    ),
                  ),
                ),
                Container(
                  color: '#F0F1F5'.color,
                  height: 16.px,
                ),
                Container(
                  color: Colors.white,
                  child: ExpHorizontalList(
                    title: BkDevopsAppi18n.of(context).$t('hots'),
                    actionTap: () {
                      goMoreAppList('hots', 'hots');
                    },
                    list: value.hots,
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
