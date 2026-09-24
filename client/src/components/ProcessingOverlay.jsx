import { motion } from 'framer-motion';
import { Download, Mic, FileText, ListChecks, Database, Check } from 'lucide-react';
import './ProcessingOverlay.css';

const stages = [
  { key: 'downloading', label: 'Downloading Audio', icon: Download },
  { key: 'transcribing', label: 'Transcribing', icon: Mic },
  { key: 'summarizing', label: 'Summarizing', icon: FileText },
  { key: 'extracting', label: 'Extracting Insights', icon: ListChecks },
  { key: 'building_rag', label: 'Building Knowledge Base', icon: Database },
];

export default function ProcessingOverlay({ progress, currentStage }) {
  const completedStages = progress.filter((p) => p.done).map((p) => p.stage);
  const currentIdx = stages.findIndex((s) => s.key === currentStage);

  return (
    <motion.div
      className="processing"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
    >
      <div className="processing__bg">
        <div className="processing__wave processing__wave--1" />
        <div className="processing__wave processing__wave--2" />
        <div className="processing__wave processing__wave--3" />
      </div>

      <div className="processing__content">
        <motion.div
          className="processing__spinner-ring"
          animate={{ rotate: 360 }}
          transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
        >
          <div className="ring-glow" />
        </motion.div>

        <h2 className="processing__title">Analyzing your meeting...</h2>
        <p className="processing__subtitle">
          This may take a few minutes depending on the video length
        </p>

        <div className="processing__stages">
          {stages.map((stage, idx) => {
            const isCompleted = completedStages.includes(stage.key);
            const isCurrent = stage.key === currentStage && !isCompleted;
            const isPending = idx > currentIdx && !isCompleted;
            const Icon = stage.icon;

            return (
              <motion.div
                key={stage.key}
                className={`stage ${isCompleted ? 'completed' : ''} ${isCurrent ? 'current' : ''} ${isPending ? 'pending' : ''}`}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: idx * 0.1 }}
              >
                <div className="stage__icon">
                  {isCompleted ? (
                    <motion.div
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      transition={{ type: 'spring', stiffness: 400 }}
                    >
                      <Check size={18} />
                    </motion.div>
                  ) : (
                    <Icon size={18} />
                  )}
                </div>
                <span className="stage__label">{stage.label}</span>
                {isCurrent && <span className="stage__pulse" />}
              </motion.div>
            );
          })}
        </div>
      </div>
    </motion.div>
  );
}
