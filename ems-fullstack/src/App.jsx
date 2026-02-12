import './App.css'
import Header from './component/Header'
import ListEmployeeComponent from './component/ListEmployeeComponent'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import EmployeeComponent from './component/EmployeeComponent'
import Login from './component/Login'
import PrivateRoute from './component/PrivateRoute'
import Dashboard from './component/Dashboard'
import Attendance from './component/Attendance'
import Leave from './component/Leave'
import Payroll from './component/Payroll'

function App() {

  return (
    <>
      <BrowserRouter>
        <Header></Header>
        <Routes>
          <Route path='/login' element={<Login />}></Route>

          <Route element={<PrivateRoute />}>
            <Route path='/' element={<Navigate to='/dashboard' replace />}></Route>
            <Route path='/dashboard' element={<Dashboard />}></Route>
            <Route path='/attendance' element={<Attendance />}></Route>
            <Route path='/leave' element={<Leave />}></Route>
          </Route>

          <Route element={<PrivateRoute allowedRoles={['ADMIN', 'HR', 'MANAGER']} />}>
            <Route path='/employees' element={<ListEmployeeComponent />}></Route>
            <Route path='/add-employee' element={<EmployeeComponent />}></Route>
            <Route path='/update-employee/:id' element={<EmployeeComponent />}></Route>
          </Route>

          <Route element={<PrivateRoute allowedRoles={['ADMIN', 'HR']} />}>
            <Route path='/payroll' element={<Payroll />}></Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </>
  )
}

export default App
