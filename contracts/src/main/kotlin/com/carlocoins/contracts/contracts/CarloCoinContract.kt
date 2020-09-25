package com.carlocoins.contracts.contracts
import com.carlocoins.contracts.states.CarloCoinState
import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.r3.corda.lib.tokens.contracts.commands.Create
import com.r3.corda.lib.tokens.contracts.commands.EvolvableTokenTypeCommand
import com.r3.corda.lib.tokens.contracts.commands.Update
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class CarloCoinContract : EvolvableTokenContract(), Contract {
    companion object {
        const val ID = "com.carlocoins.contracts.contracts.CarloCoinContract"
    }

    override fun additionalCreateChecks(tx: LedgerTransaction) {}

    override fun additionalUpdateChecks(tx: LedgerTransaction) {}

    override fun verify(tx: LedgerTransaction) {
        val requiredSigners = tx.commands.requireSingleCommand<EvolvableTokenTypeCommand>()
        val output = tx.outputs[0]
        val calorCoinsOutputState = output.data as CarloCoinState

        "The OutputState needs to be a CarloCoin".using(output.data is CarloCoinState)
        "O Issuer precisa ser a Suellen".using(calorCoinsOutputState.issuer.name.commonName == "Suellen")
        "Just ONE signer is required for this transaction".using(requiredSigners.signers.size == 1)

        super.verify(tx)
    }

}