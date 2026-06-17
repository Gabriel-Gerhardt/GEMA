import { Route, Routes } from 'react-router-dom'
import Header from './components/Header'
import CreateAccount from './pages/CreateAccount'
import Home from './pages/Home'
import Login from './pages/Login'
import QrCodeGallery from './pages/QrCodeGallery'
import QrCodeInput from './pages/QrCodeInput'
import StyleGuide from './pages/StyleGuide'
import UserProfile from './pages/UserProfile'

function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/create-account" element={<CreateAccount />} />
        <Route path="/profile" element={<UserProfile />} />
        <Route path="/qr/scan" element={<QrCodeInput />} />
        <Route path="/qr/gallery" element={<QrCodeGallery />} />
        <Route path="/style-guide" element={<StyleGuide />} />
      </Routes>
    </>
  )
}

export default App
