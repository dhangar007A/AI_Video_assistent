import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Clock, Trash2, ExternalLink, Video, AlertCircle, Loader } from 'lucide-react';
import { getMeetings, deleteMeeting } from '../utils/api';
import './MeetingHistory.css';

export default function MeetingHistory() {
  const [meetings, setMeetings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchMeetings = async () => {
      try {
        const data = await getMeetings();
        setMeetings(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchMeetings();
  }, []);

  const handleDelete = async (id, e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!window.confirm('Delete this meeting analysis?')) return;
    await deleteMeeting(id);
    setMeetings((prev) => prev.filter((m) => m._id !== id));
  };

  if (loading) {
    return (
      <div className="history-loading">
        <Loader size={24} className="spinner" />
        <span>Loading meetings...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="history-error">
        <AlertCircle size={20} />
        <span>Failed to load meetings. Is the server running?</span>
      </div>
    );
  }

  if (meetings.length === 0) {
    return (
      <div className="history-empty">
        <Video size={40} style={{ opacity: 0.2 }} />
        <p>No meetings analyzed yet</p>
        <p className="history-empty__hint">
          Paste a YouTube URL or upload a recording to get started
        </p>
      </div>
    );
  }

  return (
    <div className="history">
      <h2 className="section-title" style={{ marginBottom: 20 }}>
        <Clock size={20} className="icon" style={{ color: 'var(--accent-secondary)' }} />
        Recent Meetings
      </h2>

      <div className="history__list">
        <AnimatePresence>
          {meetings.map((meeting, i) => (
            <motion.div
              key={meeting._id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ delay: i * 0.05 }}
            >
              <Link
                to={`/meeting/${meeting._id}`}
                className="glass-card history-item"
              >
                <div className="history-item__info">
                  <h3 className="history-item__title">
                    {meeting.title || 'Untitled Meeting'}
                  </h3>
                  <p className="history-item__source">
                    {meeting.source?.startsWith('http')
                      ? new URL(meeting.source).hostname
                      : meeting.source?.split(/[/\\]/).pop() || 'Unknown source'}
                  </p>
                  <span className="history-item__date">
                    {new Date(meeting.createdAt).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
                <div className="history-item__actions">
                  <span className={`badge badge-${meeting.status}`}>
                    {meeting.status}
                  </span>
                  <button
                    className="history-item__delete"
                    onClick={(e) => handleDelete(meeting._id, e)}
                    title="Delete"
                  >
                    <Trash2 size={14} />
                  </button>
                  <ExternalLink size={14} className="history-item__arrow" />
                </div>
              </Link>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </div>
  );
}
