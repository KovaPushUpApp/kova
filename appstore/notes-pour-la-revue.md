# Notes pour la revue Apple (à coller dans App Store Connect, en anglais)

Avant de coller : compléter les deux lignes entre crochets, et joindre une vidéo de
démonstration d'une minute (le parcours « How to test » filmé sur un iPhone).

---

Kova is a personal device usage management app: adults lock their own distracting
apps to stay focused, and unlock them for a limited time by doing push-ups or squats,
counted on-device by the camera.

No account, no login. The app works without notifications.
A physical iPhone is required: the Screen Time API and the camera do not work in the simulator.
Tested on [iPhone model, iOS version].
Demo video: [link, or "attached"].

FAMILY CONTROLS
- Individual authorization only (AuthorizationCenter, .individual). The user manages
  their own device. No parental control, no management of another person's device.
- The user picks the apps with the system FamilyActivityPicker. Kova only receives
  opaque tokens. It never reads app names, bundle identifiers or usage data, and
  nothing about the selection leaves the device.
- Shields are applied with ManagedSettings. Apps are shielded, never hidden.
- Bundle IDs using the Family Controls entitlement:
  com.getkova.app (app)
  com.getkova.app.monitor (DeviceActivityMonitor: re-applies shields when an unlock window ends)
  com.getkova.app.shieldconfig (ShieldConfiguration: draws the shield)
  com.getkova.app.shieldaction (ShieldAction: handles the shield buttons)
- The user can stop the blocking at any time: remove apps in the app (effective 24 hours
  later, by design, and shown in the app), or turn off Screen Time access for Kova in
  iOS Settings, or delete the app.

CAMERA
- Used only during a workout, to detect body pose with the Vision framework.
- Frames are processed in memory and discarded. Nothing is recorded, stored or sent.
- The live camera preview is visible on screen whenever the camera is on.

HOW TO TEST
1. Launch the app and go through onboarding. Allow Screen Time access when asked.
2. Choose one app to lock with the system picker.
3. Allow the camera. On the "Test: do 3 push-ups" screen you can do the push-ups, or tap
   "Continue without trying".
4. The paywall appears. Close it with the X button (top left) to continue with the free plan,
   or subscribe. "Restore" is at the top right.
5. Leave Kova and open the app you chose: the Kova shield is displayed.
6. Back in Kova, tap "Earn time", then start the session. Place the iPhone on the floor,
   upright against a wall, about 1 m away, and do the push-ups shown. The counter goes up
   on each full repetition. Validate the session: the chosen app opens for the time shown.
7. Without exercising: tap "Earn time", then "Emergency unlock" at the bottom of the screen.
   It opens the apps for 5 minutes. It is a user-facing feature, limited to twice a week.

SUBSCRIPTIONS
- One subscription group, "Kova Premium": yearly (3-day free trial for new subscribers)
  and weekly. Both are offered on the paywall at the end of onboarding, and in Settings > Subscription.
- Free plan: one locked app, push-ups only. Kova Premium adds unlimited locked apps,
  squats, schedules and statistics.
- Prices are loaded from the App Store. The billed amount, the period, the trial terms
  and the automatic renewal are shown under the purchase button, with links to the
  Terms of Use and the Privacy Policy.

HEALTH
- Kova is not a medical app and makes no medical claim. A notice in the app tells users
  to stop and ask a doctor in case of pain or doubt.
