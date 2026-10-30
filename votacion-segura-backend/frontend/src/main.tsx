import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { App } from './app/App'
import { AuthProvider } from './providers/AuthProvider'
import { QueryProvider } from './providers/QueryProvider'
import { ToastProvider } from './providers/ToastProvider'
import './styles.css'

createRoot(document.getElementById('root')!).render(<StrictMode><BrowserRouter><QueryProvider><AuthProvider><ToastProvider><App /></ToastProvider></AuthProvider></QueryProvider></BrowserRouter></StrictMode>)
