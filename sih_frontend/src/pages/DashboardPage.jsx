import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { referenceAPI } from '../services/api';
import { useI18n } from '../i18n/i18n';
import { useAuth } from '../context/AuthContext';
import { useVoice, extractEntities } from '../hooks/useVoice';
import VoiceButton from '../components/VoiceButton';
import TopNav from '../components/TopNav';
import { formatCurrency } from '../utils/format';
import { dashboardHeroImage } from '../assets/dashboardHeroImage';
import {
  Send, MessageSquare, Mic, TrendingUp, CloudRain,
  Search, Wallet, LineChart, Target, ArrowRight, Sparkles,
  PiggyBank, HeartHandshake,
} from 'lucide-react';

const stripEmoji = (str) => str.replace(/^[^\sA-Za-z\u0900-\u097F]+\s*/u, '');

// Lightweight keyword-based intent routing for the chat box. This is
// deliberately simple (no backend NLU call) — it's meant to feel responsive
// for a demo, not to be a real intent classifier. Order matters: more
// specific/rarer keywords are checked first so e.g. "income goal" doesn't
// accidentally get swallowed by a broader match.
//
// Devanagari terms are matched as plain substrings (no \b word-boundary
// wrapping) because JS regex \b is defined in terms of \w ([A-Za-z0-9_]),
// which doesn't recognize Devanagari characters as "word" characters at all
// — wrapping them in \b...\b would silently never match.
function detectIntent(message) {
  const m = message.toLowerCase();

  const GOAL = /\b(goal|income target|target income|earn \u20b9|monthly income)\b/;
  const GOAL_HI = /\u0932\u0915\u094d\u0937\u094d\u092f|\u0906\u092e\u0926\u0928\u0940|\u0915\u092e\u093e\u0908/;

  const MAP = /\b(map|competitor|nearby|location)\b/;
  const MAP_HI = /\u0928\u0915\u094d\u0936\u093e|\u092a\u094d\u0930\u0924\u093f\u0938\u094d\u092a\u0930\u094d\u0927\u0940|\u0928\u091c\u093c\u0926\u0940\u0915/;

  const REPORT = /\b(report|pdf|download|saved)\b/;
  const REPORT_HI = /\u0930\u093f\u092a\u094b\u0930\u094d\u091f|\u0921\u093e\u0909\u0928\u0932\u094b\u0921/;

  const SIMULATE = /\b(simulat|survive|survival|what if|shock)\b/;
  const SIMULATE_HI = /\u0938\u093f\u092e\u0941\u0932\u0947\u0936\u0928|\u092e\u0902\u0926\u0940|\u091c\u094b\u0916\u093f\u092e/;

  const LOAN = /\b(loan|emi|borrow|credit|finance|interest rate|scheme)\b/;
  const LOAN_HI = /\u0932\u094b\u0928|\u0915\u0930\u094d\u095b|\u090b\u0923|\u092c\u094d\u092f\u093e\u091c|\u092f\u094b\u091c\u0928\u093e|\u0908\u090f\u092e\u0906\u0908/;

  const BUSINESS = /\b(business|start|idea|discover|shop|open a)\b/;
  const BUSINESS_HI = /\u0935\u094d\u092f\u0935\u0938\u093e\u092f|\u092c\u093f\u091c\u0928\u0947\u0938|\u0927\u0902\u0927|\u0926\u0941\u0915\u093e\u0928|\u0906\u0908\u0921\u093f\u092f\u093e|\u0922\u0942\u0902\u0922/;

  if (GOAL.test(m) || GOAL_HI.test(message)) {
    return { path: '/goal-seek', label: 'Goal Seek', labelHi: '\u0906\u092f \u0932\u0915\u094d\u0937\u094d\u092f \u0928\u093f\u0930\u094d\u0927\u093e\u0930\u0923' };
  }
  if (MAP.test(m) || MAP_HI.test(message)) {
    return { path: '/map', label: 'the Map', labelHi: '\u0928\u0915\u094d\u0936\u0947' };
  }
  if (REPORT.test(m) || REPORT_HI.test(message)) {
    return { path: '/reports', label: 'your Reports', labelHi: '\u0930\u093f\u092a\u094b\u0930\u094d\u091f\u094d\u0938' };
  }
  if (SIMULATE.test(m) || SIMULATE_HI.test(message)) {
    return { path: '/simulator', label: 'the Survival Simulator', labelHi: '\u0938\u0930\u094d\u0935\u093e\u0907\u0935\u0932 \u0938\u093f\u092e\u0941\u0932\u0947\u0936\u0928' };
  }
  if (LOAN.test(m) || LOAN_HI.test(message)) {
    return { path: '/analysis', label: 'Loan Analysis', labelHi: '\u0932\u094b\u0928 \u0935\u093f\u0936\u094d\u0932\u0947\u0937\u0923' };
  }
  if (BUSINESS.test(m) || BUSINESS_HI.test(message)) {
    return { path: '/discovery', label: 'Business Discovery', labelHi: '\u092c\u093f\u091c\u0928\u0947\u0938 \u0921\u093f\u0938\u094d\u0915\u0935\u0930\u0940' };
  }
  return null;
}

// The primary action is deliberately visually heavier than the rest — it's the
// natural first step for a first-time visitor (find a business before anything
// else can be evaluated). The remaining four are secondary, equal-weight entries.
const secondaryActions = [
  { key: 'checkLoan', icon: Wallet, action: 'analysis' },
  { key: 'analyzeBusiness', icon: LineChart, action: 'simulator' },
  { key: 'setGoal', icon: Target, action: 'goal-seek' },
  { key: 'talkToAI', icon: Mic, action: 'voice' },
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
      case 'simulator':
        navigate('/simulator');
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

    const intent = detectIntent(message);

    setTimeout(() => {
      const response = intent
        ? (language === 'hi' ? `\u0920\u0940\u0915 \u0939\u0948, \u0906\u092a\u0915\u094b ${intent.labelHi} \u092a\u0930 \u0932\u0947 \u091c\u093e \u0930\u0939\u0947 \u0939\u0948\u0902...` : `Sure, taking you to ${intent.label}...`)
        : (language === 'hi' ? '\u092e\u0948\u0902 \u0906\u092a\u0915\u0940 \u092e\u0926\u0926 \u0915\u0930 \u0938\u0915\u0924\u093e \u0939\u0942\u0902! \u0915\u0943\u092a\u092f\u093e \u090a\u092a\u0930 \u0926\u093f\u090f \u0917\u090f \u0915\u094d\u0935\u093f\u0915 \u090f\u0915\u094d\u0936\u0928 \u0915\u093e \u0909\u092a\u092f\u094b\u0917 \u0915\u0930\u0947\u0902\u0964' : "I can help you with that! Please use the quick actions above or visit the specific pages for detailed analysis.");

      setMessages(prev => [...prev, { role: 'bot', content: response }]);
      speak(response, language === 'hi' ? 'hi-IN' : 'en-IN');
      setIsLoading(false);

      // Small delay after the bot's reply so the person actually sees the
      // acknowledgement before the page changes out from under them, instead
      // of the chat message flashing and instantly vanishing.
      if (intent) {
        setTimeout(() => navigate(intent.path), 700);
      }
    }, 500);
  };

  const handleVoiceResult = (spokenText) => {
    if (spokenText) {
      setInput(spokenText);
      const villages = ['Ghoti', 'Bilaspur', 'Peddapuram'];
      const categories = ['Dairy', 'Tailoring', 'Retail / Kirana Store', 'Flour Mill'];
      const entities = extractEntities(spokenText, villages, categories);

      if (entities.village || entities.category || entities.capital) {
        setPrefillData(entities);
        speak(`Got it. Village: ${entities.village || 'not specified'}, Business: ${entities.category || 'not specified'}, Capital: ${entities.capital ? formatCurrency(entities.capital) : 'not specified'}`, language === 'hi' ? 'hi-IN' : 'en-IN');
      }
    }
  };

  const firstName = (user?.fullName || 'Friend').split(' ')[0];

  return (
    <div className="min-h-screen bg-[#f9fafb] flex flex-col relative isolate overflow-hidden">
      {/* Subtle decorative background — a single composited layer (fade + photo)
          instead of two stacked opacities, which were multiplying down to
          near-zero and rendering as effectively invisible. */}
      <div
        aria-hidden="true"
        className="pointer-events-none fixed inset-0 -z-10"
        style={{
          backgroundImage: `linear-gradient(to bottom, rgba(249,250,251,0.92) 0%, rgba(249,250,251,0.72) 35%, rgba(249,250,251,0.72) 65%, rgba(249,250,251,0.94) 100%), url(${dashboardHeroImage})`,
          backgroundSize: 'cover, cover',
          backgroundPosition: 'center, center 35%',
          backgroundRepeat: 'no-repeat, no-repeat',
          filter: 'grayscale(20%)',
        }}
      />

      <TopNav subtitle={t('app.tagline')} />

      {/* Greeting Banner */}
      <div className="relative bg-gradient-to-br from-primary-800 via-primary-700 to-primary-600">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-7 sm:py-9">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-5">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-full bg-white/15 border border-white/20 flex items-center justify-center text-white font-semibold text-lg shrink-0">
                {firstName.charAt(0).toUpperCase()}
              </div>
              <div>
                <h2 className="text-xl sm:text-2xl font-bold text-white leading-tight">
                  {t('dashboard.welcomeBack')}, {firstName}
                </h2>
                <p className="text-primary-100 text-sm mt-0.5">{t('dashboard.subtitle')}</p>
              </div>
            </div>
            <div className="flex gap-2 pl-16 sm:pl-0">
              <span className="pill bg-white/10 text-white border border-white/15">
                <TrendingUp className="w-3.5 h-3.5 mr-1.5" /> Good market conditions
              </span>
              <span className="pill bg-white/10 text-white border border-white/15">
                <CloudRain className="w-3.5 h-3.5 mr-1.5" /> Rain expected
              </span>
            </div>
          </div>
        </div>
      </div>

      <main className="flex-1 max-w-5xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-6 sm:py-8 space-y-6">
        {/* Quick Actions */}
        <section>
          <h3 className="text-sm font-semibold text-gray-500 mb-3">Where would you like to start?</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {/* Primary action — visually heavier, spans two columns on desktop */}
            <button
              onClick={() => handleQuickReply('discovery')}
              className="group text-left sm:col-span-2 lg:col-span-2 lg:row-span-2 bg-gradient-to-br from-primary-600 to-primary-700 rounded-2xl p-6 flex flex-col justify-between min-h-[168px] shadow-sm hover:shadow-md transition-shadow"
            >
              <div className="w-11 h-11 rounded-xl bg-white/15 flex items-center justify-center">
                <Search className="w-5 h-5 text-white" />
              </div>
              <div>
                <p className="text-white font-semibold text-lg mt-4">{stripEmoji(t('dashboard.quickReplies.findBusiness'))}</p>
                <p className="text-primary-100 text-sm mt-1 max-w-sm">{t('dashboard.quickReplies.findBusinessDesc')}</p>
                <span className="inline-flex items-center gap-1 text-white text-sm font-medium mt-4 group-hover:gap-2 transition-all">
                  Get started <ArrowRight className="w-4 h-4" />
                </span>
              </div>
            </button>

            {secondaryActions.map(({ key, icon: Icon, action }) => (
              <button
                key={key}
                onClick={() => handleQuickReply(action)}
                className="group text-left bg-white border border-gray-200 rounded-2xl p-5 flex flex-col justify-between min-h-[80px] shadow-sm hover:shadow-md hover:border-primary-200 transition-all"
              >
                <div className="flex items-start justify-between">
                  <div className="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center">
                    <Icon className="w-4 h-4 text-primary-700" />
                  </div>
                  <ArrowRight className="w-4 h-4 text-gray-300 group-hover:text-primary-600 group-hover:translate-x-0.5 transition-all" />
                </div>
                <div>
                  <p className="text-gray-900 font-medium text-sm mt-3">{stripEmoji(t(`dashboard.quickReplies.${key}`))}</p>
                  <p className="text-gray-500 text-xs mt-1">{t(`dashboard.quickReplies.${key}Desc`)}</p>
                </div>
              </button>
            ))}
          </div>
        </section>

        {/* Financial Tools */}
        <section>
          <h3 className="text-sm font-semibold text-gray-500 mb-3">Manage your money</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <a
              href="https://spend-smart-one-beta.vercel.app"
              target="_blank"
              rel="noopener noreferrer"
              className="group bg-white border border-gray-200 rounded-2xl p-5 flex flex-col justify-between min-h-[128px] shadow-sm hover:shadow-md hover:border-primary-200 transition-all"
            >
              <div className="flex items-start justify-between">
                <div className="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center">
                  <PiggyBank className="w-4 h-4 text-primary-700" />
                </div>
                <span className="pill bg-primary-50 text-primary-700 border border-primary-100 text-[10px]">UPI friendly</span>
              </div>
              <div>
                <p className="text-gray-900 font-medium text-sm mt-3">Track expenses with Expenzo</p>
                <p className="text-gray-500 text-xs mt-1">Log your daily spends, see where money goes, and move to UPI for safer, trackable payments.</p>
              </div>
              <span className="inline-flex items-center gap-1 text-primary-700 text-sm font-medium mt-3 group-hover:gap-2 transition-all">
                Open Expenzo <ArrowRight className="w-4 h-4" />
              </span>
            </a>

            <button
              onClick={() => navigate('/connect-ngo')}
              className="group text-left bg-white border border-gray-200 rounded-2xl p-5 flex flex-col justify-between min-h-[128px] shadow-sm hover:shadow-md hover:border-primary-200 transition-all"
            >
              <div className="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center">
                <HeartHandshake className="w-4 h-4 text-primary-700" />
              </div>
              <div>
                <p className="text-gray-900 font-medium text-sm mt-3">Connect with an NGO</p>
                <p className="text-gray-500 text-xs mt-1">Get support from partner NGOs for training, documentation help, and mentorship.</p>
              </div>
              <span className="inline-flex items-center gap-1 text-primary-700 text-sm font-medium mt-3 group-hover:gap-2 transition-all">
                Explore <ArrowRight className="w-4 h-4" />
              </span>
            </button>
          </div>
        </section>

        {/* Chat Card */}
        <section className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden flex flex-col">
          <div className="flex items-center gap-2.5 px-5 py-4 border-b border-gray-100">
            <div className="w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center shrink-0">
              <Sparkles className="w-4 h-4 text-white" />
            </div>
            <div>
              <p className="text-sm font-semibold text-gray-900">Ask GraminSaathi</p>
              <p className="text-xs text-gray-500">Type or speak in Hindi, English, or your local language</p>
            </div>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto space-y-4 p-5 h-72 sm:h-80" role="log" aria-live="polite">
            {messages.length === 0 && (
              <div className="flex items-start gap-3">
                <div className="w-9 h-9 rounded-full bg-primary-600 flex items-center justify-center flex-shrink-0 text-white">
                  <MessageSquare className="w-4 h-4" />
                </div>
                <div className="max-w-[85%] rounded-2xl rounded-tl-md bg-gray-50 border border-gray-200 px-4 py-3">
                  <p className="text-sm text-gray-700">
                    Ask about a business idea, a loan, or an income goal — or tap one of the quick actions above.
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
                    : 'bg-gray-50 text-gray-900 border border-gray-200 rounded-tl-md'
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
                <div className="bg-gray-50 text-gray-900 border border-gray-200 rounded-tl-md rounded-2xl px-4 py-3">
                  <div className="flex gap-1">
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Input Area */}
          <div className="border-t border-gray-100 p-4 bg-gray-50/50">
            <div className="relative flex items-center gap-2 bg-white border border-gray-300 rounded-2xl px-3 py-2 shadow-sm focus-within:ring-2 focus-within:ring-primary-500 focus-within:border-transparent">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), handleSend())}
                placeholder={t('dashboard.placeholder')}
                className="flex-1 outline-none text-sm bg-transparent min-h-[24px] px-1"
                disabled={isLoading}
              />
              <VoiceButton
                onTranscript={handleVoiceResult}
                language={language === 'hi' ? 'hi-IN' : 'en-IN'}
                className="shrink-0 !p-2"
              />
              <button
                onClick={handleSend}
                disabled={isLoading || (!input.trim() && !transcript.trim())}
                className="p-2 bg-primary-600 text-white rounded-full disabled:opacity-40 hover:bg-primary-700 transition-colors shrink-0"
                aria-label={t('dashboard.sendTooltip')}
              >
                <Send className="w-4 h-4" />
              </button>
            </div>

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
        </section>
      </main>
    </div>
  );
}
