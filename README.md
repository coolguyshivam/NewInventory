# NewInventory App - Icon and Upgrade Setup

## Overview

This repository contains the NewInventory Android app - an inventory management system with barcode scanning, transaction tracking, and Firebase integration.

## Documentation

This repository includes comprehensive guides for:

### 📱 App Icon Creation
See **[APP_ICON_GUIDE.md](APP_ICON_GUIDE.md)** for detailed instructions on:
- How to create custom launcher icons for the app
- Using Android Studio Image Asset Studio (recommended method)
- Online icon generators and tools
- Manual icon creation process
- Icon design best practices for inventory apps
- Testing and troubleshooting

### 🔄 App Version Upgrades  
See **[APP_VERSION_UPGRADE_GUIDE.md](APP_VERSION_UPGRADE_GUIDE.md)** for complete information about:
- How Android automatically handles app upgrades (no manual uninstall needed!)
- Proper version numbering (versionCode and versionName)
- App signing and keystore management
- Data preservation during upgrades
- Testing upgrade scenarios
- Distribution methods (Play Store and direct APK)

## Quick Start

### For Developers

1. **Clone the repository**
   ```bash
   git clone https://github.com/coolguyshivam/NewInventory.git
   cd NewInventory
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory
   - Wait for Gradle sync to complete

3. **Build and Run**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

### Current Version

- **Application ID:** `com.example.inventoryapp`
- **Version Code:** 1
- **Version Name:** "1.0"
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

## Creating a Custom App Icon

### Quick Method: Android Studio Image Asset Studio

1. Prepare your icon design (1024x1024 PNG recommended)
2. In Android Studio: Right-click `app` folder → New → Image Asset
3. Configure:
   - Icon Type: Launcher Icons (Adaptive and Legacy)
   - Foreground: Your icon image
   - Background: App primary color (#6200EE)
4. Click Finish - all icon files are generated automatically!

For detailed instructions, see [APP_ICON_GUIDE.md](APP_ICON_GUIDE.md)

### Icon Design Suggestions for Inventory App

Consider using these symbols that represent inventory management:
- 📦 Box/Package
- 📋 Clipboard with checklist
- 🏷️ Barcode or price tag  
- 📊 Warehouse shelves
- ✓ Checkmark with inventory list

**Example:** A vector drawable icon is provided at:
`app/src/main/res/drawable/ic_launcher_vector_example.xml`

This shows a box with checklist items - feel free to use as inspiration!

## App Version Updates & Upgrades

### How It Works (TL;DR)

**Good News:** You do NOT need to uninstall the old version before installing a new one!

When you release a new version:
1. User downloads the new APK
2. User taps to install
3. Android automatically asks "Update this app?"
4. User confirms
5. **Android automatically removes old version and installs new version**
6. All user data is preserved!

### Before Each New Release

Update version numbers in `app/build.gradle`:

```gradle
android {
    defaultConfig {
        applicationId "com.example.inventoryapp"  // NEVER change this!
        versionCode 2              // Increment this (was 1, now 2)
        versionName "2.0"          // Update this (was "1.0", now "2.0")
        // ...
    }
}
```

**Important Rules:**
- ✅ Always increment `versionCode` (1 → 2 → 3 → 4...)
- ✅ Never change `applicationId` after first release
- ✅ Always use the same signing key for all releases
- ✅ Test the upgrade before releasing to users

For complete details, see [APP_VERSION_UPGRADE_GUIDE.md](APP_VERSION_UPGRADE_GUIDE.md)

## Key Features

- ✅ Inventory management with categories and status tracking
- ✅ Barcode scanning using ML Kit
- ✅ Transaction history (Purchase, Sale, Repair, Edit, Delete)
- ✅ Firebase Firestore integration for data sync
- ✅ Firebase Storage for images
- ✅ Biometric authentication
- ✅ Material 3 Design with Jetpack Compose
- ✅ Room database for local storage
- ✅ Real-time analytics and reporting

## Project Structure

```
NewInventory/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/inventoryapp/
│   │   │   ├── data/          # Repository and data sources
│   │   │   ├── model/         # Data models and ViewModels
│   │   │   ├── ui/            # Compose UI screens and components
│   │   │   └── utils/         # Utility classes
│   │   ├── res/
│   │   │   ├── mipmap-*/      # Launcher icons (all densities)
│   │   │   ├── values/        # Colors, strings, themes
│   │   │   └── xml/           # File provider paths
│   │   └── AndroidManifest.xml
│   └── build.gradle           # App-level build configuration
├── APP_ICON_GUIDE.md          # Icon creation guide
├── APP_VERSION_UPGRADE_GUIDE.md  # Version upgrade guide
├── BACKUP_PLAN.md             # Firebase backup strategy
└── README.md                  # This file
```

## Building for Release

### 1. Create Signing Key (First Time Only)

```bash
keytool -genkey -v -keystore release-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias newinventory-key
```

**Important:** Backup this keystore file securely! You'll need it for all future releases.

### 2. Configure Signing

Create `keystore.properties` in project root (add to .gitignore):
```properties
storeFile=release-keystore.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=newinventory-key
keyPassword=YOUR_KEY_PASSWORD
```

### 3. Build Release APK

```bash
./gradlew clean
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### 4. Test the Release APK

```bash
# Install on connected device
adb install -r app/build/outputs/apk/release/app-release.apk

# Or via gradlew
./gradlew installRelease
```

## Testing App Updates

### Scenario: Testing Version 1.0 → 2.0 Upgrade

1. **Install version 1.0:**
   ```bash
   ./gradlew installDebug
   ```

2. **Use the app - add some inventory data**

3. **Update build.gradle:**
   ```gradle
   versionCode 2
   versionName "2.0"
   ```

4. **Build and install version 2.0:**
   ```bash
   ./gradlew clean
   ./gradlew installDebug
   ```

5. **Verify:**
   - App updated successfully (no uninstall needed!)
   - All previous data is still present
   - New features work correctly

## Dependencies

Major libraries used:
- **Jetpack Compose** - Modern UI toolkit
- **Room** - Local database
- **Firebase** (Firestore, Storage, Auth) - Backend services
- **CameraX** - Camera functionality
- **ML Kit** - Barcode scanning
- **Coil** - Image loading
- **Kotlin Coroutines** - Asynchronous programming

See `app/build.gradle` for complete dependency list.

## Configuration

### Firebase Setup

1. Download `google-services.json` from Firebase Console
2. Place in `app/` directory
3. Update Firebase project settings as needed

### Firestore Collections

- `inventory_items` - Main inventory data
- `transactions` - Transaction history
- `transactions_archive` - Archived transactions (see BACKUP_PLAN.md)

## Development Guidelines

### Before Committing

1. ✅ Code follows Kotlin style guide
2. ✅ No commented-out code
3. ✅ All imports are used
4. ✅ Build succeeds: `./gradlew build`
5. ✅ No new warnings

### Before Releasing

1. ✅ Update versionCode and versionName
2. ✅ Test on multiple devices/Android versions
3. ✅ Test upgrade path from previous version
4. ✅ Verify data preservation
5. ✅ Update CHANGELOG (if exists)
6. ✅ Build signed release APK
7. ✅ Test release APK before distribution

## Troubleshooting

### Build Issues

**Problem:** Gradle sync fails
- **Solution:** Check internet connection, invalidate caches (File → Invalidate Caches/Restart)

**Problem:** Dependency resolution errors
- **Solution:** Update Gradle wrapper: `./gradlew wrapper --gradle-version=8.1`

### Installation Issues

**Problem:** "App not installed" error
- **Solution:** Ensure versionCode is incremented, or uninstall previous version

**Problem:** Data lost after update
- **Solution:** Never change applicationId; implement proper database migrations

### Icon Issues

**Problem:** Icon not updating after replacement
- **Solution:** Clean project, invalidate caches, uninstall app, rebuild

**Problem:** Icon looks blurry
- **Solution:** Ensure all density versions (mdpi through xxxhdpi) are provided

## Resources

### Official Documentation
- [Android Developer Guide](https://developer.android.com)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Firebase Android](https://firebase.google.com/docs/android/setup)

### Icon Design Tools
- [Android Image Asset Studio](https://developer.android.com/studio/write/image-asset-studio) (Built into Android Studio)
- [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html) (Web-based)
- [Material Icons](https://fonts.google.com/icons) (Free icon library)

### Version Management
- [App Versioning Best Practices](https://developer.android.com/studio/publish/versioning)
- [Semantic Versioning](https://semver.org/)

## Contributing

When contributing:
1. Create a feature branch
2. Follow existing code style
3. Test thoroughly
4. Update documentation if needed
5. Increment version numbers for releases

## License

[Add your license information here]

## Support

For issues or questions:
- Check the documentation files (APP_ICON_GUIDE.md, APP_VERSION_UPGRADE_GUIDE.md)
- Review Android developer documentation
- Check existing issues in the repository

## Summary

### Icon Creation
- 📖 See [APP_ICON_GUIDE.md](APP_ICON_GUIDE.md) for complete instructions
- 🎨 Use Android Studio Image Asset Studio (easiest method)
- ✅ Example vector icon provided in `app/src/main/res/drawable/ic_launcher_vector_example.xml`

### App Updates  
- 📖 See [APP_VERSION_UPGRADE_GUIDE.md](APP_VERSION_UPGRADE_GUIDE.md) for complete guide
- ✅ Android automatically handles upgrades - no manual uninstall needed!
- ✅ Just increment versionCode before each release
- ✅ User data is automatically preserved

---

**Happy coding! 🚀**
