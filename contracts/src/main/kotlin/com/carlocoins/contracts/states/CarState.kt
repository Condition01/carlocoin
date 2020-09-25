package com.carlocoins.contracts.states

import com.carlocoins.contracts.contracts.CarContract
import com.carlocoins.contracts.contracts.CarloCoinContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(CarContract::class)
data class CarState (val brand: String,
                     val model: String,
                     val dollarPrice: Double,
                     val seller: Party,
                     val owner: Party,
                     val carloCoinsPaidValue: Double,
                     val carloCoinLinearID: String,
                     override val linearId: UniqueIdentifier,
                     override var participants: List<AbstractParty>
): LinearState