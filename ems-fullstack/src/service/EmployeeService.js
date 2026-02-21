import axios from "axios";

import { getToken } from "./AuthService";

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
});

api.interceptors.request.use((config) => {
    const token = getToken();
    if (token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

const URL = "/api/emp";

export const listEmployees = () => api.get(URL);

export const savedEmployee = (employee) => api.post(URL, employee);

export const editEmployee = (employeeid) => {
    return api.get(URL + '/' + employeeid);
}

export const updateDataEmployee = (employeeid, employee) => {
    return api.put(URL + '/' + employeeid, employee);
}
export const deleteEmployee = (employeeId) => api.delete(URL + '/' + employeeId);