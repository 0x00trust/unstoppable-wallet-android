package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.storage.FavoriteCoin
import io.horizontalsystems.bankwallet.modules.market.top.Field
import io.horizontalsystems.bankwallet.modules.market.top.IMarketListDataSource
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.horizontalsystems.xrateskit.entities.TopMarket
import io.reactivex.Observable
import io.reactivex.Single

class MarketListFavoritesDataSource(
        private val xRateManager: IRateManager,
        private val marketFavoritesManager: MarketFavoritesManager
) : IMarketListDataSource() {

    override val sortingFields: Array<Field> = Field.values()
    override val dataUpdatedAsync: Observable<Unit> by marketFavoritesManager::dataUpdatedAsync
    private var cachedTopMarketList: List<TopMarket>? = null

    override fun doGetListAsync(currencyCode: String, fetchDiffPeriod: TimePeriod): Single<List<TopMarket>> {
        return getTopMarketList(currencyCode)
                .map {
                    it.filter { isCoinInFavorites(it, marketFavoritesManager.getAll()) }
                }
    }

    private fun getTopMarketList(currencyCode: String) = when {
        cachedTopMarketList != null -> {
            Single.just(cachedTopMarketList)
        }
        else -> {
            xRateManager.getTopMarketList(currencyCode, TimePeriod.HOUR_24)
                    .doOnSuccess {
                        cachedTopMarketList = it
                    }
        }
    }

    private fun isCoinInFavorites(topMarket: TopMarket, favoriteCoins: List<FavoriteCoin>): Boolean {
        return favoriteCoins.find { it.code == topMarket.coin.code } != null
    }

}