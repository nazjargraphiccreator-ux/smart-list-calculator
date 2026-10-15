package ro.smartshopping.list.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActiveShoppingSessionManager {

    private val _activeListId = MutableStateFlow<String?>(null)
    val activeListId: StateFlow<String?> = _activeListId.asStateFlow()

    private val _activeListName = MutableStateFlow<String?>("Lista Curentă")
    val activeListName: StateFlow<String?> = _activeListName.asStateFlow()

    private val _activeStoreName = MutableStateFlow<String?>("Militari Residence")
    val activeStoreName: StateFlow<String?> = _activeStoreName.asStateFlow()

    private val _checkedCount = MutableStateFlow(0)
    val checkedCount: StateFlow<Int> = _checkedCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    fun updateActiveList(
        listId: String,
        name: String,
        store: String?,
        checked: Int = 0,
        total: Int = 0,
        amount: Double = 0.0
    ) {
        _activeListId.value = listId
        _activeListName.value = name
        _activeStoreName.value = store
        _checkedCount.value = checked
        _totalCount.value = total
        _totalAmount.value = amount
    }

    fun clear() {
        _activeListId.value = null
    }
}
