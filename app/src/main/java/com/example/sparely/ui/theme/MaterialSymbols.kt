package com.example.sparely.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sparely.R

/**
 * Helper object to migrate from old Material Icons to Material Symbols.
 * Maps icon names to drawable resources.
 * 
 * All icons are XML vector drawables (48px base size) that scale perfectly to any size.
 * Download from: https://fonts.google.com/icons
 * 1. Search for icon
 * 2. Click icon → Download → Android (XML)
 * 3. Place in res/drawable/
 * 4. Rename to follow pattern: iconname_48px.xml
 * 5. Add constant here
 * 
 * REQUIRED ICONS TO DOWNLOAD:
 * - arrow_back_48px.xml (Back navigation)
 * - bar_chart_48px.xml (History)
 * - savings_48px.xml (Goals/Save)
 * - account_balance_48px.xml (Budgets)
 * - emoji_events_48px.xml (Challenges)
 * - schedule_48px.xml (Recurring)
 * - favorite_48px.xml (Health)
 * - more_horiz_48px.xml (Settings)
 */
object MaterialSymbols {
    // Navigation & UI icons
    val HOME = R.drawable.home_48px
    val ARROW_BACK = R.drawable.arrow_back_48px
    val BAR_CHART = R.drawable.bar_chart_48px
    val SAVINGS = R.drawable.savings_48px
    val ACCOUNT_BALANCE = R.drawable.account_balance_48px
    val SCHEDULE = R.drawable.schedule_48px
    val FAVORITE = R.drawable.favorite_48px
    val SETTINGS = R.drawable.settings_48px
    val TRENDING_UP = R.drawable.trending_up_48px
    
    // Core UI icons
    val ADD = R.drawable.add_48px
    val EDIT = R.drawable.edit_48px
    val DELETE = R.drawable.delete_48px
    val REMOVE = R.drawable.remove_48px
    val CHECK = R.drawable.check_48px
    val CHECK_CIRCLE = R.drawable.check_circle_48px
    val CLOSE = R.drawable.close_48px
    val HISTORY = R.drawable.history_48px
    val LOCAL_FIRE_DEPARTMENT = R.drawable.local_fire_department_48px
    val SECURITY = R.drawable.security_48px
    val ARROW_DOWNWARD = R.drawable.arrow_downward_48px
    val ARROW_UPWARD = R.drawable.arrow_upward_48px
    val ARROW_FORWARD = R.drawable.arrow_forward_48px
    val SHOPPING_CART = R.drawable.shopping_cart_48px
    val RECEIPT = R.drawable.receipt_48px
    val PERSON = R.drawable.person_48px
    val CALENDAR_MONTH = R.drawable.calendar_month_48px
    val TODAY = R.drawable.today_48px
    val LIGHTBULB = R.drawable.lightbulb_48px
    val WARNING = R.drawable.warning_48px
    val INFO = R.drawable.info_48px
    val NOTIFICATIONS = R.drawable.notifications_48px
    val PLAY_ARROW = R.drawable.play_arrow_48px
    val TROPHY = R.drawable.trophy_48px
    val CELEBRATION = R.drawable.celebration_48px
    val BLOCK = R.drawable.block_48px
    val WORK = R.drawable.work_48px
    val CAKE = R.drawable.cake_48px
    val LOCK = R.drawable.lock_48px
    val ACCOUNT_BALANCE_WALLET = R.drawable.account_balance_wallet_48px
    val FLAG = R.drawable.flag_48px
    val ATTACH_MONEY = R.drawable.attach_money_48px
    val ROCKET_LAUNCH = R.drawable.rocket_launch_48px
    val PUBLIC = R.drawable.public_48px
    val REFRESH = R.drawable.refresh_48px
    val LIST = R.drawable.list_48px
    val SYNC = R.drawable.sync_48px
}

/**
 * Composable Icon component that uses Material Symbols drawables.
 * Drop-in replacement for Material Icons.
 * 
 * All icons should be 48px base size and will be scaled to the desired size.
 * 
 * @param icon Drawable resource ID from MaterialSymbols object
 * @param contentDescription Description for accessibility
 * @param modifier Modifier for the icon
 * @param size Size of the icon in dp (default 24dp)
 * @param tint Color to tint the icon
 */
@Composable
fun MaterialSymbolIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current
) {
    Icon(
        painter = painterResource(id = icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}

/**
 * Extension function to get icon by name (useful for dynamic icon selection)
 */
fun MaterialSymbols.getIconByName(name: String): Int? {
    return when (name.lowercase()) {
        "home" -> HOME
        // Uncomment as icons are added:
        // "arrow_back", "back" -> ARROW_BACK
        // "bar_chart", "chart", "history" -> BAR_CHART
        // "savings", "save", "goals" -> SAVINGS
        // "account_balance", "balance", "budgets" -> ACCOUNT_BALANCE
        // "emoji_events", "events", "challenges" -> EMOJI_EVENTS
        // "schedule", "recurring" -> SCHEDULE
        // "favorite", "health" -> FAVORITE
        // "more_horiz", "more", "settings" -> MORE_HORIZ
        else -> null
    }
}
