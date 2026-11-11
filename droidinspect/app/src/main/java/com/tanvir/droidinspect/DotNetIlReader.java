package com.tanvir.droidinspect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/** Bounded ECMA-335 method-body reader. Displays raw IL, not reconstructed C#. */
final class DotNetIlReader {
    private static final int MAX_FILE = 512 * 1024 * 1024;
    private static final int MAX_METHODS = 10000;
    private static final int MAX_BODY = 256 * 1024;
    private static final int MAX_INSTRUCTIONS = 250;
    private static final int N=0, SI=1, I4=2, I8=3, R4=4, R8=5, SV=6, V=7, SB=8, B=9, SW=10, T=11;
    private static final String[] OPS = new String[256];
    private static final int[] ARGS = new int[256];
    private static final String[] FE = new String[256];
    private static final int[] FEA = new int[256];

    static {
        set(0x00,"nop",N); set(0x01,"break",N);
        String[] fixed={"ldarg.0","ldarg.1","ldarg.2","ldarg.3","ldloc.0","ldloc.1","ldloc.2","ldloc.3","stloc.0","stloc.1","stloc.2","stloc.3"};
        for(int i=0;i<fixed.length;i++)set(0x02+i,fixed[i],N);
        set(0x0e,"ldarg.s",SV);set(0x0f,"ldarga.s",SV);set(0x10,"starg.s",SV);
        set(0x11,"ldloc.s",SV);set(0x12,"ldloca.s",SV);set(0x13,"stloc.s",SV);
        set(0x14,"ldnull",N);set(0x15,"ldc.i4.m1",N);
        for(int i=0;i<=8;i++)set(0x16+i,"ldc.i4."+i,N);
        set(0x1f,"ldc.i4.s",SI);set(0x20,"ldc.i4",I4);set(0x21,"ldc.i8",I8);
        set(0x22,"ldc.r4",R4);set(0x23,"ldc.r8",R8);set(0x25,"dup",N);set(0x26,"pop",N);
        set(0x27,"jmp",T);set(0x28,"call",T);set(0x29,"calli",T);set(0x2a,"ret",N);
        String[] shortBranches={"br.s","brfalse.s","brtrue.s","beq.s","bge.s","bgt.s","ble.s","blt.s","bne.un.s","bge.un.s","bgt.un.s","ble.un.s","blt.un.s"};
        for(int i=0;i<shortBranches.length;i++)set(0x2b+i,shortBranches[i],SB);
        String[] longBranches={"br","brfalse","brtrue","beq","bge","bgt","ble","blt","bne.un","bge.un","bgt.un","ble.un","blt.un"};
        for(int i=0;i<longBranches.length;i++)set(0x38+i,longBranches[i],B);
        set(0x45,"switch",SW);
        String[] simple={
            "ldind.i1","ldind.u1","ldind.i2","ldind.u2","ldind.i4","ldind.u4","ldind.i8","ldind.i","ldind.r4","ldind.r8","ldind.ref",
            "stind.ref","stind.i1","stind.i2","stind.i4","stind.i8","stind.r4","stind.r8",
            "add","sub","mul","div","div.un","rem","rem.un","and","or","xor","shl","shr","shr.un","neg","not",
            "conv.i1","conv.i2","conv.i4","conv.i8","conv.r4","conv.r8","conv.u4","conv.u8"
        };
        for(int i=0;i<simple.length;i++)set(0x46+i,simple[i],N);
        set(0x6f,"callvirt",T);set(0x70,"cpobj",T);set(0x71,"ldobj",T);set(0x72,"ldstr",T);
        set(0x73,"newobj",T);set(0x74,"castclass",T);set(0x75,"isinst",T);set(0x76,"conv.r.un",N);
        set(0x79,"unbox",T);set(0x7a,"throw",N);
        String[] fields={"ldfld","ldflda","stfld","ldsfld","ldsflda","stsfld"};
        for(int i=0;i<fields.length;i++)set(0x7b+i,fields[i],T);
        set(0x81,"stobj",T);
        String[] conv={"conv.ovf.i1.un","conv.ovf.i2.un","conv.ovf.i4.un","conv.ovf.i8.un","conv.ovf.u1.un","conv.ovf.u2.un","conv.ovf.u4.un","conv.ovf.u8.un","conv.ovf.i.un","conv.ovf.u.un"};
        for(int i=0;i<conv.length;i++)set(0x82+i,conv[i],N);
        set(0x8c,"box",T);set(0x8d,"newarr",T);set(0x8e,"ldlen",N);set(0x8f,"ldelema",T);
        String[] elem={"ldelem.i1","ldelem.u1","ldelem.i2","ldelem.u2","ldelem.i4","ldelem.u4","ldelem.i8","ldelem.i","ldelem.r4","ldelem.r8","ldelem.ref","stelem.i","stelem.i1","stelem.i2","stelem.i4","stelem.i8","stelem.r4","stelem.r8","stelem.ref"};
        for(int i=0;i<elem.length;i++)set(0x90+i,elem[i],N);
        set(0xa3,"ldelem",T);set(0xa4,"stelem",T);set(0xa5,"unbox.any",T);
        String[] conv2={"conv.ovf.i1","conv.ovf.u1","conv.ovf.i2","conv.ovf.u2","conv.ovf.i4","conv.ovf.u4","conv.ovf.i8","conv.ovf.u8"};
        for(int i=0;i<conv2.length;i++)set(0xb3+i,conv2[i],N);
        set(0xc2,"refanyval",T);set(0xc3,"ckfinite",N);set(0xc6,"mkrefany",T);set(0xd0,"ldtoken",T);
        String[] tail={"conv.u2","conv.u1","conv.i","conv.ovf.i","conv.ovf.u","add.ovf","add.ovf.un","mul.ovf","mul.ovf.un","sub.ovf","sub.ovf.un","endfinally","leave","leave.s","stind.i","conv.u"};
        for(int i=0;i<tail.length;i++)set(0xd1+i,tail[i],i==12?B:(i==13?SB:N));
        fe(0x00,"arglist",N);fe(0x01,"ceq",N);fe(0x02,"cgt",N);fe(0x03,"cgt.un",N);
        fe(0x04,"clt",N);fe(0x05,"clt.un",N);fe(0x06,"ldftn",T);fe(0x07,"ldvirtftn",T);
        fe(0x09,"ldarg",V);fe(0x0a,"ldarga",V);fe(0x0b,"starg",V);fe(0x0c,"ldloc",V);
        fe(0x0d,"ldloca",V);fe(0x0e,"stloc",V);fe(0x0f,"localloc",N);fe(0x11,"endfilter",N);
        fe(0x12,"unaligned.",SI);fe(0x13,"volatile.",N);fe(0x14,"tail.",N);fe(0x15,"initobj",T);
        fe(0x16,"constrained.",T);fe(0x17,"cpblk",N);fe(0x18,"initblk",N);fe(0x1a,"rethrow",N);
        fe(0x1c,"sizeof",T);fe(0x1d,"refanytype",N);fe(0x1e,"readonly.",N);
    }

    static final class MethodInfo {
        final int row, flags;
        final String owner, name, body;
        final long rva, nameIndex;
        MethodInfo(int row,int flags,String owner,String name,long rva,long nameIndex,String body){
            this.row=row;this.flags=flags;this.owner=owner;this.name=name;this.rva=rva;this.nameIndex=nameIndex;this.body=body;
        }
        String label(){return owner+" :: "+name+"  (RVA 0x"+DotNetIlReader.h8(rva)+")";}
    }
    static final class Result {
        final List<MethodInfo> methods=new ArrayList<>();
        String assembly="(unknown assembly)";
        String report="";
    }
    private static final class Pe {
        void close() throws IOException { if (f != null) f.close(); }
        RandomAccessFile f;long len,meta,metaSize,strings,stringsSize,tables,tablesSize,cliFlags,optional,sectionTable,checksum,certSize;
        int sections,magic,dd,heap,stringW,blobW,guidW;
        long[] rows=new long[64],base=new long[64];
        int sw(){return (heap&1)!=0?4:2;}
        int bw(){return (heap&4)!=0?4:2;}
        int gw(){return (heap&2)!=0?4:2;}
        int tw(int t){return rows[t]>=65536?4:2;}
        int cw(int c){
            int[] bits={2,2,3,2,5,3,1,2,1,1,1,2,1};
            int[][] map={{0,26,35,1},{2,1,27},{2,1,26,6,27},{4,8,23},{6,4,1,2,8,9,10,0,14,23,20,17,26,27,32,35,38,39,40,42,44,43},{6,10},{4,8},{2,6,32},{20,23},{6,10},{4,6},{38,35,39},{2,6}};
            long max=0;for(int t:map[c])if(rows[t]>max)max=rows[t];
            return max<(1L<<(16-bits[c]))?2:4;
        }
        long rowSize(int t){
            switch(t){
                case 0:return 2+sw()+3L*gw();case 1:return cw(0)+2L*sw();
                case 2:return 4+2L*sw()+cw(1)+tw(4)+tw(6);case 3:return tw(4);
                case 4:return 2+sw()+bw();case 5:return tw(6);case 6:return 8+sw()+bw()+tw(8);
                case 7:return tw(8);case 8:return 4+sw();case 9:return tw(2)+cw(1);
                case 10:return cw(2)+sw()+bw();case 11:return 2+cw(3)+bw();
                case 12:return cw(4)+cw(5)+bw();case 13:return cw(6)+bw();case 14:return 2+cw(7)+bw();
                case 15:return 6+tw(2);case 16:return 4+tw(4);case 17:return bw();
                case 18:return tw(2)+tw(20);case 19:return tw(20);case 20:return 2+sw()+cw(1);
                case 21:return tw(2)+tw(23);case 22:return tw(23);case 23:return 2+sw()+bw();
                case 24:return 2+tw(6)+cw(8);case 25:return tw(2)+2L*cw(9);case 26:return sw();
                case 27:return bw();case 28:return 2+cw(10)+sw()+tw(26);case 29:return 4+tw(4);
                case 30:return 8;case 31:return 4;case 32:return 16+bw()+2L*sw();case 33:return 4;
                case 34:return 12;case 35:return 12+2L*bw()+2L*sw();case 36:return 4+tw(35);
                case 37:return 12+tw(35);case 38:return 4+sw()+bw();case 39:return 8+2L*sw()+cw(11);
                case 40:return 8+sw()+cw(11);case 41:return 2L*tw(2);case 42:return 4+cw(12)+sw();
                case 43:return cw(9)+bw();case 44:return tw(42)+cw(1);default:return -1;
            }
        }
        long index(long p,int w)throws Exception{return w==2?u16(f,p):u32(f,p);}
        String str(long ix)throws Exception{
            if(ix==0)return "";
            if(strings==0||ix<0||ix>=stringsSize)throw new IOException("String heap index out of range.");
            long p=strings+ix;ByteArrayOutputStream out=new ByteArrayOutputStream();
            for(int n=0;n<16384&&p<strings+stringsSize;n++){
                int b=byteAt(f,p++);if(b==0)return new String(out.toByteArray(),StandardCharsets.UTF_8);out.write(b);
            }
            throw new IOException("Unterminated or oversized metadata string.");
        }
    }

    private DotNetIlReader(){}
    static Result read(File file)throws Exception{
        Pe pe=parse(file);
        try {
        Result result=new Result();
        if(pe.rows[32]>0)result.assembly=pe.str(pe.index(pe.base[32]+16+pe.bw(),pe.sw()));
        long count=pe.rows[6];if(count==0){result.report="No MethodDef rows found.";return result;}
        HashMap<Integer,String> owners=new HashMap<>();
        long typeSize=pe.rowSize(2),methodCount=pe.rows[6];
        for(int i=0;i<(int)Math.min(pe.rows[2],10000);i++){
            long p=pe.base[2]+i*typeSize;
            String n=pe.str(pe.index(p+4,pe.sw())),ns=pe.str(pe.index(p+4+pe.sw(),pe.sw()));
            long list=p+4+2L*pe.sw()+pe.cw(1);
            long first=pe.index(list+pe.tw(4),pe.tw(6));
            long next=i+1<pe.rows[2]?pe.index(p+typeSize+4+2L*pe.sw()+pe.cw(1)+pe.tw(4),pe.tw(6)):methodCount+1;
            String owner=ns.isEmpty()?n:ns+"."+n;
            if(first>=1&&next>=first&&first<=methodCount+1)for(long row=first;row<Math.min(next,methodCount+1);row++)owners.put((int)row,owner);
        }
        int limit=(int)Math.min(count,MAX_METHODS);long ms=pe.rowSize(6);int bodies=0;
        for(int i=0;i<limit;i++){
            long p=pe.base[6]+i*ms,rva=u32(pe.f,p);int flags=u16(pe.f,p+6);
            long nameIndex=pe.index(p+8,pe.sw());String name=pe.str(nameIndex);
            String owner=owners.get(i+1);if(owner==null)owner="(module)";
            String body;
            if(rva==0)body="No method body (abstract, runtime, native, or external declaration).";
            else try{long off=rvaToOffset(pe.f,rva,pe.sectionTable,pe.sections,pe.len);if(off<0)throw new IOException("RVA does not map to file bytes.");body=methodBody(pe.f,off,pe.len);bodies++;}
            catch(Exception e){body="IL body unavailable: "+safe(e.getMessage());}
            result.methods.add(new MethodInfo(i+1,flags,owner,name,rva,nameIndex,body));
        }
        result.report="ASSEMBLY: "+result.assembly+"\nMethodDef rows: "+count+"\nMethods indexed: "+limit+(count>limit?" (safety cap reached)":"")+"\nBodies decoded: "+bodies+"\nView: raw ECMA-335 IL; metadata tokens are numeric.\nC# source reconstruction is not implemented.";
        return result;
        } finally { pe.close(); }
    }

    static void renameMethod(File source,int row,String replacement,File output)throws Exception{
        if(row<1||source==null||output==null)throw new IOException("Invalid rename request.");
        Pe pe=parse(source);
        try {
        if((pe.cliFlags&8L)!=0)throw new IOException("Strong-name signed assemblies cannot be edited by this writer.");
        if(pe.certSize!=0)throw new IOException("Authenticode-signed PE files cannot be edited by this writer.");
        Result result=read(source);MethodInfo target=null;
        for(MethodInfo m:result.methods)if(m.row==row){target=m;break;}
        if(target==null)throw new IOException("Method row is not indexed.");
        if((target.flags&0x0800)!=0||(target.flags&0x1000)!=0)throw new IOException("Special-name methods are not supported by the constrained rename tool.");
        validateReplacementName(target.name,replacement);
        for(MethodInfo m:result.methods)if(m.row!=row&&m.owner.equals(target.owner)&&m.name.equals(replacement))throw new IOException("A method with that name already exists in this type.");
        if(countStringRefs(pe,target.nameIndex)!=1)throw new IOException("The metadata string is shared by multiple rows; rename refused.");
        copy(source,output);
        try(RandomAccessFile out=new RandomAccessFile(output,"rw")){
            out.seek(pe.strings+target.nameIndex);out.write(replacement.getBytes(StandardCharsets.UTF_8));out.writeByte(0);
            out.seek(pe.checksum);out.writeInt(0);
        }
        Result check=read(output);MethodInfo changed=null;
        for(MethodInfo m:check.methods)if(m.row==row){changed=m;break;}
        if(changed==null||!replacement.equals(changed.name)){output.delete();throw new IOException("Post-write verification failed; output discarded.");}
        } finally { pe.close(); }
    }

    static void validateReplacementName(String oldName,String newName)throws IOException{
        if(oldName==null||newName==null||newName.trim().isEmpty()||!newName.matches("[A-Za-z_][A-Za-z0-9_]*"))
            throw new IOException("Use a non-empty ASCII identifier (letters, digits and underscore; not starting with a digit).");
        if(newName.getBytes(StandardCharsets.UTF_8).length!=oldName.getBytes(StandardCharsets.UTF_8).length)
            throw new IOException("Safe rename requires exactly the same UTF-8 byte length as the current method name.");
    }
    static String disassembleForTest(byte[] code)throws Exception{return disassemble(code);}

    private static Pe parse(File file)throws Exception{
        if(file==null||!file.isFile()||file.length()<256||file.length()>MAX_FILE)throw new IOException("Invalid input or 512 MB limit exceeded.");
        Pe p=new Pe();p.f=new RandomAccessFile(file,"r");p.len=file.length();
        try{
            if(u16(p.f,0)!=0x5a4d)throw new IOException("Missing MZ signature.");
            long pe=u32(p.f,0x3c);if(!bounds(pe,24,p.len)||u32(p.f,pe)!=0x4550L)throw new IOException("Invalid PE signature.");
            p.sections=u16(p.f,pe+6);int optSize=u16(p.f,pe+20);p.optional=pe+24;
            p.magic=u16(p.f,p.optional);p.dd=p.magic==0x10b?96:(p.magic==0x20b?112:-1);
            if(p.dd<0||optSize<p.dd+15*8||p.sections<1||p.sections>96)throw new IOException("Unsupported PE optional header.");
            p.checksum=p.optional+64;p.sectionTable=p.optional+optSize;
            if(!bounds(p.sectionTable,p.sections*40L,p.len))throw new IOException("Section table is out of bounds.");
            long cliRva=u32(p.f,p.optional+p.dd+14L*8),cliSize=u32(p.f,p.optional+p.dd+14L*8+4);
            if(cliRva==0||cliSize<24)throw new IOException("No CLI header.");
            long cli=rvaToOffset(p.f,cliRva,p.sectionTable,p.sections,p.len);
            if(cli<0||!bounds(cli,24,p.len))throw new IOException("Invalid CLI header.");
            p.cliFlags=u32(p.f,cli+16);
            long mrva=u32(p.f,cli+8);p.metaSize=u32(p.f,cli+12);p.meta=rvaToOffset(p.f,mrva,p.sectionTable,p.sections,p.len);
            if(p.meta<0||p.metaSize<24||!bounds(p.meta,p.metaSize,p.len)||u32(p.f,p.meta)!=0x424a5342L)throw new IOException("Invalid metadata root.");
            long vl=u32(p.f,p.meta+12);if(vl>1024||!bounds(p.meta+16,vl+4,p.meta+p.metaSize))throw new IOException("Invalid metadata version.");
            long q=align4(p.meta+16+vl);int streams=u16(p.f,q+2);q+=4;
            if(streams<1||streams>32)throw new IOException("Invalid metadata stream count.");
            for(int i=0;i<streams;i++){
                if(!bounds(q,9,p.meta+p.metaSize))throw new IOException("Truncated metadata stream.");
                long off=u32(p.f,q),sz=u32(p.f,q+4),n=q+8;StringBuilder name=new StringBuilder();
                for(int x=0;x<32&&n<p.meta+p.metaSize;x++){int c=byteAt(p.f,n++);if(c==0)break;name.append((char)c);}
                q=align4(n);if(off>p.metaSize||sz>p.metaSize-off)continue;
                if("#Strings".contentEquals(name)){p.strings=p.meta+off;p.stringsSize=sz;}
                if("#~".contentEquals(name)||"#-".contentEquals(name)){p.tables=p.meta+off;p.tablesSize=sz;}
            }
            if(p.strings==0||p.tables==0||!bounds(p.tables,p.tablesSize,p.meta+p.metaSize)||p.tablesSize<24)throw new IOException("Missing #Strings or metadata tables.");
            p.heap=byteAt(p.f,p.tables+6);p.stringW=p.sw();p.blobW=p.bw();p.guidW=p.gw();
            long valid=u64(p.f,p.tables+8);q=p.tables+24;
            for(int t=0;t<64;t++)if((valid&(1L<<t))!=0){if(!bounds(q,4,p.tables+p.tablesSize))throw new IOException("Truncated row counts.");p.rows[t]=u32(p.f,q);q+=4;if(p.rows[t]>10000000)throw new IOException("Metadata row limit exceeded.");}
            long cursor=q;
            for(int t=0;t<64;t++)if(p.rows[t]>0){p.base[t]=cursor;long rs=p.rowSize(t),remain=p.tables+p.tablesSize-cursor;if(rs<=0||remain<0||p.rows[t]>remain/rs)throw new IOException("Unsupported or invalid table "+t+".");cursor+=p.rows[t]*rs;}
            long cert=p.optional+p.dd+4L*8;p.certSize=u32(p.f,cert+4);if(u32(p.f,cert)==0)p.certSize=0;
            return p;
        }catch(Exception e){try{p.f.close();}catch(Exception ignored){}throw e;}
    }

    private static long countStringRefs(Pe p,long target)throws Exception{
        int[] ts={0,1,2,4,6,8,10,20,23,26,32,35,38,39,40,42};long refs=0;
        for(int t:ts){long size=p.rowSize(t);for(long i=0;i<p.rows[t];i++){
            long r=p.base[t]+i*size;int w=p.sw();
            if(t==0){if(p.index(r+2,w)==target)refs++;}
            else if(t==1){if(p.index(r+p.cw(0),w)==target)refs++;}
            else if(t==2){if(p.index(r+4,w)==target)refs++;if(p.index(r+4+w,w)==target)refs++;}
            else if(t==4){if(p.index(r+2,w)==target)refs++;}
            else if(t==6){if(p.index(r+8,w)==target)refs++;}
            else if(t==8){if(p.index(r+4,w)==target)refs++;}
            else if(t==10){if(p.index(r+p.cw(2),w)==target)refs++;}
            else if(t==20||t==23){if(p.index(r+2,w)==target)refs++;}
            else if(t==26){if(p.index(r,w)==target)refs++;}
            else if(t==32){if(p.index(r+16+p.bw(),w)==target)refs++;}
            else if(t==35){if(p.index(r+12+p.bw(),w)==target)refs++;}
            else if(t==38){if(p.index(r+4,w)==target)refs++;}
            else if(t==39){if(p.index(r+8,w)==target)refs++;if(p.index(r+8+w,w)==target)refs++;}
            else if(t==40){if(p.index(r+8,w)==target)refs++;}
            else if(t==42){if(p.index(r+4+p.cw(12),w)==target)refs++;}
            if(refs>1)return refs;
        }}
        return refs;
    }

    private static String methodBody(RandomAccessFile f,long off,long len)throws Exception{
        int first=byteAt(f,off),format=first&3;long start,size;
        if(format==2){size=first>>>2;start=off+1;}
        else if(format==3){int flags=u16(f,off),words=(flags>>>12)&15;if(words<3||words>15)throw new IOException("Invalid fat header.");long hb=words*4L;if(!bounds(off,hb,len))throw new IOException("Truncated fat header.");size=u32(f,off+4);start=off+hb;}
        else throw new IOException("Unknown method header format.");
        if(size>MAX_BODY||!bounds(start,size,len))throw new IOException("Method body exceeds 256 KB or is out of bounds.");
        byte[] code=new byte[(int)size];f.seek(start);f.readFully(code);
        return "Header: "+(format==2?"tiny":"fat")+"; IL bytes: "+size+"\n"+disassemble(code);
    }

    private static String disassemble(byte[] c)throws Exception{
        StringBuilder out=new StringBuilder();int p=0,count=0;
        while(p<c.length&&count<MAX_INSTRUCTIONS){
            int start=p,op=c[p++]&255;String name;int arg;
            if(op==0xfe){if(p>=c.length)throw new IOException("Truncated FE opcode.");int x=c[p++]&255;name=FE[x];arg=FEA[x];if(name==null){out.append(at(start)).append(" FE").append(h2(x)).append(" <unsupported>\n");break;}}
            else{name=OPS[op];arg=ARGS[op];if(name==null){out.append(at(start)).append(" ").append(h2(op)).append(" <unsupported opcode; remaining body omitted>\n");break;}}
            out.append(at(start)).append(" ").append(name);String val="";
            switch(arg){
                case N:break;
                case SI:need(c,p,1);val=String.valueOf((int)c[p]);p++;break;
                case SV:need(c,p,1);val=String.valueOf(c[p]&255);p++;break;
                case V:need(c,p,2);val=String.valueOf(u16(c,p));p+=2;break;
                case I4:need(c,p,4);val=String.valueOf(i32(c,p));p+=4;break;
                case I8:need(c,p,8);val=hexBytes(c,p,8);p+=8;break;
                case R4:need(c,p,4);val=String.valueOf(Float.intBitsToFloat(i32(c,p)));p+=4;break;
                case R8:need(c,p,8);val=String.valueOf(Double.longBitsToDouble(i64(c,p)));p+=8;break;
                case SB:need(c,p,1);int s=(byte)c[p++];val="IL_"+h4(p+s);break;
                case B:need(c,p,4);int b=i32(c,p);p+=4;val="IL_"+h4(p+b);break;
                case SW:need(c,p,4);int n=i32(c,p);p+=4;if(n<0||n>4096)throw new IOException("Invalid switch count.");need(c,p,n*4);int end=p+n*4;StringBuilder targets=new StringBuilder("[");for(int i=0;i<n;i++){if(i>0)targets.append(", ");targets.append("IL_").append(h4(end+i32(c,p+i*4)));}targets.append("]");val=targets.toString();p=end;break;
                case T:need(c,p,4);val="token 0x"+h8(i32(c,p));p+=4;break;
                default:throw new IOException("Unknown operand type.");
            }
            if(!val.isEmpty())out.append(" ").append(val);out.append("\n");count++;
        }
        if(p<c.length)out.append("… output capped at ").append(MAX_INSTRUCTIONS).append(" instructions.\n");
        if(c.length==0)out.append("(empty body)\n");
        return out.toString();
    }
    private static void set(int x,String n,int a){OPS[x]=n;ARGS[x]=a;}
    private static void fe(int x,String n,int a){FE[x]=n;FEA[x]=a;}
    private static void need(byte[] b,int p,int n)throws IOException{if(p<0||n<0||p>b.length||n>b.length-p)throw new IOException("Truncated IL operand.");}
    private static int u16(byte[] b,int p){return(b[p]&255)|((b[p+1]&255)<<8);}
    private static int i32(byte[] b,int p){return(b[p]&255)|((b[p+1]&255)<<8)|((b[p+2]&255)<<16)|(b[p+3]<<24);}
    private static long i64(byte[] b,int p){return(i32(b,p)&0xffffffffL)|((long)i32(b,p+4)<<32);}
    private static String hexBytes(byte[] b,int p,int n){StringBuilder s=new StringBuilder("0x");for(int i=n-1;i>=0;i--)s.append(h2(b[p+i]&255));return s.toString();}
    private static String at(int p){return "IL_"+h4(p);}
    private static String h2(int v){return String.format(Locale.ROOT,"%02X",v&255);}
    private static String h4(long v){return String.format(Locale.ROOT,"%04X",v&65535);}
    private static String h8(long v){return String.format(Locale.ROOT,"%08X",v&0xffffffffL);}
    private static String safe(String s){return s==null?"unknown":s;}
    private static long align4(long n){return(n+3L)&~3L;}
    private static boolean bounds(long p,long n,long len){return p>=0&&n>=0&&p<=len&&n<=len-p;}
    private static int byteAt(RandomAccessFile r,long p)throws Exception{r.seek(p);return r.readUnsignedByte();}
    private static int u16(RandomAccessFile r,long p)throws Exception{r.seek(p);return r.readUnsignedByte()|(r.readUnsignedByte()<<8);}
    private static long u32(RandomAccessFile r,long p)throws Exception{r.seek(p);return r.readUnsignedByte()|((long)r.readUnsignedByte()<<8)|((long)r.readUnsignedByte()<<16)|((long)r.readUnsignedByte()<<24);}
    private static long u64(RandomAccessFile r,long p)throws Exception{return u32(r,p)|(u32(r,p+4)<<32);}
    private static long rvaToOffset(RandomAccessFile r,long rva,long sectionTable,int sections,long len)throws Exception{
        for(int i=0;i<sections;i++){long p=sectionTable+i*40L,vs=u32(r,p+8),va=u32(r,p+12),rs=u32(r,p+16),raw=u32(r,p+20),span=Math.max(vs,rs);if(rva>=va&&rva-va<span){long off=raw+(rva-va);return bounds(off,1,len)?off:-1;}}
        return -1;
    }
    private static void copy(File a,File b)throws Exception{
        try(FileInputStream in=new FileInputStream(a);FileOutputStream out=new FileOutputStream(b)){byte[] x=new byte[32768];int n;while((n=in.read(x))!=-1)out.write(x,0,n);out.flush();}
    }
}
