import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link2, Upload, Globe, Zap, ArrowRight } from 'lucide-react';
import GlowButton from './GlowButton';
import './HeroUpload.css';

export default function HeroUpload({ onSubmit, loading }) {
  const [mode, setMode] = useState('url'); // 'url' | 'file'
  const [url, setUrl] = useState('');
  const [file, setFile] = useState(null);
  const [language, setLanguage] = useState('english');
  const [isDragging, setIsDragging] = useState(false);
  const fileRef = useRef();

  const handleSubmit = () => {
    if (mode === 'url' && url.trim()) {
      onSubmit({ source: url.trim(), language });
    } else if (mode === 'file' && file) {
      onSubmit({ file, language });
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const dropped = e.dataTransfer.files[0];
    if (dropped) {
      setFile(dropped);
      setMode('file');
    }
  };

  const canSubmit =
    (mode === 'url' && url.trim().length > 0) ||
    (mode === 'file' && file !== null);

  return (
    <div className="hero">
      {/* Animated orbs */}
      <div className="hero__orbs">
        <div className="orb orb--1" />
        <div className="orb orb--2" />
        <div className="orb orb--3" />
      </div>

      <motion.div
        className="hero__content"
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
      >
        <motion.div
          className="hero__badge"
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.2 }}
        >
          <Zap size={14} />
          <span>AI-Powered Meeting Intelligence</span>
        </motion.div>

        <h1 className="hero__title">
          Transform any video into
          <br />
          <span className="gradient-text">actionable insights</span>
        </h1>

        <p className="hero__subtitle">
          Drop a YouTube link or upload a recording. Get transcripts, summaries,
          action items, and chat with your meetings — all powered by AI.
        </p>

        {/* Mode Switcher */}
        <div className="hero__mode-switch">
          <button
            className={`mode-tab ${mode === 'url' ? 'active' : ''}`}
            onClick={() => setMode('url')}
          >
            <Link2 size={16} /> Paste URL
          </button>
          <button
            className={`mode-tab ${mode === 'file' ? 'active' : ''}`}
            onClick={() => setMode('file')}
          >
            <Upload size={16} /> Upload File
          </button>
        </div>

        {/* Input Area */}
        <AnimatePresence mode="wait">
          {mode === 'url' ? (
            <motion.div
              key="url"
              className="hero__input-wrap"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              transition={{ duration: 0.3 }}
            >
              <div className="url-input-group">
                <Globe size={20} className="url-icon" />
                <input
                  id="youtube-url-input"
                  type="url"
                  className="input url-input"
                  placeholder="https://www.youtube.com/watch?v=..."
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && canSubmit && handleSubmit()}
                />
              </div>
            </motion.div>
          ) : (
            <motion.div
              key="file"
              className={`hero__dropzone ${isDragging ? 'dragging' : ''} ${file ? 'has-file' : ''}`}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.3 }}
              onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
              onDragLeave={() => setIsDragging(false)}
              onDrop={handleDrop}
              onClick={() => fileRef.current?.click()}
            >
              <input
                ref={fileRef}
                type="file"
                accept="audio/*,video/*"
                hidden
                onChange={(e) => setFile(e.target.files[0])}
              />
              <Upload size={32} className="drop-icon" />
              {file ? (
                <p className="drop-text">{file.name}</p>
              ) : (
                <p className="drop-text">
                  Drag & drop or <span className="drop-link">browse</span>
                </p>
              )}
              <p className="drop-hint">Supports MP4, MP3, WAV, WebM, and more</p>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Language Selector */}
        <div className="hero__language">
          <label className="lang-label">Language:</label>
          <div className="lang-options">
            <button
              className={`lang-chip ${language === 'english' ? 'active' : ''}`}
              onClick={() => setLanguage('english')}
            >
              🇬🇧 English
            </button>
            <button
              className={`lang-chip ${language === 'hinglish' ? 'active' : ''}`}
              onClick={() => setLanguage('hinglish')}
            >
              🇮🇳 Hinglish
            </button>
          </div>
        </div>

        {/* Submit Button */}
        <GlowButton
          onClick={handleSubmit}
          disabled={!canSubmit}
          loading={loading}
          size="lg"
          icon={<ArrowRight size={20} />}
        >
          Analyze Meeting
        </GlowButton>
      </motion.div>
    </div>
  );
}
