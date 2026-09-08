import { useVoice } from '../hooks/useVoice';
import { Mic, MicOff, Loader2 } from 'lucide-react';

export default function VoiceButton({ onTranscript, language = 'en-IN', className = '' }) {
  const { isSupported, isListening, transcript, error, startListening, stopListening, speak, clearTranscript } = useVoice(language);

  const handleStart = () => {
    clearTranscript();
    startListening();
  };

  const handleStop = () => {
    stopListening();
  };

  if (!isSupported) {
    return (
      <button
        className={`btn-ghost opacity-50 cursor-not-allowed ${className}`}
        disabled
        title="Voice input not supported"
      >
        <MicOff className="w-5 h-5" />
      </button>
    );
  }

  return (
    <div className="relative">
      <button
        onClick={isListening ? handleStop : handleStart}
        className={`btn-ghost ${isListening ? 'bg-red-50 text-red-600' : ''} ${className}`}
        aria-label={isListening ? 'Stop listening' : 'Start voice input'}
        disabled={isListening && !transcript}
      >
        {isListening ? (
          <Loader2 className="w-5 h-5 animate-spin" />
        ) : (
          <Mic className="w-5 h-5" />
        )}
      </button>
      
      {error && (
        <div className="absolute bottom-full left-0 mb-2 px-2 py-1 bg-red-600 text-white text-xs rounded shadow-lg whitespace-nowrap">
          {error}
        </div>
      )}
    </div>
  );
}

export function VoiceOutput({ text, language = 'en-IN', autoPlay = false }) {
  const { speak } = useVoice(language);

  const handleSpeak = () => {
    speak(text, language);
  };

  if (autoPlay) {
    // Auto-play handled by parent
    return null;
  }

  return (
    <button
      onClick={handleSpeak}
      className="btn-ghost p-2"
      aria-label="Read aloud"
    >
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 5a3 3 0 015.7 2.8M15 19a3 3 0 005.7-2.8M9 5a3 3 0 01-2.8 5.7M3 11a3 3 0 002.8 5.7" />
      </svg>
    </button>
  );
}