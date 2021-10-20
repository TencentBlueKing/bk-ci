import 'package:bkci_app/pages/App.dart';
import 'package:bkci_app/providers/UserProvider.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/BkAppBar.dart';
import 'package:bkci_app/widgets/Loading.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:dio/dio.dart';

const String EXTERNAL_USER_ACCOUNT = 'EXTERNAL_USER_ACCOUNT';

class ExternalUser {
  final String username;
  final String password;

  ExternalUser({
    this.username,
    this.password,
  });
}

class MailLoginScreen extends StatefulWidget {
  static const String routePath = '/mailLogin';
  @override
  _MailLoginScreenState createState() => _MailLoginScreenState();
}

class _MailLoginScreenState extends State<MailLoginScreen> {
  final TextStyle labelStyle = TextStyle(
    color: Colors.black,
    fontSize: 24.px,
  );
  TextEditingController _usernameController;
  TextEditingController _passwordController;
  bool _remember = true;

  @override
  void initState() {
    super.initState();
    final ExternalUser account = getRememberAccount();

    _usernameController = TextEditingController(text: account?.username);
    _passwordController = TextEditingController(text: account?.password);
  }

  ExternalUser getRememberAccount() {
    final accountStr = Storage.getString(EXTERNAL_USER_ACCOUNT);
    if (accountStr is String) {
      final accountArray = accountStr.split('::');

      return ExternalUser(
        username: accountArray[0],
        password: accountArray[1],
      );
    }
    return null;
  }

  setRememberUser() async {
    await Storage.setString(EXTERNAL_USER_ACCOUNT,
        '${_usernameController.text}::${_passwordController.text}');
  }

  Future _login() async {
    try {
      final res = await ajax.post('/experience/api/open/experiences/outerLogin',
          data: {
            'username': _usernameController.text,
            'password': _passwordController.text,
          },
          options: Options(extra: {'noNeedAuth': true}));

      if (_remember) {
        setRememberUser();
      }

      await Storage.setString(CKEY_HEAD_FIELD, res.toString());

      return true;
    } catch (e) {
      toast('登录失败，请重试');
      return false;
    }
  }

  Column buildDocModalBottomSheet(BuildContext context) {
    return Column(
      children: [
        Container(
          alignment: Alignment.centerLeft,
          padding: EdgeInsets.only(
            left: 32.px,
            right: 32.px,
          ),
          margin: EdgeInsets.only(
            bottom: 50.px,
          ),
          decoration: BoxDecoration(
            border: Border(
              bottom: BorderSide(
                color: Theme.of(context).dividerColor,
                width: 1.px,
              ),
            ),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              PFMediumText(
                BkDevopsAppi18n.of(context).$t('howToUseMailAccountTip'),
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 32.px,
                ),
              ),
              IconButton(
                icon: Icon(
                  Icons.close,
                ),
                onPressed: () {
                  Navigator.pop(context);
                },
              )
            ],
          ),
        ),
        Padding(
          padding: EdgeInsets.symmetric(horizontal: 32.px),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              for (final i in [1, 2, 3])
                PFText(
                  BkDevopsAppi18n.of(context).$t('mailAccountTip$i'),
                  style: TextStyle(
                    fontSize: 28.px,
                    height: 1.6,
                  ),
                ),
            ],
          ),
        )
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: BkAppBar(
        shadowColor: Colors.transparent,
        title: BkDevopsAppi18n.of(context).$t('tencentParnerLogin'),
      ),
      body: Container(
        decoration: BoxDecoration(
          border: Border(
            top: BorderSide(
              color: Theme.of(context).dividerColor,
              width: 1.px,
            ),
          ),
        ),
        padding: EdgeInsets.all(24.px),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                margin: EdgeInsets.only(
                  top: 138.px,
                  bottom: 138.px,
                ),
                child: Image.asset(
                  "assets/images/appLogo.png",
                  width: 130.px,
                  height: 130.px,
                ),
              ),
              PFText(
                BkDevopsAppi18n.of(context).$t('username'),
                style: labelStyle,
              ),
              Container(
                margin: EdgeInsets.only(
                  bottom: 30.px,
                ),
                height: 80.px,
                child: BkCurpertinoTextField(
                  controller: _usernameController,
                  autofocus: true,
                  placeholder: 'usernamePlaceholder',
                ),
              ),
              PFText(
                BkDevopsAppi18n.of(context).$t('password'),
                style: labelStyle,
              ),
              Container(
                margin: EdgeInsets.only(
                  bottom: 150.px,
                ),
                height: 80.px,
                child: BkCurpertinoTextField(
                  controller: _passwordController,
                  placeholder: 'passwordPlaceholder',
                  obscureText: true,
                ),
              ),
              SizedBox(
                height: 88.px,
                child: ElevatedButton(
                  onPressed: () async {
                    final result = await BkLoading.of(context).during(_login());

                    if (result && mounted) {
                      Future.delayed(Duration.zero, () {
                        Provider.of<User>(context, listen: false).fetchUser();
                        Navigator.of(context).pushNamedAndRemoveUntil(
                          BkDevopsApp.routePath,
                          (route) => false,
                        );
                      });
                    }
                  },
                  child: PFText(
                    BkDevopsAppi18n.of(context).$t('login'),
                    style: TextStyle(
                      color: Colors.white,
                    ),
                  ),
                ),
              ),
              SizedBox(height: 30),
              TextButton(
                onPressed: () {
                  showModalBottomSheet(
                      context: context,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.only(
                          topLeft: Radius.circular(30.px),
                          topRight: Radius.circular(30.px),
                        ),
                      ),
                      builder: (BuildContext context) {
                        return buildDocModalBottomSheet(context);
                      });
                },
                child: PFText(
                  BkDevopsAppi18n.of(context).$t('howToUseMailAccountTip'),
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: Theme.of(context).primaryColor,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class BkCurpertinoTextField extends StatefulWidget {
  const BkCurpertinoTextField({
    Key key,
    bool obscureText,
    bool autofocus,
    @required TextEditingController controller,
    @required String placeholder,
  })  : _controller = controller,
        _placeholder = placeholder,
        _autofocus = autofocus ?? false,
        _obscureText = obscureText ?? false,
        super(key: key);

  final TextEditingController _controller;
  final bool _obscureText;
  final bool _autofocus;
  final String _placeholder;

  @override
  _BkCurpertinoTextFieldState createState() => _BkCurpertinoTextFieldState();
}

class _BkCurpertinoTextFieldState extends State<BkCurpertinoTextField> {
  FocusNode _focus = new FocusNode();
  bool _isFocus;

  @override
  void initState() {
    super.initState();
    _focus.addListener(handleFocusChange);
    _isFocus = _focus.hasPrimaryFocus;
  }

  handleFocusChange() {
    setState(() {
      _isFocus = _focus.hasPrimaryFocus;
    });
  }

  @override
  Widget build(BuildContext context) {
    return CupertinoTextField.borderless(
      autofocus: widget._autofocus,
      obscureText: widget._obscureText,
      clearButtonMode: OverlayVisibilityMode.editing,
      placeholder:
          BkDevopsAppi18n.of(context).$t(widget._placeholder ?? 'placeholder'),
      style: TextStyle(
        fontSize: 28.px,
      ),
      controller: widget._controller,
      focusNode: _focus,
      showCursor: true,
      cursorColor: Theme.of(context).primaryColor,
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(
            width: 2.px,
            color: _isFocus
                ? Theme.of(context).primaryColor
                : Theme.of(context).dividerColor,
          ),
        ),
      ),
    );
  }
}
