package com.carlocoins.contracts.states

import com.carlocoins.contracts.contracts.CarloCoinContract
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(CarloCoinContract::class)
data class CarloCoinState (val issuer: Party,
                           val dollarBased: Double,
                           val lastModifiedDate: Date,
                           override val linearId: UniqueIdentifier,
                           override val fractionDigits: Int = 2,
                           override val maintainers: List<Party> = listOf(issuer)) : EvolvableTokenType()