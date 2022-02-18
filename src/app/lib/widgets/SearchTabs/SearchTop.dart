import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/SearchInput.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/cupertino.dart';

class SearchTop extends StatelessWidget {
  SearchTop({
    Key key,
    this.searchValue = '',
    this.onChanged,
    this.placeholder,
  });

  final String searchValue;
  final Function(String, bool) onChanged;
  final String placeholder;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 100.px,
      alignment: Alignment.center,
      padding: EdgeInsets.fromLTRB(32.px, 0, 32.px, 0),
      child: Row(
        children: [
          Expanded(
            child: SearchInput(
              handleChange: onChanged,
              placeholder: placeholder,
              searchValue: searchValue,
            ),
          ),
          InkWell(
            onTap: () {
              Navigator.pop(context);
            },
            child: Padding(
              padding: EdgeInsets.fromLTRB(22.px, 0, 22.px, 0),
              child: Text(
                BkDevopsAppi18n.of(context).$t('cancel'),
                style: TextStyle(
                  fontSize: 32.px,
                  color: Theme.of(context).primaryColor,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
