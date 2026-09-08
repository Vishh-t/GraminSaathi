export default function RoadmapTimeline({ roadmap, className = '' }) {
  const { milestones } = roadmap;

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        First 365 Days Roadmap
      </h3>

      <div className="relative">
        {/* Vertical line */}
        <div className="absolute left-6 top-0 bottom-0 w-0.5 bg-gray-200" />

        <div className="space-y-6">
          {milestones.map((milestone, index) => (
            <div key={index} className="relative pl-16">
              <div className="absolute left-6 top-1 w-3 h-3 rounded-full bg-primary-600 border-4 border-white shadow-sm z-10" />
              <div className="pb-6 last:pb-0">
                <div className="flex items-start gap-3">
                  <div className="flex-shrink-0 w-16 text-right text-sm font-medium text-primary-600">
                    Month {milestone.month}
                  </div>
                  <div className="flex-1">
                    <p className="text-gray-700">{milestone.milestone}</p>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="mt-6 p-4 bg-primary-50 rounded-lg border border-primary-100">
        <p className="text-sm text-primary-800 font-medium mb-1">Roadmap Guidance:</p>
        <ul className="text-sm text-primary-700 space-y-1 pl-4 list-disc">
          <li>Month 1-2: Focus on setup and initial operations</li>
          <li>Month 3: First EMI due (moratorium ends) — critical cash flow checkpoint</li>
          <li>Month 6: Evaluate pricing, explore secondary revenue streams</li>
          <li>Month 12: Annual review against peer benchmarks</li>
        </ul>
      </div>
    </div>
  );
}