import 'package:bkci_app/models/ShareArgs.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkAppBar.dart';
import 'package:bkci_app/widgets/BkErrorWidget.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/CupertinoPullRefreshContainer.dart';
import 'package:bkci_app/widgets/DetailPage/InfoList.dart';
import 'package:bkci_app/widgets/DetailPage/Summary.dart';
import 'package:bkci_app/pages/ExpHistory.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/SharePopup.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/ExpandableText.dart';

import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/models/experienceDetail.dart';

class DetailScreenArgument {
  final String expId;
  final bool fromHistory;

  DetailScreenArgument({this.expId, this.fromHistory});
}

class DetailScreen extends StatefulWidget {
  static const String routePath = '/detail';
  final String expId;
  final bool fromHistory;

  DetailScreen({this.expId, this.fromHistory = false});

  @override
  _DetailState createState() => _DetailState();
}

class _DetailState extends State<DetailScreen> {
  ExperienceDetail detail;

  bool hasInit = false;
  ScrollController _controller = ScrollController();
  var error;

  @override
  void initState() {
    super.initState();
    getExpDetail();
  }

  Future getExpDetail() async {
    Exception err;
    var result;
    try {
      result = await ajax
          .get('/experience/api/app/experiences/${widget.expId}/detail');
    } on Exception catch (e) {
      err = e;
    } finally {
      if (mounted) {
        setState(() {
          hasInit = true;
          error = err;
          detail = err == null ? ExperienceDetail.fromJson(result.data) : null;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!hasInit) {
      return Container(
          color: Colors.white,
          child: Center(
            child: CircularProgressIndicator(
              backgroundColor: Theme.of(context).primaryColor,
            ),
          ));
    }
    if (error != null) {
      return BkErrorWidget(
        title: BkDevopsAppi18n.of(context).$t('appDetail'),
        flutterErrorDetails: error,
        authTitle: BkDevopsAppi18n.of(context).$t('noAccessExpTips'),
        authDesc: BkDevopsAppi18n.of(context).$t('applyExpTips'),
      );
    }

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: BkAppBar(
        title: BkDevopsAppi18n.of(context).$t('appDetail'),
        actions: detail != null
            ? [
                IconButton(
                  icon: Icon(BkIcons.share),
                  onPressed: () {
                    ShareArgs shareArgs = ShareArgs(
                      kind: 'webpage',
                      title: detail.experienceName,
                      isPublicExperience: detail.publicExperience,
                      description: detail.remark,
                      previewImageUrl: detail.logoUrl,
                      endDate: detail.endDate,
                      packageName: detail.packageName,
                      url:
                          '$SHARE_URL_PREFIX/expDetail/?flag=experienceDetail&experienceId=${detail.experienceHashId}',
                    );

                    SharePopup(shareArgs: shareArgs).show(context);
                  },
                ),
              ]
            : [],
      ),
      body: CupertinoPullRefreshContainer(
        onRefresh: getExpDetail,
        child: SingleChildScrollView(
          controller: _controller,
          child: Column(
            children: [
              Summary(detail: detail),
              Container(
                height: 16.px,
                color: '#F0F1F5'.color,
              ),
              Container(
                alignment: Alignment.topLeft,
                padding: EdgeInsets.fromLTRB(32.px, 24.px, 32.px, 26.px),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    PFText(
                      detail.versionTitle,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 28.px,
                      ),
                    ),
                    new Padding(
                      padding: EdgeInsets.only(top: 7.px),
                      child: PFText(
                        "${BkDevopsAppi18n.of(context).$t('version')}：${detail.version}",
                        style: TextStyle(
                          fontSize: 24.px,
                        ),
                      ),
                    )
                  ],
                ),
              ),
              Divider(
                height: 1.px,
              ),
              Container(
                padding: EdgeInsets.fromLTRB(32.px, 24.px, 32.px, 15.px),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        PFMediumText(
                          BkDevopsAppi18n.of(context).$t('versionDesc'),
                          style: TextStyle(
                            fontSize: 28.px,
                          ),
                        ),
                        if (detail.showExpHistory)
                          InkWell(
                            onTap: () {
                              if (widget.fromHistory) {
                                Navigator.of(context).pop();
                              } else {
                                Navigator.of(context).pushNamed(
                                  '/expHistory',
                                  arguments: ExpHistoryArgument(
                                    expId: widget.expId,
                                    bundleIdentifier: detail.bundleIdentifier,
                                  ),
                                );
                              }
                            },
                            child: PFText(
                              BkDevopsAppi18n.of(context).$t('expHistory'),
                              style: TextStyle(
                                fontSize: 28.px,
                                color: Theme.of(context).primaryColor,
                              ),
                            ),
                          ),
                      ],
                    ),
                    Container(
                      alignment: Alignment.topLeft,
                      padding: EdgeInsets.only(top: 15.px, bottom: 10.px),
                      child: ExpandableText(
                        detail.remark ?? '',
                        maxLines: 3,
                        beforeToggle: (bool expanded) async {
                          if (!expanded) {
                            _controller?.jumpTo(0);
                          }
                          return true;
                        },
                        style: TextStyle(
                          color: Theme.of(context).secondaryHeaderColor,
                          fontSize: 28.px,
                        ),
                      ),
                    )
                  ],
                ),
              ),
              Divider(
                height: 1.px,
              ),
              InfoList(infoMap: detail.getInfoMap)
            ],
          ),
        ),
      ),
    );
  }
}
