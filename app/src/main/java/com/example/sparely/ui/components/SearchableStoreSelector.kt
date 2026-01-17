package com.example.sparely.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.sparely.domain.model.Store
import com.example.sparely.domain.model.StoreInput
import kotlinx.coroutines.launch

/**
 * A searchable dropdown selector for stores/websites.
 * Users can search existing stores or add new ones with optional website URL for Brandfetch logos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableStoreSelector(
    stores: List<Store>,
    selectedStore: Store?,
    onStoreSelected: (Store?) -> Unit,
    onCreateStore: suspend (StoreInput) -> Store?,
    onEditStore: (Store) -> Unit = {},
    onDeleteStore: (Store) -> Unit = {},
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    brandfetchClientId: String? = null,
    brandSearchResults: List<com.example.sparely.data.remote.BrandfetchBrand> = emptyList(),
    onBrandSearch: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var showAddStoreDialog by remember { mutableStateOf(false) }
    var showEditStoreDialog by remember { mutableStateOf(false) }
    var storeToEdit by remember { mutableStateOf<Store?>(null) }
    var pendingStoreName by remember { mutableStateOf("") }
    var pendingWebsiteUrl by remember { mutableStateOf("") }
    
    val filteredStores = remember(stores, searchQuery) {
        if (searchQuery.isBlank()) {
            stores.take(10)
        } else {
            stores.filter { it.name.contains(searchQuery, ignoreCase = true) }.take(10)
        }
    }
    
    val showCreateOption = remember(searchQuery, stores) {
        searchQuery.isNotBlank() && stores.none { it.name.equals(searchQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text("Store/Website (optional)", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Track where you made this purchase",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        if (selectedStore != null) {
            // Show selected store as a chip with optional logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoreIcon(store = selectedStore, brandfetchClientId = brandfetchClientId, size = 32)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedStore.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (!selectedStore.websiteUrl.isNullOrBlank()) {
                        Text(
                            text = selectedStore.websiteUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { 
                    onStoreSelected(null)
                    onSearchQueryChange("") // Clear search when clearing selection
                }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear store"
                    )
                }
            }
        } else {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                SparelyTextField(
                    value = searchQuery,
                    onValueChange = { 
                        onSearchQueryChange(it)
                        if (it.length > 2) {
                            onBrandSearch(it)
                        } else {
                            onBrandSearch("")
                        }
                        if (!expanded) expanded = true
                    },
                    label = { Text("Search or add store") },
                    trailingIcon = { 
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                onSearchQueryChange("")
                                onBrandSearch("")
                            }) {
                                Icon(Icons.Default.Clear, "Clear search")
                            }
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // Option to clear/skip
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("None")
                                Text(
                                    text = "Skip store tracking for this expense",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onStoreSelected(null)
                            onSearchQueryChange("")
                            expanded = false
                        }
                    )
                    
                    // Filtered local stores
                    if (filteredStores.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = "Your Stores",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {},
                            enabled = false
                        )
                        
                        filteredStores.forEach { store ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(store.name)
                                        if (!store.websiteUrl.isNullOrBlank()) {
                                            Text(
                                                text = store.websiteUrl,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    StoreIcon(store = store, brandfetchClientId = brandfetchClientId, size = 24)
                                },
                                trailingIcon = {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                storeToEdit = store
                                                showEditStoreDialog = true
                                                expanded = false
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit store",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                onDeleteStore(store)
                                                if (selectedStore?.id == store.id) {
                                                    onStoreSelected(null)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete store",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onStoreSelected(store)
                                    onSearchQueryChange("")
                                    expanded = false
                                    onBrandSearch("") // Clear remote results
                                }
                            )
                        }
                    }
                    
                    // Remote brand results
                    if (brandSearchResults.isNotEmpty() && searchQuery.isNotBlank()) {
                         DropdownMenuItem(
                            text = { 
                                Text(
                                    text = "Found on Brandfetch",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {},
                            enabled = false
                        )
                        
                        brandSearchResults.forEach { brand ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(brand.name ?: "Unknown")
                                        if (!brand.domain.isNullOrBlank()) {
                                            Text(
                                                text = brand.domain,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    if (brand.iconUrl != null) {
                                        coil.compose.SubcomposeAsyncImage(
                                            model = brand.iconUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Store, null)
                                    }
                                },
                                onClick = {
                                    // Pre-fill dialog with brand info
                                    pendingStoreName = brand.name ?: searchQuery
                                    pendingWebsiteUrl = brand.domain ?: ""
                                    onSearchQueryChange("")
                                    expanded = false
                                    onBrandSearch("") // Clear results
                                    // We need to pass the domain to the dialog too
                                    showAddStoreDialog = true
                                    // We'll handle setting the URL in the dialog launch
                                }
                            )
                        }
                    } else if (searchQuery.isNotBlank() && stores.none { it.name.equals(searchQuery, ignoreCase = true) }) {
                        // Create new manually
                         DropdownMenuItem(
                            text = { 
                                Text(
                                    text = "Don't see it?",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                             onClick = {},
                            enabled = false
                        )
                        
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Add \"$searchQuery\"")
                                    Text(
                                        text = "Create custom store",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                pendingStoreName = searchQuery.trim()
                                onSearchQueryChange("")
                                expanded = false
                                showAddStoreDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Add Store Dialog with website URL input
    if (showAddStoreDialog) {
        AddStoreDialog(
            storeName = pendingStoreName,
            initialWebsiteUrl = pendingWebsiteUrl,
            onDismiss = { 
                showAddStoreDialog = false
                pendingStoreName = ""
                pendingWebsiteUrl = ""
            },
            onConfirm = { storeInput ->
                showAddStoreDialog = false
                pendingStoreName = ""
                pendingWebsiteUrl = ""
                scope.launch {
                    val newStore = onCreateStore(storeInput)
                    if (newStore != null) {
                        onStoreSelected(newStore)
                    }
                }
            }
        )
    }
    
    // Edit Store Dialog
    if (showEditStoreDialog && storeToEdit != null) {
        EditStoreDialog(
            store = storeToEdit!!,
            onDismiss = { 
                showEditStoreDialog = false
                storeToEdit = null
            },
            onConfirm = { updatedStore ->
                showEditStoreDialog = false
                storeToEdit = null
                onEditStore(updatedStore)
                // Update selection if we're editing the selected store
                if (selectedStore?.id == updatedStore.id) {
                    onStoreSelected(updatedStore)
                }
            }
        )
    }
}

/**
 * Displays a store icon - either a Brandfetch logo or a default store icon.
 */
@Composable
fun StoreIcon(
    store: Store,
    brandfetchClientId: String?,
    size: Int = 24
) {
    val logoUrl = store.getBrandfetchLogoUrl(brandfetchClientId)
    
    if (logoUrl != null) {
        coil.compose.SubcomposeAsyncImage(
            model = logoUrl,
            contentDescription = "${store.name} logo",
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            loading = {
                DefaultStoreIcon(size = size)
            },
            error = {
                DefaultStoreIcon(size = size)
            }
        )
    } else {
        DefaultStoreIcon(size = size)
    }
}

@Composable
private fun DefaultStoreIcon(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Store,
            contentDescription = null,
            modifier = Modifier.size((size * 0.6f).dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Dialog for adding a new store with optional website URL.
 */
@Composable
private fun AddStoreDialog(
    storeName: String,
    initialWebsiteUrl: String = "",
    onDismiss: () -> Unit,
    onConfirm: (StoreInput) -> Unit
) {
    var name by remember { mutableStateOf(storeName) }
    // Auto-generate initial URL from store name, or use provided initial URL
    var websiteUrl by remember(initialWebsiteUrl, storeName) { 
        mutableStateOf(
            if (initialWebsiteUrl.isNotBlank()) {
                initialWebsiteUrl
            } else if (storeName.isNotBlank()) {
                "${storeName.trim().lowercase().replace(" ", "")}.com"
            } else {
                ""
            }
        ) 
    }
    var userEditedUrl by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add New Store",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                SparelyTextField(
                    value = name,
                    onValueChange = { newName ->
                        name = newName
                        // Auto-update URL only if user hasn't manually edited it
                        if (!userEditedUrl && newName.isNotBlank()) {
                            websiteUrl = "${newName.trim().lowercase().replace(" ", "")}.com"
                        }
                    },
                    label = { Text("Store Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                SparelyTextField(
                    value = websiteUrl,
                    onValueChange = { 
                        websiteUrl = it
                        userEditedUrl = true // Mark as manually edited
                    },
                    label = { Text("Website URL (optional)") },
                    placeholder = { Text("e.g., amazon.com") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Adding a website URL will display the store's logo automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SparelyTextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    SparelyButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    StoreInput(
                                        name = name.trim(),
                                        websiteUrl = websiteUrl.trim().takeIf { it.isNotBlank() }
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for editing an existing store.
 */
@Composable
private fun EditStoreDialog(
    store: Store,
    onDismiss: () -> Unit,
    onConfirm: (Store) -> Unit
) {
    var name by remember { mutableStateOf(store.name) }
    var websiteUrl by remember { mutableStateOf(store.websiteUrl ?: "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Store",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                SparelyTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Store Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                SparelyTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = { Text("Website URL (optional)") },
                    placeholder = { Text("e.g., amazon.com") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "The website URL is used to fetch the store's logo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SparelyTextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    SparelyButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    store.copy(
                                        name = name.trim(),
                                        websiteUrl = websiteUrl.trim().takeIf { it.isNotBlank() }
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
