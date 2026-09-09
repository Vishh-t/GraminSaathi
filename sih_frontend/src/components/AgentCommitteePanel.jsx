import { getVerdictColor, formatCurrency } from '../utils/format';
import { Users } from 'lucide-react';

export default function AgentCommitteePanel({ committee, className = '' }) {
  const { marketAgent, financeAgent, riskAgent, finalVerdict, verdictReason } = committee;

  const agents = [
    { ...marketAgent, icon: '📊', color: 'blue' },
    { ...financeAgent, icon: '💰', color: 'green' },
    { ...riskAgent, icon: '⚠️', color: 'orange' },
  ];

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <Users className="w-5 h-5 text-primary-600" />
        AI Investment Committee
      </h3>

      <div className="grid md:grid-cols-3 gap-4 mb-6">
        {agents.map((agent, index) => (
          <div key={index} className="border border-gray-200 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-2xl">{agent.icon}</span>
              <h4 className="font-medium text-gray-900">{agent.agent}</h4>
            </div>
            <p className="text-sm text-gray-600 mb-2">{agent.opinion}</p>
            <div className="pt-2 border-t border-gray-100">
              <p className="text-xs text-gray-500">Basis: {agent.basis}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Final Verdict */}
      <div className={`border rounded-lg p-5 ${getVerdictColor(finalVerdict)}`}>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium">Final Verdict</p>
            <p className="text-2xl font-bold mt-1">{finalVerdict}</p>
          </div>
          <svg className="w-10 h-10 text-current opacity-80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <p className="mt-3 text-sm">{verdictReason}</p>
      </div>
    </div>
  );
}