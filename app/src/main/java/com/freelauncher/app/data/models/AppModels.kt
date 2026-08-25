package com.freelauncher.app.data.models

data class AppCategoryInfo(
    val id: String,
    val title: String,
    val isCustom: Boolean = false,
    val isHidden: Boolean = false,
    val order: Int = 0
)

data class AppItem(
    val id: String = "",
    val packageName: String,
    val activityName: String = "",
    val label: String,
    val category: AppCategory = AppCategory.TOOLS,
    val categoryId: String = category.name,
    val categoryTitle: String = category.title,
    val monogram: String = "",
    val isPinned: Boolean = false,
    val pinIndex: Int = -1
)

enum class AppCategory(val title: String) {
    COMMUNICATION("COMMUNICATION"),
    SOCIAL("SOCIAL"),
    PRODUCTIVITY("PRODUCTIVITY"),
    WORK("WORK"),
    FINANCE("FINANCE"),
    SHOPPING("SHOPPING"),
    MEDIA("MEDIA"),
    PHOTOGRAPHY("PHOTOGRAPHY"),
    TRAVEL("TRAVEL"),
    FOOD("FOOD"),
    UTILITIES("UTILITIES"),
    TOOLS("TOOLS"),
    NEWS("NEWS"),
    GAMES("GAMES"),
    HEALTH_FITNESS("HEALTH & FITNESS"),
    EDUCATION("EDUCATION"),
    ENTERTAINMENT("ENTERTAINMENT"),
    SYSTEM("SYSTEM");

    companion object {
        fun fromId(id: String): AppCategory {
            return entries.find { it.name.equals(id, ignoreCase = true) } ?: TOOLS
        }
    }
}
