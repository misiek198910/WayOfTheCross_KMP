package droga_krzyzowa.droga_krzyzowa.platform

interface IosSubscriptions {
    val cachedActive: Boolean

    fun start(onUpdate: (active: Boolean, price: String?, period: String?) -> Unit)

    fun purchase()

    fun restore()

    fun manage()
}
