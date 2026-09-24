import { motion } from 'framer-motion';
import { HelpCircle } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import './InsightCard.css';

export default function QuestionsCard({ questions }) {
  return (
    <motion.div
      className="glass-card insight-card insight-card--questions"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.3 }}
    >
      <div className="insight-card__accent insight-card__accent--rose" />
      <div className="insight-card__content">
        <div className="section-title">
          <HelpCircle size={20} className="icon" style={{ color: '#9a7d8a' }} />
          Open Questions
        </div>
        <div className="markdown-content insight-card__text">
          <ReactMarkdown>{questions || 'No open questions found.'}</ReactMarkdown>
        </div>
      </div>
    </motion.div>
  );
}
