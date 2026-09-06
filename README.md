# Ampere Monitor Pro

A polished Kotlin + Jetpack Compose Android battery dashboard.

## Improvements

- Material 3-inspired dark dashboard layout
- Circular battery-level gauge
- Automatic 3-second refresh plus a manual refresh action
- Charging source: AC, USB, wireless, or battery
- Battery health reporting
- Reliable unsupported-value fallbacks
- Separate battery repository for cleaner structure and easier testing

## Run

Open in a current Android Studio release, allow Gradle to sync, and run on a physical Android device. No runtime permissions are required.

## Device compatibility

Level, state, voltage, temperature, health, and power source are obtained from Android battery broadcasts. Current uses `BATTERY_PROPERTY_CURRENT_NOW`; some manufacturers do not provide this property, so the app displays `Current unavailable` rather than misleading data.
