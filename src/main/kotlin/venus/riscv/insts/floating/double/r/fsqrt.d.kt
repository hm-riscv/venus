package venus.riscv.insts.floating.double.r

import venus.riscv.insts.dsl.floating.FRRTypeInstruction
import venus.riscv.insts.floating.Decimal
import kotlin.math.sqrt

val fsqrtd = FRRTypeInstruction(
        name = "fsqrt.d",
        opcode = 0b1010011,
        funct7 = 0b0101101,
        rs2 = 0b00000,
        eval32 = { a, b -> Decimal(d = sqrt(a.getDouble()), isF = false) }
)