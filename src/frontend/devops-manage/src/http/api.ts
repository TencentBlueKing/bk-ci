import fetch from './fetch';

const apiPerfix = '/api';

export const getUser = () => fetch.get(`${apiPerfix}/user`);
