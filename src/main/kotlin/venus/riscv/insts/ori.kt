package venus.riscv.insts

import venus.riscv.insts.dsl.ITypeInstruction

val ori = ITypeInstruction(
        name = "ori",
        opcode = 0b0010011,
        funct3 = 0b110,
        eval16 = { a, b -> (a.toInt() or b.toInt()).toShort() },
        eval32 = { a, b -> a or b },
        eval64 = { a, b -> a or b },
        eval128 = { a, b -> a or b }
)
