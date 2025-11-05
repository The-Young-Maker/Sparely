# Vault Persistence Fix

## Problem
Users were experiencing data loss where their vaults (SmartVaultEntity) would disappear after app updates or database schema changes. The issue was caused by the use of `.fallbackToDestructiveMigration()` in the Room database configuration.

## Root Cause
In `app/src/main/java/com/example/sparely/data/local/SparelyDatabase.kt`, the database was configured with:

```kotlin
.fallbackToDestructiveMigration()
```

This setting tells Room to drop all tables and recreate them from scratch whenever a database migration is missing or fails. While useful during development, this caused all user data (including vaults) to be lost when:
- The app was updated to a new version with a different database schema version
- Users had an older database version that didn't have a migration path to the current version

## Solution
The fix involves two changes:

### 1. Removed `.fallbackToDestructiveMigration()`
This prevents Room from automatically destroying user data when migrations are missing.

### 2. Added Comprehensive Migrations
Added migration paths from all previous database versions (1-11) to the current version (12). Each migration:
- Checks if tables and columns already exist before attempting to create/alter them
- Preserves all existing vault data
- Safely adds new columns with appropriate defaults
- Creates new tables that were introduced in later versions

The migration logic uses defensive programming to handle various starting states:
- Checks if tables exist before creating them
- Checks if columns exist before adding them
- Uses `CREATE TABLE IF NOT EXISTS` and similar safe SQL commands
- Provides sensible defaults for new non-nullable columns

## Migration Strategy
All migrations from versions 1-11 to version 12 use the same migration function (`migrateToV12`) which:

1. **Ensures smart_vaults table exists** with all required columns
2. **Ensures vault_auto_deposits table exists** with the `executeAutomatically` flag
3. **Ensures vault_contributions table exists** for tracking contributions
4. **Ensures vault_balance_adjustments table exists** for audit trail
5. **Creates allocation_history table** for persisting allocation suggestions
6. **Creates frozen_funds table** for tracking pending transactions
7. **Adds executeAutomatically column** to recurring_expenses if the table exists

## Impact
- **Vaults will now persist** across app updates and database schema changes
- **Existing vault data is preserved** when users upgrade from older versions
- **No data loss** during normal app operations

## Testing Recommendations
To verify this fix works correctly:

1. **Test fresh install**: Install the app on a clean device/emulator and create some vaults
2. **Test upgrade from old version**: 
   - Install an older version of the app (with database version < 12)
   - Create some vaults
   - Update to the new version
   - Verify vaults still exist with correct balances
3. **Test app data persistence**: Ensure vaults persist after:
   - App restart
   - Device restart
   - Several days of usage

## Notes
- If a user has a database version older than 1, they would need to start fresh (but this is highly unlikely)
- The migrations are designed to be idempotent - they can be run multiple times safely
- Future database changes should include proper migrations rather than relying on destructive migration
