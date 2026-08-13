export type PublicStackParamList = {
  Landing: undefined;
  Onboarding: undefined;
  Login: undefined;
  CreateAccount: undefined;
  ForgotPassword: undefined;
  /** Reached from the emailed link; the token rides in the query string. */
  ResetPassword: { token?: string } | undefined;
  EmergencyGuide: { publicId: string };
  NotFound: undefined;
};

export type HomeStackParamList = {
  HomeScreen: undefined;
  CreatePlan: undefined;
  Scan: undefined;
  PlanCreated: { planId: string };
  /** Registered here too so a signed-in owner can preview their own plan's
   * public guide from Plan Created — see PublicStack's copy of the same
   * screen/route for the unauthenticated (scanned-link) case. */
  EmergencyGuide: { publicId: string };
};

export type GalleryStackParamList = {
  GalleryScreen: undefined;
  PlanDetail: { planId: string };
  EditPlan: { planId: string };
  CreatePlan: undefined;
  PlanCreated: { planId: string };
  /** Registered here too so a signed-in owner can preview their own plan's
   * public guide from Plan Detail. */
  EmergencyGuide: { publicId: string };
};

export type ProfileStackParamList = {
  ProfileScreen: undefined;
};

export type AppTabParamList = {
  Início: undefined;
  Galeria: undefined;
  Perfil: undefined;
};
