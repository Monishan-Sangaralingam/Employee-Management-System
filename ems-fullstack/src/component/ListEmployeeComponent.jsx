import React, { useState, useEffect } from 'react'
import { listEmployees, deleteEmployee } from '../service/EmployeeService.js'
import { useNavigate } from 'react-router-dom'
import { useToasts } from './ToastProvider'


function ListEmployeeComponent() {
    const navigate = useNavigate();
    const { pushToast } = useToasts();

    const [employee, setEmployee] = useState([])
    const [isLoading, setIsLoading] = useState(false)
    const [deletingId, setDeletingId] = useState(null)

    useEffect(() => {
        getAllEmployee()
    }, [])

    function getAllEmployee() {
        setIsLoading(true)
        listEmployees().then((response) => {
            setEmployee(response.data)
        }).catch(error => {
            console.error(error);
            const message = error?.response?.data?.message || 'Failed to load employees'
            pushToast({ type: 'error', message })
        }).finally(() => {
            setIsLoading(false)
        })
    }

    function addNewEmployee() {
        navigate('/add-employee')
    }
    function updatehandler(id) {
        navigate(`/update-employee/${id}`)
    }
    function deletehandler(id) {
        setDeletingId(id)
        deleteEmployee(id).then(() => {
            pushToast({ type: 'success', message: 'Employee deleted' })
            getAllEmployee()
        }).catch(error => {
            console.error(error);
            const message = error?.response?.data?.message || 'Failed to delete employee'
            pushToast({ type: 'error', message })
        }).finally(() => {
            setDeletingId(null)
        })
    }

    return (
        <>
            <div className='container'>
                <h3 className='text-center mt-3'>List Of Employees</h3>
                <div className="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
                    <button className='btn btn-danger' onClick={addNewEmployee} aria-label="Add employee">
                        Add Employee
                    </button>
                    {isLoading ? (
                        <div className="text-muted d-inline-flex align-items-center gap-2" role="status" aria-live="polite">
                            <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                            Loading…
                        </div>
                    ) : null}
                </div>
                <table className='table table-success table-striped table-bordered table-hover'>
                    <thead>
                        <tr className='text-center'>
                            <th scope="col">Id</th>
                            <th scope="col">First Name</th>
                            <th scope="col">Last Name</th>
                            <th scope="col">Email</th>
                            <th scope='col'>Update</th>
                            <th scope='col'>Delete</th>
                        </tr>
                    </thead>
                    <tbody>

                        {
                            employee.map(item =>
                                <tr key={item.id} className='text-center'>
                                    <td>{item.id}</td>
                                    <td>{item.firstName}</td>
                                    <td>{item.lastName}</td>
                                    <td>{item.email}</td>
                                    <td>
                                        <button
                                            className='btn btn-success'
                                            onClick={() => updatehandler(item.id)}
                                            aria-label={`Update employee ${item.id}`}
                                            disabled={deletingId === item.id}
                                        >
                                            Update
                                        </button>
                                    </td>
                                    <td>
                                        <button
                                            className='btn btn-primary'
                                            onClick={() => deletehandler(item.id)}
                                            aria-label={`Delete employee ${item.id}`}
                                            disabled={deletingId === item.id}
                                        >
                                            {deletingId === item.id ? (
                                                <span className="d-inline-flex align-items-center gap-2">
                                                    <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                                                    Deleting…
                                                </span>
                                            ) : 'Delete'}
                                        </button>
                                    </td>
                                </tr>
                            )
                        }
                    </tbody>
                </table>
            </div>
        </>
    )
}

export default ListEmployeeComponent