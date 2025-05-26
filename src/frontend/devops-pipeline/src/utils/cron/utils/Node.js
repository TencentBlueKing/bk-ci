export default class Node {
  static TYPE_ENUM = 1;
  static TYPE_RANG = 2;
  static TYPE_REPEAT = 3;
  static TYPE_RANG_REPEAT = 4;
  constructor ({
      type,
      value,
      min,
      max,
      repeatInterval
  }) {
      this.type = type
      this.value = value || ''
      this.min = min
      this.max = max
      this.repeatInterval = repeatInterval
  }
}
