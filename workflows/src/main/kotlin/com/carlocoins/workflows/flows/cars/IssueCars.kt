package com.carlocoins.workflows.flows.cars

import co.paralleluniverse.fibers.Suspendable
import com.carlocoins.contracts.contracts.CarContract
import com.carlocoins.contracts.states.CarState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
object IssueCars {

    @InitiatingFlow
    class IssueCarsInitiating(
            val brand: String,
            val model: String,
            val dollarPrice: Double
    ): FlowLogic<String>() {

        @Suspendable
        override fun call(): String {

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val participants = serviceHub.networkMapCache.allNodes

            var parties = participants.map {
                it.legalIdentities.first()
            }.filter { it != notary }

            val me = serviceHub.myInfo.legalIdentities.first()

            val carState = CarState(
                    brand = brand,
                    model = model,
                    dollarPrice = dollarPrice,
                    carloCoinsPaidValue = 0.0,
                    carloCoinLinearID = "",
                    owner = me,
                    seller = me,
                    linearId = UniqueIdentifier(),
                    participants = parties
            )

            val signers = parties.map { it.owningKey }

            val command = Command(CarContract.Commands.Issue(), signers)

            val txBuilder = TransactionBuilder(notary = notary).apply {
                addOutputState(carState)
                addCommand(command)
            }

            val counterpartySessions = parties.filter {
                ourIdentity != it
            }.map {
                initiateFlow(it)
            }

            val signedTransaction = serviceHub.signInitialTransaction(txBuilder)

            val fullySignedTx = subFlow(CollectSignaturesFlow(signedTransaction, counterpartySessions))

            subFlow(FinalityFlow(fullySignedTx, counterpartySessions))

            return carState.linearId.toString()

        }

    }

    @InitiatedBy(IssueCarsInitiating::class)
    class IssueCarsResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                }
            }
            val txId = subFlow(signTransactionFlow).id
            subFlow(ReceiveFinalityFlow(counterpartySession, txId))
        }
    }

}

