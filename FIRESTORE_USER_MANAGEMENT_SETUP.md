# Firestore User Management Setup Guide

This guide explains how to configure Firebase Firestore to store user accounts permanently, allowing the Admin to manage users directly through the app.

## Current Implementation

Currently, users are stored in **SharedPreferences** (local device storage), which means:
- Users are device-specific and not shared across devices
- Users are lost if the app is reinstalled
- Users cannot be centrally managed

## Firestore-Based Implementation (Recommended)

To make users permanent and centrally managed, follow these steps:

### 1. Firestore Database Structure

Create a collection called `users` in your Firestore database with the following structure:

```
users (collection)
  └── {userId} (document)
      ├── username: string
      ├── passwordHash: string
      ├── role: string (one of: "ADMIN", "OPERATOR", "ANALYST", "VIEWER")
      ├── createdAt: timestamp
      ├── createdBy: string
      └── isActive: boolean
```

### 2. Initial Admin User Setup

**IMPORTANT:** Before implementing Firestore users, you must create an initial admin user manually in Firestore Console:

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Navigate to **Firestore Database** → **Data**
4. Click "Start collection"
5. Collection ID: `users`
6. Click "Next"
7. Add first document with these fields:
   - Document ID: `admin` (or auto-generate)
   - Field `username`: `admin` (type: string)
   - Field `passwordHash`: `240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9` (type: string)
     - This is the SHA-256 hash of password "admin123"
     - To generate hash for a different password, use the hash function in AuthRepository.kt
   - Field `role`: `ADMIN` (type: string)
   - Field `createdAt`: (current timestamp)
   - Field `createdBy`: `system` (type: string)
   - Field `isActive`: `true` (type: boolean)

### 3. Security Rules

Configure Firestore security rules to protect user data:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      // Only authenticated users can read their own user document
      allow read: if request.auth != null;
      
      // Only admins can create, update, or delete users
      allow create, update, delete: if request.auth != null && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
    }
    
    // Inventory and transactions (existing rules)
    match /inventory/{document=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        (get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN' ||
         get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'OPERATOR');
    }
    
    match /transactions/{document=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        (get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN' ||
         get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'OPERATOR');
    }
  }
}
```

### 4. User Role Permissions

The app supports 4 user roles with different permission levels:

| Role | Permissions |
|------|------------|
| **ADMIN** | - Full access to all features<br>- Can add/edit/delete users<br>- Can view analytics<br>- Can edit and delete inventory items<br>- Can create all types of transactions |
| **OPERATOR** | - Can add/edit inventory items<br>- Can create transactions (Purchase, Sale, Repair, Repair Return)<br>- Cannot delete items<br>- Cannot view analytics<br>- Cannot manage users |
| **ANALYST** | - Can view analytics and reports<br>- Read-only access to inventory<br>- Cannot edit or create transactions<br>- Cannot manage users |
| **VIEWER** | - Read-only access to inventory<br>- Can view transaction history<br>- Cannot edit anything<br>- Cannot view analytics<br>- Cannot manage users |

### 5. How to Manage Users (Admin Only)

Once logged in as Admin:

1. Navigate to **Analytics Screen**
2. Click on the **User Management** icon (group icon) in the top-right
3. Click the **+ (Add)** button to create a new user
4. Fill in:
   - Username (must be unique)
   - Password (minimum 6 characters)
   - Confirm Password
   - Role (select from dropdown: Admin, Operator, Analyst, Viewer)
5. Click "Add User"

To delete a user:
1. Find the user in the list
2. Click the delete icon (trash can)
3. Confirm deletion

**Note:** You cannot delete your own account.

### 6. Migration from SharedPreferences to Firestore

If you want to migrate existing users from SharedPreferences to Firestore:

1. Export current users from the device
2. For each user, create a document in the `users` collection with the structure shown above
3. Update the app to use `FirebaseAuthRepository` instead of `AuthRepository`

### 7. Password Security Best Practices

- **DO NOT** store plain-text passwords in Firestore
- Always use the SHA-256 hashed passwords (as shown in the code)
- Consider implementing password reset functionality
- Enforce strong password requirements (minimum 8 characters, mix of letters, numbers, symbols)
- Consider implementing two-factor authentication for Admin users

### 8. Backup and Recovery

**Important:** Regularly backup your Firestore database:
1. Go to Firebase Console → Firestore Database → Export/Import
2. Schedule automated exports using Cloud Scheduler
3. Store backups in Cloud Storage

### 9. Testing User Management

After setup:
1. Login with the initial admin account
2. Create test users with different roles
3. Logout and login with each test user to verify permissions
4. Test that Operator can edit but not delete
5. Test that Analyst can only view analytics
6. Test that Viewer can only view inventory

### 10. Troubleshooting

**Problem:** Cannot login after migration
- Solution: Verify passwordHash is correctly formatted in Firestore
- Solution: Check Firestore security rules allow authentication

**Problem:** Users can't be created
- Solution: Verify the admin user has role = "ADMIN" (uppercase)
- Solution: Check Firestore security rules allow user creation

**Problem:** Users are lost after app reinstall
- Solution: This indicates SharedPreferences is still being used. Ensure FirebaseAuthRepository is configured correctly

## Additional Notes

- User management is only available to users with ADMIN role
- All user operations are logged with timestamps
- Deleted users cannot be recovered unless you have backups
- Username changes are not currently supported (would require updating all transaction history)

## Support

For additional help with Firestore configuration, refer to:
- [Firebase Firestore Documentation](https://firebase.google.com/docs/firestore)
- [Firebase Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
