package com.jlees.mibudget.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jlees.mibudget.AppContainer
import com.jlees.mibudget.MiApp
import com.jlees.mibudget.ui.categories.CategoriesViewModel
import com.jlees.mibudget.ui.calendar.CalendarViewModel
import com.jlees.mibudget.ui.purchases.PurchaseEditViewModel
import com.jlees.mibudget.ui.purchases.PurchasesViewModel
import com.jlees.mibudget.ui.scan.ScanViewModel
import com.jlees.mibudget.ui.settings.ImportViewModel
import com.jlees.mibudget.ui.settings.PaymentMethodsViewModel
import com.jlees.mibudget.ui.settings.SettingsViewModel
import com.jlees.mibudget.ui.subscriptions.SubscriptionEditViewModel
import com.jlees.mibudget.ui.subscriptions.SubscriptionsViewModel

fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MiApp).container

object AppViewModels {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { CalendarViewModel(container()) }
        initializer { PurchasesViewModel(container(), createSavedStateHandle()) }
        initializer { PurchaseEditViewModel(container(), createSavedStateHandle()) }
        initializer { CategoriesViewModel(container(), createSavedStateHandle()) }
        initializer { SubscriptionsViewModel(container()) }
        initializer { SubscriptionEditViewModel(container(), createSavedStateHandle()) }
        initializer { ScanViewModel(container(), createSavedStateHandle()) }
        initializer { SettingsViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MiApp) }
        initializer { ImportViewModel(container()) }
        initializer { PaymentMethodsViewModel(container()) }
        initializer { com.jlees.mibudget.ui.tools.CheckSplitViewModel(container()) }
        initializer { com.jlees.mibudget.ui.tools.CurrencyViewModel(container()) }
        initializer { com.jlees.mibudget.ui.trash.RecentlyDeletedViewModel(container()) }
    }
}
