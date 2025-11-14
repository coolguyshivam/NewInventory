# Admin → Manage Users Feature

## Overview
This feature provides a comprehensive user management interface accessible only to users with the ADMIN role. It allows administrators to manage user accounts including creation, role modification, password reset, and deletion.

## Access
- **Location**: Bottom Navigation Bar → Admin (🔧 Settings icon)
- **Access Control**: ADMIN role only
- **Route**: `manage_users`

## Features

### 1. User List
- Displays all users in the system
- Shows username and role for each user
- Sorted alphabetically by username
- Three action buttons per user:
  - ✏️ Edit Role
  - 🔒 Reset Password
  - 🗑️ Delete User

### 2. Add User
- **Access**: Floating Action Button (➕) at bottom right
- **Fields**:
  - Username (required, must be unique)
  - Password (required, will be hashed)
  - Role (dropdown: ADMIN, OPERATOR, VIEWER, STAFF, MANAGER, GUEST)
- **Validation**:
  - Username must not be empty
  - Username must not already exist
  - Password must not be empty

### 3. Edit User Role
- Select new role from dropdown menu
- Updates role immediately
- **Restriction**: Cannot change last ADMIN to non-ADMIN role

### 4. Reset Password
- Enter new password for user
- Password is hashed before storage
- User can immediately log in with new password
- **Validation**: Password must not be empty

### 5. Delete User
- Shows confirmation dialog
- Permanently removes user from system
- **Restrictions**:
  - Cannot delete currently logged-in user
  - Cannot delete last ADMIN user

## Security

### Data Encryption
- User data stored in EncryptedSharedPreferences
- AES256_GCM encryption scheme
- Android Keystore integration

### Password Security
- Passwords hashed with SHA-256
- Never stored in plaintext
- Old passwords cannot be recovered

### Access Control
- All operations require ADMIN role
- Non-admin users cannot access the screen
- Attempting to navigate to route redirects back

### Business Rules
1. At least one ADMIN must exist at all times
2. Cannot delete currently logged-in user
3. Usernames must be unique
4. Passwords cannot be empty

## Migration

### Automatic Migration
On first launch after update:
1. Checks if migration is needed
2. Reads existing users from old SharedPreferences
3. Migrates to encrypted storage
4. Sets migration complete flag
5. Migration only runs once

### Default Users
If no users exist after migration:
- **admin** / admin123 (ADMIN)
- **operator** / operator123 (OPERATOR)
- **viewer** / viewer123 (VIEWER)

## User Interface

### Main Screen Layout
```
┌─────────────────────────────────┐
│ ← Manage Users                  │ (Top Bar)
├─────────────────────────────────┤
│ Success/Error Messages          │ (Dismissible)
├─────────────────────────────────┤
│ ┌───────────────────────────┐   │
│ │ admin         (ADMIN)     │   │
│ │              [✏️] [🔒] [🗑️] │   │
│ └───────────────────────────┘   │
│ ┌───────────────────────────┐   │
│ │ operator    (OPERATOR)    │   │
│ │              [✏️] [🔒] [🗑️] │   │
│ └───────────────────────────┘   │
│ ┌───────────────────────────┐   │
│ │ viewer        (VIEWER)    │   │
│ │              [✏️] [🔒] [🗑️] │   │
│ └───────────────────────────┘   │
│                                 │
│                            [➕] │ (FAB)
├─────────────────────────────────┤
│ [Inventory][Transaction][Admin]│ (Bottom Nav)
│ [Analytics][History][Logout]   │
└─────────────────────────────────┘
```

### Dialogs

#### Add User Dialog
```
┌─────────────────────────────┐
│ Add New User                │
├─────────────────────────────┤
│ Username: [____________]    │
│ Password: [____________]    │
│ Role:     [VIEWER ▼]        │
│                             │
│         [Cancel] [Add]      │
└─────────────────────────────┘
```

#### Edit Role Dialog
```
┌─────────────────────────────┐
│ Edit Role for admin         │
├─────────────────────────────┤
│ Role: [ADMIN ▼]             │
│                             │
│       [Cancel] [Update]     │
└─────────────────────────────┘
```

#### Reset Password Dialog
```
┌─────────────────────────────┐
│ Reset Password for admin    │
├─────────────────────────────┤
│ New Password: [________]    │
│                             │
│        [Cancel] [Reset]     │
└─────────────────────────────┘
```

#### Delete Confirmation Dialog
```
┌─────────────────────────────┐
│ ⚠️ Delete User               │
├─────────────────────────────┤
│ Are you sure you want to    │
│ delete user 'admin'?        │
│ This action cannot be       │
│ undone.                     │
│                             │
│       [Cancel] [Delete]     │
└─────────────────────────────┘
```

## Messages

### Success Messages
- ✅ "User '[username]' added successfully"
- ✅ "Role updated for '[username]'"
- ✅ "Password reset for '[username]'"
- ✅ "User '[username]' deleted"

### Error Messages
- ❌ "Unauthorized: Admin access required"
- ❌ "User already exists"
- ❌ "Username cannot be empty"
- ❌ "Password cannot be empty"
- ❌ "User not found"
- ❌ "Cannot change role: At least one admin must exist"
- ❌ "Cannot delete currently logged in user"
- ❌ "Cannot delete: At least one admin must exist"

## Technical Details

### Dependencies
- `androidx.security:security-crypto:1.1.0-alpha06`

### Key Classes
- `UserSecureStore`: Encrypted storage layer
- `AuthRepository`: User management API
- `ManageUsersScreen`: UI implementation

### Navigation
- Route: `manage_users`
- Access: Bottom nav "Admin" button
- Back: Returns to previous screen

## Testing

See [TESTING_GUIDE.md](TESTING_GUIDE.md) for comprehensive testing instructions.

## Future Enhancements
- User search/filter
- Bulk operations
- User profile fields (email, phone)
- Password complexity requirements
- Account lockout policies
- Audit logging
- Export/import users

## Support
For issues or questions, please contact the development team.
