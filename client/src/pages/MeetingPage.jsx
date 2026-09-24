import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft, Loader } from 'lucide-react';
import ResultsDashboard from '../components/ResultsDashboard';
import ChatInterface from '../components/ChatInterface';
import { getMeeting } from '../utils/api';
import './MeetingPage.css';

export default function MeetingPage() {
  const { id } = useParams();
  const [meeting, setMeeting] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchMeeting = async () => {
      try {
        const data = await getMeeting(id);
        setMeeting(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchMeeting();
  }, [id]);

  if (loading) {
    return (
      <div className="meeting-page__loading">
        <Loader size={32} className="spinner" />
        <span>Loading meeting...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="meeting-page__error">
        <p>Failed to load meeting: {error}</p>
        <Link to="/" className="btn btn-ghost">
          <ArrowLeft size={16} /> Back to Home
        </Link>
      </div>
    );
  }

  return (
    <>
      <div className="mesh-bg" />

      <motion.div
        className="meeting-page"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.4 }}
      >
        <div className="meeting-page__back container">
          <Link to="/" className="back-link">
            <ArrowLeft size={16} />
            <span>Back to Home</span>
          </Link>
        </div>

        <ResultsDashboard meeting={meeting} />

        {meeting.status === 'completed' && (
          <ChatInterface
            meetingId={meeting._id}
            chatHistory={meeting.chatHistory || []}
          />
        )}
      </motion.div>
    </>
  );
}
