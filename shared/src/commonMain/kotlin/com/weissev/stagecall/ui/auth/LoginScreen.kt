package com.weissev.stagecall.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.ui.components.StageCallBrand
import com.weissev.stagecall.ui.components.StageCallBrandBackground

@Composable
internal fun LoginScreen(error: String?, enabled: Boolean, signingIn: Boolean = false,
    message: String? = null,
    onEmailSubmit: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onSignIn: () -> Unit,
) {
    var registering by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailSubmitting by remember { mutableStateOf(false) }
    BrandedAuthLayout {
        Text("Discover your campus community", modifier = Modifier.padding(top = 8.dp, bottom = 24.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Surface(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (registering) "Join your community" else "Welcome back", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("Sign in with your University of Baguio account.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(false to "Log in", true to "Register").forEach { (register, label) ->
                        TextButton(
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (registering == register) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (registering == register) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            onClick = {
                                registering = register
                                password = ""
                                confirmation = ""
                                passwordVisible = false
                                formError = null
                            },
                        ) { Text(label, fontWeight = FontWeight.SemiBold) }
                    }
                }
                // Return the button tap to App.kt, which starts Google sign-in in AuthSession.kt.
                OutlinedButton(onClick = { emailSubmitting = false; onSignIn() }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFF1F1F1F))) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val googlePaths = remember {
                            listOf(
                                Color(0xFF4285F4) to "M21.35,12.25c0,-0.68 -0.06,-1.18 -0.19,-1.7H12v3.09h5.38c-0.11,0.77 -0.69,1.93 -1.98,2.71l-0.02,0.1 2.9,2.25 0.2,0.02c1.83,-1.69 2.87,-4.17 2.87,-6.47z",
                                Color(0xFF34A853) to "M12,21.75c2.62,0 4.83,-0.86 6.44,-2.35l-3.08,-2.37c-0.83,0.57 -1.94,0.97 -3.36,0.97 -2.57,0 -4.75,-1.69 -5.53,-4.02l-0.1,0.01 -3.02,2.34 -0.04,0.1A9.73,9.73 0,0 0,12 21.75z",
                                Color(0xFFFBBC05) to "M6.47,13.98a5.99,5.99 0,0 1,0 -3.96l-0.01,-0.11 -3.06,-2.38 -0.1,0.05a9.75,9.75 0,0 0,0 8.85z",
                                Color(0xFFEA4335) to "M12,6c1.82,0 3.05,0.79 3.75,1.45l2.74,-2.67A9.35,9.35 0,0 0,12 2.25a9.73,9.73 0,0 0,-8.7 5.33l3.17,2.44C7.25,7.69 9.43,6 12,6z",
                            ).map { (color, path) -> color to PathParser().parsePathString(path).toPath() }
                        }
                        Canvas(Modifier.size(20.dp)) {
                            scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                                googlePaths.forEach { (color, path) -> drawPath(path, color) }
                            }
                        }
                        Text(if (signingIn && !emailSubmitting) "Opening Google..." else "Continue with Google")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text("or use email", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.weight(1f))
                }
                OutlinedTextField(value = email, onValueChange = { email = it; formError = null },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    label = { Text("University email") }, enabled = enabled, singleLine = true,
                    placeholder = { Text("id@s.ubaguio.edu") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(value = password, onValueChange = { password = it; formError = null },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    label = { Text("Password") }, enabled = enabled, singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { TextButton(enabled = enabled, onClick = { passwordVisible = !passwordVisible }) { Text(if (passwordVisible) "Hide" else "Show") } },
                    supportingText = if (registering) ({ Text("At least 8 characters") }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                if (registering) {
                    OutlinedTextField(value = confirmation, onValueChange = { confirmation = it; formError = null },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        label = { Text("Confirm password") }, enabled = enabled, singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                }
                Button(modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(12.dp), enabled = enabled, onClick = {
                    val address = email.trim()
                    formError = when {
                        !registering && (password.isEmpty() || !Regex("^[^@\\s]+@([a-zA-Z0-9-]+\\.)*ubaguio\\.edu$", RegexOption.IGNORE_CASE).matches(address)) -> "Invalid login credentials"
                        !Regex("^[^@\\s]+@([a-zA-Z0-9-]+\\.)*ubaguio\\.edu$", RegexOption.IGNORE_CASE).matches(address) -> "Enter a valid University of Baguio email."
                        password.isEmpty() -> "Enter your password."
                        registering && password.length < 8 -> "Use a password with at least 8 characters."
                        registering && password != confirmation -> "Passwords do not match."
                        else -> null
                    }
                    if (formError == null) {
                        emailSubmitting = true
                        onEmailSubmit(address, password, registering)
                    }
                }) {
                    if (signingIn && emailSubmitting) {
                        CircularProgressIndicator(Modifier.padding(end = 8.dp).size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text(if (signingIn && emailSubmitting) "Please wait..." else if (registering) "Create account" else "Log in")
                }
                formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                com.weissev.stagecall.ui.components.ErrorNotice(error)
            }
        }
    }
}

@Composable
internal fun UniversityAccountRequiredScreen(error: String?, onSignOut: () -> Unit) {
    BrandedAuthLayout {
        Text("Your campus community awaits", modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Surface(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("University account required", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
                Text("This account isn't using a University of Baguio email. Sign out and log in with your university account to continue.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Use your university email", style = MaterialTheme.typography.labelLarge)
                        Text("For example, id@s.ubaguio.edu", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(12.dp)) {
                    Text("Sign out and switch account")
                }
                error?.let { com.weissev.stagecall.ui.components.ErrorNotice(it) }
            }
        }
    }
}

@Composable
private fun BrandedAuthLayout(content: @Composable ColumnScope.() -> Unit) {
    StageCallBrandBackground {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StageCallBrand()
            content()
        }
    }
}
