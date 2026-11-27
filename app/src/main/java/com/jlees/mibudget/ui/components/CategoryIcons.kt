package com.jlees.mibudget.ui.components

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icons available for categories. Keys are stored in the database / export, so only ever
 * add keys — never rename them.
 */
object CategoryIcons {
    val all: Map<String, ImageVector> = linkedMapOf(
        "folder" to Sym.Folder,
        "home" to Sym.Home,
        "handyman" to Sym.Handyman,
        "chair" to Sym.Chair,
        "bolt" to Sym.Bolt,
        "lightbulb" to Sym.Lightbulb,
        "water" to Sym.WaterDrop,
        "fire" to Sym.LocalFireDepartment,
        "wifi" to Sym.Wifi,
        "phone" to Sym.PhoneAndroid,
        "delete" to Sym.Delete,
        "cart" to Sym.ShoppingCart,
        "restaurant" to Sym.Restaurant,
        "fastfood" to Sym.Fastfood,
        "coffee" to Sym.LocalCafe,
        "delivery" to Sym.DeliveryDining,
        "bar" to Sym.LocalBar,
        "pizza" to Sym.LocalPizza,
        "car" to Sym.DirectionsCar,
        "gas" to Sym.LocalGasStation,
        "parking" to Sym.LocalParking,
        "taxi" to Sym.LocalTaxi,
        "bus" to Sym.DirectionsBus,
        "train" to Sym.Train,
        "bike" to Sym.PedalBike,
        "build" to Sym.Build,
        "movie" to Sym.Movie,
        "tv" to Sym.LiveTv,
        "games" to Sym.SportsEsports,
        "music" to Sym.MusicNote,
        "ticket" to Sym.ConfirmationNumber,
        "brush" to Sym.Brush,
        "bag" to Sym.ShoppingBag,
        "checkroom" to Sym.Checkroom,
        "devices" to Sym.Devices,
        "cleaning" to Sym.CleaningServices,
        "store" to Sym.Storefront,
        "health" to Sym.Favorite,
        "hospital" to Sym.LocalHospital,
        "pharmacy" to Sym.LocalPharmacy,
        "fitness" to Sym.FitnessCenter,
        "face" to Sym.Face,
        "shield" to Sym.Shield,
        "spa" to Sym.Spa,
        "apps" to Sym.Apps,
        "cloud" to Sym.Cloud,
        "school" to Sym.School,
        "book" to Sym.AutoStories,
        "flight" to Sym.Flight,
        "hotel" to Sym.Hotel,
        "beach" to Sym.BeachAccess,
        "gift" to Sym.CardGiftcard,
        "donate" to Sym.VolunteerActivism,
        "pets" to Sym.Pets,
        "child" to Sym.ChildCare,
        "card" to Sym.CreditCard,
        "receipt" to Sym.Receipt,
        "bank" to Sym.AccountBalance,
        "savings" to Sym.Savings,
        "payments" to Sym.Payments,
        "work" to Sym.Work,
        "celebration" to Sym.Celebration,
        "star" to Sym.Star,
        "sports" to Sym.SportsBasketball,
        "park" to Sym.Park,
        "laundry" to Sym.LocalLaundryService,
        "haircut" to Sym.ContentCut,
        "subscriptions" to Sym.Subscriptions,
        "computer" to Sym.Computer,
        "category" to Sym.Category,
    )

    fun get(key: String?): ImageVector = all[key] ?: Sym.Folder
}

/** Palette offered when picking a category color. */
val CategoryColors: List<Long> = listOf(
    0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5, 0xFF039BE5, 0xFF00ACC1,
    0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFC0CA33, 0xFFFDD835, 0xFFFFB300, 0xFFFB8C00, 0xFFF4511E,
    0xFF6D4C41, 0xFF757575, 0xFF546E7A, 0xFF2E5E4E,
)
