import { api } from './axiosInstance';

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