package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.WorkoutPreferences
import com.example.myapplication.ui.theme.DeepDark
import com.example.myapplication.ui.theme.ElectricGreen
import com.example.myapplication.ui.theme.ElevatedDark
import com.example.myapplication.ui.theme.MediumGray
import com.example.myapplication.ui.theme.OffWhite
import com.example.myapplication.ui.theme.Spacing
import com.example.myapplication.ui.theme.SubtleGray

/**
 * Settings Screen - User profile management
 */
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { WorkoutPreferences(context) }
    
    // User profile state
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    
    // Load saved data
    LaunchedEffect(Unit) {
        name = prefs.getUserName()
        age = prefs.getUserAge().let { if (it > 0) it.toString() else "" }
        height = prefs.getUserHeight().let { if (it > 0) it.toString() else "" }
        weight = prefs.getUserWeight().let { if (it > 0) it.toString() else "" }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        SettingsTopBar(
            onBackPressed = onBackPressed,
            modifier = Modifier.padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = ElectricGreen.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = ElectricGreen,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ElevatedDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OffWhite
                        )
                        
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = ElectricGreen
                            )
                        }
                    }
                    
                    // Name Field
                    ProfileTextField(
                        label = "Name",
                        value = name,
                        onValueChange = { name = it },
                        enabled = isEditing,
                        keyboardType = KeyboardType.Text
                    )
                    
                    // Age Field
                    ProfileTextField(
                        label = "Age",
                        value = age,
                        onValueChange = { age = it.filter { c -> c.isDigit() } },
                        enabled = isEditing,
                        keyboardType = KeyboardType.Number,
                        suffix = "years"
                    )
                    
                    // Height Field
                    ProfileTextField(
                        label = "Height",
                        value = height,
                        onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                        enabled = isEditing,
                        keyboardType = KeyboardType.Decimal,
                        suffix = "cm"
                    )
                    
                    // Weight Field
                    ProfileTextField(
                        label = "Weight",
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        enabled = isEditing,
                        keyboardType = KeyboardType.Decimal,
                        suffix = "kg"
                    )
                }
            }
            
            // Save Button (only visible when editing)
            if (isEditing) {
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Button(
                    onClick = {
                        // Save to preferences
                        prefs.setUserName(name)
                        prefs.setUserAge(age.toIntOrNull() ?: 0)
                        prefs.setUserHeight(height.toFloatOrNull() ?: 0f)
                        prefs.setUserWeight(weight.toFloatOrNull() ?: 0f)
                        isEditing = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen)
                ) {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepDark
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = ElectricGreen
            )
        }
        
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OffWhite,
            modifier = Modifier.padding(start = Spacing.sm)
        )
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType,
    suffix: String? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MediumGray
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricGreen,
                unfocusedBorderColor = SubtleGray,
                disabledBorderColor = SubtleGray.copy(alpha = 0.5f),
                focusedTextColor = OffWhite,
                unfocusedTextColor = OffWhite,
                disabledTextColor = MediumGray,
                cursorColor = ElectricGreen
            ),
            suffix = suffix?.let {
                { Text(text = it, color = MediumGray) }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }
}
