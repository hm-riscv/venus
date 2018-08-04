package venus.riscv.insts

import venus.riscv.insts.dsl.LoadTypeInstruction
import venus.simulator.Simulator

val lhu = LoadTypeInstruction(
        name = "lhu",
        opcode = 0b0000011,
        funct3 = 0b101,
//        load16 = NoImplementation,
        load32 = Simulator::loadHalfWordwCache
//        load64 = NoImplementation,
//        load128 = NoImplementation
)
