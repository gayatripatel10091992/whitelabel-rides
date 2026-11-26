package com.gayatripatel.rides.core

enum class GatewayId(val label: String) {
    CASH("Cash"),
    STRIPE("Card · Stripe"),
    RAZORPAY("UPI / Card · Razorpay"),
    FLUTTERWAVE_MPESA("M-Pesa · Flutterwave"),
    XENDIT("e-Wallet · Xendit"),
    OMISE("PromptPay · Omise"),
}

sealed interface PaymentResult {
    data class Success(val reference: String) : PaymentResult
    data class RequiresAction(val instructions: String) : PaymentResult
    data class Failed(val reason: String, val retryable: Boolean) : PaymentResult
}

/** One payment provider. Each brand enables a subset; the app never talks to a provider it doesn't need. */
interface PaymentGateway {
    val id: GatewayId
    val currencies: Set<String>
    fun charge(amountMinor: Long, currency: Currency, customerRef: String): PaymentResult
}

/** Brand-level switches, delivered from the admin panel as remote config. */
data class AdminPaymentConfig(
    val enabled: List<GatewayId>,
    val cashEnabled: Boolean = true,
    val maxCashMinor: Long = Long.MAX_VALUE,
)

class GatewayRegistry(gateways: List<PaymentGateway>) {
    private val byId = gateways.associateBy { it.id }

    /** Gateways the rider can pick for this fare, in the admin's order. Cash is offered last and capped. */
    fun available(config: AdminPaymentConfig, currency: Currency, amountMinor: Long): List<GatewayId> {
        val online = config.enabled.filter { it != GatewayId.CASH }.filter { id -> byId[id]?.currencies?.contains(currency.code) == true }
        val cash = if (config.cashEnabled && amountMinor <= config.maxCashMinor) listOf(GatewayId.CASH) else emptyList()
        return online + cash
    }

    fun charge(id: GatewayId, amountMinor: Long, currency: Currency, customerRef: String): PaymentResult {
        if (id == GatewayId.CASH) return PaymentResult.Success("cash-$customerRef")
        val gateway = byId[id] ?: return PaymentResult.Failed("${id.label} is not enabled", retryable = false)
        if (currency.code !in gateway.currencies) return PaymentResult.Failed("${id.label} doesn't support ${currency.code}", false)
        return gateway.charge(amountMinor, currency, customerRef)
    }
}

/** Sandboxed stand-ins for the real SDKs, with each provider's characteristic flow. */
object DemoGateways {
    private class Simple(
        override val id: GatewayId,
        override val currencies: Set<String>,
        private val flow: (Long) -> PaymentResult,
    ) : PaymentGateway {
        override fun charge(amountMinor: Long, currency: Currency, customerRef: String): PaymentResult = flow(amountMinor)
    }

    val all: List<PaymentGateway> = listOf(
        Simple(GatewayId.STRIPE, setOf("USD", "AED", "EUR", "GBP")) { PaymentResult.Success("pi_demo_$it") },
        Simple(GatewayId.RAZORPAY, setOf("INR")) { PaymentResult.Success("pay_demo_$it") },
        Simple(GatewayId.FLUTTERWAVE_MPESA, setOf("KES")) {
            PaymentResult.RequiresAction("Approve the M-Pesa prompt on your phone to pay")
        },
        Simple(GatewayId.XENDIT, setOf("IDR", "PHP")) { PaymentResult.Success("xnd_demo_$it") },
        Simple(GatewayId.OMISE, setOf("THB", "JPY")) { PaymentResult.Success("chrg_demo_$it") },
    )
}
