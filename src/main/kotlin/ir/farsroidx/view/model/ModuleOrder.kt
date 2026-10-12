package ir.farsroidx.view.model

/**
 * Orders root modules first, then numeric priorities, then case-insensitive module names.
 *
 * Modules without a valid priority follow modules with explicit priorities.
 *
 * @param name the module display name
 * @param order optional numeric display priority
 * @param isRoot whether this is a root build module
 */
data class ModuleOrder(
    val name: String,
    val order: Long?,
    val isRoot: Boolean
) : Comparable<ModuleOrder> {

    /**
     * Compares root status, optional numeric priority, and case-insensitive module name.
     *
     * @param other the ordering key to compare
     *
     * @return a negative value, zero, or a positive value according to display order
     */
    override fun compareTo(other: ModuleOrder): Int {

        if (isRoot != other.isRoot) return if (isRoot) -1 else 1

        if (order != other.order) {

            if (order == null) return 1

            if (other.order == null) return -1

            return order.compareTo(other.order)

        }

        return name.compareTo(other.name, ignoreCase = true)

    }

}
