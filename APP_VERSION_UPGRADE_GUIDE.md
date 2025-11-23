# App Version Upgrade Guide

## Overview
This guide explains how Android handles app version upgrades and how to properly configure the NewInventory app for automatic updates without requiring manual uninstall/reinstall.

## How Android App Upgrades Work

### Automatic Upgrade Process

**Good News:** Android automatically handles app upgrades! When a user installs a new version of your app, the system:

1. ✅ **Automatically removes the old version**
2. ✅ **Installs the new version in its place**
3. ✅ **Preserves user data** (databases, preferences, files)
4. ✅ **Maintains app permissions**
5. ✅ **Keeps the app in the same location** (home screen, folders)

### No Manual Uninstall Required!

Users do NOT need to manually uninstall the old version before installing a new one. The Android system handles this automatically when:
- The new APK has the same `applicationId` (package name)
- The new APK has a higher `versionCode`
- The new APK is signed with the same signing key

## Current Version Configuration

### In `app/build.gradle`:
```gradle
android {
    defaultConfig {
        applicationId "com.example.inventoryapp"
        versionCode 1
        versionName "1.0"
        // ... other configs
    }
}
```

### Version Fields Explained

1. **applicationId**: `"com.example.inventoryapp"`
   - Unique identifier for your app on the Play Store
   - MUST remain the same for upgrades to work
   - Changing this creates a completely different app

2. **versionCode**: `1` (integer)
   - Internal version number (not shown to users)
   - MUST be incremented with each release
   - Android uses this to determine if an update is newer
   - Example progression: 1 → 2 → 3 → 4...

3. **versionName**: `"1.0"` (string)
   - User-facing version number
   - Shown in Play Store and app settings
   - Can be any string (e.g., "1.0", "1.1", "2.0.0", "2.1-beta")
   - Follow semantic versioning: MAJOR.MINOR.PATCH

## Preparing for Version Updates

### Step 1: Version Numbering Strategy

Choose a versioning strategy and stick to it:

#### Option A: Semantic Versioning (Recommended)
```gradle
// Version 1.0.0 (Initial release)
versionCode 1
versionName "1.0.0"

// Version 1.0.1 (Bug fixes)
versionCode 2
versionName "1.0.1"

// Version 1.1.0 (New features)
versionCode 3
versionName "1.1.0"

// Version 2.0.0 (Major changes)
versionCode 4
versionName "2.0.0"
```

#### Option B: Date-Based Versioning
```gradle
// January 2024 release
versionCode 202401
versionName "2024.01"

// February 2024 release
versionCode 202402
versionName "2024.02"
```

#### Option C: Simple Incremental
```gradle
// Release 1
versionCode 1
versionName "1.0"

// Release 2
versionCode 2
versionName "2.0"
```

### Step 2: Update build.gradle Before Each Release

Example update for version 2.0:
```gradle
android {
    defaultConfig {
        applicationId "com.example.inventoryapp"
        minSdk 24
        targetSdk 34
        versionCode 2              // INCREMENT THIS
        versionName "2.0"          // UPDATE THIS
        // ... rest of config
    }
}
```

### Step 3: Maintain the Same Signing Key

**Critical:** Your app MUST be signed with the same key for upgrades to work!

#### Generate a Release Keystore (First Time Only)

```bash
keytool -genkey -v -keystore release-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias newinventory-key
```

You'll be prompted for:
- Keystore password (remember this!)
- Key password (remember this!)
- Organization details (can use "New Inventory" or your name)

#### Configure Signing in build.gradle

Add to `app/build.gradle`:
```gradle
android {
    signingConfigs {
        release {
            storeFile file("../release-keystore.jks")
            storePassword "YOUR_KEYSTORE_PASSWORD"
            keyAlias "newinventory-key"
            keyPassword "YOUR_KEY_PASSWORD"
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

**Security Note:** For production, use environment variables or gradle.properties:
```gradle
// In gradle.properties (add to .gitignore!)
RELEASE_STORE_FILE=../release-keystore.jks
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=newinventory-key
RELEASE_KEY_PASSWORD=your_password

// In build.gradle
signingConfigs {
    release {
        storeFile file(RELEASE_STORE_FILE)
        storePassword RELEASE_STORE_PASSWORD
        keyAlias RELEASE_KEY_ALIAS
        keyPassword RELEASE_KEY_PASSWORD
    }
}
```

### Step 4: Build Release APK

```bash
# Clean previous builds
./gradlew clean

# Build release APK
./gradlew assembleRelease

# APK will be at:
# app/build/outputs/apk/release/app-release.apk
```

## Distribution Methods

### Method 1: Google Play Store (Recommended)

When distributing via Play Store:
1. Users receive automatic update notifications
2. Updates download and install automatically (if enabled)
3. Play Store handles versioning checks
4. Users can enable auto-updates

**Steps:**
1. Build signed release APK or AAB (Android App Bundle)
2. Upload to Play Console
3. Play Store validates version codes
4. Users get update prompts automatically

### Method 2: Direct APK Distribution

When distributing APK files directly:
1. Users must enable "Install from Unknown Sources"
2. They download the new APK
3. Click to install
4. Android prompts: "Do you want to update this app?"
5. User taps "Update"
6. Old version is replaced automatically

**No uninstall needed!** Android handles it automatically.

### Method 3: Enterprise/MDM Distribution

For corporate deployments:
- Use MDM (Mobile Device Management) solutions
- Configure automatic app updates
- Push updates silently to managed devices

## Data Preservation During Updates

### What Gets Preserved
✅ **Automatically Preserved:**
- Room database (all inventory data)
- SharedPreferences (user settings)
- Internal storage files (images, documents)
- App-specific external storage
- User permissions

❌ **Not Preserved:**
- Cache files (automatically cleared)
- Temporary files in cache directory

### Handling Database Migrations

If your database schema changes between versions:

```kotlin
// In your database class
@Database(
    entities = [InventoryItem::class, Transaction::class],
    version = 2,  // Increment this when schema changes
    exportSchema = false
)
abstract class InventoryDatabase : RoomDatabase() {
    
    companion object {
        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventoryDatabase::class.java,
                    "inventory_database"
                )
                    .addMigrations(MIGRATION_1_2)  // Add migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Define migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: Add new column
                database.execSQL(
                    "ALTER TABLE inventory_items ADD COLUMN new_field TEXT"
                )
            }
        }
    }
}
```

## Testing App Updates

### Local Testing Process

1. **Install Initial Version**
   ```bash
   # Build and install version 1
   ./gradlew installDebug
   ```

2. **Add Some Test Data**
   - Add inventory items
   - Create transactions
   - Change settings

3. **Update Version Numbers**
   ```gradle
   versionCode 2
   versionName "2.0"
   ```

4. **Install Updated Version**
   ```bash
   # Build and install version 2
   ./gradlew installDebug
   ```

5. **Verify:**
   - App icon might have changed (if you updated it)
   - All previous data is still there
   - New features work correctly
   - Settings are preserved

### Using ADB for Testing

```bash
# Install initial version
adb install app-v1.apk

# Later, install update (no uninstall needed!)
adb install -r app-v2.apk  # -r flag means reinstall/replace

# Check installed version
adb shell dumpsys package com.example.inventoryapp | grep versionName
```

## Common Upgrade Scenarios

### Scenario 1: Feature Update (1.0 → 1.1)
```gradle
// Old version
versionCode 1
versionName "1.0"

// New version with new features
versionCode 2
versionName "1.1"
```
**Result:** Users update normally, all data preserved.

### Scenario 2: Bug Fix (1.0 → 1.0.1)
```gradle
// Old version
versionCode 1
versionName "1.0"

// Bug fix update
versionCode 2
versionName "1.0.1"
```
**Result:** Minor update, seamless for users.

### Scenario 3: Major Update (1.x → 2.0)
```gradle
// Old version
versionCode 5
versionName "1.5"

// Major rewrite
versionCode 6
versionName "2.0"
```
**Result:** Major update, but upgrade process is the same. Consider adding data migration code if database schema changed significantly.

### Scenario 4: Database Schema Change
```gradle
// Old version
versionCode 3
versionName "1.2"
// Database version: 1

// New version with schema changes
versionCode 4
versionName "1.3"
// Database version: 2
```
**Required:** Implement Room migration (see Data Preservation section above).

## Troubleshooting

### Issue: "App not installed" Error

**Cause:** Version code not incremented OR signing key mismatch

**Solution:**
1. Check that new versionCode > old versionCode
2. Verify signing key is the same
3. If testing, try: `adb install -r app.apk`

### Issue: Data Lost After Update

**Cause:** Changed applicationId OR manually uninstalled

**Solution:**
- Never change applicationId after first release
- Don't manually uninstall between updates
- Implement proper database migrations

### Issue: Update Not Showing in Play Store

**Cause:** Version code not higher than published version

**Solution:**
- Check Play Console for currently published versionCode
- Ensure new versionCode is higher
- Wait up to 2-3 hours for Play Store propagation

### Issue: "Signature Conflict" Error

**Cause:** APK signed with different key

**Solution:**
- Use the same keystore for all releases
- Back up your keystore file!
- If key is lost, you MUST create a new app listing

## Best Practices

### 1. Version Control
✅ Always increment versionCode for each release
✅ Use meaningful versionName values
✅ Tag releases in Git: `git tag v2.0.0`
✅ Document changes in CHANGELOG.md

### 2. Signing Security
✅ Keep keystore file secure and backed up
✅ Never commit passwords to Git
✅ Use environment variables for sensitive data
✅ Store keystore in secure location (password manager, secure storage)

### 3. Testing
✅ Test upgrade path before releasing
✅ Verify data preservation
✅ Test on multiple Android versions
✅ Check for database migration issues

### 4. User Communication
✅ Show changelog in app after update
✅ Notify users of major changes
✅ Provide update notes in Play Store listing
✅ Consider in-app update prompts for critical updates

### 5. Rollback Strategy
✅ Keep previous version APK backed up
✅ Test rollback scenarios
✅ Have a plan for emergency rollbacks
✅ Monitor crash reports after releases

## Current Configuration Review

### Current Setup (app/build.gradle):
```gradle
defaultConfig {
    applicationId "com.example.inventoryapp"  // ✅ Good - unique identifier
    versionCode 1                             // ⚠️  Ready for first release
    versionName "1.0"                         // ✅ Good - standard initial version
}
```

### Recommendations:

1. **Before Next Release:**
   - Increment to `versionCode 2`, `versionName "1.1"` or "2.0"

2. **Set Up Signing:**
   - Create release keystore
   - Configure signing in build.gradle
   - Back up keystore file securely

3. **Test Upgrade:**
   - Install current version (versionCode 1)
   - Increment version to 2
   - Install updated version
   - Verify data preserved

## Summary

### Key Points:
1. ✅ **Android automatically handles app upgrades** - no manual uninstall needed
2. ✅ **Always increment versionCode** before each release
3. ✅ **Never change applicationId** after first release
4. ✅ **Use same signing key** for all releases
5. ✅ **User data is automatically preserved** during upgrades
6. ✅ **Test the upgrade process** before releasing to users

### The Upgrade Process is Simple:
```
Old App (versionCode 1) 
    ↓ 
User installs APK with versionCode 2
    ↓ 
Android automatically replaces old version
    ↓ 
New App (versionCode 2) with all data preserved
```

**No uninstall step required!** This is built into Android's package management system.

## Additional Resources

- [Android App Versioning Guide](https://developer.android.com/studio/publish/versioning)
- [In-App Updates API](https://developer.android.com/guide/playcore/in-app-updates) (for Play Store)
- [App Signing](https://developer.android.com/studio/publish/app-signing)
- [Database Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions)

## Questions?

If you encounter any issues with app updates, check:
1. Version codes are properly incremented
2. ApplicationId matches previous version
3. APK is signed with the same key
4. Database migrations are implemented (if schema changed)
