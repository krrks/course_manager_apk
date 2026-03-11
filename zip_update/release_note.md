## Hotfix: menuAnchor unresolved reference

`ExposedDropdownMenuAnchorType` is `@ExperimentalMaterial3Api` in the project's current BOM (`2024.10.00`) and requires an opt-in that was missing, causing a compile error.

Changed approach: use `@Suppress("DEPRECATION")` on `FormDropdown` (CommonComponents.kt) and `AutocompleteTextField` (FluentComponentAliases.kt) to silence the deprecation warning without touching the experimental API. All other fixes from the previous patch (AutoMirrored icons, HorizontalDivider, UI changes) remain intact.
