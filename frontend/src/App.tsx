import type { ReactNode } from 'react'
import { Route, Routes } from 'react-router-dom'
import Header from './components/Header'
import { SunflowerMark } from './components/Logo'
import CreateAccount from './pages/CreateAccount'
import EmergencyGuideView from './pages/EmergencyGuideView'
import Home from './pages/Home'
import Login from './pages/Login'
import NotFound from './pages/NotFound'
import Onboarding from './pages/Onboarding'
import QrCodeDetail from './pages/QrCodeDetail'
import QrCodeGallery from './pages/QrCodeGallery'
import QrCodeInput from './pages/QrCodeInput'
import StyleGuide from './pages/StyleGuide'
import UserProfile from './pages/UserProfile'

/** Authenticated/internal shell: header + nav above the page content. */
function AppLayout({ children }: { children: ReactNode }) {
  return (
    <>
      <Header />
      {children}
    </>
  )
}

/** Minimal shell for pages reachable without being logged in (e.g. a
 * scanned QR code's public guide): just the brand mark, no nav/Login link. */
function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <>
      <div className="border-b border-border-warm-200 bg-base-white px-4 py-3">
        <SunflowerMark size={28} />
      </div>
      {children}
    </>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/q/:publicId" element={<PublicLayout><EmergencyGuideView /></PublicLayout>} />
      <Route path="/welcome" element={<PublicLayout><Onboarding /></PublicLayout>} />
      <Route path="/" element={<AppLayout><Home /></AppLayout>} />
      <Route path="/login" element={<AppLayout><Login /></AppLayout>} />
      <Route path="/create-account" element={<AppLayout><CreateAccount /></AppLayout>} />
      <Route path="/profile" element={<AppLayout><UserProfile /></AppLayout>} />
      <Route path="/qr/scan" element={<AppLayout><QrCodeInput /></AppLayout>} />
      <Route path="/qr/gallery" element={<AppLayout><QrCodeGallery /></AppLayout>} />
      <Route path="/qr/:publicId/edit" element={<AppLayout><QrCodeDetail /></AppLayout>} />
      <Route path="/style-guide" element={<AppLayout><StyleGuide /></AppLayout>} />
      <Route path="*" element={<PublicLayout><NotFound /></PublicLayout>} />
    </Routes>
  )
}

export default App
