package com.banno.android.gordontest

import org.junit.Assert
import org.junit.Test
import org.junit.runners.Suite.SuiteClasses

@SuiteClasses(
    NestedTestSuite::class,
    TestB::class,
    TestC::class,
)
class TestSuite

@SuiteClasses(
    TestA::class,
    TestB::class,
    FirstSubTestSuite::class,
)
class NestedTestSuite

@SuiteClasses(
    TestA::class,
)
class FirstSubTestSuite

@SuiteClasses(
    TestD::class,
)
class SecondSubTestSuite

@SuiteClasses
class EmptySuite

class TestA {
    @Test fun test1() = Assert.assertEquals(1, 1)

    @Test fun test2() = Assert.assertEquals(1, 1)
}

class TestB {
    @Test fun test1() = Assert.assertEquals(1, 1)

    @Test fun test2() = Assert.assertEquals(1, 1)
}

class TestC {
    @Test fun test1() = Assert.assertEquals(1, 1)

    @Test fun test2() = Assert.assertEquals(1, 1)
}

class TestD {
    @Test fun test1() = Assert.assertEquals(1, 1)

    @Test fun test2() = Assert.assertEquals(1, 1)
}
