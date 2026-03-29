import { useState } from 'react'
import POSPage from './pages/POSPage'
import AdminPage from './pages/AdminPage'
import './App.css'

export default function App() {
  const [page, setPage] = useState('admin')

  return (
    <div className="app">
      {page === 'admin' ? (
        <AdminPage onGoPOS={() => setPage('pos')} />
      ) : (
        <POSPage onGoAdmin={() => setPage('admin')} />
      )}
    </div>
  )
}
