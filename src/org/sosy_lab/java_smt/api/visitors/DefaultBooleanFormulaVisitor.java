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
package org.sosy_lab.java_smt.api.visitors;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;

import java.util.List;

/**
 * A formula visitor which allows for the default implementation.
 *
 * @param <R> Return type for each traversal operation.
 */
public abstract class DefaultBooleanFormulaVisitor<R> implements BooleanFormulaVisitor<R> {

  protected abstract R visitDefault();

  @Override
  public R visitConstant(boolean value) {
    return visitDefault();
  }

  @Override
  public R visitBoundVar(BooleanFormula var, int deBruijnIdx) {
    return visitDefault();
  }

  @Override
  public R visitAtom(BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> decl) {
    return visitDefault();
  }

  @Override
  public R visitNot(BooleanFormula pOperand) {
    return visitDefault();
  }

  @Override
  public R visitAnd(List<BooleanFormula> pOperands) {
    return visitDefault();
  }

  @Override
  public R visitOr(List<BooleanFormula> pOperands) {
    return visitDefault();
  }

  @Override
  public R visitXor(BooleanFormula operand1, BooleanFormula operand2) {
    return visitDefault();
  }

  @Override
  public R visitEquivalence(BooleanFormula pOperand1, BooleanFormula pOperand2) {
    return visitDefault();
  }

  @Override
  public R visitImplication(BooleanFormula pOperand1, BooleanFormula pOperand2) {
    return visitDefault();
  }

  @Override
  public R visitIfThenElse(
      BooleanFormula pCondition, BooleanFormula pThenFormula, BooleanFormula pElseFormula) {
    return visitDefault();
  }

  @Override
  public R visitQuantifier(
      Quantifier quantifier,
      BooleanFormula quantifiedAST,
      List<Formula> boundVars,
      BooleanFormula body) {
    return visitDefault();
  }
}
