import { formatCurrency } from './format';

// Maps benefit.calculationType -> a short human badge label, mirroring the
// handoff doc's display rules (a ₹ figure for grant/capped types, an
// effective rate for rate-reduction, a cover % for guarantees, text for the
// rest - never summed into one number).
const CALCULATION_TYPE_BADGES = {
  flat_grant_or_cap: 'Grant',
  pct_of_cost_capped: 'Grant/Subsidy',
  pct_of_loan_capped: 'Subsidy on loan',
  pct_of_loan_uncapped: 'Subsidy on loan',
  per_unit_or_in_kind: 'Grant/Subsidy',
  interest_rate_reduction: 'Interest rate',
  guarantee_cover_pct: 'Loan guarantee',
  equity_or_uncapped: 'Equity / funding',
  composite: 'Multiple benefits',
  text_only_see_note: 'See details',
};

export function benefitBadge(benefit) {
  if (!benefit) return 'Scheme';
  return CALCULATION_TYPE_BADGES[benefit.calculationType] || 'Scheme';
}

/** Short headline for a card - a real ₹/rate/cover figure when known, else the calculator's own text. */
export function benefitHeadline(benefit) {
  if (!benefit) return null;
  if (benefit.amount != null) return formatCurrency(benefit.amount);
  if (benefit.effectiveInterestRatePct != null) return `${benefit.effectiveInterestRatePct}% p.a.`;
  if (benefit.guaranteeCoverPct != null) return `${benefit.guaranteeCoverPct}% cover`;
  return benefit.displayText;
}
