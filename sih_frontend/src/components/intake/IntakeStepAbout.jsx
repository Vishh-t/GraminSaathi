import { User, MapPin, GraduationCap, Home } from 'lucide-react';
import Select from '../Select';
import { ChipSingleSelect } from './Chip';
import { CATEGORY_OPTIONS, EDUCATION_OPTIONS, RURAL_URBAN_OPTIONS, STATE_OPTIONS } from './constants';

/** Step 1 of 3 — core identity facts every scheme's eligibility rules lean on. */
export default function IntakeStepAbout({ profile, onChange }) {
  const set = (key) => (value) => onChange({ ...profile, [key]: value });

  return (
    <div className="space-y-6">
      <div>
        <label className="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1.5">
          <User className="w-3.5 h-3.5 text-primary-600" />
          Your age
        </label>
        <input
          type="number"
          min="0"
          max="120"
          value={profile.age ?? ''}
          onChange={(e) => set('age')(e.target.value === '' ? null : parseInt(e.target.value, 10))}
          placeholder="e.g. 32"
          className="input-field max-w-[160px]"
        />
      </div>

      <div>
        <label className="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1.5">
          <MapPin className="w-3.5 h-3.5 text-primary-600" />
          State
        </label>
        <Select
          value={profile.state}
          onChange={set('state')}
          options={STATE_OPTIONS}
          placeholder="Select your state"
          className="max-w-sm"
        />
      </div>

      <div>
        <label className="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1.5">
          <Home className="w-3.5 h-3.5 text-primary-600" />
          Area
        </label>
        <ChipSingleSelect options={RURAL_URBAN_OPTIONS} value={profile.rural_urban} onChange={set('rural_urban')} />
      </div>

      <div>
        <label className="text-sm font-medium text-gray-700 mb-1.5 block">Social category</label>
        <ChipSingleSelect options={CATEGORY_OPTIONS} value={profile.category} onChange={set('category')} />
      </div>

      <div>
        <label className="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1.5">
          <GraduationCap className="w-3.5 h-3.5 text-primary-600" />
          Education
        </label>
        <ChipSingleSelect options={EDUCATION_OPTIONS} value={profile.education} onChange={set('education')} />
      </div>
    </div>
  );
}
