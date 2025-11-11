package com.nekzabirov.catalog.table

import com.nekzabirov.aggregators.value.Aggregator
import com.nekzabirov.catalog.table.base.AbstractTable

object GameVariantTable : AbstractTable("game_variants") {
    val gameId = reference("game_id", GameTable.id).nullable()

    val symbol = varchar("symbol", 100)

    val name = varchar("name", 255)

    val providerName = varchar("provider_name", 60)

    val aggregator = enumeration<Aggregator>("aggregator")

    val playLines = integer("play_lines")

    val freeSpinEnable = bool("free_spin_enable")

    val freeChipEnable = bool("free_chip_enable")

    val jackpotEnable = bool("jackpot_enable")

    val demoEnable = bool("demo_enable")

    val bonusBuyEnable = bool("bonus_buy_enable")

    val locales = array<String>("locales")

    val platforms = array<String>("platforms")

    init {
        uniqueIndex(symbol, aggregator)
    }
}