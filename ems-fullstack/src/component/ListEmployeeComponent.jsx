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
            <div className='container ems-page-container'>
                <div className='ems-page-header d-flex align-items-center justify-content-between flex-wrap gap-2'>
                    <h3>Employees</h3>
                    <div className="d-flex align-items-center gap-3">
                        {isLoading ? (
                            <div className="text-muted d-inline-flex align-items-center gap-2" role="status" aria-live="polite">
                                <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                                Loading…
                            </div>
                        ) : null}
                        <button className='btn btn-primary' onClick={addNewEmployee} aria-label="Add employee">
                            + Add Employee
                        </button>
                    </div>
                </div>

                <div className="card">
                    <div className="card-body p-0">
                        <div className="table-responsive">
                            <table className='ems-table'>
                                <thead>
                                    <tr>
                                        <th scope="col">Id</th>
                                        <th scope="col">First Name</th>
                                        <th scope="col">Last Name</th>
                                        <th scope="col">Email</th>
                                        <th scope='col' className='text-center'>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {employee.length === 0 && !isLoading ? (
                                        <tr>
                                            <td colSpan={5} className="text-center text-muted" style={{ padding: '2rem' }}>
                                                No employees found. Click "Add Employee" to get started.
                                            </td>
                                        </tr>
                                    ) : null}
                                    {
                                        employee.map(item =>
                                            <tr key={item.id}>
                                                <td style={{ fontWeight: 600, color: 'var(--ems-primary)' }}>{item.id}</td>
                                                <td>{item.firstName}</td>
                                                <td>{item.lastName}</td>
                                                <td>{item.email}</td>
                                                <td className='text-center'>
                                                    <div className="d-inline-flex gap-2">
                                                        <button
                                                            className='btn btn-sm btn-outline-primary'
                                                            onClick={() => updatehandler(item.id)}
                                                            aria-label={`Update employee ${item.id}`}
                                                            disabled={deletingId === item.id}
                                                        >
                                                            Edit
                                                        </button>
                                                        <button
                                                            className='btn btn-sm btn-outline-danger'
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
                                                    </div>
                                                </td>
                                            </tr>
                                        )
                                    }
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </>
    )
}

export default ListEmployeeComponent