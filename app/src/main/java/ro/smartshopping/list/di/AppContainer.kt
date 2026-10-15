package ro.smartshopping.list.di

import android.content.Context
import ro.smartshopping.list.data.local.AppDatabase
import ro.smartshopping.list.data.repository.AuthRepository
import ro.smartshopping.list.data.repository.AuthRepositoryImpl
import ro.smartshopping.list.data.repository.PriceTrackingRepository
import ro.smartshopping.list.data.repository.PriceTrackingRepositoryImpl
import ro.smartshopping.list.data.repository.ProductRepository
import ro.smartshopping.list.data.repository.ProductRepositoryImpl
import ro.smartshopping.list.data.repository.ShoppingListRepository
import ro.smartshopping.list.data.repository.ShoppingListRepositoryImpl
import ro.smartshopping.list.domain.usecase.CalculateCompletionRateUseCase
import ro.smartshopping.list.domain.usecase.CalculateFinancialSummaryUseCase
import ro.smartshopping.list.domain.usecase.CarryForwardUnpurchasedUseCase
import ro.smartshopping.list.domain.usecase.FormatListForSharingUseCase
import ro.smartshopping.list.domain.usecase.NormalizePriceUseCase
import ro.smartshopping.list.domain.usecase.RecordPurchasePriceUseCase

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    // Use cases
    val completionRateCalculator = CalculateCompletionRateUseCase()
    val financialCalculator = CalculateFinancialSummaryUseCase(completionRateCalculator)
    val carryForwardUseCase = CarryForwardUnpurchasedUseCase()
    val recordPriceUseCase = RecordPurchasePriceUseCase()
    val normalizePriceUseCase = NormalizePriceUseCase()
    val formatShareUseCase = FormatListForSharingUseCase(financialCalculator)
    val textImportParser = ro.smartshopping.list.domain.parser.SmartTextImportParser()

    // Repositories
    val shoppingListRepository: ShoppingListRepository by lazy {
        ShoppingListRepositoryImpl(
            listDao = database.shoppingListDao(),
            itemDao = database.shoppingListItemDao(),
            completionCalculator = completionRateCalculator,
            financialCalculator = financialCalculator
        )
    }

    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(
            productDao = database.productDao()
        )
    }

    val priceTrackingRepository: PriceTrackingRepository by lazy {
        PriceTrackingRepositoryImpl(
            priceRecordDao = database.priceRecordDao(),
            productDao = database.productDao()
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            userDao = database.userDao()
        )
    }

    val storeDealsRepository: ro.smartshopping.list.data.repository.StoreDealsRepository by lazy {
        ro.smartshopping.list.data.repository.StoreDealsRepositoryImpl()
    }

    val smartDiscountAgent: ro.smartshopping.list.domain.usecase.SmartDiscountAgentUseCase by lazy {
        ro.smartshopping.list.domain.usecase.SmartDiscountAgentUseCase(storeDealsRepository)
    }

    val loyaltyCardRepository: ro.smartshopping.list.data.repository.LoyaltyCardRepository by lazy {
        ro.smartshopping.list.data.repository.LoyaltyCardRepositoryImpl(
            loyaltyCardDao = database.loyaltyCardDao()
        )
    }

    val storeDealsWorker: ro.smartshopping.list.domain.worker.StoreDealsBackgroundWorker by lazy {
        ro.smartshopping.list.domain.worker.StoreDealsBackgroundWorker(
            context = context,
            listRepository = shoppingListRepository,
            storeDealsRepository = storeDealsRepository
        )
    }
}
