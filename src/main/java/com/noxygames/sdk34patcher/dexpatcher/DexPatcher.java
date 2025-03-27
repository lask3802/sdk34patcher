package com.noxygames.sdk34patcher.dexpatcher;

import org.jf.dexlib2.*;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11n;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.dexbacked.instruction.*;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.Instruction11n;
import org.jf.dexlib2.immutable.*;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.rewriter.*;
import org.jf.dexlib2.writer.builder.*;
import org.jf.dexlib2.writer.io.FileDataStore;
import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.*;

public class DexPatcher {

    static BuilderMethodReference gNewRegisterBroadcastRef;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("<source dex path> <dst dex path>");
            return ;
        }

        String inputFile = args[0];
        String outputFile = args[1];
        try {
            modifyDexWithRewrite(inputFile, outputFile);
        }catch(Exception e){
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return ;
    }

    private static void modifyDexWithRewrite(String inputFile, String outputFile) throws IOException {

        DexFile dexFile = DexFileFactory.loadDexFile(inputFile, Opcodes.forApi(21));
        //DexFile dexFile = MultiDexIO.readDexFile(true, new File(inputFile), new BasicDexFileNamer(), Opcodes.forApi(21),null);
        DexFile rewrittenDexFile = rewriteDex(dexFile);
        //MultiDexIO.writeDexFile(true, new File(outputFile), new BasicDexFileNamer(), dexFile, 1024*1024*100, null);
        DexPool pool = new DexPool(Opcodes.forApi(21));
        for(ClassDef cl: rewrittenDexFile.getClasses()){
            pool.internClass(cl);
        }
        pool.writeTo(new FileDataStore(new File(outputFile)));
    }



    private static boolean shouldRewriteMethod(MethodImplementation impl) {
        if(impl == null) return false;
        if(impl.getClass().getName().startsWith("com.google.android.play.core.assetpacks")) return false;
        for (Instruction instruction : impl.getInstructions()) {
            if (instruction.getOpcode() == Opcode.INVOKE_VIRTUAL) {
                ReferenceInstruction refInstruction = (ReferenceInstruction) instruction;
                if (refInstruction.getReference().toString().equals("Landroid/content/Context;->registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Instruction shiftParameterRegister(Instruction instruction, int origRegisterCnt, int parameters, int expandCount) {
        DexBackedInstruction inst = (DexBackedInstruction) instruction;

        switch (inst.opcode.format) {
            case Format10t:
                return instruction;
            case Format10x:
                return instruction;
            case Format11n: {
                Instruction11n inst1 = (Instruction11n) inst;
                int registerA = inst1.getRegisterA();
                if (registerA >= origRegisterCnt - parameters) {
                    int newReg = registerA + expandCount;
                    return new ImmutableInstruction11n(inst.getOpcode(), newReg, inst1.getNarrowLiteral());
                }
                break;
            }
            case Format11x: {
                int registerA = ((OneRegisterInstruction) inst).getRegisterA();
                if (registerA >= origRegisterCnt - parameters) {
                    int newReg = registerA + expandCount;
                    return new ImmutableInstruction11x(inst.getOpcode(), newReg);
                }
                break;
            }
            case Format12x: {
                int registerA = ((TwoRegisterInstruction) inst).getRegisterA();
                int registerB = ((TwoRegisterInstruction) inst).getRegisterB();
                boolean modified = false;
                if (registerA >= origRegisterCnt - parameters) {
                    registerA += expandCount;
                    modified = true;
                }
                if (registerB >= origRegisterCnt - parameters) {
                    registerB += expandCount;
                    modified = true;
                }
                if (modified) {
                    return new ImmutableInstruction12x(inst.getOpcode(), registerA, registerB);
                }
                break;
            }
            case Format20bc:
                return instruction;
            case Format20t:
                return instruction;
            case Format21c: {
                int registerA = ((OneRegisterInstruction) inst).getRegisterA();
                if (registerA >= origRegisterCnt - parameters) {
                    int newReg = registerA + expandCount;
                    return new ImmutableInstruction21c(inst.getOpcode(), newReg, ((ReferenceInstruction) inst).getReference());
                }
                break;
            }
            case Format21ih:
            case Format21lh:
            case Format21s: {
                int registerA = ((OneRegisterInstruction) inst).getRegisterA();
                if (registerA >= origRegisterCnt - parameters) {
                    int newReg = registerA + expandCount;
                    return new ImmutableInstruction21ih(inst.getOpcode(), newReg, ((NarrowLiteralInstruction) inst).getNarrowLiteral());
                }
                break;
            }
            case Format21t: {
                int registerA = ((OneRegisterInstruction) inst).getRegisterA();
                if (registerA >= origRegisterCnt - parameters) {
                    int newReg = registerA + expandCount;
                    return new ImmutableInstruction21t(inst.getOpcode(), newReg, ((OffsetInstruction) inst).getCodeOffset());
                }
                break;
            }
            case Format22b: {
                int registerA = ((TwoRegisterInstruction) inst).getRegisterA();
                int registerB = ((TwoRegisterInstruction) inst).getRegisterB();
                boolean modified = false;
                if (registerA >= origRegisterCnt - parameters) {
                    registerA += expandCount;
                    modified = true;
                }
                if (registerB >= origRegisterCnt - parameters) {
                    registerB += expandCount;
                    modified = true;
                }
                if (modified) {
                    return new ImmutableInstruction22b(inst.getOpcode(), registerA, registerB, ((NarrowLiteralInstruction) inst).getNarrowLiteral());
                }
                break;
            }
            case Format22c:
            case Format22cs: {
                int registerA = ((TwoRegisterInstruction) inst).getRegisterA();
                int registerB = ((TwoRegisterInstruction) inst).getRegisterB();
                boolean modified = false;
                if (registerA >= origRegisterCnt - parameters) {
                    registerA += expandCount;
                    modified = true;
                }
                if (registerB >= origRegisterCnt - parameters) {
                    registerB += expandCount;
                    modified = true;
                }
                if (modified) {
                    return new ImmutableInstruction22c(inst.getOpcode(), registerA, registerB, ((ReferenceInstruction) inst).getReference());
                }
                break;
            }
            case Format22s: {
                int registerA = ((TwoRegisterInstruction) inst).getRegisterA();
                int registerB = ((TwoRegisterInstruction) inst).getRegisterB();
                boolean modified = false;
                if (registerA >= origRegisterCnt - parameters) {
                    registerA += expandCount;
                    modified = true;
                }
                if (registerB >= origRegisterCnt - parameters) {
                    registerB += expandCount;
                    modified = true;
                }
                if (modified) {
                    return new ImmutableInstruction22s(inst.getOpcode(), registerA, registerB, ((NarrowLiteralInstruction) inst).getNarrowLiteral());
                }
                break;
            }
            case Format22t: {
                int registerA = ((TwoRegisterInstruction) inst).getRegisterA();
                int registerB = ((TwoRegisterInstruction) inst).getRegisterB();
                boolean modified = false;
                if (registerA >= origRegisterCnt - parameters) {
                    registerA += expandCount;
                    modified = true;
                }
                if (registerB >= origRegisterCnt - parameters) {
                    registerB += expandCount;
                    modified = true;
                }
                if (modified) {
                    return new ImmutableInstruction22t(inst.getOpcode(), registerA, registerB, ((OffsetInstruction) inst).getCodeOffset());
                }
                break;
            }
            case Format22x: {
                int registerA = ((TwoRegisterInstruction) inst).getRegisterA();
                int registerB = ((TwoRegisterInstruction) inst).getRegisterB();
                boolean modified = false;
                if (registerA >= origRegisterCnt - parameters) {
                    registerA += expandCount;
                    modified = true;
                }
                if (registerB >= origRegisterCnt - parameters) {
                    registerB += expandCount;
                    modified = true;
                }
                if (modified) {
                    return new ImmutableInstruction22x(inst.getOpcode(), registerA, registerB);
                }
                break;
            }
            case Format23x: {
                int registerA = ((ThreeRegisterInstruction) inst).getRegisterA();
                int registerB = ((ThreeRegisterInstruction) inst).getRegisterB();
                int registerC = ((ThreeRegisterInstruction) inst).getRegisterC();
                boolean modified = false;
                if (registerA >= origRegisterCnt - parameters) {
                    registerA += expandCount;
                    modified = true;
                }
                if (registerB >= origRegisterCnt - parameters) {
                    registerB += expandCount;
                    modified = true;
                }
                if (registerC >= origRegisterCnt - parameters) {
                    registerC += expandCount;
                    modified = true;
                }
                if (modified) {
                    return new ImmutableInstruction23x(inst.getOpcode(), registerA, registerB, registerC);
                }
                break;
            }
            case Format30t:
                return instruction;
            case Format31c: {
                int registerA = ((OneRegisterInstruction) inst).getRegisterA();
                if (registerA >= origRegisterCnt - parameters) {
                    int newReg = registerA + expandCount;
                    return new ImmutableInstruction31c(inst.getOpcode(), newReg, ((ReferenceInstruction) inst).getReference());
                }
                break;
            }
            case Format31i: {
                int registerA = ((OneRegisterInstruction) inst).getRegisterA();
                if (registerA >= origRegisterCnt - parameters) {
                    int newReg = registerA + expandCount;
                    return new ImmutableInstruction31i(inst.getOpcode(), newReg, ((NarrowLiteralInstruction) inst).getNarrowLiteral());
                }
                break;
            }
            case Format31t: {
                int registerA = ((OneRegisterInstruction) inst).getRegisterA();
                if (registerA >= origRegisterCnt - parameters) {
                    int newReg = registerA + expandCount;
                    return new ImmutableInstruction31t(inst.getOpcode(), newReg, ((OffsetInstruction) inst).getCodeOffset());
                }
                break;
            }
            case Format32x: {
                int registerA = ((TwoRegisterInstruction) inst).getRegisterA();
                int registerB = ((TwoRegisterInstruction) inst).getRegisterB();
                boolean modified = false;
                if (registerA >= origRegisterCnt - parameters) {
                    registerA += expandCount;
                    modified = true;
                }
                if (registerB >= origRegisterCnt - parameters) {
                    registerB += expandCount;
                    modified = true;
                }
                if (modified) {
                    return new ImmutableInstruction32x(inst.getOpcode(), registerA, registerB);
                }
                break;
            }
            case Format35c:
            case Format35ms:
            case Format35mi: {
                DexBackedInstruction35c inst35c = (DexBackedInstruction35c) inst;
                int[] registers = new int[] {
                        inst35c.getRegisterC(),
                        inst35c.getRegisterD(),
                        inst35c.getRegisterE(),
                        inst35c.getRegisterF(),
                        inst35c.getRegisterG()
                };
                boolean modified = false;
                for (int i = 0; i < registers.length; i++) {
                    if (registers[i] >= origRegisterCnt - parameters) {
                        registers[i] += expandCount;
                        modified = true;
                    }
                }
                if (modified) {
                    return new ImmutableInstruction35c(
                            inst.getOpcode(),
                            inst35c.getRegisterCount(),
                            registers[0],
                            registers[1],
                            registers[2],
                            registers[3],
                            registers[4],
                            inst35c.getReference()
                    );
                }
                break;
            }
            case Format3rc:
            case Format3rmi:
            case Format3rms: {
                DexBackedInstruction3rc inst3rc = (DexBackedInstruction3rc) inst;
                int startRegister = inst3rc.getStartRegister();
                if (startRegister >= origRegisterCnt - parameters) {
                    int newStartRegister = startRegister + expandCount;
                    return new ImmutableInstruction3rc(inst.getOpcode(), newStartRegister, inst3rc.getRegisterCount(), inst3rc.getReference());
                }
                break;
            }
            case Format45cc: {
                DexBackedInstruction45cc inst45cc = ( DexBackedInstruction45cc) inst;
                int[] registers = new int[] {
                        inst45cc.getRegisterC(),
                        inst45cc.getRegisterD(),
                        inst45cc.getRegisterE(),
                        inst45cc.getRegisterF(),
                        inst45cc.getRegisterG()
                };
                boolean modified = false;
                for (int i = 0; i < registers.length; i++) {
                    if (registers[i] >= origRegisterCnt - parameters) {
                        registers[i] += expandCount;
                        modified = true;
                    }
                }
                if (modified) {
                    return new ImmutableInstruction45cc(
                            inst.getOpcode(),
                            inst45cc.getRegisterCount(),
                            registers[0],
                            registers[1],
                            registers[2],
                            registers[3],
                            registers[4],
                            inst45cc.getReference(),
                            inst45cc.getReference2()
                    );
                }
                break;
            }
            case Format4rcc: {
                DexBackedInstruction4rcc inst4rcc = ( DexBackedInstruction4rcc) inst;
                int startRegister = inst4rcc.getStartRegister();
                if (startRegister >= origRegisterCnt - parameters) {
                    int newStartRegister = startRegister + expandCount;
                    return new ImmutableInstruction4rcc(inst.getOpcode(), newStartRegister, inst4rcc.getRegisterCount(), inst4rcc.getReference(), inst4rcc.getReference2());
                }
                break;
            }
            case Format51l: {
                int registerA = ((OneRegisterInstruction) inst).getRegisterA();
                if (registerA >= origRegisterCnt - parameters) {
                    int newReg = registerA + expandCount;
                    return new ImmutableInstruction51l(inst.getOpcode(), newReg, ((WideLiteralInstruction) inst).getWideLiteral());
                }
                break;
            }
            case PackedSwitchPayload:
                return instruction;
            case SparseSwitchPayload:
                return instruction;
            case ArrayPayload:
                return instruction;
            case UnresolvedOdexInstruction:
                break;
            default:
                throw new ExceptionWithContext("Unexpected opcode format: %s", new Object[]{inst.opcode.format.toString()});
        }
        return instruction;
    }

    private static void patchSwitchInstruction(List<Instruction> instructions, HashMap<Integer,Integer> relocateMap){
        int currentOffset = 0;
        for(int j = 0; j < instructions.size(); j++) {
            Instruction inst2 = instructions.get(j);
            if(inst2.getOpcode() == Opcode.PACKED_SWITCH){
                DexBackedInstruction31t inst = (DexBackedInstruction31t)inst2;
                System.out.println(String.format("packed-switch %d table-offset: %d", currentOffset, inst.getCodeOffset()));
                Integer relocateOffset = relocateMap.get(currentOffset + inst.getCodeOffset());

                if(relocateOffset != null){

                    int newOffset = relocateOffset-currentOffset;
                    System.out.println(String.format("packed-switch %d relocate-offset: %d", currentOffset, newOffset));
                    Instruction newInst = new ImmutableInstruction31t(
                            inst2.getOpcode(),
                            ((DexBackedInstruction31t) inst2).getRegisterA(),
                            newOffset
                    );
                    instructions.set(j, newInst);

                }

            }
            else if(inst2.getOpcode() == Opcode.SPARSE_SWITCH){
                DexBackedInstruction31t inst = (DexBackedInstruction31t)inst2;
                System.out.println(String.format("sparse-switch %d table-offset: %d", currentOffset, inst.getCodeOffset()));
                Integer relocateOffset = relocateMap.get(currentOffset + inst.getCodeOffset());
                if(relocateOffset != null){
                    int newOffset = relocateOffset-currentOffset;
                    System.out.println(String.format("sparse-switch %d relocate-offset: %d", currentOffset, newOffset));
                    Instruction newInst = new ImmutableInstruction31t(
                            inst2.getOpcode(),
                            ((DexBackedInstruction31t) inst2).getRegisterA(),
                            newOffset
                    );
                    instructions.set(j, newInst);
                }

            }
            currentOffset+= inst2.getCodeUnits();
        }
    }

    private static void checkAndFixAlignment(String methodName, List<Instruction> instructions) {
        int currentOffset = 0;
        HashMap<Integer, Integer> relocateTable = new HashMap<Integer, Integer>();
        int relocateOffsets = 0;
        for(int i = 0; i < instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            if(inst.getOpcode().format == Format.PackedSwitchPayload || inst.getOpcode().format == Format.SparseSwitchPayload || inst.getOpcode().format == Format.ArrayPayload)
            {
                //some relocate happened before, fix all offset after relocate
                if(!relocateTable.isEmpty()){
                    relocateTable.put(currentOffset - relocateOffsets, currentOffset);
                }

                if(currentOffset %2 != 0){
                    System.out.println(String.format("Found non aligned instruction at %s offset: %d, prevInstruction: %s", methodName, currentOffset, instructions.get(Math.max(i-1,0)).toString()));

                    relocateTable.put(currentOffset, currentOffset+1);
                    instructions.add(i, new ImmutableInstruction10x(Opcode.NOP));
                    currentOffset+=1;
                    relocateOffsets++;
                }
            }
            currentOffset += inst.getCodeUnits();
        }
        if(!relocateTable.isEmpty()){
            for(Map.Entry<Integer, Integer> entry : relocateTable.entrySet()){
                System.out.println("try relocate " + entry.getKey() + "->" + entry.getValue());
            }

            patchSwitchInstruction(instructions, relocateTable);
        }
    }


    private static MethodImplementation rewriteMethodImplementation(MethodImplementation impl, int parameters){
        List<Instruction> newInstructions = new ArrayList<>();

        int currentOffset = 0;
        int latestOffset = 0;

        for (Instruction instruction : impl.getInstructions()) {
            currentOffset = latestOffset;
            latestOffset = currentOffset + instruction.getCodeUnits();
            if (instruction.getOpcode() == Opcode.INVOKE_VIRTUAL) {
                ReferenceInstruction refInstruction = (ReferenceInstruction) instruction;
                DexBackedInstruction35c oldInstruction = (DexBackedInstruction35c) instruction;

                if (refInstruction.getReference().toString().equals("Landroid/content/Context;->registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;")) {

                    int newRegister = impl.getRegisterCount()-parameters-1;

                    int regC = oldInstruction.getRegisterC() >= impl.getRegisterCount()-1-parameters? oldInstruction.getRegisterC()+1:oldInstruction.getRegisterC();
                    int regD = oldInstruction.getRegisterD() >= impl.getRegisterCount()-1-parameters? oldInstruction.getRegisterD()+1:oldInstruction.getRegisterD();
                    int regE = oldInstruction.getRegisterE() >= impl.getRegisterCount()-1-parameters? oldInstruction.getRegisterE()+1:oldInstruction.getRegisterE();

                    //Check all branch before instruction
                    for(int i = 0, newInstOffset = 0 ; i < newInstructions.size(); i++){
                        Instruction inst = newInstructions.get(i);
                        if(inst.getOpcode() == Opcode.GOTO ){

                            DexBackedInstruction10t gotoInst =  (DexBackedInstruction10t)inst;

                            //jump over rewritten instruction
                            if(gotoInst.getCodeOffset() + newInstOffset > currentOffset) {
                                newInstructions.set(i, new ImmutableInstruction10t(inst.getOpcode(), gotoInst.getCodeOffset() + 1));
                            }
                        }

                        if(inst instanceof DexBackedInstruction22t){
                            DexBackedInstruction22t inst22 = (DexBackedInstruction22t) inst;
                            if(inst22.getCodeOffset() + newInstOffset > currentOffset) {
                                newInstructions.set(i, new ImmutableInstruction22t(inst.getOpcode(), inst22.getRegisterA(), inst22.getRegisterB(), inst22.getCodeOffset() + 1 ));
                            }
                        }

                        if(inst instanceof DexBackedInstruction21t){
                            DexBackedInstruction21t inst21 = (DexBackedInstruction21t) inst;
                            if(inst21.getCodeOffset() + newInstOffset > currentOffset) {
                                newInstructions.set(i, new ImmutableInstruction21t(inst.getOpcode(), inst21.getRegisterA(), inst21.getCodeOffset() + 1 ));
                            }
                        }
                        newInstOffset+=newInstructions.get(i).getCodeUnits();
                    }

                    newInstructions.add(new BuilderInstruction11n(Opcode.CONST_4,newRegister ,2));
                    newInstructions.add(new BuilderInstruction35c(
                            Opcode.INVOKE_VIRTUAL,
                            4,
                            regC,
                            regD,
                            regE,
                            newRegister,
                            oldInstruction.getRegisterG(),
                            new ImmutableMethodReference(
                                    "Landroid/content/Context;",
                                    "registerReceiver",
                                    Arrays.asList(
                                            "Landroid/content/BroadcastReceiver;",
                                            "Landroid/content/IntentFilter;",
                                            "I"
                                    ),
                                    "Landroid/content/Intent;"
                            )
                    ));

                    continue;
                }
            }


            newInstructions.add(shiftParameterRegister(instruction, impl.getRegisterCount()-1, parameters, 1));

        }
        checkAndFixAlignment(impl.getClass().getName(), newInstructions );
        ImmutableMethodImplementation immutableMethodImplementation = new ImmutableMethodImplementation(
                impl.getRegisterCount() + 1,
                newInstructions,
                impl.getTryBlocks(),
                impl.getDebugItems()
        );
        return immutableMethodImplementation;
    }


    private static DexFile rewriteDex(DexFile dexFile){
        DexRewriter rewriter = new DexRewriter(new RewriterModule(){
            @Nonnull
            @Override
            public Rewriter<Method> getMethodRewriter(@Nonnull Rewriters rewriters) {
                return new Rewriter<Method>() {
                    @Nonnull
                    @Override
                    public Method rewrite(@Nonnull Method method) {
                        if(shouldRewriteMethod(method.getImplementation())){
                            return new ImmutableMethod(
                                    method.getDefiningClass(),
                                    method.getName(),
                                    method.getParameters(),
                                    method.getReturnType(),
                                    method.getAccessFlags(),
                                    method.getAnnotations(),
                                    method.getHiddenApiRestrictions(),
                                    rewriteMethodImplementation(method.getImplementation(), method.getParameters().size())
                            );
                        }
                        return new ImmutableMethod(
                                method.getDefiningClass(),
                                method.getName(),
                                method.getParameters(),
                                method.getReturnType(),
                                method.getAccessFlags(),
                                method.getAnnotations(),
                                method.getHiddenApiRestrictions(),
                                method.getImplementation()
                        );

                    }
                };
            }

        });



        return rewriter.getDexFileRewriter().rewrite(dexFile);
    }

    private static List<Method> modifyMethods(Iterable<? extends Method> methods) {
        List<Method> newMethods = new ArrayList<>();

        for (Method method : methods) {
            List<Instruction> newInstructions = new ArrayList<>();

            if(method.getImplementation() == null){
                newMethods.add(method);
                continue;
            }


            for (Instruction instruction : method.getImplementation().getInstructions()) {
                if (instruction.getOpcode() == Opcode.INVOKE_VIRTUAL) {
                    ReferenceInstruction refInstruction = (ReferenceInstruction) instruction;
                    DexBackedInstruction35c oldInstruction = (DexBackedInstruction35c) instruction;

                    if (refInstruction.getReference().toString().equals("Landroid/content/Context;->registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;")) {


                        int constRegister = Collections.max(Arrays.asList(
                                oldInstruction.getRegisterC(),
                                oldInstruction.getRegisterD(),
                                oldInstruction.getRegisterE()
                                )) + 1 ;
                        newInstructions.add(new BuilderInstruction11n(Opcode.CONST_4,constRegister ,2));




                        newInstructions.add(new BuilderInstruction35c(
                                Opcode.INVOKE_VIRTUAL,
                                oldInstruction.getRegisterCount()+1,
                                oldInstruction.getRegisterC(),
                                oldInstruction.getRegisterD(),
                                oldInstruction.getRegisterE(),
                                constRegister,
                                oldInstruction.getRegisterG(),
                                new ImmutableMethodReference(
                                        "Landroid/content/Context;",
                                        "registerReceiver",
                                        Arrays.asList(
                                                "Landroid/content/BroadcastReceiver;",
                                                "Landroid/content/IntentFilter;",
                                                "I"
                                        ),
                                        "Landroid/content/Intent;"
                                )
                        ));
                        continue;
                    }
                }


                newInstructions.add(instruction);
            }


            MethodImplementation methodImpl = new ImmutableMethodImplementation(
                    method.getImplementation().getRegisterCount(),
                    newInstructions,
                    method.getImplementation().getTryBlocks(),
                    method.getImplementation().getDebugItems()
            );


            ImmutableMethod newMethod = new ImmutableMethod(
                    method.getDefiningClass(),
                    method.getName(),
                    method.getParameters(),
                    method.getReturnType(),
                    method.getAccessFlags(),
                    method.getAnnotations(),
                    method.getHiddenApiRestrictions(),
                    methodImpl
            );


            newMethods.add(newMethod);
        }

        return newMethods;
    }
}
