package com.carlocoins.workflows.flows.utils

import com.carlocoins.contracts.states.CarState
import com.carlocoins.contracts.states.CarloCoinState
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import java.math.RoundingMode

object QueryUtilities {

    inline fun <reified T : ContractState> getState(services: ServiceHub): Vault.Page<T> {
        val queryAllStatusServiceProvider = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        return services.vaultService.queryBy(queryAllStatusServiceProvider)
    }

    inline fun <reified T : LinearState> getLinearStateById(services: ServiceHub, linearId: String): Vault.Page<T> {
        val queryAllStatusServiceProvider = QueryCriteria.LinearStateQueryCriteria(status = Vault.StateStatus.UNCONSUMED,
                linearId = listOf(UniqueIdentifier.fromString(linearId)))
        return services.vaultService.queryBy(queryAllStatusServiceProvider)
    }

    inline fun <reified T : LinearState> getLinearStates(services: ServiceHub): Vault.Page<T> {
        val queryAllStatusServiceProvider = QueryCriteria.LinearStateQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        return services.vaultService.queryBy(queryAllStatusServiceProvider)
    }

    fun queryCarloCoinPointer(services: ServiceHub): List<TokenPointer<CarloCoinState>> {
        val page = getState<CarloCoinState>(services)
        val listOfPointers = mutableListOf<TokenPointer<CarloCoinState>>()
        page.states.forEach {
            listOfPointers.add(it.state.data.toPointer(CarloCoinState::class.java))
        }
        return listOfPointers
    }

    fun getTotalCarloCoins(services: ServiceHub, pointers: List<TokenPointer<CarloCoinState>>? = null): Pair<Long, Double> {
        val tokenPointers = pointers ?: queryCarloCoinPointer(services)
        val carloCoinState = getLinearStateById<CarloCoinState>(services,tokenPointers.first().pointer.pointer.id.toString())
        var totalAmountWithoutDollarParse : Long = 0L
        tokenPointers.forEach { tokenPointer ->
            totalAmountWithoutDollarParse += services.vaultService.tokenBalance(tokenPointer).quantity
        }
        val dollarParsedAmount = totalAmountWithoutDollarParse * carloCoinState.states.first().state.data.dollarBased/100
        return Pair(totalAmountWithoutDollarParse, dollarParsedAmount)
    }





}