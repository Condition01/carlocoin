package com.carlocoins.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.carlocoins.contracts.states.CarloCoinState
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import java.time.Instant
import java.util.*

@InitiatingFlow
@StartableByRPC
class IssueCarloTokens (
    val currency: String,
    val amount: Long,
    val recipient: Party,
    val observers: List<Party>
): FlowLogic<String>() {

    @Suspendable
    override fun call(): String {

        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val carloCoinState = CarloCoinState(
                issuer = ourIdentity,
                dollarBased = 1.0,
                lastModifiedDate = Date.from(Instant.now()),
                linearId = UniqueIdentifier())



        val transactionState = carloCoinState withNotary notary

        subFlow(CreateEvolvableTokens(transactionState, observers))

        val issuedCarloCoin = carloCoinState.toPointer(carloCoinState.javaClass) issuedBy ourIdentity

        val issuedAmount = amount(amount , issuedCarloCoin)

        val carloCoinToken = FungibleToken(issuedAmount, recipient, null)

        val stx = subFlow(IssueTokens(listOf(carloCoinToken), listOf(recipient)))

        return ("\n Generated + $amount of CarloCoins embased on currency: $currency with initial cotation ${carloCoinState.dollarBased}")
    }

}