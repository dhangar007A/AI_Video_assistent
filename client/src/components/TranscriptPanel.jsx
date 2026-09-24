import { useState } from 'react';
import { motion } from 'framer-motion';
import { FileText, Copy, Check, ChevronDown, ChevronUp } from 'lucide-react';
import './TranscriptPanel.css';

export default function TranscriptPanel({ transcript }) {
  const [expanded, setExpanded] = useState(false);
  const [copied, setCopied] = useState(false);

  const preview = transcript?.slice(0, 500) || '';
  const isLong = transcript?.length > 500;

  const handleCopy = async () => {
    await navigator.clipboard.writeText(transcript);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <motion.div
      className="glass-card transcript-panel"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.1 }}
    >
      <div className="transcript-panel__header">
        <div className="section-title">
          <FileText size={20} className="icon" style={{ color: 'var(--accent-secondary)' }} />
          Transcript
        </div>
        <button className="copy-btn" onClick={handleCopy}>
          {copied ? <Check size={16} /> : <Copy size={16} />}
          {copied ? 'Copied!' : 'Copy'}
        </button>
      </div>

      <div className="transcript-panel__body">
        <p className="transcript-text">
          {expanded ? transcript : preview}
          {!expanded && isLong && '...'}
        </p>
      </div>

      {isLong && (
        <button
          className="transcript-panel__toggle"
          onClick={() => setExpanded(!expanded)}
        >
          {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          {expanded ? 'Show less' : 'Show full transcript'}
        </button>
      )}
    </motion.div>
  );
}
