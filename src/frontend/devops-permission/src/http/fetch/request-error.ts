export default class RequestError extends Error {
  public code: number;
  public message: string;
  public response: any;
  constructor(code: number, message: string, response?: any) {
    super();
    this.code = code;
    this.message = message;
    this.response = response;
  }
}
