package io.horizontalsystems.bankwallet.modules.market.top100

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class MarketTop100ViewModel(
        private val service: MarketTop100Service,
        private val clearables: List<Clearable>
) : ViewModel() {

    val sortingFields: Array<Field> = Field.values()
    val periods by service::periods

    var sortingField: Field = Field.HighestCap
        set(value) {
            field = value

            syncViewItemsBySortingField()
        }
    var period by service::period

    val marketTopViewItemsLiveData = MutableLiveData<List<MarketTopViewItem>>()
    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
                .subscribe {
                    syncState(it)
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun syncState(state: MarketTop100Service.State) {
        loadingLiveData.postValue(state is MarketTop100Service.State.Loading)
        errorLiveData.postValue((state as? MarketTop100Service.State.Error)?.error?.let { convertErrorMessage(it) })

        if (state is MarketTop100Service.State.Loaded) {
            syncViewItemsBySortingField()
        }
    }

    private fun syncViewItemsBySortingField() {
        val sorted = when (sortingField) {
            Field.HighestCap -> service.marketTopItems.sortedByDescending { it.marketCap }
            Field.LowestCap -> service.marketTopItems.sortedBy { it.marketCap }
            Field.HighestVolume -> service.marketTopItems.sortedByDescending { it.volume }
            Field.LowestVolume -> service.marketTopItems.sortedBy { it.volume }
            Field.HighestPrice -> service.marketTopItems.sortedByDescending { it.rate }
            Field.LowestPrice -> service.marketTopItems.sortedBy { it.rate }
            Field.TopGainers -> service.marketTopItems.sortedByDescending { it.diff }
            Field.TopLosers -> service.marketTopItems.sortedBy { it.diff }
        }

        val viewItems = sorted.map {
            val formattedRate = App.numberFormatter.formatFiat(it.rate, service.currency.symbol, 2, 2)

            MarketTopViewItem(it.rank, it.coinCode, it.coinName, formattedRate, it.diff)
        }

        marketTopViewItemsLiveData.postValue(viewItems)
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }


    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
        super.onCleared()
    }

    fun refresh() {
        service.refresh()
    }
}

data class MarketTopViewItem(
        val rank: Int,
        val coinCode: String,
        val coinName: String,
        val rate: String,
        val diff: BigDecimal
)