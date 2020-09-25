package com.carlocoins.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.carlocoins.contracts.states.CarloCoinState
import com.carlocoins.workflows.flows.utils.QueryUtilities
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.Party


@StartableByRPC
object MoveCarloTokens {

    @InitiatingFlow
    class MoveCarloTokenInitiating(
            val quantity: Long,
            val recipient: Party
    ): FlowLogic<String>() {

        @Suspendable
        override fun call(): String {
            val carloPointers: List<TokenPointer<CarloCoinState>> = QueryUtilities.queryCarloCoinPointer(services = serviceHub)

            val carloPointer: TokenPointer<CarloCoinState> = carloPointers.first()

            val amount = amount(quantity , carloPointer as TokenType)

            val stx = subFlow(MoveFungibleTokens(amount, recipient))

            return ("\n Moved $quantity CarloCoin Tokens to $recipient")
        }
    }

    @InitiatedBy(MoveCarloTokenInitiating::class)
    class MoveCarloTokenResponder(val counterpartySession: FlowSession): FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            return subFlow(MoveFungibleTokensHandler(counterpartySession))
        }

    }

}