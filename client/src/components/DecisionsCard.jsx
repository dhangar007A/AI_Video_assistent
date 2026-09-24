import { motion } from 'framer-motion';
import { Lightbulb } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import './InsightCard.css';

export default function DecisionsCard({ decisions }) {
  return (
    <motion.div
      className="glass-card insight-card insight-card--decisions"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.25 }}
    >
      <div className="insight-card__accent insight-card__accent--amber" />
      <div className="insight-card__content">
        <div className="section-title">
          <Lightbulb size={20} className="icon" style={{ color: '#c4a35a' }} />
          Key Decisions
        </div>
        <div className="markdown-content insight-card__text">
          <ReactMarkdown>{decisions || 'No key decisions found.'}</ReactMarkdown>
        </div>
      </div>
    </motion.div>
  );
}
