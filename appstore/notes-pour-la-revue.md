# Notes pour la revue Apple (à coller dans App Store Connect, en anglais)

Kova lets adults lock their own distracting apps and earn screen time by doing
push-ups, counted on-device by the front camera.

Family Controls
- We request individual authorization (AuthorizationCenter, .individual). The user
  picks the apps with the system FamilyActivityPicker.
- Shields are applied with ManagedSettings. A DeviceActivityMonitor extension
  re-applies them when an unlock window ends.
- The app never reads app names, bundle identifiers or usage data.

Camera
- The camera is used only during a workout to detect body pose with the Vision
  framework. Frames are processed in memory and discarded. Nothing is recorded,
  stored or sent.

How to test
1. Complete onboarding, allow Screen Time and camera access, choose one app.
2. The chosen app is now shielded. Open it to see the Kova shield.
3. In Kova, tap "Earn time", place the phone on the floor about 1 m away and do
   the push-ups shown. Then tap "Open now": the app is unlocked until the time shown.
4. Without push-ups, "Emergency unlock" opens the apps for 5 minutes (twice a week).

Subscriptions
- Yearly (3-day free trial) and weekly. A one-time discounted first year is offered
  when the paywall is closed. The paywall has a visible close button and a Restore
  button. Without a subscription the app remains usable with one locked app.
