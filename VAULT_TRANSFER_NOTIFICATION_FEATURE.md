# Vault Transfer Notification Workflow

## Overview
The Vault Transfer Notification Workflow allows users to perform vault transfers directly from their banking app using step-by-step notifications, without needing to constantly switch back to the Sparely app.

## How It Works

### User Flow
1. User navigates to **Vault Transfers** screen in Sparely
2. User taps **"Start Notification Workflow"** button
3. A persistent notification appears showing:
   - Current vault name
   - Total amount to transfer
   - Source breakdown (Income, Saving tax, Auto deposit, etc.)
   - Progress indicator (e.g., "Vault 1 of 3")
4. User switches to their banking app and performs the transfer
5. User taps **"Transferred"** in the notification
6. Notification automatically updates to show the next vault
7. Process repeats until all vaults are complete
8. Final notification dismisses automatically

### Actions
- **Transferred**: Marks all pending contributions for the current vault as reconciled and advances to the next vault
- **Dismiss**: Cancels the workflow and dismisses the notification

## Technical Implementation

### Components

#### 1. VaultTransferNotificationReceiver
- **Location**: `app/src/main/java/com/example/sparely/notifications/VaultTransferNotificationReceiver.kt`
- **Purpose**: BroadcastReceiver that handles notification actions
- **Key Methods**:
  - `handleTransferred()`: Reconciles contributions via `savingsRepository.reconcileVaultContribution()`, tracks progress, shows next vault or dismisses
  - `handleDismiss()`: Dismisses the notification
- **Progress Tracking**: Uses SharedPreferences (`vault_transfer_workflow`) to track completed vault count
- **Vault Lookup**: Uses `kotlinx.coroutines.flow.first()` on `savingsRepository.observeSmartVaults()` to get vault names

#### 2. NotificationHelper
- **Location**: `app/src/main/java/com/example/sparely/notifications/NotificationHelper.kt`
- **Added Methods**:
  - `showVaultTransferNotification()`: Creates notification with vault details and actions
  - `dismissVaultTransferNotification()`: Cancels the notification
- **Added Constants**:
  - `VAULT_TRANSFER_CHANNEL_ID = "sparely_vault_transfers"`
  - `VAULT_TRANSFER_NOTIFICATION_ID = 4001`
- **Channel Configuration**: High priority, ongoing notification, no badge

#### 3. NotificationScheduler
- **Location**: `app/src/main/java/com/example/sparely/notifications/NotificationScheduler.kt`
- **Added Methods**:
  - `showVaultTransferWorkflow()`: Initiates the workflow with first vault
  - `dismissVaultTransferWorkflow()`: Cancels the workflow

#### 4. SparelyViewModel
- **Location**: `app/src/main/java/com/example/sparely/ui/SparelyViewModel.kt`
- **Added Method**:
  - `startVaultTransferNotificationWorkflow()`: Triggers workflow from UI

#### 5. VaultTransfersScreen
- **Location**: `app/src/main/java/com/example/sparely/ui/screens/VaultTransfersScreen.kt`
- **Added UI**:
  - `NotificationWorkflowButton`: Card with explanation and button to start workflow
  - Parameter: `onStartNotificationWorkflow`

#### 6. AndroidManifest.xml
- **Registered**: `VaultTransferNotificationReceiver` as broadcast receiver

## Notification Details

### Title
Vault name (e.g., "Emergency Fund")

### Content
- Short text: "Transfer $X.XX • Vault 1 of 3"
- Expanded text includes source breakdown:
  ```
  Transfer $500.00 • Vault 1 of 3
  
  Saving tax: $300.00
  Income: $150.00
  Auto deposit: $50.00
  ```

### Properties
- **Priority**: High (appears at top of notification shade)
- **Ongoing**: True (user must interact with actions to dismiss)
- **Auto-cancel**: False (controlled by workflow logic)

## Data Flow

```
User taps "Start Notification Workflow"
    ↓
SparelyViewModel.startVaultTransferNotificationWorkflow()
    ↓
NotificationScheduler.showVaultTransferWorkflow()
    ↓ (queries pending contributions, groups by vault, resets counter)
NotificationHelper.showVaultTransferNotification()
    ↓ (shows first vault with "Transferred" and "Dismiss" actions)
[User performs bank transfer and taps "Transferred"]
    ↓
VaultTransferNotificationReceiver.handleTransferred()
    ↓ (reconciles contributions, increments counter)
    ├─ More vaults? → Show next notification
    └─ No more vaults? → Dismiss notification
```

## Progress Tracking

### SharedPreferences Key: `vault_transfer_workflow`
- **Key**: `completed_count` (Int)
- **Purpose**: Track how many vaults have been completed in current workflow
- **Reset**: When workflow starts or completes
- **Calculation**: 
  - Current index = completed_count
  - Total count = remaining vaults + completed_count

## Benefits

1. **Streamlined Banking**: Users can work through transfers in their banking app without switching contexts
2. **One Vault at a Time**: Reduces cognitive load by showing only the current vault
3. **Clear Progress**: Users see which vault they're on and how many remain
4. **Source Transparency**: Breakdown shows exactly where contributions came from
5. **Error Prevention**: Ongoing notification ensures users don't forget to complete workflow
6. **Flexible**: Users can dismiss and resume later if needed

## Future Enhancements

Potential improvements:
- Add "Skip" action to defer specific vault
- Support partial transfers with amount input
- Schedule recurring workflows
- Integration with banking APIs for automatic detection
- Notification summary when complete with total transferred
