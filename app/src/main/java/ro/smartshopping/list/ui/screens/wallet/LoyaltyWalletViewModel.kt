package ro.smartshopping.list.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ro.smartshopping.list.data.repository.LoyaltyCardRepository
import ro.smartshopping.list.domain.model.BarcodeType
import ro.smartshopping.list.domain.model.LoyaltyCard
import java.util.UUID

data class LoyaltyWalletUiState(
    val cards: List<LoyaltyCard> = emptyList(),
    val favoriteCards: List<LoyaltyCard> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCardForDisplay: LoyaltyCard? = null,
    val showAddDialog: Boolean = false,
    val showCameraScanner: Boolean = false,
    val showOnlyFavorites: Boolean = false
)

class LoyaltyWalletViewModel(
    private val repository: LoyaltyCardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoyaltyWalletUiState())
    val uiState: StateFlow<LoyaltyWalletUiState> = _uiState.asStateFlow()

    init {
        loadCards()
    }

    private fun loadCards() {
        viewModelScope.launch {
            repository.getAllCardsFlow().collectLatest { all ->
                val favs = all.filter { it.isFavorite }
                _uiState.value = _uiState.value.copy(
                    cards = all,
                    favoriteCards = favs,
                    isLoading = false
                )
            }
        }
    }

    fun openCardDisplay(card: LoyaltyCard) {
        _uiState.value = _uiState.value.copy(selectedCardForDisplay = card)
        viewModelScope.launch {
            repository.markCardScanned(card.id)
        }
    }

    fun closeCardDisplay() {
        _uiState.value = _uiState.value.copy(selectedCardForDisplay = null)
    }

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun closeAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun openCameraScanner() {
        _uiState.value = _uiState.value.copy(showCameraScanner = true)
    }

    fun closeCameraScanner() {
        _uiState.value = _uiState.value.copy(showCameraScanner = false)
    }

    fun toggleFavoritesFilter() {
        _uiState.value = _uiState.value.copy(showOnlyFavorites = !_uiState.value.showOnlyFavorites)
    }

    fun toggleFavorite(card: LoyaltyCard) {
        viewModelScope.launch {
            repository.toggleFavorite(card.id, !card.isFavorite)
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    fun addCard(
        storeName: String,
        programName: String,
        cardNumber: String,
        barcodeType: BarcodeType,
        primaryColorHex: String,
        secondaryColorHex: String,
        cardHolderName: String?,
        notes: String?,
        isFavorite: Boolean
    ) {
        viewModelScope.launch {
            val newCard = LoyaltyCard(
                id = UUID.randomUUID().toString(),
                storeName = storeName.trim(),
                programName = programName.trim(),
                cardNumber = cardNumber.trim(),
                barcodeType = barcodeType,
                primaryColorHex = primaryColorHex,
                secondaryColorHex = secondaryColorHex,
                cardHolderName = cardHolderName?.takeIf { it.isNotBlank() }?.trim(),
                notes = notes?.takeIf { it.isNotBlank() }?.trim(),
                isFavorite = isFavorite,
                createdAt = System.currentTimeMillis()
            )
            repository.saveCard(newCard)
            closeAddDialog()
        }
    }

    class Factory(private val repository: LoyaltyCardRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoyaltyWalletViewModel(repository) as T
        }
    }
}
