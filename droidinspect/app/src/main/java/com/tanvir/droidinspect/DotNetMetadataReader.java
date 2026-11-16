package com.tanvir.droidinspect;

import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bounded ECMA-335 metadata reader for identity, references, TypeDef, Field and MethodDef rows. */
final class DotNetMetadataReader {
    private static final int[] CODED_BITS = {2,2,3,2,5,3,1,2,1,1,1,2,1};
    private static final int[][] CODED_TABLES = {
        {0,26,35,1},                         // ResolutionScope
        {2,1,27},                            // TypeDefOrRef
        {2,1,26,6,27},                       // MemberRefParent
        {4,8,23},                            // HasConstant
        {6,4,1,2,8,9,10,0,14,23,20,17,26,27,32,35,38,39,40,42,44,43}, // HasCustomAttribute
        {6,10},                              // CustomAttributeType
        {4,8},                               // HasFieldMarshal
        {2,6,32},                            // HasDeclSecurity
        {20,23},                             // HasSemantics
        {6,10},                              // MethodDefOrRef
        {4,6},                               // MemberForwarded
        {38,35,39},                          // Implementation
        {2,6}                                // TypeOrMethodDef
    };

    private final RandomAccessFile r;
    private final long fileLength, root, rootSize;
    private long stringsBase, stringsSize, tablesBase, tablesSize, blobBase, blobSize, guidBase, guidSize;
    private long[] rows = new long[64], tableBase = new long[64];
    private int heapSizes;

    private DotNetMetadataReader(RandomAccessFile r,long root,long rootSize,long fileLength) {
        this.r=r; this.root=root; this.rootSize=rootSize; this.fileLength=fileLength;
    }

    static String inspect(RandomAccessFile r,long root,long rootSize,long fileLength) {
        try { return new DotNetMetadataReader(r,root,rootSize,fileLength).read(); }
        catch(Exception e) { return "\nDECODED .NET METADATA\nMetadata decoding: INCOMPLETE — "
                + e.getClass().getSimpleName() + ": " + safe(e.getMessage()); }
    }

    private String read() throws Exception {
        if (!bounds(root,20,rootSize)) throw new Exception("Metadata root is outside the declared metadata range.");
        if (u32(root)!=0x424A5342L) throw new Exception("BSJB metadata signature is missing.");
        long versionLen=u32(root+12);
        if(versionLen>1024 || !bounds(16,versionLen+4,rootSize)) throw new Exception("Invalid metadata version length.");
        long p=align4(root+16+versionLen);
        if(!bounds(p-root,4,rootSize)) throw new Exception("Truncated stream header.");
        int streamCount=u16(p+2); p+=4;
        if(streamCount>32) throw new Exception("Unreasonable metadata stream count.");
        for(int i=0;i<streamCount;i++) {
            if(!bounds(p-root,9,rootSize)) throw new Exception("Truncated metadata stream descriptor.");
            long off=u32(p), size=u32(p+4); long q=p+8; StringBuilder n=new StringBuilder();
            for(int j=0;j<32 && q<root+rootSize;j++) { int c=b(q++); if(c==0) break; n.append((char)c); }
            p=align4(q);
            if(off>rootSize || size>rootSize-off) continue;
            if("#Strings".contentEquals(n)){stringsBase=root+off;stringsSize=size;}
            else if("#Blob".contentEquals(n)){blobBase=root+off;blobSize=size;}
            else if("#GUID".contentEquals(n)){guidBase=root+off;guidSize=size;}
            else if("#~".contentEquals(n)||"#-".contentEquals(n)){tablesBase=root+off;tablesSize=size;}
        }
        if(tablesBase==0) throw new Exception("No #~ or #- metadata tables stream was found.");
        if(!bounds(tablesBase-root,tablesSize,rootSize) || tablesSize<24) throw new Exception("Invalid metadata tables stream.");
        heapSizes=b(tablesBase+6);
        long valid=u64(tablesBase+8), q=tablesBase+24;
        for(int i=0;i<64;i++) if((valid&(1L<<i))!=0){ if(!fileBounds(q,4))throw new Exception("Truncated metadata row counts."); rows[i]=u32(q); q+=4; }
        long cursor=q;
        for(int i=0;i<64;i++) if(rows[i]>0) {
            tableBase[i]=cursor;
            long size=rowSize(i);
            long remaining=tablesBase+tablesSize-cursor;
            if(size<=0 || remaining<0 || rows[i]>remaining/size)
                throw new Exception("Invalid or unsupported metadata table "+i+" bounds (rows="+rows[i]+", rowSize="+size+", remaining="+remaining+").");
            cursor+=rows[i]*size;
        }
        StringBuilder out=new StringBuilder("\nDECODED .NET ASSEMBLY METADATA\n");
        out.append("Metadata tables stream: ").append("#~ / #-").append('\n');
        out.append("String heap: ").append(stringsSize).append(" bytes; Blob heap: ").append(blobSize).append(" bytes\n");
        decodeModule(out);
        decodeAssembly(out);
        decodeReferences(out);
        decodeModuleReferences(out);
        decodeStandAloneSignatures(out);
        decodeTypeReferences(out);
        decodeMemberReferences(out);
        decodeTypeSpecifications(out);
        decodeMethodSpecifications(out);
        decodeTypes(out);
        decodeProperties(out);
        decodeEvents(out);
        decodeManifestResources(out);
        out.append("\nDecode status: ").append(stringsBase!=0 ? "core identity/type/member tables decoded where present" : "string heap unavailable").append('\n');
        out.append("Note: This is metadata inspection, not C# decompilation or IL editing.\n");
        return out.toString();
    }

    private void decodeModule(StringBuilder o) throws Exception {
        o.append("\nMODULE IDENTITY\n");
        if(rows[0]==0){o.append("Module table: absent.\n");return;}
        long p=tableBase[0];
        int generation=u16(p), name=indexString(p+2);
        long guidPos=p+2L+stringWidth();
        int mvid=guidWidth()==2?u16(guidPos):(int)u32(guidPos);
        o.append("Module name: ").append(str(name)).append('\n');
        o.append("Generation: ").append(generation).append('\n');
        if(mvid>0 && guidBase>0 && (long)(mvid-1)*16+16<=guidSize) {
            byte[] g=new byte[16]; r.seek(guidBase+(long)(mvid-1)*16); r.readFully(g);
            o.append("MVID: ").append(guid(g)).append('\n');
        } else o.append("MVID: unavailable\n");
    }

    private static String guid(byte[] g) {
        return String.format(Locale.ROOT,"%02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X",
            g[3]&255,g[2]&255,g[1]&255,g[0]&255,g[5]&255,g[4]&255,g[7]&255,g[6]&255,
            g[8]&255,g[9]&255,g[10]&255,g[11]&255,g[12]&255,g[13]&255,g[14]&255,g[15]&255);
    }

    private void decodeAssembly(StringBuilder o) throws Exception {
        o.append("\nASSEMBLY IDENTITY\n");
        if(rows[32]==0){o.append("Assembly table: absent (netmodule or non-assembly module).\n");return;}
        long p=tableBase[32];
        int hash=(int)u32(p), major=u16(p+4), minor=u16(p+6), build=u16(p+8), revision=u16(p+10);
        long flags=u32(p+12); int blob=indexBlob(p+16), name=indexString(p+16+blobWidth());
        int culture=indexString(p+16+blobWidth()+stringWidth());
        o.append("Name: ").append(str(name)).append('\n');
        o.append("Version: ").append(major).append('.').append(minor).append('.').append(build).append('.').append(revision).append('\n');
        o.append("Culture: ").append(empty(str(culture))).append('\n');
        o.append("Flags: 0x").append(hex(flags)).append('\n');
        o.append("Assembly metadata token: 0x20000001\n");
        o.append("Public key blob index: ").append(blob).append("; payload: ").append(blobLength(blob)).append(" bytes\n");
        o.append("Public-key token: ").append(publicKeyToken(blobBytes(blob), true)).append('\n');
    }

    private void decodeReferences(StringBuilder o) throws Exception {
        o.append("\nASSEMBLY REFERENCES (").append(rows[35]).append(")\n");
        int limit=(int)Math.min(rows[35],200);
        for(int i=0;i<limit;i++){
            long p=tableBase[35]+i*rowSize(35);
            int ma=u16(p),mi=u16(p+2),bu=u16(p+4),re=u16(p+6);
            long flags=u32(p+8);
            int keyBlob=indexBlob(p+12);
            int name=indexString(p+12+blobWidth()), culture=indexString(p+12+blobWidth()+stringWidth());
            o.append(i+1).append(". ").append(str(name)).append(", Version=").append(ma).append('.').append(mi).append('.').append(bu).append('.').append(re);
            String c=str(culture); if(!c.isEmpty())o.append(", Culture=").append(c);
            o.append(", PublicKeyToken=").append(publicKeyToken(blobBytes(keyBlob), (flags & 1L) != 0));
            o.append(" (AssemblyRef token 0x").append(hex(0x23000000L | (i + 1))).append(')');
            o.append('\n');
        }
        if(rows[35]>limit)o.append("… ").append(rows[35]-limit).append(" more references not displayed.\n");
    }



    private void decodeModuleReferences(StringBuilder o) throws Exception {
        o.append("\nMODULE REFERENCES (").append(rows[26]).append(")\n");
        int limit = (int) Math.min(rows[26], 500);
        for (int i = 0; i < limit; i++) {
            long p = tableBase[26] + i * rowSize(26);
            int name = indexString(p);
            o.append(i + 1).append(". ").append(str(name))
                    .append(" (ModuleRef token 0x").append(hex(0x1A000000L | (i + 1))).append(')')
                    .append('\n');
        }
    }

    private void decodeStandAloneSignatures(StringBuilder o) throws Exception {
        o.append("\nSTANDALONE SIGNATURES (").append(rows[17]).append(")\n");
        int limit = (int) Math.min(rows[17], 1000);
        for (int i = 0; i < limit; i++) {
            long p = tableBase[17] + i * rowSize(17);
            int signature = indexBlob(p);
            o.append(i + 1).append(". Signature bytes ").append(blobLength(signature))
                    .append(" (StandAloneSig token 0x").append(hex(0x11000000L | (i + 1))).append(')')
                    .append('\n');
        }
    }

    private void decodeTypeReferences(StringBuilder o) throws Exception {
        o.append("\nTYPE REFERENCES (").append(rows[1]).append(")\n");
        int limit = (int) Math.min(rows[1], 1000);
        long size = rowSize(1);
        for (int i = 0; i < limit; i++) {
            long p = tableBase[1] + i * size;
            long scope = codedIndex(p, 0);
            int name = indexString(p + codedWidth(0));
            int namespace = indexString(p + codedWidth(0) + stringWidth());
            String fullName = str(namespace).isEmpty() ? str(name) : str(namespace) + "." + str(name);
            o.append(i + 1).append(". ").append(fullName)
                    .append(" (TypeRef token 0x").append(hex(0x01000000L | (i + 1)))
                    .append("; ResolutionScope token 0x").append(hex(codedToken(scope, 0))).append(')')
                    .append('\n');
        }
        if (rows[1] > limit) o.append("… ").append(rows[1] - limit).append(" more TypeRef rows omitted.\n");
    }

    private void decodeMemberReferences(StringBuilder o) throws Exception {
        o.append("\nMEMBER REFERENCES (").append(rows[10]).append(")\n");
        int limit = (int) Math.min(rows[10], 2000);
        long size = rowSize(10);
        for (int i = 0; i < limit; i++) {
            long p = tableBase[10] + i * size;
            long parent = codedIndex(p, 2);
            int name = indexString(p + codedWidth(2));
            int signature = indexBlob(p + codedWidth(2) + stringWidth());
            o.append(i + 1).append(". ").append(str(name))
                    .append(" (MemberRef token 0x").append(hex(0x0A000000L | (i + 1)))
                    .append("; parent token 0x").append(hex(codedToken(parent, 2)))
                    .append("; signature bytes ").append(blobLength(signature)).append(')')
                    .append('\n');
        }
        if (rows[10] > limit) o.append("… ").append(rows[10] - limit).append(" more MemberRef rows omitted.\n");
    }

    private void decodeTypeSpecifications(StringBuilder o) throws Exception {
        o.append("\nTYPE SPECIFICATIONS (").append(rows[27]).append(")\n");
        int limit = (int) Math.min(rows[27], 1000);
        long size = rowSize(27);
        for (int i = 0; i < limit; i++) {
            long p = tableBase[27] + i * size;
            int signature = indexBlob(p);
            o.append(i + 1).append(". TypeSpec signature bytes ").append(blobLength(signature))
                    .append(" (TypeSpec token 0x").append(hex(0x1B000000L | (i + 1))).append(')')
                    .append('\n');
        }
        if (rows[27] > limit) o.append("… ").append(rows[27] - limit).append(" more TypeSpec rows omitted.\n");
    }

    private void decodeMethodSpecifications(StringBuilder o) throws Exception {
        o.append("\nMETHOD SPECIFICATIONS (").append(rows[43]).append(")\n");
        int limit = (int) Math.min(rows[43], 1000);
        long size = rowSize(43);
        for (int i = 0; i < limit; i++) {
            long p = tableBase[43] + i * size;
            long method = codedIndex(p, 9);
            int instantiation = indexBlob(p + codedWidth(9));
            o.append(i + 1).append(". MethodSpec token 0x").append(hex(0x2B000000L | (i + 1)))
                    .append("; method token 0x").append(hex(codedToken(method, 9)))
                    .append("; instantiation bytes ").append(blobLength(instantiation)).append('\n');
        }
        if (rows[43] > limit) o.append("… ").append(rows[43] - limit).append(" more MethodSpec rows omitted.\n");
    }

    private long codedToken(long coded, int codedTable) {
        int tagBits = CODED_BITS[codedTable];
        int tag = (int) (coded & ((1L << tagBits) - 1));
        long row = coded >>> tagBits;
        if (row <= 0 || tag < 0 || tag >= CODED_TABLES[codedTable].length) return 0;
        int table = CODED_TABLES[codedTable][tag];
        long prefix;
        switch (table) {
            case 0: prefix = 0x00000000L; break;
            case 1: prefix = 0x01000000L; break;
            case 2: prefix = 0x02000000L; break;
            case 6: prefix = 0x06000000L; break;
            case 10: prefix = 0x0A000000L; break;
            case 26: prefix = 0x1A000000L; break;
            case 27: prefix = 0x1B000000L; break;
            case 35: prefix = 0x23000000L; break;
            default: return 0;
        }
        return prefix | row;
    }

    private void decodeTypes(StringBuilder o) throws Exception {
        o.append("\nTYPES AND MEMBERS\n");
        o.append("TypeDef rows: ").append(rows[2]).append("; Field rows: ").append(rows[4]).append("; MethodDef rows: ").append(rows[6]).append('\n');
        int limit=(int)Math.min(rows[2],300);
        long typeSize=rowSize(2), fieldSize=rowSize(4), methodSize=rowSize(6);
        for(int i=0;i<limit;i++){
            long p=tableBase[2]+i*typeSize;
            long flags=u32(p); int name=indexString(p+4), ns=indexString(p+4+stringWidth());
            long listPos=p+4+2L*stringWidth()+codedWidth(1);
            long firstField=tableIndex(listPos,4), firstMethod=tableIndex(listPos+tableWidth(4),6);
            long nextField=rows[4]+1, nextMethod=rows[6]+1;
            if(i+1<rows[2]){
                long np=p+typeSize;
                nextField=tableIndex(np+4+2L*stringWidth()+codedWidth(1),4);
                nextMethod=tableIndex(np+4+2L*stringWidth()+codedWidth(1)+tableWidth(4),6);
            }
            String typeName=str(name), namespace=str(ns);
            o.append("\n").append(i+1).append(". ").append(namespace.isEmpty()?typeName:namespace+"."+typeName)
                    .append(" (TypeDef token 0x").append(hex(0x02000000L | (i + 1))).append(')');
            if((flags&0x20)!=0)o.append(" [interface]");
            o.append('\n');
            appendMembers(o,"Field",firstField,nextField,4,fieldSize,200);
            appendMembers(o,"Method",firstMethod,nextMethod,6,methodSize,200);
        }
        if(rows[2]>limit)o.append("\n… ").append(rows[2]-limit).append(" more types not displayed.\n");
    }

    private void decodeProperties(StringBuilder o) throws Exception {
        o.append("\nPROPERTIES\n");
        o.append("PropertyMap rows: ").append(rows[21]).append("; Property rows: ").append(rows[23]).append('\n');
        if (rows[21] == 0 || rows[23] == 0) {
            o.append("No property metadata rows were found.\n");
            return;
        }
        int mapLimit = (int) Math.min(rows[21], 300);
        long mapSize = rowSize(21);
        long propertySize = rowSize(23);
        for (int i = 0; i < mapLimit; i++) {
            long p = tableBase[21] + i * mapSize;
            long parentType = tableIndex(p, 2);
            long first = tableIndex(p + tableWidth(2), 23);
            long next = i + 1 < rows[21]
                    ? tableIndex(p + mapSize + tableWidth(2), 23)
                    : rows[23] + 1;
            if (parentType < 1 || parentType > rows[2] || first < 1
                    || next < first || first > rows[23] + 1) {
                o.append("PropertyMap ").append(i + 1).append(": invalid parent/list indices; omitted.\n");
                continue;
            }
            long typeRow = tableBase[2] + (parentType - 1) * rowSize(2);
            String typeName = str(indexString(typeRow + 4));
            String namespace = str(indexString(typeRow + 4 + stringWidth()));
            o.append("\n").append(namespace.isEmpty() ? typeName : namespace + "." + typeName).append('\n');
            int shown = (int) Math.min(Math.min(next - first, 200), rows[23] + 1 - first);
            for (int j = 0; j < shown; j++) {
                long propertyRow = tableBase[23] + (first - 1 + j) * propertySize;
                int propertySignature = indexBlob(propertyRow + 2 + stringWidth());
                o.append("   Property: ").append(str(indexString(propertyRow + 2)))
                        .append(" (Property token 0x").append(hex(0x17000000L | (first + j)))
                        .append("; signature=").append(signatureHex(propertySignature)).append(')')
                        .append('\n');
            }
            if (next - first > shown) {
                o.append("   … additional properties omitted by display cap.\n");
            }
        }
        if (rows[21] > mapLimit) {
            o.append("… ").append(rows[21] - mapLimit).append(" additional PropertyMap rows omitted.\n");
        }
    }


    private void decodeEvents(StringBuilder o) throws Exception {
        o.append("\nEVENTS\n");
        o.append("EventMap rows: ").append(rows[18]).append("; Event rows: ").append(rows[20]).append('\n');
        if (rows[18] == 0 || rows[20] == 0) {
            o.append("No event metadata rows were found.\n");
            return;
        }
        int mapLimit = (int) Math.min(rows[18], 300);
        long mapSize = rowSize(18);
        long eventSize = rowSize(20);
        for (int i = 0; i < mapLimit; i++) {
            long p = tableBase[18] + i * mapSize;
            long parentType = tableIndex(p, 2);
            long first = tableIndex(p + tableWidth(2), 20);
            long next = i + 1 < rows[18]
                    ? tableIndex(p + mapSize + tableWidth(2), 20)
                    : rows[20] + 1;
            if (parentType < 1 || parentType > rows[2] || first < 1
                    || next < first || first > rows[20] + 1) {
                o.append("EventMap ").append(i + 1).append(": invalid parent/list indices; omitted.\n");
                continue;
            }
            long typeRow = tableBase[2] + (parentType - 1) * rowSize(2);
            String typeName = str(indexString(typeRow + 4));
            String namespace = str(indexString(typeRow + 4 + stringWidth()));
            o.append("\n").append(namespace.isEmpty() ? typeName : namespace + "." + typeName).append('\n');
            int shown = (int) Math.min(Math.min(next - first, 200), rows[20] + 1 - first);
            for (int j = 0; j < shown; j++) {
                long eventRow = first + j;
                long eventPos = tableBase[20] + (eventRow - 1) * eventSize;
                String eventName = str(indexString(eventPos + 2));
                long codedType = codedIndex(eventPos + 2 + stringWidth(), 1);
                o.append("   Event: ").append(eventName)
                        .append(" (Event token 0x").append(hex(0x14000000L | eventRow))
                        .append("; event type token 0x").append(hex(decodeTypeDefOrRefToken(codedType))).append(')')
                        .append('\n');
            }
            if (next - first > shown) o.append("   … additional events omitted by display cap.\n");
        }
        if (rows[18] > mapLimit) o.append("… ").append(rows[18] - mapLimit).append(" additional EventMap rows omitted.\n");
    }

    private void decodeManifestResources(StringBuilder out) throws Exception {
        out.append("\nMANIFEST RESOURCES (").append(rows[40]).append(")\n");
        int limit = (int) Math.min(rows[40], 2000);
        long size = rowSize(40);
        for (int i = 0; i < limit; i++) {
            long p = tableBase[40] + i * size;
            long offset = u32(p);
            long flags = u32(p + 4);
            int nameIndex = indexString(p + 8);
            long implementation = codedIndex(p + 8 + stringWidth(), 11);
            int tag = (int) (implementation & 3L);
            long row = implementation >>> 2;
            String kind = implementation == 0 ? "embedded" : tag == 0 ? "File" : tag == 1 ? "AssemblyRef" : tag == 2 ? "ExportedType" : "invalid";
            out.append(i + 1).append(". ").append(str(nameIndex))
                    .append(" (ManifestResource token 0x").append(hex(0x28000000L | (i + 1)))
                    .append("; offset=").append(offset)
                    .append("; flags=0x").append(hex(flags))
                    .append("; implementation=").append(kind);
            if (row > 0) out.append(" row=").append(row);
            out.append(')').append('\n');
        }
        if (rows[40] > limit) out.append("… ").append(rows[40] - limit).append(" additional resource rows omitted.\n");
        if (rows[40] == 0) out.append("No manifest resource metadata rows found.\n");
    }

    private long decodeTypeDefOrRefToken(long coded) {
        int tag = (int) (coded & 3L);
        long row = coded >>> 2;
        if (row <= 0 || tag < 0 || tag >= 3) return 0;
        int table = CODED_TABLES[1][tag];
        long prefix = table == 2 ? 0x02000000L : table == 1 ? 0x01000000L : 0x1B000000L;
        return prefix | row;
    }

    private void appendMembers(StringBuilder o,String label,long first,long next,int table,long size,int cap)throws Exception{
        if(first<1||next<first||first>rows[table]+1)return;
        int count=(int)Math.min(Math.min(next-first,cap),rows[table]+1-first);
        for(int j=0;j<count;j++){
            long p=tableBase[table]+(first-1+j)*size;
            int name = table == 4 ? indexString(p + 2) : indexString(p + 8);
            int signature = table == 4 ? indexBlob(p + 2 + stringWidth())
                    : indexBlob(p + 8 + stringWidth());
            long row = first + j;
            long token = (table == 4 ? 0x04000000L : 0x06000000L) | row;
            o.append("   ").append(label).append(": ").append(str(name))
                    .append(" (").append(label).append(table == 4 ? " token 0x" : "Def token 0x")
                    .append(hex(token)).append("; signature=").append(signatureHex(signature)).append(')')
                    .append('\n');
        }
        if(next-first>count)o.append("   … additional ").append(label.toLowerCase(Locale.ROOT)).append(" entries omitted\n");
    }

    private long rowSize(int t) {
        switch(t){
            case 0:return 2+stringWidth()+3L*guidWidth();
            case 1:return codedWidth(0)+2L*stringWidth();
            case 2:return 4+2L*stringWidth()+codedWidth(1)+tableWidth(4)+tableWidth(6);
            case 3:return tableWidth(4);
            case 4:return 2+stringWidth()+blobWidth();
            case 5:return tableWidth(6);
            case 6:return 8+stringWidth()+blobWidth()+tableWidth(8);
            case 7:return tableWidth(8);
            case 8:return 4+stringWidth();
            case 9:return tableWidth(2)+codedWidth(1);
            case 10:return codedWidth(2)+stringWidth()+blobWidth();
            case 11:return 2+codedWidth(3)+blobWidth();
            case 12:return codedWidth(4)+codedWidth(5)+blobWidth();
            case 13:return codedWidth(6)+blobWidth();
            case 14:return 2+codedWidth(7)+blobWidth();
            case 15:return 6+tableWidth(2);
            case 16:return 4+tableWidth(4);
            case 17:return blobWidth();
            case 18:return tableWidth(2)+tableWidth(20);
            case 19:return tableWidth(20);
            case 20:return 2+stringWidth()+codedWidth(1);
            case 21:return tableWidth(2)+tableWidth(23);
            case 22:return tableWidth(23);
            case 23:return 2+stringWidth()+blobWidth();
            case 24:return 2+tableWidth(6)+codedWidth(8);
            case 25:return tableWidth(2)+2L* codedWidth(9);
            case 26:return stringWidth();
            case 27:return blobWidth();
            case 28:return 2+codedWidth(10)+stringWidth()+tableWidth(26);
            case 29:return 4+tableWidth(4);
            case 30:return 8;
            case 31:return 4;
            case 32:return 16+blobWidth()+2L*stringWidth();
            case 33:return 4;
            case 34:return 12;
            case 35:return 12+2L*blobWidth()+2L*stringWidth();
            case 36:return 4+tableWidth(35);
            case 37:return 12+tableWidth(35);
            case 38:return 4+stringWidth()+blobWidth();
            case 39:return 8+2L*stringWidth()+codedWidth(11);
            case 40:return 8+stringWidth()+codedWidth(11);
            case 41:return 2L*tableWidth(2);
            case 42:return 4+codedWidth(12)+stringWidth();
            case 43:return codedWidth(9)+blobWidth();
            case 44:return tableWidth(42)+codedWidth(1);
            default:return -1;
        }
    }

    private int stringWidth(){return (heapSizes&1)!=0?4:2;}
    private int guidWidth(){return (heapSizes&2)!=0?4:2;}
    private int blobWidth(){return (heapSizes&4)!=0?4:2;}
    private int tableWidth(int t){return rows[t]>=65536?4:2;}
    private int codedWidth(int c){
        int max=0; for(int t:CODED_TABLES[c]) if(t>=0&&t<rows.length)max=Math.max(max,(int)Math.min(rows[t],Integer.MAX_VALUE));
        return max < (1 << (16-CODED_BITS[c])) ? 2 : 4;
    }
    private long codedIndex(long p, int c) throws Exception {
        return codedWidth(c) == 2 ? u16(p) : u32(p);
    }
    private long tableIndex(long p,int t)throws Exception{return tableWidth(t)==2?u16(p):u32(p);}
    private int indexString(long p)throws Exception{return stringWidth()==2?u16(p):(int)u32(p);}
    private int indexBlob(long p)throws Exception{return blobWidth()==2?u16(p):(int)u32(p);}

    private byte[] blobBytes(int index) throws Exception {
        int length = blobLength(index);
        if (length <= 0) return new byte[0];
        long p = blobBase + index;
        int first = b(p++);
        if ((first & 0x80) == 0) {
            // One-byte compressed length.
        } else if ((first & 0xC0) == 0x80) {
            p++;
        } else if ((first & 0xE0) == 0xC0) {
            p += 3;
        } else {
            return new byte[0];
        }
        if (!fileBounds(p, length)) return new byte[0];
        byte[] bytes = new byte[length];
        r.seek(p);
        r.readFully(bytes);
        return bytes;
    }

    private String publicKeyToken(byte[] value, boolean fullPublicKey) {
        if (value == null || value.length == 0) return "null";
        try {
            byte[] token = value;
            if (fullPublicKey) {
                byte[] hash = java.security.MessageDigest.getInstance("SHA-1").digest(value);
                token = new byte[8];
                for (int i = 0; i < token.length; i++) token[i] = hash[hash.length - 1 - i];
            }
            StringBuilder text = new StringBuilder();
            for (byte b : token) text.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return text.toString();
        } catch (Exception error) {
            return "unavailable";
        }
    }

    private int blobLength(int index)throws Exception {
        if(index<=0||blobBase==0||index>=blobSize)return 0;
        long p=blobBase+index; int first=b(p++); long length;
        if((first&0x80)==0) length=first;
        else if((first&0xC0)==0x80) length=((first&0x3F)<<8)|b(p++);
        else if((first&0xE0)==0xC0) length=((long)(first&0x1F)<<24)|((long)b(p++)<<16)|((long)b(p++)<<8)|b(p++);
        else return 0;
        if (length > Integer.MAX_VALUE || length > blobSize - (p - blobBase)) return 0;
        return (int) length;
    }

    private String signatureHex(int index) throws Exception {
        if (index <= 0) return "empty";
        byte[] bytes = blobBytes(index);
        if (bytes.length == 0) return "unavailable";
        StringBuilder value = new StringBuilder("0x");
        int limit = Math.min(bytes.length, 64);
        for (int i = 0; i < limit; i++) value.append(String.format(Locale.ROOT, "%02X", bytes[i] & 255));
        if (bytes.length > limit) value.append("…(").append(bytes.length).append(" bytes)");
        return value.toString();
    }

    private String str(int index)throws Exception{
        if(index==0||stringsBase==0||index>=stringsSize)return "";
        long p=stringsBase+index; ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        while(p<stringsBase+stringsSize){int value=b(p++);if(value==0)break;bytes.write(value);}
        return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
    }
    private static String empty(String s){return s.isEmpty()?"(neutral)":s;}
    private static String hex(long v){return String.format(Locale.ROOT,"%08X",v);}
    private static String safe(String s){return s==null?"unknown":s;}
    private int b(long p)throws Exception{r.seek(p);return r.readUnsignedByte();}
    private int u16(long p)throws Exception{r.seek(p);return r.readUnsignedByte()|(r.readUnsignedByte()<<8);}
    private long u32(long p)throws Exception{return u16(p)|(long)u16(p+2)<<16;}
    private long u64(long p)throws Exception{return u32(p)|(u32(p+4)<<32);}
    private long align4(long v){return (v+3)&~3L;}
    private boolean fileBounds(long p,long n){return p>=0&&n>=0&&p<=fileLength&&n<=fileLength-p;}
    private boolean bounds(long p,long n,long limit){return p>=0&&n>=0&&p<=limit&&n<=limit-p;}
}