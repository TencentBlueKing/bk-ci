import 'package:bkci_app/providers/BkGlobalStateProvider.dart';
import 'package:bkci_app/utils/AppSetting.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkAppBar.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/Form/Options.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:provider/provider.dart';

class SettingPage extends StatelessWidget {
  static const String routePath = '/settings';
  SettingPage({Key key}) : super(key: key);

  final double cacheExtent = 88.px;
  final appSettingItems = AppSetting.getSettingItems();

  Widget _buildSettingItem(BuildContext context, int index, Map settings) {
    final SettingItem item = appSettingItems[index];
    final value = settings[item.key];
    final selecteOption =
        item.options.find((element) => element['key'] == value);
    final valueLabel = selecteOption != null ? selecteOption['value'] : value;

    return GestureDetector(
      onTap: () => handleItemClick(context, item, settings),
      child: Container(
        height: cacheExtent,
        padding: EdgeInsets.symmetric(
          horizontal: 32.px,
        ),
        decoration: BoxDecoration(
          color: Colors.white,
        ),
        child: Row(
          children: [
            Expanded(
              child: PFText(
                BkDevopsAppi18n.of(context).$t(item.label),
                style: TextStyle(
                  color: Colors.black,
                  fontSize: 28.px,
                ),
              ),
            ),
            PFText(
              valueLabel,
              style: TextStyle(
                fontSize: 28.px,
              ),
            ),
            Icon(
              BkIcons.right,
              color: Theme.of(context).textTheme.subtitle2.color,
            ),
          ],
        ),
      ),
    );
  }

  handleItemClick(BuildContext context, SettingItem item, Map settings) {
    return showModalBottomSheet(
      context: context,
      enableDrag: true,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(30.px),
          topRight: Radius.circular(30.px),
        ),
      ),
      builder: (BuildContext context) {
        final provider =
            Provider.of<BkGlobalStateProvider>(context, listen: false);
        return Container(
          padding: EdgeInsets.symmetric(
            horizontal: 32.px,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: EdgeInsets.only(
                  top: 30.px,
                  bottom: 23.px,
                ),
                child: PFMediumText(
                  BkDevopsAppi18n.of(context).$t(item.label),
                  textAlign: TextAlign.left,
                  style: TextStyle(
                    fontSize: 32.px,
                  ),
                ),
              ),
              Divider(
                height: 1.px,
              ),
              Expanded(
                child: Options(
                  options: item.options,
                  handleChange: (value) async {
                    await provider.setSettings(item.key, value);
                    Navigator.of(context).pop();
                    toast(
                        '${BkDevopsAppi18n.of(context).$t(item.label)} ${BkDevopsAppi18n.of(context).$t("settingDoneTips")}');
                  },
                  value: settings[item.key],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildSeparator(BuildContext context, int index) {
    return Divider(
      height: 1.px,
      indent: 32.px,
    );
  }

  @override
  Widget build(BuildContext context) {
    final i18n = BkDevopsAppi18n.of(context);
    return Scaffold(
      appBar: BkAppBar(
        title: i18n.$t('setting'),
        shadowColor: Colors.transparent,
      ),
      body: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Divider(
            height: 1.px,
          ),
          Container(
            height: cacheExtent * appSettingItems.length,
            color: Colors.white,
            child: Selector<BkGlobalStateProvider, Map>(selector:
                (BuildContext context, BkGlobalStateProvider provider) {
              return provider.settings;
            }, builder: (BuildContext context, Map settings, _) {
              return ListView.separated(
                physics: NeverScrollableScrollPhysics(),
                itemBuilder: (BuildContext context, int index) =>
                    _buildSettingItem(context, index, settings),
                separatorBuilder: _buildSeparator,
                itemCount: appSettingItems.length,
                cacheExtent: cacheExtent,
              );
            }),
          ),
        ],
      ),
    );
  }
}
