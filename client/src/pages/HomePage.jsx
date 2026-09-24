import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import HeroUpload from '../components/HeroUpload';
import ProcessingOverlay from '../components/ProcessingOverlay';
import MeetingHistory from '../components/MeetingHistory';
import useSSE from '../hooks/useSSE';
import { createMeeting, uploadMeeting } from '../utils/api';
import './HomePage.css';

export default function HomePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [meetingId, setMeetingId] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const { progress, currentStage, isComplete, error } = useSSE(meetingId);

  // When processing completes, navigate to results
  useEffect(() => {
    if (isComplete && meetingId) {
      const timer = setTimeout(() => {
        navigate(`/meeting/${meetingId}`);
      }, 800);
      return () => clearTimeout(timer);
    }
  }, [isComplete, meetingId, navigate]);

  const handleSubmit = async ({ source, file, language }) => {
    setLoading(true);
    try {
      let result;
      if (file) {
        result = await uploadMeeting(file, language);
      } else {
        result = await createMeeting(source, language);
      }
      setMeetingId(result.meetingId);
      setIsProcessing(true);
    } catch (err) {
      alert(`Error: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="mesh-bg" />

      <AnimatePresence>
        {isProcessing && !isComplete && (
          <ProcessingOverlay progress={progress} currentStage={currentStage} />
        )}
      </AnimatePresence>

      <div className="home-page">
        <HeroUpload onSubmit={handleSubmit} loading={loading} />

        <div className="home-page__history container">
          <MeetingHistory />
        </div>
      </div>

      {error && (
        <div className="home-page__error">
          <p>⚠️ {error}</p>
        </div>
      )}
    </>
  );
}
