package org.koin.test.module

import org.junit.Assert.fail
import org.junit.Test
import org.koin.dsl.module.module
import org.koin.log.PrintLogger
import org.koin.test.AutoCloseKoinTest
import org.koin.test.checkModules
import org.koin.test.error.BrokenDefinitionException
import org.koin.test.ext.junit.assertDefinitions
import org.koin.test.ext.junit.assertRemainingInstanceHolders

class CheckModulesTest : AutoCloseKoinTest() {

    class ComponentA()
    class ComponentB(val componentA: ComponentA)

    interface Component
    class ComponentC() : Component
    class ComponentD(val component: Component)

    class ComponentE(val c: ComponentC) : Component
    class MyFactory(val msg : String)
    class ComponentF(val msg: String, component: ComponentA)

    @Test
    fun `successful check`() {
        checkModules(listOf(module {
            single { ComponentA() }
            single { ComponentB(get()) }
        }))

        assertDefinitions(2)
        assertRemainingInstanceHolders(2)
    }

    @Test
    fun `successful check with injection params`() {
        checkModules(listOf(module {
            single { (a : ComponentA) -> ComponentB(a) }
        }))

        assertDefinitions(1)
        assertRemainingInstanceHolders(1)
    }

    @Test
    fun `successful check with interface`() {
        checkModules(listOf(module {
            single { ComponentC() }
            single { ComponentE(get()) as Component }
        }))

        assertDefinitions(2)
        assertRemainingInstanceHolders(2)

    }

    @Test
    fun `unsuccessful check`() {
        try {
            checkModules(listOf(module {
                single { ComponentB(get()) }
            }))
            fail()
        } catch (e: BrokenDefinitionException) {
            System.err.println(e)
        }

        assertDefinitions(1)
    }

    @Test
    fun `interface definition check`() {
        checkModules(listOf(module {
            single { ComponentC() as Component }
            single { ComponentD(get()) }
        }))

        assertDefinitions(2)
        assertRemainingInstanceHolders(2)
    }

    @Test
    fun `multiple interface & module definition check`() {
        checkModules(listOf(module {
            single { ComponentC() as Component }
            module("otherModule") {
                single { ComponentC() as Component }
            }
            single { ComponentD(get()) }
        }))

        assertDefinitions(3)
        assertRemainingInstanceHolders(3)


    }

    @Test
    fun `mutiple module defs - check`() {
        checkModules(listOf(
            module {
                module("otherModule") {
                    single { ComponentC() as Component }
                }
                single { ComponentD(get()) }
            },
            module {
                single { ComponentC() as Component }
            }
        ))

        assertDefinitions(3)
        assertRemainingInstanceHolders(3)

    }

    @Test
    fun `multiple interface definition check`() {
        checkModules(listOf(module {
            single("default") { ComponentC() as Component }
            single("other") { ComponentC() as Component }
            single { ComponentD(get("default")) }
        }))

        assertDefinitions(3)
        assertRemainingInstanceHolders(3)

    }

    @Test
    fun `successful check definition with injection params`() {
        checkModules(listOf(module {
            factory { (msg : String) -> MyFactory(msg) }
        }),logger = PrintLogger(showDebug = true))

        assertDefinitions(1)
        assertRemainingInstanceHolders(1)
    }

    @Test
    fun `module with parameters fails when dependency missing`() {
        try {
            checkModules(listOf(module {
                single { (msg : String) -> ComponentF(msg, get()) }
            }))
            fail()
        } catch (e: BrokenDefinitionException) {
            System.err.println(e)
        }

        assertDefinitions(1)
    }
}
