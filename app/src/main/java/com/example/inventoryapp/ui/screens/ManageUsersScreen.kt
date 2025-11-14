package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.Result
import com.example.inventoryapp.data.User
import com.example.inventoryapp.model.UserRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    navController: NavController,
    authRepo: AuthRepository
) {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf(authRepo.getAllUsers()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Check if user is admin, otherwise navigate back
    if (!authRepo.isAdmin()) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Users") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, "Add User")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Success/Error messages
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { errorMessage = null }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
            }

            successMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { successMessage = null }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
            }

            // Users List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { user ->
                    UserCard(
                        user = user,
                        onEditRole = {
                            selectedUser = user
                            showEditDialog = true
                        },
                        onResetPassword = {
                            selectedUser = user
                            showResetPasswordDialog = true
                        },
                        onDelete = {
                            selectedUser = user
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Add User Dialog
    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { username, password, role ->
                scope.launch {
                    when (val result = authRepo.addUser(username, password, role)) {
                        is Result.Success -> {
                            users = authRepo.getAllUsers()
                            successMessage = "User '$username' added successfully"
                            showAddDialog = false
                        }
                        is Result.Error -> {
                            errorMessage = result.exception.message
                        }
                    }
                }
            }
        )
    }

    // Edit Role Dialog
    if (showEditDialog && selectedUser != null) {
        EditUserRoleDialog(
            user = selectedUser!!,
            onDismiss = {
                showEditDialog = false
                selectedUser = null
            },
            onConfirm = { newRole ->
                scope.launch {
                    when (val result = authRepo.updateUserRole(selectedUser!!.username, newRole)) {
                        is Result.Success -> {
                            users = authRepo.getAllUsers()
                            successMessage = "Role updated for '${selectedUser!!.username}'"
                            showEditDialog = false
                            selectedUser = null
                        }
                        is Result.Error -> {
                            errorMessage = result.exception.message
                        }
                    }
                }
            }
        )
    }

    // Reset Password Dialog
    if (showResetPasswordDialog && selectedUser != null) {
        ResetPasswordDialog(
            user = selectedUser!!,
            onDismiss = {
                showResetPasswordDialog = false
                selectedUser = null
            },
            onConfirm = { newPassword ->
                scope.launch {
                    when (val result = authRepo.resetUserPassword(selectedUser!!.username, newPassword)) {
                        is Result.Success -> {
                            successMessage = "Password reset for '${selectedUser!!.username}'"
                            showResetPasswordDialog = false
                            selectedUser = null
                        }
                        is Result.Error -> {
                            errorMessage = result.exception.message
                        }
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && selectedUser != null) {
        DeleteUserDialog(
            user = selectedUser!!,
            onDismiss = {
                showDeleteDialog = false
                selectedUser = null
            },
            onConfirm = {
                scope.launch {
                    when (val result = authRepo.deleteUser(selectedUser!!.username)) {
                        is Result.Success -> {
                            users = authRepo.getAllUsers()
                            successMessage = "User '${selectedUser!!.username}' deleted"
                            showDeleteDialog = false
                            selectedUser = null
                        }
                        is Result.Error -> {
                            errorMessage = result.exception.message
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun UserCard(
    user: User,
    onEditRole: () -> Unit,
    onResetPassword: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Role: ${user.role.name}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEditRole) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Role",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onResetPassword) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Reset Password",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete User",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, UserRole) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.VIEWER) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New User") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedRole.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = {
                                    selectedRole = role
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(username, password, selectedRole) },
                enabled = username.isNotBlank() && password.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditUserRoleDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(user.role) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Role for ${user.username}") },
        text = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedRole.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Role") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    UserRole.entries.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.name) },
                            onClick = {
                                selectedRole = role
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedRole) }) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ResetPasswordDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password for ${user.username}") },
        text = {
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newPassword) },
                enabled = newPassword.isNotBlank()
            ) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = "Warning") },
        title = { Text("Delete User") },
        text = {
            Text("Are you sure you want to delete user '${user.username}'? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
