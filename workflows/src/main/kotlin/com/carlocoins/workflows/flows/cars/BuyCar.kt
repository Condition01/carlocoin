package com.carlocoins.workflows.flows.cars

import co.paralleluniverse.fibers.Suspendable
import com.carlocoins.contracts.contracts.CarContract
import com.carlocoins.contracts.states.CarState
import com.carlocoins.contracts.states.CarloCoinState
import com.carlocoins.workflows.flows.utils.QueryUtilities
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import net.corda.core.contracts.Amount
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.math.RoundingMode

@StartableByRPC
object BuyCar {

    @InitiatingFlow
    class BuyCarInitianting(val linearId: String, val newOwner: Party): FlowLogic<String>(){

        @Suspendable
        override fun call(): String {

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val me = serviceHub.myInfo.legalIdentities.first()

            val carStateRef = QueryUtilities.getLinearStateById<CarState>(services = serviceHub, linearId = linearId)
                    .states.single()

            val carState = carStateRef.state.data

            val carloCoinPointer = QueryUtilities.queryCarloCoinPointer(services = serviceHub).first()

            val totalCarloCoins = QueryUtilities.getTotalCarloCoins(services = serviceHub, pointers = listOf(carloCoinPointer))

            val carPriceInCoins = calculateAmount(carState = carState, carloCoinPointer = carloCoinPointer)

            requireThat {
                "Você não tem carlocoins suficientes para realizar a compra".using(totalCarloCoins.first >= carPriceInCoins)
            }

            val carloCoinAmount: Amount<TokenType> = Amount(carPriceInCoins, carloCoinPointer as TokenType)

            val newCarState = carState.copy(owner = newOwner, carloCoinsPaidValue = totalCarloCoins.second, carloCoinLinearID = "")

            val command = CarContract.Commands.Buy()

            val txBuilder = TransactionBuilder(notary = notary).apply {
                addInputState(carStateRef)
                addOutputState(newCarState)
                addCommand(command, carState.participants.filter{ it.owningKey != me.owningKey } .map { it.owningKey })
            }

            addMoveFungibleTokens(
                    transactionBuilder = txBuilder,
                    serviceHub = serviceHub,
                    amount = carloCoinAmount,
                    holder = carState.owner,
                    changeHolder = me)

            val counterpartySessions = newCarState.participants.filter {
                ourIdentity != it
            }.map {
                initiateFlow(it)
            }

            val signedTransaction = serviceHub.signInitialTransaction(txBuilder)

            val fullySignedTx = subFlow(CollectSignaturesFlow(signedTransaction, counterpartySessions))

            subFlow(FinalityFlow(fullySignedTx, counterpartySessions))

            return carState.linearId.toString()

        }

        fun calculateAmount(carState: CarState, carloCoinPointer: TokenPointer<CarloCoinState>): Long {
            val carloCoinState = QueryUtilities.getLinearStateById<CarloCoinState>(
                    services = serviceHub,
                    linearId = carloCoinPointer.pointer.pointer.id.toString())
                    .states.single().state.data

            var calculatedPrice = carState.dollarPrice / carloCoinState.dollarBased
            calculatedPrice = calculatedPrice.toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
            return (calculatedPrice * 100).toLong()
        }

    }

    @InitiatedBy(BuyCarInitianting::class)
    class BuyCarResponder(val otherpartySession: FlowSession): FlowLogic<Unit>(){

        @Suspendable
        override fun call() {
            val signTransactionFlow = object : SignTransactionFlow(otherpartySession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                }
            }
            val txId = subFlow(signTransactionFlow).id
            subFlow(ReceiveFinalityFlow(otherpartySession, txId))
        }

    }

}