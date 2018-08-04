package venus.riscv.insts

import venus.riscv.insts.dsl.ITypeInstruction

val xori = ITypeInstruction(
        name = "xori",
        opcode = 0b0010011,
        funct3 = 0b100,
        eval16 = { a, b -> (a.toInt() xor b.toInt()).toShort() },
        eval32 = { a, b -> a xor b },
        eval64 = { a, b -> a xor b },
        eval128 = { a, b -> a xor b }
)
