export interface Section {
  id: string;
  title: string;
  content: string;
}

export interface Plan {
  id: string;
  publicId: string;
  title: string;
  active: boolean;
  createdAt: string;
  sections: Section[];
  /** The person the plan is about (e.g. a caregiver's plan for someone
   * else) — sources the Emergency Guide View's greeting headline ("Olá,
   * meu nome é {ownerName}."). Not a field in the Create/Edit Plan form's
   * design mock, so it isn't editable through this pass's UI — populated
   * via seed data only. A real implementation would need a product
   * decision on whether this belongs on the plan or the account profile;
   * out of scope for GAB-31. */
  ownerName?: string;
  /** Structured emergency contact. The guide's call action reads these when
   * present, instead of recovering a phone number from prose — which broke
   * whenever the owner reworded the section. Optional: plans written before
   * these fields existed still fall back to the text scan. */
  emergencyContactName?: string;
  emergencyContactPhone?: string;
}
