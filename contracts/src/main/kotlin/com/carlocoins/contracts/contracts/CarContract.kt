package com.carlocoins.contracts.contracts

import com.carlocoins.contracts.states.CarState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey


class CarContract: Contract {
    companion object {
        const val ID = "net.corda.core.transactions.LedgerTransaction"
    }

    override fun verify(tx: LedgerTransaction) {
        val requiredSigners = tx.commands.first().signers
        val command = tx.commands

        when (command.first().value) {
            is Commands.Issue -> verifyIssue(tx,requiredSigners)
            is Commands.Buy -> verifyBuy(tx, requiredSigners)
            else -> throw IllegalAccessError("Command $command not specified on the contract")
        }

    }

    interface Commands: CommandData {
        class Issue: Commands
        class Buy: Commands
    }

    fun verifyIssue(tx: LedgerTransaction, requiredSigners: List<PublicKey>) {
        requireThat {
            val inputs = tx.inputs
            val outputs = tx.outputs

            "No INPUT is needed in a Issue Transaction".using(inputs.isEmpty())
            "Just one OUTPUT is need in a Issue Transaction".using(outputs.size == 1)

            "The output need to be a CarState".using(outputs.single().data is CarState)

            val participants = tx.outputs.single().data.participants
            val carState = outputs.single().data as CarState

            "The owner and the seller needs to be the SAME".using( carState.owner == carState.seller )
            "The carloCoins paid value has to be 0".using(carState.carloCoinsPaidValue == 0.0)
            "The carloCoins linearID has to bem blank".using(carState.carloCoinLinearID == "")
            "The seller need to be in the transaction".using(participants.contains(carState.seller))
        }
    }

    fun verifyBuy(tx: LedgerTransaction, requiredSigners: List<PublicKey>) {
        requireThat {

        }
    }

}