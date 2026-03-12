package com.school.manager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.school.manager.ui.theme.*

// ─── Aliases used by ClassesScreen (and others) ───────────────────────────────

@Composable
fun FluentTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) = FormTextField(label, value, onValueChange, placeholder, modifier)

@Composable
fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) = FormDropdown(label, selected, options, onSelect)

@Suppress("DEPRECATION")
@Composable
fun AutocompleteTextField(
    label: String,
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = suggestions.filter { it.contains(value, ignoreCase = true) && it != value }

    ExposedDropdownMenuBox(
        expanded         = expanded && filtered.isNotEmpty(),
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value         = value,
            onValueChange = { onValueChange(it); expanded = true },
            label         = { Text(label) },
            shape         = RoundedCornerShape(12.dp),
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            singleLine    = true,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = FluentBlue,
                unfocusedBorderColor = FluentBorder
            )
        )
        if (filtered.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filtered.take(8).forEach { opt ->
                    DropdownMenuItem(
                        text    = { Text(opt) },
                        onClick = { onValueChange(opt); expanded = false }
                    )
                }
            }
        }
    }
}

/** Convenience code generator used in form dialogs. */
fun genCode(prefix: String): String =
    "$prefix${System.currentTimeMillis().toString().takeLast(6)}"
