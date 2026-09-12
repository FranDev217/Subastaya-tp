import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import CatalogoPage from './pages/CatalogoPage'
import LoginPage from './pages/LoginPage'
import PublicarSubastaPage from './pages/PublicarSubastaPage'
import SalaSubastaPage from './pages/SalaSubastaPage'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<CatalogoPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/publicar" element={<PublicarSubastaPage />} />
        <Route path="/subasta/:id" element={<SalaSubastaPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
