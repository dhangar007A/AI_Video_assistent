import { Link, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { History, Sparkles } from 'lucide-react';
import './Navbar.css';

export default function Navbar() {
  const location = useLocation();

  return (
    <motion.nav
      className="navbar"
      initial={{ y: -80, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
    >
      <div className="navbar-inner container">
        <Link to="/" className="navbar-brand">
          <span className="brand-text">
            <span className="gradient-text">AI</span>_Video_Agent
          </span>
        </Link>

        <div className="navbar-links">
          <Link
            to="/"
            className={`nav-link ${location.pathname === '/' ? 'active' : ''}`}
          >
            <Sparkles size={16} />
            <span>New</span>
          </Link>
          <Link
            to="/history"
            className={`nav-link ${location.pathname === '/history' ? 'active' : ''}`}
          >
            <History size={16} />
            <span>History</span>
          </Link>
        </div>
      </div>
    </motion.nav>
  );
}
