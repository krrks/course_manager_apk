## Fix: Unresolved reference androidx.ui.test.manifest

Added missing `androidx-ui-test-manifest` library entry to `gradle/libs.versions.toml`.

This fixes the build error:
  Line 83: debugImplementation(libs.androidx.ui.test.manifest)
  ^ Unresolved reference
