import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkAppBar.dart';
import 'package:bkci_app/widgets/DownloadRecordsTab.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class DownloadRecords extends StatelessWidget {
  static const String routePath = '/downloadRecrods';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: BkAppBar(
        shadowColor: Colors.transparent,
        title: BkDevopsAppi18n.of(context).$t('downloadRecords'),
      ),
      body: Container(
        color: Colors.white,
        padding: EdgeInsets.symmetric(vertical: 20.px),
        child: DownloadRecordsTab(),
      ),
    );
  }
}
