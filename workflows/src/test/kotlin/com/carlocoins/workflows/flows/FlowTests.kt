package com.carlocoins.workflows.flows

import com.carlocoins.contracts.states.CarState
import com.carlocoins.contracts.states.CarloCoinState
import com.carlocoins.workflows.flows.cars.BuyCar
import com.carlocoins.workflows.flows.cars.IssueCars
import com.carlocoins.workflows.flows.utils.QueryUtilities
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

open class FlowTests {

    private val network = MockNetwork(MockNetworkParameters(
            networkParameters = testNetworkParameters(minimumPlatformVersion = 5),
            cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.carlocoins.contracts"),
                    TestCordapp.findCordapp("com.carlocoins.workflows"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
            )))

    private val suellen = network.createNode(CordaX500Name(organisation = "Suellen", locality = "Sao Paulo",
            country = "BR", commonName = "Suellen", organisationUnit = "Suellen", state = "SP"))
    private val carlao = network.createNode(CordaX500Name(organisation = "Carlao", locality = "Sao Paulo",
            country = "BR", commonName = "Carlao", organisationUnit = "Carlao", state = "SP"))
    private val bruno = network.createNode(CordaX500Name(organisation = "Bruno", locality = "Sao Paulo",
            country = "BR", commonName = "Bruno", organisationUnit = "Bruno", state = "SP"))
    private val g2 = network.createNode(CordaX500Name(organisation = "gui2", locality = "Sao Paulo",
            country = "BR", commonName = "gui2", organisationUnit = "gui2", state = "SP"))
    private val newNotary = network.defaultNotaryNode

    init {
        listOf(bruno, suellen, carlao, carlao, g2).forEach {
        }
    }

    @Before
    fun setup() = network.startNodes()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `CarloCoin Integration Test`(){



        /***
         * Issue new CarloCoinTokens
         */
        suellen.runFlow(IssueCarloTokens(
                currency = "BRL",
                amount = 150L,
                recipient = bruno.info.legalIdentities.first(),
                observers = getObservers()
                )).getOrThrow()

        var statesPointers = QueryUtilities.queryCarloCoinPointer(bruno.services)
        var statePointer = statesPointers.first()
        var tokenBalance = bruno.services.vaultService.tokenBalance(statePointer)

        assertEquals(amount(150L, statePointer).quantity, tokenBalance.quantity)

        val carlosStates2 = QueryUtilities.getLinearStateById<CarloCoinState>(services = carlao.services,
                linearId = statePointer.pointer.pointer.id.toString())

        /***
         * Moving some CarloCoins to a Observer Party
         */
        bruno.runFlow(MoveCarloTokens.MoveCarloTokenInitiating(
                quantity = 50L,
                recipient = carlao.info.legalIdentities.first())).getOrThrow()

        statesPointers = QueryUtilities.queryCarloCoinPointer(carlao.services)
        statePointer = statesPointers.first()
        tokenBalance = carlao.services.vaultService.tokenBalance(statePointer)

        assertEquals(amount(50L, statePointer).quantity, tokenBalance.quantity)

        statesPointers = QueryUtilities.queryCarloCoinPointer(bruno.services)
        statePointer = statesPointers.first()
        tokenBalance = bruno.services.vaultService.tokenBalance(statePointer)

        assertEquals(amount(100L, statePointer).quantity, tokenBalance.quantity)

        /***
         * Validating Totals
         */
        var totalValue = QueryUtilities.getTotalCarloCoins(services = bruno.services)
        assertEquals(100.0, totalValue.second)

        totalValue = QueryUtilities.getTotalCarloCoins(services = carlao.services)
        assertEquals(50.0, totalValue.second)

        /***
         * Update CarloCoinState
         */
        suellen.runFlow(UpdateCarloCoinState.UpdateCarloCoinStateInitiating(
                statePointer.pointer.pointer.id.toString(), getObservers())).getOrThrow()

        totalValue = QueryUtilities.getTotalCarloCoins(services = bruno.services)
        assertEquals(90.00, totalValue.second)

        /***
         * Moving some CarloCoins to a Non Observer Party
         */
        carlao.runFlow(MoveCarloTokens.MoveCarloTokenInitiating(3L, g2.info.legalIdentities.first())).getOrThrow()

        totalValue = QueryUtilities.getTotalCarloCoins(services = g2.services)
        assertEquals(2.7, totalValue.second)

        /***
         * Issue a car
         */
        val linearId = suellen.runFlow(IssueCars.IssueCarsInitiating(
                brand = "GM",
                model = "Camaro",
                dollarPrice = 89.0
        )).getOrThrow()

        var suellenCar = QueryUtilities.getLinearStateById<CarState>(services = suellen.services, linearId = linearId)

        var suellenCarByBruno = QueryUtilities.getLinearStateById<CarState>(services = bruno.services, linearId = linearId)

        bruno.runFlow(BuyCar.BuyCarInitianting(linearId = linearId, newOwner = bruno.info.legalIdentities.first())).getOrThrow()

        suellenCar = QueryUtilities.getLinearStateById<CarState>(services = suellen.services, linearId = linearId)

        suellenCarByBruno = QueryUtilities.getLinearStateById<CarState>(services = bruno.services, linearId = linearId)

        val newBrunoBalance = bruno.services.vaultService.tokenBalance(statePointer)
    }

    private fun getObservers(): List<Party>{
        return listOf<Party>(bruno.info.legalIdentities.first(), carlao.info.legalIdentities.first())
    }

    private fun <T> StartedMockNode.runFlow(logic: FlowLogic<T>): CordaFuture<T> {
        return transaction {
            val result = startFlow(logic)
            network.runNetwork()
            result
        }
    }

}


