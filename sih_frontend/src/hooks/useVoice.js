import { useState, useCallback, useRef, useEffect } from 'react';

export function useVoice(language = 'en-IN') {
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [error, setError] = useState(null);
  const recognitionRef = useRef(null);
  const isSupported = typeof window !== 'undefined' && ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window);

  useEffect(() => {
    if (!isSupported) return;

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.interimResults = true;
    recognition.lang = language;

    recognition.onstart = () => {
      setIsListening(true);
      setError(null);
    };

    recognition.onresult = (event) => {
      let finalTranscript = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        if (event.results[i].isFinal) {
          finalTranscript += event.results[i][0].transcript;
        }
      }
      if (finalTranscript) {
        setTranscript(finalTranscript.trim());
      }
    };

    recognition.onerror = (event) => {
      setError(event.error);
      setIsListening(false);
    };

    recognition.onend = () => {
      setIsListening(false);
    };

    recognitionRef.current = recognition;

    return () => {
      recognition.stop();
    };
  }, [language, isSupported]);

  const startListening = useCallback(() => {
    if (!isSupported) {
      setError('Voice input not supported in this browser');
      return;
    }
    if (recognitionRef.current && !isListening) {
      setTranscript('');
      recognitionRef.current.start();
    }
  }, [isSupported, isListening]);

  const stopListening = useCallback(() => {
    if (recognitionRef.current && isListening) {
      recognitionRef.current.stop();
    }
  }, [isListening]);

  const speak = useCallback((text, lang = language) => {
    if ('speechSynthesis' in window) {
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = lang;
      utterance.rate = 0.9;
      window.speechSynthesis.speak(utterance);
    }
  }, [language]);

  const clearTranscript = useCallback(() => {
    setTranscript('');
  }, []);

  return {
    isSupported,
    isListening,
    transcript,
    error,
    startListening,
    stopListening,
    speak,
    clearTranscript,
    setTranscript,
  };
}

export function extractEntities(transcript, villages, categories) {
  const lower = transcript.toLowerCase();
  const result = {
    village: null,
    category: null,
    capital: null,
    income: null,
  };

  // Extract village
  for (const village of villages) {
    if (lower.includes(village.toLowerCase())) {
      result.village = village;
      break;
    }
  }

  // Extract category
  for (const category of categories) {
    if (lower.includes(category.toLowerCase())) {
      result.category = category;
      break;
    }
  }

  // Extract numbers (lakh, crore, thousand, plain numbers)
  const lakhMatch = lower.match(/(\d+(?:\.\d+)?)\s*lakh/);
  if (lakhMatch) {
    result.capital = parseFloat(lakhMatch[1]) * 100000;
  } else {
    const croreMatch = lower.match(/(\d+(?:\.\d+)?)\s*crore/);
    if (croreMatch) {
      result.capital = parseFloat(croreMatch[1]) * 10000000;
    } else {
      const thousandMatch = lower.match(/(\d+(?:\.\d+)?)\s*thousand/);
      if (thousandMatch) {
        result.capital = parseFloat(thousandMatch[1]) * 1000;
      } else {
        const plainMatch = lower.match(/(\d{5,})/);
        if (plainMatch) {
          result.capital = parseInt(plainMatch[1]);
        }
      }
    }
  }

  // Extract desired income for goal seek
  const incomeMatch = lower.match(/(?:income|earn|want|need|target|goal).*?(\d+(?:\.\d+)?)\s*(?:thousand|lakh|k)?/);
  if (incomeMatch) {
    let val = parseFloat(incomeMatch[1]);
    if (lower.includes('lakh')) val *= 100000;
    else if (lower.includes('thousand') || lower.includes('k')) val *= 1000;
    result.income = val;
  }

  return result;
}