/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.bre.bvm;

import com.fasterxml.jackson.databind.JsonNode;
import org.ballerinalang.bre.Context;
import org.ballerinalang.model.types.BType;
import org.ballerinalang.model.types.BTypes;
import org.ballerinalang.model.types.TypeTags;
import org.ballerinalang.model.util.JSONUtils;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BBooleanArray;
import org.ballerinalang.model.values.BConnector;
import org.ballerinalang.model.values.BFloat;
import org.ballerinalang.model.values.BFloatArray;
import org.ballerinalang.model.values.BIntArray;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BJSON;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BRefType;
import org.ballerinalang.model.values.BRefValueArray;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BStringArray;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.model.values.StructureType;
import org.ballerinalang.natives.AbstractNativeFunction;
import org.ballerinalang.util.codegen.ActionInfo;
import org.ballerinalang.util.codegen.CallableUnitInfo;
import org.ballerinalang.util.codegen.FunctionInfo;
import org.ballerinalang.util.codegen.Instruction;
import org.ballerinalang.util.codegen.InstructionCodes;
import org.ballerinalang.util.codegen.Mnemonics;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.ProgramFile;
import org.ballerinalang.util.codegen.cpentries.ActionRefCPEntry;
import org.ballerinalang.util.codegen.cpentries.ConstantPoolEntry;
import org.ballerinalang.util.codegen.cpentries.FloatCPEntry;
import org.ballerinalang.util.codegen.cpentries.FunctionCallCPEntry;
import org.ballerinalang.util.codegen.cpentries.FunctionRefCPEntry;
import org.ballerinalang.util.codegen.cpentries.FunctionReturnCPEntry;
import org.ballerinalang.util.codegen.cpentries.IntegerCPEntry;
import org.ballerinalang.util.codegen.cpentries.StringCPEntry;
import org.ballerinalang.util.codegen.cpentries.StructureRefCPEntry;
import org.ballerinalang.util.exceptions.BLangExceptionHelper;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.ballerinalang.util.exceptions.RuntimeErrors;

import java.io.PrintStream;
import java.util.StringJoiner;

/**
 * @since 0.87
 */
public class BLangVM {
    private Context context;
    private ControlStackNew controlStack;
    private ProgramFile programFile;
    private ConstantPoolEntry[] constPool;

    // Instruction pointer;
    private int ip = 0;
    private Instruction[] code;

    public BLangVM(ProgramFile programFile) {
        this.programFile = programFile;
    }

    // TODO Remove
    private void traceCode() {
        PrintStream printStream = System.out;
        for (int i = 0; i < code.length; i++) {
            printStream.println(i + ": " + Mnemonics.getMnem(code[i].getOpcode()) + " " +
                    getOperandsLine(code[i].getOperands()));
        }
    }

    public void execFunction(PackageInfo packageInfo, Context context, int ip) {
        this.constPool = packageInfo.getConstPool().toArray(new ConstantPoolEntry[0]);
        this.code = packageInfo.getInstructionList().toArray(new Instruction[0]);

        this.context = context;
        this.controlStack = context.getControlStackNew();
        this.context.setVMBasedExecutor(true);
        this.ip = ip;

//        traceCode();
        exec();
    }

    /**
     * Act as a virtual CPU
     */
    private void exec() {
        int i;
        int j;
        int k;
        int lvIndex;
        int cpIndex;
        int fieldIndex;

        int[] fieldCount;

        BIntArray bIntArray;
        BFloatArray bFloatArray;
        BStringArray bStringArray;
        BBooleanArray bBooleanArray;
        BRefValueArray bArray;
        StructureType structureType;
        BMap<String, BRefType> bMap;
        BRefType bRefType;

        StructureRefCPEntry structureRefCPEntry;
        FunctionCallCPEntry funcCallCPEntry;
        FunctionRefCPEntry funcRefCPEntry;
        FunctionInfo functionInfo;
        StringCPEntry stringCPEntry;

        // TODO use HALT Instruction in the while condition
        while (ip >= 0 && ip < code.length && controlStack.fp >= 0) {

            Instruction instruction = code[ip];
            int opcode = instruction.getOpcode();
            int[] operands = instruction.getOperands();
            ip++;
            StackFrame sf = controlStack.getCurrentFrame();

            switch (opcode) {
                case InstructionCodes.ICONST:
                    cpIndex = operands[0];
                    i = operands[1];
                    sf.longRegs[i] = ((IntegerCPEntry) constPool[cpIndex]).getValue();
                    break;
                case InstructionCodes.FCONST:
                    cpIndex = operands[0];
                    i = operands[1];
                    sf.doubleRegs[i] = ((FloatCPEntry) constPool[cpIndex]).getValue();
                    break;
                case InstructionCodes.SCONST:
                    cpIndex = operands[0];
                    i = operands[1];
                    sf.stringRegs[i] = ((StringCPEntry) constPool[cpIndex]).getValue();
                    break;
                case InstructionCodes.ICONST_0:
                    i = operands[0];
                    sf.longRegs[i] = 0;
                    break;
                case InstructionCodes.ICONST_1:
                    i = operands[0];
                    sf.longRegs[i] = 1;
                    break;
                case InstructionCodes.ICONST_2:
                    i = operands[0];
                    sf.longRegs[i] = 2;
                    break;
                case InstructionCodes.ICONST_3:
                    i = operands[0];
                    sf.longRegs[i] = 3;
                    break;
                case InstructionCodes.ICONST_4:
                    i = operands[0];
                    sf.longRegs[i] = 4;
                    break;
                case InstructionCodes.ICONST_5:
                    i = operands[0];
                    sf.longRegs[i] = 5;
                    break;
                case InstructionCodes.FCONST_0:
                    i = operands[0];
                    sf.doubleRegs[i] = 0;
                    break;
                case InstructionCodes.FCONST_1:
                    i = operands[0];
                    sf.doubleRegs[i] = 1;
                    break;
                case InstructionCodes.FCONST_2:
                    i = operands[0];
                    sf.doubleRegs[i] = 2;
                    break;
                case InstructionCodes.FCONST_3:
                    i = operands[0];
                    sf.doubleRegs[i] = 3;
                    break;
                case InstructionCodes.FCONST_4:
                    i = operands[0];
                    sf.doubleRegs[i] = 4;
                    break;
                case InstructionCodes.FCONST_5:
                    i = operands[0];
                    sf.doubleRegs[i] = 5;
                    break;
                case InstructionCodes.BCONST_0:
                    i = operands[0];
                    sf.intRegs[i] = 0;
                    break;
                case InstructionCodes.BCONST_1:
                    i = operands[0];
                    sf.intRegs[i] = 1;
                    break;
                case InstructionCodes.RCONST_NULL:
                    i = operands[0];
                    sf.refRegs[i] = null;
                    break;

                case InstructionCodes.ILOAD:
                    lvIndex = operands[0];
                    i = operands[1];
                    sf.longRegs[i] = sf.longLocalVars[lvIndex];
                    break;
                case InstructionCodes.FLOAD:
                    lvIndex = operands[0];
                    i = operands[1];
                    sf.doubleRegs[i] = sf.doubleLocalVars[lvIndex];
                    break;
                case InstructionCodes.SLOAD:
                    lvIndex = operands[0];
                    i = operands[1];
                    sf.stringRegs[i] = sf.stringLocalVars[lvIndex];
                    break;
                case InstructionCodes.BLOAD:
                    lvIndex = operands[0];
                    i = operands[1];
                    sf.intRegs[i] = sf.intLocalVars[lvIndex];
                    break;
                case InstructionCodes.RLOAD:
                    lvIndex = operands[0];
                    i = operands[1];
                    sf.refRegs[i] = sf.refLocalVars[lvIndex];
                    break;
                case InstructionCodes.IALOAD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bIntArray = (BIntArray) sf.refRegs[i];
                    sf.longRegs[k] = bIntArray.get(sf.longRegs[j]);
                    break;
                case InstructionCodes.FALOAD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bFloatArray = (BFloatArray) sf.refRegs[i];
                    sf.doubleRegs[k] = bFloatArray.get(sf.longRegs[j]);
                    break;
                case InstructionCodes.SALOAD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bStringArray = (BStringArray) sf.refRegs[i];
                    sf.stringRegs[k] = bStringArray.get(sf.longRegs[j]);
                    break;
                case InstructionCodes.BALOAD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bBooleanArray = (BBooleanArray) sf.refRegs[i];
                    sf.intRegs[k] = bBooleanArray.get(sf.longRegs[j]);
                    break;
                case InstructionCodes.RALOAD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bArray = (BRefValueArray) sf.refRegs[i];
                    sf.refRegs[k] = bArray.get(sf.longRegs[j]);
                    break;
                case InstructionCodes.JSONALOAD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    // TODO Proper error handling
                    sf.refRegs[k] = JSONUtils.getArrayElement((BJSON) sf.refRegs[i], sf.longRegs[j]);
                    break;
                case InstructionCodes.ISTORE:
                    i = operands[0];
                    lvIndex = operands[1];
                    sf.longLocalVars[lvIndex] = sf.longRegs[i];
                    break;
                case InstructionCodes.FSTORE:
                    i = operands[0];
                    lvIndex = operands[1];
                    sf.doubleLocalVars[lvIndex] = sf.doubleRegs[i];
                    break;
                case InstructionCodes.SSTORE:
                    i = operands[0];
                    lvIndex = operands[1];
                    sf.stringLocalVars[lvIndex] = sf.stringRegs[i];
                    break;
                case InstructionCodes.BSTORE:
                    i = operands[0];
                    lvIndex = operands[1];
                    sf.intLocalVars[lvIndex] = sf.intRegs[i];
                    break;
                case InstructionCodes.RSTORE:
                    i = operands[0];
                    lvIndex = operands[1];
                    sf.refLocalVars[lvIndex] = sf.refRegs[i];
                    break;
                case InstructionCodes.IASTORE:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bIntArray = (BIntArray) sf.refRegs[i];
                    bIntArray.add(sf.longRegs[j], sf.longRegs[k]);
                    break;
                case InstructionCodes.FASTORE:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bFloatArray = (BFloatArray) sf.refRegs[i];
                    bFloatArray.add(sf.longRegs[j], sf.doubleRegs[k]);
                    break;
                case InstructionCodes.SASTORE:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bStringArray = (BStringArray) sf.refRegs[i];
                    bStringArray.add(sf.longRegs[j], sf.stringRegs[k]);
                    break;
                case InstructionCodes.BASTORE:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bBooleanArray = (BBooleanArray) sf.refRegs[i];
                    bBooleanArray.add(sf.longRegs[j], sf.intRegs[k]);
                    break;
                case InstructionCodes.RASTORE:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bArray = (BRefValueArray) sf.refRegs[i];
                    bArray.add(sf.longRegs[j], sf.refRegs[k]);
                    break;
                case InstructionCodes.JSONASTORE:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    // TODO Proper error handling
                    JSONUtils.setArrayElement((BJSON) sf.refRegs[i], sf.longRegs[j], (BJSON) sf.refRegs[k]);
                    break;
                case InstructionCodes.IFIELDLOAD:
                    i = operands[0];
                    fieldIndex = operands[1];
                    j = operands[2];
                    structureType = (StructureType) sf.refRegs[i];
                    sf.longRegs[j] = structureType.getIntField(fieldIndex);
                    break;
                case InstructionCodes.FFIELDLOAD:
                    i = operands[0];
                    fieldIndex = operands[1];
                    j = operands[2];
                    structureType = (StructureType) sf.refRegs[i];
                    sf.doubleRegs[j] = structureType.getFloatField(fieldIndex);
                    break;
                case InstructionCodes.SFIELDLOAD:
                    i = operands[0];
                    fieldIndex = operands[1];
                    j = operands[2];
                    structureType = (StructureType) sf.refRegs[i];
                    sf.stringRegs[j] = structureType.getStringField(fieldIndex);
                    break;
                case InstructionCodes.BFIELDLOAD:
                    i = operands[0];
                    fieldIndex = operands[1];
                    j = operands[2];
                    structureType = (StructureType) sf.refRegs[i];
                    sf.intRegs[j] = structureType.getBooleanField(fieldIndex);
                    break;
                case InstructionCodes.RFIELDLOAD:
                    i = operands[0];
                    fieldIndex = operands[1];
                    j = operands[2];
                    structureType = (StructureType) sf.refRegs[i];
                    sf.refRegs[j] = structureType.getRefField(fieldIndex);
                    break;
                case InstructionCodes.IFIELDSTORE:
                    i = operands[0];
                    fieldIndex = operands[1];
                    j = operands[2];
                    structureType = (StructureType) sf.refRegs[i];
                    structureType.setIntField(fieldIndex, sf.longRegs[j]);
                    break;
                case InstructionCodes.FFIELDSTORE:
                    i = operands[0];
                    fieldIndex = operands[1];
                    j = operands[2];
                    structureType = (StructureType) sf.refRegs[i];
                    structureType.setFloatField(fieldIndex, sf.doubleRegs[j]);
                    break;
                case InstructionCodes.SFIELDSTORE:
                    i = operands[0];
                    fieldIndex = operands[1];
                    j = operands[2];
                    structureType = (StructureType) sf.refRegs[i];
                    structureType.setStringField(fieldIndex, sf.stringRegs[j]);
                    break;
                case InstructionCodes.BFIELDSTORE:
                    i = operands[0];
                    fieldIndex = operands[1];
                    j = operands[2];
                    structureType = (StructureType) sf.refRegs[i];
                    structureType.setBooleanField(fieldIndex, sf.intRegs[j]);
                    break;
                case InstructionCodes.RFIELDSTORE:
                    i = operands[0];
                    fieldIndex = operands[1];
                    j = operands[2];
                    structureType = (StructureType) sf.refRegs[i];
                    structureType.setRefField(fieldIndex, sf.refRegs[j]);
                    break;

                case InstructionCodes.MAPLOAD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bMap = (BMap<String, BRefType>) sf.refRegs[i];
                    sf.refRegs[k] = bMap.get(sf.stringRegs[j]);
                    break;
                case InstructionCodes.MAPSTORE:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bMap = (BMap<String, BRefType>) sf.refRegs[i];
                    bMap.put(sf.stringRegs[j], sf.refRegs[k]);
                    break;

                case InstructionCodes.JSONLOAD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    // TODO Proper error handling
                    sf.refRegs[k] = JSONUtils.getElement((BJSON) sf.refRegs[i], sf.stringRegs[j]);
                    break;
                case InstructionCodes.JSONSTORE:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    // TODO Proper error handling
                    JSONUtils.setElement((BJSON) sf.refRegs[i], sf.stringRegs[j], (BJSON) sf.refRegs[k]);
                    break;

                case InstructionCodes.IADD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    sf.longRegs[k] = sf.longRegs[i] + sf.longRegs[j];
                    break;
                case InstructionCodes.FADD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    sf.doubleRegs[k] = sf.doubleRegs[i] + sf.doubleRegs[j];
                    break;
                case InstructionCodes.SADD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    sf.stringRegs[k] = sf.stringRegs[i] + sf.stringRegs[j];
                    break;
                case InstructionCodes.ISUB:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    sf.longRegs[k] = sf.longRegs[i] - sf.longRegs[j];
                    break;
                case InstructionCodes.FSUB:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    sf.doubleRegs[k] = sf.doubleRegs[i] - sf.doubleRegs[j];
                    break;
                case InstructionCodes.IMUL:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    sf.longRegs[k] = sf.longRegs[i] * sf.longRegs[j];
                    break;
                case InstructionCodes.FMUL:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    sf.doubleRegs[k] = sf.doubleRegs[i] * sf.doubleRegs[j];
                    break;
                case InstructionCodes.IDIV:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];

                    // TODO improve error handling in VM
                    if (sf.longRegs[j] == 0) {
                        throw new BallerinaException(" / by zero");
                    }

                    sf.longRegs[k] = sf.longRegs[i] / sf.longRegs[j];
                    break;
                case InstructionCodes.FDIV:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];

                    // TODO improve error handling in VM
                    if (sf.doubleRegs[j] == 0) {
                        throw new BallerinaException(" / by zero");
                    }

                    sf.doubleRegs[k] = sf.doubleRegs[i] / sf.doubleRegs[j];
                    break;
                case InstructionCodes.IMOD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];

                    // TODO improve error handling in VM
                    if (sf.longRegs[j] == 0) {
                        throw new BallerinaException(" / by zero");
                    }

                    sf.longRegs[k] = sf.longRegs[i] % sf.longRegs[j];
                    break;
                case InstructionCodes.FMOD:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];

                    // TODO improve error handling in VM
                    if (sf.doubleRegs[j] == 0) {
                        throw new BallerinaException(" / by zero");
                    }

                    sf.doubleRegs[k] = sf.doubleRegs[i] % sf.doubleRegs[j];
                    break;
                case InstructionCodes.INEG:
                    i = operands[0];
                    j = operands[1];
                    sf.longRegs[j] = -sf.longRegs[i];
                    break;
                case InstructionCodes.FNEG:
                    i = operands[0];
                    j = operands[1];
                    sf.doubleRegs[j] = -sf.doubleRegs[i];
                    break;

                case InstructionCodes.ICMP:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    if (sf.longRegs[i] == sf.longRegs[j]) {
                        sf.intRegs[k] = 0;
                    } else if (sf.longRegs[i] > sf.longRegs[j]) {
                        sf.intRegs[k] = 1;
                    } else {
                        sf.intRegs[k] = -1;
                    }
                    break;
                case InstructionCodes.IFEQ:
                    i = operands[0];
                    j = operands[1];
                    if (sf.intRegs[i] == 0) {
                        ip = j;
                    }
                    break;
                case InstructionCodes.IFNE:
                    i = operands[0];
                    j = operands[1];
                    if (sf.intRegs[i] != 0) {
                        ip = j;
                    }
                    break;
                case InstructionCodes.IFLT:
                    i = operands[0];
                    j = operands[1];
                    if (sf.intRegs[i] < 0) {
                        ip = j;
                    }
                    break;
                case InstructionCodes.IFGE:
                    i = operands[0];
                    j = operands[1];
                    if (sf.intRegs[i] >= 0) {
                        ip = j;
                    }
                    break;
                case InstructionCodes.IFGT:
                    i = operands[0];
                    j = operands[1];
                    if (sf.intRegs[i] > 0) {
                        ip = j;
                    }
                    break;
                case InstructionCodes.IFLE:
                    i = operands[0];
                    j = operands[1];
                    if (sf.intRegs[i] <= 0) {
                        ip = j;
                    }
                    break;
                case InstructionCodes.GOTO:
                    i = operands[0];
                    ip = i;
                    break;
                case InstructionCodes.CALL:
                    cpIndex = operands[0];
                    funcRefCPEntry = (FunctionRefCPEntry) constPool[cpIndex];
                    functionInfo = funcRefCPEntry.getFunctionInfo();

                    cpIndex = operands[1];
                    funcCallCPEntry = (FunctionCallCPEntry) constPool[cpIndex];
                    invokeCallableUnit(functionInfo, funcCallCPEntry);
                    break;
                case InstructionCodes.NCALL:
                    cpIndex = operands[0];
                    funcRefCPEntry = (FunctionRefCPEntry) constPool[cpIndex];
                    functionInfo = funcRefCPEntry.getFunctionInfo();

                    cpIndex = operands[1];
                    funcCallCPEntry = (FunctionCallCPEntry) constPool[cpIndex];
                    invokeNativeFunction(functionInfo, funcCallCPEntry);
                    break;
                case InstructionCodes.ACALL:
                    cpIndex = operands[0];
                    ActionRefCPEntry actionRefCPEntry = (ActionRefCPEntry) constPool[cpIndex];
                    ActionInfo actionInfo = actionRefCPEntry.getActionInfo();

                    cpIndex = operands[1];
                    funcCallCPEntry = (FunctionCallCPEntry) constPool[cpIndex];
                    invokeCallableUnit(actionInfo, funcCallCPEntry);
                    break;
                case InstructionCodes.NACALL:
                    break;
                case InstructionCodes.RET:
                    cpIndex = operands[0];
                    FunctionReturnCPEntry funcRetCPEntry = (FunctionReturnCPEntry) constPool[cpIndex];
                    handleReturn(funcRetCPEntry.getRegIndexes());
                    break;

                case InstructionCodes.I2F:
                    i = operands[0];
                    j = operands[1];
                    sf.doubleRegs[j] = (double) sf.longRegs[i];
                    break;
                case InstructionCodes.I2S:
                    i = operands[0];
                    j = operands[1];
                    sf.stringRegs[j] = Long.toString(sf.longRegs[i]);
                    break;
                case InstructionCodes.I2B:
                    i = operands[0];
                    j = operands[1];
                    sf.intRegs[j] = sf.longRegs[i] != 0 ? 1 : 0;
                    break;
                case InstructionCodes.I2ANY:
                    i = operands[0];
                    j = operands[1];
                    sf.refRegs[j] = new BInteger(sf.longRegs[i]);
                    break;
                case InstructionCodes.I2JSON:
                    i = operands[0];
                    j = operands[1];
                    sf.refRegs[j] = new BJSON(Long.toString(sf.longRegs[i]));
                    break;
                case InstructionCodes.F2I:
                    i = operands[0];
                    j = operands[1];
                    sf.longRegs[j] = (long) sf.doubleRegs[i];
                    break;
                case InstructionCodes.F2S:
                    i = operands[0];
                    j = operands[1];
                    sf.stringRegs[j] = Double.toString(sf.doubleRegs[i]);
                    break;
                case InstructionCodes.F2B:
                    i = operands[0];
                    j = operands[1];
                    sf.intRegs[j] = sf.doubleRegs[i] != 0.0 ? 1 : 0;
                    break;
                case InstructionCodes.F2ANY:
                    i = operands[0];
                    j = operands[1];
                    sf.refRegs[j] = new BFloat(sf.doubleRegs[i]);
                    break;
                case InstructionCodes.F2JSON:
                    i = operands[0];
                    j = operands[1];
                    sf.refRegs[j] = new BJSON(Double.toString(sf.doubleRegs[i]));
                    break;
                case InstructionCodes.S2I:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];

                    try {
                        sf.longRegs[j] = Long.parseLong(sf.stringRegs[i]);
                    } catch (NumberFormatException e) {
                        // TODO
                        throw new BallerinaException("incompatible types");
                    }
                    break;
                case InstructionCodes.S2F:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];

                    try {
                        sf.doubleRegs[j] = Double.parseDouble(sf.stringRegs[i]);
                    } catch (NumberFormatException e) {
                        // TODO
                        throw new BallerinaException("incompatible types");
                    }
                    break;
                case InstructionCodes.S2B:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];

                    try {
                        sf.intRegs[j] = Boolean.parseBoolean(sf.stringRegs[i]) ? 1 : 0;
                    } catch (NumberFormatException e) {
                        // TODO
                        throw new BallerinaException("incompatible types");
                    }
                    break;
                case InstructionCodes.S2ANY:
                    i = operands[0];
                    j = operands[1];
                    sf.refRegs[j] = new BString(sf.stringRegs[i]);
                    break;
                case InstructionCodes.S2JSON:
                    i = operands[0];
                    j = operands[1];
                    String jsonStr = sf.stringRegs[i];

                    // If this is a string-representation of complex JSON object, generate a BJSON out of it.
                    if (jsonStr.matches("\\{.*\\}|\\[.*\\]")) {
                        sf.refRegs[j] = new BJSON(jsonStr);
                    }

                    // Else, generate a BJSON with a quoted string.
                    sf.refRegs[j] = new BJSON("\"" + jsonStr + "\"");
                    break;
                case InstructionCodes.B2I:
                    i = operands[0];
                    j = operands[1];
                    sf.longRegs[j] = sf.intRegs[i];
                    break;
                case InstructionCodes.B2F:
                    i = operands[0];
                    j = operands[1];
                    sf.doubleRegs[j] = sf.intRegs[i];
                    break;
                case InstructionCodes.B2S:
                    i = operands[0];
                    j = operands[1];
                    sf.stringRegs[j] = sf.intRegs[i] == 1 ? "true" : "false";
                    break;
                case InstructionCodes.B2ANY:
                    i = operands[0];
                    j = operands[1];
                    sf.refRegs[j] = new BBoolean(sf.intRegs[i] == 1);
                    break;
                case InstructionCodes.B2JSON:
                    i = operands[0];
                    j = operands[1];
                    sf.refRegs[j] = new BJSON(sf.intRegs[i] == 1 ? "true" : "false");
                    break;
                case InstructionCodes.JSON2I:
                    convertJSONToInt(operands, sf);
                    break;
                case InstructionCodes.JSON2F:
                    convertJSONToFloat(operands, sf);
                    break;
                case InstructionCodes.JSON2S:
                    convertJSONToString(operands, sf);
                    break;
                case InstructionCodes.JSON2B:
                    convertJSONToBoolean(operands, sf);
                    break;

                case InstructionCodes.ANY2I:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bRefType = sf.refRegs[i];

                    if (bRefType.getType() == BTypes.typeInt) {
                        sf.longRegs[j] = ((BInteger) bRefType).intValue();
                    } else {
                        // TODO
                        throw new BallerinaException("incompatible types");
                    }
                    break;
                case InstructionCodes.ANY2F:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bRefType = sf.refRegs[i];

                    if (bRefType.getType() == BTypes.typeFloat) {
                        sf.doubleRegs[j] = ((BFloat) bRefType).floatValue();
                    } else {
                        // TODO
                        throw new BallerinaException("incompatible types");
                    }
                    break;
                case InstructionCodes.ANY2S:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bRefType = sf.refRegs[i];

                    if (bRefType.getType() == BTypes.typeString) {
                        sf.stringRegs[j] = bRefType.stringValue();
                    } else {
                        // TODO
                        throw new BallerinaException("incompatible types");
                    }
                    break;
                case InstructionCodes.ANY2B:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bRefType = sf.refRegs[i];

                    if (bRefType.getType() == BTypes.typeBoolean) {
                        sf.intRegs[j] = ((BBoolean) bRefType).booleanValue() ? 1 : 0;
                    } else {
                        // TODO
                        throw new BallerinaException("incompatible types");
                    }
                    break;
                case InstructionCodes.ANY2JSON:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bRefType = sf.refRegs[i];

                    if (bRefType.getType() == BTypes.typeJSON) {
                        sf.refRegs[j] = bRefType;
                    } else {
                        // TODO
                        throw new BallerinaException("incompatible types");
                    }
                    break;
                case InstructionCodes.ANY2MAP:
                    i = operands[0];
                    j = operands[1];
                    k = operands[2];
                    bRefType = sf.refRegs[i];

                    if (bRefType.getType() == BTypes.typeMap) {
                        sf.refRegs[j] = bRefType;
                    } else {
                        // TODO
                        throw new BallerinaException("incompatible types");
                    }
                    break;
                case InstructionCodes.NULL2JSON:
                    i = operands[0];
                    j = operands[1];
                    sf.refRegs[j] = new BJSON("null");
                    break;

                case InstructionCodes.INEWARRAY:
                    i = operands[0];
                    sf.refRegs[i] = new BIntArray();
                    break;
                case InstructionCodes.FNEWARRAY:
                    i = operands[0];
                    sf.refRegs[i] = new BFloatArray();
                    break;
                case InstructionCodes.SNEWARRAY:
                    i = operands[0];
                    sf.refRegs[i] = new BStringArray();
                    break;
                case InstructionCodes.BNEWARRAY:
                    i = operands[0];
                    sf.refRegs[i] = new BBooleanArray();
                    break;
                case InstructionCodes.RNEWARRAY:
                    i = operands[0];
                    sf.refRegs[i] = new BRefValueArray();
                    break;
                case InstructionCodes.JSONNEWARRAY:
                    i = operands[0];
                    j = operands[1];
                    // This is a temporary solution to create n-valued JSON array
                    StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
                    for (int index = 0; index < sf.longRegs[j]; index++) {
                        stringJoiner.add("0");
                    }
                    sf.refRegs[i] = new BJSON(stringJoiner.toString());
                    break;
                case InstructionCodes.NEWSTRUCT:
                    cpIndex = operands[0];
                    i = operands[1];
                    structureRefCPEntry = (StructureRefCPEntry) constPool[cpIndex];
                    fieldCount = structureRefCPEntry.getStructureTypeInfo().getFieldCount();
                    BStruct bStruct = new BStruct();
                    bStruct.init(fieldCount);
                    sf.refRegs[i] = bStruct;
                    break;
                case InstructionCodes.NEWCONNECTOR:
                    cpIndex = operands[0];
                    i = operands[1];
                    structureRefCPEntry = (StructureRefCPEntry) constPool[cpIndex];
                    fieldCount = structureRefCPEntry.getStructureTypeInfo().getFieldCount();
                    BConnector bConnector = new BConnector();
                    bConnector.init(fieldCount);
                    sf.refRegs[i] = bConnector;
                    break;
                case InstructionCodes.NEWMAP:
                    i = operands[0];
                    sf.refRegs[i] = new BMap<String, BRefType>();
                    break;
                case InstructionCodes.NEWJSON:
                    i = operands[0];
                    sf.refRegs[i] = new BJSON("{}");
                    break;

                default:
                    throw new UnsupportedOperationException("Opcode " + opcode + " is not supported yet");
            }
        }
    }

    public void invokeCallableUnit(CallableUnitInfo callableUnitInfo, FunctionCallCPEntry funcCallCPEntry) {
        int[] argRegs = funcCallCPEntry.getArgRegs();
        BType[] paramTypes = callableUnitInfo.getParamTypes();
        StackFrame callerSF = controlStack.getCurrentFrame();

        StackFrame calleeSF = new StackFrame(callableUnitInfo, ip, funcCallCPEntry.getRetRegs());
        controlStack.pushFrame(calleeSF);

        // Copy arg values from the current StackFrame to the new StackFrame
        copyArgValues(callerSF, calleeSF, argRegs, paramTypes);

        // TODO Improve following two lines
        this.constPool = calleeSF.packageInfo.getConstPool().toArray(new ConstantPoolEntry[0]);
        this.code = calleeSF.packageInfo.getInstructionList().toArray(new Instruction[0]);
        ip = callableUnitInfo.getCodeAttributeInfo().getCodeAddrs();
    }

    private void copyArgValues(StackFrame callerSF, StackFrame calleeSF, int[] argRegs, BType[] paramTypes) {
        int longRegIndex = -1;
        int doubleRegIndex = -1;
        int stringRegIndex = -1;
        int booleanRegIndex = -1;
        int refRegIndex = -1;

        for (int i = 0; i < argRegs.length; i++) {
            BType paramType = paramTypes[i];
            int argReg = argRegs[i];
            switch (paramType.getTag()) {
                case TypeTags.INT_TAG:
                    calleeSF.longLocalVars[++longRegIndex] = callerSF.longRegs[argReg];
                    break;
                case TypeTags.FLOAT_TAG:
                    calleeSF.doubleLocalVars[++doubleRegIndex] = callerSF.doubleRegs[argReg];
                    break;
                case TypeTags.STRING_TAG:
                    calleeSF.stringLocalVars[++stringRegIndex] = callerSF.stringRegs[argReg];
                    break;
                case TypeTags.BOOLEAN_TAG:
                    calleeSF.intLocalVars[++booleanRegIndex] = callerSF.intRegs[argReg];
                    break;
                default:
                    calleeSF.refLocalVars[++refRegIndex] = callerSF.refRegs[argReg];
            }
        }
    }

    private void handleReturn(int[] regIndexes) {
        StackFrame currentSF = controlStack.popFrame();
        if (controlStack.fp >= 0) {

            StackFrame callersSF = controlStack.currentFrame;
            BType[] retTypes = currentSF.callableUnitInfo.getRetParamTypes();

            for (int i = 0; i < regIndexes.length; i++) {
                int regIndex = regIndexes[i];
                int callersRetRegIndex = currentSF.retRegIndexes[i];
                BType retType = retTypes[i];
                switch (retType.getTag()) {
                    case TypeTags.INT_TAG:
                        callersSF.longRegs[callersRetRegIndex] = currentSF.longRegs[regIndex];
                        break;
                    case TypeTags.FLOAT_TAG:
                        callersSF.doubleRegs[callersRetRegIndex] = currentSF.doubleRegs[regIndex];
                        break;
                    case TypeTags.STRING_TAG:
                        callersSF.stringRegs[callersRetRegIndex] = currentSF.stringRegs[regIndex];
                        break;
                    case TypeTags.BOOLEAN_TAG:
                        callersSF.intRegs[callersRetRegIndex] = currentSF.intRegs[regIndex];
                        break;
                    default:
                        callersSF.refRegs[callersRetRegIndex] = currentSF.refRegs[regIndex];
                }
            }

            // TODO Improve
            this.constPool = callersSF.packageInfo.getConstPool().toArray(new ConstantPoolEntry[0]);
            this.code = callersSF.packageInfo.getInstructionList().toArray(new Instruction[0]);
        }

        ip = currentSF.retAddrs;
    }

    private String getOperandsLine(int[] operands) {
        if (operands.length == 0) {
            return "";
        }

        if (operands.length == 1) {
            return "" + operands[0];
        }

        StringBuilder sb = new StringBuilder();
        sb.append(operands[0]);
        for (int i = 1; i < operands.length; i++) {
            sb.append(" ");
            sb.append(operands[i]);
        }
        return sb.toString();
    }

    private void invokeNativeFunction(FunctionInfo functionInfo, FunctionCallCPEntry funcCallCPEntry) {
        StackFrame callerSF = controlStack.currentFrame;
        BValue[] nativeArgValues = populateNativeArgs(callerSF, funcCallCPEntry.getArgRegs(),
                functionInfo.getParamTypes());

        BType[] retTypes = functionInfo.getRetParamTypes();
        BValue[] returnValues = new BValue[retTypes.length];
        StackFrame caleeSF = new StackFrame(nativeArgValues, returnValues);
        controlStack.pushFrame(caleeSF);

        // Invoke Native function;
        AbstractNativeFunction nativeFunction = functionInfo.getNativeFunction();
        nativeFunction.executeNative(context);

        // Copy return values to the callers stack
        controlStack.popFrame();
        handleReturnFromNativeCallableUnit(callerSF, funcCallCPEntry.getRetRegs(), returnValues, retTypes);
    }

    private void invokeNativeAction(ActionInfo actionInfo, FunctionCallCPEntry funcCallCPEntry) {

    }

    private BValue[] populateNativeArgs(StackFrame callerSF, int[] argRegs, BType[] paramTypes) {
        BValue[] nativeArgValues = new BValue[paramTypes.length];
        for (int i = 0; i < argRegs.length; i++) {
            BType paramType = paramTypes[i];
            int argReg = argRegs[i];
            switch (paramType.getTag()) {
                case TypeTags.INT_TAG:
                    nativeArgValues[i] = new BInteger(callerSF.longRegs[argReg]);
                    break;
                case TypeTags.FLOAT_TAG:
                    nativeArgValues[i] = new BFloat(callerSF.doubleRegs[argReg]);
                    break;
                case TypeTags.STRING_TAG:
                    nativeArgValues[i] = new BString(callerSF.stringRegs[argReg]);
                    break;
                case TypeTags.BOOLEAN_TAG:
                    nativeArgValues[i] = new BBoolean(callerSF.intRegs[argReg] == 1);
                    break;
                default:
                    nativeArgValues[i] = callerSF.refRegs[argReg];
            }
        }
        return nativeArgValues;
    }

    private void handleReturnFromNativeCallableUnit(StackFrame callerSF, int[] returnRegIndexes,
                                                    BValue[] returnValues, BType[] retTypes) {
        for (int i = 0; i < returnValues.length; i++) {
            int callersRetRegIndex = returnRegIndexes[i];
            BType retType = retTypes[i];
            switch (retType.getTag()) {
                case TypeTags.INT_TAG:
                    callerSF.longRegs[callersRetRegIndex] = ((BInteger) returnValues[i]).intValue();
                    break;
                case TypeTags.FLOAT_TAG:
                    callerSF.doubleRegs[callersRetRegIndex] = ((BFloat) returnValues[i]).floatValue();
                    break;
                case TypeTags.STRING_TAG:
                    callerSF.stringRegs[callersRetRegIndex] = returnValues[i].stringValue();
                    break;
                case TypeTags.BOOLEAN_TAG:
                    callerSF.intRegs[callersRetRegIndex] = ((BBoolean) returnValues[i]).booleanValue() ? 1 : 0;
                    break;
                default:
                    callerSF.refRegs[callersRetRegIndex] = (BRefType) returnValues[i];
            }
        }
    }

    // TODO Refactor these methods and move them to a proper util class
    private static void convertJSONToInt(int[] operands, StackFrame sf) {
        int i = operands[0];
        int j = operands[1];
        int k = operands[2];

        BJSON jsonValue = (BJSON) sf.refRegs[i];
        // TODO  Check for NULL
//        if (bjson == null) {
//            String errorMsg =
//                    BLangExceptionHelper.getErrorMessage(RuntimeErrors.CASTING_ANY_TYPE_WITHOUT_INIT, BTypes.typeInt);
//            return TypeMappingUtils.getError(returnErrors, errorMsg, BTypes.typeJSON, targetType);
//        }

        JsonNode jsonNode;
        try {
            jsonNode = jsonValue.value();
        } catch (BallerinaException e) {
            String errorMsg = BLangExceptionHelper.getErrorMessage(RuntimeErrors.CASTING_FAILED_WITH_CAUSE,
                    BTypes.typeJSON, BTypes.typeInt, e.getMessage());
            throw new BallerinaException(errorMsg);
            // TODO
//            return TypeMappingUtils.getError(returnErrors, errorMsg, BTypes.typeJSON, targetType);
        }

        if (jsonNode.isInt() || jsonNode.isLong()) {
            sf.longRegs[j] = jsonNode.longValue();
            return;
        }

        throw BLangExceptionHelper.getRuntimeException(RuntimeErrors.INCOMPATIBLE_TYPE_FOR_CASTING_JSON,
                BTypes.typeInt, JSONUtils.getTypeName(jsonNode));
    }

    private static void convertJSONToFloat(int[] operands, StackFrame sf) {
        int i = operands[0];
        int j = operands[1];
        int k = operands[2];

        BJSON jsonValue = (BJSON) sf.refRegs[i];
        // TODO  Check for NULL
//        if (bjson == null) {
//            String errorMsg =
//                    BLangExceptionHelper.getErrorMessage(RuntimeErrors.CASTING_ANY_TYPE_WITHOUT_INIT,
// BTypes.typeFloat);
//            return TypeMappingUtils.getError(returnErrors, errorMsg, BTypes.typeJSON, targetType);
//        }

        JsonNode jsonNode;
        try {
            jsonNode = jsonValue.value();
        } catch (BallerinaException e) {
            String errorMsg = BLangExceptionHelper.getErrorMessage(RuntimeErrors.CASTING_FAILED_WITH_CAUSE,
                    BTypes.typeJSON, BTypes.typeFloat, e.getMessage());
            throw new BallerinaException(errorMsg);
            // TODO
//            return TypeMappingUtils.getError(returnErrors, errorMsg, BTypes.typeJSON, targetType);
        }

        if (jsonNode.isFloat() || jsonNode.isDouble()) {
            sf.doubleRegs[j] = jsonNode.doubleValue();
            return;
        }

        throw BLangExceptionHelper.getRuntimeException(RuntimeErrors.INCOMPATIBLE_TYPE_FOR_CASTING_JSON,
                BTypes.typeFloat, JSONUtils.getTypeName(jsonNode));
    }

    private static void convertJSONToString(int[] operands, StackFrame sf) {
        int i = operands[0];
        int j = operands[1];
        int k = operands[2];

        BJSON jsonValue = (BJSON) sf.refRegs[i];
        // TODO  Check for NULL
//        if (bjson == null) {
//            String errorMsg =
//                    BLangExceptionHelper.getErrorMessage(RuntimeErrors.CASTING_ANY_TYPE_WITHOUT_INIT,
// BTypes.typeString);
//            return TypeMappingUtils.getError(returnErrors, errorMsg, BTypes.typeJSON, targetType);
//        }

        try {
            sf.stringRegs[j] = jsonValue.stringValue();
        } catch (BallerinaException e) {
            String errorMsg = BLangExceptionHelper.getErrorMessage(RuntimeErrors.CASTING_FAILED_WITH_CAUSE,
                    BTypes.typeJSON, BTypes.typeString, e.getMessage());
            throw new BallerinaException(errorMsg);
            // TODO
//            return TypeMappingUtils.getError(returnErrors, errorMsg, BTypes.typeJSON, targetType);
        }
    }

    private static void convertJSONToBoolean(int[] operands, StackFrame sf) {
        int i = operands[0];
        int j = operands[1];
        int k = operands[2];

        BJSON jsonValue = (BJSON) sf.refRegs[i];
        // TODO  Check for NULL
//        if (bjson == null) {
//            String errorMsg =
//                    BLangExceptionHelper.getErrorMessage(RuntimeErrors.CASTING_ANY_TYPE_WITHOUT_INIT,
// BTypes.typeBoolean);
//            return TypeMappingUtils.getError(returnErrors, errorMsg, BTypes.typeJSON, targetType);
//        }

        JsonNode jsonNode;
        try {
            jsonNode = jsonValue.value();
        } catch (BallerinaException e) {
            String errorMsg = BLangExceptionHelper.getErrorMessage(RuntimeErrors.CASTING_FAILED_WITH_CAUSE,
                    BTypes.typeJSON, BTypes.typeBoolean, e.getMessage());
            throw new BallerinaException(errorMsg);
            // TODO
//            return TypeMappingUtils.getError(returnErrors, errorMsg, BTypes.typeJSON, targetType);
        }

        if (jsonNode.isBoolean()) {
            sf.intRegs[j] = jsonNode.booleanValue() ? 1 : 0;
            return;
        }

        throw BLangExceptionHelper.getRuntimeException(RuntimeErrors.INCOMPATIBLE_TYPE_FOR_CASTING_JSON,
                BTypes.typeFloat, JSONUtils.getTypeName(jsonNode));
    }
}
