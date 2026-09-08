import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { referenceAPI } from '../services/api';
import { useI18n } from '../i18n/i18n';
import { useAuth } from '../context/AuthContext';
import { useVoice, extractEntities } from '../hooks/useVoice';
import VoiceButton from '../components/VoiceButton';
import TopNav from '../components/TopNav';
import { formatCurrency } from '../utils/format';
import { Send, MessageSquare, Mic, Paperclip, TrendingUp, CloudRain } from 'lucide-react';

const quickReplies = [
  { key: 'findBusiness', icon: '🔍', action: 'discovery' },
  { key: 'checkLoan', icon: '💰', action: 'analysis' },
  { key: 'analyzeBusiness', icon: '📊', action: 'analysis' },
  { key: 'setGoal', icon: '🎯', action: 'goal-seek' },
  { key: 'talkToAI', icon: '🎙️', action: 'voice' },
];

export default function DashboardPage() {
  const { t, language } = useI18n();
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [prefillData, setPrefillData] = useState(null);

  const { isListening, transcript, error, startListening, speak, clearTranscript } = useVoice(language === 'hi' ? 'hi-IN' : 'en-IN');

  const handleQuickReply = (action) => {
    switch (action) {
      case 'discovery':
        navigate('/discovery');
        break;
      case 'analysis':
        if (prefillData) {
          navigate(`/analysis?village=${prefillData.village}&category=${prefillData.category}&capital=${prefillData.capital}`);
        } else {
          navigate('/analysis');
        }
        break;
      case 'goal-seek':
        navigate('/goal-seek');
        break;
      case 'voice':
        startListening();
        break;
    }
  };

  const handleSend = async () => {
    if (!input.trim() && !transcript.trim()) return;

    const message = input || transcript;
    setMessages(prev => [...prev, { role: 'user', content: message }]);
    setInput('');
    clearTranscript();
    setIsLoading(true);

    setTimeout(() => {
      let response = "I can help you with that! Please use the quick reply buttons or visit the specific pages for detailed analysis.";

      if (message.toLowerCase().includes('loan') || message.toLowerCase().includes('emi')) {
        response = "For loan analysis, please use the 'Check my loan' or 'Analyze a business' buttons, or visit the Analysis page.";
      } else if (message.toLowerCase().includes('business') || message.toLowerCase().includes('start')) {
        response = "To find the best business for your location, use 'Find best business' or visit the Discovery page.";
      } else if (message.toLowerCase().includes('goal') || message.toLowerCase().includes('income') || message.toLowerCase().includes('target')) {
        response = "For income goal planning, use 'Set an income goal' or visit the Goal Seek page.";
      }

      setMessages(prev => [...prev, { role: 'bot', content: response }]);
      speak(response, language === 'hi' ? 'hi-IN' : 'en-IN');
      setIsLoading(false);
    }, 500);
  };

  const handleVoiceResult = () => {
    if (transcript) {
      setInput(transcript);
      const villages = ['Ghoti', 'Bilaspur', 'Peddapuram'];
      const categories = ['Dairy', 'Tailoring', 'Retail / Kirana Store', 'Flour Mill'];
      const entities = extractEntities(transcript, villages, categories);

      if (entities.village || entities.category || entities.capital) {
        setPrefillData(entities);
        speak(`Got it. Village: ${entities.village || 'not specified'}, Business: ${entities.category || 'not specified'}, Capital: ${entities.capital ? formatCurrency(entities.capital) : 'not specified'}`, language === 'hi' ? 'hi-IN' : 'en-IN');
      }
    }
  };

  return (
    <div className="min-h-screen bg-[#f9fafb] flex flex-col">
      <TopNav subtitle={t('app.tagline')} />

      {/* Greeting Banner */}
      <div className="relative bg-gradient-to-r from-primary-800 to-primary-600 overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_70%_30%,rgba(255,255,255,0.08),transparent_60%)]" />
        <div className="relative max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
            <div>
              <h2 className="text-2xl sm:text-3xl font-bold text-white">
                {t('dashboard.welcomeBack')}, {user?.fullName || 'Friend'}!
              </h2>
              <p className="text-primary-100 mt-1">{t('dashboard.subtitle')}</p>
            </div>
            <div className="flex gap-2">
              <span className="pill bg-white/15 text-white backdrop-blur-sm">
                <TrendingUp className="w-3.5 h-3.5 mr-1" /> Good Market
              </span>
              <span className="pill bg-white/15 text-white backdrop-blur-sm">
                <CloudRain className="w-3.5 h-3.5 mr-1" /> Rain Expected
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Chat Area */}
      <main className="flex-1 overflow-hidden flex flex-col max-w-3xl mx-auto w-full p-4">
        <div className="flex justify-center my-3">
          <span className="pill bg-surface-container text-gray-500">Today</span>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto space-y-4 mb-6" role="log" aria-live="polite">
          {messages.length === 0 && (
            <div className="flex items-start gap-3">
              <div className="w-9 h-9 rounded-full bg-primary-600 flex items-center justify-center flex-shrink-0 text-white">
                <MessageSquare className="w-4 h-4" />
              </div>
              <div className="max-w-[85%] rounded-2xl rounded-tl-md bg-white border border-gray-200 shadow-sm px-4 py-3">
                <p className="text-sm text-gray-800">
                  {t('dashboard.subtitle')}. Try asking about a business idea, a loan, or an income goal — or use a quick reply below.
                </p>
              </div>
            </div>
          )}

          {messages.map((msg, index) => (
            <div key={index} className={`flex items-start gap-3 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
              {msg.role === 'bot' && (
                <div className="w-9 h-9 rounded-full bg-primary-600 flex items-center justify-center flex-shrink-0 text-white">
                  <MessageSquare className="w-4 h-4" />
                </div>
              )}
              <div className={`max-w-[80%] rounded-2xl px-4 py-3 ${
                msg.role === 'user'
                  ? 'bg-primary-600 text-white rounded-br-md'
                  : 'bg-white text-gray-900 border border-gray-200 rounded-tl-md shadow-sm'
              }`}>
                <p className="text-sm">{msg.content}</p>
              </div>
              {msg.role === 'user' && (
                <div className="w-9 h-9 rounded-full bg-gray-200 flex items-center justify-center flex-shrink-0 text-gray-600 text-xs font-semibold">
                  {(user?.fullName || 'U').charAt(0).toUpperCase()}
                </div>
              )}
            </div>
          ))}

          {isLoading && (
            <div className="flex items-start gap-3">
              <div className="w-9 h-9 rounded-full bg-primary-600 flex items-center justify-center flex-shrink-0 text-white">
                <MessageSquare className="w-4 h-4" />
              </div>
              <div className="bg-white text-gray-900 border border-gray-200 rounded-tl-md rounded-2xl px-4 py-3 shadow-sm">
                <div className="flex gap-1">
                  <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                  <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                  <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Quick Replies */}
        <div className="flex flex-wrap gap-2 mb-4">
          {quickReplies.map((reply, i) => (
            <button
              key={reply.key}
              onClick={() => handleQuickReply(reply.action)}
              className={i === 0 ? 'btn-primary !min-h-0 !py-2 text-sm gap-1.5' : 'btn-secondary !py-2 text-sm flex items-center gap-1.5'}
            >
              <span>{reply.icon}</span>
              <span>{t(`dashboard.quickReplies.${reply.key}`).replace(/^[^\sA-Za-z]+\s*/u, '')}</span>
            </button>
          ))}
        </div>

        {/* Input Area */}
        <div className="border-t border-gray-200 pt-4">
          <div className="flex items-center gap-3 bg-white border border-gray-300 rounded-2xl px-3 py-2 shadow-sm focus-within:ring-2 focus-within:ring-primary-500 focus-within:border-transparent">
            <Paperclip className="w-5 h-5 text-gray-400 shrink-0" />
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), handleSend())}
              placeholder={t('dashboard.placeholder')}
              className="flex-1 outline-none text-sm bg-transparent min-h-[24px]"
              disabled={isLoading}
            />
            <button
              onClick={handleSend}
              disabled={isLoading || (!input.trim() && !transcript.trim())}
              className="p-2 bg-primary-600 text-white rounded-full disabled:opacity-40 hover:bg-primary-700 transition-colors shrink-0"
              aria-label={t('dashboard.sendTooltip')}
            >
              <Send className="w-4 h-4" />
            </button>
            <VoiceButton
              onTranscript={handleVoiceResult}
              language={language === 'hi' ? 'hi-IN' : 'en-IN'}
              className="shrink-0"
            />
          </div>

          <p className="text-center text-xs text-gray-400 mt-2">You can speak in Hindi, English, or your local language.</p>

          {isListening && (
            <div className="mt-2 flex items-center justify-center gap-2 text-red-600 text-sm animate-pulse">
              <Mic className="w-4 h-4" />
              <span>{t('dashboard.voiceListening')}</span>
            </div>
          )}

          {error && (
            <div className="mt-2 text-center text-red-600 text-sm">
              {t('dashboard.voiceError')}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
