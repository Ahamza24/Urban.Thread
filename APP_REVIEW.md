# Urban Thread App Review

## Findings

- Build currently fails. `DesignAdapter.java` calls `design.getCategory()`, but `Design.java` has no `category` field or getter. `:app:assembleDebug` stops at this Java compile error.
- Auth gate is missing at app launch/upload. `AndroidManifest.xml` launches `MainActivity`, not login. `UploadFragment.java` saves unauthenticated uploads with `tailorId = "anonymous"`, which can create orphaned designs and bypass the intended customer/tailor role flow.
- Tailor detail actions can lose the tailor ID. `TailorAdapter.java` stores IDs in a map keyed by `User` object instances, but `HomeFragment.java` later replaces the adapter list with filtered `User` objects from a different list. Clicking a tailor can pass `null` for `tailorId`, breaking follow/hire.
- Orders are created but there is no order workflow. Hire buttons write to `orders`, but the app has no orders tab/screen, no accept/reject/status updates, and no customer/tailor visibility into requests.
- Tailor portfolio/profile image fields exist but no usable upload/edit flow does. `profileImage` and `portfolio` are modeled, and the tailor detail page tries to show them, but the profile edit dialog only updates shop/location/phone/description.
- Profile edit refresh can crash or fail after saving. `ProfileFragment.java` calls `loadUserProfile((View) getView().getParent())`, which is not guaranteed to be the inflated `fragment_profile_container` root and may not contain `profile_container`.
- Registration validation is incomplete for tailors. `RegisterActivity.java` saves shop/location/phone/description without checking they are filled, and defaults to `"Female Clothing"` if neither gender radio is selected.

## Most Important Missing Pieces

1. Login/session routing before `MainActivity`.
2. Fix compile error in `DesignAdapter`.
3. Store `id` directly on `models.User` and use that everywhere instead of the adapter map.
4. Orders screen/workflow for customers and tailors.
5. Tailor profile image and portfolio upload/manage flow.
6. Stronger validation and role checks before uploads/follows/hires.

## Verification

Ran `.\gradlew.bat :app:assembleDebug` with Android Studio's bundled JDK. The build reached Java compilation and failed on the missing `getCategory()` method.
