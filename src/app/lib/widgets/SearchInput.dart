import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:flutter/services.dart';

class SearchInput extends StatefulWidget {
  final Function(String, bool) handleChange;
  final String placeholder;
  final String searchValue;
  final double height;
  final bool autofocus;

  SearchInput({
    @required this.handleChange,
    this.placeholder,
    this.searchValue = '',
    this.height,
    this.autofocus = true,
  });

  _SearchInputState createState() => _SearchInputState();
}

class _SearchInputState extends State<SearchInput> {
  final TextEditingController _controller = new TextEditingController();
  bool editing = false;
  @override
  void initState() {
    super.initState();
    editing = widget.searchValue.isNotEmpty;
    setSearchValue(widget.searchValue);
  }

  @override
  void didUpdateWidget(SearchInput oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (_controller.text != widget.searchValue) {
      setSearchValue(widget.searchValue);
    }
  }

  void handleSubmit(String val) {
    widget.handleChange(val, true);
  }

  void handleChange(String val) {
    widget.handleChange(val, false);
    if (editing != val.isNotEmpty) {
      setState(() {
        editing = val.isNotEmpty;
      });
    }
  }

  void setSearchValue(String text) {
    _controller.text = text;
    _controller.selection = TextSelection.fromPosition(
      TextPosition(
        affinity: TextAffinity.downstream,
        offset: text.length,
      ),
    );
    setState(() {
      editing = text.isNotEmpty;
    });
  }

  void emptyValue() {
    setSearchValue('');
    handleSubmit('');
    setState(() {
      editing = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final height = widget.height ?? 68.px;

    return Container(
      height: height,
      decoration: BoxDecoration(
        color: theme.backgroundColor,
        borderRadius: BorderRadius.circular(
          34.px,
        ),
      ),
      child: TextField(
        controller: _controller,
        onSubmitted: handleSubmit,
        onChanged: handleChange,
        style: TextStyle(
          color: theme.secondaryHeaderColor,
          fontSize: 28.px,
        ),
        textInputAction: TextInputAction.search,
        textAlignVertical: TextAlignVertical.center,
        autofocus: widget.autofocus,
        autocorrect: false,
        decoration: InputDecoration(
          isDense: true,
          contentPadding: EdgeInsets.zero,
          prefixIcon: Icon(
            BkIcons.search,
            color: theme.secondaryHeaderColor,
            size: 36.px,
          ),
          suffixIcon: editing
              ? IconButton(
                  highlightColor: Colors.transparent,
                  splashColor: Colors.transparent,
                  onPressed: emptyValue,
                  padding: EdgeInsets.zero,
                  icon: Icon(
                    BkIcons.closeFill,
                    size: 36.px,
                    color: theme.hintColor,
                  ),
                )
              : SizedBox(),
          border: InputBorder.none,
          focusedBorder: InputBorder.none,
          enabledBorder: InputBorder.none,
          errorBorder: InputBorder.none,
          disabledBorder: InputBorder.none,
          hintText: widget.placeholder ?? 'Search',
          hintStyle: TextStyle(
            color: theme.hintColor,
            fontSize: 28.px,
          ),
        ),
      ),
    );
  }
}
