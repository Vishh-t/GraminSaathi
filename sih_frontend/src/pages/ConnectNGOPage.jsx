import { useNavigate } from 'react-router-dom';
import TopNav from '../components/TopNav';
import { HeartHandshake, Sparkles, ArrowLeft, GraduationCap, FileText, Users } from 'lucide-react';

const upcoming = [
  { icon: GraduationCap, title: 'Skill & business training', desc: 'Workshops run by partner NGOs in your local language.' },
  { icon: FileText, title: 'Documentation support', desc: 'Help filling loan and scheme paperwork correctly the first time.' },
  { icon: Users, title: 'Mentorship', desc: 'Get paired with someone who has run a similar business.' },
];

export default function ConnectNGOPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#f9fafb] flex flex-col">
      <TopNav subtitle="Connect NGO" />

      <main className="flex-1 max-w-3xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-14 sm:py-20 text-center">
        <div className="w-16 h-16 rounded-2xl bg-primary-50 flex items-center justify-center mx-auto mb-6">
          <HeartHandshake className="w-8 h-8 text-primary-700" />
        </div>

        <span className="pill bg-amber-50 text-amber-700 border border-amber-100 inline-flex items-center gap-1 mb-4">
          <Sparkles className="w-3.5 h-3.5" /> Coming soon
        </span>

        <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 mb-3">Connect with an NGO</h1>
        <p className="text-gray-500 text-sm sm:text-base leading-relaxed max-w-xl mx-auto mb-10">
          We're building a network of partner NGOs to support first-generation entrepreneurs
          beyond just the loan — training, paperwork help, and mentorship. This page is under
          active development and will go live soon.
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-left mb-10">
          {upcoming.map(({ icon: Icon, title, desc }) => (
            <div key={title} className="bg-white border border-gray-200 rounded-2xl p-5">
              <div className="w-9 h-9 rounded-xl bg-primary-50 flex items-center justify-center mb-3">
                <Icon className="w-4 h-4 text-primary-700" />
              </div>
              <p className="text-gray-900 font-medium text-sm">{title}</p>
              <p className="text-gray-500 text-xs mt-1">{desc}</p>
            </div>
          ))}
        </div>

        <button
          onClick={() => navigate('/dashboard')}
          className="btn-primary inline-flex items-center gap-2 !w-auto"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Dashboard
        </button>
      </main>
    </div>
  );
}
