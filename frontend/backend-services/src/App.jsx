import { Route, Routes, Navigate } from 'react-router-dom'
import './App.css'
import Sidebar from './components/Sidebar'
import ExponentialBackoff from './pages/ExponentialBackoff'
import { ToastProvider } from './components/ToastProvider'

function App() {

  return (
    <ToastProvider>
      <div className="app-layout">
        <Sidebar/>
        <main className="main-content">
          <Routes>
            <Route path='/' element={<Navigate to="/exponential-backoff" replace />} />
            <Route path='/exponential-backoff' element={<ExponentialBackoff />} />
          </Routes>
        </main>
      </div>
    </ToastProvider>
  )
}

export default App
