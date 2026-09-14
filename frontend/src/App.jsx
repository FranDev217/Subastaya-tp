import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import BilleteraPage from './pages/BilleteraPage'
import CatalogoPage from './pages/CatalogoPage'
import LoginPage from './pages/LoginPage'
import PublicarSubastaPage from './pages/PublicarSubastaPage'
import SalaSubastaPage from './pages/SalaSubastaPage'
import { ToastProvider } from './components/Toasts/ToastProvider'

function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <Routes>
          <Route path="/" element={<CatalogoPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/publicar" element={<PublicarSubastaPage />} />
          <Route path="/billetera" element={<BilleteraPage />} />
          <Route path="/subasta/:id" element={<SalaSubastaPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ToastProvider>
    </BrowserRouter>
  )
}

export default App
