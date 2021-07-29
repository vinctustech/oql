entity account (accounts) {
 *id: uuid
  name: text!
  industry: text!
  phoneNumber (phone_number): text
  country: text!
  uom: text!
  enabled: bool!
  plan: text!
  paymentMethodExempt (payment_method_exempt): bool!
  stripeCustomerId (stripe_customer_id): text
  stripeSubscriptionId (stripe_subscription_id): text
  stripeSubscriptionTripId (stripe_subscription_trip_id): text
  stripeSubscriptionSmsId (stripe_subscription_sms_id): text
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
  trialEndAt (trial_end_at): timestamp!
  stores: [store]
  users: [user]
  integrations: [integration] (accounts_integrations)
}

entity place (places) {
 *id: uuid
  store (store_id): store!
  latitude: float!
  longitude: float!
  address: text!
  isFavorite (is_favorite): bool!
  createdAt (created_at): timestamp!
}

entity virtualPhoneNumber (virtual_phone_numbers) {
 *id: uuid
  digits: text!
}

entity store (stores) {
 *id: uuid
  enabled: bool!
  account (account_id): account!
  name: text!
  timezoneId (timezone_id): text!
  place (place_id): place
  color: text!
  iconUrl (icon_url): text
  markerUrl (marker_url): text
  radiusBound (radius_bound): int
  virtualPhoneNumber (virtual_phone_number_id): virtualPhoneNumber!
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
  createdBy (created_by): user
  updatedBy (updated_by): user
  users: [user] (users_stores)
  vehicles: [vehicle]
  workflows: [workflow]
  trips: [trip]
}

entity user (users) {
 *id: uuid
  account (account_id): account!
  role: text!
  email: text!
  emailVerified (email_verified): bool!
  password: text!
  firstName (first_name): text!
  lastName (last_name): text!
  language: text!
  phoneNumber (phone_number): text!
  fcmToken (fcm_token): text
  enabled: bool!
  loginToken (login_token): text!
  lastLoginAt (last_login_at): timestamp
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
  createdBy (created_by): user
  updatedBy (updated_by): user
  stores: [store] (users_stores)
  vehicle: <vehicle>.driver
}

entity pendingInvitation (pending_invitations) {
 *id: uuid
  account (account_id): account!
  role: text!
  email: text!
  stores: [store] (pending_invitations_stores)
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
  expiresAt (expires_at): timestamp!
  createdBy (created_by): user
  updatedBy (updated_by): user
}

entity users_stores {
  user (user_id): user
  store (store_id): store
}

entity pending_invitations_stores {
  pendingInvitation (pending_invitation_id): pendingInvitation
  store (store_id): store
}

entity vehicle (vehicles) {
 *id: uuid
  driver (driver_id): user
  type: text!
  enabled: bool!
  seats: int!
  make: text!
  model: text!
  color: text!
  licensePlate (license_plate): text!
  store (store_id): store!
  vehicleCoordinate (vehicle_coordinate_id): vehicleCoordinate
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
  createdBy (created_by): user
  updatedBy (updated_by): user
  trips: [trip]
}

entity vehicleCoordinate (vehicle_coordinates) {
 *id: uuid
  vehicle (vehicle_id): vehicle!
  driver (driver_id): user
  latitude: float!
  longitude: float!
  altitude: float!
  accuracy: float!
  altitudeAccuracy (altitude_accuracy): float!
  heading: float!
  speed: float!
  createdAt (created_at): timestamp!
}

entity customer (customers) {
 *id: uuid
  store (store_id): store!
  firstName (first_name): text
  lastName (last_name): text
  phoneNumber (phone_number): text!
  email: text
  language: text!
  companyName (company_name): text
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
  createdBy (created_by): user
  updatedBy (updated_by): user
  places: [place] (customers_places)
  enabled: bool!
}

entity customers_places {
  customer (customer_id): customer
  place (place_id): place
}

entity messageTemplate (message_templates) {
 *id: uuid
  name: text!
  enabled: bool!
  type: text!
  store (store_id): store!
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
  createdBy (created_by): user
  updatedBy (updated_by): user
  locales: [messageTemplateLocale]
}

entity messageTemplateLocale (message_template_locales) {
  *id: uuid
  messageTemplate (message_template_id): messageTemplate!
  language: text!
  template: text!
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
  createdBy (created_by): user
  updatedBy (updated_by): user
}

entity workflow (workflows) {
 *id: uuid
  store (store_id): store!
  enabled: bool!
  name: text!
  description: text
  steps: [workflowStep]
  customerRequired (customer_required): bool!
  emailRequired (email_required): bool!
  companyNameRequired (company_name_required): bool!
  defaultReturnTripWorkflow (default_return_trip_workflow): workflow
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
  createdBy (created_by): user
  updatedBy (updated_by): user
}

entity workflowStep (workflow_steps) {
 *id: uuid
  workflow (workflow_id): workflow!
  type: text!
  name: text!
  place (place_id): place
  position: int!
  messageTemplate (message_template_id): messageTemplate
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
  createdBy (created_by): user
  updatedBy (updated_by): user
}

entity trip (trips) {
 *id: uuid
  reference: text!
  state: text!
  position: int!
  seats: int!
  shortUrl (short_url): text!
  customer (customer_id): customer
  store (store_id): store!
  workflow (workflow_id): workflow!
  vehicle (vehicle_id): vehicle
  returnTrip (return_trip_id): trip
  returnTripFor (return_trip_for_id): trip
  createdAt (created_at): timestamp!
  requestedAt (requested_at): timestamp
  scheduledAt (scheduled_at): timestamp
  confirmedAt (confirmed_at): timestamp
  confirmedBy (confirmed_by): user
  notifyAt (notify_at): timestamp
  updatedAt (updated_at): timestamp
  finishedAt (finished_at): timestamp
  finishedBy (finished_by): user
  createdBy (created_by): user
  updatedBy (updated_by): user
  notes: [tripNote]
  drivers: [user] (trips_drivers)
  steps: [tripStep]
}

entity tripNote (trip_notes) {
 *id: uuid
  content: text!
  trip (trip_id): trip!
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
  createdBy (created_by): user
  updatedBy (updated_by): user
}

entity trips_drivers {
  trip (trip_id): trip!
  driver (driver_id): user!
}

entity tripStep (trip_steps) {
 *id: uuid
  type: text!
  trip (trip_id): trip!
  driver (driver_id): user
  name: text!
  place (place_id): place
  imageUrl (image_url): text
  position: int!
  vehicle (vehicle_id): vehicle
  distance: float
  finishedAt (finished_at): timestamp
  messageTemplate (message_template_id): messageTemplate
}

entity integration (integrations) {
 *id: uuid
  name: text!
  description: text!
  logoUrl (logo_url): text
  websiteUrl (website_url): text!
  createdAt (created_at): timestamp!
  updatedAt (updated_at): timestamp
}

entity accounts_integrations {
  account (account_id): account
  integration (integration_id): integration
}

entity kimobyIntegration (kimoby_integrations) {
  *id: uuid
   account (account_id): account!
   apiUser (api_user): text!
   apiPassword (api_password): text!
   syncedContacts (synced_contacts): int!
   syncing: bool!
   createdAt (created_at): timestamp!
 }
