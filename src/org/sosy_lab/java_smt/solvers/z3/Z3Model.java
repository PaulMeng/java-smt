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
package org.sosy_lab.java_smt.solvers.z3;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.microsoft.z3.Native;

import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.basicimpl.AbstractModel.CachingAbstractModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

class Z3Model extends CachingAbstractModel<Long, Long, Long> {

  private final long model;
  private final long z3context;
  private static final Pattern Z3_IRRELEVANT_MODEL_TERM_PATTERN = Pattern.compile(".*![0-9]+");

  @SuppressWarnings("hiding")
  private final Z3FormulaCreator creator;

  private Z3Model(long z3context, long z3model, Z3FormulaCreator pCreator) {
    super(pCreator);
    Native.modelIncRef(z3context, z3model);
    model = z3model;
    this.z3context = z3context;
    creator = pCreator;
  }

  static Z3Model create(long z3context, long z3model, Z3FormulaCreator pCreator) {
    return new Z3Model(z3context, z3model, pCreator);
  }

  @Nullable
  @Override
  public Object evaluateImpl(Long f) {
    Native.LongPtr out = new Native.LongPtr();
    boolean status = Native.modelEval(z3context, model, f, false, out);
    Verify.verify(status, "Error during model evaluation");
    long outValue = out.value;

    if (creator.isConstant(outValue)) {
      return creator.convertValue(outValue);
    }

    // Z3 does not give us a direct API to query for "irrelevant" ASTs during evaluation.
    // The only hint we get is that the input AST is not simplified down to a constant:
    // thus, it is assumed to be irrelevant.
    return null;
  }

  @Override
  protected ImmutableList<ValueAssignment> modelToList() {
    Builder<ValueAssignment> out = ImmutableList.builder();

    // Iterate through constants.
    for (int constIdx = 0; constIdx < Native.modelGetNumConsts(z3context, model); constIdx++) {
      long keyDecl = Native.modelGetConstDecl(z3context, model, constIdx);
      Native.incRef(z3context, keyDecl);
      out.addAll(getConstAssignments(keyDecl));
      Native.decRef(z3context, keyDecl);
    }

    // Iterate through function applications.
    for (int funcIdx = 0; funcIdx < Native.modelGetNumFuncs(z3context, model); funcIdx++) {
      long funcDecl = Native.modelGetFuncDecl(z3context, model, funcIdx);
      Native.incRef(z3context, funcDecl);
      if (!isInternalSymbol(funcDecl)) {
        String functionName = creator.symbolToString(Native.getDeclName(z3context, funcDecl));
        out.addAll(getFunctionAssignments(funcDecl, funcDecl, functionName));
      }
      Native.decRef(z3context, funcDecl);
    }

    return out.build();
  }

  /**
   * The symbol "!" is part of temporary symbols used for quantified formulas or aliases.
   * This method is only a heuristic, because the user can also create a symbol containing "!".
   **/
  private boolean isInternalSymbol(long funcDecl) {
    return Z3_IRRELEVANT_MODEL_TERM_PATTERN
        .matcher(creator.symbolToString(Native.getDeclName(z3context, funcDecl)))
        .matches();
  }

  /**
   * @return ValueAssignments for a constant declaration in the model
   * */
  private Collection<ValueAssignment> getConstAssignments(long keyDecl) {
    Preconditions.checkArgument(
        Native.getArity(z3context, keyDecl) == 0, "Declaration is not a constant");

    long var = Native.mkApp(z3context, keyDecl, 0, new long[] {});
    Formula key = creator.encapsulateWithTypeOf(var);

    long value = Native.modelGetConstInterp(z3context, model, keyDecl);
    checkReturnValue(value, keyDecl);
    Native.incRef(z3context, value);

    try {
      long symbol = Native.getDeclName(z3context, keyDecl);
      if (creator.isConstant(value)) {
        return Collections.singletonList(
            new ValueAssignment(
                key,
                creator.symbolToString(symbol),
                creator.convertValue(value),
                ImmutableList.of()));

      } else if (Native.isAsArray(z3context, value)) {
        long arrayFormula = Native.mkConst(z3context, symbol, Native.getSort(z3context, value));
        Native.incRef(z3context, arrayFormula);
        return getArrayAssignments(symbol, arrayFormula, value, Collections.emptyList());

      } else {
        throw new UnsupportedOperationException(
            "unknown model evaluation: " + Native.astToString(z3context, value));
      }

    } finally {
      // cleanup outdated data
      Native.decRef(z3context, value);
    }
  }

  /**
   * Z3 models an array as an uninterpreted function.
   *
   * @return a list of assignments {@code a[1]=0; a[2]=0; a[5]=0}.
   */
  private Collection<ValueAssignment> getArrayAssignments(
      long arraySymbol, long arrayFormula, long value, List<Object> upperIndices) {
    long evalDecl = Native.getAsArrayFuncDecl(z3context, value);
    Native.incRef(z3context, evalDecl);
    long interp = Native.modelGetFuncInterp(z3context, model, evalDecl);
    checkReturnValue(interp, evalDecl);
    Native.funcInterpIncRef(z3context, interp);

    Collection<ValueAssignment> lst = new ArrayList<>();

    // get all assignments for the array
    int numInterpretations = Native.funcInterpGetNumEntries(z3context, interp);
    for (int interpIdx = 0; interpIdx < numInterpretations; interpIdx++) {
      long entry = Native.funcInterpGetEntry(z3context, interp, interpIdx);
      Native.funcEntryIncRef(z3context, entry);
      long arrayValue = Native.funcEntryGetValue(z3context, entry);
      Native.incRef(z3context, arrayValue);
      int noArgs = Native.funcEntryGetNumArgs(z3context, entry);
      assert noArgs == 1 : "array modelled as UF is expected to have only one parameter, aka index";
      long arrayIndex = Native.funcEntryGetArg(z3context, entry, 0);
      Native.incRef(z3context, arrayIndex);
      long select = Native.mkSelect(z3context, arrayFormula, arrayIndex);
      Native.incRef(z3context, select);

      List<Object> innerIndices = Lists.newArrayList(upperIndices);
      innerIndices.add(evaluateImpl(arrayIndex));

      if (creator.isConstant(arrayValue)) {
        lst.add(
            new ValueAssignment(
                creator.encapsulateWithTypeOf(select),
                creator.symbolToString(arraySymbol),
                creator.convertValue(arrayValue),
                innerIndices));

      } else if (Native.isAsArray(z3context, arrayValue)) {
        lst.addAll(getArrayAssignments(arraySymbol, select, arrayValue, innerIndices));
      }

      Native.decRef(z3context, arrayIndex);
      Native.funcEntryDecRef(z3context, entry);
    }

    Native.funcInterpDecRef(z3context, interp);
    Native.decRef(z3context, evalDecl);
    return lst;
  }

  private void checkReturnValue(long value, long funcDecl) {
    if (value == 0) {
      throw new VerifyException(
          "Z3 unexpectedly claims that the value of "
              + Native.funcDeclToString(z3context, funcDecl)
              + " does not matter in model.");
    }
  }

  /**
   * get all ValueAssignments for a function declaration in the model
   *
   * @param evalDecl function declaration where the evaluation comes from
   * @param funcDecl function declaration where the function name comes from
   * @param functionName the name of the funcDecl
   */
  private Collection<ValueAssignment> getFunctionAssignments(
      long evalDecl, long funcDecl, String functionName) {
    long interp = Native.modelGetFuncInterp(z3context, model, evalDecl);
    checkReturnValue(interp, evalDecl);
    Native.funcInterpIncRef(z3context, interp);

    List<ValueAssignment> lst = new ArrayList<>();

    int numInterpretations = Native.funcInterpGetNumEntries(z3context, interp);

    if (numInterpretations == 0) {
      // we found an alias in the model, follow the alias
      long elseInterp = Native.funcInterpGetElse(z3context, interp);
      Native.incRef(z3context, elseInterp);
      long aliasDecl = Native.getAppDecl(z3context, elseInterp);
      Native.incRef(z3context, aliasDecl);
      if (isInternalSymbol(aliasDecl)) {
        lst.addAll(getFunctionAssignments(aliasDecl, funcDecl, functionName));
        // TODO Can we guarantee termination of this recursive call?
        //      A chain of aliases should end after several steps.
      } else {
        // ignore functionDeclarations like "ite", "and",...
      }
      Native.decRef(z3context, aliasDecl);
      Native.decRef(z3context, elseInterp);

    } else {
      for (int interpIdx = 0; interpIdx < numInterpretations; interpIdx++) {
        long entry = Native.funcInterpGetEntry(z3context, interp, interpIdx);
        Native.funcEntryIncRef(z3context, entry);
        long entryValue = Native.funcEntryGetValue(z3context, entry);
        if (creator.isConstant(entryValue)) {
          lst.add(getFunctionAssignment(functionName, funcDecl, entry, entryValue));
        } else {
          // ignore values of complex types, e.g. Arrays
        }
        Native.funcEntryDecRef(z3context, entry);
      }
    }

    Native.funcInterpDecRef(z3context, interp);
    return lst;
  }

  /**
   * @return ValueAssignment for an entry (one evaluation)
   * of an uninterpreted function in the model.
   **/
  private ValueAssignment getFunctionAssignment(
      String functionName, long funcDecl, long entry, long entryValue) {
    Object value = creator.convertValue(entryValue);
    int noArgs = Native.funcEntryGetNumArgs(z3context, entry);
    long[] args = new long[noArgs];
    List<Object> argumentInterpretation = new ArrayList<>();

    for (int k = 0; k < noArgs; k++) {
      long arg = Native.funcEntryGetArg(z3context, entry, k);
      Native.incRef(z3context, arg);
      argumentInterpretation.add(creator.convertValue(arg));
      args[k] = arg;
    }
    Formula formula =
        creator.encapsulateWithTypeOf(Native.mkApp(z3context, funcDecl, args.length, args));

    // Clean up memory.
    for (long arg : args) {
      Native.decRef(z3context, arg);
    }

    return new ValueAssignment(formula, functionName, value, argumentInterpretation);
  }

  @Override
  public String toString() {
    return Native.modelToString(z3context, model);
  }

  @Override
  public void close() {
    Native.modelDecRef(z3context, model);
  }
}
