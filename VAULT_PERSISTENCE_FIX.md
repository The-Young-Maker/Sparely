# Vault Persistence Fix

## Problem
Users were experiencing data loss where their vaults (SmartVaultEntity) would disappear. This could happen even without updating the app if the database schema didn't match what the code expected. The issue was caused by the use of `.fallbackToDestructiveMigration()` in the Room database configuration.

## Root Causes

### 1. Destructive Migration Fallback
In `app/src/main/java/com/example/sparely/data/local/SparelyDatabase.kt`, the database was configured with:

```kotlin
.fallbackToDestructiveMigration()
```

This setting tells Room to drop all tables and recreate them from scratch whenever a database migration is missing or fails. While useful during development, this caused all user data (including vaults) to be lost when:
- The app was updated to a new version with a different database schema version
- Users had an older database version that didn't have a migration path to the current version
- A migration failed for any reason

### 2. Missing createdAt Column in MIGRATION_11_12
The `SmartVaultEntity` was recently updated to include a `createdAt` field, but the existing `MIGRATION_11_12` didn't add this column. This meant:
- Users upgrading from version 11 to 12 would have vaults without the `createdAt` column
- The app would crash or malfunction when trying to read/write vault data
- This could cause the database to be considered corrupt, triggering the destructive migration fallback

## Solution
The fix involves three changes:

### 1. Removed `.fallbackToDestructiveMigration()`
This prevents Room from automatically destroying user data when migrations are missing or fail.

### 2. Added Comprehensive Migrations
Added migration paths from all previous database versions (1-11) to the current version (12). Each migration:
- Checks if tables and columns already exist before attempting to create/alter them
- Preserves all existing vault data
- Safely adds new columns with appropriate defaults
- Creates new tables that were introduced in later versions

The migration logic uses defensive programming to handle various starting states:
- Checks if tables exist before creating them (`CREATE TABLE IF NOT EXISTS`)
- Checks if columns exist before adding them (using `PRAGMA table_info`)
- Uses appropriate SQL commands to safely modify the schema
- Provides sensible defaults for new non-nullable columns (e.g., `archived INTEGER NOT NULL DEFAULT 0`)

### 3. Fixed MIGRATION_11_12 to Include createdAt Column
Updated `MIGRATION_11_12` to add the missing `createdAt` column:
```kotlin
addColumnIfMissing("ALTER TABLE smart_vaults ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0", "createdAt")
```

This ensures that users upgrading from version 11 to 12 will have all required columns.

## Migration Strategy
All migrations from versions 1-11 to version 12 use a shared migration function (`migrateToV12`) which:

1. **Ensures smart_vaults table exists** with all required columns (including `createdAt`)
2. **Ensures vault_auto_deposits table exists** with the `executeAutomatically` flag
3. **Ensures vault_contributions table exists** for tracking contributions
4. **Ensures vault_balance_adjustments table exists** for audit trail
5. **Creates allocation_history table** for persisting allocation suggestions
6. **Creates frozen_funds table** for tracking pending transactions
7. **Adds executeAutomatically column** to recurring_expenses if the table exists
8. **Adds createdAt column** to smart_vaults if missing (with default value of 0 = epoch)

## Impact
- **Vaults will now persist** across app updates and database schema changes
- **Existing vault data is preserved** when users upgrade from older versions
- **No data loss** during normal app operations
- **App won't crash** due to missing database columns

## Testing Recommendations
To verify this fix works correctly:

1. **Test fresh install**: Install the app on a clean device/emulator and create some vaults
2. **Test upgrade from old version**: 
   - Install an older version of the app (with database version < 12)
   - Create some vaults with balances and contributions
   - Update to the new version
   - Verify vaults still exist with correct balances and all data intact
3. **Test app data persistence**: Ensure vaults persist after:
   - App restart
   - Device restart
   - Several days of usage
4. **Test migration from version 11**: Specifically test upgrading from version 11 to ensure `createdAt` column is added

## Notes
- If a user has a database version older than 1, they would need to start fresh (but this is highly unlikely)
- The migrations are designed to be idempotent - they can be run multiple times safely
- Future database changes should include proper migrations rather than relying on destructive migration
- The `createdAt` field for existing vaults will default to epoch day 0 (January 1, 1970) after migration, but this is for historical vaults only and won't affect functionality

