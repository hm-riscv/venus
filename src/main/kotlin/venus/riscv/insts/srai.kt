package venus.riscv.insts

import venus.riscv.insts.dsl.ShiftImmediateInstruction

val srai = ShiftImmediateInstruction(
        name = "srai",
        funct3 = 0b101,
        funct7 = 0b0100000,
        eval16 = { a, b -> if (b == 0.toShort()) a else ((a.toInt() shr b.toInt()).toShort()) },
        eval32 = { a, b -> if (b == 0) a else (a shr b) },
        eval64 = { a, b -> if (b == 0.toLong()) a else (a shr b.toInt()) },
        eval128 = { a, b -> if (b == 0.toLong()) a else (a shr b.toInt()) }
)
