/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.java_smt.test;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;

import org.junit.AssumptionViolatedException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.NumeralType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RunWith(Parameterized.class)
public class SolverTheoriesTest extends SolverBasedTest0 {

  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solver;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  @Test
  public void basicBoolTest() throws Exception {
    BooleanFormula a = bmgr.makeVariable("a");
    BooleanFormula b = bmgr.makeBoolean(false);
    BooleanFormula c = bmgr.xor(a, b);
    BooleanFormula d = bmgr.makeVariable("b");
    BooleanFormula e = bmgr.xor(a, d);

    BooleanFormula notImpl = bmgr.and(a, bmgr.not(e));

    assertThatFormula(a).implies(c);
    assertThatFormula(notImpl).isSatisfiable();
  }

  @Test
  public void basicIntTest() {
    IntegerFormula a = imgr.makeVariable("a");
    IntegerFormula b = imgr.makeVariable("b");
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  public void basisRatTest() throws Exception {
    requireRationals();

    RationalFormula a = rmgr.makeVariable("int_c");
    RationalFormula num = rmgr.makeNumber(4);

    BooleanFormula f = rmgr.equal(rmgr.add(a, a), num);
    assertThatFormula(f).isSatisfiable();
  }

  @Test
  public void intTest1() throws Exception {
    IntegerFormula a = imgr.makeVariable("int_a");
    IntegerFormula num = imgr.makeNumber(2);

    BooleanFormula f = imgr.equal(imgr.add(a, a), num);
    assertThatFormula(f).isSatisfiable();
  }

  @Test
  public void intTest2() throws Exception {
    IntegerFormula a = imgr.makeVariable("int_b");
    IntegerFormula num = imgr.makeNumber(1);

    BooleanFormula f = imgr.equal(imgr.add(a, a), num);
    assertThatFormula(f).isUnsatisfiable();
  }

  @Test
  public void intTest3_DivModLinear() throws Exception {
    IntegerFormula a = imgr.makeVariable("int_a");
    IntegerFormula b = imgr.makeVariable("int_b");

    IntegerFormula num10 = imgr.makeNumber(10);
    IntegerFormula num5 = imgr.makeNumber(5);
    IntegerFormula num3 = imgr.makeNumber(3);
    IntegerFormula num2 = imgr.makeNumber(2);
    IntegerFormula num1 = imgr.makeNumber(1);
    IntegerFormula num0 = imgr.makeNumber(0);

    BooleanFormula fa = imgr.equal(a, num10);
    BooleanFormula fb = imgr.equal(b, num2);
    BooleanFormula fADiv5;
    try {
      fADiv5 = imgr.equal(imgr.divide(a, num5), b);
    } catch (UnsupportedOperationException e) {
      throw new AssumptionViolatedException("Support for operation DIV is optional", e);
    }
    BooleanFormula fADiv3 = imgr.equal(imgr.divide(a, num3), num3);
    BooleanFormula fAMod5 = imgr.equal(imgr.modulo(a, num5), num0);
    BooleanFormula fAMod3 = imgr.equal(imgr.modulo(a, num3), num1);

    // check division-by-constant, a=10 && b=2 && a/5=b
    assertThatFormula(bmgr.and(fa, fb, fADiv5)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADiv5))).isUnsatisfiable();

    // check division-by-constant, a=10 && a/3=3
    assertThatFormula(bmgr.and(fa, fb, fADiv3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADiv3))).isUnsatisfiable();

    // check modulo-by-constant, a=10 && a%5=0
    assertThatFormula(bmgr.and(fa, fAMod5)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, bmgr.not(fAMod5))).isUnsatisfiable();

    // check modulo-by-constant, a=10 && a%3=1
    assertThatFormula(bmgr.and(fa, fAMod3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, bmgr.not(fAMod3))).isUnsatisfiable();
  }

  @Test
  public void intTest3_DivModNonLinear() throws Exception {
    // not all solvers support division-by-variable,
    // we guarantee soundness by allowing any value that yields SAT.

    IntegerFormula a = imgr.makeVariable("int_a");
    IntegerFormula b = imgr.makeVariable("int_b");

    IntegerFormula num10 = imgr.makeNumber(10);
    IntegerFormula num5 = imgr.makeNumber(5);
    IntegerFormula num2 = imgr.makeNumber(2);

    BooleanFormula fa = imgr.equal(a, num10);
    BooleanFormula fb = imgr.equal(b, num2);
    BooleanFormula fADivB;
    try {
      fADivB = imgr.equal(imgr.divide(a, b), num5);
    } catch (UnsupportedOperationException e) {
      throw new AssumptionViolatedException("Support for non-linear arithmetic is optional", e);
    }

    // check division-by-variable, a=10 && b=2 && a/b=5
    assertThatFormula(bmgr.and(fa, fb, fADivB)).isSatisfiable();

    // TODO disabled, because we would need the option
    // solver.solver.useNonLinearIntegerArithmetic=true.
    // assertThatFormula(bmgr.and(fa,fb,bmgr.not(fADivB))).isUnsatisfiable();
  }

  @Test
  public void intTest3_DivMod_NegativeNumbersLinear() throws Exception {
    IntegerFormula a = imgr.makeVariable("int_a");
    IntegerFormula b = imgr.makeVariable("int_b");

    IntegerFormula numNeg10 = imgr.makeNumber(-10);
    IntegerFormula num5 = imgr.makeNumber(5);
    IntegerFormula num4 = imgr.makeNumber(4);
    IntegerFormula numNeg4 = imgr.makeNumber(-4);
    IntegerFormula num3 = imgr.makeNumber(3);
    IntegerFormula numNeg3 = imgr.makeNumber(-3);
    IntegerFormula numNeg2 = imgr.makeNumber(-2);
    IntegerFormula num2 = imgr.makeNumber(2);
    IntegerFormula num0 = imgr.makeNumber(0);

    BooleanFormula fa = imgr.equal(a, numNeg10);
    BooleanFormula fb = imgr.equal(b, numNeg2);
    BooleanFormula fADiv5;
    try {
      fADiv5 = imgr.equal(imgr.divide(a, num5), b);
    } catch (UnsupportedOperationException e) {
      throw new AssumptionViolatedException("Support for operation DIV is optional", e);
    }
    BooleanFormula fADiv3 = imgr.equal(imgr.divide(a, num3), numNeg4);
    BooleanFormula fADivNeg3 = imgr.equal(imgr.divide(a, numNeg3), num4);
    BooleanFormula fAMod5 = imgr.equal(imgr.modulo(a, num5), num0);
    BooleanFormula fAMod3 = imgr.equal(imgr.modulo(a, num3), num2);
    BooleanFormula fAModNeg3 = imgr.equal(imgr.modulo(a, numNeg3), num2);

    // integer-division for negative numbers is __not__ C99-conform!
    // SMTlib always rounds against +/- infinity.

    // check division-by-constant, a=-10 && b=-2 && a/5=b
    assertThatFormula(bmgr.and(fa, fb, fADiv5)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADiv5))).isUnsatisfiable();

    // check division-by-constant, a=-10 && a/3=-4
    assertThatFormula(bmgr.and(fa, fb, fADiv3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADiv3))).isUnsatisfiable();

    // check division-by-constant, a=-10 && a/(-3)=4
    assertThatFormula(bmgr.and(fa, fb, fADivNeg3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADivNeg3))).isUnsatisfiable();

    // check modulo-by-constant, a=-10 && a%5=0
    assertThatFormula(bmgr.and(fa, fAMod5)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, bmgr.not(fAMod5))).isUnsatisfiable();

    // check modulo-by-constant, a=-10 && a%3=2
    assertThatFormula(bmgr.and(fa, fAMod3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, bmgr.not(fAMod3))).isUnsatisfiable();

    // check modulo-by-constant, a=-10 && a%(-3)=2
    assertThatFormula(bmgr.and(fa, fAModNeg3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, bmgr.not(fAModNeg3))).isUnsatisfiable();
  }

  @Test
  public void intTest3_DivMod_NegativeNumbersNonLinear() throws Exception {
    // TODO not all solvers support division-by-variable,
    // we guarantee soundness by allowing any value that yields SAT.

    IntegerFormula a = imgr.makeVariable("int_a");
    IntegerFormula b = imgr.makeVariable("int_b");

    IntegerFormula numNeg10 = imgr.makeNumber(-10);
    IntegerFormula num5 = imgr.makeNumber(5);
    IntegerFormula numNeg2 = imgr.makeNumber(-2);

    BooleanFormula fa = imgr.equal(a, numNeg10);
    BooleanFormula fb = imgr.equal(b, numNeg2);
    BooleanFormula fADivB;
    try {
      fADivB = imgr.equal(imgr.divide(a, b), num5);
    } catch (UnsupportedOperationException e) {
      throw new AssumptionViolatedException("Support for non-linear arithmetic is optional", e);
    }

    // integer-division for negative numbers is __not__ C99-conform!
    // SMTlib always rounds against +/- infinity.

    // check division-by-variable, a=-10 && b=-2 && a/b=5
    assertThatFormula(bmgr.and(fa, fb, fADivB)).isSatisfiable();
    // TODO disabled, because we would need the option
    // solver.solver.useNonLinearIntegerArithmetic=true.
    // assertThatFormula(bmgr.and(fa,fb,bmgr.not(fADivB))).isUnsatisfiable();
  }

  @Test
  public void intTestBV_DivMod() throws Exception {
    requireBitvectors();

    BitvectorFormula a = bvmgr.makeVariable(32, "int_a");
    BitvectorFormula b = bvmgr.makeVariable(32, "int_b");

    BitvectorFormula num10 = bvmgr.makeBitvector(32, 10);
    BitvectorFormula num5 = bvmgr.makeBitvector(32, 5);
    BitvectorFormula num3 = bvmgr.makeBitvector(32, 3);
    BitvectorFormula num2 = bvmgr.makeBitvector(32, 2);
    BitvectorFormula num1 = bvmgr.makeBitvector(32, 1);
    BitvectorFormula num0 = bvmgr.makeBitvector(32, 0);

    BooleanFormula fa = bvmgr.equal(a, num10);
    BooleanFormula fb = bvmgr.equal(b, num2);
    BooleanFormula fADiv5 = bvmgr.equal(bvmgr.divide(a, num5, true), b);
    BooleanFormula fADiv3 = bvmgr.equal(bvmgr.divide(a, num3, true), num3);
    BooleanFormula fADivB = bvmgr.equal(bvmgr.divide(a, b, true), num5);
    BooleanFormula fAMod5 = bvmgr.equal(bvmgr.modulo(a, num5, true), num0);
    BooleanFormula fAMod3 = bvmgr.equal(bvmgr.modulo(a, num3, true), num1);

    // check division-by-constant, a=10 && b=2 && a/5=b
    assertThatFormula(bmgr.and(fa, fb, fADiv5)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADiv5))).isUnsatisfiable();

    // check division-by-constant, a=10 && a/3=3
    assertThatFormula(bmgr.and(fa, fb, fADiv3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADiv3))).isUnsatisfiable();

    // check division-by-variable, a=10 && b=2 && a/b=5
    // TODO not all solvers support division-by-variable,
    // we guarantee soundness by allowing any value that yields SAT.
    assertThatFormula(bmgr.and(fa, fb, fADivB)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADivB))).isUnsatisfiable();

    // check modulo-by-constant, a=10 && a%5=0
    assertThatFormula(bmgr.and(fa, fAMod5)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, bmgr.not(fAMod5))).isUnsatisfiable();

    // check modulo-by-constant, a=10 && a%3=1
    assertThatFormula(bmgr.and(fa, fAMod3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, bmgr.not(fAMod3))).isUnsatisfiable();
  }

  @Test
  public void intTestBV_DivMod_NegativeNumbers() throws Exception {
    requireBitvectors();

    BitvectorFormula a = bvmgr.makeVariable(32, "int_a");
    BitvectorFormula b = bvmgr.makeVariable(32, "int_b");

    BitvectorFormula numNeg10 = bvmgr.makeBitvector(32, -10);
    BitvectorFormula num5 = bvmgr.makeBitvector(32, 5);
    BitvectorFormula num3 = bvmgr.makeBitvector(32, 3);
    BitvectorFormula numNeg3 = bvmgr.makeBitvector(32, -3);
    BitvectorFormula numNeg2 = bvmgr.makeBitvector(32, -2);
    BitvectorFormula numNeg1 = bvmgr.makeBitvector(32, -1);
    BitvectorFormula num0 = bvmgr.makeBitvector(32, 0);

    BooleanFormula fa = bvmgr.equal(a, numNeg10);
    BooleanFormula fb = bvmgr.equal(b, numNeg2);
    BooleanFormula fADiv5 = bvmgr.equal(bvmgr.divide(a, num5, true), b);
    BooleanFormula fADiv3 = bvmgr.equal(bvmgr.divide(a, num3, true), numNeg3);
    BooleanFormula fADivNeg3 = bvmgr.equal(bvmgr.divide(a, numNeg3, true), num3);
    BooleanFormula fADivB = bvmgr.equal(bvmgr.divide(a, b, true), num5);
    BooleanFormula fAMod5 = bvmgr.equal(bvmgr.modulo(a, num5, true), num0);
    BooleanFormula fAMod3 = bvmgr.equal(bvmgr.modulo(a, num3, true), numNeg1);
    BooleanFormula fAModNeg3 = bvmgr.equal(bvmgr.modulo(a, numNeg3, true), numNeg1);

    // bitvector-division for negative numbers is C99-conform!

    // check division-by-constant, a=-10 && b=-2 && a/5=b
    assertThatFormula(bmgr.and(fa, fb, fADiv5)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADiv5))).isUnsatisfiable();

    // check division-by-constant, a=-10 && a/3=-3
    assertThatFormula(bmgr.and(fa, fb, fADiv3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADiv3))).isUnsatisfiable();

    // check division-by-constant, a=-10 && a/(-3)=3
    assertThatFormula(bmgr.and(fa, fb, fADivNeg3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADivNeg3))).isUnsatisfiable();

    // check division-by-variable, a=-10 && b=-2 && a/b=5
    // TODO not all solvers support division-by-variable
    // we guarantee soundness by allowing any value, that yields SAT.
    assertThatFormula(bmgr.and(fa, fb, fADivB)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fADivB))).isUnsatisfiable();

    // check modulo-by-constant, a=-10 && a%5=0
    assertThatFormula(bmgr.and(fa, fAMod5)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, bmgr.not(fAMod5))).isUnsatisfiable();

    // check modulo-by-constant, a=-10 && a%3=-1
    assertThatFormula(bmgr.and(fa, fAMod3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, bmgr.not(fAMod3))).isUnsatisfiable();

    // check modulo-by-constant, a=-10 && a%(-3)=-1
    assertThatFormula(bmgr.and(fa, fAModNeg3)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, bmgr.not(fAModNeg3))).isUnsatisfiable();
  }

  @Test
  public void intTest4_ModularCongruence_Simple() throws Exception {
    final IntegerFormula x = imgr.makeVariable("x");
    final BooleanFormula f1 = imgr.modularCongruence(x, imgr.makeNumber(0), 2);
    final BooleanFormula f2 = imgr.equal(x, imgr.makeNumber(1));

    assertThatFormula(bmgr.and(f1, f2)).isUnsatisfiable();
  }

  @Test
  public void intTest4_ModularCongruence() throws Exception {
    IntegerFormula a = imgr.makeVariable("int_a");
    IntegerFormula b = imgr.makeVariable("int_b");
    IntegerFormula c = imgr.makeVariable("int_c");
    IntegerFormula d = imgr.makeVariable("int_d");
    IntegerFormula num10 = imgr.makeNumber(10);
    IntegerFormula num5 = imgr.makeNumber(5);
    IntegerFormula num0 = imgr.makeNumber(0);
    IntegerFormula numNeg5 = imgr.makeNumber(-5);

    BooleanFormula fa = imgr.equal(a, num10);
    BooleanFormula fb = imgr.equal(b, num5);
    BooleanFormula fc = imgr.equal(c, num0);
    BooleanFormula fd = imgr.equal(d, numNeg5);

    // we have equal results modulo 5
    BooleanFormula fConb5 = imgr.modularCongruence(a, b, 5);
    BooleanFormula fConc5 = imgr.modularCongruence(a, c, 5);
    BooleanFormula fCond5 = imgr.modularCongruence(a, d, 5);

    // we have different results modulo 7
    BooleanFormula fConb7 = imgr.modularCongruence(a, b, 7);
    BooleanFormula fConc7 = imgr.modularCongruence(a, c, 7);
    BooleanFormula fCond7 = imgr.modularCongruence(a, d, 7);

    // check modular congruence, a=10 && b=5 && (a mod 5 = b mod 5)
    assertThatFormula(bmgr.and(fa, fb, fConb5)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fConb5))).isUnsatisfiable();
    assertThatFormula(bmgr.and(fa, fc, fConc5)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fc, bmgr.not(fConc5))).isUnsatisfiable();
    assertThatFormula(bmgr.and(fa, fd, fCond5)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fd, bmgr.not(fCond5))).isUnsatisfiable();

    // check modular congruence, a=10 && b=5 && (a mod 7 != b mod 7)
    assertThatFormula(bmgr.and(fa, fb, fConb7)).isUnsatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fConb7))).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fc, fConc7)).isUnsatisfiable();
    assertThatFormula(bmgr.and(fa, fc, bmgr.not(fConc7))).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fd, fCond7)).isUnsatisfiable();
    assertThatFormula(bmgr.and(fa, fd, bmgr.not(fCond7))).isSatisfiable();
  }

  @Test
  public void intTest4_ModularCongruence_NegativeNumbers() throws Exception {
    IntegerFormula a = imgr.makeVariable("int_a");
    IntegerFormula b = imgr.makeVariable("int_b");
    IntegerFormula c = imgr.makeVariable("int_c");
    IntegerFormula num8 = imgr.makeNumber(8);
    IntegerFormula num3 = imgr.makeNumber(3);
    IntegerFormula numNeg2 = imgr.makeNumber(-2);

    BooleanFormula fa = imgr.equal(a, num8);
    BooleanFormula fb = imgr.equal(b, num3);
    BooleanFormula fc = imgr.equal(c, numNeg2);
    BooleanFormula fConb = imgr.modularCongruence(a, b, 5);
    BooleanFormula fConc = imgr.modularCongruence(a, c, 5);

    // check modular congruence, a=10 && b=5 && (a mod 5 = b mod 5)
    assertThatFormula(bmgr.and(fa, fb, fConb)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fb, bmgr.not(fConb))).isUnsatisfiable();
    assertThatFormula(bmgr.and(fa, fc, fConc)).isSatisfiable();
    assertThatFormula(bmgr.and(fa, fc, bmgr.not(fConc))).isUnsatisfiable();
  }

  @Test
  public void testHardCongruence() throws Exception {
    IntegerFormula a, b, c;
    a = imgr.makeVariable("a");
    b = imgr.makeVariable("b");
    c = imgr.makeVariable("c");
    List<BooleanFormula> constraints = new ArrayList<>();
    Random r = new Random(42);
    int bitSize = 8;
    BigInteger prime1 = BigInteger.probablePrime(bitSize, r);
    BigInteger prime2 = BigInteger.probablePrime(bitSize + 1, r);
    BigInteger prime3 = BigInteger.probablePrime(bitSize + 2, r);

    constraints.add(imgr.modularCongruence(imgr.add(a, imgr.makeNumber(1)), b, prime1));
    constraints.add(imgr.modularCongruence(b, c, prime2));
    constraints.add(imgr.modularCongruence(a, c, prime3));

    try (ProverEnvironment pe = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      pe.addConstraint(imgr.greaterThan(a, imgr.makeNumber(1)));
      pe.addConstraint(imgr.greaterThan(b, imgr.makeNumber(1)));
      pe.addConstraint(imgr.greaterThan(c, imgr.makeNumber(1)));
      pe.addConstraint(bmgr.and(constraints));

      assertThat(pe.isUnsat()).isFalse();

      try (Model m = pe.getModel()) {
        BigInteger aValue = m.evaluate(a);
        BigInteger bValue = m.evaluate(b);
        BigInteger cValue = m.evaluate(c);
        assertThat(aValue.add(BigInteger.ONE).subtract(bValue).mod(prime1))
            .isEqualTo(BigInteger.ZERO);
        assertThat(bValue.subtract(cValue).mod(prime2)).isEqualTo(BigInteger.ZERO);
        assertThat(aValue.subtract(cValue).mod(prime3)).isEqualTo(BigInteger.ZERO);
      }
    }
  }

  @Test
  public void realTest() throws Exception {
    requireRationals();

    RationalFormula a = rmgr.makeVariable("int_c");
    RationalFormula num = rmgr.makeNumber(1);

    BooleanFormula f = rmgr.equal(rmgr.add(a, a), num);
    assertThatFormula(f).isSatisfiable();
  }

  @Test
  public void test_BitvectorIsZeroAfterShiftLeft() throws Exception {
    requireBitvectors();

    BitvectorFormula one = bvmgr.makeBitvector(32, 1);

    // unsigned char
    BitvectorFormula a = bvmgr.makeVariable(8, "char_a");
    BitvectorFormula b = bvmgr.makeVariable(8, "char_b");
    BitvectorFormula rightOp = bvmgr.makeBitvector(32, 7);

    // 'cast' a to unsigned int
    a = bvmgr.extend(a, 32 - 8, false);
    b = bvmgr.extend(b, 32 - 8, false);
    a = bvmgr.or(a, one);
    b = bvmgr.or(b, one);
    a = bvmgr.extract(a, 7, 0, true);
    b = bvmgr.extract(b, 7, 0, true);
    a = bvmgr.extend(a, 32 - 8, false);
    b = bvmgr.extend(b, 32 - 8, false);

    a = bvmgr.shiftLeft(a, rightOp);
    b = bvmgr.shiftLeft(b, rightOp);
    a = bvmgr.extract(a, 7, 0, true);
    b = bvmgr.extract(b, 7, 0, true);
    BooleanFormula f = bmgr.not(bvmgr.equal(a, b));

    assertThatFormula(f).isUnsatisfiable();
  }

  @Test
  public void testUfWithBoolType() throws SolverException, InterruptedException {
    FunctionDeclaration<BooleanFormula> uf =
        fmgr.declareUF("fun_ib", FormulaType.BooleanType, FormulaType.IntegerType);
    BooleanFormula uf0 = fmgr.callUF(uf, ImmutableList.of(imgr.makeNumber(0)));
    BooleanFormula uf1 = fmgr.callUF(uf, ImmutableList.of(imgr.makeNumber(1)));
    BooleanFormula uf2 = fmgr.callUF(uf, ImmutableList.of(imgr.makeNumber(2)));

    BooleanFormula f01 = bmgr.xor(uf0, uf1);
    BooleanFormula f02 = bmgr.xor(uf0, uf2);
    BooleanFormula f12 = bmgr.xor(uf1, uf2);
    assertThatFormula(f01).isSatisfiable();
    assertThatFormula(f02).isSatisfiable();
    assertThatFormula(f12).isSatisfiable();

    BooleanFormula f = bmgr.and(ImmutableList.of(f01, f02, f12));
    assertThatFormula(f).isUnsatisfiable();
  }

  @Test
  @Ignore
  public void testUfWithBoolArg() throws SolverException, InterruptedException {
    // Not all SMT solvers support UFs with boolean arguments.
    // We can simulate this with "uf(ite(p,0,1))", but currently we do not need this.
    // Thus this test is disabled and the following is enabled.

    FunctionDeclaration<IntegerFormula> uf =
        fmgr.declareUF("fun_bi", FormulaType.IntegerType, FormulaType.BooleanType);
    IntegerFormula ufTrue = fmgr.callUF(uf, ImmutableList.of(bmgr.makeBoolean(true)));
    IntegerFormula ufFalse = fmgr.callUF(uf, ImmutableList.of(bmgr.makeBoolean(false)));

    BooleanFormula f = bmgr.not(imgr.equal(ufTrue, ufFalse));
    assertThat(f.toString()).isEmpty();
    assertThatFormula(f).isSatisfiable();
  }

  @Test(expected = IllegalArgumentException.class)
  @SuppressWarnings("CheckReturnValue")
  public void testUfWithBoolArg_unsupported() {
    fmgr.declareUF("fun_bi", FormulaType.IntegerType, FormulaType.BooleanType);
  }

  @Test
  public void quantifierEliminationTest1() throws Exception {
    requireQuantifiers();

    IntegerFormula var_B = imgr.makeVariable("b");
    IntegerFormula var_C = imgr.makeVariable("c");
    IntegerFormula num_2 = imgr.makeNumber(2);
    IntegerFormula num_1000 = imgr.makeNumber(1000);
    BooleanFormula eq_c_2 = imgr.equal(var_C, num_2);
    IntegerFormula minus_b_c = imgr.subtract(var_B, var_C);
    BooleanFormula gt_bMinusC_1000 = imgr.greaterThan(minus_b_c, num_1000);
    BooleanFormula and_cEq2_bMinusCgt1000 = bmgr.and(eq_c_2, gt_bMinusC_1000);

    BooleanFormula f = qmgr.exists(ImmutableList.of(var_C), and_cEq2_bMinusCgt1000);
    BooleanFormula result = qmgr.eliminateQuantifiers(f);
    assertThat(result.toString()).doesNotContain("exists");
    assertThat(result.toString()).doesNotContain("c");

    BooleanFormula expected = imgr.greaterOrEquals(var_B, imgr.makeNumber(1003));
    assertThatFormula(result).isEquivalentTo(expected);
  }

  @Test
  @Ignore
  public void quantifierEliminationTest2() throws Exception {
    requireQuantifiers();

    IntegerFormula i1 = imgr.makeVariable("i@1");
    IntegerFormula j1 = imgr.makeVariable("j@1");
    IntegerFormula j2 = imgr.makeVariable("j@2");
    IntegerFormula a1 = imgr.makeVariable("a@1");

    IntegerFormula _1 = imgr.makeNumber(1);
    IntegerFormula _minus1 = imgr.makeNumber(-1);

    IntegerFormula _1_plus_a1 = imgr.add(_1, a1);
    BooleanFormula not_j1_eq_minus1 = bmgr.not(imgr.equal(j1, _minus1));
    BooleanFormula i1_eq_1_plus_a1 = imgr.equal(i1, _1_plus_a1);

    IntegerFormula j2_plus_a1 = imgr.add(j2, a1);
    BooleanFormula j1_eq_j2_plus_a1 = imgr.equal(j1, j2_plus_a1);

    BooleanFormula fm = bmgr.and(i1_eq_1_plus_a1, not_j1_eq_minus1, j1_eq_j2_plus_a1);

    BooleanFormula q = qmgr.exists(ImmutableList.of(j1), fm);
    BooleanFormula result = qmgr.eliminateQuantifiers(q);
    assertThat(result.toString()).doesNotContain("exists");
    assertThat(result.toString()).doesNotContain("j@1");

    BooleanFormula expected = bmgr.not(imgr.equal(i1, j2));
    assertThatFormula(result).isEquivalentTo(expected);
  }

  @Test
  public void testGetFormulaType() {
    BooleanFormula _boolVar = bmgr.makeVariable("boolVar");
    assertThat(mgr.getFormulaType(_boolVar)).isEqualTo(FormulaType.BooleanType);

    IntegerFormula _intVar = imgr.makeNumber(1);
    assertThat(mgr.getFormulaType(_intVar)).isEqualTo(FormulaType.IntegerType);

    requireArrays();
    ArrayFormula<IntegerFormula, IntegerFormula> _arrayVar =
        amgr.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
    assertThat(mgr.getFormulaType(_arrayVar)).isInstanceOf(FormulaType.ArrayFormulaType.class);
  }

  @Test
  public void testMakeIntArray() {
    requireArrays();

    IntegerFormula _i = imgr.makeVariable("i");
    IntegerFormula _1 = imgr.makeNumber(1);
    IntegerFormula _i_plus_1 = imgr.add(_i, _1);

    ArrayFormula<IntegerFormula, IntegerFormula> _b =
        amgr.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);
    IntegerFormula _b_at_i_plus_1 = amgr.select(_b, _i_plus_1);

    if (solver == Solvers.MATHSAT5) {
      // Mathsat5 has a different internal representation of the formula
      assertThat(_b_at_i_plus_1.toString()).isEqualTo("(`read_int_int` b (`+_int` i 1))");
    } else if (solver == Solvers.PRINCESS) {
      assertThat(_b_at_i_plus_1.toString()).isEqualTo("select(b, (i + 1))");
    } else {
      assertThat(_b_at_i_plus_1.toString())
          .isEqualTo("(select b (+ i 1))"); // Compatibility to all solvers not guaranteed
    }
  }

  @Test
  public void testMakeBitVectorArray() {
    requireArrays();
    requireBitvectors();

    BitvectorFormula _i = mgr.getBitvectorFormulaManager().makeVariable(64, "i");
    ArrayFormula<BitvectorFormula, BitvectorFormula> _b =
        amgr.makeArray(
            "b",
            FormulaType.getBitvectorTypeWithSize(64),
            FormulaType.getBitvectorTypeWithSize(32));
    BitvectorFormula _b_at_i = amgr.select(_b, _i);

    if (solver == Solvers.MATHSAT5) {
      // Mathsat5 has a different internal representation of the formula
      assertThat(_b_at_i.toString()).isEqualTo("(`read_<BitVec, 64, >_<BitVec, 32, >` b i)");
    } else {
      assertThat(_b_at_i.toString())
          .isEqualTo("(select b i)"); // Compatibility to all solvers not guaranteed
    }
  }

  @Test
  public void testNestedRationalArray() {
    requireArrays();
    requireRationals();

    IntegerFormula _i = imgr.makeVariable("i");
    ArrayFormula<IntegerFormula, ArrayFormula<IntegerFormula, RationalFormula>> multi =
        amgr.makeArray(
            "multi",
            NumeralType.IntegerType,
            FormulaType.getArrayType(NumeralType.IntegerType, NumeralType.RationalType));

    RationalFormula valueInMulti = amgr.select(amgr.select(multi, _i), _i);

    if (solver == Solvers.MATHSAT5) {
      assertThat(valueInMulti.toString())
          .isEqualTo("(`read_int_rat` (`read_int_<Array, Int, Real, >` multi i) i)");
    } else {
      assertThat(valueInMulti.toString()).isEqualTo("(select (select multi i) i)");
    }
  }

  @Test
  public void testNestedBitVectorArray() {
    requireArrays();
    requireBitvectors();

    IntegerFormula _i = imgr.makeVariable("i");
    ArrayFormula<IntegerFormula, ArrayFormula<IntegerFormula, BitvectorFormula>> multi =
        amgr.makeArray(
            "multi",
            NumeralType.IntegerType,
            FormulaType.getArrayType(
                NumeralType.IntegerType, FormulaType.getBitvectorTypeWithSize(32)));

    BitvectorFormula valueInMulti = amgr.select(amgr.select(multi, _i), _i);

    if (solver == Solvers.MATHSAT5) {
      assertThat(valueInMulti.toString())
          .isEqualTo(
              "(`read_int_<BitVec, 32, >` (`read_int_<Array, Int, <BitVec, 32, >, "
                  + ">` multi i) i)");
    } else {
      assertThat(valueInMulti.toString()).isEqualTo("(select (select multi i) i)");
    }
  }

  @Test
  public void nonLinearMultiplication() throws SolverException, InterruptedException {
    IntegerFormula i2 = imgr.makeNumber(2);
    IntegerFormula i3 = imgr.makeNumber(3);
    IntegerFormula i4 = imgr.makeNumber(4);
    IntegerFormula x = imgr.makeVariable("x");
    IntegerFormula y = imgr.makeVariable("y");
    IntegerFormula z = imgr.makeVariable("z");

    IntegerFormula x_mult_y;
    try {
      x_mult_y = imgr.multiply(x, y);
    } catch (UnsupportedOperationException e) {
      // do nothing, this exception is fine here, because solvers do not need
      // to support non-linear arithmetic, we can then skip the test completely
      throw new AssumptionViolatedException("Support for non-linear arithmetic is optional", e);
    }

    BooleanFormula x_equal_2 = imgr.equal(i2, x);
    BooleanFormula y_equal_3 = imgr.equal(i3, y);
    BooleanFormula z_equal_4 = imgr.equal(i4, z);
    BooleanFormula z_equal_x_mult_y = imgr.equal(z, x_mult_y);

    try (ProverEnvironment env = context.newProverEnvironment()) {
      env.push(x_equal_2);
      env.push(y_equal_3);
      env.push(z_equal_4);
      env.push(z_equal_x_mult_y);
      assertThatEnvironment(env).isUnsatisfiable();
    }
  }

  @Test
  public void nonLinearDivision() throws SolverException, InterruptedException {
    IntegerFormula i2 = imgr.makeNumber(2);
    IntegerFormula i3 = imgr.makeNumber(3);
    IntegerFormula i4 = imgr.makeNumber(4);
    IntegerFormula x = imgr.makeVariable("x");
    IntegerFormula y = imgr.makeVariable("y");
    IntegerFormula z = imgr.makeVariable("z");

    IntegerFormula x_div_y;
    try {
      x_div_y = imgr.divide(x, y);
    } catch (UnsupportedOperationException e) {
      // do nothing, this exception is fine here, because solvers do not need
      // to support non-linear arithmetic, we can then skip the test completely
      throw new AssumptionViolatedException("Support for non-linear arithmetic is optional", e);
    }

    BooleanFormula x_equal_4 = imgr.equal(i4, x);
    BooleanFormula y_equal_2 = imgr.equal(i2, y);
    BooleanFormula z_equal_3 = imgr.equal(i3, z);
    BooleanFormula z_equal_x_div_y = imgr.equal(z, x_div_y);

    try (ProverEnvironment env = context.newProverEnvironment()) {
      env.push(x_equal_4);
      env.push(y_equal_2);
      env.push(z_equal_3);
      env.push(z_equal_x_div_y);
      assertThatEnvironment(env).isUnsatisfiable();
    }
  }
}
