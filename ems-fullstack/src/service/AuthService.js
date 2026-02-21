import axios from 'axios';

const API_BASE_URL = 'http://localhost:8090';
const TOKEN_KEY = 'ems_token';

function base64UrlDecode(input) {
  const base64 = input.replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  return atob(padded);
}

export function decodeJwtPayload(token) {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;

  try {
    const json = base64UrlDecode(parts[1]);
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (!token) {
    localStorage.removeItem(TOKEN_KEY);
  } else {
    localStorage.setItem(TOKEN_KEY, token);
  }
  window.dispatchEvent(new Event('authChanged'));
}

export function logout() {
  setToken(null);
}

export function isTokenValid(token = getToken()) {
  const payload = decodeJwtPayload(token);
  if (!payload) return false;
  if (!payload.exp) return true;
  const nowSeconds = Math.floor(Date.now() / 1000);
  return payload.exp > nowSeconds;
}

export function getUsernameFromToken(token = getToken()) {
  const payload = decodeJwtPayload(token);
  return payload?.sub ?? null;
}

export function getRolesFromToken(token = getToken()) {
  const payload = decodeJwtPayload(token);
  const roles = payload?.roles;
  if (!roles) return [];
  return Array.isArray(roles) ? roles : Array.from(roles);
}

export function getEmployeeIdFromToken(token = getToken()) {
  const payload = decodeJwtPayload(token);
  const employeeId = payload?.employeeId;
  if (employeeId === undefined || employeeId === null) return null;
  const asNumber = Number(employeeId);
  return Number.isFinite(asNumber) ? asNumber : null;
}

export async function login(username, password) {
  const response = await axios.post(`${API_BASE_URL}/api/auth/login`, {
    username,
    password,
  });
  const token = response?.data?.token;
  setToken(token);
  return response.data;
}

export function hasAnyRole(allowedRoles, token = getToken()) {
  if (!allowedRoles || allowedRoles.length === 0) return true;
  const userRoles = getRolesFromToken(token);
  return allowedRoles.some((r) => userRoles.includes(r));
}
