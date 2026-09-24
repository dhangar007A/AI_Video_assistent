import { motion } from 'framer-motion';
import { BookOpen } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import './InsightCard.css';

export default function SummaryCard({ summary }) {
  return (
    <motion.div
      className="glass-card insight-card insight-card--summary"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.15 }}
    >
      <div className="insight-card__accent" />
      <div className="insight-card__content">
        <div className="section-title">
          <BookOpen size={20} className="icon" style={{ color: '#7a9cc6' }} />
          Summary
        </div>
        <div className="markdown-content insight-card__text">
          <ReactMarkdown>{summary || 'No summary available.'}</ReactMarkdown>
        </div>
      </div>
    </motion.div>
  );
}
