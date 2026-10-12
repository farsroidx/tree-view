package ir.farsroidx.view.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleOrderTest {

    @Test
    fun rootComparesEqualToItself() {

        val root = ModuleOrder("root", null, true)

        assertEquals(0, root.compareTo(root))

    }

    @Test
    fun rootPrecedesOrderedModules() {

        val root = ModuleOrder("root", null, true)

        val module = ModuleOrder("module", Long.MIN_VALUE, false)

        assertTrue(root < module)

        assertTrue(module > root)

    }

    @Test
    fun explicitOrderPrecedesAlphabeticalFallback() {

        val modules = listOf(
            ModuleOrder("alpha", null, false),
            ModuleOrder("zeta", 1, false),
            ModuleOrder("beta", 2, false),
            ModuleOrder("Alpha", 2, false)
        )

        assertEquals(
            listOf("zeta", "Alpha", "beta", "alpha"),
            modules.sorted().map { it.name }
        )

    }

    @Test
    fun orderingIsAntisymmetricAndTransitive() {

        val modules = listOf(
            ModuleOrder("root", null, true),
            ModuleOrder("alpha", Long.MIN_VALUE, false),
            ModuleOrder("beta", 0, false),
            ModuleOrder("gamma", Long.MAX_VALUE, false),
            ModuleOrder("delta", null, false)
        )

        modules.forEach { first ->

            modules.forEach { second ->

                assertEquals(
                    first.compareTo(second).sign(),
                    -second.compareTo(first).sign()
                )

                modules.forEach { third ->

                    if (first <= second && second <= third) {

                        assertTrue(first <= third)

                    }

                }

            }

        }

    }

    private fun Int.sign(): Int {

        return compareTo(0)

    }

}
