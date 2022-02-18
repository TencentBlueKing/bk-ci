import 'package:bkci_app/pages/App.dart';
import 'package:bkci_app/pages/LoginScreen.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class GuidePage extends StatefulWidget {
  static const String routePath = 'guide';
  const GuidePage({Key key}) : super(key: key);

  @override
  _GuidePageState createState() => _GuidePageState();
}

class _GuidePageState extends State<GuidePage> {
  int _pageIndex = 0;
  PageController _controller = PageController();

  final List<int> guildList = List.generate(4, (int i) => i);

  @override
  void didChangeDependencies() {
    precacheImage(
      AssetImage(
        "assets/images/guide_1.png",
      ),
      context,
    );
    print('did change dependencies GUIDE');
    super.didChangeDependencies();
  }

  Widget _createPageView() {
    return PageView.builder(
      controller: _controller,
      itemCount: guildList.length,
      physics: BouncingScrollPhysics(),
      onPageChanged: (int pageIndex) {
        final nextIndex =
            pageIndex + 1 < guildList.length ? pageIndex + 1 : pageIndex;
        precacheImage(
          AssetImage(
            "assets/images/guide_$nextIndex.png",
          ),
          context,
        );

        setState(() {
          _pageIndex = pageIndex;
        });
      },
      itemBuilder: (BuildContext context, int index) => Container(
        child: Image.asset(
          "assets/images/guide_$index.png",
          fit: BoxFit.contain,
        ),
      ),
    );
  }

  _createPageIndicator() {
    return Opacity(
      opacity: 0.7,
      child: SizedBox(
        height: 40.px,
        width: 180.px,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            for (int i = 0; i < guildList.length; i++) _dotWidget(i),
          ],
        ),
      ),
    );
  }

  _dotWidget(int index) {
    final bool isActive = _pageIndex == index;
    final double dotRadius = 18.px;
    return AnimatedContainer(
      duration: Duration(milliseconds: 300),
      width: isActive ? 60.px : dotRadius,
      height: dotRadius,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(dotRadius),
        color: isActive ? Colors.white : Colors.white70,
      ),
    );
  }

  Widget _createButton() {
    final isLast = _pageIndex == guildList.length - 1;
    return Align(
      alignment: FractionalOffset.bottomCenter,
      child: AnimatedContainer(
        duration: Duration(milliseconds: 600),
        width: isLast ? SizeFit.deviceWidth : 100,
        height: 106.px,
        curve: Curves.easeInOutCirc,
        margin: EdgeInsets.symmetric(
          horizontal: 40.px,
        ),
        child: Opacity(
          opacity: isLast ? 1 : 0,
          child: ElevatedButton(
            onPressed: handlePress,
            style: ElevatedButton.styleFrom(
              shape: StadiumBorder(),
              primary: '#2a6eff'.color,
              padding: EdgeInsets.all(0),
            ),
            child: AnimatedDefaultTextStyle(
              duration: Duration(milliseconds: 600),
              style: TextStyle(
                fontSize: 40.px,
                fontFamily: 'PingFang-medium',
                color: isLast ? Colors.white : Colors.transparent,
              ),
              child: Text(
                BkDevopsAppi18n.of(context).$t('experienceImmediately'),
              ),
            ),
          ),
        ),
      ),
    );
  }

  void handlePress() {
    final route =
        Storage.hasCkey() ? BkDevopsApp.routePath : LoginScreen.routePath;

    Storage.setGuideShowed();
    Navigator.of(context).pushReplacementNamed(route);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          Container(
            color: Colors.white,
            height: SizeFit.deviceHeight,
            child: Image.asset(
              "assets/images/guide_bg.jpg",
              fit: BoxFit.fitHeight,
            ),
          ),
          _createPageView(),
          Align(
            alignment: FractionalOffset.bottomCenter,
            child: Container(
              margin: EdgeInsets.only(bottom: 120.px),
              child: _createPageIndicator(),
            ),
          ),
          Align(
            alignment: FractionalOffset.bottomCenter,
            child: Container(
              margin: EdgeInsets.only(bottom: 120.px),
              child: _createButton(),
            ),
          ),
        ],
      ),
    );
  }
}
