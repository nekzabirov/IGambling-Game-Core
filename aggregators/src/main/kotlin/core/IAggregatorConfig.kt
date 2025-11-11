package com.nekzabirov.aggregators.core

interface IAggregatorConfig {
    fun parse(data: Map<String, String>)
}