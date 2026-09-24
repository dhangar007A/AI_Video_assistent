import { motion } from 'framer-motion';
import TranscriptPanel from './TranscriptPanel';
import SummaryCard from './SummaryCard';
import ActionItemsCard from './ActionItemsCard';
import DecisionsCard from './DecisionsCard';
import QuestionsCard from './QuestionsCard';
import './ResultsDashboard.css';

export default function ResultsDashboard({ meeting }) {
  if (!meeting) return null;

  return (
    <div className="dashboard">
      {/* Title Header */}
      <motion.div
        className="dashboard__header"
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="dashboard__title gradient-text">
          {meeting.title || 'Meeting Analysis'}
        </h1>
        <div className="dashboard__meta">
          <span className={`badge badge-${meeting.status}`}>
            {meeting.status}
          </span>
          <span className="dashboard__date">
            {new Date(meeting.createdAt).toLocaleDateString('en-US', {
              year: 'numeric',
              month: 'short',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
            })}
          </span>
        </div>
      </motion.div>

      {/* Bento Grid */}
      <div className="dashboard__grid">
        <div className="dashboard__col dashboard__col--main">
          <SummaryCard summary={meeting.summary} />
          <TranscriptPanel transcript={meeting.transcript} />
        </div>
        <div className="dashboard__col dashboard__col--side">
          <ActionItemsCard actionItems={meeting.actionItems} />
          <DecisionsCard decisions={meeting.keyDecisions} />
          <QuestionsCard questions={meeting.openQuestions} />
        </div>
      </div>
    </div>
  );
}
