package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.PortalViewModel

@Composable
fun LoginScreen(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val allUsers by viewModel.allUsers.collectAsState()
    var userIdInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val loginError by viewModel.loginError.collectAsState()

    var isRegisterMode by remember { mutableStateOf(false) }
    var regName by remember { mutableStateOf("") }
    var regUserId by remember { mutableStateOf("") }
    var regDept by remember { mutableStateOf("Computer Science") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var showRegPassword by remember { mutableStateOf(false) }
    var registrationError by remember { mutableStateOf<String?>(null) }
    var showDeptDropdown by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Scholastic Header
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_nec_logo_new),
                            contentDescription = "NEC College Logo",
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "CAMPUS CONNECT",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Unified Services & Approvals Portal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Inputs card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        if (!isRegisterMode) {
                            Text(
                                text = "Log in to Continue",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = userIdInput,
                                onValueChange = { userIdInput = it },
                                label = { Text("Email, Student Roll or Staff ID") },
                                placeholder = { Text("e.g. user@nrtec.in or 23CS101") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    val icon = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(icon, contentDescription = "Password toggle")
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            loginError?.let { err ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Start
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.login(userIdInput, passwordInput) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("login_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Authenticate",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "New to the portal?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { isRegisterMode = true },
                                    modifier = Modifier.testTag("register_tab_button")
                                ) {
                                    Text("Register Account", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = "Register New Account",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = regName,
                                onValueChange = { 
                                    regName = it
                                    registrationError = null
                                },
                                label = { Text("Full Name") },
                                placeholder = { Text("e.g. Alex Rivera") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("reg_name_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regUserId,
                                onValueChange = { 
                                    regUserId = it
                                    registrationError = null
                                    getDeptFromRollNumber(it)?.let { autoDept ->
                                        regDept = autoDept
                                    }
                                },
                                label = { Text("Choose Roll or Staff ID") },
                                placeholder = { Text("e.g. 23CS105 or advisor2") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("reg_userid_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val deptOptions = listOf(
                                "Computer Science",
                                "Electronics & Communication",
                                "Electrical & Electronics",
                                "Civil Engineering",
                                "Mechanical Engineering",
                                "Information Technology"
                            )
                            val inferredDept = getDeptFromRollNumber(regUserId)

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = regDept,
                                    onValueChange = { 
                                        regDept = it
                                        registrationError = null
                                    },
                                    label = { Text("Department / Location") },
                                    placeholder = { Text("Select or enter department") },
                                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showDeptDropdown = !showDeptDropdown }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose department")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reg_dept_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    supportingText = {
                                        if (inferredDept != null) {
                                            Text(
                                                text = "Auto-selected for Roll Number: $inferredDept",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                )

                                DropdownMenu(
                                    expanded = showDeptDropdown,
                                    onDismissRequest = { showDeptDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    deptOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                regDept = option
                                                showDeptDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            val matchingHod = allUsers.find { it.role == "HOD" && it.department.equals(regDept, ignoreCase = true) }
                            matchingHod?.let { hod ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("associated_hod_card")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "HOD Icon",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = "Department HOD (Can Login & Assign)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = hod.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Email: ${hod.email} • Dept: ${hod.department}",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { 
                                    regEmail = it
                                    registrationError = null
                                },
                                label = { Text("Email Address") },
                                placeholder = { Text("e.g. user@nrtec.in") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("reg_email_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { 
                                    regPassword = it
                                    registrationError = null
                                },
                                label = { Text("Choose Password") },
                                placeholder = { Text("Enter password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    val icon = if (showRegPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { showRegPassword = !showRegPassword }) {
                                        Icon(icon, contentDescription = "Password toggle")
                                    }
                                },
                                visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().testTag("reg_password_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            registrationError?.let { err ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Start
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val emailText = regEmail.trim().lowercase()
                                    val nameText = regName.trim()
                                    val idText = regUserId.trim()
                                    val deptText = regDept.trim()
                                    val passwordText = regPassword.trim()

                                    if (nameText.isBlank()) {
                                        registrationError = "Name cannot be empty."
                                    } else if (idText.isBlank()) {
                                        registrationError = "ID/Roll Number cannot be empty."
                                    } else if (emailText.isBlank()) {
                                        registrationError = "Email cannot be empty."
                                    } else if (passwordText.isBlank()) {
                                        registrationError = "Password cannot be empty."
                                    } else {
                                        val determinedRole = when {
                                            emailText.endsWith("@nrtec.in") -> "STUDENT"
                                            emailText.endsWith("@technoelite.com") -> "MENTOR"
                                            emailText.endsWith("@hod.com") -> "HOD"
                                            emailText.endsWith("@principal.com") -> "PRINCIPAL"
                                            emailText.endsWith("@ranbidge.com") -> "ADMIN"
                                            emailText.endsWith("@neccanteen.com") -> "CANTEEN"
                                            emailText.endsWith("@necstationary.in") -> "STORE"
                                            emailText.endsWith("@necsecurity.in") -> "SECURITY"
                                            emailText.endsWith("@pa.com") || emailText.endsWith("@necpa.com") -> "PA"
                                            else -> null
                                        }

                                        if (determinedRole == null) {
                                            registrationError = "Invalid email extension! Use @nrtec.in for Student, @technoelite.com for Staff, @hod.com for HOD, @principal.com for Principal, @ranbidge.com for Admin, @neccanteen.com for Canteen, @necstationary.in for Stationary, @necsecurity.in for Security, or @pa.com for PA."
                                        } else {
                                            viewModel.registerNewUser(
                                                userId = idText,
                                                name = nameText,
                                                roll = idText,
                                                dept = deptText,
                                                email = emailText,
                                                role = determinedRole,
                                                password = passwordText,
                                                autoLogin = true
                                            )
                                            userIdInput = idText
                                            passwordInput = passwordText
                                            isRegisterMode = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("register_submit_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Create Account & Log In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { isRegisterMode = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("back_to_login_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Already have an account? Log In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "© 2026 RANBIDGE SOLUTIONS PVT LTD",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "All Rights Reserved. Campus Connect Portal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun getDeptFromRollNumber(roll: String): String? {
    val clean = roll.uppercase().trim()
    return when {
        clean.contains("CSE") || clean.contains("CS") -> "Computer Science"
        clean.contains("ECE") || clean.contains("EC") -> "Electronics & Communication"
        clean.contains("EEE") || clean.contains("EE") -> "Electrical & Electronics"
        clean.contains("CIVIL") || clean.contains("CIV") || clean.contains("CE") -> "Civil Engineering"
        clean.contains("MECH") || clean.contains("MEC") || clean.contains("ME") -> "Mechanical Engineering"
        clean.contains("IT") -> "Information Technology"
        else -> null
    }
}
