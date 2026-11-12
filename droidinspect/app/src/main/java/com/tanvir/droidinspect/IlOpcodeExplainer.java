package com.tanvir.droidinspect;

import java.util.HashMap;
import java.util.Map;

/**
 * Human-readable explanations for the ECMA-335 opcodes emitted by DotNetIlReader.
 * Unknown mnemonics intentionally return an explicit unavailable message.
 */
final class IlOpcodeExplainer {
    private static final Map<String, String> MEANINGS = new HashMap<>();

    static {
        put("nop", "No operation; execution continues without changing the evaluation stack.");
        put("break", "Requests a debugger break; it does not perform an application-level calculation.");
        put("ldarg.0", "Load argument 0 onto the evaluation stack.");
        put("ldarg.1", "Load argument 1 onto the evaluation stack.");
        put("ldarg.2", "Load argument 2 onto the evaluation stack.");
        put("ldarg.3", "Load argument 3 onto the evaluation stack.");
        put("ldarg.s", "Load the argument selected by the one-byte argument index onto the evaluation stack.");
        put("ldarg", "Load the argument selected by the two-byte argument index onto the evaluation stack.");
        put("ldarga.s", "Push the managed address of the argument selected by the one-byte index.");
        put("ldarga", "Push the managed address of the argument selected by the two-byte index.");
        put("starg.s", "Pop a value and store it in the argument selected by the one-byte index.");
        put("starg", "Pop a value and store it in the argument selected by the two-byte index.");
        put("ldloc.0", "Load local variable 0 onto the evaluation stack.");
        put("ldloc.1", "Load local variable 1 onto the evaluation stack.");
        put("ldloc.2", "Load local variable 2 onto the evaluation stack.");
        put("ldloc.3", "Load local variable 3 onto the evaluation stack.");
        put("ldloc.s", "Load the local variable selected by the one-byte index onto the evaluation stack.");
        put("ldloc", "Load the local variable selected by the two-byte index onto the evaluation stack.");
        put("ldloca.s", "Push the managed address of the local variable selected by the one-byte index.");
        put("ldloca", "Push the managed address of the local variable selected by the two-byte index.");
        put("stloc.0", "Pop a value and store it in local variable 0.");
        put("stloc.1", "Pop a value and store it in local variable 1.");
        put("stloc.2", "Pop a value and store it in local variable 2.");
        put("stloc.3", "Pop a value and store it in local variable 3.");
        put("stloc.s", "Pop a value and store it in the local variable selected by the one-byte index.");
        put("stloc", "Pop a value and store it in the local variable selected by the two-byte index.");
        put("ldnull", "Push a null object reference onto the evaluation stack.");
        put("ldc.i4.m1", "Push the 32-bit integer constant -1 onto the evaluation stack.");
        for (int i = 0; i <= 8; i++) put("ldc.i4." + i, "Push the 32-bit integer constant " + i + " onto the evaluation stack.");
        put("ldc.i4.s", "Push the signed 8-bit integer operand, sign-extended to 32 bits.");
        put("ldc.i4", "Push the 32-bit integer operand.");
        put("ldc.i8", "Push the 64-bit integer operand.");
        put("ldc.r4", "Push the 32-bit floating-point operand.");
        put("ldc.r8", "Push the 64-bit floating-point operand.");
        put("dup", "Duplicate the value at the top of the evaluation stack.");
        put("pop", "Remove the value at the top of the evaluation stack.");
        put("ret", "Return from the current method; a return value is consumed when the method signature requires one.");
        put("add", "Pop two values, add the second to the first, and push the result.");
        put("sub", "Pop two values, subtract the top value from the next value, and push the result.");
        put("mul", "Pop two values, multiply them, and push the result.");
        put("div", "Pop two values, divide the lower value by the top value, and push the quotient.");
        put("div.un", "Unsigned division: pop two values and push the quotient.");
        put("rem", "Pop two values and push the signed remainder.");
        put("rem.un", "Pop two values and push the unsigned remainder.");
        put("and", "Pop two integer values, perform bitwise AND, and push the result.");
        put("or", "Pop two integer values, perform bitwise OR, and push the result.");
        put("xor", "Pop two integer values, perform bitwise XOR, and push the result.");
        put("shl", "Pop a value and shift count; shift the value left and push the result.");
        put("shr", "Pop a value and shift count; perform an arithmetic right shift and push the result.");
        put("shr.un", "Pop a value and shift count; perform a logical right shift and push the result.");
        put("neg", "Pop a numeric value, negate it, and push the result.");
        put("not", "Pop an integer value, invert its bits, and push the result.");
        put("call", "Call the method identified by the metadata token; arguments and any instance are consumed and a return value may be pushed.");
        put("callvirt", "Make a virtual or interface method call using the metadata token; the instance and arguments are consumed.");
        put("calli", "Call through the function pointer described by the signature token; arguments and pointer are consumed.");
        put("newobj", "Allocate an object and invoke the constructor identified by the metadata token; push the new reference.");
        put("jmp", "Transfer control to the method identified by the metadata token; the current method returns without a normal call frame.");
        put("ldstr", "Load the user-string heap entry identified by the metadata token and push the string reference.");
        put("ldtoken", "Push a runtime handle for the type, method, or field identified by the metadata token.");
        put("throw", "Pop an exception object reference and throw it; control transfers to exception handling.");
        put("rethrow", "Rethrow the currently handled exception from the active catch handler.");
        put("box", "Box the value type identified by the metadata token; pop the value and push an object reference.");
        put("unbox", "Pop an object reference and push a managed pointer to the contained value type.");
        put("unbox.any", "Unbox or cast the object to the type identified by the metadata token and push the resulting value.");
        put("castclass", "Pop an object reference and cast it to the metadata-token type; push the cast reference or throw.");
        put("isinst", "Test an object reference against the metadata-token type; push the compatible reference or null.");
        put("newarr", "Pop the element count and create an array of the metadata-token element type; push the array reference.");
        put("ldlen", "Pop an array reference and push its length.");
        put("ldfld", "Pop an object reference and load the instance field identified by the metadata token.");
        put("ldflda", "Pop an object reference and push the managed address of the instance field.");
        put("stfld", "Pop a value and object reference, then store the value in the instance field.");
        put("ldsfld", "Load the static field identified by the metadata token and push its value.");
        put("ldsflda", "Push the managed address of the static field identified by the metadata token.");
        put("stsfld", "Pop a value and store it in the static field identified by the metadata token.");
        put("ldobj", "Pop a managed pointer and load the value of the metadata-token type.");
        put("stobj", "Pop a value and managed pointer, then store the value using the metadata-token type.");
        put("cpobj", "Copy the value at one managed address to another using the metadata-token type.");
        put("initobj", "Initialize the value at a managed address to the default value of the metadata-token type.");
        put("sizeof", "Push the size in bytes of the metadata-token type.");
        put("ldftn", "Push a native pointer to the method identified by the metadata token.");
        put("ldvirtftn", "Pop an object reference and push the virtual function pointer for the metadata-token method.");
        put("arglist", "Push a runtime handle for the current method's variable argument list.");
        put("localloc", "Pop a byte count, allocate stack memory, and push its address.");
        put("ckfinite", "Check the floating-point value at the top of the stack for finiteness.");
        put("mkrefany", "Pop a managed pointer and create a typed reference using the metadata-token type.");
        put("refanyval", "Pop a typed reference and push a managed pointer to the requested metadata-token type.");
        put("refanytype", "Pop a typed reference and push its runtime type handle.");
        put("cpblk", "Copy a specified number of bytes between unmanaged memory addresses.");
        put("initblk", "Initialize a specified number of bytes at an unmanaged memory address.");
        put("endfinally", "End a finally or fault handler and continue the pending control transfer.");
        put("endfilter", "End an exception filter; the integer result determines whether the handler is selected.");
        put("leave", "Clear the evaluation stack and branch to the target, running intervening finally handlers.");
        put("leave.s", "Short form of leave: clear the evaluation stack and branch to the target.");
        put("switch", "Pop an integer selector and branch to the matching relative target; otherwise continue after the table.");
        put("br", "Unconditionally branch to the relative target.");
        put("br.s", "Short unconditional branch to the relative target.");
        put("brfalse", "Pop a value; branch when it is zero or null.");
        put("brfalse.s", "Short conditional branch when the popped value is zero or null.");
        put("brtrue", "Pop a value; branch when it is nonzero or non-null.");
        put("brtrue.s", "Short conditional branch when the popped value is nonzero or non-null.");
        put("beq", "Pop two values; branch when they are equal.");
        put("beq.s", "Short branch when the two popped values are equal.");
        put("bne.un", "Pop two values; branch when they are not equal using unsigned/unordered comparison.");
        put("bne.un.s", "Short branch when the two popped values are not equal using unsigned/unordered comparison.");
        put("tail.", "Call prefix: the following call may be executed as a tail call.");
        put("volatile.", "Memory-access prefix: the following memory operation is volatile.");
        put("readonly.", "Array-address prefix: the following ldelema operation returns a read-only managed pointer.");
        put("ceq", "Pop two values, compare them for equality, and push 1 when equal or 0 otherwise.");
        put("cgt", "Pop two values, compare whether the first is greater than the second, and push 1 or 0.");
        put("cgt.un", "Unsigned/unordered greater-than comparison; pop two values and push 1 or 0.");
        put("clt", "Pop two values, compare whether the first is less than the second, and push 1 or 0.");
        put("clt.un", "Unsigned/unordered less-than comparison; pop two values and push 1 or 0.");
        put("ldelema", "Pop an array index and array reference, then push the managed address of that element.");
        put("ldvirtftn", "Pop an object reference and push the resolved virtual method pointer identified by the token.");
        put("ldind.i1", "Pop a managed pointer, load a signed 8-bit integer, and push it.");
        put("ldind.u1", "Pop a managed pointer, load an unsigned 8-bit integer, and push it.");
        put("ldind.i2", "Pop a managed pointer, load a signed 16-bit integer, and push it.");
        put("ldind.u2", "Pop a managed pointer, load an unsigned 16-bit integer, and push it.");
        put("ldind.i4", "Pop a managed pointer, load a 32-bit integer, and push it.");
        put("ldind.u4", "Pop a managed pointer, load a 32-bit unsigned integer, and push it.");
        put("ldind.i8", "Pop a managed pointer, load a 64-bit integer, and push it.");
        put("ldind.i", "Pop a managed pointer, load a native-sized integer, and push it.");
        put("ldind.r4", "Pop a managed pointer, load a 32-bit floating-point value, and push it.");
        put("ldind.r8", "Pop a managed pointer, load a 64-bit floating-point value, and push it.");
        put("ldind.ref", "Pop a managed pointer, load an object reference, and push it.");
        put("stind.ref", "Pop an object reference and managed pointer, then store the reference.");
        put("stind.i1", "Pop an integer and managed pointer, then store the low 8 bits.");
        put("stind.i2", "Pop an integer and managed pointer, then store the low 16 bits.");
        put("stind.i4", "Pop an integer and managed pointer, then store the 32-bit value.");
        put("stind.i8", "Pop an integer and managed pointer, then store the 64-bit value.");
        put("stind.r4", "Pop a floating-point value and managed pointer, then store the 32-bit value.");
        put("stind.r8", "Pop a floating-point value and managed pointer, then store the 64-bit value.");
        put("stind.i", "Pop a native-sized integer and managed pointer, then store the value.");
        put("endfilter", "Pop the filter result and end the exception filter; nonzero selects the handler.");
        put("ldelem", "Pop an array index and array reference, load the element using the metadata-token type, and push it.");
        put("stelem", "Pop a value, array index, and array reference, then store using the metadata-token type.");
        put("constrained.", "Call prefix: supplies the constrained type for the following callvirt instruction.");
        put("unaligned.", "Memory-access prefix: specifies the alignment guarantee for the following memory operation.");
    }

    private static void put(String opcode, String meaning) { MEANINGS.put(opcode, meaning); }

    static String explain(String opcode, String operand) {
        String known = MEANINGS.get(opcode);
        if (known != null) return known + operandNote(operand);
        if (opcode.startsWith("br") || opcode.startsWith("beq") || opcode.startsWith("bge")
                || opcode.startsWith("bgt") || opcode.startsWith("ble") || opcode.startsWith("blt")) {
            return "Conditional or unconditional control-flow branch; target operand is relative to the instruction following this opcode." + operandNote(operand);
        }
        if (opcode.startsWith("ldind.")) return "Pop a managed pointer, load the indicated primitive or reference value, and push it.";
        if (opcode.startsWith("stind.")) return "Pop a value and managed pointer, then store the value through that pointer.";
        if (opcode.startsWith("ldelem.")) return "Pop an array index and array reference, load the indicated element, and push it.";
        if (opcode.startsWith("stelem.")) return "Pop a value, array index, and array reference, then store the element.";
        if (opcode.startsWith("conv.")) return "Convert the top evaluation-stack value to the type indicated by the opcode; overflow variants may throw.";
        if (opcode.startsWith("ldc.")) return "Load the constant represented by the opcode and operand onto the evaluation stack.";
        if (opcode.startsWith("add.") || opcode.startsWith("sub.") || opcode.startsWith("mul.")) {
            return "Pop two numeric values, perform the indicated arithmetic operation, and push the result; overflow variants may throw.";
        }
        if (opcode.startsWith("ldelem") || opcode.startsWith("stelem")) {
            return "Array element operation; stack inputs include the array reference and index.";
        }
        return "No verified explanation is available for this opcode in the current description table.";
    }

    private static String operandNote(String operand) {
        if (operand == null || operand.isEmpty()) return "";
        return " Operand: " + operand + ".";
    }

    private IlOpcodeExplainer() { }
}
