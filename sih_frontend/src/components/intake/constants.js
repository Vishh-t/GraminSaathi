// Option lists for the applicant intake form. Values are the EXACT strings the
// eligibility rules (JsonLogic) expect on the `applicant.*` vocabulary — see
// GraminSaathi_Schemes_Integration_Handoff.md section 2 and ApplicantProfile.java.
// Labels are the human-friendly text shown on the chip/select; only the `value`
// is ever sent to the backend.

export const CATEGORY_OPTIONS = [
  { value: 'general', label: 'General' },
  { value: 'obc', label: 'OBC' },
  { value: 'sc', label: 'SC' },
  { value: 'st', label: 'ST' },
  { value: 'bc', label: 'BC' },
  { value: 'mbc', label: 'MBC' },
  { value: 'minority', label: 'Minority' },
  { value: 'pwd', label: 'PwD' },
  { value: 'dnc', label: 'De-notified community' },
  { value: 'maratha_ebc', label: 'Maratha EBC' },
];

export const BUSINESS_STAGE_OPTIONS = [
  { value: 'greenfield', label: 'New / not started yet' },
  { value: 'early_stage', label: 'Early stage (< 3 yrs)' },
  { value: 'existing', label: 'Existing / established' },
];

export const RURAL_URBAN_OPTIONS = [
  { value: 'rural', label: 'Rural' },
  { value: 'urban', label: 'Urban' },
];

export const EDUCATION_OPTIONS = [
  { value: '4th_pass', label: '4th pass' },
  { value: '5th_pass', label: '5th pass' },
  { value: '7th_pass', label: '7th pass' },
  { value: '8th_pass', label: '8th pass' },
  { value: '10th_pass', label: '10th pass' },
  { value: '12th_pass', label: '12th pass' },
  { value: 'iti', label: 'ITI' },
  { value: 'diploma', label: 'Diploma' },
  { value: 'graduate', label: 'Graduate' },
  { value: 'vocational', label: 'Vocational training' },
];

export const ENTERPRISE_TYPE_OPTIONS = [
  { value: 'individual', label: 'Individual' },
  { value: 'proprietorship', label: 'Proprietorship' },
  { value: 'partnership', label: 'Partnership' },
  { value: 'llp', label: 'LLP' },
  { value: 'company', label: 'Company' },
  { value: 'cooperative', label: 'Cooperative' },
  { value: 'shg', label: 'SHG' },
  { value: 'shg_federation', label: 'SHG federation' },
  { value: 'fpo', label: 'FPO' },
  { value: 'jlg', label: 'JLG' },
  { value: 'ngo', label: 'NGO' },
  { value: 'section_8_company', label: 'Section 8 company' },
  { value: 'startup', label: 'Startup' },
  { value: 'micro', label: 'Micro enterprise' },
  { value: 'small', label: 'Small enterprise' },
  { value: 'medium', label: 'Medium enterprise' },
  { value: 'msme', label: 'MSME' },
  { value: 'artisan_guild', label: 'Artisan guild' },
  { value: 'state_dairy_federation', label: 'State dairy federation' },
];

export const OCCUPATION_OPTIONS = [
  { value: 'farmer', label: 'Farmer' },
  { value: 'tenant_farmer', label: 'Tenant farmer' },
  { value: 'agricultural_labourer', label: 'Agricultural labourer' },
  { value: 'artisan', label: 'Artisan' },
  { value: 'traditional_artisan', label: 'Traditional artisan' },
  { value: 'street_vendor', label: 'Street vendor' },
  { value: 'hawker', label: 'Hawker' },
  { value: 'vegetable_seller', label: 'Vegetable seller' },
  { value: 'auto_driver', label: 'Auto driver' },
  { value: 'taxi_driver', label: 'Taxi driver' },
  { value: 'cab_driver', label: 'Cab driver' },
  { value: 'crew_member', label: 'Crew member' },
  { value: 'barber', label: 'Barber' },
  { value: 'tailor', label: 'Tailor' },
  { value: 'washerman', label: 'Washerman' },
  { value: 'cattle_rearer', label: 'Cattle rearer' },
  { value: 'fisherman', label: 'Fisherman' },
  { value: 'laborer', label: 'Laborer' },
  { value: 'unemployed', label: 'Unemployed' },
];

export const SECTOR_OPTIONS = [
  { value: 'manufacturing', label: 'Manufacturing' },
  { value: 'services', label: 'Services' },
  { value: 'trading', label: 'Trading' },
  { value: 'agri_allied', label: 'Agriculture & allied' },
  { value: 'technology', label: 'Technology' },
  { value: 'tourism', label: 'Tourism' },
  { value: 'green_energy', label: 'Green energy' },
  { value: 'logistics', label: 'Logistics' },
];

export const SUB_SECTOR_OPTIONS = [
  { value: 'food_processing', label: 'Food processing' },
  { value: 'dairy', label: 'Dairy' },
  { value: 'fisheries', label: 'Fisheries' },
  { value: 'poultry', label: 'Poultry' },
  { value: 'livestock', label: 'Livestock' },
  { value: 'goat', label: 'Goat rearing' },
  { value: 'sheep', label: 'Sheep rearing' },
  { value: 'piggery', label: 'Piggery' },
  { value: 'aquaculture', label: 'Aquaculture' },
  { value: 'horticulture', label: 'Horticulture' },
  { value: 'forestry', label: 'Forestry' },
  { value: 'handicrafts', label: 'Handicrafts' },
  { value: 'furniture', label: 'Furniture' },
  { value: 'warehousing', label: 'Warehousing' },
  { value: 'coir_processing', label: 'Coir processing' },
  { value: 'animal_feed', label: 'Animal feed' },
  { value: 'fodder', label: 'Fodder' },
  { value: 'meat_processing', label: 'Meat processing' },
  { value: 'marine_logistics', label: 'Marine logistics' },
  { value: 'post_harvest_management', label: 'Post-harvest management' },
  { value: 'community_farming_assets', label: 'Community farming assets' },
];

export const ARTISAN_TYPE_OPTIONS = [
  { value: 'handloom', label: 'Handloom' },
  { value: 'powerloom', label: 'Powerloom' },
  { value: 'weaver', label: 'Weaver' },
];

// Narrow, rarely used — dataset only references these 4.
export const SUB_CASTE_OPTIONS = [
  { value: 'chambhar', label: 'Chambhar' },
  { value: 'dhor', label: 'Dhor' },
  { value: 'holiya', label: 'Holiya' },
  { value: 'mochi', label: 'Mochi' },
];

// Verified 2026-09-22 against the actual distinct `state` values in
// all_schemes_enriched.json (28 states) — this is the real set, not a generic
// list of Indian states: it includes "Jammu and Kashmir" (a UT, but that's the
// string the dataset uses) and omits Manipur (no scheme in the dataset is
// tagged with it).
export const STATE_OPTIONS = [
  'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh', 'Goa',
  'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jammu and Kashmir', 'Jharkhand',
  'Karnataka', 'Kerala', 'Madhya Pradesh', 'Maharashtra', 'Meghalaya', 'Mizoram',
  'Nagaland', 'Odisha', 'Punjab', 'Rajasthan', 'Sikkim', 'Tamil Nadu', 'Telangana',
  'Tripura', 'Uttar Pradesh', 'Uttarakhand', 'West Bengal',
].map(s => ({ value: s, label: s }));

// Boolean applicant.* flags shown as toggle chips on the "Does this apply to
// you?" screen. Grouped loosely for scanability; grouping is display-only.
export const BOOLEAN_FLAG_GROUPS = [
  {
    title: 'Social category & identity',
    items: [
      { key: 'is_sc', label: 'SC' },
      { key: 'is_st', label: 'ST' },
      { key: 'is_woman', label: 'Woman' },
      { key: 'is_pwd', label: 'Person with disability' },
      { key: 'is_bpl', label: 'Below poverty line' },
      { key: 'is_landless', label: 'Landless' },
      { key: 'is_manual_scavenger', label: 'Former manual scavenger' },
      { key: 'is_safai_karamchari', label: 'Safai karamchari' },
      { key: 'is_returned_emigrant', label: 'Returned emigrant (worked abroad)' },
    ],
  },
  {
    title: 'Group / collective membership',
    items: [
      { key: 'is_shg_member', label: 'SHG member' },
      { key: 'is_fpo_member', label: 'FPO member' },
      { key: 'is_jlg_member', label: 'JLG member' },
      { key: 'is_sc_majority_owned', label: 'Business is SC-majority owned' },
    ],
  },
  {
    title: 'Occupation & business flags',
    items: [
      { key: 'is_artisan', label: 'Artisan' },
      { key: 'is_street_vendor', label: 'Street vendor' },
      { key: 'is_registered_unemployed', label: 'Registered unemployed' },
      { key: 'is_registered_labour', label: 'Registered labour' },
      { key: 'is_income_tax_payer', label: 'Income-tax payer' },
      { key: 'is_incubated', label: 'Business is incubated' },
      { key: 'dpiit_recognized', label: 'DPIIT-recognized startup' },
    ],
  },
  {
    title: 'Assets & documents',
    items: [
      { key: 'has_electricity_connection', label: 'Have electricity connection' },
      { key: 'has_kalia_bsky_card', label: 'Have KALIA / BSKY card' },
      { key: 'holds_driving_license', label: 'Have a driving license' },
      { key: 'owns_vehicle', label: 'Own a vehicle' },
      { key: 'owns_indigenous_cow', label: 'Own an indigenous cow breed' },
      { key: 'owns_loom', label: 'Own a handloom' },
      { key: 'completed_dairy_training', label: 'Completed dairy training' },
    ],
  },
];

// Empty (all-null) profile shape — mirrors ApplicantProfile.java field-for-field
// in snake_case, since that's the wire format profileAPI already speaks.
export const EMPTY_APPLICANT_PROFILE = {
  // booleans
  is_sc: null, is_st: null, is_woman: null, is_pwd: null, is_bpl: null,
  is_shg_member: null, is_fpo_member: null, is_jlg_member: null, is_artisan: null,
  is_street_vendor: null, is_landless: null, is_returned_emigrant: null,
  is_manual_scavenger: null, is_safai_karamchari: null, is_registered_unemployed: null,
  is_registered_labour: null, is_income_tax_payer: null, is_incubated: null,
  is_sc_majority_owned: null, dpiit_recognized: null, has_electricity_connection: null,
  has_kalia_bsky_card: null, holds_driving_license: null, owns_indigenous_cow: null,
  owns_loom: null, owns_vehicle: null, completed_dairy_training: null,
  // numbers
  age: null, annual_family_income: null, annual_turnover: null,
  business_age_months: null, land_holding_acres: null, overseas_service_years: null,
  // enums
  category: null, business_stage: null, enterprise_type: null, education: null,
  occupation: null, sector: null, sub_sector: null, rural_urban: null,
  artisan_type: null, sub_caste: null, udyam_status: null, state: null,
  parent_occupation: null,
};
