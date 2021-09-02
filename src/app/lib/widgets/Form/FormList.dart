import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/cupertino.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:bkci_app/widgets/Form/Selector.dart';
import 'package:bkci_app/widgets/Form/TextInput.dart';
import 'package:bkci_app/widgets/Form/BooleanRadio.dart';
import 'package:bkci_app/widgets/Form/DatePicker.dart';

class FormList extends StatefulWidget {
  final formType;
  final List paramList;
  final Map paramValue;
  final String buttonText;
  final Function submit;
  final List syncItems;
  final bool submiting;
  final Function handleSync;

  FormList({
    this.formType = 'horizonal',
    this.paramList,
    this.paramValue,
    this.buttonText,
    this.submit,
    this.syncItems,
    this.handleSync,
    this.submiting = false,
  });

  @override
  _FormListState createState() => _FormListState();
}

class _FormListState extends State<FormList> {
  bool errorFlag = false;

  @override
  void initState() {
    super.initState();
    checkHasError();
  }

  checkHasError() {
    bool flag = false;
    widget.paramList.forEach((param) {
      bool isEmpty = this._valueIsEmpty(widget.paramValue[param['id']]);
      if (param['required'] == true && isEmpty) {
        flag = true;
      }
      return;
    });
    this.errorFlag = flag;
    return;
  }

  handleChange(name, value) {
    setState(() {
      widget.paramValue[name] = value;
      this.checkHasError();
    });
    if (widget.syncItems != null && widget.syncItems.indexOf(name) >= 0) {
      widget.handleSync(name, value);
    }
  }

  void showTipsDialog({String title, String content, String height}) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        var child = Column(
          children: <Widget>[
            Container(
              padding: EdgeInsets.fromLTRB(0, 40.px, 0, 24.px),
              child: PFMediumText(
                title,
                overflow: TextOverflow.ellipsis,
                maxLines: 1,
                style: TextStyle(fontSize: 36.px, color: Colors.black),
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                child: Container(
                  padding: EdgeInsets.fromLTRB(26.px, 0, 26.px, 24.px),
                  alignment: Alignment.topLeft,
                  child: Text(
                    content,
                    style: TextStyle(
                      fontSize: 30.px,
                    ),
                  ),
                ),
              ),
            ),
            Divider(
              height: 1.px,
            ),
            GestureDetector(
              child: Container(
                padding: EdgeInsets.fromLTRB(200.px, 52.px, 200.px, 40.px),
                decoration: BoxDecoration(),
                child: Text(
                  BkDevopsAppi18n.of(context).$t('confirm'),
                  style: TextStyle(
                    fontSize: 32.px,
                    color: Theme.of(context).primaryColor,
                  ),
                ),
              ),
              onTap: () {
                Navigator.of(context).pop();
              },
            ),
          ],
        );
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(32.px),
          ),
          child: Container(
            height: height ?? 504.px,
            width: 686.px,
            margin: EdgeInsets.symmetric(horizontal: 36.px),
            child: child,
          ),
        );
      },
    );
  }

  bool _valueIsEmpty(val) {
    if (val == null) {
      return true;
    } else if (val is int || val is double) {
      return val <= 0;
    } else if (val is String || val is List) {
      return val.length <= 0;
    } else {
      return false;
    }
  }

  void submitData() {
    FocusScope.of(context).requestFocus(FocusNode());
    widget.submit(widget.paramValue);
  }

  getItemWidget(Map item) {
    switch (item['widgetType']) {
      case 'select':
        return Selector(
          name: item['id'],
          value: widget.paramValue[item['id']],
          label: item['label'],
          options: item['options'],
          multiple: false,
          placeholder: item['placeholder'],
          require: item['required'] ?? false,
          handleChange: handleChange,
        );
      case 'multiple':
        return Selector(
          name: item['id'],
          value: widget.paramValue[item['id']],
          label: item['label'],
          options: item['options'],
          multiple: true,
          isArray: item['isArray'] ?? false,
          placeholder: item['placeholder'],
          require: item['required'] ?? false,
          handleChange: handleChange,
        );
      case 'boolean':
        return BooleanRadio(
          name: item['id'],
          value: widget.paramValue[item['id']],
          options: [
            {'key': true, 'value': 'true'},
            {'key': false, 'value': 'false'},
          ],
          handleChange: handleChange,
        );
      case 'datePicker':
        return DatePicker(
          name: item['id'],
          value: widget.paramValue[item['id']],
          placeholder: item['placeholder'],
          require: item['required'],
          handleChange: handleChange,
        );
      default:
        return TextInput(
          name: item['id'],
          value: widget.paramValue[item['id']],
          placeholder: item['placeholder'],
          maxLength: item['maxLength'],
          maxLines: item['maxLines'],
          require: item['required'] ?? false,
          handleChange: handleChange,
        );
    }
  }

  Widget horizonForm(Map item) {
    return Offstage(
      offstage: item['hidden'] == true,
      child: Container(
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(
                color: Theme.of(context).dividerColor,
                width: .5.px,
                style: BorderStyle.solid),
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 250.px,
              height: 88.px,
              child: Row(
                children: [
                  Container(
                    constraints: BoxConstraints(maxWidth: 200.px),
                    padding: EdgeInsets.only(right: 5.px),
                    child: Text(
                      item['label'] ?? item['id'],
                      overflow: TextOverflow.ellipsis,
                      maxLines: 2,
                      style: TextStyle(fontSize: 28.px, color: Colors.black),
                    ),
                  ),
                  Offstage(
                    offstage:
                        item['required'] == null || item['required'] == false,
                    child: Text(
                      '*',
                      style: TextStyle(fontSize: 24.px, color: Colors.red),
                    ),
                  ),
                  Offstage(
                    offstage: item['desc'] == null || item['desc'] == '',
                    child: InkWell(
                      child: Padding(
                        padding: EdgeInsets.only(left: 5.px),
                        child: Icon(
                          BkIcons.info,
                          size: 28.px,
                          color: Theme.of(context).secondaryHeaderColor,
                        ),
                      ),
                      onTap: () {
                        showTipsDialog(
                            title: item['label'] ?? item['id'],
                            content: item['desc']);
                      },
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: getItemWidget(item),
            ),
          ],
        ),
      ),
    );
  }

  Widget verticalForm(Map item) {
    return Offstage(
      offstage: item['hidden'] == true,
      child: Container(
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(
                color: Theme.of(context).dividerColor,
                width: .5.px,
                style: BorderStyle.solid),
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 250.px,
              height: 88.px,
              child: Row(
                children: [
                  Container(
                    constraints: BoxConstraints(maxWidth: 200.px),
                    padding: EdgeInsets.only(right: 5.px),
                    child: Text(
                      item['label'] ?? item['id'],
                      overflow: TextOverflow.ellipsis,
                      maxLines: 2,
                      style: TextStyle(fontSize: 28.px, color: Colors.black),
                    ),
                  ),
                  Offstage(
                    offstage:
                        item['required'] == null || item['required'] == false,
                    child: Text(
                      '*',
                      style: TextStyle(fontSize: 24.px, color: Colors.red),
                    ),
                  ),
                  Offstage(
                    offstage: item['desc'] == null || item['desc'] == '',
                    child: InkWell(
                      child: Padding(
                        padding: EdgeInsets.only(left: 10.px),
                        child: Icon(
                          BkIcons.info,
                          size: 28.px,
                          color: Theme.of(context).secondaryHeaderColor,
                        ),
                      ),
                      onTap: () {
                        Fluttertoast.showToast(
                          msg: item['desc'],
                          gravity: ToastGravity.CENTER,
                          fontSize: 24.px,
                          backgroundColor: Colors.black,
                        );
                      },
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: getItemWidget(item),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (final item in widget.paramList)
            widget.formType == 'vertical'
                ? verticalForm(item)
                : horizonForm(item),
          Container(
            height: 88.px,
            width: 686.px,
            margin: EdgeInsets.only(top: 40.px),
            child: ElevatedButton(
              onPressed: errorFlag ? null : submitData,
              style: ElevatedButton.styleFrom(
                primary: widget.submiting
                    ? Theme.of(context).backgroundColor
                    : Theme.of(context).primaryColor,
              ),
              child: widget.submiting
                  ? SizedBox(
                      width: 36.px,
                      height: 36.px,
                      child: CircularProgressIndicator(
                        color: Colors.white,
                        strokeWidth: 2,
                      ),
                    )
                  : Text(
                      widget.buttonText,
                      style: TextStyle(
                        fontSize: 30.px,
                        color: Colors.white,
                      ),
                    ),
            ),
          )
        ],
      ),
    );
  }
}
