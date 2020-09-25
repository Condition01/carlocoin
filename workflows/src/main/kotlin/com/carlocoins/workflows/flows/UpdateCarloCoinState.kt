package com.carlocoins.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.carlocoins.contracts.states.CarloCoinState
import com.carlocoins.workflows.flows.utils.QueryUtilities
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlowHandler
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import net.corda.core.flows.*
import net.corda.core.identity.Party
import java.time.Instant
import java.util.*

@StartableByRPC
object UpdateCarloCoinState {

    @InitiatingFlow
    class UpdateCarloCoinStateInitiating(val linearId: String, val observers: List<Party>): FlowLogic<String>() {

        @Suspendable
        override fun call(): String {
            val identities = serviceHub.networkMapCache.allNodes

            val obSessions: MutableList<FlowSession> = ArrayList()
            for (observer in observers) {
                obSessions.add(initiateFlow(observer))
            }

            val carloCoinStatesRef = QueryUtilities.getLinearStateById<CarloCoinState>(
                    services = serviceHub,
                    linearId = linearId)

            val carloCoinStateRef = carloCoinStatesRef.states.single()

            val carloCoin = carloCoinStateRef.state.data

            val outputState = carloCoin.copy(dollarBased = 0.9, lastModifiedDate = Date.from(Instant.now()))

            val stx = subFlow(UpdateEvolvableTokenFlow(carloCoinStatesRef.states.single(), outputState, listOf(), obSessions))

            subFlow(UpdateDistributionListFlow(stx));

            return "Updated CarloCoinState with $linearId to a cotation of ${outputState.dollarBased}"
        }

    }

    @InitiatedBy(UpdateCarloCoinStateInitiating::class)
    class UpdateCarloCoinStateResponder(val counterpartySession: FlowSession): FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            subFlow(UpdateEvolvableTokenFlowHandler(counterpartySession))
        }

    }

    //TODO PEGAR OS OBSERVERS BASEADOS NOS NODES OU ACCOUNTS QUE TEM TOKENS

}