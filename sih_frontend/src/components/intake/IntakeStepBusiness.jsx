import { Briefcase, Wheat } from 'lucide-react';
import Select from '../Select';
import { ChipSingleSelect, ChipBooleanToggle } from './Chip';
import {
  BUSINESS_STAGE_OPTIONS, ENTERPRISE_TYPE_OPTIONS, OCCUPATION_OPTIONS,
  SECTOR_OPTIONS, SUB_SECTOR_OPTIONS, ARTISAN_TYPE_OPTIONS, SUB_CASTE_OPTIONS,
} from './constants';

function NumberField({ label, value, onChange, placeholder, suffix }) {
  return (
    <div>
      <label className="text-sm font-medium text-gray-700 mb-1.5 block">{label}</label>
      <div className="relative max-w-[220px]">
        <input
          type="number"
          min="0"
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value === '' ? null : parseFloat(e.target.value))}
          placeholder={placeholder}
          className="input-field"
        />
        {suffix && (
          <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-gray-400">
            {suffix}
          </span>
        )}
      </div>
    </div>
  );
}

/** Step 3 of 3 — business stage, sector, and the numeric facts benefit formulas need. */
export default function IntakeStepBusiness({ profile, onChange }) {
  const set = (key) => (value) => onChange({ ...profile, [key]: value });

  return (
    <div className="space-y-6">
      <div>
        <label className="text-sm font-medium text-gray-700 mb-1.5 block">Business stage</label>
        <ChipSingleSelect options={BUSINESS_STAGE_OPTIONS} value={profile.business_stage} onChange={set('business_stage')} />
      </div>

      <div>
        <label className="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1.5">
          <Briefcase className="w-3.5 h-3.5 text-primary-600" />
          Enterprise type
        </label>
        <Select
          value={profile.enterprise_type}
          onChange={set('enterprise_type')}
          options={ENTERPRISE_TYPE_OPTIONS}
          placeholder="Select enterprise type"
          className="max-w-sm"
        />
      </div>

      <div>
        <label className="text-sm font-medium text-gray-700 mb-1.5 block">Occupation</label>
        <Select
          value={profile.occupation}
          onChange={set('occupation')}
          options={OCCUPATION_OPTIONS}
          placeholder="Select occupation"
          className="max-w-sm"
        />
      </div>

      <div>
        <label className="text-sm font-medium text-gray-700 mb-1.5 block">Sector</label>
        <ChipSingleSelect options={SECTOR_OPTIONS} value={profile.sector} onChange={set('sector')} />
      </div>

      <div>
        <label className="text-sm font-medium text-gray-700 mb-1.5 block">Sub-sector</label>
        <Select
          value={profile.sub_sector}
          onChange={set('sub_sector')}
          options={SUB_SECTOR_OPTIONS}
          placeholder="Select sub-sector (optional)"
          className="max-w-sm"
        />
      </div>

      {profile.is_artisan === true && (
        <div>
          <label className="text-sm font-medium text-gray-700 mb-1.5 block">Artisan type</label>
          <ChipSingleSelect options={ARTISAN_TYPE_OPTIONS} value={profile.artisan_type} onChange={set('artisan_type')} />
        </div>
      )}

      <div className="grid sm:grid-cols-2 gap-4">
        <NumberField
          label="Annual family income"
          value={profile.annual_family_income}
          onChange={set('annual_family_income')}
          placeholder="e.g. 250000"
          suffix="₹/yr"
        />
        <NumberField
          label="Annual turnover"
          value={profile.annual_turnover}
          onChange={set('annual_turnover')}
          placeholder="e.g. 500000"
          suffix="₹/yr"
        />
        <NumberField
          label="Business age"
          value={profile.business_age_months}
          onChange={set('business_age_months')}
          placeholder="e.g. 18"
          suffix="months"
        />
        <NumberField
          label="Land holding"
          value={profile.land_holding_acres}
          onChange={set('land_holding_acres')}
          placeholder="e.g. 2.5"
          suffix="acres"
        />
        <NumberField
          label="Years worked overseas"
          value={profile.overseas_service_years}
          onChange={set('overseas_service_years')}
          placeholder="e.g. 3"
          suffix="years"
        />
      </div>

      <div>
        <label className="text-sm font-medium text-gray-700 mb-1.5 block">
          A few more (only if they apply)
        </label>
        <div className="flex flex-wrap gap-2">
          <ChipBooleanToggle
            label="Udyam-registered business"
            checked={profile.udyam_status === 'registered'}
            onToggle={(v) => set('udyam_status')(v ? 'registered' : null)}
          />
          <ChipBooleanToggle
            label="Parent is a farmer"
            checked={profile.parent_occupation === 'farmer'}
            onToggle={(v) => set('parent_occupation')(v ? 'farmer' : null)}
          />
        </div>
      </div>

      <div>
        <label className="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1.5">
          <Wheat className="w-3.5 h-3.5 text-primary-600" />
          Sub-caste <span className="text-gray-400 font-normal">(rarely needed — only a few schemes ask)</span>
        </label>
        <Select
          value={profile.sub_caste}
          onChange={set('sub_caste')}
          options={SUB_CASTE_OPTIONS}
          placeholder="Skip unless relevant"
          className="max-w-sm"
        />
      </div>
    </div>
  );
}
