import React, { useMemo, useState } from 'react'
import { savedEmployee, updateDataEmployee, editEmployee } from '../service/EmployeeService'
import '../style/employeeform.css'
import { useNavigate, useParams } from 'react-router-dom'
import { useEffect } from 'react'
import { useToasts } from './ToastProvider'

function EmployeeComponent() {
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')
    const [email, setEmail] = useState('')
    const [errors, setErrors] = useState({})
    const [isLoading, setIsLoading] = useState(false)
    const [isSaving, setIsSaving] = useState(false)

    const navigate = useNavigate()
    const { id } = useParams()
    const { pushToast } = useToasts()

    const ids = useMemo(() => ({
        firstName: 'employee-first-name',
        lastName: 'employee-last-name',
        email: 'employee-email',
    }), [])


    useEffect(() => {
        if (id) {
            setIsLoading(true)
            editEmployee(id).then((response) => {
                setFirstName(response.data.firstName || '');
                setLastName(response.data.lastName || '');
                setEmail(response.data.email || '');
            }).catch((error) => {
                console.error(error)
                pushToast({ type: 'error', message: 'Failed to load employee' })
            }).finally(() => {
                setIsLoading(false)
            })
        }
    }, [id])

    function isValidEmail(value) {
        const v = (value || '').trim();
        if (!v) return false;
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
    }

    function validate() {
        const next = {};
        if (!firstName.trim()) next.firstName = 'First name is required';
        if (!lastName.trim()) next.lastName = 'Last name is required';
        if (!email.trim()) next.email = 'Email is required';
        else if (!isValidEmail(email)) next.email = 'Enter a valid email address';
        return next;
    }

    function saveEmployee(e) {
        e.preventDefault()

        const nextErrors = validate();
        setErrors(nextErrors);
        if (Object.keys(nextErrors).length > 0) {
            pushToast({ type: 'error', message: 'Please fix the highlighted fields' })
            return;
        }

        setIsSaving(true)

        const employee = { firstName: firstName.trim(), lastName: lastName.trim(), email: email.trim() }
        if (id) {
            updateDataEmployee(id, employee).then(() => {
                pushToast({ type: 'success', message: 'Employee updated successfully' })
                navigate('/')
            }).catch(error => {
                console.error(error);
                const message = error?.response?.data?.message || 'Failed to update employee'
                pushToast({ type: 'error', message })
            }).finally(() => {
                setIsSaving(false)
            })
        } else {
            savedEmployee(employee).then(() => {
                pushToast({ type: 'success', message: 'Employee added successfully' })
                navigate("/")
            }).catch(error => {
                console.error(error);
                const message = error?.response?.data?.message || 'Failed to add employee'
                pushToast({ type: 'error', message })
            }).finally(() => {
                setIsSaving(false)
            })
        }
    }

    return (
        <>
            <div className='ems-form-page'>
                <div className='card ems-form-card'>
                    <div className='ems-form-header'>
                        {id ? <h4>Update Employee</h4> : <h4>Add Employee</h4>}
                    </div>
                    <div className="card-body">
                        <form onSubmit={saveEmployee} noValidate aria-label={id ? 'Update employee form' : 'Add employee form'}>
                            <div className='form-group mb-3'>
                                <label className='form-label' htmlFor={ids.firstName}>First name</label>
                                <input
                                    type="text"
                                    placeholder='Enter first name'
                                    value={firstName}
                                    className={`form-control ${errors.firstName ? 'is-invalid' : ''}`}
                                    onChange={(e) => setFirstName(e.target.value)}
                                    id={ids.firstName}
                                    aria-label='First name'
                                    aria-required='true'
                                    aria-invalid={errors.firstName ? 'true' : 'false'}
                                    disabled={isLoading || isSaving}
                                    required
                                />
                                {errors.firstName ? <div className="invalid-feedback">{errors.firstName}</div> : null}
                            </div>
                            <div className='form-group mb-3'>
                                <label className='form-label' htmlFor={ids.lastName}>Last name</label>
                                <input
                                    type="text"
                                    placeholder='Enter last name'
                                    value={lastName}
                                    className={`form-control ${errors.lastName ? 'is-invalid' : ''}`}
                                    onChange={(e) => setLastName(e.target.value)}
                                    id={ids.lastName}
                                    aria-label='Last name'
                                    aria-required='true'
                                    aria-invalid={errors.lastName ? 'true' : 'false'}
                                    disabled={isLoading || isSaving}
                                    required
                                />
                                {errors.lastName ? <div className="invalid-feedback">{errors.lastName}</div> : null}
                            </div>
                            <div className='form-group mb-3'>
                                <label className='form-label' htmlFor={ids.email}>Email</label>
                                <input
                                    type="email"
                                    placeholder='Enter email address'
                                    value={email}
                                    className={`form-control ${errors.email ? 'is-invalid' : ''}`}
                                    onChange={(e) => setEmail(e.target.value)}
                                    id={ids.email}
                                    aria-label='Email'
                                    aria-required='true'
                                    aria-invalid={errors.email ? 'true' : 'false'}
                                    disabled={isLoading || isSaving}
                                    required
                                />
                                {errors.email ? <div className="invalid-feedback">{errors.email}</div> : null}
                            </div>
                            <button className='btn btn-primary w-100' type='submit' disabled={isLoading || isSaving} style={{ padding: '0.6rem', fontWeight: 600 }}>
                                {isSaving ? (
                                    <span className="d-inline-flex align-items-center gap-2">
                                        <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                                        Saving…
                                    </span>
                                ) : id ? 'Update Employee' : 'Add Employee'}
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </>
    )
}

export default EmployeeComponent