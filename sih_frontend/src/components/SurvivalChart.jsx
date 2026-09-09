import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
} from 'recharts';
import { formatCurrency } from '../utils/format';
import { TrendingUp } from 'lucide-react';

export default function SurvivalChart({ cashCurve, deficitMonth, className = '' }) {
  const data = cashCurve.map(point => ({
    month: point.month,
    cash: point.cumulativeCash,
  }));

  const minCash = Math.min(...data.map(d => d.cash), 0);
  const maxCash = Math.max(...data.map(d => d.cash), 0);

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <TrendingUp className="w-5 h-5 text-primary-600" />
        Cumulative Cash Flow (24 Months)
      </h3>

      <div className="h-80">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
            <XAxis
              dataKey="month"
              tick={{ fontSize: 11, fill: '#6b7280' }}
              axisLine={{ stroke: '#e5e7eb' }}
              tickLine={false}
              interval={2}
            />
            <YAxis
              tick={{ fontSize: 11, fill: '#6b7280' }}
              axisLine={false}
              tickLine={false}
              tickFormatter={value => formatCurrency(value).replace('₹', '').replace(/,/g, ',')}
              width={60}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: '#fff',
                border: '1px solid #e5e7eb',
                borderRadius: '8px',
                boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
              }}
              formatter={(value, name) => [formatCurrency(value), 'Cumulative Cash']}
              labelFormatter={month => `Month ${month}`}
            />
            <ReferenceLine y={0} stroke="#9ca3af" strokeDasharray="3 3" strokeWidth={1} />
            {deficitMonth && (
              <ReferenceLine
                x={deficitMonth}
                stroke="#dc2626"
                strokeDasharray="5 5"
                strokeWidth={2}
                label={{
                  value: `Deficit: Month ${deficitMonth}`,
                  position: 'top',
                  fill: '#dc2626',
                  fontSize: 11,
                  fontWeight: 'bold',
                }}
              />
            )}
            <Line
              type="monotone"
              dataKey="cash"
              stroke="#16a34a"
              strokeWidth={2.5}
              dot={false}
              activeDot={{ r: 6, fill: '#16a34a' }}
              isAnimationActive={true}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>

      <div className="flex items-center justify-between mt-4 pt-4 border-t border-gray-100">
        <div className="flex items-center gap-4 text-sm text-gray-600">
          <div className="flex items-center gap-1.5">
            <div className="w-3 h-3 rounded-full bg-green-500" />
            <span>Positive Cash Flow</span>
          </div>
          <div className="flex items-center gap-1.5">
            <div className="w-3 h-3 rounded-full bg-red-500" />
            <span>Negative Cash Flow</span>
          </div>
          {deficitMonth && (
            <div className="flex items-center gap-1.5">
              <div className="w-3 h-3 rounded-full bg-red-500 border-2 border-white shadow-sm" />
              <span>Deficit Month</span>
            </div>
          )}
        </div>
        <div className="text-sm text-gray-500">
          Starting Capital: {formatCurrency(data[0]?.cash || 0)}
        </div>
      </div>
    </div>
  );
}