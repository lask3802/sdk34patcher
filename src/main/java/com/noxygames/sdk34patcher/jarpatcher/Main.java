package com.noxygames.sdk34patcher.jarpatcher;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.*;


/*
* Don't use this patcher with decompiled jar from dex. Use it for source jar or DexPatcher instead.
* */
public final class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: <JAR/AAR file path>");
            return;
        }

        String filePath = args[0];
        if (filePath.endsWith(".aar")) {
            modifyAAR(filePath);
        } else if (filePath.endsWith(".jar")) {
            modifyJAR(filePath, filePath+"_patched.jar");
        } else if (filePath.endsWith(".class")){
            modifyClassFile(filePath, filePath+"_patched.class");
        }

        else {
            System.out.println("Only support aar/jar files");
        }
    }

    private static void modifyAAR(String aarFilePath) throws IOException {
        Path tempDir = Files.createTempDirectory("aar_temp");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(aarFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = tempDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Files.walk(tempDir).forEach(file -> {
            if (file.toString().endsWith(".jar")) {
                try {
                    modifyJAR(file.toString(), file.toString()+"_patched.jar");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(aarFilePath))) {
            Files.walk(tempDir).forEach(file -> {
                if (Files.isRegularFile(file)) {
                    try {
                        ZipEntry entry = new ZipEntry(tempDir.relativize(file).toString().replace("\\", "/"));
                        zos.putNextEntry(entry);
                        Files.copy(file, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(path -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void modifyJAR(String inputJarPath, String outputJarPath) throws IOException {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJarPath));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJarPath))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = jis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }

                byte[] classData = baos.toByteArray();
                if (entry.getName().endsWith(".class")) {
                    classData = modifyClass(classData);
                }

                JarEntry newEntry = new JarEntry(entry.getName());
                jos.putNextEntry(newEntry);
                jos.write(classData);
                jos.closeEntry();
            }
        }
    }


    private static void cacheJarClasses(String inputJarPath) throws IOException {
        JarInputStream jis = new JarInputStream(new FileInputStream(inputJarPath));

            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = jis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }

                byte[] classData = baos.toByteArray();
                if (entry.getName().endsWith(".class")) {
                    classData = modifyClass(classData);
                }
            }

    }

    private static void cacheClass(byte[] classData) {

    }

    private static void modifyClassFile(String inputFile, String outputFile) throws IOException {
        // 讀取 class 文件
        byte[] classData = Files.readAllBytes(Paths.get(inputFile));

        // 修改 class 文件
        byte[] modifiedClassData = modifyClass(classData);

        // 將修改後的 class 文件寫入輸出文件
        Files.write(Paths.get(outputFile), modifiedClassData);
    }

    private static byte[] modifyClass(byte[] classData) {
        ClassReader cr = new ClassReader(classData);
        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        cr.accept(classNode, ClassReader.EXPAND_FRAMES);

        for (MethodNode method : (List<MethodNode>) classNode.methods) {
            ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insn = iterator.next();
                if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (methodInsn.owner.equals("android/content/Context")
                            && methodInsn.name.equals("registerReceiver")
                            && methodInsn.desc.equals("(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;")) {

                        // 在前一個指令插入 ICONST_2
                        InsnList insnList = new InsnList();
                        insnList.add(new InsnNode(Opcodes.ICONST_2));
                        method.instructions.insertBefore(insn, insnList);

                        // 修改指令描述符
                        methodInsn.desc = "(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;I)Landroid/content/Intent;";
                    }
                }
            }
        }

        // 保留元數據
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS ) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    Class<?> c = Class.forName(type1.replace('/', '.'), false, getClass().getClassLoader());
                    Class<?> d = Class.forName(type2.replace('/', '.'), false, getClass().getClassLoader());
                    if (c.isAssignableFrom(d)) {
                        return type1;
                    }
                    if (d.isAssignableFrom(c)) {
                        return type2;
                    }
                    if (c.isInterface() || d.isInterface()) {
                        return "java/lang/Object";
                    } else {
                        do {
                            c = c.getSuperclass();
                        } while (!c.isAssignableFrom(d));
                        return c.getName().replace('.', '/');
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e.toString());
                }
            }
        };
        classNode.accept(cw);
        return cw.toByteArray();
    }

}