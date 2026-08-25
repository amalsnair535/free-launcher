# Google Play Store Compatibility Report

This report summarizes the compatibility of **FREE Launcher** with Google Play Store policies and technical requirements as of August 25, 2026.

## Technical Requirements Checklist

| Requirement | Status | Details |
| :--- | :--- | :--- |
| **Target SDK** | ✅ Pass | Targeted at API 36, which exceeds the current requirement. |
| **Min SDK** | ✅ Pass | Set to API 24 (Android 7.0), ensuring a wide reach. |
| **Package Name** | ✅ Pass | `com.aistudio.freelauncher.zen` is unique and follows conventions. |
| **App Bundle (AAB)** | ✅ Pass | Build scripts are ready to generate signed release AABs. |
| **Adaptive Icons** | ✅ Pass | Adaptive and monochrome icons are correctly implemented. |
| **64-bit Support** | ✅ Pass | Implicitly supported by modern Kotlin/Android builds. |

## Sensitive Permissions Analysis

The following permissions require specific justifications or declarations in the Google Play Console:

### 1. `QUERY_ALL_PACKAGES`
> [!NOTE]
> **Status**: Justifiable
> **Use Case**: Core launcher functionality (listing and launching all apps).
> **Requirement**: You must complete the "Sensitive permissions declaration" in the Play Console.

### 2. `PACKAGE_USAGE_STATS`
> [!NOTE]
> **Status**: Justifiable
> **Use Case**: Digital Wellbeing / Productivity features.
> **Requirement**: Users must explicitly grant this in System Settings.

### 3. `READ_CONTACTS`
> [!NOTE]
> **Status**: Moderate Risk
> **Use Case**: Universal Search (finding contacts).
> **Requirement**: Ensure the privacy policy clearly explains why this is needed.

### 4. `READ_SMS`
> [!CAUTION]
> **Status: HIGH RISK**
> **Issue**: Google Play has extremely strict policies on SMS permissions. It is typically only allowed for apps that are the **Default SMS Handler**.
> **Recommendation**: If this feature is not "critical" to your launcher's identity, consider removing it or moving it to a separate "Optional Plugin" (though plugins are also heavily regulated). If you keep it, be prepared for a high chance of rejection unless you can prove it's a core requirement.

## Privacy Policy Audit

The current [privacy_policy.md](file:///C:/Users/USER/Downloads/free-launcher%20(2)/privacy_policy.md) is well-written but has one critical omission:

> [!WARNING]
> **Missing Permission**: The privacy policy does not mention **READ_SMS**, which is currently used in `UniversalSearchManager.kt`.
> **Action Required**: Add a section explaining the use of SMS permissions for searching messages locally on the device.

## Recommended Next Steps

1.  **Update Privacy Policy**: Add the `READ_SMS` justification.
2.  **Declare Permissions**: When uploading to the Play Console, prepare your justifications for `QUERY_ALL_PACKAGES` and `READ_SMS`.
3.  **Data Safety Section**: Prepare to declare that:
    *   Contacts and SMS are read but **not transmitted** off the device.
    *   App usage data is processed **locally**.
4.  **Testing**: Verify the release build (`minifyEnabled = true`) works correctly, as R8/ProGuard can sometimes break reflection or dynamic features (like RSS parsing).
