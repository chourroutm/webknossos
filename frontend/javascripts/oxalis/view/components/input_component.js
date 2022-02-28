// @flow

import { Input } from "antd";
import * as React from "react";
import _ from "lodash";

type InputComponentProp = {
  onChange?: Function,
  onFocus?: Function,
  onBlur?: Function,
  onPressEnter?: Function,
  placeholder?: string,
  value: string,
  style?: any,
  isTextArea?: boolean,
};

type InputComponentState = {
  isFocused: boolean,
  currentValue: string,
};

// TODO Double check if we still need this once React v16 is released.

/*
 * A lightweight wrapper around <Input> to deal with a "jumping cursor" bug.
 * Modifying a input's value will always reset the cursor to the end even if
 * you are editing the middle of a string. Saving the input's value in state
 * remedies this. Rumors claim React v16 will fix this.
 * Inspired by https://github.com/facebook/react/issues/955#issuecomment-281802381
 * @class
 */
class InputComponent extends React.PureComponent<InputComponentProp, InputComponentState> {
  static defaultProps: InputComponentProp = {
    onChange: _.noop,
    onFocus: _.noop,
    onBlur: _.noop,
    onPressEnter: null,
    placeholder: "",
    value: "",
    style: {},
    isTextArea: false,
  };

  state = {
    isFocused: false,
    currentValue: this.props.value,
  };

  componentDidUpdate = (prevProps: InputComponentProp) => {
    if (!this.state.isFocused && prevProps.value !== this.props.value) {
      // eslint-disable-next-line react/no-did-update-set-state
      this.setState({ currentValue: this.props.value });
    }
  };

  handleChange = (e: SyntheticInputEvent<>) => {
    this.setState({ currentValue: e.target.value });
    if (this.props.onChange) {
      this.props.onChange(e);
    }
  };

  handleFocus = (e: SyntheticInputEvent<>) => {
    this.setState({ isFocused: true });
    if (this.props.onFocus) {
      this.props.onFocus(e);
    }
  };

  handleBlur = (e: SyntheticInputEvent<>) => {
    this.setState({ isFocused: false }, () => {
      if (this.props.onBlur) {
        this.props.onBlur(e);
      }
    });
  };

  blurYourself = () => (document.activeElement ? document.activeElement.blur() : null);

  blurOnEscape = (event: KeyboardEvent) => {
    if (event.key === "Escape") {
      event.preventDefault();
      this.blurYourself();
    }
  };

  render() {
    const { isTextArea, onPressEnter, ...inputProps } = this.props;
    const InputClass = isTextArea ? Input.TextArea : Input;
    const defaultOnPressEnter = !isTextArea ? this.blurYourself : null;
    return (
      <InputClass
        {...inputProps}
        onChange={this.handleChange}
        onFocus={this.handleFocus}
        onBlur={this.handleBlur}
        value={this.state.currentValue}
        onPressEnter={onPressEnter != null ? onPressEnter : defaultOnPressEnter}
        onKeyDown={this.blurOnEscape}
      />
    );
  }
}

export default InputComponent;
