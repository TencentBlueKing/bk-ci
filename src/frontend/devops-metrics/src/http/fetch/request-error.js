export default class RequestError extends Error {
  constructor(code, message, response) {
    super();
    this.code = code;
    this.message = message;
    this.response = response;
  }
}
