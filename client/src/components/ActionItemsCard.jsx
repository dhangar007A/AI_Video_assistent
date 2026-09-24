import { motion } from 'framer-motion';
import { ListChecks } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import './InsightCard.css';

export default function ActionItemsCard({ actionItems }) {
  return (
    <motion.div
      className="glass-card insight-card insight-card--actions"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.2 }}
    >
      <div className="insight-card__accent insight-card__accent--green" />
      <div className="insight-card__content">
        <div className="section-title">
          <ListChecks size={20} className="icon" style={{ color: '#6aad82' }} />
          Action Items
        </div>
        <div className="markdown-content insight-card__text">
          <ReactMarkdown>{actionItems || 'No action items found.'}</ReactMarkdown>
        </div>
      </div>
    </motion.div>
  );
}
