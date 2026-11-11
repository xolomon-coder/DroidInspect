package com.tanvir.droidinspect;

/** Host regression tests for IL instruction decoding and safe rename constraints. */
public final class DotNetIlReaderSelfTest {
    public static void main(String[] args) throws Exception {
        String il = DotNetIlReader.disassembleForTest(new byte[]{0x18,0x19,0x58,0x2a});
        require(il.contains("ldc.i4.2"),"ldc.i4.2");
        require(il.contains("ldc.i4.3"),"ldc.i4.3");
        require(il.contains("add"),"add");
        require(il.contains("ret"),"ret");
        DotNetIlReader.validateReplacementName("MethodA","MethodB");
        boolean rejected=false;
        try{DotNetIlReader.validateReplacementName("MethodA","MethodLong");}
        catch(java.io.IOException e){rejected=true;}
        require(rejected,"different byte lengths");
        rejected=false;
        try{DotNetIlReader.validateReplacementName("MethodA","Bad-Name");}
        catch(java.io.IOException e){rejected=true;}
        require(rejected,"invalid identifier");
        String branch=DotNetIlReader.disassembleForTest(new byte[]{0x2b,0x01,0x2a});
        require(branch.contains("br.s IL_0003"),"branch target");
        System.out.println("DotNetIlReaderSelfTest: PASS");
    }
    private static void require(boolean ok,String name){if(!ok)throw new AssertionError(name);}
}
