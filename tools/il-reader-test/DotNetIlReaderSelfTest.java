package com.tanvir.droidinspect;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Host tests: IL decoding plus a synthetic PE/CLI assembly read-edit-reopen cycle. */
public final class DotNetIlReaderSelfTest {
    public static void main(String[] args) throws Exception {
        String il = DotNetIlReader.disassembleForTest(new byte[]{0x18, 0x19, 0x58, 0x2a});
        require(il.contains("ldc.i4.2"), "ldc.i4.2");
        require(il.contains("ldc.i4.3"), "ldc.i4.3");
        require(il.contains("add"), "add");
        require(il.contains("ret"), "ret");
        require(il.contains("58 | add | Pop two values"), "opcode meaning and byte display");
        require(IlOpcodeExplainer.explain("not-a-real-opcode", "").startsWith("No verified explanation"), "unknown opcode explanation");
        String operand = DotNetIlReader.disassembleForTest(new byte[]{0x0e, 0x02, 0x2a});
        require(operand.contains("0E 02 | ldarg.s 2"), "operand bytes and operand");
        String unknown = DotNetIlReader.disassembleForTest(new byte[]{(byte)0xff});
        require(unknown.contains("FF | FF | No verified explanation"), "unknown opcode bytes");
        boolean truncatedRejected = false;
        try { DotNetIlReader.disassembleForTest(new byte[]{0x20, 0x01}); }
        catch (java.io.IOException expected) { truncatedRejected = true; }
        require(truncatedRejected, "truncated operand rejection");

        String branch = DotNetIlReader.disassembleForTest(new byte[]{0x2b, 0x01, 0x2a});
        require(branch.contains("br.s IL_0003"), "short branch target");

        DotNetIlReader.validateReplacementName("MethodA", "MethodB");
        reject(() -> DotNetIlReader.validateReplacementName("MethodA", "MethodLong"), "different byte lengths");
        reject(() -> DotNetIlReader.validateReplacementName("MethodA", "Bad-Name"), "invalid identifier");

        File source = File.createTempFile("di-managed-fixture-", ".dll");
        File renamed = File.createTempFile("di-renamed-fixture-", ".dll");
        File ilEdited = File.createTempFile("di-il-edited-fixture-", ".dll");
        File rejected = File.createTempFile("di-il-rejected-fixture-", ".dll");
        try {
            writeFixture(source);
            DotNetIlReader.Result original = DotNetIlReader.read(source);
            require(original.methods.size() == 1, "one MethodDef indexed");
            DotNetIlReader.MethodInfo method = original.methods.get(0);
            require("TestNS.MyType".equals(method.owner), "MethodDef owner resolution");
            require("MethodA".equals(method.name), "original method name");
            require(method.label().contains("MethodDef token 0x06000001"), "metadata token display");
            require(method.body.contains("ldc.i4.2") && method.body.contains("ret"), "method RVA and IL body decode");

            DotNetIlReader.renameMethod(source, 1, "MethodB", renamed);
            DotNetIlReader.Result after = DotNetIlReader.read(renamed);
            require(after.methods.size() == 1, "renamed MethodDef indexed");
            require("MethodB".equals(after.methods.get(0).name), "metadata rename survives re-open");
            require("MethodA".equals(DotNetIlReader.read(source).methods.get(0).name), "source assembly remains unchanged");

            DotNetIlReader.replaceMethodBody(source, 1, "19 19 59 2A", ilEdited);
            DotNetIlReader.Result edited = DotNetIlReader.read(ilEdited);
            require("1919592A".equals(edited.methods.get(0).codeHex), "edited IL bytes survive reopen");
            require(edited.methods.get(0).body.contains("ldc.i4.3") && edited.methods.get(0).body.contains("sub"),
                    "edited IL disassembles after reopen");
            require("1819582A".equals(DotNetIlReader.read(source).methods.get(0).codeHex),
                    "source IL remains unchanged");
            reject(() -> DotNetIlReader.replaceMethodBody(source, 1, "1919", rejected), "IL body size change");
            reject(() -> DotNetIlReader.replaceMethodBody(source, 1, "20000000", rejected), "operand-bearing IL edit");
            System.out.println("DotNetIlReaderSelfTest: PASS");
        } finally {
            source.delete();
            renamed.delete();
            ilEdited.delete();
            rejected.delete();
        }
    }

    private static void writeFixture(File file) throws Exception {
        byte[] image = new byte[0x600];
        ByteBuffer b = ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN);

        // DOS and PE headers.
        b.put(0, (byte) 'M'); b.put(1, (byte) 'Z'); b.putInt(0x3c, 0x80);
        b.putInt(0x80, 0x00004550);
        b.putShort(0x84, (short) 0x014c);
        b.putShort(0x86, (short) 1);
        b.putShort(0x94, (short) 224);
        b.putShort(0x96, (short) 0x2102);

        // PE32 optional header with a CLI data directory.
        int optional = 0x98;
        b.putShort(optional, (short) 0x10b);
        b.putInt(optional + 92, 16);
        b.putInt(optional + 96 + 14 * 8, 0x2000);
        b.putInt(optional + 96 + 14 * 8 + 4, 72);

        // One raw-backed section maps RVA 0x2000 to file offset 0x200.
        int section = optional + 224;
        byte[] sectionName = ".text".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(sectionName, 0, image, section, sectionName.length);
        b.putInt(section + 8, 0x400);
        b.putInt(section + 12, 0x2000);
        b.putInt(section + 16, 0x400);
        b.putInt(section + 20, 0x200);
        b.putInt(section + 36, 0x60000020);

        // CLI header at RVA 0x2000; metadata root at RVA 0x2048.
        b.putInt(0x200, 72);
        b.putShort(0x204, (short) 2);
        b.putShort(0x206, (short) 5);
        b.putInt(0x208, 0x2048);
        b.putInt(0x20c, 172);
        b.putInt(0x210, 1); // ILONLY; no strong-name signature flag.

        byte[] strings = ("\0Module.dll\0MyType\0TestNS\0MethodA\0")
                .getBytes(StandardCharsets.UTF_8);
        byte[] metadata = new byte[138 + strings.length];
        ByteBuffer m = ByteBuffer.wrap(metadata).order(ByteOrder.LITTLE_ENDIAN);
        m.putInt(0, 0x424a5342); // BSJB
        m.putShort(4, (short) 1);
        m.putShort(6, (short) 1);
        m.putInt(8, 0);
        m.putInt(12, 12);
        byte[] version = new byte[]{'v','4','.','0','.','3','0','3','1','9',0,0};
        System.arraycopy(version, 0, metadata, 16, version.length);
        m.putShort(28, (short) 0);
        m.putShort(30, (short) 2);

        // #~ stream descriptor.
        m.putInt(32, 64);
        m.putInt(36, 74);
        int q = 40;
        metadata[q++] = '#'; metadata[q++] = '~'; metadata[q++] = 0;
        while ((q & 3) != 0) metadata[q++] = 0;

        // #Strings stream descriptor.
        m.putInt(q, 138);
        m.putInt(q + 4, strings.length);
        q += 8;
        byte[] streamName = "#Strings\0".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(streamName, 0, metadata, q, streamName.length);

        // Tables stream: one Module, one TypeDef, one MethodDef row.
        int t = 64;
        m.put(t + 4, (byte) 2); // major
        m.put(t + 5, (byte) 0); // minor
        m.put(t + 6, (byte) 0); // heap sizes: 2-byte indices
        m.put(t + 7, (byte) 1);
        m.putLong(t + 8, (1L << 0) | (1L << 2) | (1L << 6));
        m.putLong(t + 16, 0);
        m.putInt(t + 24, 1); // Module rows
        m.putInt(t + 28, 1); // TypeDef rows
        m.putInt(t + 32, 1); // MethodDef rows

        int module = t + 36;
        m.putShort(module, (short) 0);
        m.putShort(module + 2, (short) 1); // Module.dll
        m.putShort(module + 4, (short) 0); // MVID
        m.putShort(module + 6, (short) 0);
        m.putShort(module + 8, (short) 0);

        int type = module + 10;
        m.putInt(type, 0);
        m.putShort(type + 4, (short) 12); // MyType
        m.putShort(type + 6, (short) 19); // TestNS
        m.putShort(type + 8, (short) 0);  // Extends
        m.putShort(type + 10, (short) 1); // FieldList
        m.putShort(type + 12, (short) 1); // MethodList

        int method = type + 14;
        m.putInt(method, 0x2200);          // method body RVA
        m.putShort(method + 4, (short) 0); // ImplFlags
        m.putShort(method + 6, (short) 0); // Flags
        m.putShort(method + 8, (short) 26); // MethodA
        m.putShort(method + 10, (short) 0); // signature blob
        m.putShort(method + 12, (short) 1); // ParamList

        System.arraycopy(metadata, 0, image, 0x248, metadata.length);
        System.arraycopy(strings, 0, image, 0x248 + 138, strings.length);

        // Tiny method header followed by ldc.i4.2, ldc.i4.3, add, ret.
        image[0x400] = 0x12;
        image[0x401] = 0x18;
        image[0x402] = 0x19;
        image[0x403] = 0x58;
        image[0x404] = 0x2a;

        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(image);
            out.flush();
        }
    }

    private interface CheckedAction { void run() throws Exception; }
    private static void reject(CheckedAction action, String name) throws Exception {
        boolean rejected = false;
        try { action.run(); } catch (java.io.IOException expected) { rejected = true; }
        require(rejected, name);
    }
    private static void require(boolean condition, String name) {
        if (!condition) throw new AssertionError(name + " failed");
    }
}
